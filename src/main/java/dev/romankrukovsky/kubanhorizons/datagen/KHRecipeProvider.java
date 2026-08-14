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
import net.minecraft.world.flag.FeatureFlags;
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

<<<<<<< Updated upstream
=======
        // Магическое зеркало — двухточечный инструмент безопасных снимков области.
        this.shaped(RecipeCategory.TOOLS, KHItems.MAGIC_MIRROR.get())
                .pattern("AGA")
                .pattern("GPG")
                .pattern("AGA")
                .define('A', Items.AMETHYST_SHARD)
                .define('G', Items.GOLD_INGOT)
                .define('P', Items.ENDER_PEARL)
                .unlockedBy("has_amethyst", this.has(Items.AMETHYST_SHARD))
                .save(this.output);

        // Пустая лампа получает связь только при взаимодействии со своей джиннией.
        this.shaped(RecipeCategory.TOOLS, KHItems.GENIE_LAMP.get())
                .pattern(" G ")
                .pattern("GAG")
                .pattern(" E ")
                .define('G', Items.GOLD_INGOT)
                .define('A', Items.AMETHYST_SHARD)
                .define('E', Items.ENDER_PEARL)
                .unlockedBy("has_amethyst", this.has(Items.AMETHYST_SHARD))
                .save(this.output);

>>>>>>> Stashed changes
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

        // Виноградный чан: бочарная клепка — доски по кругу, дно из бревна.
        // Рисунок «кольцо досок вокруг пустоты» читается как открытая ёмкость,
        // а не как станина маслопресса (у того винт сверху и сплошной корпус).
        // Цена ниже маслопресса намеренно: чан не имеет привода, он работает
        // ногами игрока, и требовать за него железо было бы нечестно.
        this.shaped(RecipeCategory.DECORATIONS, KHItems.GRAPE_PRESS.get())
                .pattern("P P")
                .pattern("P P")
                .pattern("PLP")
                .define('P', net.minecraft.tags.ItemTags.PLANKS)
                .define('L', net.minecraft.tags.ItemTags.LOGS)
                .unlockedBy("has_grapes", this.has(KHItems.GRAPES.get()))
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

<<<<<<< Updated upstream
=======
        // Рецепты копчения. Рыба коптится быстрее мяса — тонкая тушка
        // прокапчивается насквозь раньше, чем кабаний окорок.
        smoking("smoked_fish_from_sturgeon",
                KHItems.RAW_STURGEON.get(), KHItems.SMOKED_FISH.get(), 1200);
        smoking("smoked_meat_from_boar",
                KHItems.RAW_BOAR.get(), KHItems.SMOKED_MEAT.get(), 2400);
        smoking("smoked_meat_from_pheasant",
                KHItems.RAW_PHEASANT.get(), KHItems.SMOKED_MEAT.get(), 1800);
        smoking("smoked_meat_from_quail",
                KHItems.RAW_QUAIL.get(), KHItems.SMOKED_MEAT.get(), 1800);

        // Рецепты давки сока. Без этого рецепта виноградный чан был бы мёртвым
        // блоком — ровно та ошибка, что случилась с разделочным столом: он
        // был полностью зарегистрирован и на любой предмет отвечал отказом,
        // потому что рецептов его типа не существовало.
        //
        // Две грозди на бутылку: сок ценнее ягоды по применению (питьё,
        // основа кухни), но не должен быть выгоднее её по калориям, иначе
        // давка стала бы источником питания из ничего.
        pressing("grape_juice", KHItems.GRAPES.get(), 1, KHItems.GRAPE_JUICE.get(), 2);

