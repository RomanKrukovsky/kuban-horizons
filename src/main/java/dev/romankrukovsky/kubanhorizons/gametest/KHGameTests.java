package dev.romankrukovsky.kubanhorizons.gametest;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.romankrukovsky.kubanhorizons.KubanHorizons;
import dev.romankrukovsky.kubanhorizons.blockentity.OilPressBlockEntity;
import dev.romankrukovsky.kubanhorizons.crop.SunflowerCropBlock;
import dev.romankrukovsky.kubanhorizons.item.KubanGuide;
import dev.romankrukovsky.kubanhorizons.registry.KHBlocks;
import dev.romankrukovsky.kubanhorizons.registry.KHItems;
import dev.romankrukovsky.kubanhorizons.util.KHIds;
import dev.romankrukovsky.kubanhorizons.worldgen.KHBiomes;
import dev.romankrukovsky.kubanhorizons.worldgen.KHNoiseSettings;
import dev.romankrukovsky.kubanhorizons.worldgen.KHPlacedFeatures;
import dev.romankrukovsky.kubanhorizons.worldgen.KHStructures;
import dev.romankrukovsky.kubanhorizons.worldgen.KHStructureSets;
import dev.romankrukovsky.kubanhorizons.worldgen.KHWorldPresets;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementNode;
import net.minecraft.advancements.AdvancementTree;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestInstance;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.WrittenBookContent;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Автоматические игровые тесты вертикального контура «подсолнечник».
 *
 * <p>GameTest в MC 26.2 — registry-based: функции тестов регистрируются в
 * {@code TEST_FUNCTION} через {@link TestFunctionLoader}, а экземпляры —
 * через {@link RegisterGameTestsEvent} NeoForge.</p>
 */
@EventBusSubscriber(modid = KubanHorizons.MOD_ID)
public final class KHGameTests {
    /** DeferredRegister гарантирует попадание функций в реестр TEST_FUNCTION. */
    private static final DeferredRegister<Consumer<GameTestHelper>> TEST_FUNCTIONS =
            DeferredRegister.create(Registries.TEST_FUNCTION, KubanHorizons.MOD_ID);

    /** Имена зарегистрированных тестов (для создания test instances). */
    private static final Map<String, Integer> TEST_MAX_TICKS = new LinkedHashMap<>();

    static {
        register("sunflower_plant", KHGameTests::testSunflowerPlant, 200);
        register("sunflower_full_growth", KHGameTests::testSunflowerFullGrowth, 600);
        register("sunflower_bonemeal", KHGameTests::testSunflowerBonemeal, 200);
        register("sunflower_harvest", KHGameTests::testSunflowerHarvest, 200);
        register("sunflower_top_no_duplicate_loot", KHGameTests::testTopHalfNoDuplicateLoot, 200);
        register("oil_press_recipe", KHGameTests::testOilPressRecipe, 200);
        register("oil_press_requires_bottle", KHGameTests::testOilPressRequiresBottle, 200);
        register("oil_press_persistence", KHGameTests::testOilPressSaveLoad, 200);
        register("registry_content", KHGameTests::testRegistryContent, 100);
        register("fertility_depletes_on_harvest", KHGameTests::testFertilityDepletion, 200);
        register("fertility_compost_restores", KHGameTests::testFertilityCompost, 200);
        register("fertility_rotation_gentler", KHGameTests::testFertilityRotation, 200);
        register("melon_harvest_depletes_stem_farmland", KHGameTests::testMelonHarvestFertility, 200);
        register("irrigation_channel_fills", KHGameTests::testIrrigationFills, 400);
        register("irrigation_channel_dries", KHGameTests::testIrrigationDries, 400);
        register("irrigation_hydrates_farmland", KHGameTests::testIrrigationHydratesFarmland, 400);
        register("corn_full_growth", KHGameTests::testCornFullGrowth, 600);
        register("tea_bush_pick_regrows", KHGameTests::testTeaBushPick, 300);
        register("rice_grows_in_water", KHGameTests::testRiceGrowsInWater, 600);
        register("rice_requires_water", KHGameTests::testRiceRequiresWater, 200);
        register("grape_graft_cutting", KHGameTests::testGrapeGraft, 200);
        register("grape_harvest_regrows", KHGameTests::testGrapeHarvest, 200);
        register("tomato_pick_regrows", KHGameTests::testTomatoPick, 200);
        register("fruit_leaves_ripen", KHGameTests::testFruitLeavesRipen, 400);
        register("fruit_pick_resets", KHGameTests::testFruitPickResets, 200);
        register("sapling_grows_tree", KHGameTests::testSaplingGrowsTree, 400);
        register("drying_rack_dries_tea", KHGameTests::testDryingRack, 400);
        register("hand_mill_grinds_wheat", KHGameTests::testHandMill, 300);
        register("kuban_guide_content", KHGameTests::testKubanGuideContent, 100);
        register("kuban_steppe_world_preset", KHGameTests::testKubanSteppeWorldPreset, 100);
        register("worldgen_feature_order", KHGameTests::testWorldgenFeatureOrder, 100);
        register("river_floodplain_biome", KHGameTests::testRiverFloodplainBiome, 100);
        register("structure_registry_integrity", KHGameTests::testStructureRegistryIntegrity, 100);
        register("advancement_tree_complete", KHGameTests::testAdvancementTree, 100);
        register("building_slab_drops_two", KHGameTests::testBuildingSlabDropsTwo, 100);
        register("building_requires_pickaxe", KHGameTests::testBuildingRequiresPickaxe, 100);
        register("wattle_connects", KHGameTests::testWattleConnects, 100);
        register("building_stonecutting_namespace", KHGameTests::testBuildingStonecuttingNamespace, 100);
    }

    private KHGameTests() {
    }

    private static void register(String name, Consumer<GameTestHelper> function, int maxTicks) {
        TEST_FUNCTIONS.register(name, () -> function);
        TEST_MAX_TICKS.put(name, maxTicks);
    }

    /** Вызывается из главного класса мода. */
    public static void register(net.neoforged.bus.api.IEventBus modEventBus) {
        TEST_FUNCTIONS.register(modEventBus);
    }

    @SubscribeEvent
    static void onRegisterGameTests(RegisterGameTestsEvent event) {
        Holder<TestEnvironmentDefinition<?>> env = event.registerEnvironment(
                KHIds.of("default"), new TestEnvironmentDefinition.AllOf(java.util.List.of()));
        Identifier emptyStructure = Identifier.withDefaultNamespace("empty");

        TEST_MAX_TICKS.forEach((name, maxTicks) -> {
            ResourceKey<Consumer<GameTestHelper>> functionKey =
                    ResourceKey.create(Registries.TEST_FUNCTION, KHIds.of(name));
            event.registerTest(KHIds.of(name), new FunctionGameTestInstance(functionKey,
                    new TestData<>(env, emptyStructure, maxTicks, 0, true)));
        });
    }

    // --- Вспомогательные ---

    private static BlockPos preparedFarmland(GameTestHelper helper, BlockPos pos) {
        helper.setBlock(pos.below(), Blocks.DIRT);
        helper.setBlock(pos, Blocks.FARMLAND.defaultBlockState()
                .setValue(net.minecraft.world.level.block.FarmlandBlock.MOISTURE, 7));
        return pos.above();
    }

    // --- Тесты культуры ---

    /** Семена сажаются на грядку и создают стадию 0 (нижняя половина). */
    private static void testSunflowerPlant(GameTestHelper helper) {
        BlockPos cropPos = preparedFarmland(helper, new BlockPos(1, 1, 1));
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,
                new ItemStack(KHItems.SUNFLOWER_SEEDS.get()));
        // Клик по верхней грани грядки — как это делает игрок при посадке.
        BlockPos absFarmland = helper.absolutePos(cropPos.below());
        helper.useBlock(cropPos.below(), player, new net.minecraft.world.phys.BlockHitResult(
                net.minecraft.world.phys.Vec3.atCenterOf(absFarmland).add(0.0, 0.5, 0.0),
                net.minecraft.core.Direction.UP, absFarmland, false));

