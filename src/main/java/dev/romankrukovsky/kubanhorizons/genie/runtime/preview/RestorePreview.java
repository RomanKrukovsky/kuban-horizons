package dev.romankrukovsky.kubanhorizons.genie.runtime.preview;

import dev.romankrukovsky.kubanhorizons.genie.runtime.capability.CapabilityRisk;
import dev.romankrukovsky.kubanhorizons.genie.runtime.selection.RegionSelection;
import dev.romankrukovsky.kubanhorizons.genie.runtime.snapshot.SnapshotId;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;

/** Неизменяемое описание точных последствий восстановления. */
public record RestorePreview(
        UUID previewId,
        SnapshotId snapshotId,
        String snapshotDigest,
        UUID actorId,
        RegionSelection selection,
        int changedBlocks,
        int changedBlockEntities,
        CapabilityRisk risk,
        String currentStateDigest,
        String previewDigest,
        Instant expiresAt
) {
    private static final Pattern DIGEST = Pattern.compile("[0-9a-f]{64}");

    public RestorePreview {
        Objects.requireNonNull(previewId, "previewId");
        Objects.requireNonNull(snapshotId, "snapshotId");
        validateDigest(snapshotDigest, "snapshotDigest");
        Objects.requireNonNull(actorId, "actorId");
        Objects.requireNonNull(selection, "selection");
        Objects.requireNonNull(risk, "risk");
        validateDigest(currentStateDigest, "currentStateDigest");
        validateDigest(previewDigest, "previewDigest");
        Objects.requireNonNull(expiresAt, "expiresAt");
        if (changedBlocks < 0 || changedBlockEntities < 0) {
            throw new IllegalArgumentException("preview counts must not be negative");
        }
    }

    private static void validateDigest(String value, String name) {
        Objects.requireNonNull(value, name);
        if (!DIGEST.matcher(value).matches()) {
            throw new IllegalArgumentException(name + " must be SHA-256");
        }
    }
}
