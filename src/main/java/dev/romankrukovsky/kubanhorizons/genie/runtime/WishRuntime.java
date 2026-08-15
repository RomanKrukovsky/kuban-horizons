package dev.romankrukovsky.kubanhorizons.genie.runtime;

import dev.romankrukovsky.kubanhorizons.KubanHorizons;
import dev.romankrukovsky.kubanhorizons.genie.runtime.capability.CapabilityRegistry;
import dev.romankrukovsky.kubanhorizons.genie.runtime.confirmation.ConfirmationAuthority;
import dev.romankrukovsky.kubanhorizons.genie.runtime.confirmation.ConfirmedRestore;
import dev.romankrukovsky.kubanhorizons.genie.runtime.confirmation.ConfirmedMiniaturize;
import dev.romankrukovsky.kubanhorizons.genie.runtime.confirmation.ConfirmedPocketScene;
import dev.romankrukovsky.kubanhorizons.genie.runtime.confirmation.ConfirmedStructureMove;
import dev.romankrukovsky.kubanhorizons.genie.runtime.confirmation.ConfirmedStructureRotate;
import dev.romankrukovsky.kubanhorizons.genie.runtime.confirmation.ConfirmedBiomeRewrite;
import dev.romankrukovsky.kubanhorizons.genie.runtime.confirmation.ConfirmedDrawing;
import dev.romankrukovsky.kubanhorizons.genie.runtime.confirmation.ConfirmedWord;
import dev.romankrukovsky.kubanhorizons.genie.runtime.confirmation.ConfirmedPolicy;
import dev.romankrukovsky.kubanhorizons.genie.runtime.plan.PlanGate;
import dev.romankrukovsky.kubanhorizons.genie.runtime.preview.PreviewService;
import dev.romankrukovsky.kubanhorizons.genie.runtime.preview.RestorePreview;
import dev.romankrukovsky.kubanhorizons.genie.runtime.preview.MiniaturizePreview;
import dev.romankrukovsky.kubanhorizons.genie.runtime.preview.PocketScenePreview;
import dev.romankrukovsky.kubanhorizons.genie.runtime.preview.StructureMovePreview;
import dev.romankrukovsky.kubanhorizons.genie.runtime.preview.StructureRotatePreview;
import dev.romankrukovsky.kubanhorizons.genie.runtime.preview.BiomeRewritePreview;
import dev.romankrukovsky.kubanhorizons.genie.runtime.preview.DrawingPreview;
import dev.romankrukovsky.kubanhorizons.genie.runtime.preview.WordPreview;
import dev.romankrukovsky.kubanhorizons.genie.runtime.preview.PolicyPreview;
import dev.romankrukovsky.kubanhorizons.genie.runtime.policy.PolicyManifestStore;
import dev.romankrukovsky.kubanhorizons.genie.runtime.policy.PolicyService;
import dev.romankrukovsky.kubanhorizons.genie.runtime.recovery.RecoveryClassifier;
import dev.romankrukovsky.kubanhorizons.genie.runtime.recovery.RecoveryJournal;
import dev.romankrukovsky.kubanhorizons.genie.runtime.selection.RegionSelection;
import dev.romankrukovsky.kubanhorizons.genie.runtime.selection.SelectionService;
import dev.romankrukovsky.kubanhorizons.genie.runtime.snapshot.RegionSnapshot;
import dev.romankrukovsky.kubanhorizons.genie.runtime.snapshot.SnapshotService;
import dev.romankrukovsky.kubanhorizons.genie.runtime.snapshot.SnapshotStore;
import dev.romankrukovsky.kubanhorizons.genie.runtime.transform.RegionRotateService;
import dev.romankrukovsky.kubanhorizons.genie.runtime.transaction.CausalLedger;
import dev.romankrukovsky.kubanhorizons.genie.runtime.transaction.CausalLedgerEntry;
import dev.romankrukovsky.kubanhorizons.genie.runtime.transaction.RecoveryService;
import dev.romankrukovsky.kubanhorizons.genie.runtime.transaction.RegionLockManager;
import dev.romankrukovsky.kubanhorizons.genie.runtime.transaction.RestoreTransactionService;
import dev.romankrukovsky.kubanhorizons.genie.runtime.transaction.TransactionManifestStore;
import dev.romankrukovsky.kubanhorizons.genie.runtime.transaction.TransactionReport;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.time.Duration;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.item.ItemStack;

/** Серверный фасад безопасных сильных желаний для одного мира. */
public final class WishRuntime {
    private static final String ROOT = "kubanhorizons/genie_runtime";
    private static final Map<MinecraftServer, WishRuntime> INSTANCES = new ConcurrentHashMap<>();

