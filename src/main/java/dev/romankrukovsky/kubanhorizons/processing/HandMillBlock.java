package dev.romankrukovsky.kubanhorizons.processing;

import com.mojang.serialization.MapCodec;
import dev.romankrukovsky.kubanhorizons.blockentity.HandMillBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * Ручная мельница (жёрнов).
 *
 * <p>ПКМ с зерном — засыпать; ПКМ пустой рукой — оборот жёрнова;
 * shift+ПКМ пустой рукой — забрать сырьё.</p>
 */
public class HandMillBlock extends BaseEntityBlock {
    public static final MapCodec<HandMillBlock> CODEC = simpleCodec(HandMillBlock::new);

    private static final VoxelShape SHAPE = Block.column(14.0, 0.0, 8.0);

    public HandMillBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<HandMillBlock> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected InteractionResult useItemOn(ItemStack itemStack, BlockState state, Level level, BlockPos pos,
            Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (itemStack.isEmpty()) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        if (level instanceof ServerLevel serverLevel
                && level.getBlockEntity(pos) instanceof HandMillBlockEntity mill) {
            if (mill.insert(serverLevel, itemStack)) {
                serverLevel.playSound(null, pos, SoundEvents.COMPOSTER_FILL,
                        SoundSource.BLOCKS, 0.8F, 1.0F);
                return InteractionResult.SUCCESS;
            }
            return InteractionResult.PASS;
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (level instanceof ServerLevel serverLevel
                && level.getBlockEntity(pos) instanceof HandMillBlockEntity mill) {
            if (player.isShiftKeyDown()) {
                ItemStack taken = mill.removeInput(serverLevel);
                if (!taken.isEmpty()) {
                    if (!player.getInventory().add(taken)) {
                        player.drop(taken, false);
                    }
                    return InteractionResult.SUCCESS;
                }
                return InteractionResult.PASS;
            }
            if (mill.turn(serverLevel)) {
                serverLevel.playSound(null, pos, SoundEvents.GRINDSTONE_USE,
                        SoundSource.BLOCKS, 0.6F,
                        0.8F + serverLevel.getRandom().nextFloat() * 0.3F);
                return InteractionResult.SUCCESS;
            }
        }
        return InteractionResult.PASS;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new HandMillBlockEntity(pos, state);
    }
}
