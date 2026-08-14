package dev.romankrukovsky.kubanhorizons.processing;

import com.mojang.serialization.MapCodec;
import dev.romankrukovsky.kubanhorizons.blockentity.GrapePressBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * Виноградный пресс — давильный чан (GAME_DESIGN.md §7).
 *
 * <p><b>Почему это не маслопресс с другой текстурой.</b> Маслопресс — станок:
 * винт крутят на месте, ПКМ за ПКМ, устройство ведёт один цикл партии и по
 * конфигу умеет работать само. Чан — открытая ёмкость, в которую <i>заходят
 * ногами</i>: ягоды давятся не оборотом винта, а весом игрока, и не за цикл, а
 * по одной грозди с накоплением сока (см.
 * {@link GrapePressBlockEntity}).</p>
 *
 * <p>Отсюда три различия, которые видит игрок, а не только код:</p>
 * <ul>
 *   <li><b>Топтание.</b> {@link #stepOn} давит ягоду из руки при движении по
 *       чану. Это единственное устройство мода, которое приводится в действие
 *       перемещением, а не кликом; наполнить его можно, просто пройдя по чану
 *       несколько раз с гроздьями в руке.</li>
 *   <li><b>Открытая ёмкость.</b> Форма — низкий чан 9/16 высоты, в который
 *       игрок заходит шагом. Маслопресс — сплошная станина полной высоты.</li>
 *   <li><b>Уровень вместо GUI.</b> {@link #LEVEL} 0..4 показывает, сколько
 *       сока налито. GUI нет: у устройства нет ни партии, ни прогресса, ни
 *       выходных слотов — показывать в меню было бы нечего.</li>
 * </ul>
 *
 * <p>Взаимодействия:</p>
 * <ul>
 *   <li>ПКМ с виноградом (или иным сырьём давки) — раздавить одну единицу;</li>
 *   <li>ПКМ со стеклянной бутылкой — налить бутылку сока, если он набрался;</li>
 *   <li>наступить/пройти по чану с сырьём в руке — раздавить одну единицу;</li>
 *   <li>ПКМ пустой рукой — оценить наполнение (сообщение в статус-строке).</li>
 * </ul>
 *
 * <p>Алкоголя здесь нет и не будет: чан даёт сок, брожение — вне области этого
 * устройства (GAME_DESIGN.md §7, §3 столп «регион с уважением»).</p>
 */
public class GrapePressBlock extends BaseEntityBlock {
    public static final MapCodec<GrapePressBlock> CODEC = simpleCodec(GrapePressBlock::new);

    /** Видимое наполнение чана соком: 0 — пусто, 4 — почти полный. */
    public static final IntegerProperty LEVEL = IntegerProperty.create("level", 0, 4);

    /**
     * Низкий чан: 9/16 высоты — игрок заходит в него шагом, без прыжка.
     *
     * <p>Высота выбрана не декоративно, а под механику. Она должна быть
     * достаточно малой, чтобы игрок мог зайти в чан обычным шагом
     * (ванильный шаг — 0.6 блока), и при этом коллизия обязана существовать:
     * {@link #stepOn} вызывается для блока, на котором игрок <i>стоит</i>.
     * Убери коллизию — игрок провалится и будет топтать пол под чаном.</p>
     */
    private static final VoxelShape SHAPE = Block.column(16.0, 0.0, 9.0);

    public GrapePressBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(LEVEL, 0));
    }

    @Override
    protected MapCodec<GrapePressBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LEVEL);
    }

    /** Пересчёт единиц сока в видимый уровень 0..4. */
    public static int levelFor(int juice, int capacity) {
        if (juice <= 0) {
            return 0;
        }
        // Любое ненулевое количество показывается хотя бы уровнем 1: иначе
        // одна раздавленная гроздь выглядела бы как «ничего не произошло».
        return Math.clamp(1 + (juice * 4) / (capacity + 1), 1, 4);
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
        if (!(level instanceof ServerLevel serverLevel)
                || !(level.getBlockEntity(pos) instanceof GrapePressBlockEntity vat)) {
            return InteractionResult.SUCCESS;
        }
        // Порядок важен: бутылка проверяется первой. Иначе, если для стекла
        // однажды появился бы рецепт давки, игрок не смог бы налить сок тем,
        // что держит в руке.
        ItemStack bottled = vat.drawOff(serverLevel, itemStack);
        if (!bottled.isEmpty()) {
            if (!player.getInventory().add(bottled)) {
                player.drop(bottled, false);
            }
            serverLevel.playSound(null, pos, SoundEvents.BREWING_STAND_BREW,
                    SoundSource.BLOCKS, 0.7F, 1.2F);
            return InteractionResult.SUCCESS;
        }
        if (vat.stomp(serverLevel, itemStack)) {
            this.splash(serverLevel, pos);
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hitResult) {
        if (level instanceof ServerLevel serverLevel
                && level.getBlockEntity(pos) instanceof GrapePressBlockEntity vat) {
            // Пустой рукой чан не опустошить: сок наливается только в тару.
            // Вместо бесполезного действия — честный отчёт о наполнении.
            if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
                serverPlayer.sendSystemMessage(Component.translatable(
                        "message.kubanhorizons.grape_press.level",
                        vat.juice(), GrapePressBlockEntity.CAPACITY), true);
            }
            return InteractionResult.CONSUME;
        }
        return InteractionResult.SUCCESS;
    }

    /**
     * Топтание: проход по чану давит одну единицу сырья из руки.
     *
     * <p>Это и есть основной способ работы устройства. Проверка на игрока
     * обязательна: иначе забежавшая в чан корова «доила» бы его инвентарь,
     * а сок появлялся бы без участия человека — ровно то, чего чан не
     * должен уметь (у него нет пассивного режима).</p>
     */
    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        if (!(level instanceof ServerLevel serverLevel) || !(entity instanceof Player player)) {
            return;
        }
        if (!(level.getBlockEntity(pos) instanceof GrapePressBlockEntity vat)) {
            return;
        }
        ItemStack held = player.getMainHandItem();
        if (vat.stomp(serverLevel, held)) {
            this.splash(serverLevel, pos);
            serverLevel.playSound(null, pos, SoundEvents.HONEY_BLOCK_SLIDE,
                    SoundSource.BLOCKS, 0.6F,
                    0.7F + serverLevel.getRandom().nextFloat() * 0.3F);
        }
    }

    /** Брызги сока: единственная анимация устройства, по ART_BIBLE §4. */
    private void splash(ServerLevel level, BlockPos pos) {
        level.sendParticles(ParticleTypes.FALLING_HONEY,
                pos.getX() + 0.5, pos.getY() + 0.75, pos.getZ() + 0.5,
                6, 0.3, 0.05, 0.3, 0.0);
        level.playSound(null, pos, SoundEvents.SWEET_BERRY_BUSH_PICK_BERRIES,
                SoundSource.BLOCKS, 0.7F, 0.8F);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new GrapePressBlockEntity(pos, state);
    }
}