    private final CapabilityRegistry capabilities = new CapabilityRegistry();
    private final PlanGate planGate = new PlanGate(capabilities);
    private final SnapshotStore snapshotStore;
    private final SelectionService selections = new SelectionService();
    private final SnapshotService snapshotService;
    private final PreviewService previewService = new PreviewService();
    private final ConfirmationAuthority confirmations = new ConfirmationAuthority();
    private final RecoveryJournal recoveryJournal;
    private final TransactionManifestStore manifests;
    private final CausalLedger ledger;
    private final RecoveryService recoveryService;
    private final RestoreTransactionService restoreService;
    private final PolicyService policyService;
    private final Set<UUID> issuedMiniaturizeConfirmations = new HashSet<>();
    private final Set<UUID> issuedPocketSceneConfirmations = new HashSet<>();
    private final Set<UUID> issuedStructureMoveConfirmations = new HashSet<>();
    private final Set<UUID> issuedStructureRotateConfirmations = new HashSet<>();
    private final Set<UUID> issuedBiomeRewriteConfirmations = new HashSet<>();
    private final Set<UUID> issuedDrawingConfirmations = new HashSet<>();
    private final Set<UUID> issuedWordConfirmations = new HashSet<>();
    private final Map<UUID, dev.romankrukovsky.kubanhorizons.genie.dimension.FlyingStructureEngine.MovePlan>
            pendingStructureMoves = new ConcurrentHashMap<>();
    private final Map<UUID, RegionRotateService.RotatePlan>
            pendingStructureRotates = new ConcurrentHashMap<>();
    private final Map<UUID, dev.romankrukovsky.kubanhorizons.genie.expression.BiomeRewriterEngine.BiomePlan>
            pendingBiomeRewrites = new ConcurrentHashMap<>();
    private final Map<UUID, dev.romankrukovsky.kubanhorizons.genie.expression.MagicDrawingHandler.DrawingPlan>
            pendingDrawings = new ConcurrentHashMap<>();
    private final Map<UUID, dev.romankrukovsky.kubanhorizons.genie.expression.WordMaterializer.WordPlan>
            pendingWords = new ConcurrentHashMap<>();
    private final MinecraftServer server;
    private volatile boolean ready;
    private volatile String blockedReason = "startup recovery has not run";

    private WishRuntime(MinecraftServer server) {
        this.server = server;
        Path root = server.getWorldPath(LevelResource.ROOT).resolve(ROOT);
        snapshotStore = new SnapshotStore(root.resolve("snapshots"));
        snapshotService = new SnapshotService(snapshotStore);
        recoveryJournal = new RecoveryJournal(root.resolve("recovery.journal"));
        manifests = new TransactionManifestStore(root.resolve("transactions"));
        this.ledger = new CausalLedger(root.resolve("causal-ledger.tsv"));
        RegionLockManager locks = new RegionLockManager();
        recoveryService = new RecoveryService(recoveryJournal, snapshotStore, manifests);
        restoreService = new RestoreTransactionService(confirmations, previewService, snapshotStore,
                snapshotService, recoveryJournal, ledger, locks, manifests);
        policyService = new PolicyService(new PolicyManifestStore(root.resolve("policies")));
    }

    public static WishRuntime get(MinecraftServer server) {
        return INSTANCES.computeIfAbsent(Objects.requireNonNull(server, "server"), WishRuntime::new);
    }

    public static void remove(MinecraftServer server) {
        INSTANCES.remove(server);
    }

    public void recover() {
        try {
            recoveryService.recover(server());
            restoreService.cleanupExpiredUndo(Instant.now());
            policyService.recover(server());
            ready = true;
            blockedReason = "";
        } catch (IOException | RuntimeException exception) {
            ready = false;
            blockedReason = exception.getMessage();
            KubanHorizons.LOGGER.error("Strong-wish runtime entered failed-safe mode", exception);
        }
    }

    public RegionSnapshot createSnapshot(ServerLevel level, UUID actor, String name,
                                         RegionSelection selection) throws IOException {
        ensureReady(selection);
        requireServerThread();
        return snapshotService.capture(level, actor, name, selection, Instant.now());
    }

    public RegionSnapshot createSelectedSnapshot(ServerLevel level, UUID actor,
                                                 String name) throws IOException {
        RegionSelection selection = selections.requireCompleted(actor);
        RegionSnapshot snapshot = createSnapshot(level, actor, name, selection);
        selections.clear(actor);
        return snapshot;
    }

    public SelectionService.SelectionUpdate select(ServerPlayer player, BlockPos pos) {
        if (!ready) {
            throw new IllegalStateException("strong wishes are blocked: " + blockedReason);
        }
        return selections.select(player, pos);
    }

    public void setSelection(UUID actor, RegionSelection selection) {
        selections.setCompleted(actor, selection);
    }

