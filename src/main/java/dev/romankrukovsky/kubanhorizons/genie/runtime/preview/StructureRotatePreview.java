package dev.romankrukovsky.kubanhorizons.genie.runtime.preview;

import dev.romankrukovsky.kubanhorizons.genie.runtime.selection.RegionSelection;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import net.minecraft.world.level.block.Rotation;

/** Preview поворота выделенной структуры вокруг центра области. */
public record StructureRotatePreview(UUID previewId, UUID actorId, RegionSelection selection,
                                     Rotation rotation, int changedBlocks, String currentDigest,
                                     String targetDigest, String previewDigest, Instant expiresAt) {
    public StructureRotatePreview {
        Objects.requireNonNull(previewId, "previewId");
        Objects.requireNonNull(actorId, "actorId");
        Objects.requireNonNull(selection, "selection");
        Objects.requireNonNull(rotation, "rotation");
        Objects.requireNonNull(expiresAt, "expiresAt");
        for (String digest : java.util.List.of(currentDigest, targetDigest, previewDigest)) {
            if (digest == null || !digest.matches("[0-9a-f]{64}")) {
                throw new IllegalArgumentException("structure rotate digest must be SHA-256");
            }
        }
        if (changedBlocks < 0) {
            throw new IllegalArgumentException("changedBlocks must not be negative");
        }
    }
}
