package dev.romankrukovsky.kubanhorizons.worldgen;

import dev.romankrukovsky.kubanhorizons.util.KHIds;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BiomeDefaultFeatures;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.worldgen.biome.OverworldBiomes;
import net.minecraft.data.worldgen.placement.AquaticPlacements;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.Musics;
import net.minecraft.world.attribute.BackgroundMusic;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeGenerationSettings;
import net.minecraft.world.level.biome.BiomeSpecialEffects;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.carver.ConfiguredWorldCarver;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

/** Datapack-биомы Kuban Horizons. */
public final class KHBiomes {
    /** Цвет воды ванильных рек и равнин ({@code OverworldBiomes.NORMAL_WATER_COLOR}). */
    private static final int NORMAL_WATER_COLOR = 4159204;

    public static final ResourceKey<Biome> KUBAN_STEPPE =
            ResourceKey.create(Registries.BIOME, KHIds.of("kuban_steppe"));
    public static final ResourceKey<Biome> PLAVNI =
            ResourceKey.create(Registries.BIOME, KHIds.of("plavni"));
    public static final ResourceKey<Biome> LIMAN =
            ResourceKey.create(Registries.BIOME, KHIds.of("liman"));
    public static final ResourceKey<Biome> RIVER_FLOODPLAIN =
            ResourceKey.create(Registries.BIOME, KHIds.of("river_floodplain"));

    private KHBiomes() {
    }

    public static void bootstrap(BootstrapContext<Biome> context) {
        HolderGetter<PlacedFeature> features = context.lookup(Registries.PLACED_FEATURE);
        HolderGetter<ConfiguredWorldCarver<?>> carvers = context.lookup(Registries.CONFIGURED_CARVER);

        // Солнечная степь наследует полный безопасный набор генерации равнин:
        // руды, озёра, траву, цветы, животных и редкие поля подсолнухов.
        context.register(KUBAN_STEPPE, OverworldBiomes.plains(features, carvers, true, false, false));
        context.register(PLAVNI, OverworldBiomes.swamp(features, carvers));
        context.register(LIMAN, OverworldBiomes.swamp(features, carvers));
        context.register(RIVER_FLOODPLAIN, riverFloodplain(features, carvers));
    }

    /**
     * Пойма реки — заливной луг кубанских рек.
     *
     * <p>Строится по образцу ванильной {@code OverworldBiomes.river}:
     * прибрежные деревья и кусты, цветы, трава, грибы и речная растительность.
     * Речная фауна ванильная (кальмар, лосось, утопленники), плюс домашний
     * скот заливных пастбищ.
     * Приватные хелперы {@code baseBiome}/{@code globalOverworldGeneration}
     * ванильного класса недоступны извне, поэтому их состав воспроизведён
     * здесь публичными вызовами {@link BiomeDefaultFeatures}.</p>
     */
    private static Biome riverFloodplain(HolderGetter<PlacedFeature> features,
            HolderGetter<ConfiguredWorldCarver<?>> carvers) {
        MobSpawnSettings.Builder mobs = new MobSpawnSettings.Builder()
                .addSpawn(MobCategory.WATER_CREATURE, 2,
                        new MobSpawnSettings.SpawnerData(EntityTypes.SQUID, 1, 4))
                .addSpawn(MobCategory.WATER_AMBIENT, 5,
                        new MobSpawnSettings.SpawnerData(EntityTypes.SALMON, 1, 5));
        // Заливные луга — исторические пастбища поймы.
        BiomeDefaultFeatures.farmAnimals(mobs);
        BiomeDefaultFeatures.commonSpawns(mobs);
        mobs.addSpawn(MobCategory.MONSTER, 100,
                new MobSpawnSettings.SpawnerData(EntityTypes.DROWNED, 1, 1));

        BiomeGenerationSettings.Builder generation =
                new BiomeGenerationSettings.Builder(features, carvers);
        // Состав ванильного globalOverworldGeneration.
        BiomeDefaultFeatures.addDefaultCarversAndLakes(generation);
        BiomeDefaultFeatures.addDefaultCrystalFormations(generation);
        BiomeDefaultFeatures.addDefaultMonsterRoom(generation);
        BiomeDefaultFeatures.addDefaultUndergroundVariety(generation);
        BiomeDefaultFeatures.addDefaultSprings(generation);
        BiomeDefaultFeatures.addSurfaceFreezing(generation);

        BiomeDefaultFeatures.addDefaultOres(generation);
        BiomeDefaultFeatures.addDefaultSoftDisks(generation);
        // Порядок намеренно совпадает с ванильной незамёрзшей рекой. FeatureSorter
        // объединяет все возможные биомы opt-in preset, поэтому общие placed
        // features обязаны сохранять глобально совместимый относительный порядок.
        BiomeDefaultFeatures.addWaterTrees(generation);
        BiomeDefaultFeatures.addBushes(generation);
        BiomeDefaultFeatures.addDefaultFlowers(generation);
        BiomeDefaultFeatures.addDefaultGrass(generation);
        BiomeDefaultFeatures.addDefaultMushrooms(generation);
        BiomeDefaultFeatures.addDefaultExtraVegetation(generation, true);
        generation.addFeature(GenerationStep.Decoration.VEGETAL_DECORATION,
                AquaticPlacements.SEAGRASS_RIVER);

        float temperature = 0.7F;
        return new Biome.BiomeBuilder()
                .hasPrecipitation(true)
                .temperature(temperature)
                .downfall(0.8F)
                .setAttribute(EnvironmentAttributes.SKY_COLOR,
                        OverworldBiomes.calculateSkyColor(temperature))
                .setAttribute(EnvironmentAttributes.BACKGROUND_MUSIC,
                        BackgroundMusic.OVERWORLD.withUnderwater(Musics.UNDER_WATER))
                .specialEffects(new BiomeSpecialEffects.Builder()
                        .waterColor(NORMAL_WATER_COLOR)
                        .build())
                .mobSpawnSettings(mobs.build())
                .generationSettings(generation.build())
                .build();
    }
}