    public MiniaturizePreview previewMiniaturizeSelected(ServerPlayer player) throws IOException {
        requireServerThread();
        RegionSelection selection = selections.requireCompleted(player.getUUID());
        ensureReady(selection);
        var snapshot = dev.romankrukovsky.kubanhorizons.genie.spatial.MiniaturizationEngine
                .captureSelection(player.level(), selection, player.getUUID());
        int nonAir = (int) snapshot.blocks().stream().filter(record ->
                !record.blockState().getStringOr("Name", "minecraft:air").equals("minecraft:air")).count();
        int blockEntities = (int) snapshot.blocks().stream()
                .filter(record -> record.blockEntity() != null).count();
        var risk = selection.chunkCount() > 16 || nonAir > 4_096
                ? dev.romankrukovsky.kubanhorizons.genie.runtime.capability.CapabilityRisk.HIGH
                : dev.romankrukovsky.kubanhorizons.genie.runtime.capability.CapabilityRisk.ELEVATED;
        UUID previewId = UUID.randomUUID();
        Instant expiresAt = Instant.now().plus(Duration.ofMinutes(2));
        String digest = digest(previewId + "|" + player.getUUID() + "|" + snapshot.contentDigest()
                + "|" + selection.dimension() + "|" + selection.min().asLong() + "|"
                + selection.max().asLong() + "|" + nonAir + "|" + blockEntities + "|"
                + snapshot.entities().size() + "|" + risk + "|" + expiresAt);
        return new MiniaturizePreview(previewId, player.getUUID(), selection, nonAir,
                blockEntities, snapshot.entities().size(), risk, snapshot.contentDigest(), digest, expiresAt);
    }

    public synchronized ConfirmedMiniaturize confirmMiniaturize(UUID actor,
                                                                MiniaturizePreview preview) {
        Instant now = Instant.now();
        if (!preview.actorId().equals(actor) || !preview.expiresAt().isAfter(now)) {
            throw new IllegalArgumentException("miniaturization preview is stale or belongs to another actor");
        }
        UUID id = UUID.randomUUID();
        issuedMiniaturizeConfirmations.add(id);
        return new ConfirmedMiniaturize(id, preview, now);
    }

    public ItemStack executeMiniaturize(ServerPlayer player, ConfirmedMiniaturize confirmed) throws IOException {
        requireServerThread();
        Instant now = Instant.now();
        synchronized (this) {
            if (!confirmed.preview().actorId().equals(player.getUUID())
                    || !confirmed.preview().expiresAt().isAfter(now)
                    || !issuedMiniaturizeConfirmations.remove(confirmed.confirmationId())) {
                throw new IllegalArgumentException("miniaturization confirmation is invalid or already used");
            }
        }
        RegionSelection selection = confirmed.preview().selection();
        ensureReady(selection);
        RegionSnapshot source = dev.romankrukovsky.kubanhorizons.genie.spatial.MiniaturizationEngine
                .captureSelection(player.level(), selection, player.getUUID());
        if (!source.contentDigest().equals(confirmed.preview().currentStateDigest())) {
            throw new IllegalStateException("world state changed after miniaturization preview");
        }
        RegionSnapshot empty = dev.romankrukovsky.kubanhorizons.genie.spatial.MiniaturizationEngine
                .emptyTarget(source);
        var report = restoreService.applyPreparedTarget(player.level(), player.getUUID(), source, empty,
                confirmed.preview().previewDigest(), now);
        if (report.outcome() != dev.romankrukovsky.kubanhorizons.genie.runtime.transaction.TransactionOutcome.COMPLETED
                && report.outcome() != dev.romankrukovsky.kubanhorizons.genie.runtime.transaction.TransactionOutcome.COMPLETED_WITH_WARNINGS) {
            throw new IllegalStateException("miniaturization transaction failed: " + report.detail());
        }
        selections.clear(player.getUUID());
        return dev.romankrukovsky.kubanhorizons.genie.spatial.MiniaturizationEngine
                .createMiniatureItem(source);
    }

    public PocketScenePreview previewPocketScene(ServerPlayer player, BlockPos origin,
                                                 int durationTicks) throws IOException {
        requireServerThread();
        RegionSnapshot target = dev.romankrukovsky.kubanhorizons.genie.dimension.PocketSceneEngine
                .buildBeachTarget(player.level(), origin, player.getUUID());
        ensureReady(target.selection());
        SnapshotService.SnapshotState current = SnapshotService.captureState(player.level(), target.selection());
        int changed = 0;
        for (int index = 0; index < target.blocks().size(); index++) {
            if (!target.blocks().get(index).blockState().equals(current.blocks().get(index).blockState())
                    || !Objects.equals(target.blocks().get(index).blockEntity(),
                    current.blocks().get(index).blockEntity())) {
                changed++;
            }
        }
        String currentDigest = SnapshotService.digest(current);
        UUID previewId = UUID.randomUUID();
        Instant expiresAt = Instant.now().plus(Duration.ofMinutes(2));
        String previewDigest = digest(previewId + "|" + player.getUUID() + "|" + currentDigest
                + "|" + target.contentDigest() + "|" + durationTicks + "|" + expiresAt);
        return new PocketScenePreview(previewId, player.getUUID(), target.selection(), changed,
                durationTicks, dev.romankrukovsky.kubanhorizons.genie.runtime.capability.CapabilityRisk.ELEVATED,
                currentDigest, target.contentDigest(), previewDigest, expiresAt);
    }

