package dev.romankrukovsky.kubanhorizons.item;

import dev.romankrukovsky.kubanhorizons.soil.SoilFertility;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FarmlandBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Почвенный щуп — анализатор плодородия.
 *
 * <p>ПКМ по грядке показывает в чате уровень плодородия, влажность и совет
 * по севообороту. Работает только на серверной стороне; клиент получает
 * готовое сообщение.</p>
 */
public class SoilProbeItem extends Item {
    public SoilProbeItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);
        Player player = context.getPlayer();

        if (player == null) {
            return InteractionResult.PASS;
        }
        // Щуп применяется к грядке либо к земле.
        boolean isFarmland = state.getBlock() instanceof FarmlandBlock;
        boolean isSoil = isFarmland || state.is(Blocks.DIRT) || state.is(Blocks.GRASS_BLOCK);
        if (!isSoil) {
            return InteractionResult.PASS;
        }
        if (!(level instanceof ServerLevel serverLevel)) {
            return InteractionResult.SUCCESS;
        }

        if (!isFarmland) {
            player.sendSystemMessage(
                    Component.translatable("message.kubanhorizons.soil_probe.not_farmland"));
            return InteractionResult.CONSUME;
        }

        int fertility = SoilFertility.fertility(serverLevel, pos);
        int moisture = state.getValue(FarmlandBlock.MOISTURE);

        String levelKey = fertility >= 70 ? "rich" : fertility >= 40 ? "normal" : "poor";
        ChatFormatting color = fertility >= 70 ? ChatFormatting.GREEN
                : fertility >= 40 ? ChatFormatting.YELLOW : ChatFormatting.RED;

        player.sendSystemMessage(Component.translatable(
                "message.kubanhorizons.soil_probe.result",
                Component.literal(String.valueOf(fertility)).withStyle(color),
                Component.translatable("message.kubanhorizons.soil_probe." + levelKey).withStyle(color),
                moisture));
        if (fertility < 40) {
            player.sendSystemMessage(
                    Component.translatable("message.kubanhorizons.soil_probe.advice")
                            .withStyle(ChatFormatting.GRAY));
        }
        return InteractionResult.CONSUME;
    }
}
