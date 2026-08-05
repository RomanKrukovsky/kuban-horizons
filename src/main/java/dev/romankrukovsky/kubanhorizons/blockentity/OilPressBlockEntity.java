package dev.romankrukovsky.kubanhorizons.blockentity;

import dev.romankrukovsky.kubanhorizons.menu.OilPressMenu;
import dev.romankrukovsky.kubanhorizons.config.KHServerConfig;
import dev.romankrukovsky.kubanhorizons.processing.OilPressInput;
import dev.romankrukovsky.kubanhorizons.processing.OilPressingRecipe;
import dev.romankrukovsky.kubanhorizons.registry.KHBlockEntities;
import dev.romankrukovsky.kubanhorizons.registry.KHRecipes;
import dev.romankrukovsky.kubanhorizons.registry.KHSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Логика маслопресса.
 *
 * <p>Слоты: 0 — сырьё (семена), 1 — стеклянные бутылки, 2 — результат
 * (масло), 3 — побочный продукт (жмых).</p>
 *
 * <p>Два режима работы:</p>
 * <ul>
 *   <li><b>Ручной</b>: игрок прокручивает винт (ПКМ по блоку) — каждый такт
 *       добавляет {@code MANUAL_WORK_PER_TURN} прогресса;</li>
 *   <li><b>Пассивный</b> (если включён в конфиге): прогресс медленно растёт
 *       сам по одному тику за игровой тик.</li>
 * </ul>
 *
 * <p>Защита от дюпа: результат создаётся единственным способом — в
 * {@link #finishPressing} на серверной стороне, с одновременным списанием
 * сырья и бутылки; сама операция начинается только если выходные слоты
 * гарантированно вмещают результат.</p>
 *
 * <p>Формат сохранения версионируется полем {@code SchemaVersion} (AD-006).</p>
 */
public class OilPressBlockEntity extends BaseContainerBlockEntity {
    /** Версия схемы сохраняемых данных. */
    public static final int SCHEMA_VERSION = 1;

    public static final int SLOT_INPUT = 0;
    public static final int SLOT_BOTTLE = 1;
    public static final int SLOT_RESULT = 2;
    public static final int SLOT_BYPRODUCT = 3;
    public static final int SLOT_COUNT = 4;

    /** Прогресс за один ручной оборот винта. */
    public static final int MANUAL_WORK_PER_TURN = 60;

    public static final int DATA_PROGRESS = 0;
    public static final int DATA_TOTAL = 1;
    public static final int DATA_COUNT = 2;

    private NonNullList<ItemStack> items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
    private int progress;
    private int totalWork;

    private final RecipeManager.CachedCheck<OilPressInput, OilPressingRecipe> quickCheck =
            RecipeManager.createCheck(KHRecipes.OIL_PRESSING_TYPE.get());

    private final ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int dataId) {
            return switch (dataId) {
                case DATA_PROGRESS -> OilPressBlockEntity.this.progress;
                case DATA_TOTAL -> OilPressBlockEntity.this.totalWork;
                default -> 0;
            };
        }

        @Override
        public void set(int dataId, int value) {
            switch (dataId) {
                case DATA_PROGRESS -> OilPressBlockEntity.this.progress = value;
                case DATA_TOTAL -> OilPressBlockEntity.this.totalWork = value;
                default -> {
                }
            }
        }

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    public OilPressBlockEntity(BlockPos pos, BlockState state) {
        super(KHBlockEntities.OIL_PRESS.get(), pos, state);
    }

    // --- Сериализация ---

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        // Схема v1; при изменении формата добавить миграцию по значению версии.
        int schema = input.getIntOr("SchemaVersion", 1);
        if (schema > SCHEMA_VERSION) {
            // Данные из более новой версии мода: читаем что можем, не удаляя.
            dev.romankrukovsky.kubanhorizons.KubanHorizons.LOGGER.warn(
                    "Маслопресс на {}: схема данных v{} новее поддерживаемой v{}.",
                    this.getBlockPos(), schema, SCHEMA_VERSION);
        }
        this.items = NonNullList.withSize(this.getContainerSize(), ItemStack.EMPTY);
        ContainerHelper.loadAllItems(input, this.items);
        this.progress = input.getIntOr("progress", 0);
        this.totalWork = input.getIntOr("total_work", 0);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("SchemaVersion", SCHEMA_VERSION);
        ContainerHelper.saveAllItems(output, this.items);
        output.putInt("progress", this.progress);
        output.putInt("total_work", this.totalWork);
    }

    // --- Container ---

    @Override
    public int getContainerSize() {
        return SLOT_COUNT;
    }

    @Override
    protected NonNullList<ItemStack> getItems() {
        return this.items;
    }

    @Override
    protected void setItems(NonNullList<ItemStack> items) {
        this.items = items;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return switch (slot) {
            case SLOT_INPUT -> true;
            case SLOT_BOTTLE -> stack.is(Items.GLASS_BOTTLE);
            // Выходные слоты закрыты для ручной и автоматической вставки.
            default -> false;
        };
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.kubanhorizons.oil_press");
    }

    @Override
    protected AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
        return new OilPressMenu(containerId, inventory, this, this.dataAccess);
    }

    // --- Логика работы ---

    /**
     * Один ручной оборот винта. Вызывается блоком при взаимодействии.
     *
     * @return {@code true}, если оборот принят (есть валидный рецепт).
     */
    public boolean turnScrew(ServerLevel level) {
        RecipeHolder<OilPressingRecipe> recipe = this.findRecipe(level);
        if (recipe == null) {
            return false;
        }
        this.advanceWork(level, recipe, MANUAL_WORK_PER_TURN);
        return true;
    }

    /** Серверный тик: пассивный режим работы. */
    public static void serverTick(Level level, BlockPos pos, BlockState state, OilPressBlockEntity press) {
        if (!(level instanceof ServerLevel serverLevel) || !KHServerConfig.oilPressAuto()) {
            return;
        }
        RecipeHolder<OilPressingRecipe> recipe = press.findRecipe(serverLevel);
        if (recipe == null) {
            if (press.progress != 0) {
                press.progress = 0;
                press.totalWork = 0;
                press.setChanged();
            }
            return;
        }
        press.advanceWork(serverLevel, recipe, 1);
        // Редкий рабочий звук, чтобы не спамить (раз в ~4 секунды).
        if (press.progress % 80 == 1) {
            serverLevel.playSound(null, pos, KHSounds.OIL_PRESS_WORK.get(), SoundSource.BLOCKS, 0.4F, 1.0F);
        }
    }

    /**
     * Ищет рецепт по текущему сырью и проверяет выполнимость операции
     * (наличие бутылки и место в выходных слотах).
     */
    private RecipeHolder<OilPressingRecipe> findRecipe(ServerLevel level) {
        ItemStack input = this.items.get(SLOT_INPUT);
        if (input.isEmpty()) {
            return null;
        }
        RecipeHolder<OilPressingRecipe> holder =
                this.quickCheck.getRecipeFor(new OilPressInput(input), level).orElse(null);
        if (holder == null) {
            return null;
        }
        OilPressingRecipe recipe = holder.value();
        if (input.getCount() < recipe.inputCount()) {
            return null;
        }
        if (this.items.get(SLOT_BOTTLE).isEmpty()) {
            return null;
        }
        return canFit(recipe.assemble(new OilPressInput(input)), this.items.get(SLOT_RESULT))
                && canFit(recipe.createByproduct(), this.items.get(SLOT_BYPRODUCT))
                ? holder
                : null;
    }

    private static boolean canFit(ItemStack result, ItemStack slot) {
        if (result.isEmpty() || slot.isEmpty()) {
            return true;
        }
        return ItemStack.isSameItemSameComponents(slot, result)
                && slot.getCount() + result.getCount() <= slot.getMaxStackSize();
    }

    private void advanceWork(ServerLevel level, RecipeHolder<OilPressingRecipe> recipe, int amount) {
        int required = (int) Math.max(20,
                recipe.value().workTicks() * (KHServerConfig.oilPressWorkTicks() / 300.0));
        if (this.totalWork != required) {
            this.totalWork = required;
            this.progress = Math.min(this.progress, required);
        }
        this.progress += amount;
        if (this.progress >= required) {
            this.finishPressing(level, recipe.value());
            this.progress = 0;
            this.totalWork = 0;
        }
        this.setChanged();
    }

    /**
     * Завершает отжим: атомарно списывает сырьё и бутылку, кладёт результат
     * и побочный продукт. Единственная точка создания предметов.
     */
    private void finishPressing(ServerLevel level, OilPressingRecipe recipe) {
        ItemStack input = this.items.get(SLOT_INPUT);
        ItemStack bottle = this.items.get(SLOT_BOTTLE);
        // Повторная валидация непосредственно перед списанием: между
        // проверкой и завершением инвентарь мог измениться хоппером.
        if (input.getCount() < recipe.inputCount() || bottle.isEmpty()
                || !recipe.input().test(input)) {
            return;
        }
        ItemStack result = recipe.assemble(new OilPressInput(input));
        ItemStack byproduct = recipe.createByproduct();
        if (!canFit(result, this.items.get(SLOT_RESULT))
                || !canFit(byproduct, this.items.get(SLOT_BYPRODUCT))) {
            return;
        }

        input.shrink(recipe.inputCount());
        bottle.shrink(1);
        insert(this.items, SLOT_RESULT, result);
        insert(this.items, SLOT_BYPRODUCT, byproduct);

        level.playSound(null, this.getBlockPos(), KHSounds.OIL_PRESS_FINISH.get(),
                SoundSource.BLOCKS, 0.7F, 1.0F);
    }

    private static void insert(NonNullList<ItemStack> items, int slot, ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }
        ItemStack existing = items.get(slot);
        if (existing.isEmpty()) {
            items.set(slot, stack.copy());
        } else {
            existing.grow(stack.getCount());
        }
    }

    /** Прогресс для GUI (0..1). */
    public ContainerData dataAccess() {
        return this.dataAccess;
    }

    public boolean hasWork() {
        return this.progress > 0;
    }
}
