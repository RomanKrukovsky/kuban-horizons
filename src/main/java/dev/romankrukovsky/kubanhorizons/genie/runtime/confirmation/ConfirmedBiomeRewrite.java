package dev.romankrukovsky.kubanhorizons.genie.runtime.confirmation;

import dev.romankrukovsky.kubanhorizons.genie.runtime.preview.BiomeRewritePreview;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record ConfirmedBiomeRewrite(UUID confirmationId, BiomeRewritePreview preview,
                                    Instant issuedAt) {
    public ConfirmedBiomeRewrite {
        Objects.requireNonNull(confirmationId, "confirmationId");
        Objects.requireNonNull(preview, "preview");
        Objects.requireNonNull(issuedAt, "issuedAt");
    }
}
