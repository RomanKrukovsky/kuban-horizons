package genie.vessel;

import genie.genie.KubanGenie;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;

import javax.annotation.Nullable;

/**
 * Individual law that governs genie behavior within a vessel.
 * Can restrict wishes, limit powers, or enforce specific behaviors.
 */
public class VesselLaw {

    /**
     * Protection levels for vessels
     */
    public enum ProtectionLevel {
        NORMAL,     // Standard protection
        ENHANCED,   // Enhanced protection
        ABSOLUTE    // Absolute protection
    }

    /**
     * Law types
     */
    public enum LawType {
        WISH_RESTRICTION,      // Restrict certain wishes
        POWER_LIMIT,           // Limit wish power
        TIME_RESTRICTION,      // Restrict when wishes can be made
        LOCATION_RESTRICTION,  // Restrict where wishes can be made
        BEHAVIOR_RESTRICTION,  // Restrict genie behavior
        SUMMON_RESTRICTION,    // Restrict summoning
        DISMISS_RESTRICTION     // Restrict dismissal
    }

    private final LawType type;
    private final String lawId;
    private String description;
    private boolean isActive = true;
    private int priority = 1; // 1-10, higher = more important

    // Law-specific data
    private String restrictionPattern;
    private int maxPowerLimit = -1;
    private long timeRestrictionStart = -1;
    private long timeRestrictionEnd = -1;
    private String allowedLocation = "anywhere";

    public VesselLaw(LawType type, String lawId) {
        this.type = type;
        this.lawId = lawId;
        this.description = "Default " + type + " law";
    }

    /**
     * Set law description
     */
    public VesselLaw setDescription(String description) {
        this.description = description;
        return this;
    }

    /**
     * Set law priority
     */
    public VesselLaw setPriority(int priority) {
        this.priority = Math.max(1, Math.min(10, priority));
        return this;
    }

    /**
     * Set active status
     */
    public VesselLaw setActive(boolean active) {
        this.isActive = active;
        return this;
    }

    /**
     * Check if wish is allowed
     * @return true if wish is allowed
     */
    public boolean isWishAllowed(KubanGenie genie, String wishText) {
        if (!isActive) return true;

        switch (type) {
            case WISH_RESTRICTION:
                if (restrictionPattern != null && !restrictionPattern.isEmpty()) {
                    return !wishText.toLowerCase().contains(restrictionPattern.toLowerCase());
                }
                break;
            case POWER_LIMIT:
                if (maxPowerLimit > 0) {
                    return genie.getWishPower() <= maxPowerLimit;
                }
                break;
            case TIME_RESTRICTION:
                if (timeRestrictionStart > 0 && timeRestrictionEnd > 0) {
                    long currentTime = System.currentTimeMillis();
                    return currentTime >= timeRestrictionStart && currentTime <= timeRestrictionEnd;
                }
                break;
            case LOCATION_RESTRICTION:
                if (!"anywhere".equals(allowedLocation)) {
                    // Would check genie's location
                    return true; // Simplified for now
                }
                break;
        }
        return true;
    }

    /**
     * Modify wish text based on law
     * @return modified wish or null if wish is blocked
     */
    @Nullable
    public String modifyWish(KubanGenie genie, String wishText) {
        if (!isActive) return wishText;

        switch (type) {
            case WISH_RESTRICTION:
                if (restrictionPattern != null && !restrictionPattern.isEmpty() &&
                    wishText.toLowerCase().contains(restrictionPattern.toLowerCase())) {
                    return null; // Block wish
                }
                break;
            case POWER_LIMIT:
                if (maxPowerLimit > 0 && genie.getWishPower() > maxPowerLimit) {
                    // Reduce wish power
                    return wishText + " (limited to " + maxPowerLimit + " power)";
                }
                break;
        }
        return wishText;
    }

