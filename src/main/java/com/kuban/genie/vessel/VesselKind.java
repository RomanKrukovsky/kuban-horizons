package com.kuban.genie.vessel;

/**
 * Enum representing the 5 types of genie vessels.
 * Each vessel type has unique properties and capabilities.
 */
public enum VesselKind {
    /**
     * Genie Lamp - Classic vessel that stores and releases genies.
     * Properties: High capacity, can store multiple wishes, portable.
     */
    LAMP("lamp", 5, true, true, false),

    /**
     * Magic Mirror - Smart mirror that acts as a communication device and portal.
     * Properties: Two-way communication, can show distant locations, stores memories.
     */
    MIRROR("mirror", 3, false, true, true),

    /**
     * Magic Ring - Personal wearable vessel that follows the owner.
     * Properties: Always in inventory, teleports with owner, limited capacity.
     */
    RING("ring", 2, true, false, false),

    /**
     * Kuban Jug - Large decorative vessel that can be placed in the world.
     * Properties: Block-based, can be broken and moved, medium capacity.
     */
    JUG("jug", 4, false, false, false),

    /**
     * Music Box - Special vessel that stores wishes as melodies.
     * Properties: Can play wishes as music, aesthetic, limited functionality.
     */
    MUSIC_BOX("music_box", 1, true, false, true);

    private final String name;
    private final int maxWishes;
    private final boolean portable;
    private final boolean twoWay;
    private final boolean aesthetic;

    VesselKind(String name, int maxWishes, boolean portable, boolean twoWay, boolean aesthetic) {
        this.name = name;
        this.maxWishes = maxWishes;
        this.portable = portable;
        this.twoWay = twoWay;
        this.aesthetic = aesthetic;
    }

    public String getName() {
        return name;
    }

    public int getMaxWishes() {
        return maxWishes;
    }

    public boolean isPortable() {
        return portable;
    }

    public boolean isTwoWay() {
        return twoWay;
    }

    public boolean isAesthetic() {
        return aesthetic;
    }

    /**
     * Get vessel kind by name
     */
    public static VesselKind byName(String name) {
        for (VesselKind kind : values()) {
            if (kind.name.equalsIgnoreCase(name)) {
                return kind;
            }
        }
        return LAMP; // Default fallback
    }

    /**
     * Get vessel kind by ordinal
     */
    public static VesselKind byOrdinal(int ordinal) {
        if (ordinal >= 0 && ordinal < values().length) {
            return values()[ordinal];
        }
        return LAMP;
    }

    /**
     * Check if vessel can store wishes
     */
    public boolean canStoreWishes() {
        return this != MUSIC_BOX; // Music boxes store wishes as melodies
    }

    /**
     * Get vessel display name
     */
    public String getDisplayName() {
        return switch (this) {
            case LAMP -> "Genie Lamp";
            case MIRROR -> "Magic Mirror";
            case RING -> "Magic Ring";
            case JUG -> "Kuban Jug";
            case MUSIC_BOX -> "Music Box";
        };
    }

    /**
     * Get vessel description
     */
    public String getDescription() {
        return switch (this) {
            case LAMP -> "A classic vessel that stores and releases genies. Can hold multiple wishes.";
            case MIRROR -> "A magical mirror that acts as a communication device and portal to other dimensions.";
            case RING -> "A personal wearable vessel that follows its owner and stores a few wishes.";
            case JUG -> "A large decorative vessel that can be placed in the world. Stores wishes when filled with water.";
            case MUSIC_BOX -> "A special vessel that stores wishes as melodies. Can play wishes as music.";
        };
    }
}
