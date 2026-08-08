package dev.romankrukovsky.kubanhorizons.genie.runtime.recovery;

import java.util.Objects;
import java.util.regex.Pattern;

public record AffectedScope(
        String dimension,
        int minChunkX,
        int minChunkZ,
        int maxChunkX,
        int maxChunkZ
) {
    private static final Pattern DIMENSION = Pattern.compile("[a-z0-9_.-]+:[a-z0-9_./-]+");

    public AffectedScope {
        Objects.requireNonNull(dimension, "dimension");
        if (!DIMENSION.matcher(dimension).matches()) {
            throw new IllegalArgumentException("dimension must be a lowercase namespaced identifier");
        }
        int firstChunkX = minChunkX;
        int firstChunkZ = minChunkZ;
        minChunkX = Math.min(firstChunkX, maxChunkX);
        minChunkZ = Math.min(firstChunkZ, maxChunkZ);
        maxChunkX = Math.max(firstChunkX, maxChunkX);
        maxChunkZ = Math.max(firstChunkZ, maxChunkZ);
    }

    public boolean overlaps(AffectedScope other) {
        Objects.requireNonNull(other, "other");
        return dimension.equals(other.dimension)
                && minChunkX <= other.maxChunkX && other.minChunkX <= maxChunkX
                && minChunkZ <= other.maxChunkZ && other.minChunkZ <= maxChunkZ;
    }
}
