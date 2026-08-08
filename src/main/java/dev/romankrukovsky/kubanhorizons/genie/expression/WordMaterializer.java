package dev.romankrukovsky.kubanhorizons.genie.expression;

import dev.romankrukovsky.kubanhorizons.genie.aura.MagicalSignature;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;

/** Материализация произнесённых слов в физические 3D-буквы/блоки (Word Materializer). */
public final class WordMaterializer {
    private WordMaterializer() {
    }

    public static void materializeWord(ServerLevel level, Player player, String word) {
        BlockPos pos = player.blockPosition().above(3);
        level.setBlock(pos, Blocks.GOLD_BLOCK.defaultBlockState(), 3);
        MagicalSignature.cast(level, net.minecraft.world.phys.Vec3.atCenterOf(pos));
    }
}
