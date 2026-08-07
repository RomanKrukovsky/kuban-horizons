package dev.romankrukovsky.kubanhorizons.worldgen;

import dev.romankrukovsky.kubanhorizons.registry.KHEntities;
import dev.romankrukovsky.kubanhorizons.util.KHIds;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.random.Weighted;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.neoforged.neoforge.common.world.BiomeModifier;
import net.neoforged.neoforge.common.world.BiomeModifiers;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * Модификаторы биомов: добавление диких культур в ванильные биомы.
 *
 * <p>Дикий чай — влажные тёплые леса (джунгли, тёмный лес); дикие
 * томаты — равнины и саванны; одичавший виноград — саванны и лесистые
 * холмы (сухие «южные» склоны); дикий рис — мелководье поймы реки.</p>
 */
public final class KHBiomeModifiers {
    public static final ResourceKey<BiomeModifier> ADD_WILD_TEA =
            createKey("add_wild_tea");
    public static final ResourceKey<BiomeModifier> ADD_WILD_TOMATO =
            createKey("add_wild_tomato");
    public static final ResourceKey<BiomeModifier> ADD_WILD_GRAPE =
            createKey("add_wild_grape");
    public static final ResourceKey<BiomeModifier> ADD_WILD_RICE =
            createKey("add_wild_rice");
    public static final ResourceKey<BiomeModifier> ADD_PHEASANT_SPAWNS =
            createKey("add_pheasant_spawns");
    public static final ResourceKey<BiomeModifier> ADD_QUAIL_SPAWNS =
            createKey("add_quail_spawns");
    public static final ResourceKey<BiomeModifier> ADD_WILD_BOAR_SPAWNS =
            createKey("add_wild_boar_spawns");
    public static final ResourceKey<BiomeModifier> ADD_NUTRIA_SPAWNS =
            createKey("add_nutria_spawns");
    public static final ResourceKey<BiomeModifier> ADD_CAUCASIAN_BEE_SPAWNS =
            createKey("add_caucasian_bee_spawns");
    public static final ResourceKey<BiomeModifier> ADD_GULL_SPAWNS =
            createKey("add_gull_spawns");
    public static final ResourceKey<BiomeModifier> ADD_HERON_SPAWNS =
            createKey("add_heron_spawns");
    public static final ResourceKey<BiomeModifier> ADD_STURGEON_SPAWNS =
            createKey("add_sturgeon_spawns");

    private KHBiomeModifiers() {
    }

    private static ResourceKey<BiomeModifier> createKey(String name) {
        return ResourceKey.create(NeoForgeRegistries.Keys.BIOME_MODIFIERS, KHIds.of(name));
    }

