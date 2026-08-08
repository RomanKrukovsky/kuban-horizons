package dev.romankrukovsky.kubanhorizons.genie.runtime.preview;

import dev.romankrukovsky.kubanhorizons.genie.runtime.selection.RegionSelection;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record WordPreview(UUID previewId, UUID actorId, RegionSelection selection, String word,
                          int changedBlocks, String currentDigest, String targetDigest,
                          String previewDigest, Instant expiresAt) {
    public WordPreview {
        Objects.requireNonNull(previewId, "previewId");
        Objects.requireNonNull(actorId, "actorId");
        Objects.requireNonNull(selection, "selection");
        Objects.requireNonNull(word, "word");
        Objects.requireNonNull(expiresAt, "expiresAt");
    }
}
