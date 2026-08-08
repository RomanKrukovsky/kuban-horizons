package dev.romankrukovsky.kubanhorizons.genie.runtime.transaction;

import dev.romankrukovsky.kubanhorizons.genie.runtime.confirmation.ConfirmationAuthority;
import dev.romankrukovsky.kubanhorizons.genie.runtime.confirmation.ConfirmedRestore;
import dev.romankrukovsky.kubanhorizons.genie.runtime.preview.PreviewService;
import dev.romankrukovsky.kubanhorizons.genie.runtime.recovery.AffectedScope;
import dev.romankrukovsky.kubanhorizons.genie.runtime.recovery.RecoveryJournal;
import dev.romankrukovsky.kubanhorizons.genie.runtime.recovery.RecoveryRecord;
import dev.romankrukovsky.kubanhorizons.genie.runtime.recovery.TransactionState;
import dev.romankrukovsky.kubanhorizons.genie.runtime.selection.RegionSelection;
import dev.romankrukovsky.kubanhorizons.genie.runtime.snapshot.RegionSnapshot;
import dev.romankrukovsky.kubanhorizons.genie.runtime.snapshot.SnapshotService;
import dev.romankrukovsky.kubanhorizons.genie.runtime.snapshot.SnapshotStore;
import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;

/** Единственная точка мутации region restore: prepare, apply, verify, commit/rollback. */
public final class RestoreTransactionService {
    private static final java.time.Duration UNDO_RETENTION = java.time.Duration.ofHours(24);
    private final ConfirmationAuthority confirmations;
    private final PreviewService previews;
    private final SnapshotStore store;
    private final SnapshotService snapshots;
    private final RecoveryJournal journal;
    private final CausalLedger ledger;
    private final RegionLockManager locks;
    private final TransactionManifestStore manifests;

    public RestoreTransactionService(ConfirmationAuthority confirmations, PreviewService previews,
                                     SnapshotStore store, SnapshotService snapshots,
                                     RecoveryJournal journal, CausalLedger ledger,
                                     RegionLockManager locks, TransactionManifestStore manifests) {
        this.confirmations = Objects.requireNonNull(confirmations, "confirmations");
        this.previews = Objects.requireNonNull(previews, "previews");
        this.store = Objects.requireNonNull(store, "store");
        this.snapshots = Objects.requireNonNull(snapshots, "snapshots");
        this.journal = Objects.requireNonNull(journal, "journal");
        this.ledger = Objects.requireNonNull(ledger, "ledger");
        this.locks = Objects.requireNonNull(locks, "locks");
        this.manifests = Objects.requireNonNull(manifests, "manifests");
    }

