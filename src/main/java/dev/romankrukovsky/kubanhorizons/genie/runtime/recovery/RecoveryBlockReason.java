package dev.romankrukovsky.kubanhorizons.genie.runtime.recovery;

public enum RecoveryBlockReason {
    STARTUP_UNCLASSIFIED,
    OVERLAPPING_RECOVERY,
    FAILED_SAFE,
    LEGACY_UNKNOWN_SCOPE,
    CORRUPT_TAIL_UNCERTAINTY
}
