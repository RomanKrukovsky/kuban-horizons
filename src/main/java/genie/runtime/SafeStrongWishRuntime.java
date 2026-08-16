package genie.runtime;

import genie.entity.GenieEntity;
import genie.util.GenieLogger;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkStatus;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Safe wish execution with rollback capabilities
 * Implements snapshot → preview → confirmation → transaction → journal → rollback (24h)
 */
public class SafeStrongWishRuntime {
    private final GenieEntity genie;
    private final Map<String, RegionSnapshot> snapshots = new ConcurrentHashMap<>();
    private final RecoveryService recoveryService;
    private String lastSnapshotName = "";
    private long lastSnapshotTime = 0;
    private static final int MAX_SNAPSHOTS = 20;
    private static final long SNAPSHOT_RETENTION_MS = 24 * 60 * 60 * 1000; // 24 hours

    // Configuration
    private static final int SNAPSHOT_RADIUS = 32; // Blocks
    private static final int MAX_BLOCKS_PER_SNAPSHOT = 128 * 1024; // 128K blocks
    private static final int CHUNK_CACHE_SIZE = 256;

    public SafeStrongWishRuntime(GenieEntity genie) {
        this.genie = genie;
        this.recoveryService = new RecoveryService(genie);
    }

    /**
     * Take a snapshot of the region around genie
     */
    public boolean takeSnapshot(String snapshotName) {
        if (genie.level().isClientSide) {
            GenieLogger.warn("Cannot take snapshot on client side");
            return false;
        }

        long startTime = System.currentTimeMillis();
        ServerLevel level = (ServerLevel) genie.level();

        try {
            // Check if we can take snapshot
            if (!canTakeSnapshot()) {
                GenieLogger.warn("Cannot take snapshot: world state unstable");
                return false;
            }

            // Create new snapshot
            RegionSnapshot snapshot = new RegionSnapshot(snapshotName, level, genie.blockPosition(), SNAPSHOT_RADIUS);
            snapshot.capture();

            // Store snapshot
            snapshots.put(snapshotName, snapshot);
            lastSnapshotName = snapshotName;
            lastSnapshotTime = level.getGameTime();

            // Clean up old snapshots
            cleanupOldSnapshots();

            long duration = System.currentTimeMillis() - startTime;
            GenieLogger.info("Snapshot " + snapshotName + " captured in " + duration + "ms. Blocks: " + snapshot.getBlockCount());

            return true;

        } catch (Exception e) {
            GenieLogger.error("Failed to take snapshot: " + e.getMessage());
            return false;
        }
    }

    /**
     * Check if snapshot can be taken
     */
    private boolean canTakeSnapshot() {
        // Check if world is loading
        if (genie.level().isClientSide) return false;

        // Check if genie is in valid state
        if (genie.isRemoved() || !genie.isAlive()) return false;

        return true;
    }

    /**
     * Rollback to last snapshot
     */
    public boolean rollbackLastSnapshot() {
        if (lastSnapshotName.isEmpty()) {
            GenieLogger.warn("No snapshot available for rollback");
            return false;
        }

        return rollbackToSnapshot(lastSnapshotName);
    }

    /**
     * Rollback to specific snapshot
     */
    public boolean rollbackToSnapshot(String snapshotName) {
        if (genie.level().isClientSide) {
            GenieLogger.warn("Cannot rollback on client side");
            return false;
        }

        long startTime = System.currentTimeMillis();
        ServerLevel level = (ServerLevel) genie.level();

        try {
            RegionSnapshot snapshot = snapshots.get(snapshotName);
            if (snapshot == null) {
                GenieLogger.warn("Snapshot " + snapshotName + " not found");
                return false;
            }

            // Restore snapshot
            snapshot.restore();

            // Record recovery
            recoveryService.recordRecovery(
                snapshotName,
                genie.blockPosition(),
                "rollback",
                "success"
            );

            long duration = System.currentTimeMillis() - startTime;
            GenieLogger.info("Restored snapshot " + snapshotName + " in " + duration + "ms");

            return true;

        } catch (Exception e) {
            GenieLogger.error("Failed to rollback snapshot: " + e.getMessage());
            recoveryService.recordRecovery(
                snapshotName,
                genie.blockPosition(),
                "rollback",
                "failed: " + e.getMessage()
            );
            return false;
        }
    }

