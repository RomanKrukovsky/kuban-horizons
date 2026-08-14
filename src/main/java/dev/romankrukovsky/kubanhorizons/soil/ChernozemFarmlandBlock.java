package dev.romankrukovsky.kubanhorizons.soil;

import com.mojang.serialization.MapCodec;
import dev.romankrukovsky.kubanhorizons.registry.KHBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FarmlandBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import org.jspecify.annotations.Nullable;

/**
 * Вспаханный чернозём — грядка высшего яруса почвы.
 *
 * <p>Наследуется от ванильной {@link FarmlandBlock} намеренно: на проверке
 * {@code instanceof FarmlandBlock} держатся щуп, сухой ветер, половодье,
 * отладочный оверлей и стандартное {@code isFertile} NeoForge (влажность даёт
 * ванильный бонус роста). Отдельный класс «с нуля» пришлось бы прописывать во
 * все эти места руками, и любое забытое место молча теряло бы новый ярус.</p>
 *
 * <p>Главная причина существования класса — ванильный
 * {@link FarmlandBlock#turnToDirt}: он <b>жёстко</b> возвращает
 * {@code Blocks.DIRT}. Без переопределения пересохшая или вытоптанная грядка
 * чернозёма превращалась бы в обычную землю, и игрок терял бы принесённый
 * издалека дефицитный блок безвозвратно. Чернозём — ресурс, который ищут и
 * носят, а не расходник: он обязан возвращаться в своё нетронутое состояние.</p>
 */
public class ChernozemFarmlandBlock extends FarmlandBlock {
    /**
     * Кодек объявлен как {@code MapCodec<FarmlandBlock>}, а не по своему типу:
     * {@link FarmlandBlock#codec()} возвращает конкретный тип, и сузить его в
     * наследнике язык не даёт.
     */
    public static final MapCodec<FarmlandBlock> CODEC =
            simpleCodec(ChernozemFarmlandBlock::new);

    public ChernozemFarmlandBlock(Properties properties) {
        super(properties);
    }

    @Override
    public MapCodec<FarmlandBlock> codec() {
        return CODEC;
    }

    /**
     * Состояние, в которое разрушается грядка.
     *
     * <p>Возврат именно в чернозём, а не в землю: это разница между
     * «перепахать заново» и «потерять ресурс».</p>
     */
    private static BlockState revertState() {
        return KHBlocks.CHERNOZEM.get().defaultBlockState();
    }

    /** Полная копия ванильного {@code turnToDirt}, но с нашим блоком. */
    private static void turnToChernozem(@Nullable Entity source, BlockState state,
            Level level, BlockPos pos) {
        BlockState reverted = pushEntitiesUp(state, revertState(), level, pos);
        level.setBlockAndUpdate(pos, reverted);
        level.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(source, reverted));
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        // Придавлен блоком сверху: ванилька здесь ушла бы в DIRT.
        if (!state.canSurvive(level, pos)) {
            turnToChernozem(null, state, level, pos);
        }
    }

    /**
     * Пересыхание: влажность падает, но при нуле грядка становится
     * чернозёмом, а не землёй.
     *
     * <p>Логика повторяет ванильную построчно, потому что расходится с ней
     * только в одном — в блоке-результате. Наследоваться и «дописать сверху»
     * нельзя: ванильный метод сам вызывает свой {@code turnToDirt}.</p>
     */
    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        int moisture = state.getValue(MOISTURE);
        if (!hasWaterNearby(level, pos) && !level.isRainingAt(pos.above())) {
            if (moisture > 0) {
                level.setBlock(pos, state.setValue(MOISTURE, moisture - 1), 2);
            } else if (!isUnderCrop(level, pos)) {
                turnToChernozem(null, state, level, pos);
            }
        } else if (moisture < MAX_MOISTURE) {
            level.setBlock(pos, state.setValue(MOISTURE, MAX_MOISTURE), 2);
        }
    }

    @Override
    public void fallOn(Level level, BlockState state, BlockPos pos, Entity entity, double fallDistance) {
        if (level instanceof ServerLevel serverLevel
                && net.neoforged.neoforge.common.CommonHooks.onFarmlandTrample(
                        serverLevel, pos, revertState(), fallDistance, entity)) {
            turnToChernozem(entity, state, level, pos);
        }
        // Урон от падения наносим сами (тело {@code Block#fallOn}), а не через
        // super: у {@link FarmlandBlock} тот же метод повторно проверил бы
        // топтание и увёл бы блок в DIRT — ровно то, что класс и предотвращает.
        entity.causeFallDamage(fallDistance, 1.0F, entity.damageSources().fall());
    }

    /** Ванильный {@code shouldMaintainFarmland} — под растением грядка живёт. */
    private static boolean isUnderCrop(BlockGetter level, BlockPos pos) {
        return level.getBlockState(pos.above()).is(BlockTags.MAINTAINS_FARMLAND);
    }

    /** Ванильный {@code isNearWater} плюс тикеты орошения NeoForge. */
    private static boolean hasWaterNearby(LevelReader level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        for (BlockPos cursor : BlockPos.betweenClosed(pos.offset(-4, 0, -4), pos.offset(4, 1, 4))) {
            if (state.canBeHydrated(level, pos, level.getFluidState(cursor), cursor)) {
                return true;
            }
        }
        return net.neoforged.neoforge.common.FarmlandWaterManager.hasBlockWaterTicket(level, pos);
    }

    /** Блок, в который грядка возвращается при разрушении. */
    public static Block revertBlock() {
        return KHBlocks.CHERNOZEM.get();
    }
}
