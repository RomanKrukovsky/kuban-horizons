package com.kuban.genie.pocket;

import com.kuban.genie.KubanGenie;
import com.kuban.genie.memory.WorldGenieMemory;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.server.ServerLifecycleHooks;

import javax.annotation.Nullable;
import java.util.*;

/**
 * Service layer for pocket scene operations.
 * Provides high-level APIs for scene management and player interactions.
 */
public class PocketSceneService {

    private final PocketSceneEngine engine;
    private final WorldGenieMemory memory;

    public PocketSceneService() {
        this.engine = new PocketSceneEngine();
        this.memory = KubanGenie.getGenieMemory();
    }

    /**
     * Create a new pocket scene
     * @param owner The player creating the scene
     * @param name Name of the scene
     * @param position Position to anchor the scene
     * @param shared Whether the scene is shared with other players
     * @return The created scene, or null if creation failed
     */
    @Nullable
    public PocketScene createScene(Player owner, String name, BlockPos position, boolean shared) {
        if (!(owner instanceof ServerPlayer serverPlayer)) {
            return null;
        }

        if (!engine.canCreateScene(serverPlayer.getUUID())) {
            serverPlayer.sendSystemMessage(Component.literal("§cCannot create more scenes. Maximum: " + PocketSceneEngine.MAX_SCENES_PER_PLAYER));
            return null;
        }

        // Get the world where the scene will be created
        ServerLevel world = (ServerLevel) owner.level();

        PocketScene scene = engine.createScene(world, position, name, serverPlayer.getUUID());
        if (scene != null) {
            scene.setShared(shared);
            memory.recordEvent(
                serverPlayer.getUUID(),
                "created_pocket_scene",
                Map.of("scene_name", name, "position", position.toShortString(), "shared", shared)
            );
            return scene;
        }

        return null;
    }

    /**
     * Enter a pocket scene
     * @param player The player entering the scene
     * @param scene The scene to enter
     * @return true if successful
     */
    public boolean enterScene(Player player, PocketScene scene) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return false;
        }

        if (engine.enterScene(serverPlayer, scene)) {
            memory.recordEvent(
                serverPlayer.getUUID(),
                "entered_pocket_scene",
                Map.of("scene", scene.getName())
            );
            return true;
        }

        return false;
    }

    /**
     * Exit the current pocket scene
     * @param player The player exiting
     * @return true if successful
     */
    public boolean exitScene(Player player) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return false;
        }

        ServerLevel currentWorld = serverPlayer.getLevel();
        ResourceKey<net.minecraft.world.level.Level> dimensionId = currentWorld.dimension();

        PocketScene scene = engine.getScene(dimensionId);
        if (scene != null) {
            return engine.exitScene(serverPlayer, scene);
        }

        return false;
    }

    /**
     * List all scenes owned by a player
     * @param player The player
     * @return List of scenes, or empty list if none
     */
    public List<PocketScene> listPlayerScenes(Player player) {
        return engine.getPlayerScenes(player.getUUID());
    }

    /**
     * Delete a scene
     * @param player The player deleting the scene
     * @param scene The scene to delete
     * @return true if successful
     */
    public boolean deleteScene(Player player, PocketScene scene) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return false;
        }

        if (!player.getUUID().equals(scene.getOwner())) {
            player.sendSystemMessage(Component.literal("§cYou can only delete your own scenes"));
            return false;
        }

        engine.cleanupScene(scene);
        memory.recordEvent(
            serverPlayer.getUUID(),
            "deleted_pocket_scene",
            Map.of("scene", scene.getName())
        );
        return true;
    }

    /**
     * Share a scene with other players
     * @param player The owner
     * @param scene The scene to share
     * @param shareWith List of player UUIDs to share with
     * @return true if successful
     */
    public boolean shareScene(Player player, PocketScene scene, List<UUID> shareWith) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return false;
        }

        if (!player.getUUID().equals(scene.getOwner())) {
            player.sendSystemMessage(Component.literal("§cYou can only share your own scenes"));
            return false;
        }

        scene.addSharedPlayers(shareWith);
        memory.recordEvent(
            serverPlayer.getUUID(),
            "shared_pocket_scene",
            Map.of("scene", scene.getName(), "players", shareWith.size())
        );
        return true;
    }

    /**
     * Get a scene by name
     * @param name The scene name
     * @return The scene, or null if not found
     */
    @Nullable
    public PocketScene getSceneByName(String name) {
        for (PocketScene scene : engine.getPlayerScenes(UUID.randomUUID())) {
            if (scene.getName().equalsIgnoreCase(name)) {
                return scene;
            }
        }
        return null;
    }

    /**
     * Create a portal to a scene
     * @param player The player
     * @param scene The target scene
     * @param portalPos The portal position
     * @return true if successful
     */
    public boolean createPortal(Player player, PocketScene scene, BlockPos portalPos) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return false;
        }

        ServerLevel world = (ServerLevel) player.level();
        engine.createPortalBlock(world, portalPos, player.getDirection(), scene);

        memory.recordEvent(
            serverPlayer.getUUID(),
            "created_pocket_portal",
            Map.of("scene", scene.getName(), "portal", portalPos.toShortString())
        );
        return true;
    }

    /**
     * Set scene time flow multiplier
     * @param scene The scene
     * @param multiplier Time flow multiplier (0.1-10.0)
     */
    public void setSceneTimeFlow(PocketScene scene, float multiplier) {
        engine.setSceneTimeFlow(scene, multiplier);
        memory.recordEvent(
            scene.getOwner(),
            "changed_scene_time_flow",
            Map.of("scene", scene.getName(), "multiplier", multiplier)
        );
    }

    /**
     * Check if player is in a pocket scene
     * @param player The player
     * @return true if in a pocket scene
     */
    public boolean isInPocketScene(Player player) {
        ServerLevel world = (ServerLevel) player.level();
        return engine.getScene(world.dimension()) != null;
    }

    /**
     * Get the current pocket scene
     * @param player The player
     * @return The current scene, or null if not in one
     */
    @Nullable
    public PocketScene getCurrentScene(Player player) {
        ServerLevel world = (ServerLevel) player.level();
        return engine.getScene(world.dimension());
    }

    /**
     * Cleanup all expired scenes
     */
    public void cleanupExpiredScenes() {
        engine.onServerTick(null); // Force cleanup
    }

    /**
     * Get scene count statistics
     * @return Map with scene statistics
     */
    public Map<String, Integer> getSceneStatistics() {
        Map<String, Integer> stats = new HashMap<>();
        stats.put("total_scenes", engine.getPlayerScenes(UUID.randomUUID()).size());
        stats.put("active_scenes", engine.getActiveSceneCount());
        stats.put("max_limit", PocketSceneEngine.MAX_SCENES_PER_PLAYER);
        return stats;
    }

    private int getActiveSceneCount() {
        int count = 0;
        for (List<PocketScene> scenes : engine.getAllPlayerScenes().values()) {
            count += scenes.size();
        }
        return count;
    }
}
