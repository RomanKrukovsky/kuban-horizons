package dev.romankrukovsky.kubanhorizons.worldgen;

import dev.romankrukovsky.kubanhorizons.util.KHIds;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
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
 * холмы (сухие «южные» склоны).</p>
 */
public final class KHBiomeModifiers {
    public static final ResourceKey<BiomeModifier> ADD_WILD_TEA =
            createKey("add_wild_tea");
    public static final ResourceKey<BiomeModifier> ADD_WILD_TOMATO =
            createKey("add_wild_tomato");
    public static final ResourceKey<BiomeModifier> ADD_WILD_GRAPE =
            createKey("add_wild_grape");

    private KHBiomeModifiers() {
    }

    private static ResourceKey<BiomeModifier> createKey(String name) {
        return ResourceKey.create(NeoForgeRegistries.Keys.BIOME_MODIFIERS, KHIds.of(name));
    }

    public static void bootstrap(BootstrapContext<BiomeModifier> context) {
        HolderGetter<Biome> biomes = context.lookup(Registries.BIOME);
        HolderGetter<PlacedFeature> features = context.lookup(Registries.PLACED_FEATURE);

        context.register(ADD_WILD_TEA, new BiomeModifiers.AddFeaturesBiomeModifier(
                biomes.getOrThrow(BiomeTags.IS_JUNGLE),
                HolderSet.direct(features.getOrThrow(KHPlacedFeatures.WILD_TEA_PLACED)),
                GenerationStep.Decoration.VEGETAL_DECORATION));

        context.register(ADD_WILD_TOMATO, new BiomeModifiers.AddFeaturesBiomeModifier(
                HolderSet.direct(
                        biomes.getOrThrow(Biomes.PLAINS),
                        biomes.getOrThrow(Biomes.SUNFLOWER_PLAINS),
                        biomes.getOrThrow(Biomes.SAVANNA)),
                HolderSet.direct(features.getOrThrow(KHPlacedFeatures.WILD_TOMATO_PLACED)),
                GenerationStep.Decoration.VEGETAL_DECORATION));

        context.register(ADD_WILD_GRAPE, new BiomeModifiers.AddFeaturesBiomeModifier(
                HolderSet.direct(
                        biomes.getOrThrow(Biomes.SAVANNA),
                        biomes.getOrThrow(Biomes.SAVANNA_PLATEAU),
                        biomes.getOrThrow(Biomes.WOODED_BADLANDS)),
                HolderSet.direct(features.getOrThrow(KHPlacedFeatures.WILD_GRAPE_PLACED)),
                GenerationStep.Decoration.VEGETAL_DECORATION));
    }
}
