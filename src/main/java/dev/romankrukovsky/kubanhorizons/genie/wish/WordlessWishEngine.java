package dev.romankrukovsky.kubanhorizons.genie.wish;

import dev.romankrukovsky.kubanhorizons.entity.KubanGenie;
import dev.romankrukovsky.kubanhorizons.genie.aura.MagicalSignature;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/** Движок желаний без слов (Wordless Intent Perception Engine). */
public final class WordlessWishEngine {
    private WordlessWishEngine() {
    }

    public static boolean checkWordlessIntent(KubanGenie genie, ServerLevel level, ServerPlayer player) {
        if (genie.personality().trust() < 40 && genie.personality().affection() < 40) {
            return false;
        }

        HitResult hit = player.pick(8.0D, 0.0F, false);
        if (hit.getType() == HitResult.Type.BLOCK && hit instanceof BlockHitResult blockHit) {
            BlockPos pos = blockHit.getBlockPos();
            BlockState state = level.getBlockState(pos);

            // 1. Потоптанная/иссушенная грядка -> Восстановление
            if (state.is(Blocks.DIRT) || state.is(Blocks.COARSE_DIRT)) {
                level.setBlock(pos, Blocks.FARMLAND.defaultBlockState(), 3);
                MagicalSignature.cast(level, net.minecraft.world.phys.Vec3.atCenterOf(pos));
                player.sendSystemMessage(Component.translatable("message.kubanhorizons.genie.wordless.farmland"));
                return true;
            }

            // 2. Растрескавшийся или помшанный камень -> Починка
            if (state.is(Blocks.CRACKED_STONE_BRICKS)) {
                level.setBlock(pos, Blocks.STONE_BRICKS.defaultBlockState(), 3);
                MagicalSignature.cast(level, net.minecraft.world.phys.Vec3.atCenterOf(pos));
                player.sendSystemMessage(Component.translatable("message.kubanhorizons.genie.wordless.stone"));
                return true;
            }
        }
        return false;
    }
}
