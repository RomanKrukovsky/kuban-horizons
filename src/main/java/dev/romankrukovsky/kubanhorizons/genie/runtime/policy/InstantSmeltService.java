package dev.romankrukovsky.kubanhorizons.genie.runtime.policy;

import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BlastFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.FurnaceBlockEntity;
import net.minecraft.world.level.block.entity.SmokerBlockEntity;
import net.minecraft.world.level.block.entity.TickingBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Подготавливает загруженные печи так, чтобы следующий обычный тик завершил рецепт. */
public final class InstantSmeltService {
    private InstantSmeltService() {
    }

    public static void prepareLoadedFurnaces(ServerLevel level) {
        // Копия защищает обход от добавления/удаления block entity в том же тике.
        for (TickingBlockEntity ticker : List.copyOf(level.blockEntityTickers)) {
            BlockEntity blockEntity = level.getBlockEntity(ticker.getPos());
            if (blockEntity instanceof AbstractFurnaceBlockEntity furnace) {
                prepareFurnace(level, furnace);
            }
        }
    }

    private static void prepareFurnace(ServerLevel level, AbstractFurnaceBlockEntity furnace) {
        ItemStack ingredient = furnace.getItem(0);
        if (ingredient.isEmpty()) {
            return;
        }

        RecipeType<? extends AbstractCookingRecipe> recipeType = recipeType(furnace);
        if (recipeType == null) {
            return;
        }
        SingleRecipeInput input = new SingleRecipeInput(ingredient);
        RecipeHolder<? extends AbstractCookingRecipe> recipe = findRecipe(level, recipeType, input);
        if (recipe == null) {
            return;
        }
        ItemStack result = recipe.value().assemble(input);
        if (result.isEmpty() || !canAcceptResult(furnace, result)) {
            return;
        }

        BlockState state = furnace.getBlockState();
        boolean lit = state.hasProperty(AbstractFurnaceBlock.LIT)
                && state.getValue(AbstractFurnaceBlock.LIT);
        ItemStack fuel = furnace.getItem(1);
        if (!lit && (fuel.isEmpty() || fuel.getBurnTime(recipeType, level.fuelValues()) <= 0)) {
            return;
        }

        // Vanilla сама выполняет рецепт, расходует топливо, записывает опыт и
        // обновляет блок. Мы меняем только длительность ближайшего цикла.
        furnace.cookingTimer = 0;
        furnace.cookingTotalTime = 1;
    }

    private static boolean canAcceptResult(AbstractFurnaceBlockEntity furnace, ItemStack result) {
        ItemStack existing = furnace.getItem(2);
        if (existing.isEmpty()) {
            return true;
        }
        if (!ItemStack.isSameItemSameComponents(existing, result)) {
            return false;
        }
        int maximum = Math.min(furnace.getMaxStackSize(), result.getMaxStackSize());
        return existing.getCount() + result.getCount() <= maximum;
    }

    private static RecipeHolder<? extends AbstractCookingRecipe> findRecipe(
            ServerLevel level, RecipeType<? extends AbstractCookingRecipe> type,
            SingleRecipeInput input) {
        if (type == RecipeType.SMELTING) {
            return level.getServer().getRecipeManager()
                    .getRecipeFor(RecipeType.SMELTING, input, level).orElse(null);
        }
        if (type == RecipeType.BLASTING) {
            return level.getServer().getRecipeManager()
                    .getRecipeFor(RecipeType.BLASTING, input, level).orElse(null);
        }
        if (type == RecipeType.SMOKING) {
            return level.getServer().getRecipeManager()
                    .getRecipeFor(RecipeType.SMOKING, input, level).orElse(null);
        }
        return null;
    }

    private static RecipeType<? extends AbstractCookingRecipe> recipeType(
            AbstractFurnaceBlockEntity furnace) {
        if (furnace instanceof FurnaceBlockEntity) {
            return RecipeType.SMELTING;
        }
        if (furnace instanceof BlastFurnaceBlockEntity) {
            return RecipeType.BLASTING;
        }
        if (furnace instanceof SmokerBlockEntity) {
            return RecipeType.SMOKING;
        }
        return null;
    }
}
