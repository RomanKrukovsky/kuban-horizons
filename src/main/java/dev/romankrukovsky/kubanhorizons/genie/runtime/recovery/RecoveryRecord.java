package dev.romankrukovsky.kubanhorizons.genie.runtime.recovery;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

public record RecoveryRecord(
        UUID transactionId,
        long sequence,
        Instant recordedAt,
        TransactionState state,
        String payloadDigest,
        Optional<AffectedScope> scope
) {
    private static final Pattern SHA_256 = Pattern.compile("[0-9a-f]{64}");

    public RecoveryRecord {
        Objects.requireNonNull(transactionId, "transactionId");
        Objects.requireNonNull(recordedAt, "recordedAt");
        Objects.requireNonNull(state, "state");
        Objects.requireNonNull(payloadDigest, "payloadDigest");
        Objects.requireNonNull(scope, "scope");
        if (sequence < 0) {
            throw new IllegalArgumentException("sequence must not be negative");
        }
        if (!SHA_256.matcher(payloadDigest).matches()) {
            throw new IllegalArgumentException("payloadDigest must be lowercase SHA-256 hex");
        }
    }

    public RecoveryRecord(UUID transactionId, long sequence, Instant recordedAt,
                          TransactionState state, String payloadDigest) {
        this(transactionId, sequence, recordedAt, state, payloadDigest, Optional.empty());
    }

    public RecoveryRecord(UUID transactionId, long sequence, Instant recordedAt,
                          TransactionState state, String payloadDigest, AffectedScope scope) {
        this(transactionId, sequence, recordedAt, state, payloadDigest,
                Optional.of(Objects.requireNonNull(scope, "scope")));
    }
}
