package dev.romankrukovsky.kubanhorizons.worldgen;

import dev.romankrukovsky.kubanhorizons.util.KHIds;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;
import net.minecraft.world.level.biome.MultiNoiseBiomeSourceParameterList;
import net.minecraft.world.level.biome.MultiNoiseBiomeSourceParameterLists;
import net.minecraft.world.level.biome.TheEndBiomeSource;
import net.minecraft.world.level.dimension.BuiltinDimensionTypes;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.presets.WorldPreset;

import java.util.Map;

/** Мир «Кубанские горизонты», сохраняющий ванильную географию измерений. */
public final class KHWorldPresets {
    public static final ResourceKey<WorldPreset> KUBAN_HORIZONS =
            ResourceKey.create(Registries.WORLD_PRESET, KHIds.of("kuban_horizons"));

    private KHWorldPresets() {
    }

    public static void bootstrap(BootstrapContext<WorldPreset> context) {
        HolderGetter<Biome> biomes = context.lookup(Registries.BIOME);
        HolderGetter<DimensionType> dimensionTypes = context.lookup(Registries.DIMENSION_TYPE);
        HolderGetter<NoiseGeneratorSettings> noiseSettings = context.lookup(Registries.NOISE_SETTINGS);
        HolderGetter<MultiNoiseBiomeSourceParameterList> parameterLists =
                context.lookup(Registries.MULTI_NOISE_BIOME_SOURCE_PARAMETER_LIST);

        Holder<Biome> steppe = biomes.getOrThrow(KHBiomes.KUBAN_STEPPE);
        Holder<Biome> plavni = biomes.getOrThrow(KHBiomes.PLAVNI);
        Holder<Biome> liman = biomes.getOrThrow(KHBiomes.LIMAN);
        Holder<Biome> floodplain = biomes.getOrThrow(KHBiomes.RIVER_FLOODPLAIN);
        Holder<Biome> foothillForest = biomes.getOrThrow(KHBiomes.FOOTHILL_FOREST);
        Holder<Biome> mountainForest = biomes.getOrThrow(KHBiomes.MOUNTAIN_FOREST);
        Holder<Biome> azovCoast = biomes.getOrThrow(KHBiomes.AZOV_COAST);
        Holder<Biome> blackSeaCoast = biomes.getOrThrow(KHBiomes.BLACK_SEA_COAST);
        Holder<Biome> vineyardHills = biomes.getOrThrow(KHBiomes.VINEYARD_HILLS);
        Holder<Biome> teaSlopes = biomes.getOrThrow(KHBiomes.TEA_SLOPES);

        LevelStem overworld = new LevelStem(
                dimensionTypes.getOrThrow(BuiltinDimensionTypes.OVERWORLD),
                new NoiseBasedChunkGenerator(
                        new KubanBiomeSource(
                                parameterLists.getOrThrow(MultiNoiseBiomeSourceParameterLists.OVERWORLD),
                                steppe, plavni, liman, floodplain,
                                foothillForest, mountainForest, azovCoast, blackSeaCoast,
                                vineyardHills, teaSlopes),
                        noiseSettings.getOrThrow(KHNoiseSettings.OVERWORLD)));
        LevelStem nether = new LevelStem(
                dimensionTypes.getOrThrow(BuiltinDimensionTypes.NETHER),
                new NoiseBasedChunkGenerator(
                        MultiNoiseBiomeSource.createFromPreset(
                                parameterLists.getOrThrow(MultiNoiseBiomeSourceParameterLists.NETHER)),
                        noiseSettings.getOrThrow(NoiseGeneratorSettings.NETHER)));
        LevelStem end = new LevelStem(
                dimensionTypes.getOrThrow(BuiltinDimensionTypes.END),
                new NoiseBasedChunkGenerator(
                        TheEndBiomeSource.create(biomes),
                        noiseSettings.getOrThrow(NoiseGeneratorSettings.END)));

        context.register(KUBAN_HORIZONS, new WorldPreset(Map.of(
                LevelStem.OVERWORLD, overworld,
                LevelStem.NETHER, nether,
                LevelStem.END, end)));
    }
}