    public static void bootstrap(BootstrapContext<BiomeModifier> context) {
        HolderGetter<Biome> biomes = context.lookup(Registries.BIOME);
        HolderGetter<PlacedFeature> features = context.lookup(Registries.PLACED_FEATURE);

        context.register(ADD_WILD_TEA, new BiomeModifiers.AddFeaturesBiomeModifier(
                HolderSet.direct(
                        biomes.getOrThrow(Biomes.JUNGLE),
                        biomes.getOrThrow(Biomes.SPARSE_JUNGLE),
                        biomes.getOrThrow(Biomes.BAMBOO_JUNGLE),
                        biomes.getOrThrow(KHBiomes.PLAVNI),
                        biomes.getOrThrow(KHBiomes.LIMAN)),
                HolderSet.direct(features.getOrThrow(KHPlacedFeatures.WILD_TEA_PLACED)),
                GenerationStep.Decoration.VEGETAL_DECORATION));

        context.register(ADD_WILD_TOMATO, new BiomeModifiers.AddFeaturesBiomeModifier(
                HolderSet.direct(
                        biomes.getOrThrow(Biomes.PLAINS),
                        biomes.getOrThrow(Biomes.SUNFLOWER_PLAINS),
                        biomes.getOrThrow(Biomes.SAVANNA),
                        biomes.getOrThrow(KHBiomes.KUBAN_STEPPE)),
                HolderSet.direct(features.getOrThrow(KHPlacedFeatures.WILD_TOMATO_PLACED)),
                GenerationStep.Decoration.VEGETAL_DECORATION));

        context.register(ADD_WILD_GRAPE, new BiomeModifiers.AddFeaturesBiomeModifier(
                HolderSet.direct(
                        biomes.getOrThrow(Biomes.SAVANNA),
                        biomes.getOrThrow(Biomes.SAVANNA_PLATEAU),
                        biomes.getOrThrow(Biomes.WOODED_BADLANDS),
                        biomes.getOrThrow(KHBiomes.KUBAN_STEPPE)),
                HolderSet.direct(features.getOrThrow(KHPlacedFeatures.WILD_GRAPE_PLACED)),
                GenerationStep.Decoration.VEGETAL_DECORATION));

        // Дикий рис — только пойма реки: мелководье с илистым дном.
        context.register(ADD_WILD_RICE, new BiomeModifiers.AddFeaturesBiomeModifier(
                HolderSet.direct(biomes.getOrThrow(KHBiomes.RIVER_FLOODPLAIN)),
                HolderSet.direct(features.getOrThrow(KHPlacedFeatures.WILD_RICE_PLACED)),
                GenerationStep.Decoration.VEGETAL_DECORATION));

        context.register(ADD_PHEASANT_SPAWNS, BiomeModifiers.AddSpawnsBiomeModifier.singleSpawn(
                HolderSet.direct(
                        biomes.getOrThrow(KHBiomes.KUBAN_STEPPE),
                        biomes.getOrThrow(KHBiomes.RIVER_FLOODPLAIN)),
                new Weighted<>(new MobSpawnSettings.SpawnerData(KHEntities.PHEASANT.get(), 1, 3), 8)));

        context.register(ADD_QUAIL_SPAWNS, BiomeModifiers.AddSpawnsBiomeModifier.singleSpawn(
                HolderSet.direct(biomes.getOrThrow(KHBiomes.KUBAN_STEPPE)),
                new Weighted<>(new MobSpawnSettings.SpawnerData(KHEntities.QUAIL.get(), 3, 6), 10)));

        // Ниже — расселение остальной фауны. Без этих модификаторов восемь
        // существ существовали только в спавн-яйце: в мире их было не встретить,
        // а значит ни давление на ферму, ни оживление плавней не работали.
        // Веса подобраны так, чтобы полезные и нейтральные виды встречались
        // заметно чаще вредителей: кабан на ферме — событие, а не фон.

        // Кабан: лес и пойма, откуда он и выходит на поля. В степи не живёт —
        // ему нужно укрытие, иначе он караулил бы грядки круглосуточно.
        context.register(ADD_WILD_BOAR_SPAWNS, BiomeModifiers.AddSpawnsBiomeModifier.singleSpawn(
                HolderSet.direct(
                        biomes.getOrThrow(Biomes.FOREST),
                        biomes.getOrThrow(Biomes.BIRCH_FOREST),
                        biomes.getOrThrow(Biomes.DARK_FOREST),
                        biomes.getOrThrow(KHBiomes.RIVER_FLOODPLAIN)),
                new Weighted<>(new MobSpawnSettings.SpawnerData(
                        KHEntities.WILD_BOAR.get(), 1, 2), 4)));

        // Нутрия: плавни и лиманы — полуводный грызун живёт только у воды.
        context.register(ADD_NUTRIA_SPAWNS, BiomeModifiers.AddSpawnsBiomeModifier.singleSpawn(
                HolderSet.direct(
                        biomes.getOrThrow(KHBiomes.PLAVNI),
                        biomes.getOrThrow(KHBiomes.LIMAN)),
                new Weighted<>(new MobSpawnSettings.SpawnerData(
                        KHEntities.NUTRIA.get(), 2, 4), 8)));

        // Пчела: цветущая степь и предгорья. Единственный полезный агент
        // давления, поэтому вес высокий — её должно быть легко найти.
        context.register(ADD_CAUCASIAN_BEE_SPAWNS,
                BiomeModifiers.AddSpawnsBiomeModifier.singleSpawn(
                        HolderSet.direct(
                                biomes.getOrThrow(KHBiomes.KUBAN_STEPPE),
                                biomes.getOrThrow(Biomes.MEADOW),
                                biomes.getOrThrow(Biomes.SUNFLOWER_PLAINS),
                                biomes.getOrThrow(Biomes.FLOWER_FOREST)),
                        new Weighted<>(new MobSpawnSettings.SpawnerData(
                                KHEntities.CAUCASIAN_BEE.get(), 2, 4), 10)));

        // Чайка: побережья и лиманы — примета берега.
        context.register(ADD_GULL_SPAWNS, BiomeModifiers.AddSpawnsBiomeModifier.singleSpawn(
                HolderSet.direct(
                        biomes.getOrThrow(Biomes.BEACH),
                        biomes.getOrThrow(Biomes.STONY_SHORE),
                        biomes.getOrThrow(KHBiomes.LIMAN)),
                new Weighted<>(new MobSpawnSettings.SpawnerData(
                        KHEntities.GULL.get(), 2, 4), 10)));

        // Цапля: плавни. Держится по одной — территориальный хищник отмели.
        context.register(ADD_HERON_SPAWNS, BiomeModifiers.AddSpawnsBiomeModifier.singleSpawn(
                HolderSet.direct(
                        biomes.getOrThrow(KHBiomes.PLAVNI),
                        biomes.getOrThrow(KHBiomes.RIVER_FLOODPLAIN)),
                new Weighted<>(new MobSpawnSettings.SpawnerData(
                        KHEntities.HERON.get(), 1, 2), 6)));

        // Осётр: пойма и лиманы. Стайная рыба (AbstractSchoolingFish),
        // поэтому группа крупнее и категория WATER_AMBIENT.
        context.register(ADD_STURGEON_SPAWNS, BiomeModifiers.AddSpawnsBiomeModifier.singleSpawn(
                HolderSet.direct(
                        biomes.getOrThrow(KHBiomes.RIVER_FLOODPLAIN),
                        biomes.getOrThrow(KHBiomes.LIMAN)),
                new Weighted<>(new MobSpawnSettings.SpawnerData(
                        KHEntities.STURGEON.get(), 2, 4), 8)));
    }
}
