package dev.romankrukovsky.kubanhorizons.genie.runtime.confirmation;

import dev.romankrukovsky.kubanhorizons.genie.runtime.preview.StructureMovePreview;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record ConfirmedStructureMove(UUID confirmationId, StructureMovePreview preview,
                                     Instant issuedAt) {
    public ConfirmedStructureMove {
        Objects.requireNonNull(confirmationId, "confirmationId");
        Objects.requireNonNull(preview, "preview");
        Objects.requireNonNull(issuedAt, "issuedAt");
    }
}
