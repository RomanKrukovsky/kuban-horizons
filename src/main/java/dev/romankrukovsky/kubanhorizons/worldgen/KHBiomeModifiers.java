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
    }
}
