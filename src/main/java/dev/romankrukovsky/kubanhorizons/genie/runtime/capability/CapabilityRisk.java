package dev.romankrukovsky.kubanhorizons.genie.runtime.capability;

public enum CapabilityRisk {
    NONE,
    LOW,
    ELEVATED,
    HIGH,
    CRITICAL;

    public boolean requiresConfirmation() {
        return ordinal() >= ELEVATED.ordinal();
    }
}
