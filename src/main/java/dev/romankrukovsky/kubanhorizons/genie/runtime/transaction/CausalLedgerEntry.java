package dev.romankrukovsky.kubanhorizons.genie.runtime.transaction;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Связывает желание, целевой снимок и технический before-image для undo. */
public record CausalLedgerEntry(
        UUID transactionId,
        UUID actorId,
        UUID targetSnapshotId,
        UUID beforeImageId,
        String dimension,
        Instant committedAt,
        TransactionOutcome outcome
) {
    public CausalLedgerEntry {
        Objects.requireNonNull(transactionId, "transactionId");
        Objects.requireNonNull(actorId, "actorId");
        Objects.requireNonNull(targetSnapshotId, "targetSnapshotId");
        Objects.requireNonNull(beforeImageId, "beforeImageId");
        Objects.requireNonNull(dimension, "dimension");
        Objects.requireNonNull(committedAt, "committedAt");
        Objects.requireNonNull(outcome, "outcome");
    }
}
