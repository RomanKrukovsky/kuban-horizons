package com.kuban.genie.pocket;

import com.kuban.genie.KubanGenie;
import com.kuban.genie.memory.WorldGenieMemory;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.*;

/**
 * Engine for miniaturization - transforming large objects into miniature versions
 * that can be placed in pocket dimensions or carried by genies.
 */
public class MiniaturizationEngine {

    private static final int MAX_MINIATURES_PER_PLAYER = 20;
    private static final double MINIATURE_SCALE = 0.1; // 10% of original size
    private static final int MINIATURE_COOLDOWN_TICKS = 20 * 15; // 15 seconds

    private final Map<UUID, List<MiniatureWorldItem>> playerMiniatures = new HashMap<>();
    private final Map<BlockPos, UUID> miniatureOrigins = new HashMap<>();
    private final Map<UUID, Long> lastMiniaturizeTime = new HashMap<>();

    /**
     * Miniaturize a block or entity into a portable miniature
     */
    public MiniatureWorldItem miniaturize(Player player, BlockPos pos, String name) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return null;
        }

        // Check cooldown
        Long lastUsed = lastMiniaturizeTime.get(serverPlayer.getUUID());
        if (lastUsed != null && System.currentTimeMillis() - lastUsed < MINIATURE_COOLDOWN_TICKS) {
            return null;
        }

        ServerLevel world = (ServerLevel) player.level();
        BlockState blockState = world.getBlockState(pos);

        // Check if block is miniaturizable
        if (blockState.isAir() || !canMiniaturize(blockState)) {
            return null;
        }

        // Check player limit
        List<MiniatureWorldItem> miniatures = playerMiniatures.computeIfAbsent(
            serverPlayer.getUUID(), k -> new ArrayList<>()
        );

        if (miniatures.size() >= MAX_MINIATURES_PER_PLAYER) {
            serverPlayer.sendSystemMessage(Component.literal(
                "§cCannot miniaturize more items. Maximum: " + MAX_MINIATURES_PER_PLAYER));
            return null;
        }

        // Create miniature
        MiniatureWorldItem miniature = new MiniatureWorldItem(
            UUID.randomUUID(),
            name,
            serverPlayer.getUUID(),
            blockState,
            pos,
            world.dimension(),
            MINIATURE_SCALE
        );

        miniatures.add(miniature);
        miniatureOrigins.put(pos, serverPlayer.getUUID());
        lastMiniaturizeTime.put(serverPlayer.getUUID(), System.currentTimeMillis());

        // Remove original block
        world.removeBlock(pos, false);

        // Record event
        WorldGenieMemory memory = KubanGenie.getGenieMemory();
        memory.recordEvent(
            serverPlayer.getUUID(),
            "miniaturized_block",
            Map.of(
                "block", blockState.getBlock().getDescriptionId(),
                "position", pos.toShortString(),
                "scale", MINIATURE_SCALE
            )
        );

        KubanGenie.LOGGER.info("Player {} miniaturized {} at {}",
            serverPlayer.getName().getString(), name, pos);
        return miniature;
    }

    /**
     * Restore a miniature to its original size and position
     */
    public boolean restoreMiniature(Player player, MiniatureWorldItem miniature) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return false;
        }

        if (!miniature.getOwner().equals(serverPlayer.getUUID())) {
            serverPlayer.sendSystemMessage(Component.literal("§cYou can only restore your own miniatures"));
            return false;
        }

        ServerLevel world = (ServerLevel) player.level();
        BlockPos restorePos = miniature.getOriginalPosition();

        // Check if position is available
        if (!world.getBlockState(restorePos).isAir()) {
            serverPlayer.sendSystemMessage(Component.literal("§cCannot restore - position occupied"));
            return false;
        }

        // Restore the block
        world.setBlock(restorePos, miniature.getOriginalBlockState(), 3);

        // Remove from inventory
        List<MiniatureWorldItem> miniatures = playerMiniatures.get(serverPlayer.getUUID());
        if (miniatures != null) {
            miniatures.remove(miniature);
        }

        // Record event
        WorldGenieMemory memory = KubanGenie.getGenieMemory();
        memory.recordEvent(
            serverPlayer.getUUID(),
            "restored_miniature",
            Map.of("block", miniature.getName())
        );

        KubanGenie.LOGGER.info("Player {} restored miniature {} at {}",
            serverPlayer.getName().getString(), miniature.getName(), restorePos);
        return true;
    }

    /**
     * Place a miniature in a pocket scene
     */
    public boolean placeMiniatureInScene(Player player, MiniatureWorldItem miniature, PocketScene scene) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return false;
        }

        if (!miniature.getOwner().equals(serverPlayer.getUUID())) {
            serverPlayer.sendSystemMessage(Component.literal("§cYou can only place your own miniatures"));
            return false;
        }

        // Add to scene
        scene.addMiniature(miniature);

        // Remove from player inventory
        List<MiniatureWorldItem> miniatures = playerMiniatures.get(serverPlayer.getUUID());
        if (miniatures != null) {
            miniatures.remove(miniature);
        }

        // Record event
        WorldGenieMemory memory = KubanGenie.getGenieMemory();
        memory.recordEvent(
            serverPlayer.getUUID(),
            "placed_miniature_in_scene",
            Map.of("scene", scene.getName(), "miniature", miniature.getName())
        );

        return true;
    }

    /**
     * Get all miniatures owned by a player
     */
    public List<MiniatureWorldItem> getPlayerMiniatures(UUID playerId) {
        return playerMiniatures.getOrDefault(playerId, Collections.emptyList());
    }

    /**
     * Get miniature by ID
     */
    @Nullable
    public MiniatureWorldItem getMiniatureById(UUID miniatureId) {
        for (List<MiniatureWorldItem> miniatures : playerMiniatures.values()) {
            for (MiniatureWorldItem miniature : miniatures) {
                if (miniature.getId().equals(miniatureId)) {
                    return miniature;
                }
            }
        }
        return null;
    }

    /**
     * Check if a block can be miniaturized
     */
    private boolean canMiniaturize(BlockState state) {
        String blockId = state.getBlock().getDescriptionId();
        return !blockId.contains("bedrock") &&
               !blockId.contains("command_block") &&
               !blockId.contains("structure_block");
    }

    /**
     * Get miniature statistics
     */
    public Map<String, Integer> getStatistics() {
        Map<String, Integer> stats = new HashMap<>();
        int totalMiniatures = 0;
        int placedInScenes = 0;

        for (List<MiniatureWorldItem> miniatures : playerMiniatures.values()) {
            totalMiniatures += miniatures.size();
            for (MiniatureWorldItem miniature : miniatures) {
                if (miniature.getPlacedInScene()) {
                    placedInScenes++;
                }
            }
        }

        stats.put("total_miniatures", totalMiniatures);
        stats.put("max_limit", MAX_MINIATURES_PER_PLAYER);
        stats.put("placed_in_scenes", placedInScenes);
        return stats;
    }

    /**
     * Save all miniatures to NBT
     */
    public CompoundTag saveAllMiniatures() {
        CompoundTag tag = new CompoundTag();
        int index = 0;

        for (List<MiniatureWorldItem> miniatures : playerMiniatures.values()) {
            for (MiniatureWorldItem miniature : miniatures) {
                CompoundTag miniatureTag = new CompoundTag();
                miniature.save(miniatureTag);
                tag.put("miniature_" + index++, miniatureTag);
            }
        }

        tag.putInt("count", playerMiniatures.values().stream().mapToInt(List::size).sum());
        return tag;
    }

    /**
     * Load miniatures from NBT
     */
    public void loadAllMiniatures(CompoundTag tag) {
        playerMiniatures.clear();
        int count = tag.getInt("count");

        for (int i = 0; i < count; i++) {
            CompoundTag miniatureTag = tag.getCompound("miniature_" + i);
            MiniatureWorldItem miniature = new MiniatureWorldItem();
            miniature.load(miniatureTag);

            playerMiniatures.computeIfAbsent(miniature.getOwner(), k -> new ArrayList<>()).add(miniature);
        }
    }

    /**
     * Get miniature by origin position
     */
    @Nullable
    public MiniatureWorldItem getMiniatureByOrigin(BlockPos pos) {
        UUID ownerId = miniatureOrigins.get(pos);
        if (ownerId != null) {
            List<MiniatureWorldItem> miniatures = playerMiniatures.get(ownerId);
            if (miniatures != null) {
                for (MiniatureWorldItem miniature : miniatures) {
                    if (miniature.getOriginalPosition().equals(pos)) {
                        return miniature;
                    }
                }
            }
        }
        return null;
    }

    /**
     * Check if position has a miniature origin
     */
    public boolean hasMiniatureOrigin(BlockPos pos) {
        return miniatureOrigins.containsKey(pos);
    }

    /**
     * Remove miniature origin tracking
     */
    public void removeMiniatureOrigin(BlockPos pos) {
        miniatureOrigins.remove(pos);
    }
}
