package genie.genie;

import genie.GenieStateSnapshot;
import genie.defense.PhantomDeathController;
import genie.defense.WishborneDefenseHandler;
import genie.vessel.VesselConfinement;
import genie.vessel.VesselKind;
import genie.vessel.VesselSchool;
import genie.vessel.VesselTracker;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * Main Kuban Genie entity class.
 * Implements all core genie functionality including wishes, defense, and vessel binding.
 */
public class KubanGenie extends LivingEntity {

    private UUID ownerId;
    private boolean isWild = false;
    private String emotionalAura;
    private VesselKind vesselKind = VesselKind.LAMP;
    private VesselSchool vesselSchool = VesselSchool.LUMI;
    @Nullable private VesselConfinement vesselConfinement;
    private final WishborneDefenseHandler defenseHandler;
    private final PhantomDeathController deathController;
    private int wishPower = 100;
    private int maxWishPower = 100;

    public KubanGenie(EntityType<? extends LivingEntity> type, Level level) {
        super(type, level);
        this.defenseHandler = new WishborneDefenseHandler(this);
        this.deathController = new PhantomDeathController(this);
    }

    @Override
    protected void defineSynchedData() {
        // Define entity data
    }

    @Override
    public void tick() {
        super.tick();
        deathController.update();
    }

    /**
     * Bind this genie to a vessel
     */
    public void bindToVessel(ItemStack vesselStack, VesselKind kind, VesselSchool school) {
        this.vesselKind = kind;
        this.vesselSchool = school;
        this.vesselConfinement = new VesselConfinement(UUID.randomUUID());
        this.vesselConfinement.setProtectionLevel(VesselLaw.ProtectionLevel.ENHANCED);
        this.vesselConfinement.applyVesselEffects(this);

        // Mark vessel stack
        if (vesselStack.hasTag()) {
            vesselStack.getTag().putString("bound_genie", this.getStringUUID());
        }
    }

    /**
     * Set the owner of this genie
     */
    public void setOwner(@Nullable Player player) {
        this.ownerId = player != null ? player.getUUID() : null;
        this.isWild = player == null;
    }

    /**
     * Check if this genie has an owner
     */
    public boolean hasOwner() {
        return ownerId != null;
    }

    /**
     * Get the owner
     */
    @Nullable
    public Player getOwner() {
        if (level() instanceof ServerLevel serverLevel && ownerId != null) {
            return serverLevel.getPlayerByUUID(ownerId);
        }
        return null;
    }

    /**
     * Set emotional aura
     */
    public void setEmotionalAura(String aura) {
        this.emotionalAura = aura;
    }

    /**
     * Get emotional aura
     */
    @Nullable
    public String getEmotionalAura() {
        return emotionalAura;
    }

    /**
     * Set wish power
     */
    public void setWishPower(int power) {
        this.wishPower = Math.min(power, maxWishPower);
    }

    /**
     * Get wish power
     */
    public int getWishPower() {
        return wishPower;
    }

    /**
     * Set max wish power
     */
    public void setMaxWishPower(int power) {
        this.maxWishPower = power;
    }

    /**
     * Get max wish power
     */
    public int getMaxWishPower() {
        return maxWishPower;
    }

    /**
     * Get vessel kind
     */
    public VesselKind getVesselKind() {
        return vesselKind;
    }

    /**
     * Get vessel school
     */
    public VesselSchool getVesselSchool() {
        return vesselSchool;
    }

    /**
     * Get vessel confinement
     */
    @Nullable
    public VesselConfinement getVesselConfinement() {
        return vesselConfinement;
    }

    /**
     * Get defense handler
     */
    public WishborneDefenseHandler getDefenseHandler() {
        return defenseHandler;
    }

    /**
     * Get death controller
     */
    public PhantomDeathController getDeathController() {
        return deathController;
    }

    /**
     * Spawn reform particles
     */
    public void spawnReformParticles() {
        // Implementation would spawn particles
        if (level().isClientSide) {
            System.out.println("Genie reform particles");
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        // Handle wishborne defense
        if (defenseHandler.handleDamage(source, amount)) {
            return false; // Damage cancelled
        }

        // Handle phantom states
        if (deathController.handleDamage(amount, source)) {
            return false; // Damage handled by phantom controller
        }

        return super.hurt(source, amount);
    }

    /**
     * Save genie state to NBT
     */
    @Override
    public CompoundTag saveWithoutId(CompoundTag tag) {
        super.saveWithoutId(tag);

        if (ownerId != null) {
            tag.putUUID("owner_id", ownerId);
        }
        tag.putBoolean("is_wild", isWild);
        tag.putString("emotional_aura", emotionalAura != null ? emotionalAura : "");
        tag.putString("vessel_kind", vesselKind.name());
        tag.putString("vessel_school", vesselSchool.name());
        tag.putInt("wish_power", wishPower);
        tag.putInt("max_wish_power", maxWishPower);

        if (vesselConfinement != null) {
            tag.put("vessel_confinement", vesselConfinement.saveToNBT());
        }

        return tag;
    }

    /**
     * Load genie state from NBT
     */
    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);

        if (tag.contains("owner_id")) {
            ownerId = tag.getUUID("owner_id");
        }
        isWild = tag.getBoolean("is_wild");
        emotionalAura = tag.getString("emotional_aura");
        if (emotionalAura.isEmpty()) emotionalAura = null;

        try {
            vesselKind = VesselKind.valueOf(tag.getString("vessel_kind"));
        } catch (Exception e) {
            vesselKind = VesselKind.LAMP;
        }

        try {
            vesselSchool = VesselSchool.valueOf(tag.getString("vessel_school"));
        } catch (Exception e) {
            vesselSchool = VesselSchool.LUMI;
        }

        wishPower = tag.getInt("wish_power");
        maxWishPower = tag.getInt("max_wish_power");

        if (tag.contains("vessel_confinement")) {
            vesselConfinement = VesselConfinement.loadFromNBT(tag.getCompound("vessel_confinement"));
        }
    }

    /**
     * Save genie to NBT (alternative method)
     */
    public CompoundTag saveToNBT(CompoundTag tag) {
        return this.saveWithoutId(tag);
    }

    /**
     * Load genie from NBT (alternative method)
     */
    public static KubanGenie loadFromNBT(CompoundTag tag, Level level) {
        KubanGenie genie = new KubanGenie(genie.entity.KubanGenieEntityType.INSTANCE, level);
        genie.readAdditionalSaveData(tag);
        return genie;
    }

    // Utility methods
    @Override
    public boolean isPushable() {
        return !deathController.getCurrentState().equals(PhantomDeathController.PhantomState.SEALED);
    }

    @Override
    public boolean canBeCollidedWith() {
        return !deathController.getCurrentState().equals(PhantomDeathController.PhantomState.SEALED);
    }

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        return deathController.getCurrentState().equals(PhantomDeathController.PhantomState.SEALED) ||
               deathController.getCurrentState().equals(PhantomDeathController.PhantomState.BANISHED);
    }
}
