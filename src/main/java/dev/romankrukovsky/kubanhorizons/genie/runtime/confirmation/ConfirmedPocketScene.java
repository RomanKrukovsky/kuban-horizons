package dev.romankrukovsky.kubanhorizons.genie.runtime.confirmation;

import dev.romankrukovsky.kubanhorizons.genie.runtime.preview.PocketScenePreview;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Одноразовое право на создание точной временной сцены. */
public record ConfirmedPocketScene(UUID confirmationId, PocketScenePreview preview,
                                   Instant issuedAt) {
    public ConfirmedPocketScene {
        Objects.requireNonNull(confirmationId, "confirmationId");
        Objects.requireNonNull(preview, "preview");
        Objects.requireNonNull(issuedAt, "issuedAt");
    }
}