    public synchronized ConfirmedPocketScene confirmPocketScene(UUID actor,
                                                                 PocketScenePreview preview) {
        Instant now = Instant.now();
        if (!preview.actorId().equals(actor) || !preview.expiresAt().isAfter(now)) {
            throw new IllegalArgumentException("pocket scene preview is stale or belongs to another actor");
        }
        UUID id = UUID.randomUUID();
        issuedPocketSceneConfirmations.add(id);
        return new ConfirmedPocketScene(id, preview, now);
    }

    public TransactionReport executePocketScene(ServerPlayer player,
                                                ConfirmedPocketScene confirmed) throws IOException {
        requireServerThread();
        synchronized (this) {
            if (!confirmed.preview().actorId().equals(player.getUUID())
                    || !confirmed.preview().expiresAt().isAfter(Instant.now())
                    || !issuedPocketSceneConfirmations.remove(confirmed.confirmationId())) {
                throw new IllegalArgumentException("pocket scene confirmation is invalid or already used");
            }
        }
        SnapshotService.SnapshotState currentState = SnapshotService.captureState(
                player.level(), confirmed.preview().selection());
        RegionSnapshot current = new RegionSnapshot(RegionSnapshot.CURRENT_SCHEMA_VERSION,
                new dev.romankrukovsky.kubanhorizons.genie.runtime.snapshot.SnapshotId(
                        UUID.randomUUID(), "pocket_before"), player.getUUID(), Instant.now(),
                confirmed.preview().selection(), currentState.blocks(), currentState.blockTicks(),
                currentState.fluidTicks(), currentState.entities(), currentState.biomes(),
                SnapshotService.digest(currentState));
        if (!current.contentDigest().equals(confirmed.preview().currentStateDigest())) {
            throw new IllegalStateException("world state changed after pocket scene preview");
        }
        RegionSnapshot target = dev.romankrukovsky.kubanhorizons.genie.dimension.PocketSceneEngine
                .buildBeachTarget(player.level(), confirmed.preview().selection().min().offset(3, 0, 3),
                        player.getUUID());
        if (!target.contentDigest().equals(confirmed.preview().targetDigest())) {
            throw new IllegalStateException("pocket scene target changed after preview");
        }
        return restoreService.applyPreparedTarget(player.level(), player.getUUID(), current, target,
                confirmed.preview().previewDigest(), Instant.now());
    }

    public StructureMovePreview previewStructureMove(ServerPlayer player, BlockPos origin)
            throws IOException {
        return previewStructureMove(player,
                new RegionSelection(player.level().dimension().identifier().toString(),
                        origin, origin.offset(1, 1, 1)), new BlockPos(0, 10, 0));
    }

    public StructureMovePreview previewSelectedStructureMove(ServerPlayer player,
                                                             BlockPos offset) throws IOException {
        return previewSelectedStructureMove(player, offset, net.minecraft.world.level.block.Rotation.NONE);
    }

    public StructureMovePreview previewSelectedStructureMove(ServerPlayer player,
                                                             BlockPos offset,
                                                             net.minecraft.world.level.block.Rotation rotation) throws IOException {
        RegionSelection source = selections.requireCompleted(player.getUUID());
        StructureMovePreview preview = previewStructureMove(player, source, offset, rotation);
        selections.clear(player.getUUID());
        return preview;
    }

    private StructureMovePreview previewStructureMove(ServerPlayer player, RegionSelection source,
                                                      BlockPos offset) throws IOException {
        return previewStructureMove(player, source, offset, net.minecraft.world.level.block.Rotation.NONE);
    }

    private StructureMovePreview previewStructureMove(ServerPlayer player, RegionSelection source,
                                                      BlockPos offset,
                                                      net.minecraft.world.level.block.Rotation rotation) throws IOException {
        requireServerThread();
        var plan = dev.romankrukovsky.kubanhorizons.genie.dimension.FlyingStructureEngine
                .buildMovePlan(player.level(), source, offset, rotation, player.getUUID());
        ensureReady(plan.current().selection());
        int changed = 0;
        for (int index = 0; index < plan.current().blocks().size(); index++) {
            if (!plan.current().blocks().get(index).blockState()
                    .equals(plan.target().blocks().get(index).blockState())) {
                changed++;
            }
        }
        UUID previewId = UUID.randomUUID();
        Instant expiresAt = Instant.now().plus(Duration.ofMinutes(2));
        String previewDigest = digest(previewId + "|" + player.getUUID() + "|"
                + plan.current().contentDigest() + "|" + plan.target().contentDigest()
                + "|" + offset.asLong() + "|" + expiresAt);
        pendingStructureMoves.put(previewId, plan);
        return new StructureMovePreview(previewId, player.getUUID(), plan.current().selection(), changed,
                plan.current().contentDigest(), plan.target().contentDigest(), previewDigest, expiresAt);
    }

