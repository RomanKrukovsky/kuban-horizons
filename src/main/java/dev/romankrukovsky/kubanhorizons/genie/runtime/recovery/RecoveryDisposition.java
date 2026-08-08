package dev.romankrukovsky.kubanhorizons.genie.runtime.recovery;

public enum RecoveryDisposition {
    SAFE_TO_IGNORE,
    VERIFY_COMMIT,
    NEEDS_ROLLBACK,
    FAILED_SAFE
}
