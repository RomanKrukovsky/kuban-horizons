package dev.romankrukovsky.kubanhorizons.genie.runtime.confirmation;

import dev.romankrukovsky.kubanhorizons.genie.runtime.preview.MiniaturizePreview;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Одноразовое серверное право на сжатие точной области. */
public record ConfirmedMiniaturize(UUID confirmationId, MiniaturizePreview preview,
                                  Instant issuedAt) {
    public ConfirmedMiniaturize {
        Objects.requireNonNull(confirmationId, "confirmationId");
        Objects.requireNonNull(preview, "preview");
        Objects.requireNonNull(issuedAt, "issuedAt");
    }
}