>>>>>>> Stashed changes
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

        // --- Строительные материалы ---

        // Саман: глина + песок + солома (сухая трава как связующее).
        this.shaped(RecipeCategory.BUILDING_BLOCKS, KHItems.ADOBE_BRICKS.get())
                .pattern("CS")
                .pattern("SC")
                .define('C', Items.CLAY_BALL)
                .define('S', Items.WHEAT)
                .unlockedBy("has_clay_ball", this.has(Items.CLAY_BALL))
                .save(this.output);

        // Ракушечник: пористый известняк — прессуется из песка и ракушек.
        this.shaped(RecipeCategory.BUILDING_BLOCKS, KHItems.SHELL_ROCK.get())
                .pattern("NS")
                .pattern("SN")
                .define('N', Items.NAUTILUS_SHELL)
                .define('S', Items.SAND)
                .unlockedBy("has_nautilus_shell", this.has(Items.NAUTILUS_SHELL))
                .save(this.output);

        // Белёная штукатурка: известь (кальцит) наносится на основу из самана.
        this.shapeless(RecipeCategory.BUILDING_BLOCKS, KHItems.WHITEWASHED_PLASTER.get(), 2)
                .requires(KHItems.ADOBE_BRICKS.get(), 2)
                .requires(Items.CALCITE)
                .unlockedBy("has_calcite", this.has(Items.CALCITE))
                .save(this.output);

        // Ступеньки, плиты, стенки и рецепты камнереза — из семейств.
        this.generateRecipes(KHBlockFamilies.ADOBE_BRICKS, FeatureFlags.REGISTRY.allFlags());
        this.generateRecipes(KHBlockFamilies.SHELL_ROCK, FeatureFlags.REGISTRY.allFlags());
        this.generateRecipes(KHBlockFamilies.WHITEWASHED_PLASTER, FeatureFlags.REGISTRY.allFlags());

        // Черепица обжигается из кирпичей; расписная керамика сочетает её
        // с белой глазурью из кварца и кубанским синим пигментом.
        this.shaped(RecipeCategory.BUILDING_BLOCKS, KHItems.ROOF_TILES.get(), 4)
                .pattern("BB")
                .pattern("BB")
                .define('B', Items.BRICK)
                .unlockedBy("has_brick", this.has(Items.BRICK))
                .save(this.output);
        this.generateRecipes(KHBlockFamilies.ROOF_TILES, FeatureFlags.REGISTRY.allFlags());
        this.shaped(RecipeCategory.DECORATIONS, KHItems.DECORATIVE_CERAMIC.get(), 4)
                .pattern("LQL")
                .pattern("QRQ")
                .pattern("LQL")
                .define('L', Items.LAPIS_LAZULI)
                .define('Q', Items.QUARTZ)
                .define('R', KHItems.ROOF_TILES.get())
                .unlockedBy("has_roof_tiles", this.has(KHItems.ROOF_TILES.get()))
                .save(this.output);

        // Камнерез. Семейство эти рецепты не генерирует: ванильный
        // stonecutterResultFromBase сохраняет их по имени без пространства
        // имён, и они уехали бы в minecraft:.
        stonecutting(KHItems.ADOBE_BRICKS.get(), KHItems.ADOBE_BRICK_STAIRS.get(),
                RecipeCategory.BUILDING_BLOCKS, 1);
        stonecutting(KHItems.ADOBE_BRICKS.get(), KHItems.ADOBE_BRICK_SLAB.get(),
                RecipeCategory.BUILDING_BLOCKS, 2);
        stonecutting(KHItems.ADOBE_BRICKS.get(), KHItems.ADOBE_BRICK_WALL.get(),
                RecipeCategory.DECORATIONS, 1);
        stonecutting(KHItems.SHELL_ROCK.get(), KHItems.SHELL_ROCK_STAIRS.get(),
                RecipeCategory.BUILDING_BLOCKS, 1);
        stonecutting(KHItems.SHELL_ROCK.get(), KHItems.SHELL_ROCK_SLAB.get(),
                RecipeCategory.BUILDING_BLOCKS, 2);
        stonecutting(KHItems.SHELL_ROCK.get(), KHItems.SHELL_ROCK_WALL.get(),
                RecipeCategory.DECORATIONS, 1);
        stonecutting(KHItems.WHITEWASHED_PLASTER.get(), KHItems.WHITEWASHED_PLASTER_STAIRS.get(),
                RecipeCategory.BUILDING_BLOCKS, 1);
        stonecutting(KHItems.WHITEWASHED_PLASTER.get(), KHItems.WHITEWASHED_PLASTER_SLAB.get(),
                RecipeCategory.BUILDING_BLOCKS, 2);
        stonecutting(KHItems.ROOF_TILES.get(), KHItems.ROOF_TILE_STAIRS.get(),
                RecipeCategory.BUILDING_BLOCKS, 1);
        stonecutting(KHItems.ROOF_TILES.get(), KHItems.ROOF_TILE_SLAB.get(),
                RecipeCategory.BUILDING_BLOCKS, 2);
        stonecutting(KHItems.ROOF_TILES.get(), KHItems.DECORATIVE_CERAMIC.get(),
                RecipeCategory.DECORATIONS, 1);

        // Наличник вырезается из белёной штукатурки и даёт одну готовую раму.
        this.shaped(RecipeCategory.DECORATIONS, KHItems.CARVED_WINDOW_CASING.get())
                .pattern("PPP")
                .pattern("P P")
                .pattern("PPP")
                .define('P', KHItems.WHITEWASHED_PLASTER.get())
                .unlockedBy("has_whitewashed_plaster", this.has(KHItems.WHITEWASHED_PLASTER.get()))
                .save(this.output);
        stonecutting(KHItems.WHITEWASHED_PLASTER.get(), KHItems.CARVED_WINDOW_CASING.get(),
                RecipeCategory.DECORATIONS, 1);

        // Плетень: лоза на кольях. Семейство плетня рецепты не генерирует —
        // базовый блок совпадает с самой оградой, и ванильный
        // getBaseBlockForCrafting предложил бы плести плетень из плетня.
        this.shaped(RecipeCategory.DECORATIONS, KHItems.WATTLE.get(), 3)
                .pattern("SVS")
                .pattern("SVS")
                .define('S', Items.STICK)
                .define('V', net.minecraft.tags.ItemTags.LEAVES)
                .unlockedBy("has_sticks", this.has(Items.STICK))
                .save(this.output);

        // Калитка плетня: тот же материал, но рама вертикальная.
        this.shaped(RecipeCategory.DECORATIONS, KHItems.WATTLE_GATE.get())
                .pattern("SVS")
                .pattern("SVS")
                .define('S', Items.STICK)
                .define('V', net.minecraft.tags.ItemTags.LEAVES)
                .unlockedBy("has_wattle", this.has(KHItems.WATTLE.get()))
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

    /**
     * Рецепт камнереза в пространстве имён мода.
     *
     * <p>Ключ строится через {@link KHIds}, а не строкой без префикса, —
     * иначе рецепт попал бы в {@code minecraft:}.</p>
     */
    private void stonecutting(net.minecraft.world.level.ItemLike base,
            net.minecraft.world.level.ItemLike result, RecipeCategory category, int count) {
        net.minecraft.data.recipes.SingleItemRecipeBuilder
                .stonecutting(Ingredient.of(base), category, result, count)
                .unlockedBy(getHasName(base), this.has(base))
                .save(this.output, ResourceKey.create(Registries.RECIPE,
                        KHIds.of(getItemName(result) + "_from_" + getItemName(base)
                                + "_stonecutting")));
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

<<<<<<< Updated upstream
=======
    /**
     * Рецепт копчения в коптильне.
     *
     * <p>Времена заметно больше, чем у печи (200 тиков): копчение — не способ
     * пожарить побыстрее, а отдельная, более дорогая ветка, за которую игрок
     * получает продукт с бо́льшим насыщением. Плюс расход дров в самой
     * коптильне, которого печь для этого продукта не требует.</p>
     */
    private void smoking(String name, net.minecraft.world.level.ItemLike input,
            net.minecraft.world.level.ItemLike result, int ticks) {
        this.output.accept(
                ResourceKey.create(Registries.RECIPE, KHIds.of("smoking_process/" + name)),
                new dev.romankrukovsky.kubanhorizons.processing.SmokingProcessRecipe(
                        new Recipe.CommonInfo(true), "",
                        Ingredient.of(input),
                        new ItemStackTemplate(result.asItem()),
                        ticks),
                null);
    }

    /**
     * Рецепт давки сока в виноградном чане.
     *
     * <p>Параметров два, и оба про накопление, а не про партию:
     * {@code juicePerItem} — сколько сока даёт одна ягодная единица,
     * {@code juicePerBottle} — сколько сока стоит бутылка. Длительности здесь
     * нет намеренно: чан не ведёт цикл, он суммирует раздавленное, и «время
     * работы» у него равно времени, которое игрок готов топтать.</p>
     */
    private void pressing(String name, net.minecraft.world.level.ItemLike input,
            int juicePerItem, net.minecraft.world.level.ItemLike result, int juicePerBottle) {
        this.output.accept(
                ResourceKey.create(Registries.RECIPE, KHIds.of("pressing/" + name)),
                new dev.romankrukovsky.kubanhorizons.processing.PressingRecipe(
                        new Recipe.CommonInfo(true), "",
                        Ingredient.of(input),
                        juicePerItem,
                        new ItemStackTemplate(result.asItem()),
                        juicePerBottle),
                null);
    }

>>>>>>> Stashed changes
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
