package dev.romankrukovsky.kubanhorizons.genie.runtime.confirmation;

import dev.romankrukovsky.kubanhorizons.genie.runtime.preview.PolicyPreview;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record ConfirmedPolicy(UUID confirmationId, PolicyPreview preview, Instant issuedAt) {
    public ConfirmedPolicy {
        Objects.requireNonNull(confirmationId, "confirmationId");
        Objects.requireNonNull(preview, "preview");
        Objects.requireNonNull(issuedAt, "issuedAt");
    }
}
