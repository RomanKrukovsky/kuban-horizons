package dev.romankrukovsky.kubanhorizons.crop;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.romankrukovsky.kubanhorizons.config.KHServerConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.util.ParticleUtils;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;

import java.util.function.Supplier;

/**
 * Плодоносящая листва фруктового дерева.
 *
 * <p>Стадии {@code AGE 0..2}: 0 — обычная листва, 1 — цветение,
 * 2 — спелые плоды. ПКМ по стадии 2 даёт 1–2 плода и возвращает листву
 * к стадии 0 (паттерн {@link TeaBushBlock}). Decay стандартный листвяной:
 * посаженная/выращенная саженцем листва ставится с {@code PERSISTENT=true}
 * и не осыпается.</p>
 */
public class FruitLeavesBlock extends LeavesBlock {
    /** Кодек восстанавливает плод по registry-id (roundtrip-совместим). */
    public static final MapCodec<FruitLeavesBlock> CODEC = RecordCodecBuilder.mapCodec(
            i -> i.group(
                    BuiltInRegistries.ITEM.byNameCodec().fieldOf("fruit").forGetter(b -> b.fruit.get()),
                    propertiesCodec()
            ).apply(i, (fruit, props) -> new FruitLeavesBlock(() -> fruit, props)));

    public static final int MAX_AGE = 2;
    public static final IntegerProperty AGE = BlockStateProperties.AGE_2;

    /** Зелёная листвяная частица (как у азалии). */
    private static final ColorParticleOption LEAF_PARTICLE =
            ColorParticleOption.create(ParticleTypes.TINTED_LEAVES, -9399763);

    private final Supplier<? extends Item> fruit;

    public FruitLeavesBlock(Supplier<? extends Item> fruit, BlockBehaviour.Properties properties) {
        super(0.01F, properties);
        this.fruit = fruit;
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(DISTANCE, 7)
                .setValue(PERSISTENT, false)
                .setValue(WATERLOGGED, false)
                .setValue(AGE, 0));
    }

    @Override
    public MapCodec<? extends LeavesBlock> codec() {
        return CODEC;
    }

    /**
     * Осыпание листвы с учётом настройки плотности частиц.
     *
     * <p>Проверка не в {@code animateTick}, а здесь: это единственное место,
     * где мод сам решает выпустить частицу, и настройка {@code
     * particles.density} обязана влиять именно на него. Метод вызывается
     * только на клиенте (из ванильного {@code animateTick}), поэтому чтение
     * клиентского конфига безопасно.</p>
     */
    @Override
    protected void spawnFallingLeavesParticle(Level level, BlockPos pos, RandomSource random) {
        if (!dev.romankrukovsky.kubanhorizons.client.KHParticles.allow(random)) {
            return;
        }
        ParticleUtils.spawnParticleBelow(level, pos, random, LEAF_PARTICLE);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(AGE);
    }

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return state.getValue(AGE) < MAX_AGE || super.isRandomlyTicking(state);
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (this.decaying(state)) {
            // Стандартное осыпание непостоянной листвы.
            super.randomTick(state, level, pos, random);
            return;
        }
        int age = state.getValue(AGE);
        if (age < MAX_AGE) {
            float speed = (float) KHServerConfig.cropGrowthSpeed();
            // Медленное созревание: базовый шанс 1/10.
            if (random.nextInt((int) (10.0F / Math.max(0.1F, speed)) + 1) == 0) {
                level.setBlock(pos, state.setValue(AGE, age + 1), 2);
            }
        }
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (state.getValue(AGE) == MAX_AGE) {
            if (level instanceof ServerLevel serverLevel) {
                int count = 1 + serverLevel.getRandom().nextInt(2);
                Block.popResource(serverLevel, pos, new ItemStack(this.fruit.get(), count));
                serverLevel.playSound(null, pos, SoundEvents.SWEET_BERRY_BUSH_PICK_BERRIES,
                        SoundSource.BLOCKS, 1.0F, 0.8F + serverLevel.getRandom().nextFloat() * 0.4F);
                serverLevel.setBlock(pos, state.setValue(AGE, 0), 2);
                serverLevel.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(player, state));
            }
            return InteractionResult.SUCCESS;
        }
        return super.useWithoutItem(state, level, pos, player, hitResult);
    }
}
