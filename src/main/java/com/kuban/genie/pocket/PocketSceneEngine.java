package com.kuban.genie.pocket;

import com.kuban.genie.KubanGenie;
import com.kuban.genie.memory.WorldGenieMemory;
import com.kuban.genie.util.GeniePerformanceConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.util.ITeleporter;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityTravelToDimensionEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import javax.annotation.Nullable;
import java.util.*;

/**
 * Manages pocket scenes - isolated miniature dimensions that can be entered via portals.
 * Each scene has its own coordinate system, entities, and time flow.
 */
@Mod.EventBusSubscriber(modid = KubanGenie.MODID)
public class PocketSceneEngine {

    private static final int MAX_SCENES_PER_PLAYER = 3;
    private static final int MAX_SCENE_SIZE = 128; // blocks
    private static final int SCENE_TIMEOUT_TICKS = 20 * 60 * 5; // 5 minutes

    private final Map<UUID, List<PocketScene>> playerScenes = new HashMap<>();
    private final Map<ResourceKey<Level>, PocketScene> activeScenes = new HashMap<>();
    private final Random random = new Random();

    public PocketSceneEngine() {
        KubanGenie.LOGGER.info("PocketSceneEngine initialized");
    }

    /**
     * Create a new pocket scene at the specified location
     */
    public PocketScene createScene(ServerLevel world, BlockPos pos, String name, UUID owner) {
        if (!canCreateScene(owner)) {
            KubanGenie.LOGGER.warn("Player {} cannot create more scenes", owner);
            return null;
        }

        // Generate unique dimension ID
        ResourceKey<Level> dimensionId = ResourceKey.create(
            Level.RESOURCE_KEY_REGISTRY,
            KubanGenie.modLoc("pocket_scene_" + random.nextInt(10000))
        );

        PocketScene scene = new PocketScene(
            dimensionId,
            name,
            owner,
            pos,
            MAX_SCENE_SIZE,
            world.dimension()
        );

        // Register the scene
        playerScenes.computeIfAbsent(owner, k -> new ArrayList<>()).add(scene);
        activeScenes.put(dimensionId, scene);

        // Create the dimension
        scene.createDimension();

        KubanGenie.LOGGER.info("Created pocket scene {} for player {} at {}", name, owner, pos);
        return scene;
    }

    /**
     * Check if player can create a new scene
     */
    public boolean canCreateScene(UUID playerId) {
        List<PocketScene> scenes = playerScenes.getOrDefault(playerId, Collections.emptyList());
        return scenes.size() < MAX_SCENES_PER_PLAYER &&
               KubanGenie.getGenieMemory().getGenieEnergy(playerId) > 100;
    }

    /**
     * Enter a pocket scene via portal
     */
    public boolean enterScene(ServerPlayer player, PocketScene scene) {
        if (!scene.isValid()) {
            KubanGenie.LOGGER.error("Attempt to enter invalid scene");
            return false;
        }

        if (player.getUUID().equals(scene.getOwner()) || scene.isShared()) {
            // Teleport the player
            ServerLevel targetWorld = scene.getWorld();
            BlockPos spawnPos = scene.getSpawnPosition();

            player.changeDimension(targetWorld, new PocketTeleporter(spawnPos));

            // Add to scene entities
            scene.addEntity(player.getUUID());

            // Update memory
            WorldGenieMemory memory = KubanGenie.getGenieMemory();
            memory.recordEvent(
                player.getUUID(),
                "entered_pocket_scene",
                Map.of("scene", scene.getName(), "dimension", scene.getDimensionId().location())
            );

            KubanGenie.LOGGER.info("Player {} entered scene {} at {}", player.getName().getString(), scene.getName(), spawnPos);
            return true;
        }

        return false;
    }

