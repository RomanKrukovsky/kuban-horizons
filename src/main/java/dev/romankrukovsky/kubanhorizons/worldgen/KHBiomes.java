package dev.romankrukovsky.kubanhorizons.worldgen;

import dev.romankrukovsky.kubanhorizons.registry.KHSounds;
import dev.romankrukovsky.kubanhorizons.util.KHIds;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BiomeDefaultFeatures;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.worldgen.biome.OverworldBiomes;
import net.minecraft.data.worldgen.placement.AquaticPlacements;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.Musics;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.attribute.AmbientAdditionsSettings;
import net.minecraft.world.attribute.AmbientMoodSettings;
import net.minecraft.world.attribute.AmbientSounds;
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
import net.neoforged.neoforge.registries.DeferredHolder;

import java.util.List;
import java.util.Optional;

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
        context.register(KUBAN_STEPPE, withAmbience(
                OverworldBiomes.plains(features, carvers, true, false, false),
                KHSounds.AMBIENT_STEPPE_LOOP, KHSounds.AMBIENT_STEPPE_ADDITIONS,
                STEPPE_ADDITIONS_CHANCE));
        context.register(PLAVNI, withAmbience(
                OverworldBiomes.swamp(features, carvers),
                KHSounds.AMBIENT_PLAVNI_LOOP, KHSounds.AMBIENT_PLAVNI_ADDITIONS,
                REED_ADDITIONS_CHANCE));
        context.register(LIMAN, withAmbience(
                OverworldBiomes.swamp(features, carvers),
                KHSounds.AMBIENT_LIMAN_LOOP, KHSounds.AMBIENT_LIMAN_ADDITIONS,
                OPEN_WATER_ADDITIONS_CHANCE));
        context.register(RIVER_FLOODPLAIN, withAmbience(
                riverFloodplain(features, carvers),
                KHSounds.AMBIENT_FLOODPLAIN_LOOP, KHSounds.AMBIENT_FLOODPLAIN_ADDITIONS,
                FLOODPLAIN_ADDITIONS_CHANCE));
    }

    /**
     * Шанс редкого вкрапления за тик, по образцу ванильных биомов Нижнего
     * мира ({@code 0.0111} — примерно раз в полторы минуты).
     *
     * <p>Разведены по биомам не ради разнообразия чисел, а по смыслу: в
     * открытой степи посвист ветра — обычное дело, а одинокий крик над
     * пустым лиманом должен оставаться редким, иначе простор превращается
     * в птичник.</p>
     */
    private static final double STEPPE_ADDITIONS_CHANCE = 0.0111D;
    private static final double FLOODPLAIN_ADDITIONS_CHANCE = 0.0090D;
    private static final double REED_ADDITIONS_CHANCE = 0.0075D;
    private static final double OPEN_WATER_ADDITIONS_CHANCE = 0.0055D;

    /**
     * Задержка настроения в тиках и радиус поиска тёмного блока.
     *
     * <p>Значения ванильные ({@code AmbientMoodSettings.LEGACY_CAVE_SETTINGS}):
     * пещера под степью обязана звучать как пещера, а не как отдельный
     * биом со своими правилами.</p>
     */
    private static final int MOOD_TICK_DELAY = 6000;
    private static final int MOOD_SEARCH_EXTENT = 8;
    private static final double MOOD_OFFSET = 2.0D;

    /**
     * Добавляет биому голос: петлю, настроение и редкие вкрапления.
     *
     * <p>В MC 26.2 звук ушёл из {@code BiomeSpecialEffects} — этот record
     * теперь хранит только цвета. Атмосфера задаётся атрибутом окружения
     * {@link EnvironmentAttributes#AMBIENT_SOUNDS} со значением
     * {@link AmbientSounds}(петля, настроение, вкрапления); клиент читает
     * его в {@code BiomeAmbientSoundsHandler}. Ванильный образец —
     * биомы Нижнего мира в {@code NetherBiomes}.</p>
     *
     * <p>Настроение подставляется ванильное пещерное, и это не украшение, а
     * необходимость. Слой биома работает как {@code override}: заданный на
     * биоме {@code AMBIENT_SOUNDS} целиком заменяет значение, которое
     * измерение выставило для всего Верхнего мира — а там лежит именно
     * {@code AmbientSounds.LEGACY_CAVE_SETTINGS}. Указать одну петлю без
     * настроения означало бы отключить «жуткий звук» в каждой пещере под
     * нашими биомами, то есть починить тишину в степи и сломать её под
     * землёй. Ровно так же поступают биомы Нижнего мира: каждый несёт
     * собственное настроение рядом с петлёй.</p>
     *
     * <p>Пересобирается через {@code BiomeBuilder}, потому что ванильные
     * {@code OverworldBiomes.plains}/{@code swamp} возвращают готовый
     * {@link Biome}, а не строитель. Климат берётся через
     * {@code getModifiedClimateSettings()}: у {@code downfall} нет
     * публичного геттера, и без этого пути влажность биома потерялась бы
     * при пересборке. {@code putAttributes} сохраняет всё, что ванильный
     * биом уже выставил (цвет неба, туман, музыку), и добавляет к этому
     * звук — а не затирает набор целиком.</p>
     */
    private static Biome withAmbience(Biome biome,
            DeferredHolder<SoundEvent, SoundEvent> loop,
            DeferredHolder<SoundEvent, SoundEvent> additions,
            double additionsChance) {
        Biome.ClimateSettings climate = biome.getModifiedClimateSettings();
        AmbientSounds sounds = new AmbientSounds(
                Optional.of(loop),
                Optional.of(new AmbientMoodSettings(SoundEvents.AMBIENT_CAVE,
                        MOOD_TICK_DELAY, MOOD_SEARCH_EXTENT, MOOD_OFFSET)),
                List.of(new AmbientAdditionsSettings(additions, additionsChance)));
        return new Biome.BiomeBuilder()
                .hasPrecipitation(climate.hasPrecipitation())
                .temperature(climate.temperature())
                .temperatureAdjustment(climate.temperatureModifier())
                .downfall(climate.downfall())
                .putAttributes(biome.getAttributes())
                .setAttribute(EnvironmentAttributes.AMBIENT_SOUNDS, sounds)
                .specialEffects(biome.getSpecialEffects())
                .mobSpawnSettings(biome.getMobSettings())
                .generationSettings(biome.getGenerationSettings())
                .build();
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
