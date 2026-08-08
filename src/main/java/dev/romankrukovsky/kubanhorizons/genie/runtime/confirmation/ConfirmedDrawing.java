package dev.romankrukovsky.kubanhorizons.genie.runtime.confirmation;

import dev.romankrukovsky.kubanhorizons.genie.runtime.preview.DrawingPreview;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record ConfirmedDrawing(UUID confirmationId, DrawingPreview preview, Instant issuedAt) {
    public ConfirmedDrawing {
        Objects.requireNonNull(confirmationId, "confirmationId");
        Objects.requireNonNull(preview, "preview");
        Objects.requireNonNull(issuedAt, "issuedAt");
    }
}
