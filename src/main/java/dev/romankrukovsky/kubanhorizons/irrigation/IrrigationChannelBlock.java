package dev.romankrukovsky.kubanhorizons.irrigation;

import com.mojang.serialization.MapCodec;
import dev.romankrukovsky.kubanhorizons.client.KHParticles;
import dev.romankrukovsky.kubanhorizons.config.KHServerConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Оросительный желоб.
 *
 * <p>Свойство {@code DISTANCE 0..N}: 0 — сухой желоб, 1..N — вода на
 * соответствующем удалении от водозабора (1 — вплотную). Заполненный
 * желоб отдаёт {@link FluidState} воды, поэтому ванильная логика
 * {@code FarmlandBlock.isNearWater} увлажняет грядки в радиусе 4 сама —
 * системе не нужны собственные tick-обходы.</p>
 *
 * <p>Распространение — событийное, волной scheduled ticks: изменение
 * соседа планирует один тик этого блока; тик пересчитывает уровень по
 * соседям. Бюджет — O(1) на блок на волну.</p>
 */
public class IrrigationChannelBlock extends Block {
    public static final MapCodec<IrrigationChannelBlock> CODEC = simpleCodec(IrrigationChannelBlock::new);

    /** Максимальная дальность воды от водозабора (в желобах). */
    public static final int MAX_DISTANCE = 12;

    /** 0 — сухо; 1..MAX_DISTANCE — расстояние до водозабора + 1. */
    public static final IntegerProperty DISTANCE =
            IntegerProperty.create("distance", 0, MAX_DISTANCE);

    private static final VoxelShape SHAPE = Shapes.or(
            Block.box(0, 0, 0, 16, 4, 16),
            Block.box(0, 4, 0, 2, 10, 16),
            Block.box(14, 4, 0, 16, 10, 16),
            Block.box(2, 4, 0, 14, 10, 2),
            Block.box(2, 4, 14, 14, 10, 16));

    public IrrigationChannelBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(DISTANCE, 0));
    }

    @Override
    protected MapCodec<? extends IrrigationChannelBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(DISTANCE);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    /** Заполненный желоб считается источником воды для гидратации грядок. */
    @Override
    protected FluidState getFluidState(BlockState state) {
        return state.getValue(DISTANCE) > 0
                ? Fluids.WATER.getSource(false)
                : super.getFluidState(state);
    }

    /**
     * Редкие блики воды над заполненным желобом.
     *
     * <p>Желоб с водой и сухой желоб отличались только цветом текстуры:
     * работает ли сеть, приходилось угадывать. Частица делает состояние
     * видимым в движении — там, где вода дошла, она поблёскивает.</p>
     *
     * <p>Проходит через {@link KHParticles}: настройка {@code
     * particles.density} обязана действовать на все декоративные частицы
     * мода, а не на одну листву. Метод клиентский, поэтому чтение
     * клиентского конфига здесь законно.</p>
     */
    @Override
    public void animateTick(BlockState state, net.minecraft.world.level.Level level, BlockPos pos,
            RandomSource random) {
        if (state.getValue(DISTANCE) <= 0) {
            return;
        }
        // Редко: желоб — фон хозяйства, а не фонтан.
        if (random.nextInt(24) != 0 || !KHParticles.allow(random)) {
            return;
        }
        level.addParticle(net.minecraft.core.particles.ParticleTypes.SPLASH,
                pos.getX() + 0.25D + random.nextDouble() * 0.5D,
                pos.getY() + 0.6D,
                pos.getZ() + 0.25D + random.nextDouble() * 0.5D,
                0.0D, 0.0D, 0.0D);
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos,
            Direction directionToNeighbour, BlockPos neighbourPos, BlockState neighbourState, RandomSource random) {
        // Изменение соседа → один отложенный пересчёт уровня.
        ticks.scheduleTick(pos, this, 4);
        return state;
    }

    @Override
    protected void onPlace(BlockState state, net.minecraft.world.level.Level level, BlockPos pos,
            BlockState oldState, boolean movedByPiston) {
        level.scheduleTick(pos, this, 4);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        int computed = computeDistance(level, pos);
        int current = state.getValue(DISTANCE);
        if (computed != current) {
            level.setBlock(pos, state.setValue(DISTANCE, computed), 2);
            // Волна: соседние желоба пересчитаются через updateShape/
            // neighborChanged, но надёжнее запланировать их явно.
            for (Direction dir : Direction.Plane.HORIZONTAL) {
                BlockPos next = pos.relative(dir);
                if (level.getBlockState(next).getBlock() instanceof IrrigationChannelBlock) {
                    level.scheduleTick(next, level.getBlockState(next).getBlock(), 4);
                }
            }
        }
    }

    /**
     * Уровень = (мин. дистанция среди соседей-источников) с затуханием.
     * Водозабор даёт дистанцию 1; желоб с дистанцией d — d+1 (до предела).
     */
    private int computeDistance(ServerLevel level, BlockPos pos) {
        if (!KHServerConfig.irrigationEnabled()) {
            return 0;
        }
        int best = 0;
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            BlockState neighbour = level.getBlockState(pos.relative(dir));
            if (neighbour.getBlock() instanceof WaterIntakeBlock
                    && neighbour.getValue(WaterIntakeBlock.ACTIVE)) {
                return 1;
            }
            if (neighbour.getBlock() instanceof IrrigationChannelBlock) {
                int d = neighbour.getValue(DISTANCE);
                if (d > 0 && d < MAX_DISTANCE) {
                    int candidate = d + 1;
                    if (best == 0 || candidate < best) {
                        best = candidate;
                    }
                }
            }
        }
        return best;
    }
}
