package dev.romankrukovsky.kubanhorizons.genie.runtime.preview;

import dev.romankrukovsky.kubanhorizons.genie.runtime.capability.CapabilityRisk;
import dev.romankrukovsky.kubanhorizons.genie.runtime.selection.RegionSelection;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Preview временной карманной сцены, привязанный к текущему состоянию области. */
public record PocketScenePreview(UUID previewId, UUID actorId, RegionSelection selection,
                                 int changedBlocks, int durationTicks, CapabilityRisk risk,
                                 String currentStateDigest, String targetDigest,
                                 String previewDigest, Instant expiresAt) {
    public PocketScenePreview {
        Objects.requireNonNull(previewId, "previewId");
        Objects.requireNonNull(actorId, "actorId");
        Objects.requireNonNull(selection, "selection");
        Objects.requireNonNull(risk, "risk");
        Objects.requireNonNull(expiresAt, "expiresAt");
        for (String digest : java.util.List.of(currentStateDigest, targetDigest, previewDigest)) {
            if (digest == null || !digest.matches("[0-9a-f]{64}")) {
                throw new IllegalArgumentException("pocket scene digest must be SHA-256");
            }
        }
        if (changedBlocks < 0 || durationTicks < 1) {
            throw new IllegalArgumentException("invalid pocket scene preview counts");
        }
    }
}
