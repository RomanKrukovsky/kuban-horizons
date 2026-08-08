package dev.romankrukovsky.kubanhorizons.genie.runtime.transaction;

import dev.romankrukovsky.kubanhorizons.genie.runtime.selection.RegionSelection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Заранее находит безопасные точки и выводит игроков из изменяемой области. */
final class PlayerRelocator {
    private static final int MAX_HORIZONTAL_SEARCH = 32;

    private PlayerRelocator() {
    }

    static Map<UUID, Vec3> plan(ServerLevel level, RegionSelection selection) {
        Map<UUID, Vec3> result = new LinkedHashMap<>();
        AABB boundary = AABB.encapsulatingFullBlocks(selection.min(), selection.max());
        for (ServerPlayer player : level.players()) {
            if (!boundary.intersects(player.getBoundingBox())) {
                continue;
            }
            Vec3 safe = findSafe(level, selection, player.blockPosition())
                    .orElseThrow(() -> new IllegalStateException("no safe relocation point for " + player.getName().getString()));
            result.put(player.getUUID(), safe);
        }
        return Map.copyOf(result);
    }

    static void apply(ServerLevel level, Map<UUID, Vec3> plan) {
        for (Map.Entry<UUID, Vec3> entry : plan.entrySet()) {
            net.minecraft.world.entity.player.Player found = level.getPlayerByUUID(entry.getKey());
            if (found instanceof ServerPlayer player) {
                Vec3 pos = entry.getValue();
                player.teleportTo(pos.x, pos.y, pos.z);
            }
        }
    }

    private static Optional<Vec3> findSafe(ServerLevel level, RegionSelection selection, BlockPos origin) {
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            for (int distance = 2; distance <= MAX_HORIZONTAL_SEARCH; distance++) {
                int x = direction.getStepX() < 0 ? selection.min().getX() - distance
                        : direction.getStepX() > 0 ? selection.max().getX() + distance : origin.getX();
                int z = direction.getStepZ() < 0 ? selection.min().getZ() - distance
                        : direction.getStepZ() > 0 ? selection.max().getZ() + distance : origin.getZ();
                for (int dy = 8; dy >= -8; dy--) {
                    BlockPos feet = new BlockPos(x, Math.clamp(origin.getY() + dy,
                            level.getMinY() + 1, level.getMaxY() - 2), z);
                    if (safe(level, feet) && !selection.contains(feet)) {
                        return Optional.of(Vec3.atBottomCenterOf(feet));
                    }
                }
            }
        }
        BlockPos spawn = level.getRespawnData().pos();
        return safe(level, spawn) && !selection.contains(spawn)
                ? Optional.of(Vec3.atBottomCenterOf(spawn)) : Optional.empty();
    }

    private static boolean safe(ServerLevel level, BlockPos feet) {
        if (!level.isInWorldBounds(feet) || !level.getWorldBorder().isWithinBounds(feet)) {
            return false;
        }
        BlockPos head = feet.above();
        BlockPos floor = feet.below();
        if (!level.getBlockState(feet).getCollisionShape(level, feet).isEmpty()
                || !level.getBlockState(head).getCollisionShape(level, head).isEmpty()
                || !level.getFluidState(feet).isEmpty() || !level.getFluidState(head).isEmpty()) {
            return false;
        }
        if (!level.getBlockState(floor).isFaceSturdy(level, floor, Direction.UP)
                || level.getFluidState(floor).isSource()) {
            return false;
        }
        return !level.getBlockState(feet).is(Blocks.FIRE)
                && !level.getBlockState(feet).is(Blocks.SOUL_FIRE)
                && !level.getBlockState(floor).is(Blocks.MAGMA_BLOCK);
    }
}
