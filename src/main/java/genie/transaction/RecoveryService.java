package genie.transaction;

import genie.GenieAnchor;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Recovery service for undo/redo operations.
 * Restores world state from snapshots based on transaction IDs.
 */
public class RecoveryService {
    private final CausalityLedger ledger;
    private final Map<String, Path> snapshotCache;
    private final RecoveryClassifier classifier;

    public RecoveryService(CausalityLedger ledger) {
        this.ledger = ledger;
        this.snapshotCache = new ConcurrentHashMap<>();
        this.classifier = new RecoveryClassifier();
    }

    /**
     * Archive a snapshot for recovery
     */
    public void archiveSnapshot(RegionSnapshot snapshot, String transactionId) {
        Path snapshotPath = getSnapshotPath(transactionId);
        try {
            CompoundTag snapshotTag = snapshot.serialize(null);
            Files.write(snapshotPath, snapshotTag.getAsString().getBytes());
            snapshotCache.put(transactionId, snapshotPath);
        } catch (IOException e) {
            System.err.println("Failed to archive snapshot: " + e.getMessage());
        }
    }

    /**
     * Restore world state from snapshot by transaction ID
     * @return true if restoration was successful
     */
    public boolean restoreFromSnapshot(ServerLevel level, String transactionId) {
        Path snapshotPath = getSnapshotPath(transactionId);

        if (!Files.exists(snapshotPath)) {
            System.err.println("Snapshot not found for transaction: " + transactionId);
            return false;
        }

        try {
            String json = Files.readString(snapshotPath);
            CompoundTag tag = CompoundTag.parse(json);
            RegionSnapshot snapshot = RegionSnapshot.deserialize(tag, level.registryAccess());

            // Classify recovery type
            RecoveryClassifier.RecoveryType type = classifier.classifyRecovery(snapshot);

            // Restore snapshot
            snapshot.restore(level);

            // Log recovery event
            TransactionManifest manifest = new TransactionManifest();
            manifest.setType("recovery");
            manifest.setTransactionId("recovery_" + transactionId);
            manifest.setTimestamp(System.currentTimeMillis());
            manifest.setSuccess(true);
            manifest.setMessage("Restored from snapshot: " + type.name());
            ledger.recordTransaction(manifest);

            return true;
        } catch (IOException e) {
            System.err.println("Failed to restore snapshot: " + e.getMessage());
            return false;
        }
    }

    /**
     * Rollback to a specific transaction
     */
    public boolean rollbackToTransaction(ServerLevel level, String transactionId) {
        // Find the transaction in ledger
        CausalLedgerEntry entry = ledger.getEntry(transactionId);
        if (entry == null || !entry.isSuccess()) {
            return false;
        }

        // Restore from snapshot
        return restoreFromSnapshot(level, transactionId);
    }

    /**
     * Get recovery classifier
     */
    public RecoveryClassifier getClassifier() {
        return classifier;
    }

    /**
     * Cleanup old snapshots (older than 24h)
     */
    public void cleanupOldSnapshots() {
        long cutoff = System.currentTimeMillis() - (24L * 60 * 60 * 1000);

        snapshotCache.entrySet().removeIf(entry -> {
            Path path = entry.getValue();
            try {
                if (Files.exists(path)) {
                    long lastModified = Files.getLastModifiedTime(path).toMillis();
                    if (lastModified < cutoff) {
                        Files.deleteIfExists(path);
                        return true;
                    }
                }
            } catch (IOException e) {
                System.err.println("Failed to cleanup snapshot: " + e.getMessage());
            }
            return false;
        });
    }

    /**
     * Get snapshot path for transaction ID
     */
    private Path getSnapshotPath(String transactionId) {
        return ledger.getLedgerDirectory().resolve("snapshots").resolve(transactionId + ".nbt");
    }

    /**
     * Recovery classifier for different types of recoveries
     */
    public static class RecoveryClassifier {
        public enum RecoveryType {
            FULL_REGION_RESTORE,
            PARTIAL_CHANGES,
            ANCHOR_RELOCATION,
            BLOCK_STATE_CHANGES,
            ENTITY_CHANGES,
            UNKNOWN
        }

        /**
         * Classify recovery type based on snapshot
         */
        public RecoveryType classifyRecovery(RegionSnapshot snapshot) {
            int blockCount = snapshot.getBlockCount();

            if (blockCount == 0) {
                return RecoveryType.ENTITY_CHANGES;
            } else if (blockCount < 10) {
                return RecoveryType.PARTIAL_CHANGES;
            } else if (snapshot.getAnchorsInRegion().size() > 0) {
                return RecoveryType.ANCHOR_RELOCATION;
            } else {
                return RecoveryType.FULL_REGION_RESTORE;
            }
        }
    }
}
