package dev.romankrukovsky.kubanhorizons.genie.vessel;

/**
 * SharedRule defines the coordination behavior for a VesselSchool.
 *
 * <p>Each rule determines how school members respond to events affecting
 * the collective. Rules are set at school formation and can be changed
 * through ritual or contract negotiation.</p>
 */
public enum SharedRule {

    /**
     * All members prioritize protecting the school leader.
     * When the leader is threatened or damaged, members converge
     * and apply defensive modifiers.
     */
    PROTECT_LEADER,

    /**
     * Temperament changes in one member propagate to all members.
     * The school maintains emotional synchrony, sharing mood and
     * behavioral tendencies.
     */
    SHARED_TEMPERAMENT,

    /**
     * Contract obligations are shared across all members.
     * Breach or fulfillment affects the entire school, distributing
     * consequences and rewards.
     */
    CONTRACT_MANDATORY
}