    public TransactionReport restore(ServerLevel level, UUID actorId,
                                     ConfirmedRestore confirmed, Instant now) throws IOException {
        UUID transactionId = UUID.randomUUID();
        if (!confirmations.consume(confirmed, actorId, now)) {
            return new TransactionReport(transactionId, TransactionOutcome.REJECTED, 0,
                    "confirmation was invalid or already used");
        }
        RegionSnapshot target = store.load(confirmed.preview().snapshotId().value())
                .orElseThrow(() -> new IOException("target snapshot disappeared"));
        if (!previews.stillCurrent(level, target, confirmed.preview(), now)) {
            return new TransactionReport(transactionId, TransactionOutcome.STALE_PREVIEW, 0,
                    "world state changed after preview");
        }
        RegionSelection selection = target.selection();
        if (!locks.acquire(transactionId, selection)) {
            return new TransactionReport(transactionId, TransactionOutcome.REJECTED, 0,
                    "another transaction overlaps this region");
        }
        long sequence = 0L;
        AffectedScope scope = scope(selection);
        RegionSnapshot before = null;
        Map<UUID, Vec3> relocationPlan = Map.of();
        boolean prepared = false;
        boolean committed = false;
        try {
            append(transactionId, sequence++, now, TransactionState.PREPARING,
                    confirmed.preview().previewDigest(), scope);
            before = snapshots.captureInternal(level, actorId,
                    "u_" + transactionId.toString().replace("-", ""), selection, now);
            manifests.publish(new TransactionManifest(transactionId, actorId, target.id().value(),
                    before.id().value(), selection, target.contentDigest(), before.contentDigest(),
                    confirmed.preview().previewDigest(), now));
            relocationPlan = PlayerRelocator.plan(level, selection);
            append(transactionId, sequence++, now, TransactionState.PREPARED, before.contentDigest(), scope);
            prepared = true;
            append(transactionId, sequence++, now, TransactionState.APPLYING, target.contentDigest(), scope);
            PlayerRelocator.apply(level, relocationPlan);
            RegionRestorer.apply(level, target);
            append(transactionId, sequence++, now, TransactionState.VERIFYING, target.contentDigest(), scope);
            String actual = SnapshotService.digest(SnapshotService.captureState(level, selection));
            if (!target.contentDigest().equals(actual)) {
                append(transactionId, sequence++, now, TransactionState.ROLLING_BACK, before.contentDigest(), scope);
                RegionRestorer.apply(level, before);
                String rolledBack = SnapshotService.digest(SnapshotService.captureState(level, selection));
                if (!before.contentDigest().equals(rolledBack)) {
                    append(transactionId, sequence, now, TransactionState.FAILED_SAFE, rolledBack, scope);
                    return new TransactionReport(transactionId, TransactionOutcome.FAILED_SAFE, 0,
                            "verification and rollback both failed");
                }
                append(transactionId, sequence, now, TransactionState.ROLLED_BACK, rolledBack, scope);
                store.remove(before.id().value());
                manifests.remove(transactionId);
                return new TransactionReport(transactionId, TransactionOutcome.ROLLED_BACK, 0,
                        "verification mismatch; prior state restored");
            }
            append(transactionId, sequence, now, TransactionState.COMMITTED, actual, scope);
            committed = true;
            try {
                ledger.append(new CausalLedgerEntry(transactionId, actorId, target.id().value(),
                        before.id().value(), selection.dimension(), now, TransactionOutcome.COMPLETED));
                retireOverlappingUndo(transactionId, actorId, selection, now);
            } catch (IOException ledgerFailure) {
                return new TransactionReport(transactionId, TransactionOutcome.COMPLETED_WITH_WARNINGS,
                        confirmed.preview().changedBlocks(),
                        "region restored, but the causal index needs recovery: " + ledgerFailure.getMessage());
            }
            return new TransactionReport(transactionId, TransactionOutcome.COMPLETED,
                    confirmed.preview().changedBlocks(), "region restored and verified");
        } catch (IOException | RuntimeException failure) {
            if (committed) {
                return new TransactionReport(transactionId, TransactionOutcome.COMPLETED_WITH_WARNINGS,
                        confirmed.preview().changedBlocks(),
                        "region committed; post-commit reporting failed: " + failure.getMessage());
            }
            if (before != null) {
                if (!prepared) {
                    store.remove(before.id().value());
                    manifests.remove(transactionId);
                    return new TransactionReport(transactionId, TransactionOutcome.REJECTED, 0,
                            "operation was rejected before mutation: " + failure.getMessage());
                }
                try {
                    append(transactionId, sequence++, now, TransactionState.ROLLING_BACK,
                            before.contentDigest(), scope);
                    RegionRestorer.apply(level, before);
                    String rolledBack = SnapshotService.digest(SnapshotService.captureState(level, selection));
                    if (before.contentDigest().equals(rolledBack)) {
                        append(transactionId, sequence, now, TransactionState.ROLLED_BACK, rolledBack, scope);
                        store.remove(before.id().value());
                        manifests.remove(transactionId);
                        return new TransactionReport(transactionId, TransactionOutcome.ROLLED_BACK, 0,
                                "operation failed; prior state restored: " + failure.getMessage());
                    }
                } catch (IOException | RuntimeException rollbackFailure) {
                    failure.addSuppressed(rollbackFailure);
                }
                append(transactionId, sequence, now, TransactionState.FAILED_SAFE,
                        before.contentDigest(), scope);
            }
            throw failure;
        } finally {
            locks.release(transactionId);
        }
    }

