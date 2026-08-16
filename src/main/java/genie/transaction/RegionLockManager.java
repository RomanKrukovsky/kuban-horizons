package genie.transaction;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;

/**
 * Region protection and locking system for Kuban Genie.
 * Prevents concurrent modifications to protected regions.
 */
public class RegionLockManager {
    private final Map<ChunkPos, LockState> lockedRegions;
    private final Set<ChunkPos> protectedRegions;

    public RegionLockManager() {
        this.lockedRegions = new ConcurrentHashMap<>();
        this.protectedRegions = new ConcurrentSkipListSet<>();
        initializeProtectedRegions();
    }

    /**
     * Initialize default protected regions
     */
    private void initializeProtectedRegions() {
        // Add default protected regions (e.g., spawn areas, important structures)
        protectedRegions.add(new ChunkPos(0, 0)); // World origin
    }

    /**
     * Try to lock a region for exclusive modification
     * @return true if region was successfully locked
     */
    public boolean tryLockRegion(ServerLevel level, ChunkPos chunkPos) {
        // Check if region is already locked
        if (lockedRegions.containsKey(chunkPos)) {
            return false;
        }

        // Check if region is protected
        if (isProtected(chunkPos)) {
            return false;
        }

        // Lock the region
        LockState lockState = new LockState(level.dimension().location().toString());
        lockedRegions.put(chunkPos, lockState);
        return true;
    }

    /**
     * Unlock a region
     */
    public void unlockRegion(ServerLevel level, ChunkPos chunkPos) {
        lockedRegions.remove(chunkPos);
    }

    /**
     * Check if region is locked
     */
    public boolean isLocked(ChunkPos chunkPos) {
        return lockedRegions.containsKey(chunkPos);
    }

    /**
     * Check if region is protected
     */
    public boolean isProtected(ChunkPos chunkPos) {
        return protectedRegions.contains(chunkPos);
    }

    /**
     * Add a protected region
     */
    public void addProtectedRegion(ChunkPos chunkPos) {
        protectedRegions.add(chunkPos);
    }

    /**
     * Remove a protected region
     */
    public void removeProtectedRegion(ChunkPos chunkPos) {
        protectedRegions.remove(chunkPos);
    }

    /**
     * Get all locked regions
     */
    public Set<ChunkPos> getLockedRegions() {
        return lockedRegions.keySet();
    }

    /**
     * Get all protected regions
     */
    public Set<ChunkPos> getProtectedRegions() {
        return protectedRegions;
    }

    /**
     * Lock state tracking
     */
    private static class LockState {
        private final String dimension;
        private final long lockTime;
        private final String locker;

        public LockState(String dimension) {
            this.dimension = dimension;
            this.lockTime = System.currentTimeMillis();
            this.locker = Thread.currentThread().getName();
        }

        public String getDimension() {
            return dimension;
        }

        public long getLockTime() {
            return lockTime;
        }

        public String getLocker() {
            return locker;
        }
    }

    /**
     * Configuration for region locking
     */
    public static class LockConfig {
        public int maxLockedRegions = 1000;
        public long maxLockDurationMs = 60000; // 1 minute
        public boolean enableAutoUnlock = true;
        public int autoUnlockCheckInterval = 30000; // 30 seconds
    }
}
