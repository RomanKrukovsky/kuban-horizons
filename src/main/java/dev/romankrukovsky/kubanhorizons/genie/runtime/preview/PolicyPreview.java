package dev.romankrukovsky.kubanhorizons.genie.runtime.preview;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record PolicyPreview(UUID previewId, UUID actorId, String ruleId,
                            boolean beforeValue, boolean targetValue,
                            Instant expiresAt) {
    public PolicyPreview {
        Objects.requireNonNull(previewId, "previewId");
        Objects.requireNonNull(actorId, "actorId");
        Objects.requireNonNull(ruleId, "ruleId");
        Objects.requireNonNull(expiresAt, "expiresAt");
    }
}