        helper.succeedWhen(() -> {
            helper.assertBlockPresent(KHBlocks.SUNFLOWER_CROP.get(), cropPos);
            helper.assertBlockProperty(cropPos, SunflowerCropBlock.AGE, 0);
        });
    }

    /** Полный рост: при постоянных randomTick культура достигает стадии 4 с верхушкой. */
    private static void testSunflowerFullGrowth(GameTestHelper helper) {
        BlockPos cropPos = preparedFarmland(helper, new BlockPos(1, 1, 1));
        helper.setBlock(cropPos, KHBlocks.SUNFLOWER_CROP.get().defaultBlockState());

        // Ускоряем рост прямыми randomTick вызовами каждый тик.
        helper.onEachTick(() -> {
            ServerLevel level = helper.getLevel();
            BlockPos abs = helper.absolutePos(cropPos);
            BlockState state = level.getBlockState(abs);
            if (state.is(KHBlocks.SUNFLOWER_CROP.get())
                    && state.getValue(SunflowerCropBlock.HALF) == DoubleBlockHalf.LOWER) {
                state.randomTick(level, abs, level.getRandom());
            }
        });

        helper.succeedWhen(() -> {
            helper.assertBlockProperty(cropPos, SunflowerCropBlock.AGE, SunflowerCropBlock.MAX_AGE);
            helper.assertBlockPresent(KHBlocks.SUNFLOWER_CROP.get(), cropPos.above());
            helper.assertBlockProperty(cropPos.above(), SunflowerCropBlock.HALF, DoubleBlockHalf.UPPER);
        });
    }

    /** Костная мука доращивает культуру до зрелости. */
    private static void testSunflowerBonemeal(GameTestHelper helper) {
        BlockPos cropPos = preparedFarmland(helper, new BlockPos(1, 1, 1));
        helper.setBlock(cropPos, KHBlocks.SUNFLOWER_CROP.get().defaultBlockState());

        helper.startSequence()
                .thenExecute(() -> {
                    ServerLevel level = helper.getLevel();
                    BlockPos abs = helper.absolutePos(cropPos);
                    for (int i = 0; i < SunflowerCropBlock.MAX_AGE; i++) {
                        BlockState state = level.getBlockState(abs);
                        if (state.getBlock() instanceof SunflowerCropBlock crop) {
                            crop.performBonemeal(level, level.getRandom(), abs, state);
                        }
                    }
                })
                .thenExecuteAfter(2, () -> {
                    helper.assertBlockProperty(cropPos, SunflowerCropBlock.AGE, SunflowerCropBlock.MAX_AGE);
                    helper.assertBlockPresent(KHBlocks.SUNFLOWER_CROP.get(), cropPos.above());
                })
                .thenSucceed();
    }

    /** Сбор зрелого растения даёт шляпку и семена. */
    private static void testSunflowerHarvest(GameTestHelper helper) {
        BlockPos cropPos = preparedFarmland(helper, new BlockPos(1, 1, 1));
        placeMatureSunflower(helper, cropPos);

        helper.startSequence()
                // destroyBlock у хелпера не дропает лут: рушим с дропом сами.
                .thenExecute(() -> helper.getLevel()
                        .destroyBlock(helper.absolutePos(cropPos), true, null))
                .thenExecuteAfter(5, () -> {
                    helper.assertItemEntityPresent(KHItems.SUNFLOWER_HEAD.get(), cropPos, 2.0);
                })
                .thenSucceed();
    }

    /** Разрушение верхней половины не создаёт второй комплект лута. */
    private static void testTopHalfNoDuplicateLoot(GameTestHelper helper) {
        BlockPos cropPos = preparedFarmland(helper, new BlockPos(1, 1, 1));
        placeMatureSunflower(helper, cropPos);

        helper.startSequence()
                .thenExecute(() -> helper.getLevel()
                        .destroyBlock(helper.absolutePos(cropPos.above()), true, null))
                .thenExecuteAfter(5, () -> {
                    // Лут даёт только нижняя половина: шляпок не может быть
                    // больше одной.
                    long heads = helper.getEntities(net.minecraft.world.entity.EntityTypes.ITEM, cropPos, 3.0)
                            .stream()
                            .filter(e -> e.getItem().is(KHItems.SUNFLOWER_HEAD.get()))
                            .mapToLong(e -> e.getItem().getCount())
                            .sum();
                    helper.assertTrue(heads <= 1, "Дюп: верхняя половина дала дополнительный лут");
                })
                .thenSucceed();
    }

    private static void placeMatureSunflower(GameTestHelper helper, BlockPos cropPos) {
        BlockState lower = KHBlocks.SUNFLOWER_CROP.get().defaultBlockState()
                .setValue(SunflowerCropBlock.AGE, SunflowerCropBlock.MAX_AGE)
                .setValue(SunflowerCropBlock.HALF, DoubleBlockHalf.LOWER);
        helper.setBlock(cropPos, lower);
        helper.setBlock(cropPos.above(), lower.setValue(SunflowerCropBlock.HALF, DoubleBlockHalf.UPPER));
    }

    /** Путеводитель создаётся как подписанная книга с полным набором страниц. */
    private static void testKubanGuideContent(GameTestHelper helper) {
        ItemStack guide = KubanGuide.create();
        WrittenBookContent content = guide.get(DataComponents.WRITTEN_BOOK_CONTENT);

        helper.assertTrue(guide.is(Items.WRITTEN_BOOK),
                "Путеводитель должен использовать ванильную письменную книгу");
        helper.assertTrue(content != null, "У путеводителя отсутствует содержимое книги");
        helper.assertTrue(content.pages().size() == KubanGuide.PAGES,
                "Ожидалось страниц: " + KubanGuide.PAGES + ", получено: " + content.pages().size());
        helper.assertTrue(guide.has(DataComponents.CUSTOM_NAME),
                "У путеводителя отсутствует локализуемое название");
        helper.succeed();
    }

    /** Пресет содержит три ванильных измерения и реально использует биомы мода. */
    private static void testKubanSteppeWorldPreset(GameTestHelper helper) {
        var registries = helper.getLevel().registryAccess();
        var preset = registries.lookupOrThrow(Registries.WORLD_PRESET)
                .getOrThrow(KHWorldPresets.KUBAN_HORIZONS)
                .value();
        var overworld = preset.overworld().orElseThrow();

        helper.assertTrue(preset.createWorldDimensions().dimensions().size() == 3,
                "Пресет должен сохранять Overworld, Nether и End");
        helper.assertTrue(overworld.generator().getBiomeSource().possibleBiomes().stream()
                        .anyMatch(biome -> biome.is(KHBiomes.KUBAN_STEPPE)),
                "Кубанская степь отсутствует в biome source пресета");
        helper.assertTrue(overworld.generator().getBiomeSource().possibleBiomes().stream()
                        .anyMatch(biome -> biome.is(KHBiomes.PLAVNI)),
                "Плавни отсутствуют в biome source пресета");
        helper.assertTrue(overworld.generator().getBiomeSource().possibleBiomes().stream()
                        .anyMatch(biome -> biome.is(KHBiomes.LIMAN)),
                "Лиман отсутствует в biome source пресета");
        helper.assertTrue(overworld.generator().getBiomeSource().possibleBiomes().stream()
                        .anyMatch(biome -> biome.is(KHBiomes.RIVER_FLOODPLAIN)),
                "Пойма реки отсутствует в biome source пресета");
        helper.assertTrue(overworld.generator() instanceof net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator
                        && ((net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator) overworld.generator())
                                .stable(KHNoiseSettings.OVERWORLD),
                "Пресет не использует surface rules плавней, лимана и поймы");
        helper.succeed();
    }

    /**
     * Инициализирует реальный граф декорации opt-in генератора.
     *
     * <p>{@link net.minecraft.world.level.chunk.ChunkGenerator#validate()} вызывает
     * тот же {@code FeatureSorter.buildFeaturesPerStep}, который лениво запускается
     * при декорации чанка. Поэтому тест падает на несовместимом порядке features
     * даже когда spawn GameTest-сервера не пересекает пользовательский биом.</p>
     */
    private static void testWorldgenFeatureOrder(GameTestHelper helper) {
        var preset = helper.getLevel().registryAccess()
                .lookupOrThrow(Registries.WORLD_PRESET)
                .getOrThrow(KHWorldPresets.KUBAN_HORIZONS)
                .value();
        var generator = preset.overworld().orElseThrow().generator();

        try {
            generator.validate();
        } catch (IllegalStateException exception) {
            throw new AssertionError("Несовместимый порядок worldgen features opt-in preset", exception);
        }
        helper.succeed();
    }

    /** Пойма реки: речная фауна, влажный климат и точный порядок растительности. */
    private static void testRiverFloodplainBiome(GameTestHelper helper) {
        var biome = helper.getLevel().registryAccess()
                .lookupOrThrow(Registries.BIOME)
                .getOrThrow(KHBiomes.RIVER_FLOODPLAIN)
                .value();

        helper.assertTrue(biome.hasPrecipitation(), "Пойма должна быть влажным биомом");
        helper.assertTrue(!biome.getMobSettings().getMobs(MobCategory.WATER_AMBIENT).isEmpty(),
                "В пойме отсутствует речная рыба");
        helper.assertTrue(biome.getMobSettings().getMobs(MobCategory.CREATURE).unwrap().stream()
                        .anyMatch(entry -> entry.value().type() == EntityTypes.COW),
                "Заливные луга поймы должны быть пастбищами");

        List<Holder<PlacedFeature>> vegetation = biome.getGenerationSettings()
                .features()
                .get(GenerationStep.Decoration.VEGETAL_DECORATION.ordinal())
                .stream()
                .toList();
        List<String> actualOrder = vegetation.stream()
                .map(feature -> feature.unwrapKey()
                        .map(key -> key.identifier().toString())
                        .orElse("<unregistered>"))
                .toList();
        List<String> expectedOrder = List.of(
                "minecraft:glow_lichen",
                "minecraft:trees_water",
                "minecraft:patch_bush",
                "minecraft:flower_default",
                "minecraft:patch_grass_badlands",
                "minecraft:brown_mushroom_normal",
                "minecraft:red_mushroom_normal",
                "minecraft:patch_pumpkin",
                "minecraft:patch_sugar_cane",
                "minecraft:patch_firefly_bush_near_water",
                "minecraft:seagrass_river",
                KHPlacedFeatures.WILD_RICE_PLACED.identifier().toString());
        helper.assertTrue(actualOrder.equals(expectedOrder),
                "Порядок растительности поймы отличается от vanilla river baseline: " + actualOrder);
        helper.assertTrue(vegetation.getLast().is(KHPlacedFeatures.WILD_RICE_PLACED),
                "Дикий рис должен добавляться последним biome modifier'ом");
        helper.assertTrue(actualOrder.stream().noneMatch(id -> id.equals("minecraft:patch_berry_common")
                        || id.equals("minecraft:patch_tall_grass_2")),
                "В пойму вернулся shared feature с несовместимым порядком: " + actualOrder);
        helper.succeed();
    }

    /** Структуры и их наборы загружены и не могут выйти за границы родного биома. */
    private static void testStructureRegistryIntegrity(GameTestHelper helper) {
        var registries = helper.getLevel().registryAccess();
        var biomeRegistry = registries.lookupOrThrow(Registries.BIOME);
        var structureRegistry = registries.lookupOrThrow(Registries.STRUCTURE);
        var structureSetRegistry = registries.lookupOrThrow(Registries.STRUCTURE_SET);

        var fishingCamp = structureRegistry.getOrThrow(KHStructures.FLOODPLAIN_FISHING_CAMP);
        var reedShelter = structureRegistry.getOrThrow(KHStructures.PLAVNI_REED_SHELTER);
        var fishingCamps = structureSetRegistry.getOrThrow(KHStructureSets.FLOODPLAIN_FISHING_CAMPS);
        var reedShelters = structureSetRegistry.getOrThrow(KHStructureSets.PLAVNI_REED_SHELTERS);

        helper.assertTrue(fishingCamps.value().structures().size() == 1
                        && fishingCamps.value().structures().getFirst().structure().is(
                                KHStructures.FLOODPLAIN_FISHING_CAMP),
                "Набор рыбацких станов должен ссылаться только на свой стан");
        helper.assertTrue(reedShelters.value().structures().size() == 1
                        && reedShelters.value().structures().getFirst().structure().is(
                                KHStructures.PLAVNI_REED_SHELTER),
                "Набор камышовых навесов должен ссылаться только на свой навес");

        var floodplain = biomeRegistry.getOrThrow(KHBiomes.RIVER_FLOODPLAIN);
        var plavni = biomeRegistry.getOrThrow(KHBiomes.PLAVNI);
        helper.assertTrue(fishingCamp.value().biomes().size() == 1
                        && fishingCamp.value().biomes().contains(floodplain),
                "Рыбацкий стан должен генерироваться только в пойме реки");
        helper.assertTrue(reedShelter.value().biomes().size() == 1
                        && reedShelter.value().biomes().contains(plavni),
                "Камышовый навес должен генерироваться только в плавнях");
        helper.succeed();
    }

    // --- Достижения ---

    /**
     * Дерево достижений: каждая ветка загружена, все узлы восходят к корню мода
     * и каждый заголовок/описание имеет перевод в обоих языках.
     *
     * <p>Опечатка в пути родителя приводит к тому, что ванильный
     * {@code AdvancementTree.addAll} молча выбрасывает узел, а забытый перевод
     * даёт пустую строку в интерфейсе — этот тест ловит и то, и другое.</p>
     */
    private static void testAdvancementTree(GameTestHelper helper) {
        String[] expected = {
                "root",
                "farming/sunflower_seeds", "farming/sunflower_head", "farming/sunflower_oil",
                "farming/roasted_seeds",
                "kitchen/homemade_bread", "kitchen/borscht", "kitchen/tea_cup", "kitchen/taster",
                "rice/rice_seedlings", "rice/rice_panicle", "rice/cooked_rice",
                "vineyard/grape_cutting", "vineyard/grape_trellis", "vineyard/grapes",
                "tea/tea_sapling", "tea/tea_leaves", "tea/dried_tea",
                "orchard/sapling", "orchard/first_fruit", "orchard/dried_fruit",
                "orchard/kuban_orchard",
        };

        AdvancementTree tree = helper.getLevel().getServer().getAdvancements().tree();
        Identifier rootId = KHIds.of("root");
        JsonObject english = readLang("en_us");
        JsonObject russian = readLang("ru_ru");

        for (String path : expected) {
            Identifier id = KHIds.of(path);
            AdvancementNode node = tree.get(id);
            helper.assertTrue(node != null, "Достижение не загружено (проверьте родителя): " + path);

            AdvancementNode root = node.root();
            helper.assertTrue(root.holder().id().equals(rootId),
                    "Достижение " + path + " не входит в дерево мода, корень: " + root.holder().id());

            DisplayInfo display = node.advancement().display().orElse(null);
            helper.assertTrue(display != null, "У достижения нет display: " + path);
            for (Component component : List.of(display.getTitle(), display.getDescription())) {
                String key = ((TranslatableContents) component.getContents()).getKey();
                helper.assertTrue(english.has(key), "Нет английского перевода: " + key);
                helper.assertTrue(russian.has(key), "Нет русского перевода: " + key);
            }
        }

        // Челленджи требуют выполнить все условия, а не любое из них.
        Advancement orchardChallenge = tree.get(KHIds.of("orchard/kuban_orchard")).advancement();
        helper.assertTrue(orchardChallenge.requirements().size() == 4,
                "«Кубанский сад» должен требовать все четыре плода, групп условий: "
                        + orchardChallenge.requirements().size());
        helper.succeed();
    }

    /** Читает сгенерированный файл локализации с пути ресурсов мода. */
    private static JsonObject readLang(String locale) {
        String path = "/assets/kubanhorizons/lang/" + locale + ".json";
        try (InputStream stream = KHGameTests.class.getResourceAsStream(path)) {
            if (stream == null) {
                throw new IllegalStateException("Файл локализации не найден: " + path);
            }
            return JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8))
                    .getAsJsonObject();
        } catch (IOException exception) {
            throw new IllegalStateException("Не удалось прочитать " + path, exception);
        }
    }

    // --- Тесты маслопресса ---

    /** 8 семян + бутылка → масло + жмых (ручные обороты винта). */
    private static void testOilPressRecipe(GameTestHelper helper) {
        BlockPos pressPos = new BlockPos(1, 1, 1);
        helper.setBlock(pressPos, KHBlocks.OIL_PRESS.get());

        helper.startSequence()
                .thenExecute(() -> {
                    OilPressBlockEntity press = helper.getBlockEntity(pressPos, OilPressBlockEntity.class);
                    press.setItem(OilPressBlockEntity.SLOT_INPUT, new ItemStack(KHItems.SUNFLOWER_SEEDS.get(), 8));
                    press.setItem(OilPressBlockEntity.SLOT_BOTTLE, new ItemStack(Items.GLASS_BOTTLE, 1));
                    // Ручные обороты до готовности (300 тиков работы / 60 за оборот = 5 оборотов).
                    for (int i = 0; i < 6; i++) {
                        press.turnScrew(helper.getLevel());
                    }
                })
                .thenExecuteAfter(2, () -> {
                    OilPressBlockEntity press = helper.getBlockEntity(pressPos, OilPressBlockEntity.class);
                    ItemStack result = press.getItem(OilPressBlockEntity.SLOT_RESULT);
                    ItemStack byproduct = press.getItem(OilPressBlockEntity.SLOT_BYPRODUCT);
                    helper.assertTrue(result.is(KHItems.SUNFLOWER_OIL.get()) && result.getCount() == 1,
                            "Ожидалась 1 бутылка масла, получено: " + result);
                    helper.assertTrue(byproduct.is(KHItems.OIL_CAKE.get()) && byproduct.getCount() == 1,
                            "Ожидался 1 жмых, получено: " + byproduct);
                    helper.assertTrue(press.getItem(OilPressBlockEntity.SLOT_INPUT).isEmpty(),
                            "Сырьё должно быть израсходовано полностью");
                    helper.assertTrue(press.getItem(OilPressBlockEntity.SLOT_BOTTLE).isEmpty(),
                            "Бутылка должна быть израсходована");
                })
                .thenSucceed();
    }

    /** Без бутылки отжим не начинается и сырьё не расходуется. */
    private static void testOilPressRequiresBottle(GameTestHelper helper) {
        BlockPos pressPos = new BlockPos(1, 1, 1);
        helper.setBlock(pressPos, KHBlocks.OIL_PRESS.get());

        helper.startSequence()
                .thenExecute(() -> {
                    OilPressBlockEntity press = helper.getBlockEntity(pressPos, OilPressBlockEntity.class);
                    press.setItem(OilPressBlockEntity.SLOT_INPUT, new ItemStack(KHItems.SUNFLOWER_SEEDS.get(), 8));
                    for (int i = 0; i < 10; i++) {
                        press.turnScrew(helper.getLevel());
                    }
                })
                .thenExecuteAfter(2, () -> {
                    OilPressBlockEntity press = helper.getBlockEntity(pressPos, OilPressBlockEntity.class);
                    helper.assertTrue(press.getItem(OilPressBlockEntity.SLOT_INPUT).getCount() == 8,
                            "Сырьё не должно расходоваться без бутылки");
                    helper.assertTrue(press.getItem(OilPressBlockEntity.SLOT_RESULT).isEmpty(),
                            "Без бутылки не должно быть результата");
                })
                .thenSucceed();
    }

    /** Содержимое пресса переживает пересохранение BlockEntity (NBT round-trip). */
    private static void testOilPressSaveLoad(GameTestHelper helper) {
        BlockPos pressPos = new BlockPos(1, 1, 1);
        helper.setBlock(pressPos, KHBlocks.OIL_PRESS.get());

        helper.startSequence()
                .thenExecute(() -> {
                    OilPressBlockEntity press = helper.getBlockEntity(pressPos, OilPressBlockEntity.class);
                    press.setItem(OilPressBlockEntity.SLOT_INPUT, new ItemStack(KHItems.SUNFLOWER_SEEDS.get(), 5));
                    press.setItem(OilPressBlockEntity.SLOT_BOTTLE, new ItemStack(Items.GLASS_BOTTLE, 2));

                    // Round-trip: сохранить NBT → пересоздать BE → сверить.
                    var registries = helper.getLevel().registryAccess();
                    var tag = press.saveWithFullMetadata(registries);
                    var restored = net.minecraft.world.level.block.entity.BlockEntity.loadStatic(
                            helper.absolutePos(pressPos),
                            helper.getLevel().getBlockState(helper.absolutePos(pressPos)),
                            tag, registries);
                    helper.assertTrue(restored instanceof OilPressBlockEntity,
                            "BlockEntity не восстановился из NBT");
                    OilPressBlockEntity restoredPress = (OilPressBlockEntity) restored;
                    helper.assertTrue(restoredPress.getItem(OilPressBlockEntity.SLOT_INPUT).getCount() == 5,
                            "Слот сырья потерял содержимое при сериализации");
                    helper.assertTrue(restoredPress.getItem(OilPressBlockEntity.SLOT_BOTTLE).getCount() == 2,
                            "Слот бутылок потерял содержимое при сериализации");
                })
                .thenSucceed();
    }

    // --- Тесты кукурузы и чая ---

    /** Кукуруза достигает зрелости с верхней половиной. */
    private static void testCornFullGrowth(GameTestHelper helper) {
        BlockPos cropPos = preparedFarmland(helper, new BlockPos(1, 1, 1));
        helper.setBlock(cropPos, KHBlocks.CORN_CROP.get().defaultBlockState());

        helper.onEachTick(() -> {
            ServerLevel level = helper.getLevel();
            BlockPos abs = helper.absolutePos(cropPos);
            BlockState state = level.getBlockState(abs);
            if (state.is(KHBlocks.CORN_CROP.get())
                    && state.getValue(dev.romankrukovsky.kubanhorizons.crop.DoubleCropBlock.HALF) == DoubleBlockHalf.LOWER) {
                state.randomTick(level, abs, level.getRandom());
            }
        });

        helper.succeedWhen(() -> {
            helper.assertBlockProperty(cropPos, dev.romankrukovsky.kubanhorizons.crop.DoubleCropBlock.AGE,
                    dev.romankrukovsky.kubanhorizons.crop.CornCropBlock.MAX_AGE);
            helper.assertBlockPresent(KHBlocks.CORN_CROP.get(), cropPos.above());
        });
    }

    /** Сбор чайного листа ПКМ: даёт лист, куст не уничтожается (стадия 1). */
    private static void testTeaBushPick(GameTestHelper helper) {
        BlockPos bushPos = new BlockPos(1, 2, 1);
        helper.setBlock(bushPos.below(), Blocks.GRASS_BLOCK);
        helper.setBlock(bushPos, KHBlocks.TEA_BUSH.get().defaultBlockState()
                .setValue(dev.romankrukovsky.kubanhorizons.crop.TeaBushBlock.AGE,
                        dev.romankrukovsky.kubanhorizons.crop.TeaBushBlock.MAX_AGE));

        helper.startSequence()
                .thenExecute(() -> {
                    Player player = helper.makeMockPlayer(GameType.SURVIVAL);
                    helper.useBlock(bushPos, player);
                })
                .thenExecuteAfter(5, () -> {
                    helper.assertBlockPresent(KHBlocks.TEA_BUSH.get(), bushPos);
                    helper.assertBlockProperty(bushPos,
                            dev.romankrukovsky.kubanhorizons.crop.TeaBushBlock.AGE, 1);
                    helper.assertItemEntityPresent(KHItems.TEA_LEAVES.get(), bushPos, 2.0);
                })
                .thenSucceed();
    }

    /** Сбор томатов ПКМ: даёт томаты, куст откатывается к стадии 2. */
    private static void testTomatoPick(GameTestHelper helper) {
        BlockPos bushPos = preparedFarmland(helper, new BlockPos(1, 1, 1));
        helper.setBlock(bushPos, KHBlocks.TOMATO_BUSH.get().defaultBlockState()
                .setValue(dev.romankrukovsky.kubanhorizons.crop.TomatoBushBlock.AGE,
                        dev.romankrukovsky.kubanhorizons.crop.TomatoBushBlock.MAX_AGE));

        helper.startSequence()
                .thenExecute(() -> {
                    Player player = helper.makeMockPlayer(GameType.SURVIVAL);
                    helper.useBlock(bushPos, player);
                })
                .thenExecuteAfter(5, () -> {
                    helper.assertBlockPresent(KHBlocks.TOMATO_BUSH.get(), bushPos);
                    helper.assertBlockProperty(bushPos,
                            dev.romankrukovsky.kubanhorizons.crop.TomatoBushBlock.AGE, 2);
                    helper.assertItemEntityPresent(KHItems.TOMATO.get(), bushPos, 2.0);
                })
                .thenSucceed();
    }

    // --- Тесты плодовых деревьев ---

    /** Плодовая листва дозревает: AGE 0 → 2 при форсированных randomTick. */
    private static void testFruitLeavesRipen(GameTestHelper helper) {
        BlockPos leavesPos = new BlockPos(1, 2, 1);
        helper.setBlock(leavesPos, KHBlocks.PEACH_LEAVES.get().defaultBlockState()
                .setValue(net.minecraft.world.level.block.LeavesBlock.PERSISTENT, true)
                .setValue(dev.romankrukovsky.kubanhorizons.crop.FruitLeavesBlock.AGE, 0));

        helper.onEachTick(() -> {
            ServerLevel level = helper.getLevel();
            BlockPos abs = helper.absolutePos(leavesPos);
            BlockState state = level.getBlockState(abs);
            if (state.is(KHBlocks.PEACH_LEAVES.get())) {
                state.randomTick(level, abs, level.getRandom());
            }
        });

        helper.succeedWhen(() -> helper.assertBlockProperty(leavesPos,
                dev.romankrukovsky.kubanhorizons.crop.FruitLeavesBlock.AGE,
                dev.romankrukovsky.kubanhorizons.crop.FruitLeavesBlock.MAX_AGE));
    }

    /** Сбор плодов ПКМ: даёт плод и откатывает листву к стадии 0. */
    private static void testFruitPickResets(GameTestHelper helper) {
        BlockPos leavesPos = new BlockPos(1, 2, 1);
        helper.setBlock(leavesPos, KHBlocks.PLUM_LEAVES.get().defaultBlockState()
                .setValue(net.minecraft.world.level.block.LeavesBlock.PERSISTENT, true)
                .setValue(dev.romankrukovsky.kubanhorizons.crop.FruitLeavesBlock.AGE,
                        dev.romankrukovsky.kubanhorizons.crop.FruitLeavesBlock.MAX_AGE));

        helper.startSequence()
                .thenExecute(() -> {
                    Player player = helper.makeMockPlayer(GameType.SURVIVAL);
                    helper.useBlock(leavesPos, player);
                })
                .thenExecuteAfter(5, () -> {
                    helper.assertBlockPresent(KHBlocks.PLUM_LEAVES.get(), leavesPos);
                    helper.assertBlockProperty(leavesPos,
                            dev.romankrukovsky.kubanhorizons.crop.FruitLeavesBlock.AGE, 0);
                    helper.assertItemEntityPresent(KHItems.PLUM.get(), leavesPos, 2.0);
                })
                .thenSucceed();
    }

    /** Саженец с форс-тиками строит дерево: лог над саженцем + листва. */
    private static void testSaplingGrowsTree(GameTestHelper helper) {
        BlockPos saplingPos = new BlockPos(2, 2, 2);
        helper.setBlock(saplingPos.below(), Blocks.GRASS_BLOCK);
        helper.setBlock(saplingPos, KHBlocks.APRICOT_SAPLING.get().defaultBlockState());

        helper.onEachTick(() -> {
            ServerLevel level = helper.getLevel();
            BlockPos abs = helper.absolutePos(saplingPos);
            BlockState state = level.getBlockState(abs);
            if (state.is(KHBlocks.APRICOT_SAPLING.get())) {
                state.randomTick(level, abs, level.getRandom());
            }
        });

        helper.succeedWhen(() -> {
            // Ствол занял место саженца и растёт вверх.
            helper.assertBlockPresent(Blocks.OAK_LOG, saplingPos);
            helper.assertBlockPresent(Blocks.OAK_LOG, saplingPos.above());
            // Верхушка кроны — плодовая листва с PERSISTENT=true.
            BlockPos top = saplingPos.above(4);
            helper.assertBlockPresent(KHBlocks.APRICOT_LEAVES.get(), top);
            helper.assertBlockProperty(top,
                    net.minecraft.world.level.block.LeavesBlock.PERSISTENT, true);
        });
    }

    /** Мельница мелет пшеницу в муку за 3 оборота. */
    private static void testHandMill(GameTestHelper helper) {
        BlockPos millPos = new BlockPos(1, 1, 1);
        helper.setBlock(millPos, KHBlocks.HAND_MILL.get());

        helper.startSequence()
                .thenExecute(() -> {
                    var mill = helper.getBlockEntity(millPos,
                            dev.romankrukovsky.kubanhorizons.blockentity.HandMillBlockEntity.class);
                    ItemStack wheat = new ItemStack(net.minecraft.world.item.Items.WHEAT, 1);
                    helper.assertTrue(mill.insert(helper.getLevel(), wheat),
                            "Мельница должна принять пшеницу");
                    for (int i = 0; i < 3; i++) {
                        helper.assertTrue(mill.turn(helper.getLevel()),
                                "Оборот " + i + " должен быть выполнен");
                    }
                    helper.assertTrue(!mill.hasInput(), "Сырьё должно быть смолото");
                })
                .thenExecuteAfter(5, () -> helper.assertItemEntityPresent(
                        KHItems.FLOUR.get(), millPos.above(), 2.0))
                .thenSucceed();
    }

    /** Сушилка принимает чайный лист и высушивает его в сушёный чай. */
    private static void testDryingRack(GameTestHelper helper) {
        BlockPos rackPos = new BlockPos(1, 1, 1);
        helper.setBlock(rackPos, KHBlocks.DRYING_RACK.get());

        helper.startSequence()
                .thenExecute(() -> {
                    var rack = helper.getBlockEntity(rackPos,
                            dev.romankrukovsky.kubanhorizons.blockentity.DryingRackBlockEntity.class);
                    ItemStack leaves = new ItemStack(KHItems.TEA_LEAVES.get(), 1);
                    helper.assertTrue(rack.insert(helper.getLevel(), leaves),
                            "Рама должна принять чайный лист");
                    helper.assertTrue(leaves.isEmpty(), "Лист должен быть изъят из стека");
                    // 1200 тиков сушки чая — форсируем напрямую.
                    rack.advanceDrying(helper.getLevel(), 1300);
                })
                .thenExecuteAfter(2, () -> {
                    var rack = helper.getBlockEntity(rackPos,
                            dev.romankrukovsky.kubanhorizons.blockentity.DryingRackBlockEntity.class);
                    ItemStack out = rack.removeLast(helper.getLevel());
                    helper.assertTrue(out.is(KHItems.DRIED_TEA.get()),
                            "Ожидался сушёный чай, получено: " + out);
                })
                .thenSucceed();
    }

    // --- Тесты винограда ---

    /** Черенок прививается на пустую шпалеру (AGE 0 → 1) и расходуется. */
    private static void testGrapeGraft(GameTestHelper helper) {
        BlockPos trellisPos = new BlockPos(1, 2, 1);
        helper.setBlock(trellisPos.below(), Blocks.GRASS_BLOCK);
        helper.setBlock(trellisPos, KHBlocks.GRAPE_TRELLIS.get());

        helper.startSequence()
                .thenExecute(() -> {
                    Player player = helper.makeMockPlayer(GameType.SURVIVAL);
                    player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,
                            new ItemStack(KHItems.GRAPE_CUTTING.get(), 1));
                    helper.useBlock(trellisPos, player);
                    helper.assertTrue(player.getMainHandItem().isEmpty(),
                            "Черенок должен расходоваться при прививке");
                })
                .thenExecuteAfter(2, () -> helper.assertBlockProperty(trellisPos,
                        dev.romankrukovsky.kubanhorizons.crop.GrapeTrellisBlock.AGE, 1))
                .thenSucceed();
    }

    /** Сбор гроздьев: даёт виноград, лоза откатывается к стадии 2. */
    private static void testGrapeHarvest(GameTestHelper helper) {
        BlockPos trellisPos = new BlockPos(1, 2, 1);
        helper.setBlock(trellisPos.below(), Blocks.GRASS_BLOCK);
        helper.setBlock(trellisPos, KHBlocks.GRAPE_TRELLIS.get().defaultBlockState()
                .setValue(dev.romankrukovsky.kubanhorizons.crop.GrapeTrellisBlock.AGE,
                        dev.romankrukovsky.kubanhorizons.crop.GrapeTrellisBlock.MAX_AGE));

        helper.startSequence()
                .thenExecute(() -> {
                    Player player = helper.makeMockPlayer(GameType.SURVIVAL);
                    helper.useBlock(trellisPos, player);
                })
                .thenExecuteAfter(5, () -> {
                    helper.assertBlockProperty(trellisPos,
                            dev.romankrukovsky.kubanhorizons.crop.GrapeTrellisBlock.AGE, 2);
                    helper.assertItemEntityPresent(KHItems.GRAPES.get(), trellisPos, 2.0);
                })
                .thenSucceed();
    }

    // --- Тесты риса ---

    /** Рис в затопленном чеке растёт до зрелости. */
    private static void testRiceGrowsInWater(GameTestHelper helper) {
        BlockPos ricePos = new BlockPos(1, 2, 1);
        helper.setBlock(ricePos.below(), Blocks.DIRT);
        helper.setBlock(ricePos, KHBlocks.RICE_CROP.get().defaultBlockState());

        helper.onEachTick(() -> {
            ServerLevel level = helper.getLevel();
            BlockPos abs = helper.absolutePos(ricePos);
            BlockState state = level.getBlockState(abs);
            if (state.is(KHBlocks.RICE_CROP.get())) {
                state.randomTick(level, abs, level.getRandom());
            }
        });

        helper.succeedWhen(() -> helper.assertBlockProperty(ricePos,
                dev.romankrukovsky.kubanhorizons.crop.RiceCropBlock.AGE,
                dev.romankrukovsky.kubanhorizons.crop.RiceCropBlock.MAX_AGE));
    }

    /** Без воды молодой рис не растёт (randomTick не двигает стадию). */
    private static void testRiceRequiresWater(GameTestHelper helper) {
        BlockPos ricePos = new BlockPos(1, 2, 1);
        helper.setBlock(ricePos.below(), Blocks.DIRT);
        // Осушенный рис: WATERLOGGED=false выставляем напрямую.
        helper.setBlock(ricePos, KHBlocks.RICE_CROP.get().defaultBlockState()
                .setValue(dev.romankrukovsky.kubanhorizons.crop.RiceCropBlock.WATERLOGGED, false)
                .setValue(dev.romankrukovsky.kubanhorizons.crop.RiceCropBlock.AGE, 3));
        // Незрелый без воды сломался бы canSurvive — проверяем логику
        // randomTick на зрелом, затем прямой вызов на молодом состоянии.
        helper.startSequence()
                .thenExecute(() -> {
                    ServerLevel level = helper.getLevel();
                    BlockPos abs = helper.absolutePos(ricePos);
                    // 50 принудительных randomTick на сухом блоке.
                    BlockState dryYoung = KHBlocks.RICE_CROP.get().defaultBlockState()
                            .setValue(dev.romankrukovsky.kubanhorizons.crop.RiceCropBlock.WATERLOGGED, false)
                            .setValue(dev.romankrukovsky.kubanhorizons.crop.RiceCropBlock.AGE, 0);
                    for (int i = 0; i < 50; i++) {
                        dryYoung.randomTick(level, abs, level.getRandom());
                    }
                    // Блок в мире не должен был вырасти (randomTick сухого
                    // состояния — no-op).
                    helper.assertBlockProperty(ricePos,
                            dev.romankrukovsky.kubanhorizons.crop.RiceCropBlock.AGE, 3);
                })
                .thenSucceed();
    }

    // --- Тесты плодородия ---

    /** Сбор урожая истощает грядку. */
    private static void testFertilityDepletion(GameTestHelper helper) {
        BlockPos cropPos = preparedFarmland(helper, new BlockPos(1, 1, 1));
        BlockPos farmlandAbs = helper.absolutePos(cropPos.below());
        ServerLevel level = helper.getLevel();

        int before = dev.romankrukovsky.kubanhorizons.soil.SoilFertility.fertility(level, farmlandAbs);
        dev.romankrukovsky.kubanhorizons.soil.SoilFertility.onHarvest(level, farmlandAbs,
                KHBlocks.SUNFLOWER_CROP.get());
        dev.romankrukovsky.kubanhorizons.soil.SoilFertility.onHarvest(level, farmlandAbs,
                KHBlocks.SUNFLOWER_CROP.get());
        int after = dev.romankrukovsky.kubanhorizons.soil.SoilFertility.fertility(level, farmlandAbs);

        helper.assertTrue(after < before,
                "Плодородие должно снижаться при повторных сборах: " + before + " -> " + after);
        helper.succeed();
    }

    /** Компост восстанавливает плодородие. */
    private static void testFertilityCompost(GameTestHelper helper) {
        BlockPos cropPos = preparedFarmland(helper, new BlockPos(1, 1, 1));
        BlockPos farmlandAbs = helper.absolutePos(cropPos.below());
        ServerLevel level = helper.getLevel();

        dev.romankrukovsky.kubanhorizons.soil.SoilFertility.onHarvest(level, farmlandAbs,
                KHBlocks.SUNFLOWER_CROP.get());
        dev.romankrukovsky.kubanhorizons.soil.SoilFertility.onHarvest(level, farmlandAbs,
                KHBlocks.SUNFLOWER_CROP.get());
        int depleted = dev.romankrukovsky.kubanhorizons.soil.SoilFertility.fertility(level, farmlandAbs);
        dev.romankrukovsky.kubanhorizons.soil.SoilFertility.onCompost(level, farmlandAbs);
        int restored = dev.romankrukovsky.kubanhorizons.soil.SoilFertility.fertility(level, farmlandAbs);

        helper.assertTrue(restored > depleted,
                "Компост должен восстанавливать плодородие: " + depleted + " -> " + restored);
        helper.succeed();
    }

    /** Севооборот истощает почву меньше, чем монокультура. */
    private static void testFertilityRotation(GameTestHelper helper) {
        BlockPos monoAbs = helper.absolutePos(preparedFarmland(helper, new BlockPos(1, 1, 1)).below());
        BlockPos rotAbs = helper.absolutePos(preparedFarmland(helper, new BlockPos(3, 1, 1)).below());
        ServerLevel level = helper.getLevel();

        // Монокультура: подсолнечник дважды.
        dev.romankrukovsky.kubanhorizons.soil.SoilFertility.onHarvest(level, monoAbs, KHBlocks.SUNFLOWER_CROP.get());
        dev.romankrukovsky.kubanhorizons.soil.SoilFertility.onHarvest(level, monoAbs, KHBlocks.SUNFLOWER_CROP.get());
        // Ротация: подсолнечник → пшеница.
        dev.romankrukovsky.kubanhorizons.soil.SoilFertility.onHarvest(level, rotAbs, KHBlocks.SUNFLOWER_CROP.get());
        dev.romankrukovsky.kubanhorizons.soil.SoilFertility.onHarvest(level, rotAbs, Blocks.WHEAT);

        int mono = dev.romankrukovsky.kubanhorizons.soil.SoilFertility.fertility(level, monoAbs);
        int rotated = dev.romankrukovsky.kubanhorizons.soil.SoilFertility.fertility(level, rotAbs);
        helper.assertTrue(rotated > mono,
                "Севооборот должен беречь почву: моно=" + mono + ", ротация=" + rotated);
        helper.succeed();
    }

    /** Сбор ванильного арбуза истощает грядку его привязанного стебля. */
    private static void testMelonHarvestFertility(GameTestHelper helper) {
        BlockPos stemPos = preparedFarmland(helper, new BlockPos(1, 1, 1));
        BlockPos fruitPos = stemPos.east();
        helper.setBlock(fruitPos.below(), Blocks.DIRT);
        helper.setBlock(stemPos, Blocks.ATTACHED_MELON_STEM.defaultBlockState()
                .setValue(net.minecraft.world.level.block.AttachedStemBlock.FACING, Direction.EAST));
        helper.setBlock(fruitPos, Blocks.MELON);

        ServerLevel level = helper.getLevel();
        BlockPos stemFarmland = helper.absolutePos(stemPos.below());
        BlockPos fruitGround = helper.absolutePos(fruitPos.below());
        int stemBefore = dev.romankrukovsky.kubanhorizons.soil.SoilFertility.fertility(level, stemFarmland);
        int fruitBefore = dev.romankrukovsky.kubanhorizons.soil.SoilFertility.fertility(level, fruitGround);

        helper.startSequence()
                .thenExecute(() -> {
                    Player player = helper.makeMockPlayer(GameType.SURVIVAL);
                    BlockPos fruitAbs = helper.absolutePos(fruitPos);
                    net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(
                            new net.neoforged.neoforge.event.level.block.BreakBlockEvent(
                                    level, fruitAbs, level.getBlockState(fruitAbs), player));
                    level.destroyBlock(fruitAbs, true, player);
                })
                .thenExecuteAfter(2, () -> {
                    int stemAfter = dev.romankrukovsky.kubanhorizons.soil.SoilFertility.fertility(level, stemFarmland);
                    int fruitAfter = dev.romankrukovsky.kubanhorizons.soil.SoilFertility.fertility(level, fruitGround);
                    helper.assertTrue(stemAfter < stemBefore,
                            "Арбуз должен истощать грядку привязанного стебля: "
                                    + stemBefore + " -> " + stemAfter);
                    helper.assertTrue(fruitAfter == fruitBefore,
                            "Почва под плодом не должна учитываться как грядка: "
                                    + fruitBefore + " -> " + fruitAfter);
                })
                .thenSucceed();
    }

    // --- Тесты орошения ---

    /** Желоба заполняются водой от активного водозабора. */
    private static void testIrrigationFills(GameTestHelper helper) {
        BlockPos water = new BlockPos(0, 1, 0);
        BlockPos intake = new BlockPos(1, 1, 0);
        helper.setBlock(water, Blocks.WATER);
        helper.setBlock(intake, KHBlocks.WATER_INTAKE.get());
        for (int i = 0; i < 3; i++) {
            helper.setBlock(new BlockPos(2 + i, 1, 0), KHBlocks.IRRIGATION_CHANNEL.get());
        }

        helper.succeedWhen(() -> {
            helper.assertBlockProperty(intake,
                    dev.romankrukovsky.kubanhorizons.irrigation.WaterIntakeBlock.ACTIVE, true);
            for (int i = 0; i < 3; i++) {
                helper.assertBlockProperty(new BlockPos(2 + i, 1, 0),
                        dev.romankrukovsky.kubanhorizons.irrigation.IrrigationChannelBlock.DISTANCE, i + 1);
            }
        });
    }

    /** При удалении водозабора желоба осушаются. */
    private static void testIrrigationDries(GameTestHelper helper) {
        BlockPos water = new BlockPos(0, 1, 0);
        BlockPos intake = new BlockPos(1, 1, 0);
        BlockPos channel = new BlockPos(2, 1, 0);
        helper.setBlock(water, Blocks.WATER);
        helper.setBlock(intake, KHBlocks.WATER_INTAKE.get());
        helper.setBlock(channel, KHBlocks.IRRIGATION_CHANNEL.get());

        helper.startSequence()
                .thenWaitUntil(() -> helper.assertBlockProperty(channel,
                        dev.romankrukovsky.kubanhorizons.irrigation.IrrigationChannelBlock.DISTANCE, 1))
                .thenExecute(() -> {
                    helper.setBlock(intake, Blocks.AIR);
                    helper.setBlock(water, Blocks.AIR);
                })
                .thenWaitUntil(() -> helper.assertBlockProperty(channel,
                        dev.romankrukovsky.kubanhorizons.irrigation.IrrigationChannelBlock.DISTANCE, 0))
                .thenSucceed();
    }

    /** Заполненный желоб увлажняет соседнюю грядку (moisture 7). */
    private static void testIrrigationHydratesFarmland(GameTestHelper helper) {
        BlockPos water = new BlockPos(0, 1, 0);
        BlockPos intake = new BlockPos(1, 1, 0);
        BlockPos channel = new BlockPos(2, 1, 0);
        BlockPos farmland = new BlockPos(3, 1, 0);
        helper.setBlock(water, Blocks.WATER);
        helper.setBlock(intake, KHBlocks.WATER_INTAKE.get());
        helper.setBlock(channel, KHBlocks.IRRIGATION_CHANNEL.get());
        helper.setBlock(farmland.below(), Blocks.DIRT);
        helper.setBlock(farmland, Blocks.FARMLAND.defaultBlockState()
                .setValue(net.minecraft.world.level.block.FarmlandBlock.MOISTURE, 0));

        helper.startSequence()
                .thenWaitUntil(() -> helper.assertBlockProperty(channel,
                        dev.romankrukovsky.kubanhorizons.irrigation.IrrigationChannelBlock.DISTANCE, 1))
                .thenExecute(() -> {
                    // Форсируем randomTick грядки: в реальной игре это
                    // происходит естественно; в тесте — детерминированно.
                    ServerLevel level = helper.getLevel();
                    BlockPos abs = helper.absolutePos(farmland);
                    BlockState state = level.getBlockState(abs);
                    state.randomTick(level, abs, level.getRandom());
                })
                .thenExecuteAfter(2, () -> helper.assertBlockProperty(farmland,
                        net.minecraft.world.level.block.FarmlandBlock.MOISTURE, 7))
                .thenSucceed();
    }

    // --- Строительные материалы ---

    /**
     * Двойная плита отдаёт два предмета.
     *
     * <p>Дроп проверяется через {@code Block.getDrops} с явным инструментом:
     * {@code destroyBlock} у мира ломает блок ПУСТЫМ инструментом, а плита
     * требует кирку, и лут был бы пустым независимо от таблицы.</p>
     */
    private static void testBuildingSlabDropsTwo(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, KHBlocks.ADOBE_BRICK_SLAB.get().defaultBlockState()
                .setValue(net.minecraft.world.level.block.SlabBlock.TYPE,
                        net.minecraft.world.level.block.state.properties.SlabType.DOUBLE));

        BlockPos abs = helper.absolutePos(pos);
        List<ItemStack> drops = net.minecraft.world.level.block.Block.getDrops(
                level.getBlockState(abs), level, abs, null, null,
                new ItemStack(Items.IRON_PICKAXE));
        int total = drops.stream()
                .filter(stack -> stack.is(KHItems.ADOBE_BRICK_SLAB.get()))
                .mapToInt(ItemStack::getCount)
                .sum();
        helper.assertValueEqual(total, 2, "Двойная плита самана должна дать 2 плиты");

        // Одинарная плита — ровно одна.
        helper.setBlock(pos, KHBlocks.ADOBE_BRICK_SLAB.get().defaultBlockState()
                .setValue(net.minecraft.world.level.block.SlabBlock.TYPE,
                        net.minecraft.world.level.block.state.properties.SlabType.BOTTOM));
        List<ItemStack> single = net.minecraft.world.level.block.Block.getDrops(
                level.getBlockState(abs), level, abs, null, null,
                new ItemStack(Items.IRON_PICKAXE));
        int singleTotal = single.stream()
                .filter(stack -> stack.is(KHItems.ADOBE_BRICK_SLAB.get()))
                .mapToInt(ItemStack::getCount)
                .sum();
        helper.assertValueEqual(singleTotal, 1, "Одинарная плита самана должна дать 1 плиту");
        helper.succeed();
    }

    /**
     * Саман и ракушечник добываются только киркой.
     *
     * <p>Проверяется механизм, которым ванилла реально гейтит дроп:
     * {@code requiresCorrectToolForDrops} + {@code isCorrectToolForDrops}
     * инструмента (в loot table условия на инструмент нет — его подставляет
     * {@code ServerPlayerGameMode} при разрушении).</p>
     */
    private static void testBuildingRequiresPickaxe(GameTestHelper helper) {
        assertNeedsPickaxe(helper, new BlockPos(1, 1, 1),
                KHBlocks.ADOBE_BRICKS.get(), KHItems.ADOBE_BRICKS.get());
        assertNeedsPickaxe(helper, new BlockPos(2, 1, 1),
                KHBlocks.SHELL_ROCK.get(), KHItems.SHELL_ROCK.get());
        helper.succeed();
    }

    private static void assertNeedsPickaxe(GameTestHelper helper, BlockPos pos,
            net.minecraft.world.level.block.Block block, net.minecraft.world.item.Item drop) {
        ServerLevel level = helper.getLevel();
        helper.setBlock(pos, block);
        BlockPos abs = helper.absolutePos(pos);
        BlockState state = level.getBlockState(abs);

        helper.assertTrue(state.requiresCorrectToolForDrops(),
                "Блок должен требовать инструмент: " + drop);
        helper.assertTrue(!new ItemStack(Items.IRON_AXE).isCorrectToolForDrops(state),
                "Топор не должен считаться подходящим инструментом: " + drop);
        // Ярус не нужен — как ванильный грязевой кирпич, хватает деревянной.
        helper.assertTrue(new ItemStack(Items.WOODEN_PICKAXE).isCorrectToolForDrops(state),
                "Деревянная кирка должна подходить: " + drop);

        List<ItemStack> drops = net.minecraft.world.level.block.Block.getDrops(
                state, level, abs, null, null, new ItemStack(Items.IRON_PICKAXE));
        helper.assertValueEqual(drops.size(), 1, "Кирка должна дать один предмет: " + drop);
        helper.assertTrue(drops.getFirst().is(drop),
                "Дроп не совпадает с блоком: " + drop);
    }

    /**
     * Плетень стыкуется с ванильной оградой и со своей калиткой.
     *
     * <p>Соседи ставятся ПОСЛЕ плетня: {@code setBlock} обновляет формы
     * соседей, но не свою собственную, — поэтому плетень должен быть на месте
     * первым, чтобы его обновили оба соседа.</p>
     *
     * <p>Калитка повёрнута по оси Z (FACING=NORTH): именно при такой оси
     * {@code FenceGateBlock.connectsToDirection} разрешает стык с запада и
     * востока.</p>
     */
    private static void testWattleConnects(GameTestHelper helper) {
        BlockPos wattle = new BlockPos(2, 1, 1);
        helper.setBlock(wattle.below(), Blocks.DIRT);
        helper.setBlock(wattle, KHBlocks.WATTLE.get());
        helper.setBlock(wattle.west(), Blocks.OAK_FENCE);
        helper.setBlock(wattle.east(), KHBlocks.WATTLE_GATE.get().defaultBlockState()
                .setValue(net.minecraft.world.level.block.FenceGateBlock.FACING, Direction.NORTH));

        helper.startSequence()
                .thenExecuteAfter(2, () -> {
                    helper.assertBlockProperty(wattle,
                            net.minecraft.world.level.block.FenceBlock.WEST, true);
                    helper.assertBlockProperty(wattle,
                            net.minecraft.world.level.block.FenceBlock.EAST, true);
                    // По оси Z соседей нет — стыков быть не должно.
                    helper.assertBlockProperty(wattle,
                            net.minecraft.world.level.block.FenceBlock.NORTH, false);
                    helper.assertBlockProperty(wattle,
                            net.minecraft.world.level.block.FenceBlock.SOUTH, false);
                })
                .thenSucceed();
    }

    /**
     * Рецепты камнереза лежат в {@code kubanhorizons:}, а не в {@code minecraft:}.
     *
     * <p>Регрессия: ванильный {@code stonecutterResultFromBase} сохраняет
     * рецепт по имени без пространства имён, и {@code Identifier.parse}
     * относил его к {@code minecraft}.</p>
     */
    private static void testBuildingStonecuttingNamespace(GameTestHelper helper) {
        String[] names = {
                "adobe_brick_stairs_from_adobe_bricks_stonecutting",
                "adobe_brick_slab_from_adobe_bricks_stonecutting",
                "adobe_brick_wall_from_adobe_bricks_stonecutting",
                "shell_rock_stairs_from_shell_rock_stonecutting",
                "shell_rock_slab_from_shell_rock_stonecutting",
                "shell_rock_wall_from_shell_rock_stonecutting",
        };
        var recipes = helper.getLevel().recipeAccess();
        for (String name : names) {
            helper.assertTrue(recipes.byKey(ResourceKey.create(Registries.RECIPE,
                            KHIds.of(name))).isPresent(),
                    "Нет рецепта камнереза: kubanhorizons:" + name);
            helper.assertTrue(recipes.byKey(ResourceKey.create(Registries.RECIPE,
                            Identifier.withDefaultNamespace(name))).isEmpty(),
                    "Рецепт камнереза утёк в minecraft: " + name);
        }
        helper.succeed();
    }

    // --- Реестры ---

    /** Все заявленные ID контента присутствуют в реестрах. */
    private static void testRegistryContent(GameTestHelper helper) {
        String[] blocks = {"sunflower_crop", "oil_press",
                "adobe_bricks", "adobe_brick_stairs", "adobe_brick_slab", "adobe_brick_wall",
                "shell_rock", "shell_rock_stairs", "shell_rock_slab", "shell_rock_wall",
                "wattle", "wattle_gate"};
        String[] items = {"sunflower_seeds", "sunflower_head", "sunflower_oil",
                "oil_cake", "roasted_sunflower_seeds", "oil_press",
                "adobe_bricks", "adobe_brick_stairs", "adobe_brick_slab", "adobe_brick_wall",
                "shell_rock", "shell_rock_stairs", "shell_rock_slab", "shell_rock_wall",
                "wattle", "wattle_gate"};
        for (String id : blocks) {
            helper.assertTrue(BuiltInRegistries.BLOCK.containsKey(KHIds.of(id)),
                    "Блок отсутствует в реестре: " + id);
        }
        for (String id : items) {
            helper.assertTrue(BuiltInRegistries.ITEM.containsKey(KHIds.of(id)),
                    "Предмет отсутствует в реестре: " + id);
        }
        helper.succeed();
    }
}
