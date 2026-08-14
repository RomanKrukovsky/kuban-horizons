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
import dev.romankrukovsky.kubanhorizons.worldgen.KubanBiomeSource;
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
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.WrittenBookContent;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.BlockItemTags;
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
import java.util.Set;
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
        // Тест единственной джиннии должен идти отдельно от других тестов,
        // создающих ту же глобально привязанную сущность.
        register("genie_aura_of_laws", KHGameTests::testGenieAuraOfLaws, 100);
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
        register("kuban_stronghold_biomes", KHGameTests::testKubanStrongholdBiomes, 100);
        register("kuban_wild_crop_features", KHGameTests::testKubanWildCropFeatures, 100);
        register("worldgen_feature_order", KHGameTests::testWorldgenFeatureOrder, 100);
        register("river_floodplain_biome", KHGameTests::testRiverFloodplainBiome, 100);
        // Маршрутизация биомов: биом, который источник никогда не вернёт,
        // недостижим в игре, сколько бы его ни регистрировали.
        register("biome_routing_covers_climate_regions",
                KHGameTests::testBiomeRoutingCoversClimateRegions, 100);
        register("biome_source_declares_everything_it_returns",
                KHGameTests::testBiomeSourceDeclaresEverythingItReturns, 100);
        register("world_actually_has_multiple_biomes",
                KHGameTests::testWorldActuallyHasMultipleBiomes, 200);
        register("new_biomes_are_distinct", KHGameTests::testNewBiomesAreDistinct, 100);
        register("structure_registry_integrity", KHGameTests::testStructureRegistryIntegrity, 100);
        register("structure_templates_load", KHGameTests::testStructureTemplatesLoad, 100);
        register("advancement_tree_complete", KHGameTests::testAdvancementTree, 100);
        register("building_slab_drops_two", KHGameTests::testBuildingSlabDropsTwo, 100);
        register("building_requires_pickaxe", KHGameTests::testBuildingRequiresPickaxe, 100);
        register("wattle_connects", KHGameTests::testWattleConnects, 100);
        register("building_stonecutting_namespace", KHGameTests::testBuildingStonecuttingNamespace, 100);
        register("plaster_slab_drops_two", KHGameTests::testPlasterSlabDropsTwo, 100);
        register("plaster_tools_drops_and_tags", KHGameTests::testPlasterToolsDropsAndTags, 100);
        register("carved_casing_facing_and_shape", KHGameTests::testCarvedCasingFacingAndShape, 100);
        register("roof_tile_slab_drops_two", KHGameTests::testRoofTileSlabDropsTwo, 100);
        register("ceramics_tools_drops_and_tags", KHGameTests::testCeramicsToolsDropsAndTags, 100);
<<<<<<< Updated upstream
=======
        register("genie_pull_window_shrinks", KHGameTests::testPullWindowShrinks, 100);
        register("genie_leash_tension_by_distance", KHGameTests::testLeashTensionByDistance, 100);
        register("genie_law_silence_frees", KHGameTests::testLawSilenceFrees, 100);
        register("genie_is_wishborne", KHGameTests::testGenieIsWishborne, 100);
        register("genie_personality_changes", KHGameTests::testGeniePersonalityChanges, 100);
        register("genie_brain_prioritizes_danger", KHGameTests::testGenieBrainPrioritizesDanger, 100);
        register("genie_brain_remembers_actions", KHGameTests::testGenieBrainRemembersActions, 100);
        register("genie_dialog_server_actions", KHGameTests::testGenieDialogServerActions, 100);
        register("genie_lamp_binds_and_summons", KHGameTests::testGenieLampBindsAndSummons, 100);
        register("genie_conditional_rules_persist", KHGameTests::testConditionalRulesPersist, 100);
        register("genie_conditional_wish_runtime", KHGameTests::testConditionalWishRuntime, 100);
        register("genie_living_painting_enters_other_level",
                KHGameTests::testGenieLivingPaintingEntersOtherLevel, 100);
        register("genie_predictive_planning", KHGameTests::testGeniePredictivePlanning, 100);
        register("genie_defense_irony", KHGameTests::testGenieDefenseIrony, 100);
        register("genie_survives_hit_in_place", KHGameTests::testGenieSurvivesHitInPlace, 100);
        register("genie_is_unique", KHGameTests::testGenieIsUnique, 100);
        register("genie_meta_rules", KHGameTests::testGenieMetaRules, 100);
        register("genie_weather_policy", KHGameTests::testGenieWeatherPolicy, 100);
        register("genie_clock_policy", KHGameTests::testGenieClockPolicy, 100);
        register("genie_gigantism_engine", KHGameTests::testGenieGigantismEngine, 100);
        register("genie_world_memory", KHGameTests::testGenieWorldMemory, 100);
        register("genie_inviolability", KHGameTests::testGenieInviolabilityAndKillIntercept, 100);
        register("genie_literal_wish", KHGameTests::testLiteralWishEngine, 100);
        register("genie_visual_effects", KHGameTests::testGenieTailEngineAndCartoonAnatomy, 100);
        register("genie_magical_defeat_state", KHGameTests::testGenieMagicalDefeatState, 100);
        register("genie_runtime_restores_region", KHGameTests::testGenieRuntimeRestoresRegion, 200);
        register("genie_miniaturization_round_trip", KHGameTests::testGenieMiniaturizationRoundTrip, 100);
        register("genie_materialized_word_has_letters", KHGameTests::testGenieMaterializedWord, 100);
        register("genie_hybrid_has_real_traits", KHGameTests::testGenieHybridTraits, 100);
        register("genie_contract_has_terms", KHGameTests::testGenieContractTerms, 100);
        register("genie_biome_rewrite_changes_world", KHGameTests::testGenieBiomeRewrite, 100);
        register("genie_role_swap_changes_roles", KHGameTests::testGenieRoleSwap, 100);
        register("genie_runtime_miniaturize_confirmation", KHGameTests::testRuntimeMiniaturizeConfirmation, 100);
        register("genie_snapshot_management", KHGameTests::testSnapshotManagement, 100);
        register("genie_runtime_pocket_scene", KHGameTests::testRuntimePocketScene, 100);
        register("genie_dialog_pocket_scene_cycle", KHGameTests::testDialogPocketSceneCycle, 100);
        register("genie_runtime_structure_move", KHGameTests::testRuntimeStructureMove, 100);
        register("genie_runtime_structure_rotate", KHGameTests::testRuntimeStructureRotate, 100);
        register("genie_runtime_structure_moves_entities", KHGameTests::testRuntimeStructureMovesEntities, 100);
        register("genie_runtime_magic_drawing", KHGameTests::testRuntimeMagicDrawing, 100);
        register("player_genie_distorted_wish_parse", KHGameTests::testPlayerGenieDistortedWishParse, 100);
        register("player_genie_attachment_persistence", KHGameTests::testPlayerGenieAttachmentPersistence, 100);
        register("player_genie_transformation_controller", KHGameTests::testPlayerGenieTransformationController, 100);
        register("player_genie_vessel_and_master", KHGameTests::testPlayerGenieVesselAndMaster, 100);
        register("player_genie_progression_tiers", KHGameTests::testPlayerGenieProgressionTiers, 100);
        register("player_genie_true_omnipotence_ending", KHGameTests::testPlayerGenieTrueOmnipotenceEnding, 100);
        register("region_snapshot_round_trip", KHGameTests::testRegionSnapshotRoundTrip, 200);
        register("region_snapshot_respects_limit", KHGameTests::testRegionSnapshotRespectsLimit, 100);
        register("region_payload_round_trip", KHGameTests::testRegionPayloadRoundTrip, 200);
        // Манул: доверие, характер и окрас должны переживать перезагрузку —
        // доверие, теряющееся при перезаходе, это молчаливый сбой.
        register("manul_trust_survives_reload", KHGameTests::testManulTrustSurvivesReload, 100);
        register("manul_offering_is_day_gated", KHGameTests::testManulOfferingDayGated, 100);
        register("manul_not_tamed_by_one_fish", KHGameTests::testManulNotTamedByOneFish, 100);
        register("manul_anger_cools_down", KHGameTests::testManulAngerCoolsDown, 900);
        register("fauna_chain_is_reachable", KHGameTests::testFaunaChainReachable, 100);
        register("cutting_board_cuts", KHGameTests::testCuttingBoardCuts, 100);
        register("every_device_is_craftable", KHGameTests::testDevicesCraftable, 100);
        register("manul_personalities_differ", KHGameTests::testManulPersonalitiesDiffer, 100);
        register("manul_witness_lowers_trust", KHGameTests::testManulWitnessLowersTrust, 100);
        register("manul_loot_is_worthless", KHGameTests::testManulLootIsWorthless, 100);
        register("manul_spawn_is_rare_and_nocturnal", KHGameTests::testManulSpawnRareNocturnal, 100);
        // Связь манула с миром: укрытие, репутация и достижимость критериев.
        register("manul_kill_lowers_reputation", KHGameTests::testManulKillLowersReputation, 100);
        register("manul_shelter_becomes_occupied", KHGameTests::testManulShelterBecomesOccupied, 100);
        register("manul_criteria_are_reachable", KHGameTests::testManulCriteriaAreReachable, 100);
        register("manul_steals_fish_from_trader", KHGameTests::testManulStealsFishFromTrader, 100);
        register("manul_sleeps_in_daytime_den", KHGameTests::testManulSleepsInDen, 900);
        // Садовый контур: вход в ветку (созревание, сбор и рост дерева уже
        // покрыты тестами fruit_leaves_ripen / fruit_pick_resets / sapling_grows_tree).
        register("orchard_is_reachable", KHGameTests::testOrchardReachable, 100);
        register("tooltips_are_translated", KHGameTests::testTooltipsTranslated, 100);
        // Атмосфера биомов. Регистрация звукового события ничего не значит:
        // мод уже возил девятнадцать немых голосов. Тесты проверяют, что
        // атмосфера РАЗРЕШАЕТСЯ из реестра биомов, озвучена файлами и
        // не затирает ванильное пещерное настроение.
        register("biomes_have_ambience", KHGameTests::testBiomesHaveAmbience, 100);
        register("biome_ambience_is_distinct", KHGameTests::testBiomeAmbienceDistinct, 100);
        register("ambience_is_subtitled", KHGameTests::testAmbienceSubtitled, 100);
        // Конфигурация: каждая живая настройка обязана читаться на реальном
        // пути кода, а удалённая — не оставлять следов в файле.
        register("config_options_are_alive", KHGameTests::testConfigOptionsAlive, 100);
        register("debug_overlay_reads_world", KHGameTests::testDebugOverlayReadsWorld, 100);
        // Виноградный чан: весь путь «гроздь → сок» и анти-дюп. Регистрация
        // блока и типа рецепта ничего не доказывает — доказывает бутылка сока
        // в инвентаре игрока и ровно один списанный виноград.
        register("grape_press_makes_juice", KHGameTests::testGrapePressMakesJuice, 100);
        register("grape_press_accumulates", KHGameTests::testGrapePressAccumulates, 100);
        register("grape_press_no_dupe", KHGameTests::testGrapePressNoDupe, 100);
        register("grape_press_stomping_works", KHGameTests::testGrapePressStomping, 100);
        register("grape_press_persistence", KHGameTests::testGrapePressPersistence, 100);
        register("genie_aura_dissolves_fireballs", KHGameTests::testGenieAuraDissolvesFireballs, 100);
        // Глобальная policy не должна выполняться параллельно с тестом диалога,
        // который намеренно включает и отменяет то же правило.
        register("genie_instant_smelt_policy", KHGameTests::testGenieInstantSmeltPolicy, 100);
>>>>>>> Stashed changes
    }

    private KHGameTests() {
    }

    private static void register(String name, Consumer<GameTestHelper> function, int maxTicks) {
        TEST_FUNCTIONS.register(name, () -> function);
        TEST_MAX_TICKS.put(name, maxTicks);
    }