    /**
     * Preview wish effects before execution
     */
    public PreviewService.PreviewResult previewWishEffects(String wishText) {
        PreviewService previewService = new PreviewService(genie);
        return previewService.previewWish(wishText);
    }

    /**
     * Create transaction manifest
     */
    public TransactionManifest createTransactionManifest(String manifestName) {
        TransactionManifest manifest = new TransactionManifest(manifestName, genie);
        manifest.setTimestamp(genie.level().getGameTime());
        manifest.setPosition(genie.blockPosition());
        return manifest;
    }

    /**
     * Clean up old snapshots
     */
    private void cleanupOldSnapshots() {
        if (snapshots.size() <= MAX_SNAPSHOTS) return;

        // Find oldest snapshots
        snapshots.entrySet().stream()
            .sorted(Map.Entry.comparingByValue((s1, s2) -> Long.compare(
                s1.getCreationTime(),
                s2.getCreationTime()
            )))
            .limit(snapshots.size() - MAX_SNAPSHOTS)
            .forEach(entry -> {
                try {
                    entry.getValue().cleanup();
                    snapshots.remove(entry.getKey());
                } catch (Exception e) {
                    GenieLogger.error("Failed to cleanup snapshot: " + e.getMessage());
                }
            });
    }

    /**
     * Get snapshot by name
     */
    @Nullable
    public RegionSnapshot getSnapshot(String snapshotName) {
        return snapshots.get(snapshotName);
    }

    /**
     * Get last snapshot name
     */
    public String getLastSnapshotName() {
        return lastSnapshotName;
    }

    /**
     * Get last snapshot time
     */
    public long getLastSnapshotTime() {
        return lastSnapshotTime;
    }

    /**
     * Check if snapshot exists
     */
    public boolean hasSnapshot(String snapshotName) {
        return snapshots.containsKey(snapshotName);
    }

    /**
     * Get snapshot count
     */
    public int getSnapshotCount() {
        return snapshots.size();
    }

    /**
     * Clear all snapshots
     */
    public void clearAllSnapshots() {
        for (RegionSnapshot snapshot : snapshots.values()) {
            try {
                snapshot.cleanup();
            } catch (Exception e) {
                GenieLogger.error("Failed to cleanup snapshot during clear: " + e.getMessage());
            }
        }
        snapshots.clear();
        lastSnapshotName = "";
        GenieLogger.info("All snapshots cleared");
    }

    /**
     * Get snapshot statistics
     */
    public SnapshotStatistics getSnapshotStatistics() {
        int totalBlocks = 0;
        int totalChunks = 0;
        long totalSize = 0;

        for (RegionSnapshot snapshot : snapshots.values()) {
            totalBlocks += snapshot.getBlockCount();
            totalChunks += snapshot.getChunkCount();
            totalSize += snapshot.getEstimatedSize();
        }

        return new SnapshotStatistics(
            snapshots.size(),
            totalBlocks,
            totalChunks,
            totalSize
        );
    }

    /**
     * Snapshot statistics container
     */
    public static class SnapshotStatistics {
        private final int snapshotCount;
        private final int totalBlocks;
        private final int totalChunks;
        private final long totalSize;

        public SnapshotStatistics(int snapshotCount, int totalBlocks, int totalChunks, long totalSize) {
            this.snapshotCount = snapshotCount;
            this.totalBlocks = totalBlocks;
            this.totalChunks = totalChunks;
            this.totalSize = totalSize;
        }

        public int getSnapshotCount() { return snapshotCount; }
        public int getTotalBlocks() { return totalBlocks; }
        public int getTotalChunks() { return totalChunks; }
        public long getTotalSize() { return totalSize; }

        public String getSummary() {
            return String.format("Snapshots: %d, Blocks: %d, Chunks: %d, Size: %d KB",
                snapshotCount, totalBlocks, totalChunks, totalSize / 1024);
        }
    }

    /**
     * Check if recovery is needed
     */
    public boolean needsRecovery() {
        return recoveryService.hasPendingRecoveries();
    }

    /**
     * Process pending recoveries
     */
    public void processRecoveries() {
        recoveryService.processPendingRecoveries();
    }
}