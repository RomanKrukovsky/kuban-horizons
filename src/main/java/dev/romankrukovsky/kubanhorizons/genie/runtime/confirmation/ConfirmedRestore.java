package dev.romankrukovsky.kubanhorizons.genie.runtime.confirmation;

import dev.romankrukovsky.kubanhorizons.genie.runtime.preview.RestorePreview;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Одноразовый серверный пропуск к мутации, привязанный к точному preview. */
public record ConfirmedRestore(UUID confirmationId, RestorePreview preview, Instant issuedAt,
                               String authorityProof) {
    public ConfirmedRestore {
        Objects.requireNonNull(confirmationId, "confirmationId");
        Objects.requireNonNull(preview, "preview");
        Objects.requireNonNull(issuedAt, "issuedAt");
        Objects.requireNonNull(authorityProof, "authorityProof");
        if (!authorityProof.matches("[0-9a-f]{64}")) {
            throw new IllegalArgumentException("authorityProof must be SHA-256");
        }
    }
}