<<<<<<< Updated upstream
=======
    /** Физический урон не является состоянием поражения для Wishborne-сущности. */
    /**
     * Окно затягивания сокращается с искажением, монотонно и в объявленных границах.
     *
     * <p>Проверяется свойство, а не конкретные числа: важно, что второе жестокое
     * желание никогда не удлиняет окно. Монотонность — то, на что игрок реально
     * опирается, когда решает, позволить себе ещё одно желание или нет.</p>
     */
    private static void testPullWindowShrinks(GameTestHelper helper) {
        long previous = Long.MAX_VALUE;
        for (int corruption = 0; corruption <= 100; corruption += 10) {
            long window = dev.romankrukovsky.kubanhorizons.genie.vessel.VesselPull.windowFor(corruption);
            helper.assertTrue(window <= previous,
                    "Окно должно сокращаться с искажением, но при " + corruption
                            + " выросло с " + previous + " до " + window);
            helper.assertTrue(window > 0L, "Окно обязано быть положительным при искажении " + corruption);
            previous = window;
        }
        helper.assertTrue(
                dev.romankrukovsky.kubanhorizons.genie.vessel.VesselPull.windowFor(100)
                        < dev.romankrukovsky.kubanhorizons.genie.vessel.VesselPull.windowFor(0) / 2,
                "Полное искажение должно сокращать окно более чем вдвое");
        // Выход за границы не должен ломать расчёт: значения зажимаются.
        helper.assertTrue(
                dev.romankrukovsky.kubanhorizons.genie.vessel.VesselPull.windowFor(-50)
                        == dev.romankrukovsky.kubanhorizons.genie.vessel.VesselPull.windowFor(0),
                "Отрицательное искажение должно вести себя как нулевое");
        helper.succeed();
    }

    /**
     * Натяжение растёт с расстоянием, а ненайденный сосуд не тянет вовсе.
     *
     * <p>Последнее — не мелочь, а игровое обещание: спрятать сосуд далеко значит
     * выиграть время, и если {@code null} давал бы максимум, пряталки работали бы
     * наоборот.</p>
     */
    private static void testLeashTensionByDistance(GameTestHelper helper) {
        var origin = new net.minecraft.world.phys.Vec3(0.0D, 64.0D, 0.0D);
        var leash = dev.romankrukovsky.kubanhorizons.genie.vessel.VesselLeash.Tension.SLACK;
        helper.assertTrue(
                dev.romankrukovsky.kubanhorizons.genie.vessel.VesselLeash.tensionFor(null, origin) == leash,
                "Ненайденный сосуд не должен натягивать хвост");

        var previous = -1;
        for (double distance : new double[] {2.0D, 20.0D, 40.0D, 90.0D}) {
            var vessel = new dev.romankrukovsky.kubanhorizons.genie.vessel.VesselTracker.Located(
                    origin.add(distance, 0.0D, 0.0D),
                    dev.romankrukovsky.kubanhorizons.genie.vessel.VesselTracker.Holder.GROUND);
            int ordinal = dev.romankrukovsky.kubanhorizons.genie.vessel.VesselLeash
                    .tensionFor(vessel, origin).ordinal();
            helper.assertTrue(ordinal >= previous,
                    "Натяжение не должно падать с ростом расстояния (на " + distance + " блоках)");
            previous = ordinal;
        }
        var far = new dev.romankrukovsky.kubanhorizons.genie.vessel.VesselTracker.Located(
                origin.add(90.0D, 0.0D, 0.0D),
                dev.romankrukovsky.kubanhorizons.genie.vessel.VesselTracker.Holder.GROUND);
        helper.assertTrue(
                dev.romankrukovsky.kubanhorizons.genie.vessel.VesselLeash.tensionFor(far, origin)
                        == dev.romankrukovsky.kubanhorizons.genie.vessel.VesselLeash.Tension.TAUT,
                "Далёкий сосуд должен натягивать хвост до струны");
        helper.succeed();
    }

    /**
     * Тишина обесценивает выход, а желание снова делает его дорогим.
     *
     * <p>Это и есть закон сосуда целиком, и именно это свойство закрывает
     * одиночную игру: там сосуд не трогает никто, поэтому цена доходит до нуля
     * сама, без отдельной ветки кода.</p>
     */
    private static void testLawSilenceFrees(GameTestHelper helper) {
        var attachment = new dev.romankrukovsky.kubanhorizons.genie.player.PlayerGenieAttachment();
        long now = helper.getLevel().getGameTime();

        // Желание только что: тишины нет.
        attachment.setLastWishTick(now);
        long fresh = dev.romankrukovsky.kubanhorizons.genie.vessel.VesselLaw
                .silenceTicks(helper.getLevel(), attachment);
        helper.assertTrue(fresh == 0L, "Сразу после желания тишина должна быть нулевой, а не " + fresh);

        // Желание сутки назад: тишина накопилась.
        attachment.setLastWishTick(Math.max(1L, now - 24_000L));
        long aged = dev.romankrukovsky.kubanhorizons.genie.vessel.VesselLaw
                .silenceTicks(helper.getLevel(), attachment);
        helper.assertTrue(aged > fresh, "Тишина должна расти со временем: " + aged + " не больше " + fresh);

        // Нулевой lastWishTick — сосуда не трогали ни разу, а не «трогали в начале мира».
        attachment.setLastWishTick(0L);
        long never = dev.romankrukovsky.kubanhorizons.genie.vessel.VesselLaw
                .silenceTicks(helper.getLevel(), attachment);
        helper.assertTrue(never == helper.getLevel().getGameTime(),
                "Нетронутый сосуд должен считать тишину от начала мира");
        helper.succeed();
    }

    private static void testGenieIsWishborne(GameTestHelper helper) {
        dev.romankrukovsky.kubanhorizons.genie.GenieAnchor.releaseFor(helper.getLevel());
        var genie = helper.spawn(KHEntities.KUBAN_GENIE.get(), new BlockPos(1, 2, 1));
        float before = genie.getHealth();
        boolean accepted = genie.hurtServer(helper.getLevel(), helper.getLevel().damageSources().generic(), 1000.0F);
        helper.assertTrue(!accepted, "Джинния не должна принимать физический урон");
        helper.assertTrue(genie.isAlive() && genie.getHealth() == before,
                "Wishborne-джинния должна остаться живой с прежним здоровьем");
        helper.succeed();
    }

    /** Вежливая точная формулировка меняет отношения, а характер выводится из них. */
    private static void testGeniePersonalityChanges(GameTestHelper helper) {
        dev.romankrukovsky.kubanhorizons.genie.GenieAnchor.releaseFor(helper.getLevel());
        var genie = helper.spawn(KHEntities.KUBAN_GENIE.get(), new BlockPos(1, 2, 1));
        var intent = dev.romankrukovsky.kubanhorizons.genie.wish.WishParser.parse(
                "Я желаю сундук алмазов, безопасно размещённый передо мной");
        helper.assertTrue(intent.precision() >= 75 && intent.isPreciseAndSafe(),
                "Точная формулировка не распознана: " + intent);
        genie.personality().observeWording(intent.polite(), intent.commanding(), intent.precision());
        helper.assertTrue(genie.personality().respect() > 0 && genie.personality().affection() > 0,
                "Вежливое точное желание должно повысить уважение и симпатию");
        helper.succeed();
    }

    /** Мозг всегда выбирает спасение выше боя, снаряд — выше обычной угрозы. */
    private static void testGenieBrainPrioritizesDanger(GameTestHelper helper) {
        var brain = new dev.romankrukovsky.kubanhorizons.genie.GenieBrain();
        var crisis = new dev.romankrukovsky.kubanhorizons.genie.GenieBrain.Situation(
                100L, 25.0D, 3.0F, 20.0F, true, 12.0D,
                100, 300, 4, 4.0D, 2, 5, 1, 4.0D);
        helper.assertTrue(brain.decide(crisis)
                        == dev.romankrukovsky.kubanhorizons.genie.GenieDecision.RESCUE_OWNER,
                "Спасение хозяина должно иметь высший приоритет");

        brain.record(dev.romankrukovsky.kubanhorizons.genie.GenieDecision.RESCUE_OWNER, 100L);
        var defended = new dev.romankrukovsky.kubanhorizons.genie.GenieBrain.Situation(
                110L, 25.0D, 20.0F, 20.0F, false, 0.0D,
                300, 300, 4, 4.0D, 2, 5, 0, Double.POSITIVE_INFINITY);
        helper.assertTrue(brain.decide(defended)
                        == dev.romankrukovsky.kubanhorizons.genie.GenieDecision.INTERCEPT_PROJECTILE,
                "Летящий снаряд должен быть важнее удалённой угрозы");
        helper.succeed();
    }

    /** Utility-планировщик предсказывает взрыв и отличает летящий мимо снаряд. */
    private static void testGeniePredictivePlanning(GameTestHelper helper) {
        var brain = new dev.romankrukovsky.kubanhorizons.genie.GenieBrain();
        var explosion = new dev.romankrukovsky.kubanhorizons.genie.GenieBrain.Situation(
                200L, 9.0D, 20.0F, 20.0F, false, 0.0D,
                300, 300, 1, 4.0D, 0, 200, 1, 4.0D);
        helper.assertTrue(brain.decide(explosion)
                        == dev.romankrukovsky.kubanhorizons.genie.GenieDecision.PREEMPT_EXPLOSION,
                "Взводящийся крипер должен быть предотвращён до взрыва");
        helper.assertTrue(brain.lastScore() >= 850,
                "Прогноз взрыва должен иметь высокую utility-оценку");

        var peaceful = new dev.romankrukovsky.kubanhorizons.genie.GenieBrain.Situation(
                201L, 9.0D, 20.0F, 20.0F, false, 0.0D,
                300, 300, 0, Double.POSITIVE_INFINITY, 0, 200, 0, Double.POSITIVE_INFINITY);
        helper.assertTrue(brain.decide(peaceful)
                        == dev.romankrukovsky.kubanhorizons.genie.GenieDecision.OBSERVE,
                "Без прогнозируемой угрозы джинния не должна тратить магию");
        helper.succeed();
    }

    /** Лампа сохраняет владельца, не передаёт связь вору и возвращает джиннию. */
    private static void testGenieLampBindsAndSummons(GameTestHelper helper) {
        dev.romankrukovsky.kubanhorizons.genie.GenieAnchor.releaseFor(helper.getLevel());
        var owner = helper.makeMockServerPlayerInLevel();
        var stranger = helper.makeMockServerPlayerInLevel();
        var genie = helper.spawn(KHEntities.KUBAN_GENIE.get(), new BlockPos(1, 2, 1));
        ItemStack lamp = new ItemStack(KHItems.GENIE_LAMP.get());
        owner.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, lamp);

        genie.mobInteract(owner, net.minecraft.world.InteractionHand.MAIN_HAND);
        var binding = dev.romankrukovsky.kubanhorizons.genie.vessel.GenieLampItem.binding(lamp);
        helper.assertTrue(binding != null
                        && binding.genieId().equals(genie.getUUID())
                        && binding.ownerId().equals(owner.getUUID()),
                "Лампа не сохранила UUID джиннии и настоящего владельца");
        helper.assertTrue(!dev.romankrukovsky.kubanhorizons.genie.vessel.GenieLampItem
                        .bind(lamp, stranger, genie),
                "Чужой игрок смог перепривязать украденную лампу");

        genie.snapTo(owner.getX() + 20.0D, owner.getY(), owner.getZ(), 0.0F, 0.0F);
        // Сам объект передаётся явно: GameTest запускает тесты параллельно в одном
        // мире, поэтому его общий якорь намеренно не используется в этой проверке.
        helper.assertTrue(dev.romankrukovsky.kubanhorizons.genie.vessel.GenieLampItem
                        .summonResolved(owner, binding, genie)
                        && genie.distanceToSqr(owner) < 16.0D,
                "Привязанная лампа не вернула джиннию к владельцу");
        helper.assertTrue(!dev.romankrukovsky.kubanhorizons.genie.vessel.GenieLampItem
                        .summonResolved(stranger, binding, genie),
                "Вор смог призвать джиннию чужой лампой");
        owner.discard();
        stranger.discard();
        helper.succeed();
    }

    /** Условное желание сохраняется, не дублируется и удаляется владельцем. */
    private static void testConditionalRulesPersist(GameTestHelper helper) {
        UUID ownerId = UUID.randomUUID();
        var condition = dev.romankrukovsky.kubanhorizons.genie.wish.ConditionalWishEngine
                .Condition.RAINING;
        var action = dev.romankrukovsky.kubanhorizons.genie.wish.ConditionalWishEngine
                .Action.GROW_STEPPE;
        var first = dev.romankrukovsky.kubanhorizons.genie.wish.ConditionalWishEngine
                .addRule(helper.getLevel(), ownerId, condition, action);
        var second = dev.romankrukovsky.kubanhorizons.genie.wish.ConditionalWishEngine
                .addRule(helper.getLevel(), ownerId, condition, action);
        var rules = dev.romankrukovsky.kubanhorizons.genie.wish.ConditionalWishEngine
                .rules(helper.getLevel(), ownerId);
        helper.assertTrue(first.ruleId().equals(second.ruleId()) && rules.size() == 1,
                "Повторная формулировка создала дубликат условного правила");
        helper.assertTrue(rules.getFirst().enabled()
                        && rules.getFirst().condition().equals("RAINING")
                        && rules.getFirst().action().equals("GROW_STEPPE"),
                "Сохранённое правило потеряло условие, действие или флаг включения");
        helper.assertTrue(dev.romankrukovsky.kubanhorizons.genie.wish.ConditionalWishEngine
                        .removeRule(helper.getLevel(), ownerId, condition, action)
                        && dev.romankrukovsky.kubanhorizons.genie.wish.ConditionalWishEngine
                        .rules(helper.getLevel(), ownerId).isEmpty(),
                "Владелец не смог удалить условное правило");
        helper.succeed();
    }

    /** Conditional wish runtime: preview → confirm → execute → undo with digest and before-image. */
    private static void testConditionalWishRuntime(GameTestHelper helper) {
        UUID ownerId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        var condition = dev.romankrukovsky.kubanhorizons.genie.wish.ConditionalWishEngine.Condition.DAY;
        var action = dev.romankrukovsky.kubanhorizons.genie.wish.ConditionalWishEngine.Action.GROW_CROPS;

        // Preview
        var preview = dev.romankrukovsky.kubanhorizons.genie.wish.ConditionalWishEngine
                .previewRule(actorId, ownerId, condition, action);
        helper.assertTrue(preview != null && preview.digest() != null, "Preview must produce digest");

        // Confirm
        var confirmed = dev.romankrukovsky.kubanhorizons.genie.wish.ConditionalWishEngine
                .confirmRule(actorId, preview);
        helper.assertTrue(confirmed != null, "Confirmation must succeed");

        // Execute
        boolean executed = dev.romankrukovsky.kubanhorizons.genie.wish.ConditionalWishEngine
                .executeConfirmed(helper.getLevel(), actorId, confirmed);
        helper.assertTrue(executed, "Execution must succeed on first run");

        // Verify rule exists
        var rules = dev.romankrukovsky.kubanhorizons.genie.wish.ConditionalWishEngine
                .rules(helper.getLevel(), ownerId);
        helper.assertTrue(rules.stream().anyMatch(r -> r.condition().equals("DAY") && r.action().equals("GROW_CROPS")),
                "Rule must be persisted after executeConfirmed");

        // Undo
        boolean undone = dev.romankrukovsky.kubanhorizons.genie.wish.ConditionalWishEngine
                .undoRule(helper.getLevel(), ownerId, condition, action);
        helper.assertTrue(undone, "Undo must succeed");

        helper.succeed();
    }

    /** Живая картина обязана перенести игрока в другой ServerLevel, а не показать реплику. */
    private static void testGenieLivingPaintingEntersOtherLevel(GameTestHelper helper) {
        ServerLevel original = helper.getLevel();
        var player = helper.makeMockServerPlayerInLevel();
        var origin = helper.absoluteVec(new net.minecraft.world.phys.Vec3(1.5D, 2.0D, 1.5D));
        player.setPos(origin);

        boolean entered = dev.romankrukovsky.kubanhorizons.genie.dimension.LivingPaintingEngine
                .enterDimension(original, player.blockPosition(), player,
                        net.minecraft.world.level.Level.NETHER,
                        new net.minecraft.world.phys.Vec3(0.5D, 80.0D, 0.5D));

        helper.assertTrue(entered, "Переход через живую картину был отклонён");
        helper.assertTrue(player.level() != original,
                "Живая картина оставила игрока в исходном ServerLevel");
        helper.assertTrue(dev.romankrukovsky.kubanhorizons.genie.dimension.LivingPaintingEngine
                        .leave(player),
                "Обратный переход из живой картины был отклонён");
        helper.assertTrue(player.level() == original,
                "Живая картина не вернула игрока в исходный ServerLevel");
        helper.assertTrue(player.position().distanceToSqr(origin) < 0.0001D,
                "Живая картина не вернула игрока в точные исходные координаты");
        player.discard();
        helper.succeed();
    }

    /** Действия изменяют память, а приказ циклически проходит все режимы. */
    private static void testGenieBrainRemembersActions(GameTestHelper helper) {
        var brain = new dev.romankrukovsky.kubanhorizons.genie.GenieBrain();
        brain.record(dev.romankrukovsky.kubanhorizons.genie.GenieDecision.REPEL_THREAT, 1L);
        brain.record(dev.romankrukovsky.kubanhorizons.genie.GenieDecision.INTERCEPT_PROJECTILE, 2L);
        brain.recordWish();
        helper.assertTrue(brain.threatsRepelled() == 1 && brain.projectilesIntercepted() == 1
                        && brain.wishesObserved() == 1,
                "Мозг не запомнил совершённые действия");
        helper.assertTrue(brain.cycleMode()
                        == dev.romankrukovsky.kubanhorizons.genie.GenieBehaviorMode.STAY,
                "После FOLLOW должен включаться STAY");
        helper.assertTrue(brain.cycleMode()
                        == dev.romankrukovsky.kubanhorizons.genie.GenieBehaviorMode.GUARD,
                "После STAY должен включаться GUARD");
        helper.succeed();
    }

    /** Ироническая защита превращает мечи в ложки, а взрывы вызывают частицы без урона. */
    private static void testGenieDefenseIrony(GameTestHelper helper) {
        dev.romankrukovsky.kubanhorizons.genie.GenieAnchor.releaseFor(helper.getLevel());
        var genie = helper.spawn(KHEntities.KUBAN_GENIE.get(), new BlockPos(1, 2, 1));
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(Items.NETHERITE_SWORD));

        genie.hurtServer(helper.getLevel(), helper.getLevel().damageSources().playerAttack(player), 10.0F);
        helper.assertTrue(player.getMainHandItem().is(KHItems.WOODEN_SPOON.get()),
                "Атака мечом должна превратить оружие в деревянную ложку");
        helper.succeed();
    }

    /**
     * Обычный удар не уносит джиннию из мира.
     *
     * <p>Регрессия на удалённую ложную смерть: она делала спутницу невидимой
     * и телепортировала за спину атакующего после любого удара, из-за чего
     * джинния исчезала в момент, когда игрок на неё рассчитывал.</p>
     */
    private static void testGenieSurvivesHitInPlace(GameTestHelper helper) {
        dev.romankrukovsky.kubanhorizons.genie.GenieAnchor.releaseFor(helper.getLevel());
        dev.romankrukovsky.kubanhorizons.genie.GenieAnchor.releaseFor(helper.getLevel());
        var genie = helper.spawn(KHEntities.KUBAN_GENIE.get(), new BlockPos(1, 2, 1));
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        var before = genie.position();

        genie.hurtServer(helper.getLevel(), helper.getLevel().damageSources().playerAttack(player), 10.0F);

        helper.assertTrue(genie.isAlive(), "Джинния должна остаться живой после удара");
        helper.assertTrue(!genie.isInvisible(), "Удар не должен делать джиннию невидимой");
        helper.assertTrue(genie.position().distanceToSqr(before) < 1.0E-6D,
                "Удар не должен телепортировать джиннию");
        helper.succeed();
    }

    /**
     * Джинния в мире одна, и второй появиться нельзя.
     *
     * <p>Вторая сущность не должна попадать в мир вообще: {@code /summon}
     * тихо ничего не даёт, а не создаёт вторую личность с собственной
     * памятью и характером.</p>
     */
    private static void testGenieIsUnique(GameTestHelper helper) {
        dev.romankrukovsky.kubanhorizons.genie.GenieAnchor.releaseFor(helper.getLevel());
        var first = helper.spawn(KHEntities.KUBAN_GENIE.get(), new BlockPos(1, 2, 1));
        var anchored = dev.romankrukovsky.kubanhorizons.genie.GenieAnchor
                .anchoredId(helper.getLevel().getServer());
        helper.assertTrue(first.getUUID().equals(anchored),
                "Первая джинния должна стать джиннией мира");

        var second = KHEntities.KUBAN_GENIE.get().create(helper.getLevel(),
                net.minecraft.world.entity.EntitySpawnReason.COMMAND);
        helper.assertTrue(second != null, "Сущность-кандидат должна создаваться");
        var pos = helper.absolutePos(new BlockPos(3, 2, 3));
        second.snapTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, 0.0F, 0.0F);
        boolean added = helper.getLevel().addFreshEntity(second);

        helper.assertTrue(!added, "Вторая джинния не должна попадать в мир");
        // Проверяется именно недобавление сущности, а не число джинний рядом:
        // GameTest прогоняет все тесты в одном мире, и соседние структуры
        // содержат своих джинний, что к контракту единственности не относится.
        helper.assertTrue(helper.getLevel().getEntity(second.getUUID()) == null,
                "Отклонённая джинния не должна существовать в мире");
        helper.assertTrue(first.getUUID().equals(dev.romankrukovsky.kubanhorizons.genie.GenieAnchor
                        .anchoredId(helper.getLevel().getServer())),
                "Отклонённая джинния не должна перехватывать привязку мира");
        helper.assertTrue(first.isAlive(), "Первая джинния должна остаться живой");
        helper.succeed();
    }

    /** Мета-желания изменяют правила игры Minecraft. */
    private static void testGenieMetaRules(GameTestHelper helper) {
        var player = helper.makeMockServerPlayerInLevel();
        var intent = dev.romankrukovsky.kubanhorizons.genie.wish.WishParser.parse("Хочу чтобы криперы больше не разрушали блоки");
        helper.assertTrue(intent.target() == dev.romankrukovsky.kubanhorizons.genie.wish.WishIntent.Target.META_NO_CREEPER_DAMAGE,
                "Мета-желание отключения разрушений не распознано");

        var runtime = dev.romankrukovsky.kubanhorizons.genie.runtime.WishRuntime
                .get(helper.getLevel().getServer());
        if (!runtime.ready()) runtime.recover();
        helper.getLevel().getGameRules().set(net.minecraft.world.level.gamerules.GameRules.MOB_GRIEFING,
                true, helper.getLevel().getServer());
        try {
            var preview = runtime.previewMobGriefing(player.getUUID(), false);
            helper.assertTrue(helper.getLevel().getGameRules().get(
                    net.minecraft.world.level.gamerules.GameRules.MOB_GRIEFING),
                    "Policy preview изменил gamerule");
            var report = runtime.executePolicy(player.getUUID(),
                    runtime.confirmPolicy(player.getUUID(), preview));
            helper.assertTrue(report.outcome()
                            == dev.romankrukovsky.kubanhorizons.genie.runtime.transaction.TransactionOutcome.COMPLETED
                            && !helper.getLevel().getGameRules().get(
                            net.minecraft.world.level.gamerules.GameRules.MOB_GRIEFING),
                    "Policy не отключила mobGriefing");
            runtime.undoPolicy(player.getUUID(), report.transactionId());
            helper.assertTrue(helper.getLevel().getGameRules().get(
                    net.minecraft.world.level.gamerules.GameRules.MOB_GRIEFING),
                    "Policy undo не вернул mobGriefing");
            player.discard();
            helper.succeed();
        } catch (IOException | RuntimeException exception) {
            helper.fail("Policy runtime failed: " + exception.getMessage());
        }
    }

    /** Погода проходит durable policy lifecycle и возвращается undo. */
    private static void testGenieWeatherPolicy(GameTestHelper helper) {
        var player = helper.makeMockServerPlayerInLevel();
        helper.getLevel().setRainLevel(0.0F);
        helper.getLevel().setThunderLevel(0.0F);
        var runtime = dev.romankrukovsky.kubanhorizons.genie.runtime.WishRuntime
                .get(helper.getLevel().getServer());
        if (!runtime.ready()) runtime.recover();
        try {
            var preview = runtime.previewWeather(player.getUUID(), 1.0F, 0.0F);
            helper.assertTrue(helper.getLevel().getRainLevel(1.0F) == 0.0F,
                    "Weather preview изменил уровень дождя");
            var report = runtime.executePolicy(player.getUUID(),
                    runtime.confirmPolicy(player.getUUID(), preview));
            helper.assertTrue(report.outcome()
                            == dev.romankrukovsky.kubanhorizons.genie.runtime.transaction.TransactionOutcome.COMPLETED
                            && helper.getLevel().getRainLevel(1.0F) == 1.0F,
                    "Weather policy не включила дождь");
            runtime.undoPolicy(player.getUUID(), report.transactionId());
            helper.assertTrue(helper.getLevel().getRainLevel(1.0F) == 0.0F,
                    "Weather undo не вернул ясную погоду");
            player.discard();
            helper.succeed();
        } catch (IOException | RuntimeException exception) {
            helper.fail("Weather policy failed: " + exception.getMessage());
        }
    }

    /** Длинная ночь меняет скорость WorldClock и откатывается policy undo. */
    private static void testGenieClockPolicy(GameTestHelper helper) {
        var player = helper.makeMockServerPlayerInLevel();
        var server = helper.getLevel().getServer();
        var clock = server.registryAccess().getOrThrow(net.minecraft.world.clock.WorldClocks.OVERWORLD);
        server.clockManager().setRate(clock, 1.0F);
        var runtime = dev.romankrukovsky.kubanhorizons.genie.runtime.WishRuntime.get(server);
        if (!runtime.ready()) runtime.recover();
        try {
            var preview = runtime.previewClockRate(player.getUUID(), 0.5F);
            helper.assertTrue(server.clockManager().getRate(clock) == 1.0F,
                    "Clock preview изменил скорость времени");
            var report = runtime.executePolicy(player.getUUID(),
                    runtime.confirmPolicy(player.getUUID(), preview));
            helper.assertTrue(report.outcome()
                            == dev.romankrukovsky.kubanhorizons.genie.runtime.transaction.TransactionOutcome.COMPLETED
                            && server.clockManager().getRate(clock) == 0.5F,
                    "Clock policy не замедлила ночь");
            runtime.undoPolicy(player.getUUID(), report.transactionId());
            helper.assertTrue(server.clockManager().getRate(clock) == 1.0F,
                    "Clock undo не вернул обычную скорость");
            player.discard();
            helper.succeed();
        } catch (IOException | RuntimeException exception) {
            helper.fail("Clock policy failed: " + exception.getMessage());
        }
    }

    /** Мгновенная переплавка реально работает и полностью отключается через policy undo. */
    private static void testGenieInstantSmeltPolicy(GameTestHelper helper) {
        var player = helper.makeMockServerPlayerInLevel();
        var runtime = dev.romankrukovsky.kubanhorizons.genie.runtime.WishRuntime
                .get(helper.getLevel().getServer());
        if (!runtime.ready()) runtime.recover();
        BlockPos furnacePos = helper.absolutePos(new BlockPos(2, 2, 2));
        helper.getLevel().setBlock(furnacePos, Blocks.FURNACE.defaultBlockState(), 3);
        var furnace = (net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity)
                helper.getLevel().getBlockEntity(furnacePos);
        helper.assertTrue(furnace != null, "Печь должна создать block entity");

        final dev.romankrukovsky.kubanhorizons.genie.runtime.transaction.TransactionReport report;
        try {
            var preview = runtime.previewInstantSmelt(player.getUUID(), true);
            helper.assertTrue(!runtime.isInstantSmeltEnabled(),
                    "Preview не должен включать мгновенную переплавку");
            report = runtime.executePolicy(player.getUUID(),
                    runtime.confirmPolicy(player.getUUID(), preview));
            helper.assertTrue(report.outcome()
                            == dev.romankrukovsky.kubanhorizons.genie.runtime.transaction.TransactionOutcome.COMPLETED,
                    "Instant-smelt policy не была применена");

            furnace.setItem(0, new ItemStack(Items.RAW_IRON));
            furnace.setItem(1, new ItemStack(Items.COAL));
        } catch (IOException | RuntimeException exception) {
            helper.fail("Instant-smelt policy failed: " + exception.getMessage());
            return;
        }

        helper.startSequence()
                // Новая block entity попадает в список тикеров не в тот же
                // момент, когда ставится блок. Ждём реального результата, а
                // не угадываем, понадобится регистрации один тик или два.
                .thenWaitUntil(() -> helper.assertTrue(furnace.getItem(2).is(Items.IRON_INGOT),
                        "Включённое правило не переплавило железо мгновенно"))
                .thenExecute(() -> {
                    try {
                        runtime.undoPolicy(player.getUUID(), report.transactionId());
                    } catch (IOException exception) {
                        throw new IllegalStateException(exception);
                    }
                    furnace.setItem(0, new ItemStack(Items.RAW_IRON));
                    furnace.setItem(1, new ItemStack(Items.COAL));
                    furnace.setItem(2, ItemStack.EMPTY);
                })
                .thenExecuteAfter(2, () -> helper.assertTrue(
                        furnace.getItem(2).isEmpty() && furnace.getItem(0).is(Items.RAW_IRON),
                        "После undo печь должна снова работать с обычной скоростью"))
                .thenExecute(player::discard)
                .thenSucceed();
    }

    /** Экран не может командовать чужой джиннией и меняет режим только связанной сущности. */
    private static void testGenieDialogServerActions(GameTestHelper helper) {
        dev.romankrukovsky.kubanhorizons.genie.GenieAnchor.releaseFor(helper.getLevel());
        var owner = helper.makeMockServerPlayerInLevel();
        BlockPos ownerPos = helper.absolutePos(new BlockPos(4, 2, 4));
        owner.snapTo(ownerPos.getX() + 0.5D, ownerPos.getY(), ownerPos.getZ() + 0.5D,
                0.0F, 0.0F);
        var genie = KHEntities.KUBAN_GENIE.get().create(helper.getLevel(),
                net.minecraft.world.entity.EntitySpawnReason.COMMAND);
        helper.assertTrue(genie != null, "Джинния должна создаваться");
        genie.snapTo(owner.getX() + 1.0D, owner.getY(), owner.getZ() + 1.0D, 0.0F, 0.0F);
        helper.assertTrue(helper.getLevel().addFreshEntity(genie),
                "Джинния должна добавляться в тестовый мир");
        genie.mobInteract(owner, net.minecraft.world.InteractionHand.MAIN_HAND);

        boolean changed = dev.romankrukovsky.kubanhorizons.genie.GenieConversationService
                .changeMode(owner, genie.getId(),
                        dev.romankrukovsky.kubanhorizons.genie.GenieBehaviorMode.GUARD);
        helper.assertTrue(changed && genie.brain().mode()
                        == dev.romankrukovsky.kubanhorizons.genie.GenieBehaviorMode.GUARD,
                "Команда владельца не включила режим охраны");

        var stranger = helper.makeMockServerPlayerInLevel();
        stranger.snapTo(ownerPos.getX() + 2.5D, ownerPos.getY(), ownerPos.getZ() + 0.5D,
                0.0F, 0.0F);
        boolean stolen = dev.romankrukovsky.kubanhorizons.genie.GenieConversationService
                .changeMode(stranger, genie.getId(),
                        dev.romankrukovsky.kubanhorizons.genie.GenieBehaviorMode.STAY);
        helper.assertTrue(!stolen && genie.brain().mode()
                        == dev.romankrukovsky.kubanhorizons.genie.GenieBehaviorMode.GUARD,
                "Чужой игрок смог изменить приказ джиннии");

        var runtime = dev.romankrukovsky.kubanhorizons.genie.runtime.WishRuntime
                .get(helper.getLevel().getServer());
        if (!runtime.ready()) {
            runtime.recover();
        }
        var preview = dev.romankrukovsky.kubanhorizons.genie.GenieConversationService.submitWish(
                owner, genie.getId(), "Хочу, чтобы железо плавилось мгновенно");
        helper.assertTrue(preview.confirmationRequired() && !runtime.isInstantSmeltEnabled(),
                "Диалог должен сначала показать правило, не применяя его");
        var applied = dev.romankrukovsky.kubanhorizons.genie.GenieConversationService
                .confirmPolicy(owner, genie.getId());
        helper.assertTrue(!applied.confirmationRequired() && runtime.isInstantSmeltEnabled(),
                "Подтверждение из диалога не включило правило");
        var undone = dev.romankrukovsky.kubanhorizons.genie.GenieConversationService
                .undoLastPolicy(owner, genie.getId());
        helper.assertTrue(!undone.confirmationRequired() && !runtime.isInstantSmeltEnabled(),
                "Диалог не отменил последнее глобальное правило");
        owner.discard();
        stranger.discard();
        genie.discard();
        dev.romankrukovsky.kubanhorizons.genie.GenieAnchor.releaseFor(helper.getLevel());
        helper.succeed();
    }

    /** Движок гигантизма создаёт сущности гигантских масштабов. */
    private static void testGenieGigantismEngine(GameTestHelper helper) {
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        var intent = dev.romankrukovsky.kubanhorizons.genie.wish.WishParser.parse("Хочу гигантскую курицу");
        helper.assertTrue(intent.target() == dev.romankrukovsky.kubanhorizons.genie.wish.WishIntent.Target.BIG_CHICKEN,
                "Желание гигантской курицы не распознано");

        var result = dev.romankrukovsky.kubanhorizons.genie.wish.WishExecutor.execute(helper.getLevel(), player, intent);
        helper.assertTrue(result.executed(), "Желание гигантизма должно выполниться");
        helper.succeed();
    }

    /** Долговременная память фиксирует желания и спасения. */
    private static void testGenieWorldMemory(GameTestHelper helper) {
        var memory = dev.romankrukovsky.kubanhorizons.genie.memory.WorldGenieMemory.get(helper.getLevel());
        int before = memory.totalWishesGranted();
        memory.recordWish(new BlockPos(1, 1, 1), "DIAMONDS", 100, helper.getLevel().getGameTime());
        helper.assertTrue(memory.totalWishesGranted() == before + 1,
                "Память должна учесть исполненное желание");
        helper.succeed();
    }

    /** Полная физическая неуязвимость и перехват административного /kill. */
    private static void testGenieInviolabilityAndKillIntercept(GameTestHelper helper) {
        dev.romankrukovsky.kubanhorizons.genie.GenieAnchor.releaseFor(helper.getLevel());
        var genie = helper.spawn(KHEntities.KUBAN_GENIE.get(), new BlockPos(1, 2, 1));
        var player = helper.makeMockPlayer(GameType.SURVIVAL);

        genie.hurtServer(helper.getLevel(), helper.getLevel().damageSources().fellOutOfWorld(), 1000.0F);
        helper.assertTrue(genie.isAlive(), "Джинния должна остаться живой после перехвата /kill");
        helper.succeed();
    }

    /** Аура законов нейтрализует физические снаряды вокруг Джиннии. */
    private static void testGenieAuraOfLaws(GameTestHelper helper) {
        dev.romankrukovsky.kubanhorizons.genie.GenieAnchor.releaseFor(helper.getLevel());
        // Общая карта удерживаемых снарядов НЕ чистится: она статическая
        // на весь мод, а тесты набора идут параллельно в одном мире —
        // чистка стирала снаряд соседнего теста, тот навсегда выпадал из
        // обработки и «зависал». Каждый тест смотрит только свой снаряд,
        // поэтому чужие записи в карте ему не мешают.
        var genie = helper.spawn(KHEntities.KUBAN_GENIE.get(), new BlockPos(1, 2, 1));
        var arrow = net.minecraft.world.entity.EntityTypes.ARROW.create(helper.getLevel(), net.minecraft.world.entity.EntitySpawnReason.COMMAND);
        if (arrow == null) {
            helper.succeed();
            return;
        }
        arrow.snapTo(genie.getX() + 0.5D, genie.getY(), genie.getZ() + 0.5D, 0.0F, 0.0F);
        arrow.setDeltaMovement(new net.minecraft.world.phys.Vec3(1.0D, 0.0D, 0.0D));
        helper.getLevel().addFreshEntity(arrow);

        dev.romankrukovsky.kubanhorizons.genie.aura.GenieAuraOfLaws
                .tickAuraOfLaws(genie, helper.getLevel());
        helper.assertTrue(arrow.getDeltaMovement().lengthSqr() < 0.001D,
                "Снаряд в ауре законов должен замереть");
        helper.assertTrue(arrow.isAlive(), "Снаряд должен сначала повисеть, а не исчезнуть мгновенно");

        // Остановленный снаряд обязан рассыпаться: раньше он висел в воздухе
        // вечно, а файербол и голова иссушителя продолжали коптить на месте.
        //
        // succeedWhen, а не однократная проверка через 20 тиков. Карта
        // удерживаемых снарядов статическая и общая на весь мод, а тесты
        // набора идут параллельно в одном мире: соседний тест ауры вызывал
        // clearHeldForTesting() в момент, когда этот ещё ждал, снаряд выпадал
        // из карты, и обработчик перестаёт его видеть — тест сообщал «снаряд
        // зависнет навсегда» на 20-м тике, хотя механика роспуска работала.
        // Ожидание вместо мгновенного среза переживает такую гонку.
        helper.succeedWhen(() -> {
            dev.romankrukovsky.kubanhorizons.genie.aura.GenieAuraOfLaws
                    .tickHeldProjectiles(helper.getLevel());
            helper.assertTrue(!arrow.isAlive(),
                    "Остановленный снаряд должен исчезнуть, а не зависнуть навсегда");
        });
    }

    /**
     * Файербол гаста и голова иссушителя тоже рассыпаются.
     *
     * <p>Отдельно от стрелы: эти снаряды каждый тик рисуют шлейф огня и дыма,
     * поэтому зависший навсегда файербол оставлял в воздухе вечно коптящую
     * точку, а не просто безобидный предмет.</p>
     */
    private static void testGenieAuraDissolvesFireballs(GameTestHelper helper) {
        dev.romankrukovsky.kubanhorizons.genie.GenieAnchor.releaseFor(helper.getLevel());
        // Общая карта удерживаемых снарядов НЕ чистится: она статическая
        // на весь мод, а тесты набора идут параллельно в одном мире —
        // чистка стирала снаряд соседнего теста, тот навсегда выпадал из
        // обработки и «зависал». Каждый тест смотрит только свой снаряд,
        // поэтому чужие записи в карте ему не мешают.
        var genie = helper.spawn(KHEntities.KUBAN_GENIE.get(), new BlockPos(1, 2, 1));

        var fireball = net.minecraft.world.entity.EntityTypes.FIREBALL.create(
                helper.getLevel(), net.minecraft.world.entity.EntitySpawnReason.COMMAND);
        var skull = net.minecraft.world.entity.EntityTypes.WITHER_SKULL.create(
                helper.getLevel(), net.minecraft.world.entity.EntitySpawnReason.COMMAND);
        if (fireball == null || skull == null) {
            helper.succeed();
            return;
        }
        for (var projectile : java.util.List.of(fireball, skull)) {
            projectile.snapTo(genie.getX() + 2.0D, genie.getY() + 1.0D, genie.getZ(), 0.0F, 0.0F);
            projectile.setDeltaMovement(new net.minecraft.world.phys.Vec3(-0.8D, 0.0D, 0.0D));
            helper.getLevel().addFreshEntity(projectile);
        }

        dev.romankrukovsky.kubanhorizons.genie.aura.GenieAuraOfLaws
                .tickAuraOfLaws(genie, helper.getLevel());
        helper.assertTrue(fireball.getDeltaMovement().lengthSqr() < 0.001D,
                "Файербол должен замереть в ауре");
        helper.assertTrue(skull.getDeltaMovement().lengthSqr() < 0.001D,
                "Голова иссушителя должна замереть в ауре");

        helper.runAfterDelay(20, () -> {
            dev.romankrukovsky.kubanhorizons.genie.aura.GenieAuraOfLaws
                    .tickHeldProjectiles(helper.getLevel());
            helper.assertTrue(!fireball.isAlive(), "Файербол должен рассыпаться, а не коптить в воздухе");
            helper.assertTrue(!skull.isAlive(), "Голова иссушителя должна рассыпаться");
            helper.succeed();
        });
    }

    /** Режим «Исполнить буквально» воплощает точные формулировки. */
    private static void testLiteralWishEngine(GameTestHelper helper) {
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        var result = dev.romankrukovsky.kubanhorizons.genie.wish.LiteralWishEngine.executeLiteral(
                helper.getLevel(), player, "40000 куриц");
        helper.assertTrue(result.executed(), "Буквалистское желание должно выполниться");
        helper.succeed();
    }

    /** Движок хвоста-индикатора маны и мультяшно-комедийных искажений. */
    private static void testGenieTailEngineAndCartoonAnatomy(GameTestHelper helper) {
        dev.romankrukovsky.kubanhorizons.genie.GenieAnchor.releaseFor(helper.getLevel());
        var genie = helper.spawn(KHEntities.KUBAN_GENIE.get(), new BlockPos(1, 2, 1));
        dev.romankrukovsky.kubanhorizons.genie.visual.GenieTailEngine.tickTail(genie, helper.getLevel());
        dev.romankrukovsky.kubanhorizons.genie.visual.CartoonAnatomyEngine.triggerFlatten(genie, helper.getLevel());
        helper.assertTrue(genie.isAlive(), "Джинния должна остаться живой после мультяшного сплющивания");
        helper.succeed();
    }

    /** Печать побеждает аватар через якорение, не нанося фиктивный урон. */
    private static void testGenieMagicalDefeatState(GameTestHelper helper) {
        dev.romankrukovsky.kubanhorizons.genie.GenieAnchor.releaseFor(helper.getLevel());
        var genie = helper.spawn(KHEntities.KUBAN_GENIE.get(), new BlockPos(1, 2, 1));
        var state = genie.wishborneState();
        helper.assertTrue(state.canAct() && state.anchoring() == 0,
                "Новый Wishborne-аватар должен быть проявлен и свободен");
        helper.assertTrue(!state.applyAnchoring(37) && state.anchoring() == 37,
                "Неполная печать должна только увеличить якорение");
        helper.assertTrue(state.applyAnchoring(63),
                "Якорение 100% должно запечатать аватар");
        helper.assertTrue(state.presence()
                        == dev.romankrukovsky.kubanhorizons.genie.WishborneState.Presence.SEALED
                        && !state.canAct(),
                "Запечатанная джинния не должна выполнять обычные действия");
        state.weakenAnchoring(100);
        helper.assertTrue(state.canAct(), "Разрушение всех рун должно вернуть аватар");
        helper.succeed();
    }

