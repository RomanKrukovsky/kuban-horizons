package dev.romankrukovsky.kubanhorizons.genie.runtime.recovery;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public record BlockedTransaction(
        UUID transactionId,
        Optional<AffectedScope> scope,
        TransactionState finalState,
        RecoveryDisposition disposition,
        RecoveryBlockReason reason
) {
    public BlockedTransaction {
        Objects.requireNonNull(transactionId, "transactionId");
        Objects.requireNonNull(scope, "scope");
        Objects.requireNonNull(finalState, "finalState");
        Objects.requireNonNull(disposition, "disposition");
        Objects.requireNonNull(reason, "reason");
    }
}
