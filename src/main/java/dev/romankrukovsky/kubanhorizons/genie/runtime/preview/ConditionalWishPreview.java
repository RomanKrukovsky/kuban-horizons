package dev.romankrukovsky.kubanhorizons.genie.runtime.preview;

import java.time.Instant;
import java.util.UUID;

/** Preview for a conditional wish (event-condition-action rule) before confirmation. */
public record ConditionalWishPreview(
    UUID previewId,
    UUID actorId,
    UUID ownerId,
    String condition,
    String action,
    Instant expiresAt,
    String digest
) {
    public ConditionalWishPreview {
        if (previewId == null || actorId == null || ownerId == null) {
            throw new IllegalArgumentException("IDs cannot be null");
        }
        if (condition == null || action == null || digest == null || expiresAt == null) {
            throw new IllegalArgumentException("Fields cannot be null");
        }
    }
}
