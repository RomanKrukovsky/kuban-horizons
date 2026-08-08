package dev.romankrukovsky.kubanhorizons.genie.runtime.selection;

import java.util.Objects;
import java.util.regex.Pattern;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;

/** Неизменяемая включительная область в одном измерении. */
public record RegionSelection(String dimension, BlockPos min, BlockPos max) {
    public static final int HARD_MAX_CHUNKS = 256;
    public static final long HARD_MAX_BLOCKS = 131_072L;
    private static final Pattern DIMENSION = Pattern.compile("[a-z0-9_.-]+:[a-z0-9_./-]+");

    public RegionSelection {
        Objects.requireNonNull(dimension, "dimension");
        Objects.requireNonNull(min, "min");
        Objects.requireNonNull(max, "max");
        if (!DIMENSION.matcher(dimension).matches()) {
            throw new IllegalArgumentException("dimension must be a lowercase namespaced identifier");
        }
        int minX = Math.min(min.getX(), max.getX());
        int minY = Math.min(min.getY(), max.getY());
        int minZ = Math.min(min.getZ(), max.getZ());
        int maxX = Math.max(min.getX(), max.getX());
        int maxY = Math.max(min.getY(), max.getY());
        int maxZ = Math.max(min.getZ(), max.getZ());
        min = new BlockPos(minX, minY, minZ);
        max = new BlockPos(maxX, maxY, maxZ);
        long volume = Math.multiplyExact(Math.multiplyExact((long) maxX - minX + 1L,
                (long) maxY - minY + 1L), (long) maxZ - minZ + 1L);
        int chunks = Math.multiplyExact(Math.floorDiv(maxX, 16) - Math.floorDiv(minX, 16) + 1,
                Math.floorDiv(maxZ, 16) - Math.floorDiv(minZ, 16) + 1);
        if (volume > HARD_MAX_BLOCKS || chunks > HARD_MAX_CHUNKS) {
            throw new IllegalArgumentException("selection exceeds safe limits");
        }
    }

    public long volume() {
        return Math.multiplyExact(Math.multiplyExact((long) max.getX() - min.getX() + 1L,
                (long) max.getY() - min.getY() + 1L), (long) max.getZ() - min.getZ() + 1L);
    }

    public int chunkCount() {
        ChunkPos first = ChunkPos.containing(min);
        ChunkPos last = ChunkPos.containing(max);
        return Math.multiplyExact(last.x() - first.x() + 1, last.z() - first.z() + 1);
    }

    public boolean contains(BlockPos pos) {
        return pos.getX() >= min.getX() && pos.getX() <= max.getX()
                && pos.getY() >= min.getY() && pos.getY() <= max.getY()
                && pos.getZ() >= min.getZ() && pos.getZ() <= max.getZ();
    }

    public Iterable<BlockPos> positions() {
        return BlockPos.betweenClosed(min, max);
    }
}
