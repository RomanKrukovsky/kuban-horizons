package dev.romankrukovsky.kubanhorizons.genie.runtime.transaction;

import dev.romankrukovsky.kubanhorizons.genie.runtime.selection.RegionSelection;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Долговечная связь journal-транзакции с target и before-image. */
public record TransactionManifest(
        UUID transactionId,
        UUID actorId,
        UUID targetSnapshotId,
        UUID beforeImageId,
        RegionSelection selection,
        String targetDigest,
        String beforeDigest,
        String previewDigest,
        Instant createdAt
) {
    public TransactionManifest {
        Objects.requireNonNull(transactionId, "transactionId");
        Objects.requireNonNull(actorId, "actorId");
        Objects.requireNonNull(targetSnapshotId, "targetSnapshotId");
        Objects.requireNonNull(beforeImageId, "beforeImageId");
        Objects.requireNonNull(selection, "selection");
        requireDigest(targetDigest, "targetDigest");
        requireDigest(beforeDigest, "beforeDigest");
        requireDigest(previewDigest, "previewDigest");
        Objects.requireNonNull(createdAt, "createdAt");
    }

    private static void requireDigest(String digest, String field) {
        Objects.requireNonNull(digest, field);
        if (!digest.matches("[0-9a-f]{64}")) {
            throw new IllegalArgumentException(field + " must be SHA-256");
        }
    }
}
