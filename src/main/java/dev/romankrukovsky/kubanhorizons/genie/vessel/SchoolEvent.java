package dev.romankrukovsky.kubanhorizons.genie.vessel;

/**
 * SchoolEvent represents events that can trigger school-wide behavior modifications.
 */
public enum SchoolEvent {

    /**
     * A threat has been detected near a school member.
     */
    THREAT_DETECTED,

    /**
     * The school leader has taken damage.
     */
    LEADER_DAMAGED,

    /**
     * A member's temperament has shifted significantly.
     */
    TEMPERAMENT_SHIFT,

    /**
     * A contract obligation has been breached.
     */
    CONTRACT_BREACH,

    /**
     * A contract has been successfully fulfilled.
     */
    CONTRACT_FULFILLED,

    /**
     * A member has been added or removed from the school.
     */
    MEMBERSHIP_CHANGE,

    /**
     * Leadership has been transferred.
     */
    LEADERSHIP_TRANSFER
}