    public synchronized ConfirmedStructureMove confirmStructureMove(UUID actor,
                                                                     StructureMovePreview preview) {
        if (!preview.actorId().equals(actor) || !preview.expiresAt().isAfter(Instant.now())
                || !pendingStructureMoves.containsKey(preview.previewId())) {
            throw new IllegalArgumentException("structure move preview is stale or unavailable");
        }
        UUID id = UUID.randomUUID();
        issuedStructureMoveConfirmations.add(id);
        return new ConfirmedStructureMove(id, preview, Instant.now());
    }

    public TransactionReport executeStructureMove(ServerPlayer player,
                                                   ConfirmedStructureMove confirmed) throws IOException {
        requireServerThread();
        synchronized (this) {
            if (!confirmed.preview().actorId().equals(player.getUUID())
                    || !confirmed.preview().expiresAt().isAfter(Instant.now())
                    || !issuedStructureMoveConfirmations.remove(confirmed.confirmationId())) {
                throw new IllegalArgumentException("structure move confirmation is invalid or already used");
            }
        }
        var plan = pendingStructureMoves.remove(confirmed.preview().previewId());
        if (plan == null) {
            throw new IllegalStateException("structure move plan expired");
        }
        return restoreService.applyPreparedTarget(player.level(), player.getUUID(),
                plan.current(), plan.target(), confirmed.preview().previewDigest(), Instant.now());
    }

    public StructureRotatePreview previewSelectedStructureRotate(ServerPlayer player,
                                                                 net.minecraft.world.level.block.Rotation rotation) throws IOException {
        requireServerThread();
        RegionSelection selection = selections.requireCompleted(player.getUUID());
        ensureReady(selection);
        SnapshotService.SnapshotState current = SnapshotService.captureState(player.level(), selection);
        SnapshotService.SnapshotState target = RegionRotateService.buildRotatedTarget(current, selection, rotation);
        int changed = 0;
        for (int index = 0; index < current.blocks().size(); index++) {
            if (!current.blocks().get(index).blockState().equals(target.blocks().get(index).blockState())
                    || !Objects.equals(current.blocks().get(index).blockEntity(),
                    target.blocks().get(index).blockEntity())) {
                changed++;
            }
        }
        String currentDigest = SnapshotService.digest(current);
        String targetDigest = SnapshotService.digest(target);
        UUID previewId = UUID.randomUUID();
        Instant expiresAt = Instant.now().plus(Duration.ofMinutes(2));
        String previewDigest = digest(previewId + "|" + player.getUUID() + "|" + currentDigest
                + "|" + targetDigest + "|" + rotation + "|" + expiresAt);
        RegionSnapshot currentSnapshot = new RegionSnapshot(RegionSnapshot.CURRENT_SCHEMA_VERSION,
                new dev.romankrukovsky.kubanhorizons.genie.runtime.snapshot.SnapshotId(
                        UUID.randomUUID(), "rotate_before"), player.getUUID(), Instant.now(), selection,
                current.blocks(), current.blockTicks(), current.fluidTicks(), current.entities(),
                current.biomes(), currentDigest);
        RegionSnapshot targetSnapshot = new RegionSnapshot(RegionSnapshot.CURRENT_SCHEMA_VERSION,
                new dev.romankrukovsky.kubanhorizons.genie.runtime.snapshot.SnapshotId(
                        UUID.randomUUID(), "rotate_target"), player.getUUID(), Instant.now(), selection,
                target.blocks(), target.blockTicks(), target.fluidTicks(), target.entities(),
                target.biomes(), targetDigest);
        pendingStructureRotates.put(previewId, new RegionRotateService.RotatePlan(currentSnapshot, targetSnapshot));
        selections.clear(player.getUUID());
        return new StructureRotatePreview(previewId, player.getUUID(), selection, rotation, changed,
                currentDigest, targetDigest, previewDigest, expiresAt);
    }

    public synchronized ConfirmedStructureRotate confirmStructureRotate(UUID actor,
                                                                         StructureRotatePreview preview) {
        if (!preview.actorId().equals(actor) || !preview.expiresAt().isAfter(Instant.now())
                || !pendingStructureRotates.containsKey(preview.previewId())) {
            throw new IllegalArgumentException("structure rotate preview is stale or unavailable");
        }
        UUID id = UUID.randomUUID();
        issuedStructureRotateConfirmations.add(id);
        return new ConfirmedStructureRotate(id, preview, Instant.now());
    }

