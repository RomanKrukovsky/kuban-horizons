package dev.romankrukovsky.kubanhorizons.genie.expression;

import dev.romankrukovsky.kubanhorizons.genie.aura.MagicalSignature;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;

/** Движок магии рисования контуров в воздухе (In-Air Magic Drawing Handler). */
public final class MagicDrawingHandler {
    private MagicDrawingHandler() {
    }

    public static void materializeDrawing(ServerLevel level, Player player, BlockPos startPos, BlockPos endPos) {
        int dx = endPos.getX() - startPos.getX();
        int dy = endPos.getY() - startPos.getY();
        int dz = endPos.getZ() - startPos.getZ();
        int steps = Math.max(Math.abs(dx), Math.max(Math.abs(dy), Math.abs(dz)));
        if (steps > 64) {
            return;
        }
        for (int step = 0; step <= steps; step++) {
            double progress = steps == 0 ? 0.0D : (double) step / steps;
            BlockPos pos = new BlockPos(
                    startPos.getX() + (int) Math.round(dx * progress),
                    startPos.getY() + (int) Math.round(dy * progress),
                    startPos.getZ() + (int) Math.round(dz * progress));
            if (level.isEmptyBlock(pos)) {
                level.setBlock(pos, Blocks.OAK_PLANKS.defaultBlockState(), 3);
            }
        }
        MagicalSignature.cast(level, net.minecraft.world.phys.Vec3.atCenterOf(endPos));
    }
}
