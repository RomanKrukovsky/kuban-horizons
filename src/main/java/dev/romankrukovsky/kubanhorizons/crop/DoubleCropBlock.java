package dev.romankrukovsky.kubanhorizons.crop;

import dev.romankrukovsky.kubanhorizons.config.KHServerConfig;
import dev.romankrukovsky.kubanhorizons.soil.SoilFertility;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

import java.util.function.Function;

/**
 * Базовый класс двухблочных культур мода (подсолнечник, кукуруза).
 *
 * <p>Модель поведения повторяет ванильный {@code PitcherCropBlock}:
 * до порога {@link #doubleAge()} растение одноблочное, после — появляется
 * верхняя половина. Рост учитывает влажность почвы (ванильная формула),
 * плодородие и конфигурируемую скорость. Сбор зрелого растения истощает
 * грядку через {@link SoilFertility}.</p>
 */
public abstract class DoubleCropBlock extends DoublePlantBlock implements BonemealableBlock {
    public static final IntegerProperty AGE = BlockStateProperties.AGE_4;
    public static final EnumProperty<DoubleBlockHalf> HALF = DoublePlantBlock.HALF;

    private final Function<BlockState, VoxelShape> shapes;

    protected DoubleCropBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(AGE, 0)
                .setValue(HALF, DoubleBlockHalf.LOWER));
        this.shapes = this.makeShapes();
    }

    /** Максимальная стадия роста. */
    public int maxAge() {
        return 4;
    }

    /** Стадия, начиная с которой растение занимает два блока. */
    protected int doubleAge() {
        return 3;
    }

    /** Высота растения в пикселях по стадиям (для VoxelShape). */
    protected int[] stageHeights() {
        return new int[]{4, 8, 14, 24, 30};
    }

    /** Предмет-семена культуры. */
    protected abstract ItemStack seedStack();

    private Function<BlockState, VoxelShape> makeShapes() {
        int[] heights = stageHeights();
        return this.getShapeForEachState(state -> {
            int height = heights[Math.min(state.getValue(AGE), heights.length - 1)];
            return switch (state.getValue(HALF)) {
                case LOWER -> Block.column(12.0, 0.0, Math.min(16, height));
                case UPPER -> Block.column(12.0, 0.0, Math.max(0, height - 16));
            };
        });
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState();
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return this.shapes.apply(state);
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    public BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos,
            Direction directionToNeighbour, BlockPos neighbourPos, BlockState neighbourState, RandomSource random) {
        if (isDouble(state.getValue(AGE))) {
            return super.updateShape(state, level, ticks, pos, directionToNeighbour, neighbourPos, neighbourState, random);
        }
        return state.canSurvive(level, pos) ? state : Blocks.AIR.defaultBlockState();
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        if (isLower(state) && !isDouble(state.getValue(AGE))) {
            var soil = level.getBlockState(pos.below());
            return soil.canSustainPlant(level, pos.below(), Direction.UP, state).isTrue()
                    || this.mayPlaceOn(soil, level, pos.below());
        }
        return super.canSurvive(state, level, pos);
    }

    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return state.is(BlockTags.SUPPORTS_CROPS);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AGE);
        super.createBlockStateDefinition(builder);
    }

    @Override
    public boolean canBeReplaced(BlockState state, BlockPlaceContext context) {
        return false;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity by, ItemStack itemStack) {
        // Верхняя половина создаётся только ростом.
    }

    @Override
    public boolean isRandomlyTicking(BlockState state) {
        return state.getValue(HALF) == DoubleBlockHalf.LOWER && !isMaxAge(state);
    }

    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        float growthSpeed = CropGrowth.growthSpeed(state, level, pos)
                * SoilFertility.growthMultiplier(level, pos.below())
                * (float) KHServerConfig.cropGrowthSpeed();
        if (growthSpeed > 0 && random.nextInt((int) (25.0F / growthSpeed) + 1) == 0) {
            this.grow(level, state, pos, 1);
        }
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (level instanceof ServerLevel serverLevel && isMaxAge(state)) {
            BlockPos farmland = state.getValue(HALF) == DoubleBlockHalf.LOWER
                    ? pos.below()
                    : pos.below(2);
            SoilFertility.onHarvest(serverLevel, farmland, this);
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    protected void grow(ServerLevel level, BlockState lowerState, BlockPos lowerPos, int increase) {
        int newAge = Math.min(lowerState.getValue(AGE) + increase, maxAge());
        if (this.canGrow(level, lowerPos, lowerState, newAge)) {
            BlockState newLowerState = lowerState.setValue(AGE, newAge);
            level.setBlock(lowerPos, newLowerState, 2);
            if (isDouble(newAge)) {
                level.setBlock(lowerPos.above(), newLowerState.setValue(HALF, DoubleBlockHalf.UPPER), 3);
            }
        }
    }

    private boolean canGrow(LevelReader level, BlockPos lowerPos, BlockState lowerState, int newAge) {
        return !isMaxAge(lowerState)
                && CropBlock.hasSufficientLight(level, lowerPos)
                && level.isInsideBuildHeight(lowerPos.above())
                && (!isDouble(newAge) || canGrowInto(level, lowerPos.above()));
    }

    private boolean canGrowInto(LevelReader level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.isAir() || state.is(this);
    }

    private boolean isLower(BlockState state) {
        return state.is(this) && state.getValue(HALF) == DoubleBlockHalf.LOWER;
    }

    protected boolean isDouble(int age) {
        return age >= doubleAge();
    }

    public boolean isMaxAge(BlockState state) {
        return state.getValue(AGE) >= maxAge();
    }

    @Override
    protected ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state, boolean includeData) {
        return seedStack();
    }

    private @Nullable PosAndState getLowerHalf(LevelReader level, BlockPos pos, BlockState state) {
        if (isLower(state)) {
            return new PosAndState(pos, state);
        }
        BlockPos lowerPos = pos.below();
        BlockState lowerState = level.getBlockState(lowerPos);
        return isLower(lowerState) ? new PosAndState(lowerPos, lowerState) : null;
    }

    // --- Костная мука ---

    @Override
    public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state) {
        PosAndState lower = this.getLowerHalf(level, pos, state);
        return lower != null && this.canGrow(level, lower.pos(), lower.state(), lower.state().getValue(AGE) + 1);
    }

    @Override
    public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state) {
        return true;
    }

    @Override
    public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state) {
        PosAndState lower = this.getLowerHalf(level, pos, state);
        if (lower != null) {
            this.grow(level, lower.state(), lower.pos(), 1);
        }
    }

    private record PosAndState(BlockPos pos, BlockState state) {
    }
}