    public TransactionReport executeStructureRotate(ServerPlayer player,
                                                    ConfirmedStructureRotate confirmed) throws IOException {
        requireServerThread();
        synchronized (this) {
            if (!confirmed.preview().actorId().equals(player.getUUID())
                    || !confirmed.preview().expiresAt().isAfter(Instant.now())
                    || !issuedStructureRotateConfirmations.remove(confirmed.confirmationId())) {
                throw new IllegalArgumentException("structure rotate confirmation is invalid or already used");
            }
        }
        var plan = pendingStructureRotates.remove(confirmed.preview().previewId());
        if (plan == null) {
            throw new IllegalStateException("structure rotate plan expired");
        }
        return restoreService.applyPreparedTarget(player.level(), player.getUUID(),
                plan.current(), plan.target(), confirmed.preview().previewDigest(), Instant.now());
    }

    public BiomeRewritePreview previewBiomeRewrite(ServerPlayer player, BlockPos center)
            throws IOException {
        requireServerThread();
        var plan = dev.romankrukovsky.kubanhorizons.genie.expression.BiomeRewriterEngine
                .buildSteppePlan(player.level(), center, player.getUUID());
        ensureReady(plan.current().selection());
        int changed = 0;
        for (int index = 0; index < plan.current().biomes().size(); index++) {
            if (!plan.current().biomes().get(index).equals(plan.target().biomes().get(index))) changed++;
        }
        UUID previewId = UUID.randomUUID();
        Instant expiresAt = Instant.now().plus(Duration.ofMinutes(2));
        String previewDigest = digest(previewId + "|" + player.getUUID() + "|"
                + plan.current().contentDigest() + "|" + plan.target().contentDigest()
                + "|" + center.asLong() + "|" + expiresAt);
        pendingBiomeRewrites.put(previewId, plan);
        return new BiomeRewritePreview(previewId, player.getUUID(), plan.current().selection(),
                changed, plan.current().contentDigest(), plan.target().contentDigest(),
                previewDigest, expiresAt);
    }

    public synchronized ConfirmedBiomeRewrite confirmBiomeRewrite(UUID actor,
                                                                   BiomeRewritePreview preview) {
        if (!preview.actorId().equals(actor) || !preview.expiresAt().isAfter(Instant.now())
                || !pendingBiomeRewrites.containsKey(preview.previewId())) {
            throw new IllegalArgumentException("biome rewrite preview is stale or unavailable");
        }
        UUID id = UUID.randomUUID();
        issuedBiomeRewriteConfirmations.add(id);
        return new ConfirmedBiomeRewrite(id, preview, Instant.now());
    }

    public TransactionReport executeBiomeRewrite(ServerPlayer player,
                                                  ConfirmedBiomeRewrite confirmed) throws IOException {
        requireServerThread();
        synchronized (this) {
            if (!confirmed.preview().actorId().equals(player.getUUID())
                    || !confirmed.preview().expiresAt().isAfter(Instant.now())
                    || !issuedBiomeRewriteConfirmations.remove(confirmed.confirmationId())) {
                throw new IllegalArgumentException("biome rewrite confirmation is invalid or already used");
            }
        }
        var plan = pendingBiomeRewrites.remove(confirmed.preview().previewId());
        if (plan == null) throw new IllegalStateException("biome rewrite plan expired");
        return restoreService.applyPreparedTarget(player.level(), player.getUUID(),
                plan.current(), plan.target(), confirmed.preview().previewDigest(), Instant.now());
    }

    public DrawingPreview previewSelectedDrawing(ServerPlayer player) throws IOException {
        requireServerThread();
        RegionSelection selection = selections.requireCompleted(player.getUUID());
        var plan = dev.romankrukovsky.kubanhorizons.genie.expression.MagicDrawingHandler
                .buildLinePlan(player.level(), selection.min(), selection.max(), player.getUUID());
        ensureReady(selection);
        int changed = 0;
        for (int index = 0; index < plan.current().blocks().size(); index++) {
            if (!plan.current().blocks().get(index).blockState()
                    .equals(plan.target().blocks().get(index).blockState())) changed++;
        }
        UUID previewId = UUID.randomUUID();
        Instant expiresAt = Instant.now().plus(Duration.ofMinutes(2));
        String previewDigest = digest(previewId + "|" + player.getUUID() + "|"
                + plan.current().contentDigest() + "|" + plan.target().contentDigest()
                + "|" + expiresAt);
        pendingDrawings.put(previewId, plan);
        selections.clear(player.getUUID());
        return new DrawingPreview(previewId, player.getUUID(), selection, changed,
                plan.current().contentDigest(), plan.target().contentDigest(),
                previewDigest, expiresAt);
    }

    public synchronized ConfirmedDrawing confirmDrawing(UUID actor, DrawingPreview preview) {
        if (!preview.actorId().equals(actor) || !preview.expiresAt().isAfter(Instant.now())
                || !pendingDrawings.containsKey(preview.previewId())) {
            throw new IllegalArgumentException("drawing preview is stale or unavailable");
        }
        UUID id = UUID.randomUUID();
        issuedDrawingConfirmations.add(id);
        return new ConfirmedDrawing(id, preview, Instant.now());
    }

