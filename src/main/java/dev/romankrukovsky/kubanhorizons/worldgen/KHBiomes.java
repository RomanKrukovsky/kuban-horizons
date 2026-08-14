package dev.romankrukovsky.kubanhorizons.worldgen;

import dev.romankrukovsky.kubanhorizons.util.KHIds;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BiomeDefaultFeatures;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.worldgen.biome.OverworldBiomes;
import net.minecraft.data.worldgen.placement.AquaticPlacements;
import net.minecraft.resources.ResourceKey;
<<<<<<< Updated upstream
import net.minecraft.sounds.Musics;
import net.minecraft.world.attribute.BackgroundMusic;
=======
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.attribute.AmbientAdditionsSettings;
import net.minecraft.world.attribute.AmbientMoodSettings;
import net.minecraft.world.attribute.AmbientSounds;
>>>>>>> Stashed changes
import net.minecraft.world.attribute.EnvironmentAttributes;
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

    // --- Второй пояс биомов: рельеф Кубани от моря до Кавказа ---
    //
    // До них мир состоял из четырёх биомов, и один из них — степь — забирал
    // себе всё, что не болото и не река: и горы, и леса, и пляжи. Форма
    // рельефа от ванили оставалась, но подпись и цвет были одни на всё.
    // Эти шесть биомов разбирают степной «всё остальное» по реальным
    // географическим поясам края: море → берег → равнина → предгорья →
    // горы, плюс два особых южных пояса — виноградный и чайный.

    /** Предгорный лес — дубово-грабовые леса подножия Кавказа. */
    public static final ResourceKey<Biome> FOOTHILL_FOREST =
            ResourceKey.create(Registries.BIOME, KHIds.of("foothill_forest"));
    /** Горный лес — пихтово-буковый пояс Кавказского заповедника. */
    public static final ResourceKey<Biome> MOUNTAIN_FOREST =
            ResourceKey.create(Registries.BIOME, KHIds.of("mountain_forest"));
    /** Азовское побережье — ракушечные отмели самого мелкого моря мира. */
    public static final ResourceKey<Biome> AZOV_COAST =
            ResourceKey.create(Registries.BIOME, KHIds.of("azov_coast"));
    /** Черноморское побережье — галечные обрывы и глубокая вода. */
    public static final ResourceKey<Biome> BLACK_SEA_COAST =
            ResourceKey.create(Registries.BIOME, KHIds.of("black_sea_coast"));
    /** Виноградные холмы — сухие южные склоны Тамани и Анапы. */
    public static final ResourceKey<Biome> VINEYARD_HILLS =
            ResourceKey.create(Registries.BIOME, KHIds.of("vineyard_hills"));
    /** Чайные склоны — самые северные чайные плантации в мире. */
    public static final ResourceKey<Biome> TEA_SLOPES =
            ResourceKey.create(Registries.BIOME, KHIds.of("tea_slopes"));

    private KHBiomes() {
    }

    public static void bootstrap(BootstrapContext<Biome> context) {
        HolderGetter<PlacedFeature> features = context.lookup(Registries.PLACED_FEATURE);
        HolderGetter<ConfiguredWorldCarver<?>> carvers = context.lookup(Registries.CONFIGURED_CARVER);

        // Солнечная степь наследует полный безопасный набор генерации равнин:
        // руды, озёра, траву, цветы, животных и редкие поля подсолнухов.
<<<<<<< Updated upstream
        context.register(KUBAN_STEPPE, OverworldBiomes.plains(features, carvers, true, false, false));
        context.register(PLAVNI, OverworldBiomes.swamp(features, carvers));
        context.register(LIMAN, OverworldBiomes.swamp(features, carvers));
        context.register(RIVER_FLOODPLAIN, riverFloodplain(features, carvers));
=======
        context.register(KUBAN_STEPPE, withAmbience(
                recolour(OverworldBiomes.plains(features, carvers, true, false, false),
                        new BiomeSpecialEffects.Builder()
                                .waterColor(NORMAL_WATER_COLOR)
                                .grassColorOverride(STEPPE_GRASS)
                                .foliageColorOverride(0x6F8F35)
                                .dryFoliageColorOverride(BURNT_GRASS)
                                .build()),
                KHSounds.AMBIENT_STEPPE_LOOP, KHSounds.AMBIENT_STEPPE_ADDITIONS,
                STEPPE_ADDITIONS_CHANCE));
        context.register(PLAVNI, withAmbience(
                recolour(OverworldBiomes.swamp(features, carvers),
                        new BiomeSpecialEffects.Builder()
                                .waterColor(0x476F63)
                                .grassColorOverride(0x4F7A3D)
                                .foliageColorOverride(0x3F6B33)
                                .dryFoliageColorOverride(0x7A7942)
                                .build()),
                KHSounds.AMBIENT_PLAVNI_LOOP, KHSounds.AMBIENT_PLAVNI_ADDITIONS,
                REED_ADDITIONS_CHANCE));
        context.register(LIMAN, withAmbience(
                recolour(OverworldBiomes.swamp(features, carvers),
                        new BiomeSpecialEffects.Builder()
                                .waterColor(LIMAN_WATER)
                                .grassColorOverride(0x7C9851)
                                .foliageColorOverride(0x608044)
                                .dryFoliageColorOverride(0xA19458)
                                .build()),
                KHSounds.AMBIENT_LIMAN_LOOP, KHSounds.AMBIENT_LIMAN_ADDITIONS,
                OPEN_WATER_ADDITIONS_CHANCE));
        context.register(RIVER_FLOODPLAIN, withAmbience(
                riverFloodplain(features, carvers),
                KHSounds.AMBIENT_FLOODPLAIN_LOOP, KHSounds.AMBIENT_FLOODPLAIN_ADDITIONS,
                FLOODPLAIN_ADDITIONS_CHANCE));

        // --- Второй пояс: см. комментарий у ключей ---
        //
        // Голос этим шести биомам намеренно НЕ задаётся. Атмосферных петель в
        // моде записано четыре, по одной на исходный биом; выдать предгорному
        // лесу степной посвист было бы хуже тишины — лес зазвучал бы полем.
        // Поэтому здесь остаётся ванильный слой измерения: свой лесной и
        // горный фон у Верхнего мира есть, а пещеры под новыми биомами
        // сохраняют пещерное настроение, потому что мы ничего не затираем.
        // Тест biome_ambience_is_distinct проверяет ровно четыре озвученных
        // биома и на эти шесть не распространяется.
        context.register(FOOTHILL_FOREST, foothillForest(features, carvers));
        context.register(MOUNTAIN_FOREST, mountainForest(features, carvers));
        context.register(AZOV_COAST, azovCoast(features, carvers));
        context.register(BLACK_SEA_COAST, blackSeaCoast(features, carvers));
        context.register(VINEYARD_HILLS, vineyardHills(features, carvers));
        context.register(TEA_SLOPES, teaSlopes(features, carvers));
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
                Optional.of(new AmbientMoodSettings(
                        SoundEvents.AMBIENT_CAVE, MOOD_TICK_DELAY, MOOD_SEARCH_EXTENT, MOOD_OFFSET)),
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
>>>>>>> Stashed changes
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
                .specialEffects(new BiomeSpecialEffects.Builder()
                        .waterColor(NORMAL_WATER_COLOR)
                        .grassColorOverride(0x79A85A)
                        .foliageColorOverride(0x568A45)
                        .dryFoliageColorOverride(0xA39A5D)
                        .build())
                .mobSpawnSettings(mobs.build())
                .generationSettings(generation.build())
                .build();
    }

    // =====================================================================
    // Второй пояс биомов
    //
    // Каждый собран поверх ванильного строителя того климатического региона,
    // который он забирает у степи. Это сделано намеренно: ванильный
    // строитель приносит согласованный со всем миром порядок placed
    // features, а FeatureSorter объединяет features всех возможных биомов
    // opt-in пресета и падает на несовместимом относительном порядке. Свой
    // набор растительности, собранный «как хочется», ломал бы генерацию
    // всего мира — поэтому меняются климат, цвета и фауна, но не порядок.
    //
    // Цвета взяты из ART_BIBLE.md §2, а не придуманы. Цвет травы и листвы
    // задаёт лицо биома сильнее всего остального: без него шесть биомов
    // отличались бы только подписью в F3, то есть остались бы той же самой
    // степью под другим именем.
    // =====================================================================

    /** Степная трава {@code #8aa74a} — суше ванильной равнины. */
    private static final int STEPPE_GRASS = 0x8AA74A;
    /** Выгоревшая трава {@code #b7a95c} — сухостой, август. */
    private static final int BURNT_GRASS = 0xB7A95C;
    /** Ракушечник {@code #d8cba8} — камень побережий. */
    private static final int SHELL_ROCK_TONE = 0xD8CBA8;
    /** Морская вода ЧМ {@code #1e6e8c} — Черноморское побережье. */
    private static final int BLACK_SEA_WATER = 0x1E6E8C;
    /** Лиманная вода {@code #5e8a6a} — мелкое тёплое Азовское море. */
    private static final int LIMAN_WATER = 0x5E8A6A;
    /** Виноград тёмный {@code #4a2a52} — ягоды на сухих склонах. */
    private static final int GRAPE_DARK = 0x4A2A52;
    /** Чайный лист {@code #3e6b34} — чайные кусты. */
    private static final int TEA_LEAF = 0x3E6B34;

    /**
     * Предгорный лес — дубово-грабовые леса подножия Кавказа.
     *
     * <p>Забирает у степи весь умеренно-влажный лесной регион ванили
     * (лес, березняк, цветущий лес, тёмный лес, тайга). Это самый большой
     * кусок карты, который до сих пор подписывался степью: игрок стоял
     * посреди сомкнутого леса, а F3 и цвет травы говорили «степь».</p>
     *
     * <p>География: между кубанской равниной и горами лежит полоса
     * широколиственного леса — дуб, граб, бук. Она влажнее степи и
     * прохладнее, поэтому температура и влажность взяты лесные, а не
     * равнинные. Здесь же снова обретает дом кабан: он был расселён по
     * ванильным лесам, которых в этом мире не существовало.</p>
     */
    private static Biome foothillForest(HolderGetter<PlacedFeature> features,
            HolderGetter<ConfiguredWorldCarver<?>> carvers) {
        // birch=false, tall=false, flower=false — обычный смешанный лес.
        Biome base = OverworldBiomes.forest(features, carvers, false, false, false);
        return recolour(base, new BiomeSpecialEffects.Builder()
                .waterColor(NORMAL_WATER_COLOR)
                // Листва и трава темнее и «сочнее» степной: лес держит влагу.
                .grassColorOverride(0x6E8F3E)
                .foliageColorOverride(0x4E7A2E)
                .dryFoliageColorOverride(BURNT_GRASS)
                .build());
    }

    /**
     * Горный лес — пихтово-буковый пояс Кавказского заповедника.
     *
     * <p>Забирает у степи весь горный регион ванили: обдуваемые холмы,
     * луга, рощи, снежные склоны и каменные пики. Форма гор в мире была и
     * раньше — ванильный рельеф никто не отменял, — но каждая вершина
     * называлась степью и красилась степной травой. Теперь высота получает
     * собственное лицо.</p>
     *
     * <p>География: за предгорьями поднимается Главный Кавказский хребет.
     * Пихта, бук, выше — субальпийские луга и голый камень. Это самая
     * прохладная и влажная часть края, поэтому температура низкая, а
     * {@code temperatureAdjustment} остаётся ванильным: понижение
     * температуры с высотой в MC считает сам {@link Biome}.</p>
     *
     * <p>Здесь же законный дом манула: он был расселён по обдуваемым
     * холмам, лугам и каменным пикам ванили — ни один из этих биомов в
     * этом мире не появлялся.</p>
     */
    private static Biome mountainForest(HolderGetter<PlacedFeature> features,
            HolderGetter<ConfiguredWorldCarver<?>> carvers) {
        // moreTrees=true — облесённый склон, а не голый камень: Кавказ
        // покрыт лесом до самой границы леса.
        Biome base = OverworldBiomes.windsweptHills(features, carvers, true);
        return recolour(base, new BiomeSpecialEffects.Builder()
                .waterColor(0x2E6B7A)
                // Хвоя темнее и холоднее лиственного предгорья.
                .grassColorOverride(0x5A7A44)
                .foliageColorOverride(0x3E6B3A)
                .dryFoliageColorOverride(0x8A7F4C)
                .build());
    }

    /**
     * Азовское побережье — ракушечные отмели самого мелкого моря мира.
     *
     * <p>Забирает у степи ванильный пляж. Пляжи в этом мире существовали
     * геометрически — песчаная кромка у воды генерировалась, — но
     * назывались степью.</p>
     *
     * <p>География: Азовское море — самое мелкое море планеты, средняя
     * глубина около семи метров. Оно быстро прогревается, вода мутно-зелёная,
     * а берег сложен ракушечником: спрессованными раковинами. Отсюда
     * тёплая лиманная вода и бледный ракушечный тон, а не ванильная
     * морская синь. Зимой Азов замерзает — поэтому снежный пляж ванили
     * тоже сходится сюда, а не в отдельный биом.</p>
     */
    private static Biome azovCoast(HolderGetter<PlacedFeature> features,
            HolderGetter<ConfiguredWorldCarver<?>> carvers) {
        // snowy=false, stony=false — тёплый песчаный пляж.
        Biome base = OverworldBiomes.beach(features, carvers, false, false);
        return recolour(base, new BiomeSpecialEffects.Builder()
                // Мелкое, тёплое, мутно-зелёное море, а не открытая синь.
                .waterColor(LIMAN_WATER)
                .grassColorOverride(BURNT_GRASS)
                .foliageColorOverride(SHELL_ROCK_TONE)
                .dryFoliageColorOverride(SHELL_ROCK_TONE)
                .build());
    }

    /**
     * Черноморское побережье — галечные обрывы и глубокая вода.
     *
     * <p>Забирает у степи ванильный каменистый берег. Намеренно сделан
     * визуальной противоположностью Азову: там бледная отмель и зелёная
     * вода, здесь тёмная галька и глубокая синь. Два берега рядом на одной
     * карте — самый дешёвый способ показать, что мир перестал быть
     * однородным.</p>
     *
     * <p>География: Черное море у Кубани глубокое и обрывистое — горы
     * подходят к самой воде, пляжи галечные, вода холоднее и синее
     * азовской. Отсюда {@code #1e6e8c} из палитры.</p>
     */
    private static Biome blackSeaCoast(HolderGetter<PlacedFeature> features,
            HolderGetter<ConfiguredWorldCarver<?>> carvers) {
        // stony=true — галечный, а не песчаный берег.
        Biome base = OverworldBiomes.beach(features, carvers, false, true);
        return recolour(base, new BiomeSpecialEffects.Builder()
                .waterColor(BLACK_SEA_WATER)
                // Скудная растительность на камне у солёной воды.
                .grassColorOverride(0x6F8A50)
                .foliageColorOverride(0x5A7A46)
                .dryFoliageColorOverride(SHELL_ROCK_TONE)
                .build());
    }

    /**
     * Виноградные холмы — сухие южные склоны Тамани и Анапы.
     *
     * <p>Забирает у степи ванильную саванну и плато саванны — самый жаркий
     * и сухой регион, который в этом мире тоже назывался степью.</p>
     *
     * <p>География: Таманский полуостров и окрестности Анапы — сухие
     * прогретые склоны, где виноград растёт с античности. Это не степь:
     * степь ровная и травяная, а здесь холмы, камень и лоза. Мод уже
     * расселял одичавший виноград по ванильным саваннам — которых в этом
     * мире не было, из-за чего лоза встречалась только в степи. Теперь у
     * неё есть свой пояс.</p>
     */
    private static Biome vineyardHills(HolderGetter<PlacedFeature> features,
            HolderGetter<ConfiguredWorldCarver<?>> carvers) {
        // shattered=false, plateau=false — цельные холмы, не разломы.
        Biome base = OverworldBiomes.savanna(features, carvers, false, false);
        return recolour(base, new BiomeSpecialEffects.Builder()
                .waterColor(NORMAL_WATER_COLOR)
                // Выгоревший склон с тёмной лозой.
                .grassColorOverride(BURNT_GRASS)
                .foliageColorOverride(0x7A8A46)
                .dryFoliageColorOverride(GRAPE_DARK)
                .build());
    }

    /**
     * Чайные склоны — самые северные чайные плантации в мире.
     *
     * <p>Забирает у степи ванильные джунгли и разрежённые джунгли: самый
     * влажный и тёплый регион карты.</p>
     *
     * <p>География: под Сочи, в Мацесте, растёт самый северный чай на
     * планете — влажные субтропики, тёплая зима, красноземные склоны.
     * Отдельный биом «влажные субтропики» из библии содержания сюда
     * намеренно НЕ добавлен: он претендовал бы на тот же самый джунглевый
     * климат, и два биома делили бы один источник — один из них оказался
     * бы мёртвым либо неотличимым от другого. Субтропики здесь — не
     * отдельная подпись, а свойства этого биома.</p>
     *
     * <p>Мод уже расселял дикий чай по ванильным джунглям, недостижимым в
     * этом мире: чай встречался только в плавнях и лимане, то есть на
     * болоте. Теперь он растёт там, где и должен — на тёплом влажном
     * склоне.</p>
     */
    private static Biome teaSlopes(HolderGetter<PlacedFeature> features,
            HolderGetter<ConfiguredWorldCarver<?>> carvers) {
        Biome base = OverworldBiomes.sparseJungle(features, carvers);
        return recolour(base, new BiomeSpecialEffects.Builder()
                .waterColor(0x3A7A6E)
                // Чайный лист из палитры — лицо биома.
                .grassColorOverride(0x4E7A38)
                .foliageColorOverride(TEA_LEAF)
                .dryFoliageColorOverride(0x7A6B3A)
                .build());
    }

    /**
     * Пересобирает ванильный биом с новыми цветами, сохраняя всё остальное.
     *
     * <p>Тот же приём и те же две ловушки, что в {@link #withAmbience}:
     * климат берётся через {@code getModifiedClimateSettings()}, потому что
     * у {@code downfall} нет публичного геттера и при наивной пересборке
     * влажность биома молча терялась бы; атрибуты переносятся через
     * {@code putAttributes}, потому что {@code setAttribute} — это
     * override, и выборочная запись затёрла бы цвет неба и музыку,
     * выставленные ванильным строителем.</p>
     *
     * <p>Меняются только {@link BiomeSpecialEffects} — в MC 26.2 это ровно
     * пять полей цвета: вода, листва, сухая листва, трава и модификатор
     * травы. Небо, туман и звук живут в атрибутах окружения и здесь
     * сохраняются нетронутыми.</p>
     */
    private static Biome recolour(Biome biome, BiomeSpecialEffects effects) {
        Biome.ClimateSettings climate = biome.getModifiedClimateSettings();
        return new Biome.BiomeBuilder()
                .hasPrecipitation(climate.hasPrecipitation())
                .temperature(climate.temperature())
                .temperatureAdjustment(climate.temperatureModifier())
                .downfall(climate.downfall())
                .putAttributes(biome.getAttributes())
                .specialEffects(effects)
                .mobSpawnSettings(biome.getMobSettings())
                .generationSettings(biome.getGenerationSettings())
                .build();
    }
}
