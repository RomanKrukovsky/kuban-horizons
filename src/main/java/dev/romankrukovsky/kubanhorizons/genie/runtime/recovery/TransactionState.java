package dev.romankrukovsky.kubanhorizons.genie.runtime.recovery;

public enum TransactionState {
    PREPARING,
    PREPARED,
    APPLYING,
    VERIFYING,
    ROLLING_BACK,
    COMMITTED,
    ROLLED_BACK,
    FAILED_SAFE,
    RETIRED
}