    public TransactionReport executeDrawing(ServerPlayer player, ConfirmedDrawing confirmed)
            throws IOException {
        requireServerThread();
        synchronized (this) {
            if (!confirmed.preview().actorId().equals(player.getUUID())
                    || !confirmed.preview().expiresAt().isAfter(Instant.now())
                    || !issuedDrawingConfirmations.remove(confirmed.confirmationId())) {
                throw new IllegalArgumentException("drawing confirmation is invalid or already used");
            }
        }
        var plan = pendingDrawings.remove(confirmed.preview().previewId());
        if (plan == null) throw new IllegalStateException("drawing plan expired");
        return restoreService.applyPreparedTarget(player.level(), player.getUUID(),
                plan.current(), plan.target(), confirmed.preview().previewDigest(), Instant.now());
    }

    public WordPreview previewWord(ServerPlayer player, String word) throws IOException {
        requireServerThread();
        var plan = dev.romankrukovsky.kubanhorizons.genie.expression.WordMaterializer
                .buildWordPlan(player.level(), player.blockPosition().above(3), word, player.getUUID());
        ensureReady(plan.current().selection());
        int changed = 0;
        for (int index = 0; index < plan.current().blocks().size(); index++) {
            if (!plan.current().blocks().get(index).blockState()
                    .equals(plan.target().blocks().get(index).blockState())) changed++;
        }
        UUID previewId = UUID.randomUUID();
        Instant expiresAt = Instant.now().plus(Duration.ofMinutes(2));
        String previewDigest = digest(previewId + "|" + player.getUUID() + "|"
                + plan.current().contentDigest() + "|" + plan.target().contentDigest()
                + "|" + plan.word() + "|" + expiresAt);
        pendingWords.put(previewId, plan);
        return new WordPreview(previewId, player.getUUID(), plan.current().selection(), plan.word(),
                changed, plan.current().contentDigest(), plan.target().contentDigest(),
                previewDigest, expiresAt);
    }

    public synchronized ConfirmedWord confirmWord(UUID actor, WordPreview preview) {
        if (!preview.actorId().equals(actor) || !preview.expiresAt().isAfter(Instant.now())
                || !pendingWords.containsKey(preview.previewId())) {
            throw new IllegalArgumentException("word preview is stale or unavailable");
        }
        UUID id = UUID.randomUUID();
        issuedWordConfirmations.add(id);
        return new ConfirmedWord(id, preview, Instant.now());
    }

    public TransactionReport executeWord(ServerPlayer player, ConfirmedWord confirmed) throws IOException {
        requireServerThread();
        synchronized (this) {
            if (!confirmed.preview().actorId().equals(player.getUUID())
                    || !confirmed.preview().expiresAt().isAfter(Instant.now())
                    || !issuedWordConfirmations.remove(confirmed.confirmationId())) {
                throw new IllegalArgumentException("word confirmation is invalid or already used");
            }
        }
        var plan = pendingWords.remove(confirmed.preview().previewId());
        if (plan == null) throw new IllegalStateException("word plan expired");
        return restoreService.applyPreparedTarget(player.level(), player.getUUID(),
                plan.current(), plan.target(), confirmed.preview().previewDigest(), Instant.now());
    }

    public PolicyPreview previewMobGriefing(UUID actor, boolean target) {
        requireServerThread();
        return policyService.previewMobGriefing(actor, server, target);
    }

    public PolicyPreview previewInstantSmelt(UUID actor, boolean target) {
        requireServerThread();
        return policyService.previewInstantSmelt(actor, server, target);
    }

    public boolean isInstantSmeltEnabled() {
        return PolicyService.isInstantSmeltEnabled();
    }

    public PolicyPreview previewWeather(UUID actor, float rain, float thunder) {
        requireServerThread();
        return policyService.previewWeather(actor, server, rain, thunder);
    }

    public PolicyPreview previewClockRate(UUID actor, float rate) {
        requireServerThread();
        return policyService.previewClockRate(actor, server, rate);
    }

    public ConfirmedPolicy confirmPolicy(UUID actor, PolicyPreview preview) {
        return policyService.confirm(actor, preview);
    }

    public TransactionReport executePolicy(UUID actor, ConfirmedPolicy confirmed) throws IOException {
        requireServerThread();
        return policyService.execute(actor, server, confirmed);
    }

    public TransactionReport undoPolicy(UUID actor, UUID transactionId) throws IOException {
        requireServerThread();
        return policyService.undo(actor, server, transactionId);
    }

    public RestorePreview previewRestore(ServerLevel level, UUID actor, String name) throws IOException {
        RegionSnapshot snapshot = snapshotStore.findOwnedByName(actor, name)
                .orElseThrow(() -> new IllegalArgumentException("snapshot not found"));
        requireServerThread();
        ensureReady(snapshot.selection());
        return previewService.preview(level, actor, snapshot, Instant.now());
    }

