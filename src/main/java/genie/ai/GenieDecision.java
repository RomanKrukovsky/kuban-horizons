package genie.ai;

/**
 * Possible decisions a genie can make.
 * Each decision has a utility score based on current situation.
 */
public enum GenieDecision {
    /**
     * Rescue the owner from danger
     */
    RESCUE_OWNER,

    /**
     * Intercept incoming projectiles
     */
    INTERCEPT_PROJECTILE,

    /**
     * Repel hostile threats
     */
    REPEL_THREAT,

    /**
     * Follow the owner
     */
    FOLLOW_OWNER,

    /**
     * Guard current position
     */
    GUARD_POSITION,

    /**
     * Scout ahead of owner
     */
    SCOUT_AHEAD,

    /**
     * Wait for commands
     */
    WAIT,

    /**
     * Use special ability
     */
    USE_ABILITY,

    /**
     * Teleport to safety
     */
    TELEPORT_SAFE,

    /**
     * Create distraction
     */
    CREATE_DISTRACTION
}
