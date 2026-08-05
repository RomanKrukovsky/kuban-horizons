package dev.romankrukovsky.kubanhorizons.crop;

import com.mojang.serialization.MapCodec;
import dev.romankrukovsky.kubanhorizons.config.KHServerConfig;
import dev.romankrukovsky.kubanhorizons.registry.KHItems;
import dev.romankrukovsky.kubanhorizons.soil.SoilFertility;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Виноградная шпалера.
 *
 * <p>Пустая шпалера ({@code AGE 0}) — декоративно-функциональный блок.
 * Игрок прививает черенок ({@code grape_cutting}) ПКМ — лоза растёт по
 * стадиям {@code AGE 1..4}; на стадии 4 гроздья собираются ПКМ, лоза
 * откатывается к стадии 2 (многолетник, как чай). Разрушение возвращает
 * шпалеру и, при наличии лозы, черенок.</p>
 */
public class GrapeTrellisBlock extends Block implements BonemealableBlock {
    public static final MapCodec<GrapeTrellisBlock> CODEC = simpleCodec(GrapeTrellisBlock::new);

    /** 0 — пустая шпалера; 1..4 — лоза (4 — спелые гроздья). */
    public static final int MAX_AGE = 4;
    public static final IntegerProperty AGE = IntegerProperty.create("age", 0, MAX_AGE);

    private static final VoxelShape SHAPE = Block.column(14.0, 0.0, 16.0);

    public GrapeTrellisBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(AGE, 0));
    }

    @Override
    protected MapCodec<GrapeTrellisBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AGE);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        int age = state.getValue(AGE);
        return age >= 1 && age < MAX_AGE;
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        int age = state.getValue(AGE);
        if (age < 1 || age >= MAX_AGE || level.getRawBrightness(pos.above(), 0) < 9) {
            return;
        }
        float speed = SoilFertility.growthMultiplier(level, pos.below())
                * (float) KHServerConfig.cropGrowthSpeed();
        // Лоза растёт неторопливо: базовый шанс 1/7.
        if (random.nextInt((int) (7.0F / Math.max(0.1F, speed)) + 1) == 0) {
            level.setBlock(pos, state.setValue(AGE, age + 1), 2);
        }
    }

    @Override
    protected InteractionResult useItemOn(ItemStack itemStack, BlockState state, Level level, BlockPos pos,
            Player player, InteractionHand hand, BlockHitResult hitResult) {
        // Прививка черенка на пустую шпалеру.
        if (state.getValue(AGE) == 0 && itemStack.is(KHItems.GRAPE_CUTTING.get())) {
            if (level instanceof ServerLevel serverLevel) {
                itemStack.consume(1, player);
                serverLevel.setBlock(pos, state.setValue(AGE, 1), 2);
                serverLevel.playSound(null, pos, SoundEvents.AZALEA_PLACE, SoundSource.BLOCKS, 1.0F, 1.0F);
                serverLevel.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(player, state));
            }
            return InteractionResult.SUCCESS;
        }
        if (itemStack.is(Items.BONE_MEAL) && isValidBonemealTarget(level, pos, state)) {
            return InteractionResult.PASS;
        }
        return super.useItemOn(itemStack, state, level, pos, player, hand, hitResult);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        // Сбор спелых гроздьев.
        if (state.getValue(AGE) == MAX_AGE) {
            if (level instanceof ServerLevel serverLevel) {
                int count = 2 + serverLevel.getRandom().nextInt(2);
                Block.popResource(serverLevel, pos, new ItemStack(KHItems.GRAPES.get(), count));
                serverLevel.playSound(null, pos, SoundEvents.SWEET_BERRY_BUSH_PICK_BERRIES,
                        SoundSource.BLOCKS, 1.0F, 0.9F);
                serverLevel.setBlock(pos, state.setValue(AGE, 2), 2);
                serverLevel.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(player, state));
                SoilFertility.onHarvest(serverLevel, pos.below(), this);
            }
            return InteractionResult.SUCCESS;
        }
        return super.useWithoutItem(state, level, pos, player, hitResult);
    }

    // --- Костная мука ---

    @Override
    public boolean isValidBonemealTarget(net.minecraft.world.level.LevelReader level, BlockPos pos, BlockState state) {
        int age = state.getValue(AGE);
        return age >= 1 && age < MAX_AGE;
    }

    @Override
    public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state) {
        return true;
    }

    @Override
    public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state) {
        level.setBlock(pos, state.setValue(AGE, Math.min(MAX_AGE, state.getValue(AGE) + 1)), 2);
    }
}
