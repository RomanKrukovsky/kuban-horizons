package genie.vessel;

import genie.genie.KubanGenie;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Container for vessel laws that govern genie behavior within a vessel.
 * Enforces the rules of the vessel on the contained genie.
 */
public class VesselConfinement {

    private final List<VesselLaw> laws = new ArrayList<>();
    private final UUID vesselId;
    private VesselLaw.ProtectionLevel protectionLevel = VesselLaw.ProtectionLevel.NORMAL;

    public VesselConfinement(UUID vesselId) {
        this.vesselId = vesselId;
    }

    /**
     * Add a new law to the vessel
     */
    public void addLaw(VesselLaw law) {
        if (!laws.contains(law)) {
            laws.add(law);
        }
    }

    /**
     * Remove a law from the vessel
     */
    public void removeLaw(VesselLaw law) {
        laws.remove(law);
    }

    /**
     * Check if a genie is allowed to perform a wish
     * @return true if the wish is allowed
     */
    public boolean isWishAllowed(KubanGenie genie, String wishText) {
        for (VesselLaw law : laws) {
            if (!law.isWishAllowed(genie, wishText)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Check if a genie is allowed to leave the vessel
     */
    public boolean isLeavingAllowed(KubanGenie genie) {
        for (VesselLaw law : laws) {
            if (!law.isLeavingAllowed(genie)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Apply vessel laws to a wish
     * @return modified wish text or null if wish is blocked
     */
    @Nullable
    public String applyWishLaws(KubanGenie genie, String wishText) {
        String currentWish = wishText;
        for (VesselLaw law : laws) {
            currentWish = law.modifyWish(genie, currentWish);
            if (currentWish == null) {
                return null; // Wish completely blocked
            }
        }
        return currentWish;
    }

    /**
     * Set the protection level of the vessel
     */
    public void setProtectionLevel(VesselLaw.ProtectionLevel level) {
        this.protectionLevel = level;
    }

    /**
     * Get the protection level
     */
    public VesselLaw.ProtectionLevel getProtectionLevel() {
        return protectionLevel;
    }

    /**
     * Check if the vessel has a specific law
     */
    public boolean hasLaw(VesselLaw law) {
        return laws.contains(law);
    }

    /**
     * Get all active laws
     */
    public List<VesselLaw> getActiveLaws() {
        return new ArrayList<>(laws);
    }

    /**
     * Save vessel confinement to NBT
     */
    public CompoundTag saveToNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("vessel_id", vesselId);
        tag.putString("protection_level", protectionLevel.name());

        CompoundTag lawsTag = new CompoundTag();
        for (int i = 0; i < laws.size(); i++) {
            lawsTag.put("law_" + i, laws.get(i).saveToNBT());
        }
        tag.put("laws", lawsTag);

        return tag;
    }

    /**
     * Load vessel confinement from NBT
     */
    public static VesselConfinement loadFromNBT(CompoundTag tag) {
        UUID vesselId = tag.getUUID("vessel_id");
        VesselConfinement confinement = new VesselConfinement(vesselId);

        if (tag.contains("protection_level")) {
            confinement.protectionLevel = VesselLaw.ProtectionLevel.valueOf(tag.getString("protection_level"));
        }

        CompoundTag lawsTag = tag.getCompound("laws");
        for (String key : lawsTag.getAllKeys()) {
            try {
                CompoundTag lawTag = lawsTag.getCompound(key);
                VesselLaw law = VesselLaw.loadFromNBT(lawTag);
                if (law != null) {
                    confinement.addLaw(law);
                }
            } catch (Exception e) {
                // Skip invalid laws
            }
        }

        return confinement;
    }

    /**
     * Check if genie can be summoned based on vessel laws
     */
    public boolean canSummonGenie(KubanGenie genie) {
        // Check each law for summoning restrictions
        for (VesselLaw law : laws) {
            if (!law.canSummonGenie(genie)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Check if genie can be dismissed based on vessel laws
     */
    public boolean canDismissGenie(KubanGenie genie) {
        // Check each law for dismissal restrictions
        for (VesselLaw law : laws) {
            if (!law.canDismissGenie(genie)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Apply vessel effects to genie
     */
    public void applyVesselEffects(KubanGenie genie) {
        // Apply school-specific effects
        switch (genie.getVesselSchool()) {
            case PYRO:
                // Fire-related effects
                genie.setEmotionalAura("pyro_aura");
                break;
            case HYDRO:
                // Water-related effects
                genie.setEmotionalAura("hydro_aura");
                break;
            case GEO:
                // Earth-related effects
                genie.setEmotionalAura("geo_aura");
                break;
            case AERO:
                // Air-related effects
                genie.setEmotionalAura("aero_aura");
                break;
            case LUMI:
                // Light-related effects
                genie.setEmotionalAura("lumi_aura");
                break;
        }

        // Apply protection level effects
        switch (protectionLevel) {
            case NORMAL:
                // Standard protection
                genie.setMaxHealth(genie.getMaxHealth() * 1.2f);
                break;
            case ENHANCED:
                // Enhanced protection
                genie.setMaxHealth(genie.getMaxHealth() * 1.5f);
                genie.setInvulnerableTicks(20);
                break;
            case ABSOLUTE:
                // Absolute protection
                genie.setMaxHealth(genie.getMaxHealth() * 2.0f);
                genie.setInvulnerableTicks(40);
                break;
        }
    }
}