    public TransactionReport undo(ServerLevel level, UUID actorId, UUID committedTransactionId,
                                  Instant now) throws IOException {
        TransactionManifest originalManifest = manifests.load(committedTransactionId)
                .orElseThrow(() -> new IllegalArgumentException("retained undo is unavailable"));
        if (originalManifest.createdAt().plus(UNDO_RETENTION).isBefore(now)) {
            retire(committedTransactionId, now);
            throw new IllegalArgumentException("retained undo has expired");
        }
        CausalLedgerEntry entry = ledger.find(committedTransactionId)
                .orElseThrow(() -> new IllegalArgumentException("committed transaction not found"));
        if (!entry.actorId().equals(actorId)) {
            throw new IllegalArgumentException("transaction belongs to another player");
        }
        RegionSnapshot before = store.load(entry.beforeImageId())
                .orElseThrow(() -> new IOException("retained undo image is unavailable"));
        RegionSelection selection = before.selection();
        UUID undoId = UUID.randomUUID();
        if (!locks.acquire(undoId, selection)) {
            return new TransactionReport(undoId, TransactionOutcome.REJECTED, 0,
                    "another transaction overlaps this region");
        }
        try {
            RegionSnapshot current = snapshots.captureInternal(level, actorId,
                    "u_" + undoId.toString().replace("-", ""), selection, now);
            AffectedScope scope = scope(selection);
            append(undoId, 0, now, TransactionState.PREPARING, before.contentDigest(), scope);
            manifests.publish(new TransactionManifest(undoId, actorId, before.id().value(),
                    current.id().value(), selection, before.contentDigest(), current.contentDigest(),
                    before.contentDigest(), now));
            append(undoId, 1, now, TransactionState.PREPARED, current.contentDigest(), scope);
            PlayerRelocator.apply(level, PlayerRelocator.plan(level, selection));
            append(undoId, 2, now, TransactionState.APPLYING, before.contentDigest(), scope);
            RegionRestorer.apply(level, before);
            append(undoId, 3, now, TransactionState.VERIFYING, before.contentDigest(), scope);
            String actual = SnapshotService.digest(SnapshotService.captureState(level, selection));
            if (!actual.equals(before.contentDigest())) {
                append(undoId, 4, now, TransactionState.ROLLING_BACK, current.contentDigest(), scope);
                RegionRestorer.apply(level, current);
                String rollback = SnapshotService.digest(SnapshotService.captureState(level, selection));
                if (!rollback.equals(current.contentDigest())) {
                    append(undoId, 5, now, TransactionState.FAILED_SAFE, rollback, scope);
                    return new TransactionReport(undoId, TransactionOutcome.FAILED_SAFE, 0,
                            "undo and rollback verification failed");
                }
                append(undoId, 5, now, TransactionState.ROLLED_BACK, rollback, scope);
                return new TransactionReport(undoId, TransactionOutcome.ROLLED_BACK, 0,
                        "undo verification failed; current state restored");
            }
            append(undoId, 4, now, TransactionState.COMMITTED, actual, scope);
            try {
                ledger.append(new CausalLedgerEntry(undoId, actorId, before.id().value(),
                        current.id().value(), selection.dimension(), now, TransactionOutcome.COMPLETED));
                retire(committedTransactionId, now);
                retireOverlappingUndo(undoId, actorId, selection, now);
                return new TransactionReport(undoId, TransactionOutcome.COMPLETED,
                        (int) Math.min(Integer.MAX_VALUE, selection.volume()),
                        "retained undo applied and verified");
            } catch (IOException postCommitFailure) {
                return new TransactionReport(undoId, TransactionOutcome.COMPLETED_WITH_WARNINGS,
                        (int) Math.min(Integer.MAX_VALUE, selection.volume()),
                        "undo committed; retention cleanup needs recovery: " + postCommitFailure.getMessage());
            }
        } finally {
            locks.release(undoId);
        }
    }

