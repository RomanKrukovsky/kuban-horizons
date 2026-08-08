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
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * Укрытие для манула: дрова, сено и камень — то, из чего казак сложил бы
 * дровяник у сарая.
 *
 * <p>Зачем блок вообще нужен: без него манул остаётся случайной встречей в
 * степи, которую игрок увидел один раз и забыл. Укрытие — это единственный
 * способ превратить встречу в постоянного жителя двора: манул выбирает
 * поставленное игроком укрытие своим домом и держится рядом. Роль в
 * экосистеме — не декоративная: только у поселившегося манула доверие
 * растёт до максимума и открываются достижения «Опора станицы» и «Манул
 * тебя терпит».</p>
 *
 * <p>Состояние {@link #OCCUPIED} — обратная связь, а не украшение: пока
 * укрытие пусто, игрок видит открытый лаз и понимает, что манул ещё не
 * пришёл; когда манул поселился, лаз затемняется соломой. Без видимого
 * признака игрок не мог бы отличить работающее укрытие от бесполезного и
 * решил бы, что механика сломана.</p>
 *
 * <p>Блок намеренно не {@code BlockEntity}: занятость — один бит, а
 * состояние блока и так сохраняется в чанке. Блочная сущность здесь дала бы
 * тикающий объект и NBT ради одного флага.</p>
 */
public final class ManulShelterBlock extends HorizontalDirectionalBlock {
    public static final MapCodec<ManulShelterBlock> CODEC = simpleCodec(ManulShelterBlock::new);

    /** Признак «в укрытии живёт манул». Меняет модель и служит меткой для ИИ. */
    public static final BooleanProperty OCCUPIED = BlockStateProperties.OCCUPIED;

    /**
     * Укрытие ниже полного блока: манул должен пролезать под ним, а игрок —
     * спокойно перешагивать, не застревая во дворе.
     */
    private static final VoxelShape SHAPE = Block.column(16.0D, 0.0D, 12.0D);

    public ManulShelterBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(OCCUPIED, Boolean.FALSE));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, OCCUPIED);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        // Лаз смотрит на игрока: так укрытие ставится «входом к себе» без
        // возни с разворотом.
        return defaultBlockState()
                .setValue(FACING, context.getHorizontalDirection().getOpposite())
                .setValue(OCCUPIED, Boolean.FALSE);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos,
                                  CollisionContext context) {
        return SHAPE;
    }

    /**
     * Помечает укрытие занятым/свободным.
     *
     * <p>Отдельный метод, а не прямая запись состояния из ИИ: занятость
     * должна меняться в одном месте, иначе рассинхронизация между «манул
     * считает дом своим» и «блок выглядит пустым» неизбежна.</p>
     */
    public static void setOccupied(net.minecraft.world.level.Level level, BlockPos pos,
                                   boolean occupied) {
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof ManulShelterBlock
                && state.getValue(OCCUPIED) != occupied) {
            level.setBlock(pos, state.setValue(OCCUPIED, occupied), Block.UPDATE_ALL);
        }
    }

    /** Занято ли укрытие в этой позиции. */
    public static boolean isOccupied(BlockState state) {
        return state.getBlock() instanceof ManulShelterBlock && state.getValue(OCCUPIED);
    }
}
