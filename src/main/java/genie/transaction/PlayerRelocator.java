package genie.transaction;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * Safe player teleportation system for Kuban Genie.
 * Handles teleportation with safety checks and fallbacks.
 */
public class PlayerRelocator {

    /**
     * Teleport player to a safe location
     * @return true if teleportation was successful
     */
    public boolean teleportPlayer(Player player, ServerLevel targetLevel, BlockPos targetPos) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return false;
        }

        // Check if target position is valid
        if (!isSafePosition(targetLevel, targetPos)) {
            BlockPos safePos = findSafePosition(targetLevel, targetPos);
            if (safePos == null) {
                return false;
            }
            targetPos = safePos;
        }

        // Perform teleportation
        serverPlayer.teleportTo(targetLevel, targetPos.getX() + 0.5, targetPos.getY(), targetPos.getZ() + 0.5,
                              player.getYRot(), player.getXRot());

        return true;
    }

    /**
     * Check if position is safe for teleportation
     */
    private boolean isSafePosition(ServerLevel level, BlockPos pos) {
        // Check if position is in world bounds
        if (!level.isInWorldBounds(pos)) {
            return false;
        }

        // Check if position is in air (not inside a block)
        if (!level.getBlockState(pos).isAir()) {
            return false;
        }

        // Check if there's a block below for support
        BlockPos below = pos.below();
        if (level.getBlockState(below).isAir()) {
            return false;
        }

        // Check if there's space above for player
        BlockPos above = pos.above(2);
        if (!level.getBlockState(above).isAir()) {
            return false;
        }

        return true;
    }

    /**
     * Find a safe position near target
     */
    @Nullable
    private BlockPos findSafePosition(ServerLevel level, BlockPos targetPos) {
        // Try positions in spiral pattern around target
        int radius = 5;
        for (int r = 0; r <= radius; r++) {
            for (int x = -r; x <= r; x++) {
                for (int z = -r; z <= r; z++) {
                    if (Math.abs(x) == r || Math.abs(z) == r) { // Only check perimeter
                        BlockPos pos = targetPos.offset(x, 0, z);
                        if (isSafePosition(level, pos)) {
                            return pos;
                        }
                    }
                }
            }
        }

        // Try directly above target
        BlockPos above = targetPos.above();
        if (isSafePosition(level, above)) {
            return above;
        }

        return null;
    }

    /**
     * Teleport player to anchor position
     */
    public boolean teleportToAnchor(Player player, BlockPos anchorPos) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return false;
        }

        ServerLevel level = (ServerLevel) player.level();
        return teleportPlayer(player, level, anchorPos);
    }

    /**
     * Get player's current safe position
     */
    @Nullable
    public BlockPos getSafePosition(Player player) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return null;
        }

        ServerLevel level = (ServerLevel) player.level();
        BlockPos pos = serverPlayer.blockPosition();

        if (isSafePosition(level, pos)) {
            return pos;
        }

        return findSafePosition(level, pos);
    }

    /**
     * Configuration for player relocation
     */
    public static class RelocationConfig {
        public int maxTeleportDistance = 1000;
        public int maxSearchRadius = 10;
        public boolean enableFallDamageCheck = true;
        public boolean enableBlockCollisionCheck = true;
        public int cooldownSeconds = 5;
    }
}
