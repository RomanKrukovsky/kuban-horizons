package dev.romankrukovsky.kubanhorizons.blockentity;

import dev.romankrukovsky.kubanhorizons.processing.SmokehouseBlock;
import dev.romankrukovsky.kubanhorizons.processing.SmokingProcessRecipe;
import dev.romankrukovsky.kubanhorizons.registry.KHBlockEntities;
import dev.romankrukovsky.kubanhorizons.registry.KHRecipes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * Коптильня: 2 слота продукта плюс запас дров.
 *
 * <p>Без GUI, как и остальные «крестьянские» устройства мода: продукт и дрова
 * кладутся ПКМ, готовое забирается ПКМ пустой рукой.</p>
 *
 * <h2>Почему это не вторая сушильная рама</h2>
 *
 * <p>Различие механическое, а не косметическое, и держится на одном правиле:
 * <b>копчение расходует дрова</b>. Рама требует только терпения — время идёт
 * само, игроку достаточно положить предмет и уйти. Коптильня требует
 * <i>снабжения</i>: пока {@link #fuelTicks} равен нулю, прогресс не двигается
 * ни на тик, сколько бы игрок ни ждал. Поэтому у двух устройств разная цена:
 * рама берёт время, коптильня — время и лес.</p>
 *
 * <p>Отсюда же следует, почему коптильня не «медленная печь»: печь тратит
 * топливо, но выдаёт то же жареное мясо, что и костёр. Коптильня выдаёт
 * отдельный продукт — копчёность с бо́льшим насыщением
 * ({@code KHFoods.SMOKED_FISH} / {@code SMOKED_MEAT}), которого не получить
 * никаким другим способом.</p>
 *
 * <h2>Что считается дровами</h2>
 *
 * <p>Принимаются брёвна ({@link ItemTags#LOGS_THAT_BURN}) и доски
 * ({@link ItemTags#PLANKS}). Взят именно {@code LOGS_THAT_BURN}, а не
 * {@code LOGS}: последний включает незерские стволы, которые в ваниле не
 * горят, и коптильня на незер-древесине выглядела бы багом. Доски дают
 * вчетверо меньше времени горения, чем бревно ({@link #FUEL_TICKS_PLANK}
 * против {@link #FUEL_TICKS_LOG}) — ровно в пропорции крафта 1 бревно = 4
 * доски, чтобы у игрока не было выгоды распускать брёвна перед топкой.</p>
 */
public class SmokehouseBlockEntity extends BlockEntity {
    public static final int SCHEMA_VERSION = 1;

    /** Слотов под продукт: меньше, чем у рамы, — копчение «дороже». */
    public static final int SLOT_COUNT = 2;

    /** Одно бревно горит 1200 тиков (минута реального времени). */
    public static final int FUEL_TICKS_LOG = 1200;

    /** Доска — четверть бревна, ровно как в крафте 1:4. */
    public static final int FUEL_TICKS_PLANK = 300;

    /** Потолок запаса: примерно 8 брёвен, чтобы нельзя было «зарядить навсегда». */
    public static final int MAX_FUEL_TICKS = FUEL_TICKS_LOG * 8;

    private NonNullList<ItemStack> items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
    private final int[] progress = new int[SLOT_COUNT];

    /** Остаток горения в тиках. Ноль — копчение стоит. */
    private int fuelTicks;

    private final RecipeManager.CachedCheck<SingleRecipeInput, SmokingProcessRecipe> quickCheck =
            RecipeManager.createCheck(KHRecipes.SMOKING_TYPE.get());

    public SmokehouseBlockEntity(BlockPos pos, BlockState state) {
        super(KHBlockEntities.SMOKEHOUSE.get(), pos, state);
    }

    // --- Топливо ---

    /**
     * Сколько тиков горения даёт предмет.
     *
     * @return 0, если предмет не дрова
     */
    public static int fuelValue(ItemStack stack) {
        if (stack.is(ItemTags.LOGS_THAT_BURN)) {
            return FUEL_TICKS_LOG;
        }
        if (stack.is(ItemTags.PLANKS)) {
            return FUEL_TICKS_PLANK;
        }
        return 0;
    }

    /** Дрова ли это (для подсказок и тестов). */
    public static boolean isFuel(ItemStack stack) {
        return fuelValue(stack) > 0;
    }

    /** Остаток горения в тиках. */
    public int fuelTicks() {
        return this.fuelTicks;
    }

    /** Идёт ли сейчас копчение: есть дрова и есть что коптить. */
    public boolean isLit() {
        return this.fuelTicks > 0 && !this.isEmpty();
    }

    /**
     * Подбрасывает одну единицу дров.
     *
     * <p>Отказ при переполнении осознан: иначе игрок ссыпал бы в коптильню
     * стак брёвен и забыл про снабжение, а вместе с ним — про саму механику.</p>
     *
     * @return {@code true}, если дрова приняты
     */
    public boolean addFuel(ServerLevel level, ItemStack stack) {
        int value = fuelValue(stack);
        if (value <= 0 || this.fuelTicks + value > MAX_FUEL_TICKS) {
            return false;
        }
        this.fuelTicks += value;
        stack.shrink(1);
        this.markDirtyAndSync(level);
        return true;
    }

    // --- Сериализация ---

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        input.getIntOr("SchemaVersion", 1);
        this.items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(input, this.items);
        int[] savedProgress = input.getIntArray("progress").orElse(new int[SLOT_COUNT]);
        System.arraycopy(savedProgress, 0, this.progress, 0,
                Math.min(savedProgress.length, SLOT_COUNT));
        this.fuelTicks = Math.clamp(input.getIntOr("fuelTicks", 0), 0, MAX_FUEL_TICKS);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("SchemaVersion", SCHEMA_VERSION);
        ContainerHelper.saveAllItems(output, this.items);
        output.putIntArray("progress", this.progress.clone());
        output.putInt("fuelTicks", this.fuelTicks);
    }

    // --- Синхронизация содержимого для рендера ---

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return this.saveWithoutMetadata(registries);
    }

    @Override
    public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    // --- Взаимодействие ---

    /**
     * Кладёт продукт в первый свободный слот.
     *
     * @return {@code true}, если продукт принят (есть рецепт копчения)
     */
    public boolean insert(ServerLevel level, ItemStack stack) {
        if (stack.isEmpty()
                || this.quickCheck.getRecipeFor(new SingleRecipeInput(stack), level).isEmpty()) {
            return false;
        }
        for (int i = 0; i < SLOT_COUNT; i++) {
            if (this.items.get(i).isEmpty()) {
                this.items.set(i, stack.split(1));
                this.progress[i] = 0;
                this.markDirtyAndSync(level);
                return true;
            }
        }
        return false;
    }

    /**
     * Забирает последний занятый слот.
     *
     * @return извлечённый предмет или {@link ItemStack#EMPTY}
     */
    public ItemStack removeLast(ServerLevel level) {
        for (int i = SLOT_COUNT - 1; i >= 0; i--) {
            ItemStack stack = this.items.get(i);
            if (!stack.isEmpty()) {
                this.items.set(i, ItemStack.EMPTY);
                this.progress[i] = 0;
                this.markDirtyAndSync(level);
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    /** Дроп содержимого при разрушении. Дрова сгорели — они не возвращаются. */
    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        if (this.level != null) {
            net.minecraft.world.Containers.dropContents(this.level, pos, this.items);
        }
    }

    // --- Логика копчения ---

    /** Серверный тик раз в 20 тиков (бюджет), как у рамы. */
    public static void serverTick(Level level, BlockPos pos, BlockState state,
            SmokehouseBlockEntity smokehouse) {
        if (!(level instanceof ServerLevel serverLevel) || level.getGameTime() % 20 != 0) {
            return;
        }
        smokehouse.advanceSmoking(serverLevel, 20);
    }

    /**
     * Продвигает копчение на {@code step} тиков, сжигая столько же дров.
     *
     * <p>Ключевой инвариант устройства: <b>нет дров — нет прогресса</b>.
     * Проверка стоит до инкремента, поэтому «дотлеть на последнем тике»
     * нельзя, и пустая коптильня не жжёт дрова впустую.</p>
     */
    public void advanceSmoking(ServerLevel serverLevel, int step) {
        // Без дров устройство мертво — в этом всё его отличие от рамы.
        if (this.fuelTicks <= 0) {
            return;
        }
        // Пустая коптильня не тратит топливо: игрок не наказывается за запас.
        if (this.isEmpty()) {
            return;
        }

        int burn = Math.min(step, this.fuelTicks);
        this.fuelTicks -= burn;
        boolean changed = false;

        for (int i = 0; i < SLOT_COUNT; i++) {
            ItemStack stack = this.items.get(i);
            if (stack.isEmpty()) {
                continue;
            }
            RecipeHolder<SmokingProcessRecipe> recipe = this.quickCheck
                    .getRecipeFor(new SingleRecipeInput(stack), serverLevel).orElse(null);
            if (recipe == null) {
                continue;
            }
            this.progress[i] += burn;
            if (this.progress[i] >= recipe.value().smokeTicks()) {
                this.items.set(i, recipe.value().assemble(new SingleRecipeInput(stack)));
                this.progress[i] = 0;
                changed = true;
            }
        }

        this.updateLitState(serverLevel);
        if (changed || burn > 0) {
            this.markDirtyAndSync(serverLevel);
        }
    }

    /** Держит визуальное состояние LIT в согласии с реальной работой. */
    private void updateLitState(ServerLevel level) {
        BlockState state = this.getBlockState();
        if (!state.hasProperty(SmokehouseBlock.LIT)) {
            return;
        }
        boolean lit = this.isLit();
        if (state.getValue(SmokehouseBlock.LIT) != lit) {
            level.setBlock(this.getBlockPos(), state.setValue(SmokehouseBlock.LIT, lit), 3);
        }
    }

    private void markDirtyAndSync(ServerLevel level) {
        this.setChanged();
        this.updateLitState(level);
        level.sendBlockUpdated(this.getBlockPos(), this.getBlockState(), this.getBlockState(), 3);
    }

    public ItemStack item(int slot) {
        return this.items.get(slot);
    }

    public boolean isEmpty() {
        return this.items.stream().allMatch(ItemStack::isEmpty);
    }
}
