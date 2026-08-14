package dev.romankrukovsky.kubanhorizons.genie.vessel;

/**
 * BehaviorModifier represents active behavioral changes applied to school members
 * in response to school events.
 */
public enum BehaviorModifier {

    /**
     * Members form a protective formation around the leader.
     */
    PROTECTIVE_FORMATION,

    /**
     * Members adopt a defensive combat stance.
     */
    DEFENSIVE_STANCE,

    /**
     * Members synchronize their temperament with the affected member.
     */
    TEMPERAMENT_SYNC,

    /**
     * Members enforce contract terms collectively.
     */
    CONTRACT_ENFORCEMENT,

    /**
     * Members distribute fulfillment rewards across the school.
     */
    REWARD_DISTRIBUTION,

    /**
     * Members enter a heightened alert state.
     */
    ALERT_STATUS,

    /**
     * Members share magical energy reserves.
     */
    ENERGY_SHARING
}