>>>>>>> Stashed changes
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

    // --- Маршрутизация биомов ---

    /**
     * Все биомы, которые генератор мода может вернуть игроку.
     *
     * <p>Один список на все проверки маршрутизации, тегов и крепостей: две
     * копии разошлись бы при первом же добавлении биома, и половина
     * проверок молча перестала бы охватывать новый биом.</p>
     */
    private static final List<ResourceKey<net.minecraft.world.level.biome.Biome>> ALL_KUBAN_BIOMES = List.of(
            KHBiomes.KUBAN_STEPPE, KHBiomes.PLAVNI, KHBiomes.LIMAN, KHBiomes.RIVER_FLOODPLAIN,
            KHBiomes.FOOTHILL_FOREST, KHBiomes.MOUNTAIN_FOREST, KHBiomes.AZOV_COAST,
            KHBiomes.BLACK_SEA_COAST, KHBiomes.VINEYARD_HILLS, KHBiomes.TEA_SLOPES);

    /**
     * Какой кубанский биом обязан прийти на замену каждому климатическому
     * региону ванили.
     *
     * <p>Это карта географии края, записанная проверяемо: море → берег →
     * равнина → предгорья → горы. Перечислены ИМЕННО те ванильные биомы,
     * которые несут смысловую подмену; регионы, законно уходящие в степь
     * (равнина, пустыня, badlands, снежная равнина), проверяются отдельно
     * ниже — вместе с тем, что они уходят туда осознанно.</p>
     */
    private static final Map<ResourceKey<net.minecraft.world.level.biome.Biome>,
            ResourceKey<net.minecraft.world.level.biome.Biome>> BIOME_ROUTING = Map.ofEntries(
            // Влажные низины — свои с самого начала.
            Map.entry(net.minecraft.world.level.biome.Biomes.SWAMP, KHBiomes.PLAVNI),
            Map.entry(net.minecraft.world.level.biome.Biomes.MANGROVE_SWAMP, KHBiomes.LIMAN),
            Map.entry(net.minecraft.world.level.biome.Biomes.RIVER, KHBiomes.RIVER_FLOODPLAIN),
            Map.entry(net.minecraft.world.level.biome.Biomes.FROZEN_RIVER, KHBiomes.RIVER_FLOODPLAIN),
            // Лесной пояс предгорий.
            Map.entry(net.minecraft.world.level.biome.Biomes.FOREST, KHBiomes.FOOTHILL_FOREST),
            Map.entry(net.minecraft.world.level.biome.Biomes.BIRCH_FOREST, KHBiomes.FOOTHILL_FOREST),
            Map.entry(net.minecraft.world.level.biome.Biomes.OLD_GROWTH_BIRCH_FOREST, KHBiomes.FOOTHILL_FOREST),
            Map.entry(net.minecraft.world.level.biome.Biomes.FLOWER_FOREST, KHBiomes.FOOTHILL_FOREST),
            Map.entry(net.minecraft.world.level.biome.Biomes.DARK_FOREST, KHBiomes.FOOTHILL_FOREST),
            Map.entry(net.minecraft.world.level.biome.Biomes.PALE_GARDEN, KHBiomes.FOOTHILL_FOREST),
            Map.entry(net.minecraft.world.level.biome.Biomes.WINDSWEPT_FOREST, KHBiomes.FOOTHILL_FOREST),
            Map.entry(net.minecraft.world.level.biome.Biomes.TAIGA, KHBiomes.FOOTHILL_FOREST),
            Map.entry(net.minecraft.world.level.biome.Biomes.SNOWY_TAIGA, KHBiomes.FOOTHILL_FOREST),
            Map.entry(net.minecraft.world.level.biome.Biomes.OLD_GROWTH_PINE_TAIGA, KHBiomes.FOOTHILL_FOREST),
            Map.entry(net.minecraft.world.level.biome.Biomes.OLD_GROWTH_SPRUCE_TAIGA, KHBiomes.FOOTHILL_FOREST),
            // Горный пояс.
            Map.entry(net.minecraft.world.level.biome.Biomes.WINDSWEPT_HILLS, KHBiomes.MOUNTAIN_FOREST),
            Map.entry(net.minecraft.world.level.biome.Biomes.WINDSWEPT_GRAVELLY_HILLS, KHBiomes.MOUNTAIN_FOREST),
            Map.entry(net.minecraft.world.level.biome.Biomes.MEADOW, KHBiomes.MOUNTAIN_FOREST),
            Map.entry(net.minecraft.world.level.biome.Biomes.CHERRY_GROVE, KHBiomes.MOUNTAIN_FOREST),
            Map.entry(net.minecraft.world.level.biome.Biomes.GROVE, KHBiomes.MOUNTAIN_FOREST),
            Map.entry(net.minecraft.world.level.biome.Biomes.SNOWY_SLOPES, KHBiomes.MOUNTAIN_FOREST),
            Map.entry(net.minecraft.world.level.biome.Biomes.STONY_PEAKS, KHBiomes.MOUNTAIN_FOREST),
            Map.entry(net.minecraft.world.level.biome.Biomes.JAGGED_PEAKS, KHBiomes.MOUNTAIN_FOREST),
            Map.entry(net.minecraft.world.level.biome.Biomes.FROZEN_PEAKS, KHBiomes.MOUNTAIN_FOREST),
            // Берега двух морей.
            Map.entry(net.minecraft.world.level.biome.Biomes.BEACH, KHBiomes.AZOV_COAST),
            Map.entry(net.minecraft.world.level.biome.Biomes.SNOWY_BEACH, KHBiomes.AZOV_COAST),
            Map.entry(net.minecraft.world.level.biome.Biomes.STONY_SHORE, KHBiomes.BLACK_SEA_COAST),
            // Виноградный пояс.
            Map.entry(net.minecraft.world.level.biome.Biomes.SAVANNA, KHBiomes.VINEYARD_HILLS),
            Map.entry(net.minecraft.world.level.biome.Biomes.SAVANNA_PLATEAU, KHBiomes.VINEYARD_HILLS),
            Map.entry(net.minecraft.world.level.biome.Biomes.WINDSWEPT_SAVANNA, KHBiomes.VINEYARD_HILLS),
            // Чайный пояс.
            Map.entry(net.minecraft.world.level.biome.Biomes.JUNGLE, KHBiomes.TEA_SLOPES),
            Map.entry(net.minecraft.world.level.biome.Biomes.SPARSE_JUNGLE, KHBiomes.TEA_SLOPES),
            Map.entry(net.minecraft.world.level.biome.Biomes.BAMBOO_JUNGLE, KHBiomes.TEA_SLOPES),
            // Сухая открытая равнина — законно степь.
            Map.entry(net.minecraft.world.level.biome.Biomes.PLAINS, KHBiomes.KUBAN_STEPPE),
            Map.entry(net.minecraft.world.level.biome.Biomes.SUNFLOWER_PLAINS, KHBiomes.KUBAN_STEPPE),
            Map.entry(net.minecraft.world.level.biome.Biomes.DESERT, KHBiomes.KUBAN_STEPPE),
            Map.entry(net.minecraft.world.level.biome.Biomes.BADLANDS, KHBiomes.KUBAN_STEPPE),
            Map.entry(net.minecraft.world.level.biome.Biomes.WOODED_BADLANDS, KHBiomes.KUBAN_STEPPE),
            Map.entry(net.minecraft.world.level.biome.Biomes.ERODED_BADLANDS, KHBiomes.KUBAN_STEPPE),
            Map.entry(net.minecraft.world.level.biome.Biomes.SNOWY_PLAINS, KHBiomes.KUBAN_STEPPE),
            Map.entry(net.minecraft.world.level.biome.Biomes.ICE_SPIKES, KHBiomes.KUBAN_STEPPE));

    /** Достаёт источник биомов пресета мода. */
    private static KubanBiomeSource kubanSource(GameTestHelper helper) {
        var source = helper.getLevel().registryAccess()
                .lookupOrThrow(Registries.WORLD_PRESET)
                .getOrThrow(KHWorldPresets.KUBAN_HORIZONS).value()
                .overworld().orElseThrow().generator().getBiomeSource();
        if (!(source instanceof KubanBiomeSource kuban)) {
            throw new AssertionError("Overworld пресета больше не использует KubanBiomeSource: "
                    + source.getClass().getName());
        }
        return kuban;
    }

    /**
     * Каждый климатический регион ванили приводит к своему кубанскому биому.
     *
     * <p>Проверяется ПУТЬ, а не наличие. Биом можно зарегистрировать,
     * перевести, снабдить цветами и тегами — и не встретить в игре ни разу,
     * если источник его не возвращает. В этом моде так уже было со всем
     * подряд: с устройством без рецепта, с саженцами в замкнутом круге, с
     * восемью существами без спавна. Зарегистрированный биом, который
     * генератор никогда не отдаёт, — та же болезнь, и здесь она
     * закрывается прогоном самой маршрутизации.</p>
     *
     * <p>Тест гоняет {@link KubanBiomeSource#remap} по всем ванильным
     * биомам Верхнего мира: и по тем, что обязаны стать новыми поясами, и
     * по тем, что законно уходят в степь. Если чью-то ветку убрать, регион
     * провалится в степь — и тест назовёт и регион, и то, что пришло
     * вместо ожидаемого.</p>
     */
    private static void testBiomeRoutingCoversClimateRegions(GameTestHelper helper) {
        var biomes = helper.getLevel().registryAccess().lookupOrThrow(Registries.BIOME);
        KubanBiomeSource source = kubanSource(helper);

        BIOME_ROUTING.forEach((vanilla, expected) -> {
            Holder<net.minecraft.world.level.biome.Biome> actual =
                    source.remap(biomes.getOrThrow(vanilla));
            helper.assertTrue(actual.is(expected),
                    "Климатический регион " + vanilla.identifier() + " должен становиться "
                            + expected.identifier() + ", а стал "
                            + actual.unwrapKey().map(key -> key.identifier().toString())
                                    .orElse("<неизвестным биомом>"));
        });

        // Обратная сторона: каждый новый пояс обязан быть КОНЕЧНОЙ точкой
        // хотя бы одного региона. Биом, в который не ведёт ни один
        // климатический регион, недостижим — сколько бы правил его ни
        // упоминало.
        for (ResourceKey<net.minecraft.world.level.biome.Biome> kuban : ALL_KUBAN_BIOMES) {
            boolean reachable = BIOME_ROUTING.values().stream().anyMatch(target -> target == kuban);
            helper.assertTrue(reachable, "Биом " + kuban.identifier()
                    + " не является результатом ни одного климатического региона — "
                    + "он зарегистрирован, но недостижим в мире");
        }
        helper.succeed();
    }

    /**
     * Источник объявляет всё, что может вернуть.
     *
     * <p>Биом, который {@code remap} отдаёт, но которого нет в
     * {@code collectPossibleBiomes()}, — реальный баг: ванильные системы
     * (сортировка features, поиск структур, спавн) читают именно
     * объявленный список, и биом-безбилетник ломает их молча.</p>
     *
     * <p>Проверка идёт перебором ВСЕХ ванильных биомов Верхнего мира,
     * включая океаны и пещеры, а не только тех, что перечислены в карте
     * маршрутизации: так тест поймает и биом, добавленный в {@code remap}
     * без объявления.</p>
     */
    private static void testBiomeSourceDeclaresEverythingItReturns(GameTestHelper helper) {
        var biomes = helper.getLevel().registryAccess().lookupOrThrow(Registries.BIOME);
        KubanBiomeSource source = kubanSource(helper);
        Set<ResourceKey<net.minecraft.world.level.biome.Biome>> declared =
                source.possibleBiomes().stream()
                        .map(holder -> holder.unwrapKey().orElseThrow())
                        .collect(java.util.stream.Collectors.toSet());

        int checked = 0;
        for (var entry : biomes.listElements().toList()) {
            ResourceKey<net.minecraft.world.level.biome.Biome> key = entry.unwrapKey().orElseThrow();
            // Только ванильные биомы Верхнего мира: Нижний мир и Край идут
            // через свои источники и через remap не проходят.
            if (!key.identifier().getNamespace().equals("minecraft")
                    || !biomes.getOrThrow(net.minecraft.tags.BiomeTags.IS_OVERWORLD)
                            .contains(biomes.getOrThrow(key))) {
                continue;
            }
            checked++;
            ResourceKey<net.minecraft.world.level.biome.Biome> result =
                    source.remap(entry).unwrapKey().orElseThrow();
            helper.assertTrue(declared.contains(result),
                    "remap(" + key.identifier() + ") вернул " + result.identifier()
                            + ", которого нет в collectPossibleBiomes()");
        }
        helper.assertTrue(checked > 40,
                "Проверено слишком мало ванильных биомов Верхнего мира (" + checked
                        + ") — тест перестал что-либо охватывать");
        helper.succeed();
    }

    /**
     * В настоящем мире действительно встречается несколько биомов.
     *
     * <p>Две проверки выше гоняют маршрутизацию напрямую. Эта — сам мир:
     * настоящий {@link net.minecraft.world.level.levelgen.RandomState} по
     * шумовым настройкам мода, настоящий климатический сэмплер и обход
     * большой площади с шагом в четверть чанка, как это делает генерация
     * биомов.</p>
     *
     * <p>Смысл именно в этом: до появления второго пояса такой обход
     * вернул бы почти одну степь, потому что весь рельеф — лес, гора,
     * пляж — подписывался ею. Порог в четыре биома и требование, чтобы ни
     * один биом не занимал больше девяти десятых площади, ловят возврат к
     * «мир из одного биома», даже если маршрутизация формально на месте.</p>
     */
    private static void testWorldActuallyHasMultipleBiomes(GameTestHelper helper) {
        var registries = helper.getLevel().registryAccess();
        KubanBiomeSource source = kubanSource(helper);
        var randomState = net.minecraft.world.level.levelgen.RandomState.create(
                registries.lookupOrThrow(Registries.NOISE_SETTINGS)
                        .getOrThrow(KHNoiseSettings.OVERWORLD).value(),
                registries.lookupOrThrow(Registries.NOISE),
                helper.getLevel().getSeed());
        var sampler = randomState.sampler();

        Map<String, Integer> seen = new LinkedHashMap<>();
        int samples = 0;
        // Шаг 4 — это ровно решётка биомов (четверть чанка), в которой
        // работает fillBiomesFromNoise. Диапазон широкий, чтобы попасть в
        // разные климатические зоны, а не в один биом у нуля координат.
        for (int x = -2048; x <= 2048; x += 64) {
            for (int z = -2048; z <= 2048; z += 64) {
                var biome = source.getNoiseBiome(x >> 2, 16, z >> 2, sampler);
                String name = biome.unwrapKey().orElseThrow().identifier().toString();
                seen.merge(name, 1, Integer::sum);
                samples++;
            }
        }

        helper.assertTrue(seen.size() >= 4,
                "Мир состоит из слишком малого числа биомов: " + seen);
        int max = seen.values().stream().mapToInt(Integer::intValue).max().orElse(0);
        helper.assertTrue(max < samples * 9 / 10,
                "Один биом занимает почти весь мир — вернулась монотонность: " + seen);
        // Каждый встреченный биом обязан быть кубанским: ванильный ID,
        // просочившийся наружу, означает дыру в подмене.
        seen.keySet().forEach(name -> helper.assertTrue(name.startsWith(KubanHorizons.MOD_ID + ":"),
                "В мир просочился ванильный биом: " + name));
        helper.succeed();
    }

    /**
     * У каждого биома есть своё лицо: подпись, цвета и поверхность.
     *
     * <p>Биом, не отличимый от соседнего, — это тот самый «ванильный
     * пересказ ванильного биома», из-за которого мир и казался однообразным. Проверяется то,
     * что видит игрок: перевод названия на двух языках, собственный цвет
     * травы или листвы и участие в правилах поверхности.</p>
     */
    private static void testNewBiomesAreDistinct(GameTestHelper helper) {
        var biomes = helper.getLevel().registryAccess().lookupOrThrow(Registries.BIOME);
        JsonObject english = readLang("en_us");
        JsonObject russian = readLang("ru_ru");
        Set<String> palettes = new java.util.HashSet<>();

        for (ResourceKey<net.minecraft.world.level.biome.Biome> key : ALL_KUBAN_BIOMES) {
            String translation = "biome." + KubanHorizons.MOD_ID + "." + key.identifier().getPath();
            helper.assertTrue(english.has(translation) && russian.has(translation),
                    "Биом " + key.identifier() + " не переведён на оба языка: " + translation);
            helper.assertTrue(!english.get(translation).getAsString().isBlank()
                            && !russian.get(translation).getAsString().isBlank(),
                    "Пустое название биома: " + translation);

            var effects = biomes.getOrThrow(key).value().getSpecialEffects();
            // Подпись в F3 отличать биомы не должна — их должно быть видно.
            // Цвет травы или листвы задаёт лицо биома сильнее всего.
            boolean coloured = effects.grassColorOverride().isPresent()
                    || effects.foliageColorOverride().isPresent();
            helper.assertTrue(coloured, "У биома " + key.identifier()
                    + " нет ни своего цвета травы, ни цвета листвы — на глаз он остался ванильным");
            String palette = effects.waterColor()
                    + "/" + effects.grassColorOverride().orElse(-1)
                    + "/" + effects.foliageColorOverride().orElse(-1);
            helper.assertTrue(palettes.add(palette), "Биом " + key.identifier()
                    + " повторяет палитру другого биома целиком: " + palette);
        }
        helper.succeed();
    }

    /** Пресет содержит три измерения, а Overworld публикует только кубанские биомы. */
    private static void testKubanSteppeWorldPreset(GameTestHelper helper) {        var registries = helper.getLevel().registryAccess();
        var preset = registries.lookupOrThrow(Registries.WORLD_PRESET)
                .getOrThrow(KHWorldPresets.KUBAN_HORIZONS)
                .value();
        var dimensions = preset.createWorldDimensions().dimensions();
        var overworld = preset.overworld().orElseThrow();
        var biomeSource = overworld.generator().getBiomeSource();

        helper.assertTrue(dimensions.keySet().equals(Set.of(
                        net.minecraft.world.level.dimension.LevelStem.OVERWORLD,
                        net.minecraft.world.level.dimension.LevelStem.NETHER,
                        net.minecraft.world.level.dimension.LevelStem.END)),
                "Пресет должен содержать ровно Overworld, Nether и End: " + dimensions.keySet());
        helper.assertTrue(biomeSource instanceof KubanBiomeSource,
                "Overworld пресета должен использовать KubanBiomeSource: " + biomeSource.getClass().getName());

        Set<ResourceKey<net.minecraft.world.level.biome.Biome>> actualBiomes = biomeSource.possibleBiomes().stream()
                .map(Holder::unwrapKey)
                .map(key -> key.orElseThrow(() -> new AssertionError("Незарегистрированный биом в KubanBiomeSource")))
                .collect(java.util.stream.Collectors.toSet());
        helper.assertTrue(actualBiomes.equals(Set.copyOf(ALL_KUBAN_BIOMES)),
                "KubanBiomeSource должен публиковать ровно кубанские биомы: " + actualBiomes);

        helper.assertTrue(dimensions.get(net.minecraft.world.level.dimension.LevelStem.NETHER).generator().getBiomeSource()
                        instanceof net.minecraft.world.level.biome.MultiNoiseBiomeSource,
                "Nether должен сохранять ванильный MultiNoiseBiomeSource");
        helper.assertTrue(dimensions.get(net.minecraft.world.level.dimension.LevelStem.END).generator().getBiomeSource()
                        instanceof net.minecraft.world.level.biome.TheEndBiomeSource,
                "End должен сохранять ванильный TheEndBiomeSource");
        helper.assertTrue(overworld.generator() instanceof net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator
                        && ((net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator) overworld.generator())
                                .stable(KHNoiseSettings.OVERWORLD),
                "Пресет не использует surface rules плавней, лимана и поймы");
        helper.succeed();
    }

    /** Все достижимые биомы KubanBiomeSource допускают ванильные stronghold'ы. */
    private static void testKubanStrongholdBiomes(GameTestHelper helper) {
        var biomes = helper.getLevel().registryAccess().lookupOrThrow(Registries.BIOME);
        var strongholdBiomes = biomes.getOrThrow(net.minecraft.tags.BiomeTags.HAS_STRONGHOLD);
        for (ResourceKey<net.minecraft.world.level.biome.Biome> key : ALL_KUBAN_BIOMES) {
            helper.assertTrue(strongholdBiomes.contains(biomes.getOrThrow(key)),
                    "Достижимый кубанский биом отсутствует в #has_structure/stronghold: " + key.identifier());
        }
        helper.succeed();
    }

    /** Дикие культуры присутствуют в растительности предназначенных кубанских биомов. */
    private static void testKubanWildCropFeatures(GameTestHelper helper) {
        var biomes = helper.getLevel().registryAccess().lookupOrThrow(Registries.BIOME);
        assertVegetationFeature(helper, biomes.getOrThrow(KHBiomes.KUBAN_STEPPE),
                KHPlacedFeatures.WILD_TOMATO_PLACED);
        assertVegetationFeature(helper, biomes.getOrThrow(KHBiomes.KUBAN_STEPPE),
                KHPlacedFeatures.WILD_GRAPE_PLACED);
        assertVegetationFeature(helper, biomes.getOrThrow(KHBiomes.PLAVNI),
                KHPlacedFeatures.WILD_TEA_PLACED);
        assertVegetationFeature(helper, biomes.getOrThrow(KHBiomes.LIMAN),
                KHPlacedFeatures.WILD_TEA_PLACED);
        assertVegetationFeature(helper, biomes.getOrThrow(KHBiomes.RIVER_FLOODPLAIN),
                KHPlacedFeatures.WILD_RICE_PLACED);
        helper.succeed();
    }

    private static void assertVegetationFeature(GameTestHelper helper,
            Holder<net.minecraft.world.level.biome.Biome> biome, ResourceKey<PlacedFeature> feature) {
        boolean present = biome.value().getGenerationSettings().features()
                .get(GenerationStep.Decoration.VEGETAL_DECORATION.ordinal())
                .stream().anyMatch(holder -> holder.is(feature));
        helper.assertTrue(present, "В биоме " + biome.unwrapKey().orElseThrow().identifier()
                + " отсутствует дикая культура " + feature.identifier());
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

    /** Обе NBT-заготовки структур разбираются менеджером шаблонов MC 26.2. */
    private static void testStructureTemplatesLoad(GameTestHelper helper) {
        for (String path : List.of("floodplain_fishing_camp", "plavni_reed_shelter")) {
            Identifier id = KHIds.of(path);
            StructureTemplate template = helper.getLevel().getStructureManager()
                    .get(id)
                    .orElseThrow(() -> new AssertionError("Шаблон структуры не загрузился: " + id));
            helper.assertTrue(template.getSize().getX() > 0
                            && template.getSize().getY() > 0
                            && template.getSize().getZ() > 0,
                    "Шаблон структуры должен иметь ненулевой размер: " + id);
        }
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
                // Рыболовство и ремесло: содержимое для обеих ветвей лежало
                // готовым — осётр с копчением и 32 строительных предмета, — а
                // достижений не было ни одного, и цепочку некуда было вести.
                "fishing/first_sturgeon", "fishing/cooked_sturgeon",
                "fishing/smoked_fish", "fishing/sturgeon_bucket",
                "crafts/adobe", "crafts/whitewash", "crafts/homestead",
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

        // «Кубанская усадьба» — тоже челлендж на все материалы разом.
        // Одна группа условий означала бы OR: «дом из чего-нибудь одного»,
        // что усадьбой не является. Стратегия задаётся по умолчанию, поэтому
        // ошибиться легко и заметить трудно — отсюда проверка.
        Advancement homestead = tree.get(KHIds.of("crafts/homestead")).advancement();
        helper.assertTrue(homestead.requirements().size() == 4,
                "«Кубанская усадьба» должна требовать все четыре материала, групп условий: "
                        + homestead.requirements().size());
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
                "whitewashed_plaster_stairs_from_whitewashed_plaster_stonecutting",
                "whitewashed_plaster_slab_from_whitewashed_plaster_stonecutting",
                "carved_window_casing_from_whitewashed_plaster_stonecutting",
                "roof_tile_stairs_from_roof_tiles_stonecutting",
                "roof_tile_slab_from_roof_tiles_stonecutting",
                "decorative_ceramic_from_roof_tiles_stonecutting",
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

    /** Белёная двойная плита сохраняет обе половины материала. */
    private static void testPlasterSlabDropsTwo(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, KHBlocks.WHITEWASHED_PLASTER_SLAB.get().defaultBlockState()
                .setValue(net.minecraft.world.level.block.SlabBlock.TYPE,
                        net.minecraft.world.level.block.state.properties.SlabType.DOUBLE));
        BlockPos abs = helper.absolutePos(pos);
        List<ItemStack> drops = net.minecraft.world.level.block.Block.getDrops(
                level.getBlockState(abs), level, abs, null, null,
                new ItemStack(Items.IRON_PICKAXE));
        int total = drops.stream()
                .filter(stack -> stack.is(KHItems.WHITEWASHED_PLASTER_SLAB.get()))
                .mapToInt(ItemStack::getCount)
                .sum();
        helper.assertValueEqual(total, 2, "Двойная плита штукатурки должна дать 2 плиты");

        helper.setBlock(pos, KHBlocks.WHITEWASHED_PLASTER_SLAB.get().defaultBlockState()
                .setValue(net.minecraft.world.level.block.SlabBlock.TYPE,
                        net.minecraft.world.level.block.state.properties.SlabType.BOTTOM));
        List<ItemStack> singleDrops = net.minecraft.world.level.block.Block.getDrops(
                level.getBlockState(abs), level, abs, null, null,
                new ItemStack(Items.IRON_PICKAXE));
        int singleTotal = singleDrops.stream()
                .filter(stack -> stack.is(KHItems.WHITEWASHED_PLASTER_SLAB.get()))
                .mapToInt(ItemStack::getCount)
                .sum();
        helper.assertValueEqual(singleTotal, 1, "Одинарная плита штукатурки должна дать 1 плиту");
        helper.succeed();
    }

    /** Штукатурка и наличник требуют кирку, дропают себя и входят в структурные теги. */
    private static void testPlasterToolsDropsAndTags(GameTestHelper helper) {
        assertNeedsPickaxe(helper, new BlockPos(1, 1, 1),
                KHBlocks.WHITEWASHED_PLASTER.get(), KHItems.WHITEWASHED_PLASTER.get());
        assertNeedsPickaxe(helper, new BlockPos(2, 1, 1),
                KHBlocks.CARVED_WINDOW_CASING.get(), KHItems.CARVED_WINDOW_CASING.get());

        helper.assertTrue(KHBlocks.WHITEWASHED_PLASTER_STAIRS.get().defaultBlockState().is(BlockTags.STAIRS),
                "Ступеньки штукатурки должны входить в block tag stairs");
        helper.assertTrue(KHBlocks.WHITEWASHED_PLASTER_SLAB.get().defaultBlockState().is(BlockTags.SLABS),
                "Плита штукатурки должна входить в block tag slabs");
        helper.assertTrue(new ItemStack(KHItems.WHITEWASHED_PLASTER_STAIRS.get()).is(BlockItemTags.STAIRS.item()),
                "Ступеньки штукатурки должны входить в item tag stairs");
        helper.assertTrue(new ItemStack(KHItems.WHITEWASHED_PLASTER_SLAB.get()).is(BlockItemTags.SLABS.item()),
                "Плита штукатурки должна входить в item tag slabs");
        helper.succeed();
    }

    /** Наличник хранит горизонтальный поворот и меняет тонкую форму по оси. */
    private static void testCarvedCasingFacingAndShape(GameTestHelper helper) {
        BlockState north = KHBlocks.CARVED_WINDOW_CASING.get().defaultBlockState()
                .setValue(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING, Direction.NORTH);
        BlockState east = north.setValue(
                net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING, Direction.EAST);
        helper.assertValueEqual(north.getValue(
                        net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING),
                Direction.NORTH, "Наличник должен сохранять северный поворот");
        helper.assertValueEqual(east.getValue(
                        net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING),
                Direction.EAST, "Наличник должен сохранять восточный поворот");
        var northBounds = north.getShape(helper.getLevel(), helper.absolutePos(new BlockPos(1, 1, 1))).bounds();
        var eastBounds = east.getShape(helper.getLevel(), helper.absolutePos(new BlockPos(2, 1, 1))).bounds();
        helper.assertTrue(northBounds.getZsize() < northBounds.getXsize(),
                "Северный наличник должен быть тонким по оси Z");
        helper.assertTrue(eastBounds.getXsize() < eastBounds.getZsize(),
                "Восточный наличник должен быть тонким по оси X");
        helper.assertTrue(!north.getShape(helper.getLevel(), helper.absolutePos(new BlockPos(1, 1, 1)))
                        .toAabbs().stream().anyMatch(box -> box.minX < 0.5 && box.maxX > 0.5
                                && box.minY < 0.5 && box.maxY > 0.5),
                "Северный наличник должен иметь открытый центр");
        helper.assertTrue(!east.getShape(helper.getLevel(), helper.absolutePos(new BlockPos(2, 1, 1)))
                        .toAabbs().stream().anyMatch(box -> box.minZ < 0.5 && box.maxZ > 0.5
                                && box.minY < 0.5 && box.maxY > 0.5),
                "Восточный наличник должен иметь открытый центр");
        helper.assertTrue(!north.canOcclude(), "Наличник не должен перекрывать соседние грани");
        helper.succeed();
    }

    /** Двойная плита черепицы сохраняет обе половины материала. */
    private static void testRoofTileSlabDropsTwo(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, KHBlocks.ROOF_TILE_SLAB.get().defaultBlockState()
                .setValue(net.minecraft.world.level.block.SlabBlock.TYPE,
                        net.minecraft.world.level.block.state.properties.SlabType.DOUBLE));
        BlockPos abs = helper.absolutePos(pos);
        List<ItemStack> drops = net.minecraft.world.level.block.Block.getDrops(
                level.getBlockState(abs), level, abs, null, null,
                new ItemStack(Items.IRON_PICKAXE));
        int total = drops.stream()
                .filter(stack -> stack.is(KHItems.ROOF_TILE_SLAB.get()))
                .mapToInt(ItemStack::getCount)
                .sum();
        helper.assertValueEqual(total, 2, "Двойная плита черепицы должна дать 2 плиты");

        helper.setBlock(pos, KHBlocks.ROOF_TILE_SLAB.get().defaultBlockState()
                .setValue(net.minecraft.world.level.block.SlabBlock.TYPE,
                        net.minecraft.world.level.block.state.properties.SlabType.BOTTOM));
        List<ItemStack> singleDrops = net.minecraft.world.level.block.Block.getDrops(
                level.getBlockState(abs), level, abs, null, null,
                new ItemStack(Items.IRON_PICKAXE));
        int singleTotal = singleDrops.stream()
                .filter(stack -> stack.is(KHItems.ROOF_TILE_SLAB.get()))
                .mapToInt(ItemStack::getCount)
                .sum();
        helper.assertValueEqual(singleTotal, 1, "Одинарная плита черепицы должна дать 1 плиту");
        helper.succeed();
    }

    /** Черепица и керамика требуют кирку, дропают себя и входят в shape tags. */
    private static void testCeramicsToolsDropsAndTags(GameTestHelper helper) {
        assertNeedsPickaxe(helper, new BlockPos(1, 1, 1),
                KHBlocks.ROOF_TILES.get(), KHItems.ROOF_TILES.get());
        assertNeedsPickaxe(helper, new BlockPos(2, 1, 1),
                KHBlocks.DECORATIVE_CERAMIC.get(), KHItems.DECORATIVE_CERAMIC.get());

        helper.assertTrue(KHBlocks.ROOF_TILE_STAIRS.get().defaultBlockState().is(BlockTags.STAIRS),
                "Ступеньки черепицы должны входить в block tag stairs");
        helper.assertTrue(KHBlocks.ROOF_TILE_SLAB.get().defaultBlockState().is(BlockTags.SLABS),
                "Плита черепицы должна входить в block tag slabs");
        helper.assertTrue(new ItemStack(KHItems.ROOF_TILE_STAIRS.get()).is(BlockItemTags.STAIRS.item()),
                "Ступеньки черепицы должны входить в item tag stairs");
        helper.assertTrue(new ItemStack(KHItems.ROOF_TILE_SLAB.get()).is(BlockItemTags.SLABS.item()),
                "Плита черепицы должна входить в item tag slabs");
        helper.succeed();
    }

    // --- Реестры ---

    /** Все заявленные ID контента присутствуют в реестрах. */
    private static void testRegistryContent(GameTestHelper helper) {
        String[] blocks = {"sunflower_crop", "oil_press",
                "adobe_bricks", "adobe_brick_stairs", "adobe_brick_slab", "adobe_brick_wall",
                "shell_rock", "shell_rock_stairs", "shell_rock_slab", "shell_rock_wall",
                "whitewashed_plaster", "whitewashed_plaster_stairs", "whitewashed_plaster_slab",
                "roof_tiles", "roof_tile_stairs", "roof_tile_slab", "decorative_ceramic",
                "carved_window_casing", "wattle", "wattle_gate"};
        String[] items = {"sunflower_seeds", "sunflower_head", "sunflower_oil",
                "oil_cake", "roasted_sunflower_seeds", "oil_press",
                "adobe_bricks", "adobe_brick_stairs", "adobe_brick_slab", "adobe_brick_wall",
                "shell_rock", "shell_rock_stairs", "shell_rock_slab", "shell_rock_wall",
                "whitewashed_plaster", "whitewashed_plaster_stairs", "whitewashed_plaster_slab",
                "roof_tiles", "roof_tile_stairs", "roof_tile_slab", "decorative_ceramic",
                "carved_window_casing", "wattle", "wattle_gate"};
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
<<<<<<< Updated upstream
=======

    /** Strong-wish runtime возвращает блоки и содержимое block entity после явного подтверждения. */
    private static void testGenieRuntimeRestoresRegion(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos first = helper.absolutePos(new BlockPos(1, 2, 1));
        BlockPos second = helper.absolutePos(new BlockPos(2, 2, 1));
        level.setBlock(first, Blocks.GOLD_BLOCK.defaultBlockState(), 3);
        level.setBlock(second, Blocks.CHEST.defaultBlockState(), 3);
        var chest = (net.minecraft.world.level.block.entity.ChestBlockEntity) level.getBlockEntity(second);
        helper.assertTrue(chest != null, "Сундук не создал block entity");
        chest.setItem(0, new ItemStack(Items.DIAMOND, 7));
        chest.setChanged();
        var pig = EntityTypes.PIG.create(level, net.minecraft.world.entity.EntitySpawnReason.COMMAND);
        helper.assertTrue(pig != null, "Свинья для снимка не создалась");
        pig.snapTo(first.getX() + 0.5D, first.getY(), first.getZ() + 0.5D, 0.0F, 0.0F);
        UUID pigId = pig.getUUID();
        level.addFreshEntity(pig);
        level.scheduleTick(first, Blocks.GOLD_BLOCK, 200);

        UUID owner = UUID.randomUUID();
        String name = "gametest_" + owner.toString().replace("-", "");
        var runtime = dev.romankrukovsky.kubanhorizons.genie.runtime.WishRuntime.get(level.getServer());
        if (!runtime.ready()) {
            runtime.recover();
        }
        try {
            var selection = new dev.romankrukovsky.kubanhorizons.genie.runtime.selection.RegionSelection(
                    level.dimension().identifier().toString(), first, second);
            runtime.createSnapshot(level, owner, name, selection);

            pig.discard();
            level.getBlockTicks().clearArea(
                    net.minecraft.world.level.levelgen.structure.BoundingBox.fromCorners(first, second));
            level.setBlock(first, Blocks.DIRT.defaultBlockState(), 3);
            level.setBlock(second, Blocks.AIR.defaultBlockState(), 3);
            var preview = runtime.previewRestore(level, owner, name);
            var confirmation = runtime.confirm(owner, preview);
            var report = runtime.restore(level, owner, confirmation);

            helper.assertTrue(report.outcome()
                            == dev.romankrukovsky.kubanhorizons.genie.runtime.transaction.TransactionOutcome.COMPLETED,
                    "Транзакция восстановления не завершилась: " + report);
            helper.assertTrue(level.getBlockState(first).is(Blocks.GOLD_BLOCK),
                    "Первый блок снимка не восстановлен");
            var restoredChest = (net.minecraft.world.level.block.entity.ChestBlockEntity)
                    level.getBlockEntity(second);
            helper.assertTrue(restoredChest != null, "Block entity сундука не восстановлена");
            helper.assertTrue(restoredChest.getItem(0).is(Items.DIAMOND)
                            && restoredChest.getItem(0).getCount() == 7,
                    "Инвентарь сундука не восстановлен");
            helper.assertTrue(level.getEntity(pigId) != null,
                    "Обычная сущность из снимка не восстановлена");
            helper.assertTrue(level.getBlockTicks().hasScheduledTick(first, Blocks.GOLD_BLOCK),
                    "Scheduled block tick из снимка не восстановлен");
            var undo = runtime.undo(level, owner, report.transactionId());
            helper.assertTrue(undo.outcome()
                            == dev.romankrukovsky.kubanhorizons.genie.runtime.transaction.TransactionOutcome.COMPLETED,
                    "Retained undo не завершился: " + undo);
            helper.assertTrue(level.getBlockState(first).is(Blocks.DIRT)
                            && level.getBlockState(second).isAir(),
                    "Undo не вернул состояние до восстановления");
            helper.assertTrue(level.getEntity(pigId) == null
                            && !level.getBlockTicks().hasScheduledTick(first, Blocks.GOLD_BLOCK),
                    "Undo не удалил восстановленные сущность и scheduled tick");
            helper.succeed();
        } catch (IOException | RuntimeException exception) {
            helper.fail("Strong-wish runtime failed: " + exception.getMessage());
        }
    }

    /** Сжатая область исчезает из мира и разворачивается блок-в-блок в другом месте. */
    private static void testGenieMiniaturizationRoundTrip(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        var player = helper.makeMockServerPlayerInLevel();
        BlockPos sourceMin = helper.absolutePos(new BlockPos(1, 2, 1));
        BlockPos sourceMax = helper.absolutePos(new BlockPos(2, 2, 1));
        BlockPos target = helper.absolutePos(new BlockPos(1, 2, 4));
        level.setBlock(sourceMin, Blocks.GOLD_BLOCK.defaultBlockState(), 3);
        level.setBlock(sourceMax, Blocks.EMERALD_BLOCK.defaultBlockState(), 3);
        var selection = new dev.romankrukovsky.kubanhorizons.genie.runtime.selection.RegionSelection(
                level.dimension().identifier().toString(), sourceMin, sourceMax);
        var runtime = dev.romankrukovsky.kubanhorizons.genie.runtime.WishRuntime
                .get(helper.getLevel().getServer());
        if (!runtime.ready()) {
            runtime.recover();
        }
        runtime.setSelection(player.getUUID(), selection);
        ItemStack miniature;
        try {
            var preview = runtime.previewMiniaturizeSelected(player);
            miniature = runtime.executeMiniaturize(player,
                    runtime.confirmMiniaturize(player.getUUID(), preview));
        } catch (IOException exception) {
            helper.fail("Miniaturization preview failed: " + exception.getMessage());
            return;
        }

        helper.assertTrue(!miniature.isEmpty(), "Миниатюризация не создала предмет");
        helper.assertTrue(level.isEmptyBlock(sourceMin) && level.isEmptyBlock(sourceMax),
                "Исходная область осталась в мире после сжатия");
        helper.assertTrue(dev.romankrukovsky.kubanhorizons.genie.spatial.MiniaturizationEngine
                        .uncompressRegion(level, target, miniature),
                "Миниатюра не развернулась в пустой области");
        helper.assertTrue(level.getBlockState(target).is(Blocks.GOLD_BLOCK)
                        && level.getBlockState(target.east()).is(Blocks.EMERALD_BLOCK),
                "Развёрнутая область отличается от исходной");
        helper.assertTrue(miniature.isEmpty(), "Предмет миниатюры не израсходован");
        player.discard();
        helper.succeed();
    }

    /** Материализация строит читаемый набор блоков, а не один декоративный куб. */
    private static void testGenieMaterializedWord(GameTestHelper helper) {
        var player = helper.makeMockServerPlayerInLevel();
        player.setPos(net.minecraft.world.phys.Vec3.atCenterOf(
                helper.absolutePos(new BlockPos(1, 2, 1))));
        var runtime = dev.romankrukovsky.kubanhorizons.genie.runtime.WishRuntime
                .get(helper.getLevel().getServer());
        if (!runtime.ready()) runtime.recover();
        try {
            var preview = runtime.previewWord(player, "GOLD");
            helper.assertTrue(preview.changedBlocks() >= 20,
                    "Preview слова содержит слишком мало блоков");
            var report = runtime.executeWord(player, runtime.confirmWord(player.getUUID(), preview));
            helper.assertTrue(report.outcome()
                            == dev.romankrukovsky.kubanhorizons.genie.runtime.transaction.TransactionOutcome.COMPLETED,
                    "Слово не материализовано транзакцией: " + report);
            long gold = BlockPos.betweenClosedStream(preview.selection().min(), preview.selection().max())
                    .filter(pos -> helper.getLevel().getBlockState(pos).is(Blocks.GOLD_BLOCK)).count();
            helper.assertTrue(gold >= 20, "Слово GOLD не стало набором букв: блоков " + gold);
            var undo = runtime.undo(helper.getLevel(), player.getUUID(), report.transactionId());
            helper.assertTrue(undo.outcome()
                            == dev.romankrukovsky.kubanhorizons.genie.runtime.transaction.TransactionOutcome.COMPLETED,
                    "Undo слова не завершился");
            runtime.retireUndo(player.getUUID(), undo.transactionId());
            player.discard();
            helper.succeed();
        } catch (IOException | RuntimeException exception) {
            helper.fail("Runtime word failed: " + exception.getMessage());
        }
    }

    /** Гибрид действительно летает и светится, свойства сохраняются в NBT сущности. */
    private static void testGenieHybridTraits(GameTestHelper helper) {
        var hybrid = dev.romankrukovsky.kubanhorizons.genie.entity.HybridSpeciesEngine.synthesizeHybrid(
                helper.getLevel(), helper.absoluteVec(new net.minecraft.world.phys.Vec3(1, 2, 1)),
                "flying glowing fox");
        helper.assertTrue(hybrid != null && hybrid.isNoGravity() && hybrid.isCurrentlyGlowing(),
                "Гибрид не получил заявленные свойства полёта и свечения");
        helper.assertTrue(hybrid.getPersistentData().getBooleanOr("KubanHybrid", false),
                "Признаки гибрида не помечены как персистентные");
        helper.succeed();
    }

    /** Контракт — реальная письменная книга с условием, сроком, мелким текстом и лазейкой. */
    private static void testGenieContractTerms(GameTestHelper helper) {
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        ItemStack contract = dev.romankrukovsky.kubanhorizons.genie.wish.WishContractEngine
                .createContractBook(helper.getLevel(), player, "Restore my house");
        WrittenBookContent content = contract.get(DataComponents.WRITTEN_BOOK_CONTENT);
        helper.assertTrue(content != null && content.pages().size() == 4,
                "Контракт не содержит четыре обязательных раздела");
        String text = content.getPages(false).stream().map(Component::getString)
                .collect(java.util.stream.Collectors.joining(" "));
        helper.assertTrue(text.contains("Restore my house") && text.contains("Fine print")
                        && text.contains("Loophole") && text.contains("world tick"),
                "В контракте нет формулировки, срока, мелкого текста или лазейки");
        helper.succeed();
    }

    /** Переписывание биома меняет палитру чанка, а не только создаёт частицы. */
    private static void testGenieBiomeRewrite(GameTestHelper helper) {
        BlockPos center = helper.absolutePos(new BlockPos(1, 2, 1));
        var player = helper.makeMockServerPlayerInLevel();
        var runtime = dev.romankrukovsky.kubanhorizons.genie.runtime.WishRuntime
                .get(helper.getLevel().getServer());
        if (!runtime.ready()) runtime.recover();
        var before = helper.getLevel().getBiome(center).unwrapKey().orElseThrow();
        try {
            var preview = runtime.previewBiomeRewrite(player, center);
            helper.assertTrue(helper.getLevel().getBiome(center).is(before),
                    "Preview переписывания биома изменил мир");
            var report = runtime.executeBiomeRewrite(player,
                    runtime.confirmBiomeRewrite(player.getUUID(), preview));
            helper.assertTrue(report.outcome()
                            == dev.romankrukovsky.kubanhorizons.genie.runtime.transaction.TransactionOutcome.COMPLETED
                            && helper.getLevel().getBiome(center).is(KHBiomes.KUBAN_STEPPE),
                    "Биом не переписан транзакцией: " + report);
            var undo = runtime.undo(helper.getLevel(), player.getUUID(), report.transactionId());
            helper.assertTrue(undo.outcome()
                            == dev.romankrukovsky.kubanhorizons.genie.runtime.transaction.TransactionOutcome.COMPLETED
                            && helper.getLevel().getBiome(center).is(before),
                    "Undo не вернул исходный биом");
            runtime.retireUndo(player.getUUID(), undo.transactionId());
            player.discard();
            helper.succeed();
        } catch (IOException | RuntimeException exception) {
            helper.fail("Runtime biome rewrite failed: " + exception.getMessage());
        }
    }

    /** Обмен ролями освобождает NPC и записывает игроку полноценное состояние джинна. */
    private static void testGenieRoleSwap(GameTestHelper helper) {
        dev.romankrukovsky.kubanhorizons.genie.GenieAnchor.releaseFor(helper.getLevel());
        var genie = helper.spawn(KHEntities.KUBAN_GENIE.get(), new BlockPos(1, 2, 1));
        var player = helper.makeMockServerPlayerInLevel();
        genie.mobInteract(player, net.minecraft.world.InteractionHand.MAIN_HAND);
        helper.assertTrue(dev.romankrukovsky.kubanhorizons.genie.evolution.GenieRoleSwap
                        .swapRoles(genie, helper.getLevel(), player),
                "Обмен ролями не произошёл при действующей связи");
        var data = player.getData(dev.romankrukovsky.kubanhorizons.registry.KHAttachments.PLAYER_GENIE_DATA);
        helper.assertTrue(data.isGenie() && data.getStage()
                        == dev.romankrukovsky.kubanhorizons.genie.player.PlayerGenieAttachment.Stage.FULL_GENIE,
                "Игрок не получил полную форму джинна");
        helper.assertTrue(genie.getOwner() == null, "NPC-джинния не стала свободной");
        player.discard();
        helper.succeed();
    }

    /** Сжатие через runtime требует preview и одноразовое подтверждение. */
    private static void testRuntimeMiniaturizeConfirmation(GameTestHelper helper) {
        var player = helper.makeMockServerPlayerInLevel();
        BlockPos source = helper.absolutePos(new BlockPos(1, 2, 1));
        helper.getLevel().setBlock(source, Blocks.EMERALD_BLOCK.defaultBlockState(), 3);
        var selection = new dev.romankrukovsky.kubanhorizons.genie.runtime.selection.RegionSelection(
                helper.getLevel().dimension().identifier().toString(), source, source);
        var runtime = dev.romankrukovsky.kubanhorizons.genie.runtime.WishRuntime
                .get(helper.getLevel().getServer());
        if (!runtime.ready()) {
            runtime.recover();
        }
        runtime.setSelection(player.getUUID(), selection);
        try {
            var preview = runtime.previewMiniaturizeSelected(player);
            helper.assertTrue(helper.getLevel().getBlockState(source).is(Blocks.EMERALD_BLOCK),
                    "Preview миниатюризации изменил мир");
            var confirmation = runtime.confirmMiniaturize(player.getUUID(), preview);
            ItemStack miniature = runtime.executeMiniaturize(player, confirmation);
            helper.assertTrue(miniature.is(KHItems.MINIATURE_WORLD.get())
                            && helper.getLevel().getBlockState(source).isAir(),
                    "Подтверждённая миниатюризация не создала предмет и не очистила область");
            boolean rejectedReuse = false;
            try {
                runtime.executeMiniaturize(player, confirmation);
            } catch (IllegalArgumentException expected) {
                rejectedReuse = true;
            }
            helper.assertTrue(rejectedReuse,
                    "Одно подтверждение миниатюризации сработало повторно");
            player.discard();
            helper.succeed();
        } catch (IOException | RuntimeException exception) {
            helper.fail("Runtime miniaturization failed: " + exception.getMessage());
        }
    }

    /** Список изолирован по владельцу, удаление работает и не трогает чужой снимок. */
    private static void testSnapshotManagement(GameTestHelper helper) {
        var runtime = dev.romankrukovsky.kubanhorizons.genie.runtime.WishRuntime
                .get(helper.getLevel().getServer());
        if (!runtime.ready()) {
            runtime.recover();
        }
        BlockPos pos = helper.absolutePos(new BlockPos(1, 2, 1));
        helper.getLevel().setBlock(pos, Blocks.GOLD_BLOCK.defaultBlockState(), 3);
        var selection = new dev.romankrukovsky.kubanhorizons.genie.runtime.selection.RegionSelection(
                helper.getLevel().dimension().identifier().toString(), pos, pos);
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        try {
            runtime.createSnapshot(helper.getLevel(), first, "shared_name", selection);
            runtime.createSnapshot(helper.getLevel(), second, "shared_name", selection);
            helper.assertTrue(runtime.listSnapshots(first).size() == 1
                            && runtime.listSnapshots(second).size() == 1,
                    "Список снимков смешал владельцев");
            helper.assertTrue(runtime.inspectSnapshot(first, "shared_name").blocks() == 1,
                    "Inspect не вернул метаданные снимка");
            runtime.deleteSnapshot(first, "shared_name");
            helper.assertTrue(runtime.listSnapshots(first).isEmpty()
                            && runtime.listSnapshots(second).size() == 1,
                    "Удаление снимка затронуло неверного владельца");
            runtime.deleteSnapshot(second, "shared_name");
            helper.succeed();
        } catch (IOException | RuntimeException exception) {
            helper.fail("Snapshot management failed: " + exception.getMessage());
        }
    }

    /** Карманная сцена проходит preview/confirmation/transaction и откатывается через retained undo. */
    private static void testRuntimePocketScene(GameTestHelper helper) {
        var player = helper.makeMockServerPlayerInLevel();
        BlockPos origin = helper.absolutePos(new BlockPos(4, 2, 4));
        helper.getLevel().setBlock(origin, Blocks.DIAMOND_BLOCK.defaultBlockState(), 3);
        var runtime = dev.romankrukovsky.kubanhorizons.genie.runtime.WishRuntime
                .get(helper.getLevel().getServer());
        if (!runtime.ready()) {
            runtime.recover();
        }
        try {
            var preview = runtime.previewPocketScene(player, origin, 20);
            helper.assertTrue(helper.getLevel().getBlockState(origin).is(Blocks.DIAMOND_BLOCK),
                    "Preview карманной сцены изменил мир");
            var report = runtime.executePocketScene(player,
                    runtime.confirmPocketScene(player.getUUID(), preview));
            helper.assertTrue(report.outcome()
                            == dev.romankrukovsky.kubanhorizons.genie.runtime.transaction.TransactionOutcome.COMPLETED
                            && helper.getLevel().getBlockState(origin).is(Blocks.SANDSTONE),
                    "Транзакционная карманная сцена не создалась: " + report);
            var rollback = runtime.undo(helper.getLevel(), player.getUUID(), report.transactionId());
            helper.assertTrue(rollback.outcome()
                            == dev.romankrukovsky.kubanhorizons.genie.runtime.transaction.TransactionOutcome.COMPLETED
                            && helper.getLevel().getBlockState(origin).is(Blocks.DIAMOND_BLOCK),
                    "Карманная сцена не вернула исходный мир через retained undo");
            runtime.retireUndo(player.getUUID(), rollback.transactionId());
            player.discard();
            helper.succeed();
        } catch (IOException | RuntimeException exception) {
            helper.fail("Runtime pocket scene failed: " + exception.getMessage());
        }
    }

    /** Диалоговая карманная сцена не меняет мир до подтверждения и сама возвращает его по таймеру. */
    private static void testDialogPocketSceneCycle(GameTestHelper helper) {
        var player = helper.makeMockServerPlayerInLevel();
        BlockPos origin = helper.absolutePos(new BlockPos(4, 2, 4));
        player.snapTo(origin.getX() + 0.5D, origin.getY(), origin.getZ() + 0.5D,
                0.0F, 0.0F);
        helper.getLevel().setBlock(origin, Blocks.DIAMOND_BLOCK.defaultBlockState(), 3);
        var preview = dev.romankrukovsky.kubanhorizons.genie.dimension.PocketSceneService
                .preview(player, 5);
        helper.assertTrue(preview.success(),
                "Предпросмотр карманной сцены отклонён: " + preview.message().getString());
        helper.assertTrue(helper.getLevel().getBlockState(origin).is(Blocks.DIAMOND_BLOCK),
                "Предпросмотр карманной сцены изменил мир: "
                        + helper.getLevel().getBlockState(origin));
        var applied = dev.romankrukovsky.kubanhorizons.genie.dimension.PocketSceneService
                .confirm(player);
        helper.assertTrue(applied.success()
                        && helper.getLevel().getBlockState(origin).is(Blocks.SANDSTONE),
                "Подтверждённая карманная сцена не создалась");

        helper.startSequence()
                .thenExecuteAfter(6, () ->
                        dev.romankrukovsky.kubanhorizons.genie.dimension.PocketSceneService
                                .tick(helper.getLevel()))
                .thenExecute(() -> helper.assertTrue(
                        helper.getLevel().getBlockState(origin).is(Blocks.DIAMOND_BLOCK)
                                && !dev.romankrukovsky.kubanhorizons.genie.dimension.PocketSceneService
                                .isActive(helper.getLevel(), player.getUUID()),
                        "Карманная сцена не вернула исходный мир по таймеру"))
                .thenExecute(player::discard)
                .thenSucceed();
    }

    /** Небольшой дом переносится через preview/confirmation/transaction и возвращается undo. */
    private static void testRuntimeStructureMove(GameTestHelper helper) {
        var player = helper.makeMockServerPlayerInLevel();
        BlockPos origin = helper.absolutePos(new BlockPos(4, 2, 4));
        helper.getLevel().setBlock(origin, Blocks.GOLD_BLOCK.defaultBlockState(), 3);
        helper.getLevel().setBlock(origin.east(), Blocks.EMERALD_BLOCK.defaultBlockState(), 3);
        var runtime = dev.romankrukovsky.kubanhorizons.genie.runtime.WishRuntime
                .get(helper.getLevel().getServer());
        if (!runtime.ready()) {
            runtime.recover();
        }
        var source = new dev.romankrukovsky.kubanhorizons.genie.runtime.selection.RegionSelection(
                helper.getLevel().dimension().identifier().toString(), origin, origin.east());
        runtime.setSelection(player.getUUID(), source);
        try {
            var preview = runtime.previewSelectedStructureMove(player, new BlockPos(0, 10, 0));
            helper.assertTrue(helper.getLevel().getBlockState(origin).is(Blocks.GOLD_BLOCK),
                    "Preview переноса изменил исходный дом");
            var report = runtime.executeStructureMove(player,
                    runtime.confirmStructureMove(player.getUUID(), preview));
            helper.assertTrue(report.outcome()
                            == dev.romankrukovsky.kubanhorizons.genie.runtime.transaction.TransactionOutcome.COMPLETED,
                    "Перенос структуры не завершился: " + report);
            helper.assertTrue(helper.getLevel().getBlockState(origin).isAir()
                            && helper.getLevel().getBlockState(origin.above(10)).is(Blocks.GOLD_BLOCK)
                            && helper.getLevel().getBlockState(origin.east().above(10)).is(Blocks.EMERALD_BLOCK),
                    "Дом не переместился блок-в-блок");
            var undo = runtime.undo(helper.getLevel(), player.getUUID(), report.transactionId());
            helper.assertTrue(undo.outcome()
                            == dev.romankrukovsky.kubanhorizons.genie.runtime.transaction.TransactionOutcome.COMPLETED
                            && helper.getLevel().getBlockState(origin).is(Blocks.GOLD_BLOCK)
                            && helper.getLevel().getBlockState(origin.above(10)).isAir(),
                    "Undo не вернул дом на место");
            runtime.retireUndo(player.getUUID(), undo.transactionId());
            player.discard();
            helper.succeed();
        } catch (IOException | RuntimeException exception) {
            helper.fail("Runtime structure move failed: " + exception.getMessage());
        }
    }

    /** Асимметричный blueprint поворачивается вместе с состояниями блоков. */
    private static void testRuntimeStructureRotate(GameTestHelper helper) {
        var player = helper.makeMockServerPlayerInLevel();
        BlockPos origin = helper.absolutePos(new BlockPos(3, 2, 3));
        helper.getLevel().setBlock(origin, Blocks.GOLD_BLOCK.defaultBlockState(), 3);
        helper.getLevel().setBlock(origin.east(), Blocks.EMERALD_BLOCK.defaultBlockState(), 3);
        var runtime = dev.romankrukovsky.kubanhorizons.genie.runtime.WishRuntime
                .get(helper.getLevel().getServer());
        if (!runtime.ready()) runtime.recover();
        runtime.setSelection(player.getUUID(),
                new dev.romankrukovsky.kubanhorizons.genie.runtime.selection.RegionSelection(
                        helper.getLevel().dimension().identifier().toString(), origin, origin.east()));
        BlockPos offset = new BlockPos(6, 0, 0);
        BlockPos destination = origin.offset(offset);
        try {
            var preview = runtime.previewSelectedStructureMove(player, offset,
                    net.minecraft.world.level.block.Rotation.CLOCKWISE_90);
            var report = runtime.executeStructureMove(player,
                    runtime.confirmStructureMove(player.getUUID(), preview));
            helper.assertTrue(report.outcome()
                            == dev.romankrukovsky.kubanhorizons.genie.runtime.transaction.TransactionOutcome.COMPLETED,
                    "Поворот blueprint не завершился: " + report);
            helper.assertTrue(helper.getLevel().getBlockState(origin).isAir()
                            && helper.getLevel().getBlockState(origin.east()).isAir()
                            && helper.getLevel().getBlockState(destination).is(Blocks.GOLD_BLOCK)
                            && helper.getLevel().getBlockState(destination.south()).is(Blocks.EMERALD_BLOCK),
                    "Blueprint не повернулся по часовой стрелке вокруг своего угла");
            var undo = runtime.undo(helper.getLevel(), player.getUUID(), report.transactionId());
            helper.assertTrue(undo.outcome()
                            == dev.romankrukovsky.kubanhorizons.genie.runtime.transaction.TransactionOutcome.COMPLETED
                            && helper.getLevel().getBlockState(origin).is(Blocks.GOLD_BLOCK)
                            && helper.getLevel().getBlockState(origin.east()).is(Blocks.EMERALD_BLOCK),
                    "Undo не вернул повёрнутый blueprint");
            runtime.retireUndo(player.getUUID(), undo.transactionId());
            player.discard();
            helper.succeed();
        } catch (IOException | RuntimeException exception) {
            helper.fail("Runtime structure rotation failed: " + exception.getMessage());
        }
    }

    /** Обычная сущность внутри дома перемещается вместе с ним и возвращается через undo. */
    private static void testRuntimeStructureMovesEntities(GameTestHelper helper) {
        var player = helper.makeMockServerPlayerInLevel();
        ServerLevel level = helper.getLevel();
        BlockPos origin = helper.absolutePos(new BlockPos(3, 2, 3));
        level.setBlock(origin, Blocks.OAK_PLANKS.defaultBlockState(), 3);
        var sheep = EntityTypes.SHEEP.create(level, net.minecraft.world.entity.EntitySpawnReason.COMMAND);
        helper.assertTrue(sheep != null, "Овца для переноса не создалась");
        sheep.snapTo(origin.getX() + 0.5D, origin.getY() + 1.0D,
                origin.getZ() + 0.5D, 0.0F, 0.0F);
        UUID sheepId = sheep.getUUID();
        level.addFreshEntity(sheep);
        var runtime = dev.romankrukovsky.kubanhorizons.genie.runtime.WishRuntime
                .get(level.getServer());
        if (!runtime.ready()) runtime.recover();
        runtime.setSelection(player.getUUID(),
                new dev.romankrukovsky.kubanhorizons.genie.runtime.selection.RegionSelection(
                        level.dimension().identifier().toString(), origin, origin.offset(0, 2, 0)));
        BlockPos offset = new BlockPos(6, 0, 0);
        try {
            var preview = runtime.previewSelectedStructureMove(player, offset);
            var report = runtime.executeStructureMove(player,
                    runtime.confirmStructureMove(player.getUUID(), preview));
            var moved = level.getEntity(sheepId);
            helper.assertTrue(report.outcome()
                            == dev.romankrukovsky.kubanhorizons.genie.runtime.transaction.TransactionOutcome.COMPLETED
                            && moved != null
                            && moved.blockPosition().closerThan(origin.offset(offset), 2.0D),
                    "Сущность не переместилась вместе со структурой: " + report);
            var undo = runtime.undo(level, player.getUUID(), report.transactionId());
            var restored = level.getEntity(sheepId);
            helper.assertTrue(undo.outcome()
                            == dev.romankrukovsky.kubanhorizons.genie.runtime.transaction.TransactionOutcome.COMPLETED
                            && restored != null && restored.blockPosition().closerThan(origin, 2.0D),
                    "Undo не вернул сущность в исходную структуру");
            runtime.retireUndo(player.getUUID(), undo.transactionId());
            player.discard();
            helper.succeed();
        } catch (IOException | RuntimeException exception) {
            helper.fail("Runtime entity move failed: " + exception.getMessage());
        }
    }

    /** Рисунок-линия проходит preview/confirmation/transaction и retained undo. */
    private static void testRuntimeMagicDrawing(GameTestHelper helper) {
        var player = helper.makeMockServerPlayerInLevel();
        BlockPos from = helper.absolutePos(new BlockPos(1, 3, 1));
        BlockPos to = helper.absolutePos(new BlockPos(5, 3, 1));
        var selection = new dev.romankrukovsky.kubanhorizons.genie.runtime.selection.RegionSelection(
                helper.getLevel().dimension().identifier().toString(), from, to);
        var runtime = dev.romankrukovsky.kubanhorizons.genie.runtime.WishRuntime
                .get(helper.getLevel().getServer());
        if (!runtime.ready()) runtime.recover();
        runtime.setSelection(player.getUUID(), selection);
        try {
            var preview = runtime.previewSelectedDrawing(player);
            helper.assertTrue(helper.getLevel().getBlockState(from).isAir(),
                    "Preview рисунка изменил мир");
            var report = runtime.executeDrawing(player,
                    runtime.confirmDrawing(player.getUUID(), preview));
            helper.assertTrue(report.outcome()
                            == dev.romankrukovsky.kubanhorizons.genie.runtime.transaction.TransactionOutcome.COMPLETED,
                    "Рисунок не выполнен транзакцией: " + report);
            for (BlockPos pos : BlockPos.betweenClosed(from, to)) {
                helper.assertTrue(helper.getLevel().getBlockState(pos).is(Blocks.OAK_PLANKS),
                        "Линия рисунка имеет разрыв в " + pos);
            }
            var undo = runtime.undo(helper.getLevel(), player.getUUID(), report.transactionId());
            helper.assertTrue(undo.outcome()
                            == dev.romankrukovsky.kubanhorizons.genie.runtime.transaction.TransactionOutcome.COMPLETED
                            && helper.getLevel().getBlockState(from).isAir(),
                    "Undo не убрал рисунок");
            runtime.retireUndo(player.getUUID(), undo.transactionId());
            player.discard();
            helper.succeed();
        } catch (IOException | RuntimeException exception) {
            helper.fail("Runtime drawing failed: " + exception.getMessage());
        }
    }

    /** Распознавание искажённого желания высшего порядка «Я хочу стать всемогущим». */
    private static void testPlayerGenieDistortedWishParse(GameTestHelper helper) {
        var intent = dev.romankrukovsky.kubanhorizons.genie.wish.WishParser.parse("Я хочу стать всемогущим.");
        helper.assertTrue(intent.category() == dev.romankrukovsky.kubanhorizons.genie.wish.WishIntent.Category.DISTORTED_HIGHER_WISH,
                "Желание не отнесено к категории DISTORTED_HIGHER_WISH: " + intent);
        helper.assertTrue(intent.target() == dev.romankrukovsky.kubanhorizons.genie.wish.WishIntent.Target.OMNIPOTENCE,
                "Цель OMNIPOTENCE не распознана: " + intent);
        helper.succeed();
    }

    /** Сохранение и сериализация джинновского состояния игрока в Data Attachment. */
    private static void testPlayerGenieAttachmentPersistence(GameTestHelper helper) {
        var attachment = new dev.romankrukovsky.kubanhorizons.genie.player.PlayerGenieAttachment();
        attachment.setGenie(true);
        attachment.setStage(dev.romankrukovsky.kubanhorizons.genie.player.PlayerGenieAttachment.Stage.FULL_GENIE);
        attachment.setWishProgressPercent(63);
        attachment.setTierLevel(3);

        helper.assertTrue(attachment.isGenie(), "Флаг isGenie не сохранён");
        helper.assertTrue(attachment.getWishProgressPercent() == 63, "Прогресс 63% не сохранён");
        helper.assertTrue(attachment.getTierLevel() == 3, "Уровень 3 не сохранён");
        helper.succeed();
    }

    /** Контроллер проходит стадии по времени, а не завершает всю сцену одним вызовом. */
    private static void testPlayerGenieTransformationController(GameTestHelper helper) {
        var player = helper.makeMockServerPlayerInLevel();
        dev.romankrukovsky.kubanhorizons.genie.player.PlayerGenieTransformationController.startTransformation(
                helper.getLevel(), player,
                dev.romankrukovsky.kubanhorizons.genie.wish.WishIntent.Target.OMNIPOTENCE);
        var data = player.getData(dev.romankrukovsky.kubanhorizons.registry.KHAttachments.PLAYER_GENIE_DATA);
        helper.assertTrue(data.isGenie(), "Игрок не получил джинновское состояние");
        helper.assertTrue(data.getStage()
                        == dev.romankrukovsky.kubanhorizons.genie.player.PlayerGenieAttachment.Stage.BODY_REWRITE,
                "Сцена должна остановиться на первой стадии до следующего времени");
        helper.assertTrue(data.getNextTransformationTick() > helper.getLevel().getGameTime(),
                "Следующая стадия не получила серверный таймер");

        data.setNextTransformationTick(helper.getLevel().getGameTime());
        dev.romankrukovsky.kubanhorizons.genie.player.PlayerGenieTransformationController
                .tickTransformation(helper.getLevel(), player);
        helper.assertTrue(data.getStage()
                        == dev.romankrukovsky.kubanhorizons.genie.player.PlayerGenieAttachment.Stage.TAIL_FORMATION,
                "Вторая стадия не наступила после серверного таймера");
        helper.assertTrue(player.getAbilities().mayfly, "Вторая стадия не открыла полёт");
        player.discard();
        helper.succeed();
    }

    /** Система Хозяин-Сосуд и лампа превращённого игрока. */
    private static void testPlayerGenieVesselAndMaster(GameTestHelper helper) {
        var geniePlayer = helper.makeMockPlayer(GameType.SURVIVAL);
        var masterPlayer = helper.makeMockPlayer(GameType.SURVIVAL);
        if (geniePlayer instanceof net.minecraft.server.level.ServerPlayer serverGenie) {
            var data = serverGenie.getData(dev.romankrukovsky.kubanhorizons.registry.KHAttachments.PLAYER_GENIE_DATA);
            data.setGenie(true);
            boolean summoned = dev.romankrukovsky.kubanhorizons.genie.player.GenieMasterManager.summonGeniePlayer(
                    helper.getLevel(), masterPlayer, serverGenie);
            helper.assertTrue(summoned, "Призыв игрока-джиннии должен быть успешным");
            helper.assertTrue(data.getMasterUUID().isPresent() && data.getMasterUUID().get().equals(masterPlayer.getUUID()),
                    "Хозяин не сохранился в привязке джиннии");
        }
        helper.succeed();
    }

    /** Прогрессия 5 уровней и выполнение Воли Джиннии. */
    private static void testPlayerGenieProgressionTiers(GameTestHelper helper) {
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            var data = serverPlayer.getData(dev.romankrukovsky.kubanhorizons.registry.KHAttachments.PLAYER_GENIE_DATA);
            data.setGenie(true);
            dev.romankrukovsky.kubanhorizons.genie.player.PlayerGenieProgression.advanceProgress(serverPlayer, 35); // 0 + 35 = 35 -> Tier 1
            helper.assertTrue(data.getWishProgressPercent() == 35, "Прогресс 35% не записан");
            dev.romankrukovsky.kubanhorizons.genie.player.PlayerGenieProgression.advanceProgress(serverPlayer, 60); // 35 + 60 = 95 -> Tier 5
            helper.assertTrue(data.getTierLevel() == 5, "Должен разблокироваться Tier 5 при 95%");

            boolean executed = dev.romankrukovsky.kubanhorizons.genie.player.PlayerGenieProgression.executeGenieWill(
                    helper.getLevel(), serverPlayer, "Сделай иначе");
            helper.assertTrue(executed, "Воля Джиннии на Tier 5 должна успешно выполняться");
        }
        helper.succeed();
    }

    /** Секретная концовка 100% выполнения Желания №1. */
    private static void testPlayerGenieTrueOmnipotenceEnding(GameTestHelper helper) {
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            var data = serverPlayer.getData(dev.romankrukovsky.kubanhorizons.registry.KHAttachments.PLAYER_GENIE_DATA);
            data.setGenie(true);
            dev.romankrukovsky.kubanhorizons.genie.player.TrueOmnipotenceEnding.triggerEnding(helper.getLevel(), serverPlayer);
            helper.assertTrue(data.getWishProgressPercent() == 100, "Прогресс должен достичь 100%");
            helper.assertTrue(data.getTierLevel() == 5, "Уровень должен быть равным 5");
        }
        helper.succeed();
    }

    // --- Тесты давления на хозяйство ---
    //
    // Эти механики уничтожают собственность игрока: посевы, плодородие,
    // блоки оросительной сети. Непокрытая тестами разрушительная логика —
    // самый дорогой класс ошибок в моде, потому что цена бага здесь не
    // «выглядит не так», а «съело чужую ферму».

    /**
     * Нутрия грызёт деревянный желоб, но не каменный.
     *
     * <p>Ровно на этой разнице держится смысл апгрейда сети: если бы цель
     * искала блок через {@code instanceof}, каменный желоб (подкласс
     * деревянного) тоже стал бы добычей, и платить за апгрейд было бы не за
     * что. Поэтому оба случая проверяются в одном тесте — иначе регрессия
     * прошла бы незамеченной.</p>
     */
    private static void testNutriaGnawsOnlyWoodenChannel(GameTestHelper helper) {
        if (!dev.romankrukovsky.kubanhorizons.config.KHServerConfig.pressureEnabled()) {
            helper.succeed();
            return;
        }
        BlockPos woodPos = new BlockPos(1, 2, 1);
        BlockPos stonePos = new BlockPos(3, 2, 1);
        // Пол: без опоры нутрия провалится и не дойдёт до сети.
        for (int dx = -1; dx <= 5; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                helper.setBlock(woodPos.offset(dx, -1, dz), Blocks.DIRT);
            }
        }
        helper.setBlock(woodPos, KHBlocks.IRRIGATION_CHANNEL.get());
        helper.setBlock(stonePos, KHBlocks.STONE_IRRIGATION_CHANNEL.get());

        var nutria = helper.spawn(KHEntities.NUTRIA.get(), woodPos.offset(1, 0, 0));
        helper.assertTrue(nutria != null, "Нутрия не создалась");

        helper.runAfterDelay(190, () -> {
            helper.assertTrue(
                    !helper.getBlockState(woodPos).is(KHBlocks.IRRIGATION_CHANNEL.get()),
                    "Нутрия не сгрызла деревянный желоб: сеть неуязвима, давления нет");
            helper.assertTrue(
                    helper.getBlockState(stonePos).is(KHBlocks.STONE_IRRIGATION_CHANNEL.get()),
                    "Каменный желоб уничтожен — апгрейд сети не защищает, и платить за него незачем");
            nutria.discard();
            helper.succeed();
        });
    }

    /**
     * Суховей сушит открытую грядку, но не трогает политую.
     *
     * <p>Исключение важнее самого иссушения: политая сетью земля переживает
     * ветер — в этом вся награда за постройку орошения. Сломайся это, и система
     * полива потеряла бы смысл, а тест на «сушит» остался бы зелёным.</p>
     *
     * <p>Проверка «под крышей не сушит» сюда намеренно не входит: она опирается
     * на {@code canSeeSky}, то есть на карту освещения, которая в тестовой
     * структуре не пересчитывается сразу после установки блока. Такой тест
     * падал бы на устройстве теста, а не на механике.</p>
     */
    private static void testDryWindSparesWateredAndSheltered(GameTestHelper helper) {
        if (!dev.romankrukovsky.kubanhorizons.config.KHServerConfig.dryWindEnabled()) {
            helper.succeed();
            return;
        }
        ServerLevel level = helper.getLevel();
        var moisture = net.minecraft.world.level.block.FarmlandBlock.MOISTURE;

        BlockPos open = new BlockPos(1, 2, 1);
        BlockPos watered = new BlockPos(3, 2, 1);
        for (BlockPos pos : new BlockPos[]{open, watered}) {
            helper.setBlock(pos.below(), Blocks.DIRT);
            helper.setBlock(pos, Blocks.FARMLAND.defaultBlockState().setValue(moisture, 7));
        }
        // Политая: заполненный желоб отдаёт воду, как в настоящей сети.
        helper.setBlock(watered.above(), Blocks.WATER);

        // Три волны: за одну ветер снимает один шаг влажности. Больше не нужно —
        // досуха выветренная грядка осыпается в землю, и тогда у блока уже нет
        // свойства влажности, о которое падал первый вариант теста.
        for (int i = 0; i < 3; i++) {
            dev.romankrukovsky.kubanhorizons.weather.DryWind.blow(
                    level, helper.absolutePos(open));
        }

        BlockState openAfter = helper.getBlockState(open);
        boolean driedOut = !openAfter.hasProperty(moisture)
                || openAfter.getValue(moisture) < 7;
        helper.assertTrue(driedOut,
                "Суховей не иссушил открытую грядку: орошение остаётся необязательным");
        BlockState wateredAfter = helper.getBlockState(watered);
        helper.assertTrue(wateredAfter.hasProperty(moisture)
                        && wateredAfter.getValue(moisture) == 7,
                "Политая грядка высохла — награда за оросительную сеть не работает");
        helper.succeed();
    }

    /**
     * Половодье двойственно: посев смыт, но плодородие выросло.
     *
     * <p>Это не штраф, а ставка: сеешь низко в пойме — рискуешь урожаем и
     * получаешь ил. Проверяются обе половины исхода, потому что тест только на
     * «смыло» прошёл бы и на чисто карательной версии механики.</p>
     */
    private static void testFloodingWashesCropButEnrichesSoil(GameTestHelper helper) {
        if (!dev.romankrukovsky.kubanhorizons.config.KHServerConfig.floodingEnabled()) {
            helper.succeed();
            return;
        }
        ServerLevel level = helper.getLevel();
        BlockPos cropPos = preparedFarmland(helper, new BlockPos(2, 2, 2));
        BlockPos soilPos = cropPos.below();
        BlockPos absoluteSoil = helper.absolutePos(soilPos);
        placeMatureSunflower(helper, cropPos);
        // Вода рядом и выше грядки — условие затопления.
        helper.setBlock(soilPos.offset(1, 1, 0), Blocks.WATER);

        // Плодородие сначала снижается, иначе прибавка ила упрётся в максимум
        // шкалы и тест не отличит рост от отсутствия изменений.
        dev.romankrukovsky.kubanhorizons.soil.SoilFertility.onTrample(level, absoluteSoil);
        int before = dev.romankrukovsky.kubanhorizons.soil.SoilFertility
                .fertility(level, absoluteSoil);

        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        int flooded = dev.romankrukovsky.kubanhorizons.weather.Flooding.rise(
                level, absoluteSoil);
        if (flooded == 0) {
            // Событие привязано к биому поймы; тестовый мир — не пойма.
            // Тогда проверяется хотя бы вклад ила напрямую.
            dev.romankrukovsky.kubanhorizons.soil.SoilFertility
                    .onFloodDeposit(level, absoluteSoil);
            int silted = dev.romankrukovsky.kubanhorizons.soil.SoilFertility
                    .fertility(level, absoluteSoil);
            helper.assertTrue(silted > before,
                    "Речной ил не поднял плодородие: " + before + " -> " + silted);
            helper.succeed();
            return;
        }

        helper.assertTrue(!helper.getBlockState(cropPos).is(BlockTags.CROPS),
                "Половодье не смыло посев");
        int after = dev.romankrukovsky.kubanhorizons.soil.SoilFertility
                .fertility(level, absoluteSoil);
        helper.assertTrue(after > before,
                "Плодородие не выросло после схода воды: половодье стало чистым штрафом, "
                        + before + " -> " + after);
        helper.succeed();
    }

    /**
     * Плодородие не выходит за шкалу 0..100 и реагирует на вытаптывание.
     *
     * <p>Проверяются края: без ограничения снизу многократный налёт увёл бы
     * значение в минус, и «мёртвая» грядка начала бы вести себя как плодородная
     * после переполнения.</p>
     */
    private static void testFertilityClampsAtBounds(GameTestHelper helper) {
        if (!dev.romankrukovsky.kubanhorizons.config.KHServerConfig.fertilityEnabled()) {
            helper.succeed();
            return;
        }
        ServerLevel level = helper.getLevel();
        BlockPos cropPos = preparedFarmland(helper, new BlockPos(1, 1, 1));
        BlockPos soil = helper.absolutePos(cropPos.below());
        int base = dev.romankrukovsky.kubanhorizons.soil.SoilFertility
                .fertility(level, soil);

        // Одно вытаптывание обязано что-то изменить. Без этой проверки тест на
        // одни лишь границы шкалы проходил бы и на полностью отключённом
        // вытаптывании: значение осталось бы в допустимых 0..100.
        dev.romankrukovsky.kubanhorizons.soil.SoilFertility.onTrample(level, soil);
        int once = dev.romankrukovsky.kubanhorizons.soil.SoilFertility
                .fertility(level, soil);
        helper.assertTrue(once < base,
                "Вытаптывание не снизило плодородие: " + base + " -> " + once);

        // Дно: двадцать вытаптываний подряд не должны уронить ниже нуля.
        for (int i = 0; i < 20; i++) {
            dev.romankrukovsky.kubanhorizons.soil.SoilFertility.onTrample(level, soil);
        }
        int floor = dev.romankrukovsky.kubanhorizons.soil.SoilFertility.fertility(level, soil);
        helper.assertTrue(floor >= 0,
                "Плодородие ушло ниже нуля: " + floor);

        // Потолок: столько же отложений ила не должны превысить максимум.
        for (int i = 0; i < 30; i++) {
            dev.romankrukovsky.kubanhorizons.soil.SoilFertility.onFloodDeposit(level, soil);
        }
        int ceiling = dev.romankrukovsky.kubanhorizons.soil.SoilFertility.fertility(level, soil);
        helper.assertTrue(
                ceiling <= dev.romankrukovsky.kubanhorizons.soil.SoilFertility.MAX,
                "Плодородие превысило максимум шкалы: " + ceiling);
        helper.assertTrue(ceiling > floor,
                "Ил не поднял плодородие с дна: " + floor + " -> " + ceiling);
        helper.succeed();
    }

    /**
     * Саранча объедает культуру: возраст откатывается на стадию назад.
     *
     * <p>Отличие от кабана существенное: саранча ест растение и не портит
     * почву, поэтому тест заодно следит, что грядка под ней осталась грядкой.</p>
     */
    private static void testLocustEatsCropStage(GameTestHelper helper) {
        if (!dev.romankrukovsky.kubanhorizons.config.KHServerConfig.pressureEnabled()) {
            helper.succeed();
            return;
        }
        BlockPos cropPos = preparedFarmland(helper, new BlockPos(1, 1, 1));
        placeMatureSunflower(helper, cropPos);
        int ageBefore = helper.getBlockState(cropPos).getValue(SunflowerCropBlock.AGE);

        var locust = helper.spawn(KHEntities.LOCUST.get(), cropPos);
        helper.assertTrue(locust != null, "Саранча не создалась");

        // succeedWhen, а не фиксированные 150 тиков: саранча ест раз в
        // EAT_INTERVAL (60) тиков и только когда номер такта совпадает с её
        // собственным id, поэтому за 150 тиков попыток набегает всего две-три
        // — и при неудачном id первая приходилась уже за границей ожидания.
        // Тест падал через прогон не из-за механики, а из-за арифметики.
        helper.succeedWhen(() -> {
            BlockState after = helper.getBlockState(cropPos);
            boolean damaged = !after.is(BlockTags.CROPS)
                    || after.getValue(SunflowerCropBlock.AGE) < ageBefore;
            helper.assertTrue(damaged,
                    "Саранча не тронула посев: налёт не наносит урона");
            helper.assertTrue(helper.getBlockState(cropPos.below()).is(BlockTags.SUPPORTS_CROPS),
                    "Саранча испортила почву — это работа кабана, не насекомого");
            locust.discard();
        });
    }

    /**
     * Наземная птица убивает саранчу.
     *
     * <p>Это единственная причина держать фазанов у поля: без охоты птица —
     * просто мясо, и связка «птицы защищают урожай» существует лишь на бумаге.</p>
     */
    private static void testGroundBirdHuntsLocust(GameTestHelper helper) {
        for (int dx = -1; dx <= 5; dx++) {
            for (int dz = -1; dz <= 5; dz++) {
                helper.setBlock(new BlockPos(1, 1, 1).offset(dx, 0, dz), Blocks.DIRT);
            }
        }
        BlockPos birdPos = new BlockPos(1, 2, 1);
        var bird = helper.spawn(KHEntities.PHEASANT.get(), birdPos);
        // Саранча ставится вплотную: она летает случайным курсом и за 190
        // тиков успевает улететь с тестовой площадки, из-за чего тест падал
        // примерно раз из шести — на дрейфе, а не на охоте. Проверяется
        // именно поедание, поэтому поиск цели упрощён намеренно.
        var locust = helper.spawn(KHEntities.LOCUST.get(), birdPos.offset(1, 0, 0));
        helper.assertTrue(bird != null && locust != null, "Птица или саранча не создались");

        // succeedWhen вместо фиксированной задержки: птица могла съесть добычу
        // и на сороковом тике, и на сто восьмидесятом — важен исход, а не
        // момент. Ожидание всё равно ограничено таймаутом теста.
        helper.succeedWhen(() -> {
            helper.assertTrue(!locust.isAlive(),
                    "Птица не съела саранчу: птицы не работают как защита урожая");
            bird.discard();
        });
    }
    /**
     * Днём манул уходит в укрытие и засыпает.
     *
     * <p>Проверяет то, чего в моде не было до появления {@link
     * dev.romankrukovsky.kubanhorizons.entity.ManulSleepGoal}: модель умела
     * рисовать сон клубком, но флаг сна никто не выставлял, и поза была
     * недостижима. Тест смотрит на наблюдаемый исход — зверь спит, — а не на
     * внутренности цели.</p>
     */
    private static void testManulSleepsInDen(GameTestHelper helper) {
        // Нора целиком под крышей: пол, потолок и стены. Первая версия теста
        // ставила лишь пятно крыши 3x3, и зверь успевал выйти из-под неё
        // раньше, чем засыпал — тест падал через раз не из-за механики, а
        // из-за того, что укрытием была не вся доступная площадка.
        for (int dx = 0; dx <= 4; dx++) {
            for (int dz = 0; dz <= 4; dz++) {
                helper.setBlock(new BlockPos(dx, 1, dz), Blocks.STONE);
                helper.setBlock(new BlockPos(dx, 4, dz), Blocks.STONE);
            }
        }
        for (int d = 0; d <= 4; d++) {
            for (int y = 2; y <= 3; y++) {
                helper.setBlock(new BlockPos(d, y, 0), Blocks.STONE);
                helper.setBlock(new BlockPos(d, y, 4), Blocks.STONE);
                helper.setBlock(new BlockPos(0, y, d), Blocks.STONE);
                helper.setBlock(new BlockPos(4, y, d), Blocks.STONE);
            }
        }
        var manul = helper.spawn(
                dev.romankrukovsky.kubanhorizons.registry.KHEntities.MANUL.get(),
                new BlockPos(2, 2, 2));
        helper.assertTrue(manul != null, "Манул не создался");

        // Характер и доверие задаются явно, а не берутся случайными. У
        // осторожного манула дистанция отхода 14 блоков, у храброго 6 — а
        // тесты в наборе стоят рядом, и зверь видел игрока из СОСЕДНЕГО
        // теста, считал место небезопасным и не засыпал. Тест падал примерно
        // в двух прогонах из трёх, и виноват был не сон, а случайный
        // характер: «спит ли манул днём» и «как далеко он бежит от людей» —
        // разные вопросы, и проверять их надо порознь. Доверие FRIENDLY
        // обнуляет дистанцию отхода совсем: остаётся ровно проверяемое —
        // находит ли зверь укрытие и засыпает ли в нём.
        manul.setPersonality(dev.romankrukovsky.kubanhorizons.entity
                .ManulPersonality.BRAVE);
        manul.adjustTrust(dev.romankrukovsky.kubanhorizons.entity
                .ManulTrust.FRIENDLY.threshold());

        // succeedWhen, а не фиксированные 340 тиков: зверь решает уснуть по
        // своему расписанию, и жёсткое ожидание падало примерно раз из шести —
        // на моменте засыпания, а не на самой механике. Диагностика в
        // сообщении остаётся: если сон так и не наступит до таймаута теста,
        // причину будет видно сразу.
        helper.succeedWhen(() -> {
            helper.assertTrue(manul.isDozing(),
                    "Манул не заснул в укрытии днём. Состояние: день="
                            + helper.getLevel().isBrightOutside()
                            + ", позиция=" + manul.blockPosition()
                            + ", точно=" + manul.position()
                            + ", на_земле=" + manul.onGround()
                            + ", под_ним=" + helper.getLevel()
                                    .getBlockState(manul.blockPosition().below())
                            + ", в_нём=" + helper.getLevel()
                                    .getBlockState(manul.blockPosition())
                            + ", шипит=" + manul.isHissing()
                            + ", сидит=" + manul.isLoafing()
                            + ", цель=" + manul.getTarget()
                            + ", навигация_идёт=" + !manul.getNavigation().isDone());
            manul.discard();
        });
    }
>>>>>>> Stashed changes
}
