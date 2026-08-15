package dev.romankrukovsky.kubanhorizons.genie.runtime.confirmation;

import dev.romankrukovsky.kubanhorizons.genie.runtime.preview.StructureRotatePreview;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record ConfirmedStructureRotate(UUID confirmationId, StructureRotatePreview preview,
                                       Instant issuedAt) {
    public ConfirmedStructureRotate {
        Objects.requireNonNull(confirmationId, "confirmationId");
        Objects.requireNonNull(preview, "preview");
        Objects.requireNonNull(issuedAt, "issuedAt");
    }
}
