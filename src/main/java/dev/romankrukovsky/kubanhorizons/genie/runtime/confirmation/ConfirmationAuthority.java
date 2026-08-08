package dev.romankrukovsky.kubanhorizons.genie.runtime.confirmation;

import dev.romankrukovsky.kubanhorizons.genie.runtime.preview.RestorePreview;
import java.time.Instant;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/** Выпускает и погашает одноразовые подтверждения. LLM не может создать их. */
public final class ConfirmationAuthority {
    private final Set<UUID> issued = new HashSet<>();
    private final UUID secret = UUID.randomUUID();

    public synchronized ConfirmedRestore issue(UUID actorId, RestorePreview preview, Instant now) {
        Objects.requireNonNull(actorId, "actorId");
        Objects.requireNonNull(preview, "preview");
        Objects.requireNonNull(now, "now");
        if (!preview.actorId().equals(actorId) || !preview.expiresAt().isAfter(now)) {
            throw new IllegalArgumentException("preview is stale or belongs to another actor");
        }
        UUID id = UUID.randomUUID();
        issued.add(id);
        return new ConfirmedRestore(id, preview, now, proof(id, actorId, preview));
    }

    public synchronized boolean consume(ConfirmedRestore confirmation, UUID actorId, Instant now) {
        Objects.requireNonNull(confirmation, "confirmation");
        return confirmation.preview().actorId().equals(actorId)
                && confirmation.preview().expiresAt().isAfter(now)
                && MessageDigest.isEqual(
                        confirmation.authorityProof().getBytes(StandardCharsets.US_ASCII),
                        proof(confirmation.confirmationId(), actorId, confirmation.preview())
                                .getBytes(StandardCharsets.US_ASCII))
                && issued.remove(confirmation.confirmationId());
    }

    private String proof(UUID confirmationId, UUID actorId, RestorePreview preview) {
        try {
            String material = secret + "|" + confirmationId + "|" + actorId + "|"
                    + preview.previewDigest() + "|" + preview.snapshotDigest() + "|"
                    + preview.selection().dimension() + "|" + preview.selection().min().asLong()
                    + "|" + preview.selection().max().asLong() + "|" + preview.expiresAt();
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(material.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
