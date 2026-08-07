package dev.romankrukovsky.kubanhorizons.processing;

import com.mojang.serialization.MapCodec;
import dev.romankrukovsky.kubanhorizons.blockentity.CuttingBoardBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * Разделочный стол.
 *
 * <p>ПКМ с продуктом — положить; ПКМ с ножом/инструментом — нарезать;
 * ПКМ пустой рукой — забрать. Работает без GUI, как ванильный костёр.</p>
 */
public class CuttingBoardBlock extends BaseEntityBlock {
    public static final MapCodec<CuttingBoardBlock> CODEC = simpleCodec(CuttingBoardBlock::new);
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;

    private static final VoxelShape SHAPE = Block.column(16.0, 0.0, 9.0);

    public CuttingBoardBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<CuttingBoardBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState()
                .setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos,
                                  CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected InteractionResult useItemOn(ItemStack itemStack, BlockState state, Level level,
                                          BlockPos pos, Player player, InteractionHand hand,
                                          BlockHitResult hitResult) {
        if (itemStack.isEmpty()) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        if (level instanceof ServerLevel serverLevel
                && level.getBlockEntity(pos) instanceof CuttingBoardBlockEntity board) {
            // Сначала пробуем резать тем, что в руке: если на столе уже лежит
            // подходящий продукт, игрок ожидает именно нарезки, а не подмены.
            if (board.cut(serverLevel, itemStack)) {
                serverLevel.playSound(null, pos, SoundEvents.WOOD_BREAK,
                        SoundSource.BLOCKS, 0.7F, 1.6F);
                return InteractionResult.SUCCESS;
            }
            if (board.place(serverLevel, itemStack)) {
                serverLevel.playSound(null, pos, SoundEvents.ITEM_FRAME_ADD_ITEM,
                        SoundSource.BLOCKS, 0.8F, 1.0F);
                return InteractionResult.SUCCESS;
            }
            return InteractionResult.PASS;
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hitResult) {
        if (level instanceof ServerLevel serverLevel
                && level.getBlockEntity(pos) instanceof CuttingBoardBlockEntity board) {
            ItemStack taken = board.take(serverLevel);
            if (taken.isEmpty()) {
                return InteractionResult.PASS;
            }
            if (!player.getInventory().add(taken)) {
                player.drop(taken, false);
            }
            serverLevel.playSound(null, pos, SoundEvents.ITEM_FRAME_REMOVE_ITEM,
                    SoundSource.BLOCKS, 0.8F, 1.0F);
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CuttingBoardBlockEntity(pos, state);
    }
}
