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
import net.minecraft.world.level.block.VegetationBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Чайный куст — многолетняя многосборная культура.
 *
 * <p>Стадии {@code AGE 0..3}: 0–1 — молодой куст, 2 — созревающий,
 * 3 — куст с готовым к сбору листом. Сбор (ПКМ) даёт чайный лист и
 * возвращает куст к стадии 1 — растение не уничтожается (паттерн
 * сладких ягод). Куст не требует грядки: растёт на земле и траве.</p>
 */
public class TeaBushBlock extends VegetationBlock implements BonemealableBlock {
    public static final MapCodec<TeaBushBlock> CODEC = simpleCodec(TeaBushBlock::new);

    public static final int MAX_AGE = 3;
    public static final IntegerProperty AGE = BlockStateProperties.AGE_3;

    private static final VoxelShape SHAPE_YOUNG = Block.column(10.0, 0.0, 8.0);
    private static final VoxelShape SHAPE_GROWN = Block.column(14.0, 0.0, 14.0);

    public TeaBushBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(AGE, 0));
    }

    @Override
    public MapCodec<TeaBushBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AGE);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(AGE) <= 1 ? SHAPE_YOUNG : SHAPE_GROWN;
    }

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return state.getValue(AGE) < MAX_AGE;
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        int age = state.getValue(AGE);
        if (age >= MAX_AGE || level.getRawBrightness(pos.above(), 0) < 9) {
            return;
        }
        float speed = SoilFertility.growthMultiplier(level, pos.below())
                * (float) KHServerConfig.cropGrowthSpeed();
        // Куст растёт медленнее однолетних культур: базовый шанс 1/6.
        if (random.nextInt((int) (6.0F / Math.max(0.1F, speed)) + 1) == 0) {
            level.setBlock(pos, state.setValue(AGE, age + 1), 2);
        }
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (state.getValue(AGE) == MAX_AGE) {
            if (level instanceof ServerLevel serverLevel) {
                int count = 1 + serverLevel.getRandom().nextInt(2);
                Block.popResource(serverLevel, pos, new ItemStack(KHItems.TEA_LEAVES.get(), count));
                serverLevel.playSound(null, pos, SoundEvents.SWEET_BERRY_BUSH_PICK_BERRIES,
                        SoundSource.BLOCKS, 1.0F, 0.8F + serverLevel.getRandom().nextFloat() * 0.4F);
                serverLevel.setBlock(pos, state.setValue(AGE, 1), 2);
                serverLevel.gameEvent(GameEvent.BLOCK_CHANGE, pos,
                        GameEvent.Context.of(player, state));
            }
            return InteractionResult.SUCCESS;
        }
        return super.useWithoutItem(state, level, pos, player, hitResult);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack itemStack, BlockState state, Level level, BlockPos pos,
            Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (itemStack.is(Items.BONE_MEAL) && state.getValue(AGE) < MAX_AGE) {
            // Пропускаем к обработке костной муки предметом.
            return InteractionResult.PASS;
        }
        return super.useItemOn(itemStack, state, level, pos, player, hand, hitResult);
    }

    // --- Костная мука ---

    @Override
    public boolean isValidBonemealTarget(net.minecraft.world.level.LevelReader level, BlockPos pos, BlockState state) {
        return state.getValue(AGE) < MAX_AGE;
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
