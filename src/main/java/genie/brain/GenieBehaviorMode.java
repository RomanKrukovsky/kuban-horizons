package genie.brain;

/**
 * Enum representing different behavior modes for a Genie
 * Controls how the genie interacts with the owner and environment
 */
public enum GenieBehaviorMode {
    /**
     * Genie follows owner closely at a short distance
     * Provides constant protection and support
     */
    FOLLOW(
        "follow",
        "Genie follows you closely, ready to assist at any moment.",
        1.0f,  // Movement speed factor
        2.0f,  // Follow distance
        true   // Can be interrupted by threats
    ),

    /**
     * Genie stays in one place, guarding the area
     * Provides defense from a fixed position
     */
    STAY(
        "stay",
        "Genie stays in place, guarding the area around you.",
        0.8f,
        3.0f,
        false  // Less responsive to movement
    ),

    /**
     * Genie guards owner from a distance
     * Maintains position while monitoring for threats
     */
    GUARD(
        "guard",
        "Genie keeps watch nearby, protecting you from threats.",
        0.9f,
        4.0f,
        true
    ),

    /**
     * Genie explores ahead of owner
     * Scouting for resources, dangers, and points of interest
     */
    SCOUT(
        "scout",
        "Genie scouts ahead, exploring the path before you.",
        1.3f,  // Higher speed for scouting
        6.0f,
        false
    ),

    /**
     * Genie is temporarily inactive
     * Won't follow or react to threats
     */
    PASSIVE(
        "passive",
        "Genie is passive and won't follow or react to threats.",
        0.0f,
        0.0f,
        false
    );

    private final String modeName;
    private final String description;
    private final float movementSpeedFactor;
    private final float preferredDistance;
    private final boolean responsiveToThreats;

    GenieBehaviorMode(String modeName, String description, float movementSpeedFactor,
                      float preferredDistance, boolean responsiveToThreats) {
        this.modeName = modeName;
        this.description = description;
        this.movementSpeedFactor = movementSpeedFactor;
        this.preferredDistance = preferredDistance;
        this.responsiveToThreats = responsiveToThreats;
    }

    /**
     * Get mode name
     */
    public String getModeName() {
        return modeName;
    }

    /**
     * Get description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Get movement speed factor
     */
    public float getMovementSpeedFactor() {
        return movementSpeedFactor;
    }

    /**
     * Get preferred distance from owner
     */
    public float getPreferredDistance() {
        return preferredDistance;
    }

    /**
     * Check if responsive to threats
     */
    public boolean isResponsiveToThreats() {
        return responsiveToThreats;
    }

    /**
     * Get mode from name
     */
    public static GenieBehaviorMode fromName(String name) {
        for (GenieBehaviorMode mode : values()) {
            if (mode.modeName.equalsIgnoreCase(name)) {
                return mode;
            }
        }
        return FOLLOW; // Default to follow
    }

    /**
     * Get next mode in sequence
     */
    public GenieBehaviorMode next() {
        int nextIndex = (this.ordinal() + 1) % values().length;
        return values()[nextIndex];
    }

    /**
     * Get previous mode in sequence
     */
    public GenieBehaviorMode previous() {
        int prevIndex = (this.ordinal() - 1 + values().length) % values().length;
        return values()[prevIndex];
    }
}