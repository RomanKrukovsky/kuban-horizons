package dev.romankrukovsky.kubanhorizons.blockentity;

import dev.romankrukovsky.kubanhorizons.processing.CuttingRecipe;
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
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * Разделочный стол: один слот, нарезка инструментом в руке.
 *
 * <p>Без GUI и без пассивного режима: игрок кладёт продукт ПКМ, затем режет
 * его ПКМ с ножом или другим подходящим инструментом. Один удар — одна
 * нарезка, результат выпадает в мир.</p>
 *
 * <p>Анти-дюп: единственный слот, вставка возможна только когда он пуст, а
 * выдача результата и очистка слота происходят в одной серверной операции —
 * промежуточного состояния «продукт и в слоте, и на земле» не существует.</p>
 */
public class CuttingBoardBlockEntity extends BlockEntity {
    public static final int SCHEMA_VERSION = 1;

    private NonNullList<ItemStack> items = NonNullList.withSize(1, ItemStack.EMPTY);

    private final RecipeManager.CachedCheck<SingleRecipeInput, CuttingRecipe> quickCheck =
            RecipeManager.createCheck(KHRecipes.CUTTING_TYPE.get());

    public CuttingBoardBlockEntity(BlockPos pos, BlockState state) {
        super(KHBlockEntities.CUTTING_BOARD.get(), pos, state);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        input.getIntOr("SchemaVersion", 1);
        this.items = NonNullList.withSize(1, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(input, this.items);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("SchemaVersion", SCHEMA_VERSION);
        ContainerHelper.saveAllItems(output, this.items);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return this.saveWithoutMetadata(registries);
    }

    @Override
    public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    /** Что лежит на столе — нужно рендеру и для возврата предмета. */
    public ItemStack getHeldItem() {
        return this.items.getFirst();
    }

    /** Положить продукт на стол, если для него есть рецепт нарезки. */
    public boolean place(ServerLevel level, ItemStack stack) {
        if (!this.items.getFirst().isEmpty() || stack.isEmpty()
                || this.quickCheck.getRecipeFor(new SingleRecipeInput(stack), level).isEmpty()) {
            return false;
        }
        this.items.set(0, stack.split(1));
        this.markDirtyAndSync(level);
        return true;
    }

    /**
     * Нарезать лежащий продукт инструментом {@code tool}.
     *
     * @return {@code true}, если нарезка состоялась
     */
    public boolean cut(ServerLevel level, ItemStack tool) {
        ItemStack stack = this.items.getFirst();
        if (stack.isEmpty()) {
            return false;
        }
        RecipeHolder<CuttingRecipe> recipe = this.quickCheck
                .getRecipeFor(new SingleRecipeInput(stack), level).orElse(null);
        if (recipe == null || !recipe.value().toolMatches(tool)) {
            return false;
        }
        // Слот очищается до выдачи результатов: даже если popResource бросит,
        // продукт не останется на столе продублированным.
        this.items.set(0, ItemStack.EMPTY);
        this.markDirtyAndSync(level);
        for (ItemStackTemplate template : recipe.value().results()) {
            Block.popResource(level, this.getBlockPos().above(), template.create());
        }
        return true;
    }

    /** Забрать продукт со стола (ПКМ пустой рукой). */
    public ItemStack take(ServerLevel level) {
        ItemStack stack = this.items.getFirst();
        if (!stack.isEmpty()) {
            this.items.set(0, ItemStack.EMPTY);
            this.markDirtyAndSync(level);
        }
        return stack;
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        if (this.level != null) {
            net.minecraft.world.Containers.dropContents(this.level, pos, this.items);
        }
    }

    private void markDirtyAndSync(ServerLevel level) {
        this.setChanged();
        level.sendBlockUpdated(this.getBlockPos(), this.getBlockState(), this.getBlockState(), 3);
    }
}
