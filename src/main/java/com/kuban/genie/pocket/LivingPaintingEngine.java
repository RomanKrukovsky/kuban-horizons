package com.kuban.genie.pocket;

import com.kuban.genie.KubanGenie;
import com.kuban.genie.memory.WorldGenieMemory;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import javax.annotation.Nullable;
import java.util.*;

/**
 * Engine for living paintings - paintings that contain entire miniature scenes within them.
 * When activated, the painting becomes a portal to the contained scene.
 */
public class LivingPaintingEngine {

    private static final int MAX_PAINTINGS_PER_SCENE = 5;
    private static final int PAINTING_COOLDOWN_TICKS = 20 * 10; // 10 seconds

    private final Map<BlockPos, LivingPainting> paintings = new HashMap<>();
    private final Map<UUID, Long> lastActivation = new HashMap<>();

    /**
     * Create a new living painting
     */
    public LivingPainting createLivingPainting(ServerLevel world, BlockPos pos, Direction facing, String sceneName, UUID owner) {
        if (paintings.size() >= MAX_PAINTINGS_PER_SCENE) {
            KubanGenie.LOGGER.warn("Cannot create more living paintings in this scene");
            return null;
        }

        LivingPainting painting = new LivingPainting(
            pos,
            facing,
            sceneName,
            owner,
            world.dimension()
        );

        paintings.put(pos, painting);
        KubanGenie.LOGGER.info("Created living painting '{}' at {} for scene '{}'",
            sceneName, pos, sceneName);
        return painting;
    }

    /**
     * Activate a living painting - enter the contained scene
     */
    public boolean activatePainting(Player player, BlockPos paintingPos) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return false;
        }

        // Check cooldown
        Long lastUsed = lastActivation.get(serverPlayer.getUUID());
        if (lastUsed != null && System.currentTimeMillis() - lastUsed < PAINTING_COOLDOWN_TICKS) {
            long remaining = (PAINTING_COOLDOWN_TICKS - (System.currentTimeMillis() - lastUsed)) / 20;
            serverPlayer.sendSystemMessage(Component.literal(
                "§ePlease wait " + remaining + " seconds before using this painting again"));
            return false;
        }

        LivingPainting painting = paintings.get(paintingPos);
        if (painting == null) {
            return false;
        }

        // Check ownership or sharing
        if (!painting.canPlayerUse(serverPlayer.getUUID())) {
            serverPlayer.sendSystemMessage(Component.literal("§cYou don't have permission to use this painting"));
            return false;
        }

        // Get or create the target scene
        PocketSceneService sceneService = new PocketSceneService();
        PocketScene scene = sceneService.getSceneByName(painting.getTargetScene());

        if (scene == null) {
            serverPlayer.sendSystemMessage(Component.literal("§cTarget scene not found: " + painting.getTargetScene()));
            return false;
        }

        // Enter the scene
        if (sceneService.enterScene(serverPlayer, scene)) {
            lastActivation.put(serverPlayer.getUUID(), System.currentTimeMillis());

            // Record event
            WorldGenieMemory memory = KubanGenie.getGenieMemory();
            memory.recordEvent(
                serverPlayer.getUUID(),
                "activated_living_painting",
                Map.of("painting_pos", paintingPos.toShortString(), "scene", scene.getName())
            );

            return true;
        }

        return false;
    }

    /**
     * Update all living paintings - check for entity interactions
     */
    public void updatePaintings(ServerLevel world) {
        for (LivingPainting painting : paintings.values()) {
            if (painting.getWorld() != world.dimension()) {
                continue;
            }

            // Check for nearby players
            AABB interactionArea = new AABB(painting.getPos()).inflate(1.5);
            List<ServerPlayer> nearbyPlayers = world.getEntitiesOfClass(
                ServerPlayer.class,
                interactionArea,
                player -> painting.canPlayerUse(player.getUUID())
            );

            if (!nearbyPlayers.isEmpty()) {
                // Show activation hint
                for (ServerPlayer player : nearbyPlayers) {
                    if (!player.getCooldowns().isOnCooldown(KubanGenieItems.LIVING_PAINTING.get())) {
                        player.sendSystemMessage(Component.literal(
                            "§7Right-click the painting to enter the scene"));
                    }
                }
            }
        }
    }

    /**
     * Remove a living painting
     */
    public boolean removePainting(BlockPos pos) {
        LivingPainting painting = paintings.remove(pos);
        if (painting != null) {
            KubanGenie.LOGGER.info("Removed living painting at {}", pos);
            return true;
        }
        return false;
    }

    /**
     * Get a living painting by position
     */
    @Nullable
    public LivingPainting getPainting(BlockPos pos) {
        return paintings.get(pos);
    }

    /**
     * Check if position has a living painting
     */
    public boolean hasPainting(BlockPos pos) {
        return paintings.containsKey(pos);
    }

    /**
     * Get all paintings in a dimension
     */
    public List<LivingPainting> getPaintingsInDimension(ResourceKey<Level> dimension) {
        List<LivingPainting> result = new ArrayList<>();
        for (LivingPainting painting : paintings.values()) {
            if (painting.getWorld().equals(dimension)) {
                result.add(painting);
            }
        }
        return result;
    }

    /**
     * Update painting appearance based on scene state
     */
    public void updatePaintingAppearance(LivingPainting painting) {
        // Get scene state
        PocketSceneService sceneService = new PocketSceneService();
        PocketScene scene = sceneService.getSceneByName(painting.getTargetScene());

        if (scene != null) {
            // Update painting texture based on scene activity
            painting.setActive(scene.getEntityCount() > 0);
        }
    }

    /**
     * Save all paintings to NBT
     */
    public CompoundTag saveAllPaintings() {
        CompoundTag tag = new CompoundTag();
        int index = 0;

        for (LivingPainting painting : paintings.values()) {
            CompoundTag paintingTag = new CompoundTag();
            painting.save(paintingTag);
            tag.put("painting_" + index++, paintingTag);
        }

        tag.putInt("count", paintings.size());
        return tag;
    }

    /**
     * Load paintings from NBT
     */
    public void loadAllPaintings(CompoundTag tag) {
        paintings.clear();
        int count = tag.getInt("count");

        for (int i = 0; i < count; i++) {
            CompoundTag paintingTag = tag.getCompound("painting_" + i);
            LivingPainting painting = new LivingPainting();
            painting.load(paintingTag);
            paintings.put(painting.getPos(), painting);
        }
    }

    /**
     * Get painting statistics
     */
    public Map<String, Integer> getStatistics() {
        Map<String, Integer> stats = new HashMap<>();
        stats.put("total_paintings", paintings.size());
        stats.put("max_limit", MAX_PAINTINGS_PER_SCENE);
        stats.put("active_paintings", (int) paintings.values().stream().filter(LivingPainting::isActive).count());
        return stats;
    }

    /**
     * Cleanup paintings in expired scenes
     */
    public void cleanupExpiredScenePaintings(ResourceKey<Level> dimensionId) {
        Iterator<Map.Entry<BlockPos, LivingPainting>> iterator = paintings.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<BlockPos, LivingPainting> entry = iterator.next();
            if (entry.getValue().getWorld().equals(dimensionId)) {
                iterator.remove();
            }
        }
    }
}
