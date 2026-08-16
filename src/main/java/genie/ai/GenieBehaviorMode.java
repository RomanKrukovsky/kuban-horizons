package genie.ai;

/**
 * Behavior modes for Kuban Genie.
 * Determines how the genie follows or interacts with the owner.
 */
public enum GenieBehaviorMode {
    /**
     * Genie follows owner closely
     */
    FOLLOW,

    /**
     * Genie stays in place
     */
    STAY,

    /**
     * Genie guards current position
     */
    GUARD,

    /**
     * Genie scouts ahead of owner
     */
    SCOUT
}
