package dev.romankrukovsky.kubanhorizons.genie.memory;

import dev.romankrukovsky.kubanhorizons.entity.KubanGenie;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** Движок перевода мыслей и воспоминаний старинных блоков (Block Whispers Engine). */
public final class BlockWhispersEngine {
    private BlockWhispersEngine() {
    }

    public static boolean listenToBlock(KubanGenie genie, Level level, net.minecraft.world.entity.player.Player player, BlockPos pos) {
        BlockState state = level.getBlockState(pos);

        if (state.is(Blocks.BELL)) {
            player.sendSystemMessage(Component.translatable("message.kubanhorizons.genie.whisper.bell"));
            return true;
        }
        if (state.is(Blocks.NETHER_PORTAL)) {
            player.sendSystemMessage(Component.translatable("message.kubanhorizons.genie.whisper.portal"));
            return true;
        }
        if (state.is(Blocks.CHISELED_STONE_BRICKS) || state.is(Blocks.ANCIENT_DEBRIS)) {
            player.sendSystemMessage(Component.translatable("message.kubanhorizons.genie.whisper.ancient"));
            return true;
        }
        return false;
    }
}
