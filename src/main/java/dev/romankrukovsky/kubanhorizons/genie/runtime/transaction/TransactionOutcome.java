package dev.romankrukovsky.kubanhorizons.genie.runtime.transaction;

public enum TransactionOutcome {
    COMPLETED,
    COMPLETED_WITH_WARNINGS,
    STALE_PREVIEW,
    RESOURCE_LIMIT,
    COMPATIBILITY_BLOCKED,
    REJECTED,
    ROLLED_BACK,
    FAILED_SAFE
}
