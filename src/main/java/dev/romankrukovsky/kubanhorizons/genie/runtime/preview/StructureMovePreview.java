package dev.romankrukovsky.kubanhorizons.genie.runtime.preview;

import dev.romankrukovsky.kubanhorizons.genie.runtime.selection.RegionSelection;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Preview переноса ограниченной структуры вверх. */
public record StructureMovePreview(UUID previewId, UUID actorId, RegionSelection selection,
                                   int changedBlocks, String currentDigest, String targetDigest,
                                   String previewDigest, Instant expiresAt) {
    public StructureMovePreview {
        Objects.requireNonNull(previewId, "previewId");
        Objects.requireNonNull(actorId, "actorId");
        Objects.requireNonNull(selection, "selection");
        Objects.requireNonNull(expiresAt, "expiresAt");
        for (String digest : java.util.List.of(currentDigest, targetDigest, previewDigest)) {
            if (digest == null || !digest.matches("[0-9a-f]{64}")) {
                throw new IllegalArgumentException("structure move digest must be SHA-256");
            }
        }
        if (changedBlocks < 0) {
            throw new IllegalArgumentException("changedBlocks must not be negative");
        }
    }
}