    public List<SnapshotSummary> listSnapshots(UUID actor) throws IOException {
        requireServerThread();
        return snapshotStore.listOwned(actor).stream().map(snapshot -> new SnapshotSummary(
                snapshot.id().name(), snapshot.id().value(), snapshot.selection().dimension(),
                snapshot.selection().volume(), snapshot.selection().chunkCount(), snapshot.capturedAt()))
                .toList();
    }

    public SnapshotSummary inspectSnapshot(UUID actor, String name) throws IOException {
        requireServerThread();
        RegionSnapshot snapshot = snapshotStore.findOwnedByName(actor, name)
                .orElseThrow(() -> new IllegalArgumentException("snapshot not found"));
        return new SnapshotSummary(snapshot.id().name(), snapshot.id().value(),
                snapshot.selection().dimension(), snapshot.selection().volume(),
                snapshot.selection().chunkCount(), snapshot.capturedAt());
    }

    public void deleteSnapshot(UUID actor, String name) throws IOException {
        requireServerThread();
        RegionSnapshot snapshot = snapshotStore.findOwnedByName(actor, name)
                .orElseThrow(() -> new IllegalArgumentException("snapshot not found"));
        if (manifests.referencesSnapshot(snapshot.id().value())) {
            throw new IllegalStateException("snapshot is protected by recovery or retained undo");
        }
        snapshotStore.remove(snapshot.id().value());
    }

    public ConfirmedRestore confirm(UUID actor, RestorePreview preview) {
        ensureReady(preview.selection());
        return confirmations.issue(actor, preview, Instant.now());
    }

    public TransactionReport restore(ServerLevel level, UUID actor,
                                     ConfirmedRestore confirmation) throws IOException {
        ensureReady(confirmation.preview().selection());
        requireServerThread();
        RegionSnapshot snapshot = snapshotStore.load(confirmation.preview().snapshotId().value())
                .orElseThrow(() -> new IOException("target snapshot disappeared"));
        if (!snapshot.ownerId().equals(actor)) {
            throw new IllegalArgumentException("snapshot belongs to another player");
        }
        return restoreService.restore(level, actor, confirmation, Instant.now());
    }

    public TransactionReport undo(ServerLevel level, UUID actor, UUID transactionId) throws IOException {
        requireServerThread();
        return restoreService.undo(level, actor, transactionId, Instant.now());
    }

    public List<RestoreTransactionService.UndoSummary> availableUndo(UUID actor) throws IOException {
        requireServerThread();
        restoreService.cleanupExpiredUndo(Instant.now());
        return restoreService.availableUndo(actor, Instant.now());
    }

    public void retireUndo(UUID actor, UUID transactionId) throws IOException {
        requireServerThread();
        restoreService.retireUndo(transactionId, actor, Instant.now());
    }

    /** Долговечный причинный индекс завершённых транзакций для способности «А что если?». */
    public List<CausalLedgerEntry> causalIndex() throws IOException {
        return ledger.readAll();
    }

    /** Загружает целевой снимок по UUID для сравнения альтернативной версии мира. */
    public Optional<RegionSnapshot> findSnapshot(UUID snapshotId) throws IOException {
        requireServerThread();
        return snapshotStore.load(Objects.requireNonNull(snapshotId, "snapshotId"));
    }

    public CapabilityRegistry capabilities() {
        return capabilities;
    }

    public PlanGate planGate() {
        return planGate;
    }

    public boolean ready() {
        return ready;
    }

    public String blockedReason() {
        return blockedReason;
    }

    private void ensureReady(RegionSelection selection) {
        if (!ready) {
            throw new IllegalStateException("strong wishes are blocked: " + blockedReason);
        }
        try {
            var admission = RecoveryClassifier.classify(recoveryJournal.scan())
                    .check(new dev.romankrukovsky.kubanhorizons.genie.runtime.recovery.AffectedScope(
                            selection.dimension(),
                            net.minecraft.world.level.ChunkPos.containing(selection.min()).x(),
                            net.minecraft.world.level.ChunkPos.containing(selection.min()).z(),
                            net.minecraft.world.level.ChunkPos.containing(selection.max()).x(),
                            net.minecraft.world.level.ChunkPos.containing(selection.max()).z()));
            if (!admission.permitted()) {
                throw new IllegalStateException("recovery gate blocks this region: "
                        + admission.reason().orElseThrow());
            }
        } catch (IOException exception) {
            throw new IllegalStateException("recovery journal cannot be verified", exception);
        }
    }

    private MinecraftServer server() {
        return server;
    }

    private void requireServerThread() {
        if (!server.isSameThread()) {
            throw new IllegalStateException("strong wish world access must run on the server thread");
        }
    }

    private static String digest(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }

    public record SnapshotSummary(String name, UUID id, String dimension, long blocks,
                                  int chunks, Instant capturedAt) {
        public SnapshotSummary {
            Objects.requireNonNull(name, "name");
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(dimension, "dimension");
            Objects.requireNonNull(capturedAt, "capturedAt");
        }
    }
}
