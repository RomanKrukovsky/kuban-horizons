package dev.romankrukovsky.kubanhorizons.blockentity;

import dev.romankrukovsky.kubanhorizons.processing.MillingRecipe;
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
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * Ручная мельница: один слот сырья, помол оборотами (ПКМ).
 *
 * <p>Без GUI: игрок кладёт зерно ПКМ с предметом, крутит жёрнов ПКМ
 * пустой рукой; после нужного числа оборотов продукт высыпается в мир.
 * Никакого пассивного режима — только ручной труд (механическая
 * мельница появится отдельно).</p>
 */
public class HandMillBlockEntity extends BlockEntity {
    public static final int SCHEMA_VERSION = 1;

    private NonNullList<ItemStack> items = NonNullList.withSize(1, ItemStack.EMPTY);
    private int turns;

    private final RecipeManager.CachedCheck<SingleRecipeInput, MillingRecipe> quickCheck =
            RecipeManager.createCheck(KHRecipes.MILLING_TYPE.get());

    public HandMillBlockEntity(BlockPos pos, BlockState state) {
        super(KHBlockEntities.HAND_MILL.get(), pos, state);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        input.getIntOr("SchemaVersion", 1);
        this.items = NonNullList.withSize(1, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(input, this.items);
        this.turns = input.getIntOr("turns", 0);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("SchemaVersion", SCHEMA_VERSION);
        ContainerHelper.saveAllItems(output, this.items);
        output.putInt("turns", this.turns);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return this.saveWithoutMetadata(registries);
    }

    @Override
    public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    /** Кладёт одно сырьё, если есть рецепт помола. */
    public boolean insert(ServerLevel level, ItemStack stack) {
        if (!this.items.getFirst().isEmpty() || stack.isEmpty()
                || this.quickCheck.getRecipeFor(new SingleRecipeInput(stack), level).isEmpty()) {
            return false;
        }
        this.items.set(0, stack.split(1));
        this.turns = 0;
        this.markDirtyAndSync(level);
        return true;
    }

    /**
     * Один оборот жёрнова.
     *
     * @return {@code true}, если оборот выполнен (в мельнице есть сырьё)
     */
    public boolean turn(ServerLevel level) {
        ItemStack stack = this.items.getFirst();
        if (stack.isEmpty()) {
            return false;
        }
        RecipeHolder<MillingRecipe> recipe = this.quickCheck
                .getRecipeFor(new SingleRecipeInput(stack), level).orElse(null);
        if (recipe == null) {
            return false;
        }
        this.turns++;
        if (this.turns >= recipe.value().turns()) {
            // Готово: продукт наружу, слот очищается.
            ItemStack result = recipe.value().assemble(new SingleRecipeInput(stack));
            this.items.set(0, ItemStack.EMPTY);
            this.turns = 0;
            net.minecraft.world.level.block.Block.popResource(level,
                    this.getBlockPos().above(), result);
        }
        this.markDirtyAndSync(level);
        return true;
    }

    /** Возврат сырья (ПКМ shift). */
    public ItemStack removeInput(ServerLevel level) {
        ItemStack stack = this.items.getFirst();
        if (!stack.isEmpty()) {
            this.items.set(0, ItemStack.EMPTY);
            this.turns = 0;
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

    public boolean hasInput() {
        return !this.items.getFirst().isEmpty();
    }
}
