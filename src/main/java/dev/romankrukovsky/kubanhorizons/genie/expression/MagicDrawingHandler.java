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
        level.setBlock(endPos, Blocks.OAK_PLANKS.defaultBlockState(), 3);
        MagicalSignature.cast(level, net.minecraft.world.phys.Vec3.atCenterOf(endPos));
    }
}
