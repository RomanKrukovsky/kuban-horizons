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

        // Оросительный желоб: доски корытом (4 шт.).
        this.shaped(RecipeCategory.DECORATIONS, KHItems.IRRIGATION_CHANNEL.get(), 4)
                .pattern("P P")
                .pattern("PPP")
                .define('P', net.minecraft.tags.ItemTags.PLANKS)
                .unlockedBy("has_planks", this.has(net.minecraft.tags.ItemTags.PLANKS))
                .save(this.output);

        // Водозабор: камень + железная решётка (ведро как символ забора воды).
        this.shaped(RecipeCategory.DECORATIONS, KHItems.WATER_INTAKE.get())
                .pattern("I I")
                .pattern("SBS")
                .pattern("SSS")
                .define('I', Items.IRON_BARS)
                .define('S', Items.STONE)
                .define('B', Items.BUCKET)
                .unlockedBy("has_bucket", this.has(Items.BUCKET))
                .save(this.output);

        // Почвенный щуп: медный стержень.
        this.shaped(RecipeCategory.TOOLS, KHItems.SOIL_PROBE.get())
                .pattern("C")
                .pattern("R")
                .pattern("R")
                .define('C', Items.COPPER_INGOT)
                .define('R', Items.STICK)
                .unlockedBy("has_copper", this.has(Items.COPPER_INGOT))
                .save(this.output);

        // Початок → зёрна (2 шт.).
        this.shapeless(RecipeCategory.MISC, KHItems.CORN_KERNELS.get(), 2)
                .requires(KHItems.CORN_COB.get())
                .group("kubanhorizons:corn_kernels")
                .unlockedBy("has_corn_cob", this.has(KHItems.CORN_COB.get()))
                .save(this.output);

        // Томат → семена.
        this.shapeless(RecipeCategory.MISC, KHItems.TOMATO_SEEDS.get(), 1)
                .requires(KHItems.TOMATO.get())
                .unlockedBy("has_tomato", this.has(KHItems.TOMATO.get()))
                .save(this.output);

        // Шпалера: палки + верёвка-стиль (2 шт.).
        this.shaped(RecipeCategory.DECORATIONS, KHItems.GRAPE_TRELLIS.get(), 2)
                .pattern("S S")
                .pattern("SSS")
                .pattern("S S")
                .define('S', Items.STICK)
                .unlockedBy("has_sticks", this.has(Items.STICK))
                .save(this.output);

        // Черенок из винограда (размножение лозы).
        this.shapeless(RecipeCategory.MISC, KHItems.GRAPE_CUTTING.get(), 1)
                .requires(KHItems.GRAPES.get())
                .requires(Items.STICK)
                .unlockedBy("has_grapes", this.has(KHItems.GRAPES.get()))
                .save(this.output);

        // Метёлка → рис (крупа).
        this.shapeless(RecipeCategory.MISC, KHItems.RICE.get(), 1)
                .requires(KHItems.RICE_PANICLE.get())
                .group("kubanhorizons:rice")
                .unlockedBy("has_rice_panicle", this.has(KHItems.RICE_PANICLE.get()))
                .save(this.output);

        // Отварной рис: рис + миска в жарке недоступен; варка = крафт с миской.
        this.shapeless(RecipeCategory.FOOD, KHItems.COOKED_RICE.get(), 1)
                .requires(KHItems.RICE.get())
                .requires(KHItems.RICE.get())
                .requires(Items.BOWL)
                .unlockedBy("has_rice", this.has(KHItems.RICE.get()))
                .save(this.output);

        // Жареные семечки — жарка в печи/коптильне/на костре.
        cooking(SmeltingRecipe::new, 200, "smelting");
        cooking(SmokingRecipe::new, 100, "smoking");
        cooking(CampfireCookingRecipe::new, 600, "campfire_cooking");

        // Печёная кукуруза.
        cookedCorn(SmeltingRecipe::new, 200, "smelting");
        cookedCorn(SmokingRecipe::new, 100, "smoking");
        cookedCorn(CampfireCookingRecipe::new, 600, "campfire_cooking");

        // Сушильная рама: палки + верёвка.
        this.shaped(RecipeCategory.DECORATIONS, KHItems.DRYING_RACK.get())
                .pattern("SSS")
                .pattern("T T")
                .define('S', Items.STICK)
                .define('T', Items.STRING)
                .unlockedBy("has_string", this.has(Items.STRING))
                .save(this.output);

        // Ручная мельница: камень + палка.
        this.shaped(RecipeCategory.DECORATIONS, KHItems.HAND_MILL.get())
                .pattern(" T ")
                .pattern("SSS")
                .pattern("SSS")
                .define('T', Items.STICK)
                .define('S', Items.STONE)
                .unlockedBy("has_stone", this.has(Items.STONE))
                .save(this.output);

        // Рецепты помола.
        milling("flour_from_wheat", Items.WHEAT, KHItems.FLOUR.get(), 3);
        milling("cornmeal_from_corn", KHItems.CORN_KERNELS.get(), KHItems.CORNMEAL.get(), 3);
        milling("rice_from_panicle", KHItems.RICE_PANICLE.get(), KHItems.RICE.get(), 2);

        // Рецепты сушки.
        drying("dried_tea", KHItems.TEA_LEAVES.get(), KHItems.DRIED_TEA.get(), 1200);
        drying("dried_fruit_from_peach", KHItems.PEACH.get(), KHItems.DRIED_FRUIT.get(), 2400);
        drying("dried_fruit_from_apricot", KHItems.APRICOT.get(), KHItems.DRIED_FRUIT.get(), 2400);
        drying("dried_fruit_from_plum", KHItems.PLUM.get(), KHItems.DRIED_FRUIT.get(), 2400);
        drying("dried_fruit_from_grapes", KHItems.GRAPES.get(), KHItems.DRIED_FRUIT.get(), 2400);

        // --- Кухня ---

        // Домашний хлеб: 3 муки (в печи).
        cookFood(KHItems.FLOUR.get(), KHItems.HOMEMADE_BREAD.get(), "homemade_bread", 200);

        // Кубанский борщ: свёкла + капуста(морковь) + томат + мясо + миска.
        this.shapeless(RecipeCategory.FOOD, KHItems.BORSCHT.get())
                .requires(Items.BEETROOT)
                .requires(Items.CARROT)
                .requires(KHItems.TOMATO.get())
                .requires(Items.COOKED_BEEF)
                .requires(KHItems.SUNFLOWER_OIL.get())
                .requires(Items.BOWL)
                .unlockedBy("has_tomato", this.has(KHItems.TOMATO.get()))
                .save(this.output);

        // Мамалыга: 2 крупы + миска.
        this.shapeless(RecipeCategory.FOOD, KHItems.MAMALYGA.get())
                .requires(KHItems.CORNMEAL.get())
                .requires(KHItems.CORNMEAL.get())
                .requires(Items.BOWL)
                .unlockedBy("has_cornmeal", this.has(KHItems.CORNMEAL.get()))
                .save(this.output);

        // Чашка чая: сушёный чай + бутылка воды.
        this.shapeless(RecipeCategory.FOOD, KHItems.TEA_CUP.get())
                .requires(KHItems.DRIED_TEA.get())
                .requires(Items.POTION) // бутылка воды — предмет POTION по умолчанию
                .unlockedBy("has_dried_tea", this.has(KHItems.DRIED_TEA.get()))
                .save(this.output);

        // Мёд с орехами: мёд + 2 ореха + миска.
        this.shapeless(RecipeCategory.FOOD, KHItems.HONEY_WALNUTS.get())
                .requires(Items.HONEY_BOTTLE)
                .requires(KHItems.WALNUT.get())
                .requires(KHItems.WALNUT.get())
                .requires(Items.BOWL)
                .unlockedBy("has_walnut", this.has(KHItems.WALNUT.get()))
                .save(this.output);

        // Овощная закуска: томат + арбуз? нет — томат + морковь + масло + миска.
        this.shapeless(RecipeCategory.FOOD, KHItems.VEGETABLE_SPREAD.get())
                .requires(KHItems.TOMATO.get())
                .requires(KHItems.TOMATO.get())
                .requires(Items.CARROT)
                .requires(KHItems.SUNFLOWER_OIL.get())
                .requires(Items.BOWL)
                .unlockedBy("has_oil", this.has(KHItems.SUNFLOWER_OIL.get()))
                .save(this.output);

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

    /** Выпечка еды в печи. */
    private void cookFood(net.minecraft.world.level.ItemLike input,
            net.minecraft.world.level.ItemLike result, String name, int time) {
        SimpleCookingRecipeBuilder.generic(
                        Ingredient.of(input),
                        RecipeCategory.FOOD, CookingBookCategory.FOOD,
                        result.asItem(), 0.3F, time, SmeltingRecipe::new)
                .unlockedBy("has_input", this.has(input))
                .save(this.output, ResourceKey.create(Registries.RECIPE, KHIds.of(name)));
    }

    /** Рецепт помола на мельнице. */
    private void milling(String name, net.minecraft.world.level.ItemLike input,
            net.minecraft.world.level.ItemLike result, int turns) {
        this.output.accept(
                ResourceKey.create(Registries.RECIPE, KHIds.of("milling/" + name)),
                new dev.romankrukovsky.kubanhorizons.processing.MillingRecipe(
                        new Recipe.CommonInfo(true), "",
                        Ingredient.of(input),
                        new ItemStackTemplate(result.asItem()),
                        turns),
                null);
    }

    /** Рецепт сушки на раме. */
    private void drying(String name, net.minecraft.world.level.ItemLike input,
            net.minecraft.world.level.ItemLike result, int ticks) {
        this.output.accept(
                ResourceKey.create(Registries.RECIPE, KHIds.of("drying/" + name)),
                new dev.romankrukovsky.kubanhorizons.processing.DryingRecipe(
                        new Recipe.CommonInfo(true), "",
                        Ingredient.of(input),
                        new ItemStackTemplate(result.asItem()),
                        ticks),
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

    /** Печёная кукуруза одним из трёх способов приготовления. */
    private <T extends AbstractCookingRecipe> void cookedCorn(
            AbstractCookingRecipe.Factory<T> factory, int cookingTime, String suffix) {
        SimpleCookingRecipeBuilder.generic(
                        Ingredient.of(KHItems.CORN_COB.get()),
                        RecipeCategory.FOOD, CookingBookCategory.FOOD,
                        KHItems.GRILLED_CORN.get(),
                        0.2F, cookingTime, factory)
                .unlockedBy("has_corn_cob", this.has(KHItems.CORN_COB.get()))
                .save(this.output, ResourceKey.create(Registries.RECIPE,
                        KHIds.of("grilled_corn_from_" + suffix)));
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
