package dev.romankrukovsky.kubanhorizons.genie.runtime.preview;

import dev.romankrukovsky.kubanhorizons.genie.runtime.capability.CapabilityRisk;
import dev.romankrukovsky.kubanhorizons.genie.runtime.selection.RegionSelection;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Неизменяемый preview сжатия области в предмет. */
public record MiniaturizePreview(UUID previewId, UUID actorId, RegionSelection selection,
                                int nonAirBlocks, int blockEntities, int entities,
                                CapabilityRisk risk, String currentStateDigest,
                                String previewDigest, Instant expiresAt) {
    public MiniaturizePreview {
        Objects.requireNonNull(previewId, "previewId");
        Objects.requireNonNull(actorId, "actorId");
        Objects.requireNonNull(selection, "selection");
        Objects.requireNonNull(risk, "risk");
        Objects.requireNonNull(currentStateDigest, "currentStateDigest");
        Objects.requireNonNull(previewDigest, "previewDigest");
        Objects.requireNonNull(expiresAt, "expiresAt");
        if (!currentStateDigest.matches("[0-9a-f]{64}") || !previewDigest.matches("[0-9a-f]{64}")) {
            throw new IllegalArgumentException("miniaturize preview digests must be SHA-256");
        }
        if (nonAirBlocks < 0 || blockEntities < 0 || entities < 0) {
            throw new IllegalArgumentException("preview counts must not be negative");
        }
    }
}
