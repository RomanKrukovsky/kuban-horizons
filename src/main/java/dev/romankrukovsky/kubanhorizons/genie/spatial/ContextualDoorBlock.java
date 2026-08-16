package dev.romankrukovsky.kubanhorizons.genie.spatial;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Дверь с контекстным выходом (GENIE_VISION §Мир): дверь ведёт в разное место
 * в зависимости от стороны входа.
 *
 * <p>Из обычного мира она открывается в покет-измерение джиннии
 * ({@code kubanhorizons:pocket}); изнутри покета та же дверь возвращает игрока
 * в исходную точку. Так одна дверь служит порталом в обе стороны, а выход
 * «запоминает» контекст — куда возвращаться.</p>
 */
public final class ContextualDoorBlock extends Block {

    public ContextualDoorBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hitResult) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.SUCCESS;
        }
        ServerLevel serverLevel = (ServerLevel) level;

        if (dev.romankrukovsky.kubanhorizons.genie.dimension.PocketSceneEngine
                .isPocketLevel(serverLevel)) {
            // Внутри покета — возвращаем в исходный мир.
            dev.romankrukovsky.kubanhorizons.genie.dimension.ContextualDoorMemory.leave(serverPlayer);
        } else {
            // Снаружи — открываемся в покет и запоминаем исходную точку.
            boolean entered = dev.romankrukovsky.kubanhorizons.genie.dimension.ContextualDoorMemory
                    .enter(serverLevel, serverPlayer);
            if (!entered) {
                player.sendSystemMessage(Component.translatable(
                        "wish.kubanhorizons.door.missing"));
                return InteractionResult.FAIL;
            }
        }
        serverLevel.playSound(null, pos, SoundEvents.WOODEN_DOOR_OPEN,
                SoundSource.BLOCKS, 0.9F, 1.1F);
        return InteractionResult.CONSUME;
    }
}