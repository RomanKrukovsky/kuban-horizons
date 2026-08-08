package dev.romankrukovsky.kubanhorizons.genie.runtime.transaction;

import dev.romankrukovsky.kubanhorizons.genie.runtime.recovery.AffectedScope;
import dev.romankrukovsky.kubanhorizons.genie.runtime.recovery.RecoveryJournal;
import dev.romankrukovsky.kubanhorizons.genie.runtime.recovery.RecoveryRecord;
import dev.romankrukovsky.kubanhorizons.genie.runtime.recovery.TransactionState;
import dev.romankrukovsky.kubanhorizons.genie.runtime.snapshot.RegionSnapshot;
import dev.romankrukovsky.kubanhorizons.genie.runtime.snapshot.SnapshotService;
import dev.romankrukovsky.kubanhorizons.genie.runtime.snapshot.SnapshotStore;
import java.io.IOException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;

/** На старте завершает прерванный rollback до допуска новых сильных желаний. */
public final class RecoveryService {
    private final RecoveryJournal journal;
    private final SnapshotStore snapshots;
    private final TransactionManifestStore manifests;

    public RecoveryService(RecoveryJournal journal, SnapshotStore snapshots,
                           TransactionManifestStore manifests) {
        this.journal = Objects.requireNonNull(journal, "journal");
        this.snapshots = Objects.requireNonNull(snapshots, "snapshots");
        this.manifests = Objects.requireNonNull(manifests, "manifests");
    }

    public void recover(MinecraftServer server) throws IOException {
        var scan = journal.scan();
        if (scan.discardedInvalidTail()) {
            throw new IOException("recovery journal has an invalid tail; strong wishes remain blocked");
        }
        Map<UUID, RecoveryRecord> lastByTransaction = new LinkedHashMap<>();
        for (RecoveryRecord record : scan.provenRecords()) {
            lastByTransaction.put(record.transactionId(), record);
        }
        for (Map.Entry<UUID, RecoveryRecord> entry : lastByTransaction.entrySet()) {
            RecoveryRecord last = entry.getValue();
            switch (last.state()) {
                case PREPARING -> cleanUnprepared(entry.getKey());
                case PREPARED, APPLYING, VERIFYING, ROLLING_BACK -> rollback(server, last);
                case COMMITTED -> verifyCommitted(server, last);
                case ROLLED_BACK -> verifyRolledBack(server, last);
                case RETIRED -> cleanRetired(entry.getKey());
                case FAILED_SAFE -> throw new IOException("transaction " + entry.getKey() + " is failed-safe");
                default -> throw new IllegalStateException("Unhandled transaction state " + last.state());
            }
        }
    }

    private void cleanUnprepared(UUID transactionId) throws IOException {
        var manifest = manifests.load(transactionId);
        if (manifest.isPresent()) {
            throw new IOException("PREPARING transaction unexpectedly has a recovery manifest: " + transactionId);
        }
    }

    private void rollback(MinecraftServer server, RecoveryRecord last) throws IOException {
        TransactionManifest manifest = manifests.load(last.transactionId())
                .orElseThrow(() -> new IOException("missing recovery manifest for " + last.transactionId()));
        RegionSnapshot before = snapshots.load(manifest.beforeImageId())
                .orElseThrow(() -> new IOException("missing before-image for " + last.transactionId()));
        ServerLevel level = resolveLevel(server, manifest.selection().dimension());
        long sequence = last.sequence() + 1L;
        AffectedScope scope = scope(manifest);
        if (last.state() != TransactionState.ROLLING_BACK) {
            journal.append(new RecoveryRecord(last.transactionId(), sequence++, Instant.now(),
                    TransactionState.ROLLING_BACK, manifest.beforeDigest(), scope));
        }
        RegionRestorer.apply(level, before);
        String actual = SnapshotService.digest(SnapshotService.captureState(level, manifest.selection()));
        if (!actual.equals(manifest.beforeDigest())) {
            journal.append(new RecoveryRecord(last.transactionId(), sequence, Instant.now(),
                    TransactionState.FAILED_SAFE, actual, scope));
            throw new IOException("rollback verification failed for " + last.transactionId());
        }
        journal.append(new RecoveryRecord(last.transactionId(), sequence, Instant.now(),
                TransactionState.ROLLED_BACK, actual, scope));
        snapshots.remove(manifest.beforeImageId());
        snapshots.remove(manifest.targetSnapshotId());
        manifests.remove(last.transactionId());
    }

    private void verifyCommitted(MinecraftServer server, RecoveryRecord last) throws IOException {
        TransactionManifest manifest = manifests.load(last.transactionId())
                .orElseThrow(() -> new IOException("missing committed manifest"));
        RegionSnapshot target = snapshots.load(manifest.targetSnapshotId())
                .orElseThrow(() -> new IOException("missing target snapshot for committed transaction"));
        RegionSnapshot before = snapshots.load(manifest.beforeImageId())
                .orElseThrow(() -> new IOException("missing retained undo for committed transaction"));
        if (!target.contentDigest().equals(manifest.targetDigest())
                || !before.contentDigest().equals(manifest.beforeDigest())) {
            throw new IOException("committed transaction artifacts do not match its manifest");
        }
    }

    private void verifyRolledBack(MinecraftServer server, RecoveryRecord last) throws IOException {
        var manifest = manifests.load(last.transactionId());
        if (manifest.isEmpty()) {
            return;
        }
        ServerLevel level = resolveLevel(server, manifest.get().selection().dimension());
        String actual = SnapshotService.digest(SnapshotService.captureState(level, manifest.get().selection()));
        if (!actual.equals(manifest.get().beforeDigest())) {
            throw new IOException("rolled-back transaction no longer matches before-image");
        }
        snapshots.remove(manifest.get().beforeImageId());
        snapshots.remove(manifest.get().targetSnapshotId());
        manifests.remove(last.transactionId());
    }

    private void cleanRetired(UUID transactionId) throws IOException {
        var manifest = manifests.load(transactionId);
        if (manifest.isEmpty()) {
            return;
        }
        snapshots.remove(manifest.get().beforeImageId());
        var target = snapshots.load(manifest.get().targetSnapshotId());
        if (target.isPresent() && target.get().id().name().startsWith("t_")) {
            snapshots.remove(target.get().id().value());
        }
        manifests.remove(transactionId);
    }

    private static ServerLevel resolveLevel(MinecraftServer server, String dimension) throws IOException {
        for (ServerLevel level : server.getAllLevels()) {
            if (level.dimension().identifier().toString().equals(dimension)) {
                return level;
            }
        }
        throw new IOException("dimension is unavailable: " + dimension);
    }

    private static AffectedScope scope(TransactionManifest manifest) {
        ChunkPos min = ChunkPos.containing(manifest.selection().min());
        ChunkPos max = ChunkPos.containing(manifest.selection().max());
        return new AffectedScope(manifest.selection().dimension(), min.x(), min.z(), max.x(), max.z());
    }
}
