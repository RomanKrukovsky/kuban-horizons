package dev.romankrukovsky.kubanhorizons.genie.runtime.confirmation;

import dev.romankrukovsky.kubanhorizons.genie.runtime.preview.WordPreview;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record ConfirmedWord(UUID confirmationId, WordPreview preview, Instant issuedAt) {
    public ConfirmedWord {
        Objects.requireNonNull(confirmationId, "confirmationId");
        Objects.requireNonNull(preview, "preview");
        Objects.requireNonNull(issuedAt, "issuedAt");
    }
}
