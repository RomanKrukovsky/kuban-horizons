package dev.romankrukovsky.kubanhorizons.datagen;

import dev.romankrukovsky.kubanhorizons.processing.OilPressingRecipe;
import dev.romankrukovsky.kubanhorizons.registry.KHItems;
import dev.romankrukovsky.kubanhorizons.util.KHIds;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.SimpleCookingRecipeBuilder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.CampfireCookingRecipe;
import net.minecraft.world.item.crafting.CookingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.item.crafting.SmokingRecipe;

import java.util.concurrent.CompletableFuture;

/**
 * Генерация рецептов мода.
 */
public final class KHRecipeProvider extends RecipeProvider {
    KHRecipeProvider(HolderLookup.Provider registries, RecipeOutput output) {
        super(registries, output);
    }

    @Override
    protected void buildRecipes() {
        // Шляпка → семена (4 шт.).
        this.shapeless(RecipeCategory.MISC, KHItems.SUNFLOWER_SEEDS.get(), 4)
                .requires(KHItems.SUNFLOWER_HEAD.get())
                .group("kubanhorizons:sunflower_seeds")
                .unlockedBy("has_sunflower_head", this.has(KHItems.SUNFLOWER_HEAD.get()))
                .save(this.output);

        // Маслопресс: доски + прочное дерево + железо (винт).
        this.shaped(RecipeCategory.DECORATIONS, KHItems.OIL_PRESS.get())
                .pattern("LIL")
                .pattern("P#P")
                .pattern("PPP")
                .define('L', net.minecraft.tags.ItemTags.LOGS)
                .define('I', Items.IRON_INGOT)
                .define('P', net.minecraft.tags.ItemTags.PLANKS)
                .define('#', Items.PISTON)
                .unlockedBy("has_sunflower_seeds", this.has(KHItems.SUNFLOWER_SEEDS.get()))
                .save(this.output);

        // Жареные семечки — жарка в печи/коптильне/на костре.
        cooking(SmeltingRecipe::new, 200, "smelting");
        cooking(SmokingRecipe::new, 100, "smoking");
        cooking(CampfireCookingRecipe::new, 600, "campfire_cooking");

        // Рецепт маслопресса: 8 семян + бутылка → масло + жмых.
        this.output.accept(
                ResourceKey.create(Registries.RECIPE, KHIds.of("oil_pressing/sunflower_oil")),
                new OilPressingRecipe(
                        new Recipe.CommonInfo(true),
                        "",
                        Ingredient.of(KHItems.SUNFLOWER_SEEDS.get()),
                        8,
                        new ItemStackTemplate(KHItems.SUNFLOWER_OIL.get()),
                        new ItemStackTemplate(KHItems.OIL_CAKE.get()),
                        300),
                null);
    }

    /** Жарка семечек одним из трёх способов приготовления. */
    private <T extends AbstractCookingRecipe> void cooking(
            AbstractCookingRecipe.Factory<T> factory, int cookingTime, String suffix) {
        SimpleCookingRecipeBuilder.generic(
                        Ingredient.of(KHItems.SUNFLOWER_SEEDS.get()),
                        RecipeCategory.FOOD, CookingBookCategory.FOOD,
                        KHItems.ROASTED_SUNFLOWER_SEEDS.get(),
                        0.15F, cookingTime, factory)
                .unlockedBy("has_sunflower_seeds", this.has(KHItems.SUNFLOWER_SEEDS.get()))
                .save(this.output, ResourceKey.create(Registries.RECIPE,
                        KHIds.of("roasted_sunflower_seeds_from_" + suffix)));
    }

    public static final class Runner extends RecipeProvider.Runner {
        public Runner(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
            super(output, registries);
        }

        @Override
        protected RecipeProvider createRecipeProvider(HolderLookup.Provider registries, RecipeOutput output) {
            return new KHRecipeProvider(registries, output);
        }

        @Override
        public String getName() {
            return "Kuban Horizons Recipes";
        }
    }
}
