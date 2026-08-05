package dev.romankrukovsky.kubanhorizons.irrigation;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Водозабор — начало оросительной сети.
 *
 * <p>Свойство {@code ACTIVE}: истинно, когда хотя бы одна горизонтальная
 * сторона или блок под водозабором касается воды. Активный водозабор
 * питает прилегающие желоба ({@link IrrigationChannelBlock}).</p>
 */
public class WaterIntakeBlock extends Block {
    public static final MapCodec<WaterIntakeBlock> CODEC = simpleCodec(WaterIntakeBlock::new);
    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");

    private static final VoxelShape SHAPE = Block.column(16.0, 0.0, 12.0);

    public WaterIntakeBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(ACTIVE, false));
    }

    @Override
    protected MapCodec<WaterIntakeBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(ACTIVE);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected void onPlace(BlockState state, net.minecraft.world.level.Level level, BlockPos pos,
            BlockState oldState, boolean movedByPiston) {
        level.scheduleTick(pos, this, 4);
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos,
            Direction directionToNeighbour, BlockPos neighbourPos, BlockState neighbourState, RandomSource random) {
        ticks.scheduleTick(pos, this, 4);
        return state;
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        boolean nearWater = isTouchingWater(level, pos);
        if (state.getValue(ACTIVE) != nearWater) {
            level.setBlock(pos, state.setValue(ACTIVE, nearWater), 2);
            // Пересчёт прилегающих желобов.
            for (Direction dir : Direction.Plane.HORIZONTAL) {
                BlockPos next = pos.relative(dir);
                if (level.getBlockState(next).getBlock() instanceof IrrigationChannelBlock channel) {
                    level.scheduleTick(next, channel, 4);
                }
            }
        }
    }

    private static boolean isTouchingWater(ServerLevel level, BlockPos pos) {
        if (level.getFluidState(pos.below()).is(FluidTags.WATER)) {
            return true;
        }
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            if (level.getFluidState(pos.relative(dir)).is(FluidTags.WATER)) {
                return true;
            }
        }
        return false;
    }
}