    /**
     * Check if genie can leave vessel
     */
    public boolean isLeavingAllowed(KubanGenie genie) {
        if (!isActive) return true;

        switch (type) {
            case SUMMON_RESTRICTION:
                return false; // Cannot leave vessel
            case BEHAVIOR_RESTRICTION:
                return true; // Depends on specific behavior
        }
        return true;
    }

    /**
     * Check if genie can be summoned
     */
    public boolean canSummonGenie(KubanGenie genie) {
        if (!isActive) return true;

        switch (type) {
            case SUMMON_RESTRICTION:
                return false; // Cannot summon
            case TIME_RESTRICTION:
                if (timeRestrictionStart > 0 && timeRestrictionEnd > 0) {
                    long currentTime = System.currentTimeMillis();
                    return currentTime >= timeRestrictionStart && currentTime <= timeRestrictionEnd;
                }
                break;
        }
        return true;
    }

    /**
     * Check if genie can be dismissed
     */
    public boolean canDismissGenie(KubanGenie genie) {
        if (!isActive) return true;

        switch (type) {
            case DISMISS_RESTRICTION:
                return false; // Cannot dismiss
        }
        return true;
    }

    /**
     * Set wish restriction pattern
     */
    public VesselLaw setWishRestriction(String pattern) {
        this.type = LawType.WISH_RESTRICTION;
        this.restrictionPattern = pattern;
        this.description = "Restricts wishes containing: " + pattern;
        return this;
    }

    /**
     * Set power limit
     */
    public VesselLaw setPowerLimit(int limit) {
        this.type = LawType.POWER_LIMIT;
        this.maxPowerLimit = limit;
        this.description = "Limits wish power to " + limit;
        return this;
    }

    /**
     * Set time restriction
     */
    public VesselLaw setTimeRestriction(long startTime, long endTime) {
        this.type = LawType.TIME_RESTRICTION;
        this.timeRestrictionStart = startTime;
        this.timeRestrictionEnd = endTime;
        this.description = "Allows wishes only between " + startTime + " and " + endTime;
        return this;
    }

    /**
     * Set location restriction
     */
    public VesselLaw setLocationRestriction(String location) {
        this.type = LawType.LOCATION_RESTRICTION;
        this.allowedLocation = location;
        this.description = "Allows wishes only in: " + location;
        return this;
    }

    /**
     * Save law to NBT
     */
    public CompoundTag saveToNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putString("type", type.name());
        tag.putString("law_id", lawId);
        tag.putString("description", description);
        tag.putBoolean("is_active", isActive);
        tag.putInt("priority", priority);

        if (restrictionPattern != null) {
            tag.putString("restriction_pattern", restrictionPattern);
        }
        tag.putInt("max_power_limit", maxPowerLimit);
        tag.putLong("time_start", timeRestrictionStart);
        tag.putLong("time_end", timeRestrictionEnd);
        tag.putString("allowed_location", allowedLocation);

        return tag;
    }

    /**
     * Load law from NBT
     */
    @Nullable
    public static VesselLaw loadFromNBT(CompoundTag tag) {
        try {
            LawType type = LawType.valueOf(tag.getString("type"));
            String lawId = tag.getString("law_id");
            VesselLaw law = new VesselLaw(type, lawId);

            law.description = tag.getString("description");
            law.isActive = tag.getBoolean("is_active");
            law.priority = tag.getInt("priority");

            if (tag.contains("restriction_pattern")) {
                law.restrictionPattern = tag.getString("restriction_pattern");
            }
            law.maxPowerLimit = tag.getInt("max_power_limit");
            law.timeRestrictionStart = tag.getLong("time_start");
            law.timeRestrictionEnd = tag.getLong("time_end");
            law.allowedLocation = tag.getString("allowed_location");

            return law;
        } catch (Exception e) {
            return null;
        }
    }

    // Getters
    public LawType getType() {
        return type;
    }

    public String getLawId() {
        return lawId;
    }

    public String getDescription() {
        return description;
    }

    public boolean isActive() {
        return isActive;
    }

    public int getPriority() {
        return priority;
    }
}
