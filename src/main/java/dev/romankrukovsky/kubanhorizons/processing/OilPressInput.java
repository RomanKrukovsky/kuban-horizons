package dev.romankrukovsky.kubanhorizons.processing;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;

/**
 * Вход рецепта маслопресса: единственный стек сырья.
 * Бутылка проверяется логикой блока, а не рецептом, чтобы рецепты
 * оставались простыми для сторонних дополнений.
 */
public record OilPressInput(ItemStack item) implements RecipeInput {
    @Override
    public ItemStack getItem(int index) {
        if (index != 0) {
            throw new IllegalArgumentException("No item for index " + index);
        }
        return this.item;
    }

    @Override
    public int size() {
        return 1;
    }
}
