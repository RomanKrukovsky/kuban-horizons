package dev.romankrukovsky.kubanhorizons.gametest;

import dev.romankrukovsky.kubanhorizons.KubanHorizons;
import dev.romankrukovsky.kubanhorizons.blockentity.OilPressBlockEntity;
import dev.romankrukovsky.kubanhorizons.crop.SunflowerCropBlock;
import dev.romankrukovsky.kubanhorizons.registry.KHBlocks;
import dev.romankrukovsky.kubanhorizons.registry.KHItems;
import dev.romankrukovsky.kubanhorizons.util.KHIds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestInstance;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.LinkedHashMap;
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

    // --- Реестры ---

    /** Все заявленные ID контента присутствуют в реестрах. */
    private static void testRegistryContent(GameTestHelper helper) {
        String[] blocks = {"sunflower_crop", "oil_press"};
        String[] items = {"sunflower_seeds", "sunflower_head", "sunflower_oil",
                "oil_cake", "roasted_sunflower_seeds", "oil_press"};
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
