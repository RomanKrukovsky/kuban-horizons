package dev.romankrukovsky.kubanhorizons.genie.dimension;

import dev.romankrukovsky.kubanhorizons.genie.aura.MagicalSignature;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;

/** Движок 1-минутных карманных временных сцен (пляж, ресторан, дворец) (Pocket Scene Engine). */
public final class PocketSceneEngine {
    private PocketSceneEngine() {
    }

    public static boolean spawnPocketScene(ServerLevel level, BlockPos origin, Player player, String type) {
        MagicalSignature.cast(level, net.minecraft.world.phys.Vec3.atCenterOf(origin));

        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                BlockPos pos = origin.offset(x, 0, z);
                if (level.isEmptyBlock(pos)) {
                    level.setBlock(pos, Blocks.SAND.defaultBlockState(), 3);
                }
            }
        }

        level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, origin.getX() + 0.5D, origin.getY() + 1.0D, origin.getZ() + 0.5D,
                30, 1.0D, 0.5D, 1.0D, 0.05D);
        player.sendSystemMessage(Component.translatable("message.kubanhorizons.genie.pocket_scene", type));
        return true;
    }
}
