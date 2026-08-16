package com.kuban.genie.vessel;

/**
 * Enum representing the 5 schools of magic for vessels.
 * Each school has unique properties and determines vessel capabilities.
 */
public enum VesselSchool {
    /**
     * Fire School - Enhances destruction and energy-based wishes.
     * Properties: Higher wish power, faster wish execution, but higher energy cost.
     */
    FIRE("fire", 1.5f, 0.8f, 1.2f, 0xFF5500),

    /**
     * Water School - Enhances healing and fluid-based wishes.
     * Properties: Lower energy cost, healing effects, but slower wish execution.
     */
    WATER("water", 0.7f, 1.2f, 0.9f, 0x0055FF),

    /**
     * Earth School - Enhances structural and creation wishes.
     * Properties: Higher durability, better material control, but slower execution.
     */
    EARTH("earth", 1.0f, 1.0f, 1.0f, 0x55AA00),

    /**
     * Air School - Enhances speed and mobility wishes.
     * Properties: Faster wish execution, higher mobility, but lower wish power.
     */
    AIR("air", 0.8f, 1.5f, 1.1f, 0xAAAAFF),

    /**
     * Arcane School - Balanced school for all wish types.
     * Properties: Neutral stats, versatile, good for beginners.
     */
    ARCANE("arcane", 1.0f, 1.0f, 1.0f, 0xAA00AA);

    private final String name;
    private final float wishPowerMultiplier;
    private final float wishSpeedMultiplier;
    private final float energyEfficiency;
    private final int color;

    VesselSchool(String name, float wishPowerMultiplier, float wishSpeedMultiplier,
                 float energyEfficiency, int color) {
        this.name = name;
        this.wishPowerMultiplier = wishPowerMultiplier;
        this.wishSpeedMultiplier = wishSpeedMultiplier;
        this.energyEfficiency = energyEfficiency;
        this.color = color;
    }

    public String getName() {
        return name;
    }

    public float getWishPowerMultiplier() {
        return wishPowerMultiplier;
    }

    public float getWishSpeedMultiplier() {
        return wishSpeedMultiplier;
    }

    public float getEnergyEfficiency() {
        return energyEfficiency;
    }

    public int getColor() {
        return color;
    }

    /**
     * Get school by name
     */
    public static VesselSchool byName(String name) {
        for (VesselSchool school : values()) {
            if (school.name.equalsIgnoreCase(name)) {
                return school;
            }
        }
        return ARCANE; // Default fallback
    }

    /**
     * Get school by ordinal
     */
    public static VesselSchool byOrdinal(int ordinal) {
        if (ordinal >= 0 && ordinal < values().length) {
            return values()[ordinal];
        }
        return ARCANE;
    }

    /**
     * Get display name
     */
    public String getDisplayName() {
        return switch (this) {
            case FIRE -> "Fire School";
            case WATER -> "Water School";
            case EARTH -> "Earth School";
            case AIR -> "Air School";
            case ARCANE -> "Arcane School";
        };
    }

    /**
     * Get description
     */
    public String getDescription() {
        return switch (this) {
            case FIRE -> "Enhances destruction and energy-based wishes. Higher power but higher energy cost.";
            case WATER -> "Enhances healing and fluid-based wishes. Lower energy cost with healing effects.";
            case EARTH -> "Enhances structural and creation wishes. Better material control and durability.";
            case AIR -> "Enhances speed and mobility wishes. Faster execution with higher mobility.";
            case ARCANE -> "Balanced school for all wish types. Versatile and good for beginners.";
        };
    }

    /**
     * Calculate effective wish power based on school
     */
    public float calculateWishPower(float basePower) {
        return basePower * wishPowerMultiplier;
    }

    /**
     * Calculate wish execution time based on school
     */
    public float calculateWishTime(float baseTime) {
        return baseTime / wishSpeedMultiplier;
    }

    /**
     * Calculate energy cost based on school
     */
    public float calculateEnergyCost(float baseCost) {
        return baseCost / energyEfficiency;
    }
}
