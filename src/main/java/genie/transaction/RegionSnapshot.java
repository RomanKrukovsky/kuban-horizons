package genie.transaction;

import com.google.common.collect.ImmutableMap;
import genie.GenieAnchor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * World state capture system for Kuban Genie.
 * Captures region state for recovery and rollback purposes.
 * Supports 128K blocks (16x16x128 region) with 256 chunk support.
 */
public class RegionSnapshot {
    private final ResourceLocation dimension;
    private final BlockPos centerPosition;
    private final int radiusX;
    private final int radiusY;
    private final int radiusZ;
    private final long timestamp;
    private final Map<BlockPos, BlockState> blockStates;
    private final Map<BlockPos, CompoundTag> tileEntities;
    private final Set<GenieAnchor> anchorsInRegion;
    private final int blockCount;

    private RegionSnapshot(Builder builder) {
        this.dimension = builder.dimension;
        this.centerPosition = builder.centerPosition;
        this.radiusX = builder.radiusX;
        this.radiusY = builder.radiusY;
        this.radiusZ = builder.radiusZ;
        this.timestamp = builder.timestamp;
        this.blockStates = ImmutableMap.copyOf(builder.blockStates);
        this.tileEntities = ImmutableMap.copyOf(builder.tileEntities);
        this.anchorsInRegion = ImmutableSet.copyOf(builder.anchorsInRegion);
        this.blockCount = builder.blockCount;
    }

    /**
     * Create a snapshot of a region centered at position
     */
    public static RegionSnapshot capture(ServerLevel level, BlockPos center, int radiusX, int radiusY, int radiusZ) {
        Builder builder = new Builder(level.dimension(), center, radiusX, radiusY, radiusZ);
        return builder.capture(level);
    }

    /**
     * Create a snapshot with default parameters (16x16x16 region)
     */
    public static RegionSnapshot capture(ServerLevel level, BlockPos center) {
        return capture(level, center, 8, 8, 8);
    }

    /**
     * Restore this snapshot to a world
     */
    public void restore(ServerLevel level) {
        AtomicInteger restoredCount = new AtomicInteger(0);

        blockStates.forEach((pos, state) -> {
            if (level.isInWorldBounds(pos)) {
                level.setBlock(pos, state, 3);
                restoredCount.incrementAndGet();
            }
        });

        tileEntities.forEach((pos, tag) -> {
            if (level.isInWorldBounds(pos)) {
                level.setBlock(pos, blockStates.get(pos), 3);
                // Tile entity restoration would go here
            }
        });

        // Restore anchors
        anchorsInRegion.forEach(anchor -> {
            anchor.teleportTo(level, centerPosition);
        });
    }

    /**
     * Get dimension of this snapshot
     */
    public ResourceLocation getDimension() {
        return dimension;
    }

    /**
     * Get center position
     */
    public BlockPos getCenterPosition() {
        return centerPosition;
    }

    /**
     * Get radius in X direction
     */
    public int getRadiusX() {
        return radiusX;
    }

    /**
     * Get radius in Y direction
     */
    public int getRadiusY() {
        return radiusY;
    }

    /**
     * Get radius in Z direction
     */
    public int getRadiusZ() {
        return radiusZ;
    }

    /**
     * Get timestamp when snapshot was taken
     */
    public long getTimestamp() {
        return timestamp;
    }

    /**
     * Get number of blocks captured
     */
    public int getBlockCount() {
        return blockCount;
    }

    /**
     * Get all block states in this snapshot
     */
    public Map<BlockPos, BlockState> getBlockStates() {
        return blockStates;
    }

    /**
     * Get all tile entities in this snapshot
     */
    public Map<BlockPos, CompoundTag> getTileEntities() {
        return tileEntities;
    }

    /**
     * Get anchors in this region
     */
    public Set<GenieAnchor> getAnchorsInRegion() {
        return anchorsInRegion;
    }

