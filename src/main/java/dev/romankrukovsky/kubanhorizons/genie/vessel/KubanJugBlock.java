package dev.romankrukovsky.kubanhorizons.genie.vessel;

import dev.romankrukovsky.kubanhorizons.KubanHorizons;
import dev.romankrukovsky.kubanhorizons.entity.KubanGenie;
import dev.romankrukovsky.kubanhorizons.genie.GenieAnchor;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

public class KubanJugBlock extends Block implements EntityBlock {

    public KubanJugBlock(Properties properties) {
        super(properties);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new KubanJugBlockEntity(pos, state);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        ServerLevel serverLevel = (ServerLevel) level;
        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof KubanJugBlockEntity jug)) {
            return InteractionResult.PASS;
        }

        if (hand == InteractionHand.MAIN_HAND) {
            // Left click simulation via use (in real implementation we would use attack block)
            // For now treat main hand as summon/teleport
            KubanGenie genie = jug.getOrSummonGenie(serverLevel, player);
            if (genie != null) {
                level.playSound(null, pos, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.BLOCKS, 0.8f, 1.2f);
                // TODO: play spawn animation via GeckoLib
            }
            return InteractionResult.CONSUME;
        } else {
            // Right click - look inside
            if (jug.hasGenie()) {
                player.sendSystemMessage(net.minecraft.network.chat.Component.literal("Джинния недовольно бормочет внутри кувшина..."));
            } else {
                player.sendSystemMessage(net.minecraft.network.chat.Component.literal("Кувшин пуст. Стань его хозяином."));
            }
            return InteractionResult.CONSUME;
        }
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof KubanJugBlockEntity jug) {
                jug.onRemoved();
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }
}