    /** Выполняет уже построенный сервером target через тот же prepare/apply/verify/rollback протокол. */
    public TransactionReport applyPreparedTarget(ServerLevel level, UUID actorId,
                                                 RegionSnapshot current, RegionSnapshot target,
                                                 String previewDigest, Instant now) throws IOException {
        if (!current.selection().equals(target.selection())
                || !current.ownerId().equals(actorId) || !target.ownerId().equals(actorId)) {
            throw new IllegalArgumentException("prepared target does not match actor or selection");
        }
        String actualBefore = SnapshotService.digest(SnapshotService.captureState(level, current.selection()));
        if (!actualBefore.equals(current.contentDigest())) {
            return new TransactionReport(UUID.randomUUID(), TransactionOutcome.STALE_PREVIEW, 0,
                    "world state changed after preview");
        }
        UUID transactionId = UUID.randomUUID();
        RegionSelection selection = current.selection();
        if (!locks.acquire(transactionId, selection)) {
            return new TransactionReport(transactionId, TransactionOutcome.REJECTED, 0,
                    "another transaction overlaps this region");
        }
        AffectedScope scope = scope(selection);
        String beforeName = "u_" + transactionId.toString().replace("-", "");
        RegionSnapshot before = new RegionSnapshot(RegionSnapshot.CURRENT_SCHEMA_VERSION,
                new dev.romankrukovsky.kubanhorizons.genie.runtime.snapshot.SnapshotId(
                        UUID.randomUUID(), beforeName), actorId, now, selection,
                current.blocks(), current.blockTicks(), current.fluidTicks(), current.entities(),
                current.biomes(), current.contentDigest());
        RegionSnapshot durableTarget = new RegionSnapshot(RegionSnapshot.CURRENT_SCHEMA_VERSION,
                new dev.romankrukovsky.kubanhorizons.genie.runtime.snapshot.SnapshotId(
                        UUID.randomUUID(), "t_" + transactionId.toString().replace("-", "")),
                actorId, now, selection, target.blocks(), target.blockTicks(),
                target.fluidTicks(), target.entities(), target.biomes(), target.contentDigest());
        long sequence = 0L;
        boolean prepared = false;
        boolean committed = false;
        try {
            append(transactionId, sequence++, now, TransactionState.PREPARING, previewDigest, scope);
            store.publish(before);
            store.publish(durableTarget);
            manifests.publish(new TransactionManifest(transactionId, actorId, durableTarget.id().value(),
                    before.id().value(), selection, durableTarget.contentDigest(), before.contentDigest(),
                    previewDigest, now));
            append(transactionId, sequence++, now, TransactionState.PREPARED, before.contentDigest(), scope);
            prepared = true;
            PlayerRelocator.apply(level, PlayerRelocator.plan(level, selection));
            append(transactionId, sequence++, now, TransactionState.APPLYING, durableTarget.contentDigest(), scope);
            RegionRestorer.apply(level, durableTarget);
            append(transactionId, sequence++, now, TransactionState.VERIFYING, durableTarget.contentDigest(), scope);
            String actual = SnapshotService.digest(SnapshotService.captureState(level, selection));
            if (!actual.equals(durableTarget.contentDigest())) {
                logFirstMismatch(level, durableTarget);
                append(transactionId, sequence++, now, TransactionState.ROLLING_BACK, before.contentDigest(), scope);
                RegionRestorer.apply(level, before);
                String rollback = SnapshotService.digest(SnapshotService.captureState(level, selection));
                if (!rollback.equals(before.contentDigest())) {
                    append(transactionId, sequence, now, TransactionState.FAILED_SAFE, rollback, scope);
                    return new TransactionReport(transactionId, TransactionOutcome.FAILED_SAFE, 0,
                            "prepared target and rollback verification failed");
                }
                append(transactionId, sequence, now, TransactionState.ROLLED_BACK, rollback, scope);
                store.remove(before.id().value());
                store.remove(durableTarget.id().value());
                manifests.remove(transactionId);
                return new TransactionReport(transactionId, TransactionOutcome.ROLLED_BACK, 0,
                        "prepared target failed verification; prior state restored; expected="
                                + durableTarget.contentDigest() + ", actual=" + actual);
            }
            append(transactionId, sequence, now, TransactionState.COMMITTED, actual, scope);
            committed = true;
            try {
                ledger.append(new CausalLedgerEntry(transactionId, actorId, durableTarget.id().value(),
                        before.id().value(), selection.dimension(), now, TransactionOutcome.COMPLETED));
                retireOverlappingUndo(transactionId, actorId, selection, now);
            } catch (IOException ledgerFailure) {
                return new TransactionReport(transactionId, TransactionOutcome.COMPLETED_WITH_WARNINGS,
                        (int) Math.min(Integer.MAX_VALUE, selection.volume()),
                        "prepared target committed; causal index needs recovery: " + ledgerFailure.getMessage());
            }
            return new TransactionReport(transactionId, TransactionOutcome.COMPLETED,
                    (int) Math.min(Integer.MAX_VALUE, selection.volume()),
                    "prepared target applied and verified");
        } catch (IOException | RuntimeException failure) {
            if (committed) {
                return new TransactionReport(transactionId, TransactionOutcome.COMPLETED_WITH_WARNINGS,
                        (int) Math.min(Integer.MAX_VALUE, selection.volume()),
                        "prepared target committed; post-commit reporting failed: " + failure.getMessage());
            }
            if (prepared) {
                try {
                    append(transactionId, sequence++, now, TransactionState.ROLLING_BACK,
                            before.contentDigest(), scope);
                    RegionRestorer.apply(level, before);
                    String rollback = SnapshotService.digest(SnapshotService.captureState(level, selection));
                    if (rollback.equals(before.contentDigest())) {
                append(transactionId, sequence, now, TransactionState.ROLLED_BACK, rollback, scope);
                store.remove(before.id().value());
                store.remove(durableTarget.id().value());
                manifests.remove(transactionId);
                return new TransactionReport(transactionId, TransactionOutcome.ROLLED_BACK, 0,
                        "prepared target failed; prior state restored: " + failure.getMessage());
                    }
                } catch (IOException | RuntimeException rollbackFailure) {
                    failure.addSuppressed(rollbackFailure);
                }
                append(transactionId, sequence, now, TransactionState.FAILED_SAFE,
                        before.contentDigest(), scope);
            } else {
                store.remove(before.id().value());
                store.remove(durableTarget.id().value());
                manifests.remove(transactionId);
            }
            throw failure;
        } finally {
            locks.release(transactionId);
        }
    }