    /**
     * Serialize snapshot to NBT
     */
    public CompoundTag serialize(RegistryAccess registryAccess) {
        CompoundTag tag = new CompoundTag();

        tag.putString("dimension", dimension.toString());
        tag.putLong("timestamp", timestamp);
        tag.putLong("centerX", centerPosition.getX());
        tag.putLong("centerY", centerPosition.getY());
        tag.putLong("centerZ", centerPosition.getZ());
        tag.putInt("radiusX", radiusX);
        tag.putInt("radiusY", radiusY);
        tag.putInt("radiusZ", radiusZ);
        tag.putInt("blockCount", blockCount);

        // Serialize block states
        ListTag blockStatesList = new ListTag();
        blockStates.forEach((pos, state) -> {
            CompoundTag stateTag = new CompoundTag();
            stateTag.putLong("x", pos.getX());
            stateTag.putLong("y", pos.getY());
            stateTag.putLong("z", pos.getZ());
            stateTag.putString("block", state.getBlock().getRegistryName().toString());
            stateTag.putString("state", state.toString());
            blockStatesList.add(stateTag);
        });
        tag.put("blockStates", blockStatesList);

        // Serialize anchors
        ListTag anchorsList = new ListTag();
        anchorsInRegion.forEach(anchor -> {
            anchorsList.add(StringTag.valueOf(anchor.getAnchorId()));
        });
        tag.put("anchors", anchorsList);

        return tag;
    }

    /**
     * Deserialize snapshot from NBT
     */
    public static RegionSnapshot deserialize(CompoundTag tag, RegistryAccess registryAccess) {
        ResourceLocation dimension = new ResourceLocation(tag.getString("dimension"));
        BlockPos center = new BlockPos(
            tag.getLong("centerX"),
            tag.getLong("centerY"),
            tag.getLong("centerZ")
        );
        int radiusX = tag.getInt("radiusX");
        int radiusY = tag.getInt("radiusY");
        int radiusZ = tag.getInt("radiusZ");
        int blockCount = tag.getInt("blockCount");

        // Deserialization would reconstruct the maps
        // This is simplified for the interface
        return new Builder(dimension, center, radiusX, radiusY, radiusZ)
            .setBlockCount(blockCount)
            .build();
    }

    /**
     * Builder pattern for RegionSnapshot
     */
    public static class Builder {
        private final ResourceLocation dimension;
        private final BlockPos centerPosition;
        private final int radiusX;
        private final int radiusY;
        private final int radiusZ;
        private final Map<BlockPos, BlockState> blockStates;
        private final Map<BlockPos, CompoundTag> tileEntities;
        private final Set<GenieAnchor> anchorsInRegion;
        private int blockCount;
        private long timestamp;

        public Builder(ResourceLocation dimension, BlockPos center, int radiusX, int radiusY, int radiusZ) {
            this.dimension = dimension;
            this.centerPosition = center;
            this.radiusX = radiusX;
            this.radiusY = radiusY;
            this.radiusZ = radiusZ;
            this.blockStates = new HashMap<>();
            this.tileEntities = new HashMap<>();
            this.anchorsInRegion = new HashSet<>();
            this.blockCount = 0;
            this.timestamp = System.currentTimeMillis();
        }

        public Builder setBlockCount(int count) {
            this.blockCount = count;
            return this;
        }

        public Builder setTimestamp(long timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public RegionSnapshot capture(ServerLevel level) {
            int minX = centerPosition.getX() - radiusX;
            int maxX = centerPosition.getX() + radiusX;
            int minY = Math.max(0, centerPosition.getY() - radiusY);
            int maxY = Math.min(255, centerPosition.getY() + radiusY);
            int minZ = centerPosition.getZ() - radiusZ;
            int maxZ = centerPosition.getZ() + radiusZ;

            AtomicInteger capturedCount = new AtomicInteger(0);

            for (int x = minX; x <= maxX; x++) {
                for (int z = minZ; z <= maxZ; z++) {
                    for (int y = minY; y <= maxY; y++) {
                        BlockPos pos = new BlockPos(x, y, z);
                        BlockState state = level.getBlockState(pos);

                        if (!state.isAir()) {
                            blockStates.put(pos, state);
                            capturedCount.incrementAndGet();

                            // Check for tile entities
                            if (level.getBlockEntity(pos) != null) {
                                CompoundTag tileTag = new CompoundTag();
                                tileTag.putString("id", level.getBlockEntity(pos).getType().getRegistryName().toString());
                                tileEntities.put(pos, tileTag);
                            }
                        }
                    }
                }
            }

            // Find anchors in region
            level.players().forEach(player -> {
                if (player instanceof GenieAnchor anchor) {
                    BlockPos anchorPos = anchor.blockPosition();
                    if (anchorPos.getX() >= minX && anchorPos.getX() <= maxX &&
                        anchorPos.getY() >= minY && anchorPos.getY() <= maxY &&
                        anchorPos.getZ() >= minZ && anchorPos.getZ() <= maxZ) {
                        anchorsInRegion.add(anchor);
                    }
                }
            });

            this.blockCount = capturedCount.get();
            return new RegionSnapshot(this);
        }

        public RegionSnapshot build() {
            return new RegionSnapshot(this);
        }
    }
}
