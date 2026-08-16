package genie.brain;

/**
 * Enum representing possible decisions a Genie can make
 * Each decision has a utility score and associated behavior
 */
public enum GenieDecision {
    /**
     * No active decision - default behavior
     */
    NONE(0, "none"),

    /**
     * Rescue the owner from danger
     */
    RESCUE_OWNER(1, "rescue_owner"),

    /**
     * Intercept incoming projectiles
     */
    INTERCEPT_PROJECTILE(2, "intercept_projectile"),

    /**
     * Repel threats to the owner
     */
    REPEL_THREAT(3, "repel_threat"),

    /**
     * Follow the owner closely
     */
    FOLLOW_OWNER(4, "follow_owner"),

    /**
     * Guard a specific position
     */
    GUARD_POSITION(5, "guard_position"),

    /**
     * Scout ahead of the owner
     */
    SCOUT_AHEAD(6, "scout_ahead"),

    /**
     * Investigate suspicious activity
     */
    INVESTIGATE(7, "investigate"),

    /**
     * Celebrate or perform special actions
     */
    CELEBRATE(8, "celebrate"),

    /**
     * Rest or conserve energy
     */
    REST(9, "rest");

    private final int id;
    private final String name;

    GenieDecision(int id, String name) {
        this.id = id;
        this.name = name;
    }

    /**
     * Get decision ID
     */
    public int getId() {
        return id;
    }

    /**
     * Get decision name
     */
    public String getName() {
        return name;
    }

    /**
     * Get decision from ID
     */
    public static GenieDecision fromId(int id) {
        for (GenieDecision decision : values()) {
            if (decision.id == id) {
                return decision;
            }
        }
        return NONE;
    }

    /**
     * Get decision from name
     */
    public static GenieDecision fromName(String name) {
        for (GenieDecision decision : values()) {
            if (decision.name.equalsIgnoreCase(name)) {
                return decision;
            }
        }
        return NONE;
    }

    /**
     * Check if decision is defensive
     */
    public boolean isDefensive() {
        return this == RESCUE_OWNER || this == INTERCEPT_PROJECTILE || this == REPEL_THREAT;
    }

    /**
     * Check if decision is movement-related
     */
    public boolean isMovement() {
        return this == FOLLOW_OWNER || this == SCOUT_AHEAD || this == GUARD_POSITION;
    }
}