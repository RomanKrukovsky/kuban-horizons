package dev.romankrukovsky.kubanhorizons.genie.runtime.transaction;

import java.util.Objects;
import java.util.UUID;

/** Итог транзакции отделён от реплики джиннии. */
public record TransactionReport(UUID transactionId, TransactionOutcome outcome,
                                int changedBlocks, String detail) {
    public TransactionReport {
        Objects.requireNonNull(transactionId, "transactionId");
        Objects.requireNonNull(outcome, "outcome");
        Objects.requireNonNull(detail, "detail");
        if (changedBlocks < 0) {
            throw new IllegalArgumentException("changedBlocks must not be negative");
        }
    }
}
