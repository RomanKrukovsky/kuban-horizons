package dev.romankrukovsky.kubanhorizons.genie.runtime.confirmation;

import dev.romankrukovsky.kubanhorizons.genie.runtime.preview.ConditionalWishPreview;
import java.time.Instant;
import java.util.UUID;

/** One-time confirmation for a conditional wish rule. */
public record ConfirmedConditionalWish(
    UUID confirmationId,
    ConditionalWishPreview preview,
    Instant confirmedAt
) {
    public ConfirmedConditionalWish {
        if (confirmationId == null || preview == null || confirmedAt == null) {
            throw new IllegalArgumentException("Fields cannot be null");
        }
    }
}
