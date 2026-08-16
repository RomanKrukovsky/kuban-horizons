package genie.endgame;

import genie.KubanGenie;
import genie.events.GenieEvents;
import genie.meta.MetaRuleEngine;
import genie.vessel.VesselKind;
import genie.vessel.VesselTracker;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Controller for transforming players into genies and managing genie transformations.
 * Handles the complete transformation process including visual effects, abilities, and state management.
 */
@Mod.EventBusSubscriber(modid = KubanGenie.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class PlayerGenieTransformationController {

    // Transformation states
    public enum GenieForm {
        NORMAL,
        PARTIAL_GENIE,
        FULL_GENIE,
        TRUE_GENIE
    }

    // Player transformation data storage
    private static final Map<UUID, GenieTransformationData> transformationData = new HashMap<>();
    private static final Map<UUID, Long> transformationCooldowns = new HashMap<>();

    // Transformation cooldowns
    private static final long TRANSFORMATION_COOLDOWN = 20 * 60 * 30; // 30 minutes
    private static final long REVERSION_COOLDOWN = 20 * 60 * 15; // 15 minutes

    static {
        MinecraftForge.EVENT_BUS.register(PlayerGenieTransformationController.class);
    }

    /**
     * Data class for storing player transformation state
     */
    public static class GenieTransformationData {
        public GenieForm currentForm = GenieForm.NORMAL;
        public boolean hasTransformationAbility = false;
        public long transformationTime = 0;
        public long transformationDuration = 0;
        public Vec3 originalPosition = Vec3.ZERO;
        public float originalHealth = 20.0f;
        public boolean isTransforming = false;
        public CompoundTag savedInventory = new CompoundTag();
        public CompoundTag savedAbilities = new CompoundTag();

        public void saveOriginalState(Player player) {
            this.originalPosition = player.position();
            this.originalHealth = player.getHealth();
            this.transformationTime = System.currentTimeMillis();
        }

        public void applyTransformationEffects(Player player) {
            // Apply genie effects
            player.addEffect(new MobEffectInstance(MobEffects.GLOWING, 20 * 60 * 10, 0, false, false));
            player.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 20 * 10, 0, false, false));
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 20 * 60 * 5, 1, false, false));
            player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 20 * 60 * 10, 0, false, false));
        }

        public void revertEffects(Player player) {
            // Remove genie effects
            player.removeEffect(MobEffects.GLOWING);
            player.removeEffect(MobEffects.LEVITATION);
            player.removeEffect(MobEffects.DAMAGE_BOOST);
            player.removeEffect(MobEffects.FIRE_RESISTANCE);
        }

        public CompoundTag serializeNBT() {
            CompoundTag tag = new CompoundTag();
            tag.putString("currentForm", currentForm.name());
            tag.putBoolean("hasTransformationAbility", hasTransformationAbility);
            tag.putLong("transformationTime", transformationTime);
            tag.putLong("transformationDuration", transformationDuration);
            tag.putDouble("originalX", originalPosition.x);
            tag.putDouble("originalY", originalPosition.y);
            tag.putDouble("originalZ", originalPosition.z);
            tag.putFloat("originalHealth", originalHealth);
            tag.putBoolean("isTransforming", isTransforming);
            tag.put("savedInventory", savedInventory);
            tag.put("savedAbilities", savedAbilities);
            return tag;
        }

        public void deserializeNBT(CompoundTag tag) {
            currentForm = GenieForm.valueOf(tag.getString("currentForm"));
            hasTransformationAbility = tag.getBoolean("hasTransformationAbility");
            transformationTime = tag.getLong("transformationTime");
            transformationDuration = tag.getLong("transformationDuration");
            originalPosition = new Vec3(
                tag.getDouble("originalX"),
                tag.getDouble("originalY"),
                tag.getDouble("originalZ")
            );
            originalHealth = tag.getFloat("originalHealth");
            isTransforming = tag.getBoolean("isTransforming");
            savedInventory = tag.getCompound("savedInventory");
            savedAbilities = tag.getCompound("savedAbilities");
        }
    }

    /**
     * Check if player can transform into a genie
     */
    public static boolean canTransform(ServerPlayer player) {
        UUID playerId = player.getUUID();

        // Check cooldown
        Long cooldownEnd = transformationCooldowns.get(playerId);
        if (cooldownEnd != null && System.currentTimeMillis() < cooldownEnd) {
            return false;
        }

        // Check if already transformed
        GenieTransformationData data = getTransformationData(playerId);
        if (data.currentForm != GenieForm.NORMAL) {
            return false;
        }

        // Check if player has transformation ability
        return data.hasTransformationAbility || player.hasPermissions(4);
    }

    /**
     * Transform player into a genie
     */
    public static boolean transformIntoGenie(ServerPlayer player) {
        if (!canTransform(player)) {
            return false;
        }

        UUID playerId = player.getUUID();
        GenieTransformationData data = getTransformationData(playerId);

        // Save original state
        data.saveOriginalState(player);
        data.isTransforming = true;

        // Apply transformation effects
        data.applyTransformationEffects(player);

        // Change player appearance and abilities
        data.currentForm = GenieForm.PARTIAL_GENIE;
        data.transformationDuration = 20 * 60 * 15; // 15 minutes

        // Grant genie abilities
        grantGenieAbilities(player);

        // Post transformation event
        GenieEvents.PLAYER_TRANSFORMED.post(new PlayerTransformedEvent(playerId, GenieForm.PARTIAL_GENIE));

        KubanGenie.LOGGER.info("Player {} transformed into partial genie", player.getName().getString());
        return true;
    }

    /**
     * Revert player from genie form
     */
    public static boolean revertFromGenie(ServerPlayer player) {
        UUID playerId = player.getUUID();
        GenieTransformationData data = getTransformationData(playerId);

        // Check if player is actually transformed
        if (data.currentForm == GenieForm.NORMAL) {
            return false;
        }

        // Revert effects
        data.revertEffects(player);

        // Restore original state
        player.teleportTo(data.originalPosition.x, data.originalPosition.y, data.originalPosition.z);
        player.setHealth(data.originalHealth);

        // Remove genie abilities
        removeGenieAbilities(player);

        // Set cooldown
        transformationCooldowns.put(playerId, System.currentTimeMillis() + REVERSION_COOLDOWN);

        // Post reversion event
        GenieEvents.PLAYER_REVERTED.post(new PlayerRevertedEvent(playerId, data.currentForm));

        // Reset transformation data
        data.currentForm = GenieForm.NORMAL;
        data.isTransforming = false;

        KubanGenie.LOGGER.info("Player {} reverted from genie form", player.getName().getString());
        return true;
    }

    /**
     * Grant genie abilities to a player
     */
    private static void grantGenieAbilities(ServerPlayer player) {
        // TODO: Implement actual ability system
        // This would grant abilities like wish casting, teleportation, etc.
        player.getAbilities().mayfly = true;
        player.getAbilities().instabuild = true;
        player.onUpdateAbilities();
    }

    /**
     * Remove genie abilities from a player
     */
    private static void removeGenieAbilities(ServerPlayer player) {
        player.getAbilities().mayfly = false;
        player.getAbilities().instabuild = false;
        player.onUpdateAbilities();
    }

    /**
     * Get transformation data for a player
     */
    public static GenieTransformationData getTransformationData(UUID playerId) {
        return transformationData.computeIfAbsent(playerId, k -> new GenieTransformationData());
    }

    /**
     * Check if player is currently transformed
     */
    public static boolean isTransformed(UUID playerId) {
        GenieTransformationData data = transformationData.get(playerId);
        return data != null && data.currentForm != GenieForm.NORMAL;
    }

    /**
     * Get current genie form of player
     */
    public static GenieForm getCurrentForm(UUID playerId) {
        GenieTransformationData data = transformationData.get(playerId);
        return data != null ? data.currentForm : GenieForm.NORMAL;
    }

    /**
     * Update transformation state for player
     */
    public static void updateTransformations() {
        long currentTime = System.currentTimeMillis();

        transformationData.forEach((playerId, data) -> {
            if (data.currentForm != GenieForm.NORMAL && data.isTransforming) {
                // Check if transformation duration has expired
                if (currentTime - data.transformationTime > data.transformationDuration) {
                    revertFromGenie((ServerPlayer) Level.OVERWORLD.getPlayerByUUID(playerId));
                }
            }
        });
    }

    // Event handlers

    @SubscribeEvent
    public static void onPlayerTick(PlayerEvent.PlayerTickEvent event) {
        if (event.player instanceof ServerPlayer player && event.phase == PlayerEvent.Phase.END) {
            updateTransformations();
        }
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (event.getOriginal() instanceof ServerPlayer original && event.getEntity() instanceof ServerPlayer clone) {
            UUID playerId = original.getUUID();
            GenieTransformationData data = transformationData.get(playerId);

            if (data != null) {
                // Transfer transformation data to cloned player
                transformationData.put(clone.getUUID(), data);
                transformationData.remove(playerId);
            }
        }
    }

    /**
     * Event fired when a player transforms into a genie
     */
    public static class PlayerTransformedEvent {
        public final UUID playerId;
        public final GenieForm newForm;

        public PlayerTransformedEvent(UUID playerId, GenieForm newForm) {
            this.playerId = playerId;
            this.newForm = newForm;
        }
    }

    /**
     * Event fired when a player reverts from genie form
     */
    public static class PlayerRevertedEvent {
        public final UUID playerId;
        public final GenieForm previousForm;

        public PlayerRevertedEvent(UUID playerId, GenieForm previousForm) {
            this.playerId = playerId;
            this.previousForm = previousForm;
        }
    }

    /**
     * Enable transformation ability for a player
     */
    public static void enableTransformationAbility(ServerPlayer player) {
        GenieTransformationData data = getTransformationData(player.getUUID());
        data.hasTransformationAbility = true;
        KubanGenie.LOGGER.info("Enabled genie transformation ability for player {}", player.getName().getString());
    }

    /**
     * Disable transformation ability for a player
     */
    public static void disableTransformationAbility(ServerPlayer player) {
        GenieTransformationData data = getTransformationData(player.getUUID());
        data.hasTransformationAbility = false;
        KubanGenie.LOGGER.info("Disabled genie transformation ability for player {}", player.getName().getString());
    }

    /**
     * Check if player has transformation ability
     */
    public static boolean hasTransformationAbility(ServerPlayer player) {
        GenieTransformationData data = transformationData.get(player.getUUID());
        return data != null && data.hasTransformationAbility;
    }
}