    private void append(UUID id, long sequence, Instant now, TransactionState state,
                        String digest, AffectedScope scope) throws IOException {
        journal.append(new RecoveryRecord(id, sequence, now, state, digest, scope));
    }

    public java.util.List<UndoSummary> availableUndo(UUID actorId, Instant now) throws IOException {
        java.util.List<UndoSummary> result = new java.util.ArrayList<>();
        for (TransactionManifest manifest : manifests.list()) {
            if (!manifest.actorId().equals(actorId) || manifest.createdAt().plus(UNDO_RETENTION).isBefore(now)
                    || store.load(manifest.beforeImageId()).isEmpty()) {
                continue;
            }
            result.add(new UndoSummary(manifest.transactionId(), manifest.selection(),
                    manifest.createdAt(), manifest.createdAt().plus(UNDO_RETENTION)));
        }
        result.sort(java.util.Comparator.comparing(UndoSummary::committedAt).reversed());
        return java.util.List.copyOf(result);
    }

    public void cleanupExpiredUndo(Instant now) throws IOException {
        for (TransactionManifest manifest : manifests.list()) {
            if (manifest.createdAt().plus(UNDO_RETENTION).isBefore(now)) {
                retire(manifest.transactionId(), now);
            }
        }
    }

    public void retireUndo(UUID transactionId, UUID actorId, Instant now) throws IOException {
        TransactionManifest manifest = manifests.load(transactionId)
                .orElseThrow(() -> new IllegalArgumentException("retained undo is unavailable"));
        if (!manifest.actorId().equals(actorId)) {
            throw new IllegalArgumentException("transaction belongs to another player");
        }
        retire(transactionId, now);
    }