    /**
     * Exit a pocket scene back to parent dimension
     */
    public boolean exitScene(ServerPlayer player, PocketScene scene) {
        if (!scene.isValid()) {
            return false;
        }

        ServerLevel parentWorld = player.getServer().getLevel(scene.getParentDimension());
        if (parentWorld == null) {
            KubanGenie.LOGGER.error("Parent dimension not found");
            return false;
        }

        BlockPos exitPos = scene.getExitPosition(player.getUUID());
        if (exitPos == null) {
            exitPos = scene.getSpawnPosition().above();
        }

        player.changeDimension(parentWorld, new PocketTeleporter(exitPos));
        scene.removeEntity(player.getUUID());

        KubanGenie.LOGGER.info("Player {} exited scene {} back to {}",
            player.getName().getString(), scene.getName(), exitPos);
        return true;
    }

    /**
     * Update all scenes - cleanup expired ones
     */
    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Iterator<Map.Entry<ResourceKey<Level>, PocketScene>> iterator = activeScenes.entrySet().iterator();
        long currentTime = System.currentTimeMillis();

        while (iterator.hasNext()) {
            Map.Entry<ResourceKey<Level>, PocketScene> entry = iterator.next();
            PocketScene scene = entry.getValue();

            // Check if scene should be cleaned up
            if (scene.isExpired(currentTime) || !scene.isValid()) {
                cleanupScene(scene);
                iterator.remove();
                KubanGenie.LOGGER.info("Cleaned up expired scene: {}", scene.getName());
            }
        }
    }

    /**
     * Clean up a scene and all its data
     */
    public void cleanupScene(PocketScene scene) {
        try {
            scene.deleteDimension();

            // Remove from player's list
            playerScenes.values().forEach(list -> list.removeIf(s -> s.getDimensionId().equals(scene.getDimensionId())));

            KubanGenie.LOGGER.info("Deleted pocket scene: {}", scene.getName());
        } catch (Exception e) {
            KubanGenie.LOGGER.error("Failed to cleanup scene {}: {}", scene.getName(), e.getMessage());
        }
    }

    /**
     * Get scene by dimension ID
     */
    @Nullable
    public PocketScene getScene(ResourceKey<Level> dimensionId) {
        return activeScenes.get(dimensionId);
    }

    /**
     * Get all scenes owned by a player
     */
    public List<PocketScene> getPlayerScenes(UUID playerId) {
        return playerScenes.getOrDefault(playerId, Collections.emptyList());
    }

    /**
     * Check if entity can enter a scene
     */
    public boolean canEntityEnterScene(Entity entity, PocketScene scene) {
        // Allow players and genie entities
        if (entity instanceof ServerPlayer) {
            return true;
        }

        // Check entity type restrictions
        String entityType = entity.getType().getDescriptionId();
        return !entityType.contains("hostile") && !entityType.contains("boss");
    }

    /**
     * Portal creation helper
     */
    public void createPortalBlock(ServerLevel world, BlockPos pos, Direction facing, PocketScene scene) {
        BlockState portalBlock = KubanGenieBlocks.PORTAL_BLOCK.get().defaultBlockState();
        world.setBlock(pos, portalBlock, 3);

        // Store portal data
        PocketPortalData portalData = new PocketPortalData(scene.getDimensionId(), pos);
        KubanGenie.getGenieMemory().storePortalData(portalData);
    }

    /**
     * Get scene by portal position
     */
    @Nullable
    public PocketScene getSceneByPortal(BlockPos portalPos) {
        PocketPortalData portalData = KubanGenie.getGenieMemory().getPortalData(portalPos);
        if (portalData != null) {
            return activeScenes.get(portalData.getTargetDimension());
        }
        return null;
    }

    /**
     * Update scene time flow
     */
    public void setSceneTimeFlow(PocketScene scene, float timeMultiplier) {
        scene.setTimeMultiplier(Math.max(0.1f, Math.min(10.0f, timeMultiplier)));
    }

    /**
     * Check performance limits
     */
    public boolean isWithinPerformanceLimits() {
        return activeScenes.size() < GeniePerformanceConfig.MAX_POCKET_SCENES &&
               KubanGenie.getGenieMemory().getTotalGenieEnergy() > 500;
    }
}
