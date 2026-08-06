package dev.romankrukovsky.kubanhorizons.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * Резной оконный наличник — тонкая декоративная рама, устанавливаемая
 * лицевой стороной к игроку.
 */
public final class CarvedWindowCasingBlock extends HorizontalDirectionalBlock {
    public static final MapCodec<CarvedWindowCasingBlock> CODEC = simpleCodec(CarvedWindowCasingBlock::new);

    private static final VoxelShape NORTH_SOUTH_SHAPE = Shapes.or(
            Block.box(0.0, 0.0, 6.0, 3.0, 16.0, 10.0),
            Block.box(13.0, 0.0, 6.0, 16.0, 16.0, 10.0),
            Block.box(3.0, 13.0, 6.0, 13.0, 16.0, 10.0),
            Block.box(3.0, 0.0, 6.0, 13.0, 3.0, 10.0));
    private static final VoxelShape EAST_WEST_SHAPE = Shapes.or(
            Block.box(6.0, 0.0, 0.0, 10.0, 16.0, 3.0),
            Block.box(6.0, 0.0, 13.0, 10.0, 16.0, 16.0),
            Block.box(6.0, 13.0, 3.0, 10.0, 16.0, 13.0),
            Block.box(6.0, 0.0, 3.0, 10.0, 3.0, 13.0));

    public CarvedWindowCasingBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(FACING).getAxis() == Direction.Axis.Z
                ? NORTH_SOUTH_SHAPE
                : EAST_WEST_SHAPE;
    }
}
