package genie.wish;

import genie.GenieStateSnapshot;
import genie.causality.CausalityLedger;
import genie.causality.CausalLedgerEntry;
import genie.preview.PreviewService;
import genie.recovery.RecoveryService;
import genie.recovery.RecoveryGate;
import genie.recovery.TransactionManifest;
import genie.world.RegionSnapshot;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Safe wish runtime with snapshot, preview, confirmation, journal, and rollback capabilities.
 * Handles wish execution with safety checks and recovery options.
 */
public class SafeStrongWishRuntime {

    private final Map<UUID, WishTransaction> activeTransactions = new HashMap<>();
    private final PreviewService previewService = new PreviewService();
    private final RecoveryService recoveryService = new RecoveryService();
    private final CausalityLedger causalityLedger = new CausalityLedger();

    /**
     * Execute a wish with safety checks and recovery options
     */
    @Nullable
    public WishResult executeWish(WishRuntime runtime, String wishText, BlockPos origin) {
        UUID transactionId = UUID.randomUUID();
        WishTransaction transaction = new WishTransaction(transactionId, runtime.getPlayer(), wishText, origin);
        activeTransactions.put(transactionId, transaction);

        try {
            // Phase 1: Preview
            WishPreview preview = previewService.createPreview(runtime, wishText, origin);
            if (preview == null) {
                return new WishResult(false, "preview_failed", Component.translatable("wish.preview.failed"));
            }

            // Phase 2: Confirmation
            if (!runtime.getConfirmationAuthority().requestConfirmation(runtime.getPlayer(), wishText, preview)) {
                return new WishResult(false, "user_cancelled", Component.translatable("wish.confirmation.cancelled"));
            }

            // Phase 3: Create snapshot
            RegionSnapshot snapshot = createSnapshot(runtime.getLevel(), origin, 64);
            if (snapshot == null) {
                return new WishResult(false, "snapshot_failed", Component.translatable("wish.snapshot.failed"));
            }

            // Phase 4: Execute wish
            boolean success = runtime.getWishExecutor().executeWish(wishText, origin);

            if (success) {
                // Phase 5: Record causality
                CausalLedgerEntry entry = causalityLedger.recordWish(runtime.getPlayer(), wishText, origin, preview);
                transaction.setCausalEntry(entry);

                // Phase 6: Log transaction
                TransactionManifest.logTransaction(transactionId, runtime.getPlayer(), wishText, origin, true);

                return new WishResult(true, "success", Component.translatable("wish.success"));
            } else {
                // Phase 7: Rollback on failure
                rollbackTransaction(transactionId, "wish_execution_failed");
                return new WishResult(false, "execution_failed", Component.translatable("wish.execution.failed"));
            }
        } catch (Exception e) {
            rollbackTransaction(transactionId, "exception_occurred");
            return new WishResult(false, "exception", Component.translatable("wish.exception"));
        } finally {
            activeTransactions.remove(transactionId);
        }
    }

    /**
     * Create a region snapshot for recovery
     */
    @Nullable
    private RegionSnapshot createSnapshot(Level level, BlockPos center, int radius) {
        if (level instanceof ServerLevel serverLevel) {
            BlockPos minPos = center.offset(-radius, -radius, -radius);
            BlockPos maxPos = center.offset(radius, radius, radius);
            return new RegionSnapshot(serverLevel, minPos, maxPos);
        }
        return null;
    }

    /**
     * Rollback a transaction to a previous state
     */
    public boolean rollbackTransaction(UUID transactionId, String reason) {
        WishTransaction transaction = activeTransactions.get(transactionId);
        if (transaction != null) {
            RecoveryGate gate = recoveryService.classifyRecovery(reason);
            if (gate != null) {
                RegionSnapshot snapshot = transaction.getSnapshot();
                if (snapshot != null) {
                    snapshot.restore();
                    TransactionManifest.logRollback(transactionId, reason, gate);
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Get active transaction by ID
     */
    @Nullable
    public WishTransaction getTransaction(UUID transactionId) {
        return activeTransactions.get(transactionId);
    }

    /**
     * Cleanup old transactions (24h retention)
     */
    public void cleanupOldTransactions() {
        long cutoff = System.currentTimeMillis() - (24 * 60 * 60 * 1000);
        activeTransactions.entrySet().removeIf(entry ->
            entry.getValue().getTimestamp() < cutoff
        );
    }

    /**
     * Wish transaction container
     */
    public static class WishTransaction {
        private final UUID transactionId;
        private final UUID playerId;
        private final String wishText;
        private final BlockPos origin;
        private final long timestamp;
        @Nullable private RegionSnapshot snapshot;
        @Nullable private CausalLedgerEntry causalEntry;

        public WishTransaction(UUID transactionId, UUID playerId, String wishText, BlockPos origin) {
            this.transactionId = transactionId;
            this.playerId = playerId;
            this.wishText = wishText;
            this.origin = origin;
            this.timestamp = System.currentTimeMillis();
        }

        public void setSnapshot(RegionSnapshot snapshot) {
            this.snapshot = snapshot;
        }

        public void setCausalEntry(CausalLedgerEntry entry) {
            this.causalEntry = entry;
        }

        // Getters
        public UUID getTransactionId() { return transactionId; }
        public UUID getPlayerId() { return playerId; }
        public String getWishText() { return wishText; }
        public BlockPos getOrigin() { return origin; }
        public long getTimestamp() { return timestamp; }
        @Nullable public RegionSnapshot getSnapshot() { return snapshot; }
        @Nullable public CausalLedgerEntry getCausalEntry() { return causalEntry; }
    }

    /**
     * Wish execution result
     */
    public static class WishResult {
        private final boolean success;
        private final String status;
        private final Component message;

        public WishResult(boolean success, String status, Component message) {
            this.success = success;
            this.status = status;
            this.message = message;
        }

        // Getters
        public boolean isSuccess() { return success; }
        public String getStatus() { return status; }
        public Component getMessage() { return message; }
    }
}
