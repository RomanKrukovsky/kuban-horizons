package dev.romankrukovsky.kubanhorizons.blockentity;

import dev.romankrukovsky.kubanhorizons.processing.DryingRecipe;
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
 * Сушильная рама: 4 слота, каждый сушит независимо.
 *
 * <p>Без GUI: игрок кладёт предмет ПКМ, забирает ПКМ пустой рукой.
 * Прогресс идёт только под открытым небом днём либо под крышей медленнее
 * вдвое; дождь останавливает сушку. Никакого меню — «крестьянское»
 * устройство с world-interaction, содержимое рендерится на раме
 * (синхронизация через update tag).</p>
 */
public class DryingRackBlockEntity extends BlockEntity {
    public static final int SCHEMA_VERSION = 1;
    public static final int SLOT_COUNT = 4;

    private NonNullList<ItemStack> items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
    private final int[] progress = new int[SLOT_COUNT];

    private final RecipeManager.CachedCheck<SingleRecipeInput, DryingRecipe> quickCheck =
            RecipeManager.createCheck(KHRecipes.DRYING_TYPE.get());

    public DryingRackBlockEntity(BlockPos pos, BlockState state) {
        super(KHBlockEntities.DRYING_RACK.get(), pos, state);
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
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("SchemaVersion", SCHEMA_VERSION);
        ContainerHelper.saveAllItems(output, this.items);
        output.putIntArray("progress", this.progress.clone());
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
     * Кладёт предмет в первый свободный слот.
     *
     * @return {@code true}, если предмет принят (есть рецепт сушки)
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

    /** Дроп содержимого при разрушении. */
    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        if (this.level != null) {
            net.minecraft.world.Containers.dropContents(this.level, pos, this.items);
        }
    }

    // --- Логика сушки ---

    /** Серверный тик раз в 20 тиков (бюджет): бюджетная сушка. */
    public static void serverTick(Level level, BlockPos pos, BlockState state, DryingRackBlockEntity rack) {
        if (!(level instanceof ServerLevel serverLevel) || level.getGameTime() % 20 != 0) {
            return;
        }
        // Дождь над рамой останавливает сушку.
        if (serverLevel.isRainingAt(pos.above())) {
            return;
        }
        boolean daylight = serverLevel.isBrightOutside();
        boolean openSky = serverLevel.canSeeSky(pos.above());
        // Днём под небом — полная скорость; иначе — половина.
        rack.advanceDrying(serverLevel, daylight && openSky ? 20 : 10);
    }

    /** Продвигает сушку всех слотов на {@code step} тиков прогресса. */
    public void advanceDrying(ServerLevel serverLevel, int step) {
        boolean changed = false;
        for (int i = 0; i < SLOT_COUNT; i++) {
            ItemStack stack = this.items.get(i);
            if (stack.isEmpty()) {
                continue;
            }
            RecipeHolder<DryingRecipe> recipe = this.quickCheck
                    .getRecipeFor(new SingleRecipeInput(stack), serverLevel).orElse(null);
            if (recipe == null) {
                continue;
            }
            this.progress[i] += step;
            if (this.progress[i] >= recipe.value().dryTicks()) {
                this.items.set(i, recipe.value().assemble(new SingleRecipeInput(stack)));
                this.progress[i] = 0;
                changed = true;
            }
        }
        if (changed) {
            this.markDirtyAndSync(serverLevel);
        }
    }

    private void markDirtyAndSync(ServerLevel level) {
        this.setChanged();
        level.sendBlockUpdated(this.getBlockPos(), this.getBlockState(), this.getBlockState(), 3);
    }

    public ItemStack item(int slot) {
        return this.items.get(slot);
    }

    public boolean isEmpty() {
        return this.items.stream().allMatch(ItemStack::isEmpty);
    }
}