    private void retireOverlappingUndo(UUID except, UUID actorId, RegionSelection selection,
                                       Instant now) throws IOException {
        for (TransactionManifest manifest : manifests.list()) {
            if (!manifest.transactionId().equals(except) && manifest.actorId().equals(actorId)
                    && overlaps(manifest.selection(), selection)) {
                retire(manifest.transactionId(), now);
            }
        }
    }

    private void retire(UUID transactionId, Instant now) throws IOException {
        TransactionManifest manifest = manifests.load(transactionId).orElse(null);
        if (manifest == null) {
            return;
        }
        var scan = journal.scan();
        RecoveryRecord last = scan.provenRecords().stream()
                .filter(record -> record.transactionId().equals(transactionId))
                .reduce((left, right) -> right).orElse(null);
        if (last == null || last.state() != TransactionState.COMMITTED) {
            return;
        }
        journal.append(new RecoveryRecord(transactionId, last.sequence() + 1L, now,
                TransactionState.RETIRED, manifest.beforeDigest(), scope(manifest.selection())));
        store.remove(manifest.beforeImageId());
        RegionSnapshot target = store.load(manifest.targetSnapshotId()).orElse(null);
        if (target != null && target.id().name().startsWith("t_")) {
            store.remove(target.id().value());
        }
        manifests.remove(transactionId);
    }

    private static boolean overlaps(RegionSelection left, RegionSelection right) {
        return left.dimension().equals(right.dimension())
                && left.min().getX() <= right.max().getX() && right.min().getX() <= left.max().getX()
                && left.min().getY() <= right.max().getY() && right.min().getY() <= left.max().getY()
                && left.min().getZ() <= right.max().getZ() && right.min().getZ() <= left.max().getZ();
    }

    private static void logFirstMismatch(ServerLevel level, RegionSnapshot expected) {
        java.util.List<RegionSnapshot.BlockRecord> actual = SnapshotService.captureBlocks(
                level, expected.selection());
        for (int index = 0; index < expected.blocks().size(); index++) {
            RegionSnapshot.BlockRecord left = expected.blocks().get(index);
            RegionSnapshot.BlockRecord right = actual.get(index);
            if (!left.blockState().equals(right.blockState())
                    || !Objects.equals(left.blockEntity(), right.blockEntity())) {
                dev.romankrukovsky.kubanhorizons.KubanHorizons.LOGGER.error(
                        "Prepared target mismatch at relative {},{},{}: expected state {}, actual state {}",
                        left.relativeX(), left.relativeY(), left.relativeZ(),
                        left.blockState(), right.blockState());
                return;
            }
        }
        dev.romankrukovsky.kubanhorizons.KubanHorizons.LOGGER.error(
                "Prepared target block domain matched; expected ticks {}/{}, entities {}, biomes {}; actual ticks {}/{}, entities {}, biomes {}",
                expected.blockTicks().size(), expected.fluidTicks().size(), expected.entities().size(),
                expected.biomes().size(),
                SnapshotService.captureState(level, expected.selection()).blockTicks().size(),
                SnapshotService.captureState(level, expected.selection()).fluidTicks().size(),
                SnapshotService.captureState(level, expected.selection()).entities().size(),
                SnapshotService.captureState(level, expected.selection()).biomes().size());
        var actualState = SnapshotService.captureState(level, expected.selection());
        for (int index = 0; index < Math.min(expected.biomes().size(), actualState.biomes().size()); index++) {
            if (!expected.biomes().get(index).equals(actualState.biomes().get(index))) {
                dev.romankrukovsky.kubanhorizons.KubanHorizons.LOGGER.error(
                        "First biome mismatch: expected {}, actual {}",
                        expected.biomes().get(index), actualState.biomes().get(index));
                return;
            }
        }
    }

    public record UndoSummary(UUID transactionId, RegionSelection selection,
                              Instant committedAt, Instant expiresAt) {
    }

    private static AffectedScope scope(RegionSelection selection) {
        ChunkPos min = ChunkPos.containing(selection.min());
        ChunkPos max = ChunkPos.containing(selection.max());
        return new AffectedScope(selection.dimension(), min.x(), min.z(), max.x(), max.z());
    }
}
