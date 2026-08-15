package dev.romankrukovsky.kubanhorizons.genie.state;

public enum ManifestationState {
    MANIFESTED,   // Fully present, normal behavior
    DISPERSED,    // Physical form dispersed (after heavy damage), can reform
    SEALED,       // Trapped in vessel
    BANISHED      // Exiled from current dimension/realm
}
