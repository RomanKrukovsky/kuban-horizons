package dev.romankrukovsky.kubanhorizons.worldgen.dimension;

import dev.romankrukovsky.kubanhorizons.util.KHIds;
import dev.romankrukovsky.kubanhorizons.worldgen.KHBiomes;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TimelineTags;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.world.attribute.BedRule;
import net.minecraft.world.attribute.EnvironmentAttributeMap;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.level.CardinalLighting;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.FixedBiomeSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.FlatLevelSource;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.flat.FlatLayerInfo;
import net.minecraft.world.level.levelgen.flat.FlatLevelGeneratorSettings;

import java.util.List;
import java.util.Optional;

/**
 * Внутреннее пространство лампы — «Вечная Кубань».
 *
 * <p>Отдельное измерение, а не область оверворлда: правила окружения здесь
 * задаются декларативно через {@link DimensionType}, поэтому вечный вечер,
 * отсутствие обычного неба и тёплый свет не требуют ни одного тика кода.</p>
 *
 * <p>Измерение регистрируется обычным datapack-JSON, поэтому появляется в любом
 * мире, включая существующие сохранения, и не требует opt-in world preset, за
 * которым живут биомы мода.</p>
 */
public final class KHDimensions {
    /** Ключ измерения для телепорта. */
    public static final ResourceKey<Level> ETERNAL_KUBAN =
            ResourceKey.create(Registries.DIMENSION, KHIds.of("eternal_kuban"));

    /** Ключ типа измерения. */
    public static final ResourceKey<DimensionType> ETERNAL_KUBAN_TYPE =
            ResourceKey.create(Registries.DIMENSION_TYPE, KHIds.of("eternal_kuban"));

    /** Простое карманное измерение (End-like). */
    public static final ResourceKey<Level> POCKET =
            ResourceKey.create(Registries.DIMENSION, KHIds.of("pocket"));

    /** Тип карманного измерения. */
    public static final ResourceKey<DimensionType> POCKET_TYPE =
            ResourceKey.create(Registries.DIMENSION_TYPE, KHIds.of("pocket"));

    /** Stem для карманного измерения. */
    public static final ResourceKey<LevelStem> POCKET_STEM =
            ResourceKey.create(Registries.LEVEL_STEM, KHIds.of("pocket"));

    /**
     * Нижняя граница измерения.
     *
     * <p>Отрицательная, потому что бриф требует парящих островов не только выше,
     * но и ниже дворца: игрок должен видеть пустоту под собой.</p>
     */
    public static final int MIN_Y = -128;

    /** Полная высота измерения; кратна 16, как требует {@code NoiseSettings.guardY}. */
    public static final int HEIGHT = 512;

    /** Уровень пола главного зала. Дворец стоит выше нуля, острова уходят вниз. */
    public static final int PALACE_FLOOR_Y = 96;

    /**
     * Цвет сапфирово-фиолетовой бесконечности.
     *
     * <p>Одно и то же значение используется для тумана и «неба», поэтому у
     * границы островов горизонт не читается: пустота и даль совпадают.</p>
     */
    private static final int VOID_SAPPHIRE = 0x1B1140;

    /** Тёплый янтарный подсвет дворца: камень должен выглядеть кремовым, не серым. */
    private static final int WARM_AMBER_LIGHT = 0xFFD9A0;

    private KHDimensions() {
    }

    public static void bootstrapType(BootstrapContext<DimensionType> context) {
        HolderGetter<Block> blocks = context.lookup(Registries.BLOCK);

        context.register(ETERNAL_KUBAN_TYPE, new DimensionType(
                true,
                true,
                false,
                false,
                1.0D,
                MIN_Y,
                HEIGHT,
                HEIGHT,
                blocks.getOrThrow(BlockTags.INFINIBURN_OVERWORLD),
                0.35F,
                new DimensionType.MonsterSettings(ConstantInt.of(0), 0),
                DimensionType.Skybox.NONE,
                CardinalLighting.Type.DEFAULT,
                atmosphere(),
                context.lookup(Registries.TIMELINE).getOrThrow(TimelineTags.UNIVERSAL),
                Optional.empty()));

        // Простое карманное измерение (End-like)
        context.register(POCKET_TYPE, new DimensionType(
                false,           // fixedTime
                false,           // hasSkyLight
                false,           // hasCeiling
                false,           // ultraWarm
                1.0D,            // coordinateScale
                0,               // minY
                256,             // height
                256,             // logicalHeight
                blocks.getOrThrow(BlockTags.INFINIBURN_OVERWORLD),
                0.0F,            // ambientLight
                new DimensionType.MonsterSettings(ConstantInt.of(0), 0),
                DimensionType.Skybox.END,
                CardinalLighting.Type.DEFAULT,
                EnvironmentAttributeMap.builder()
                        .set(EnvironmentAttributes.BED_RULE, new BedRule(
                                BedRule.Rule.NEVER, BedRule.Rule.NEVER, false,
                                Optional.of(Component.translatable("message.kubanhorizons.genie.pocket.no_sleep"))))
                        .set(EnvironmentAttributes.RESPAWN_ANCHOR_WORKS, false)
                        .build(),
                context.lookup(Registries.TIMELINE).getOrThrow(TimelineTags.UNIVERSAL),
                Optional.empty()));

        KHMagicDimensions.bootstrapType(context);
    }

    /**
     * Атмосфера «тёплого позднего вечера внутри дворца».
     *
     * <p>Всё, что можно выразить данными, выражено данными: цвет тумана и неба,
     * дальность видимости, тёплый свет, запрет сна и якоря возрождения. Кода на
     * поддержание атмосферы не требуется.</p>
     */
    private static EnvironmentAttributeMap atmosphere() {
        return EnvironmentAttributeMap.builder()
                // Туман и «небо» одного цвета: горизонт не читается, как требует бриф.
                .set(EnvironmentAttributes.FOG_COLOR, VOID_SAPPHIRE)
                .set(EnvironmentAttributes.SKY_COLOR, VOID_SAPPHIRE)
                // Далёкий туман: зал 112 блоков должен читаться целиком с точки входа,
                // но дальние острова обязаны растворяться в бесконечности.
                .set(EnvironmentAttributes.FOG_START_DISTANCE, 96.0F)
                .set(EnvironmentAttributes.FOG_END_DISTANCE, 320.0F)
                .set(EnvironmentAttributes.SKY_FOG_END_DISTANCE, 320.0F)
                // Вода бассейна — глубокий сапфир, а не бирюза оверворлда.
                .set(EnvironmentAttributes.WATER_FOG_COLOR, 0x102A6B)
                .set(EnvironmentAttributes.WATER_FOG_END_DISTANCE, 64.0F)
                // Гигантские магические созвездия: звёзды видны всегда, потому что
                // «неба» с солнцем здесь нет.
                .set(EnvironmentAttributes.STAR_BRIGHTNESS, 1.0F)
                // Тёплый интерьерный свет вместо холодного дневного.
                .set(EnvironmentAttributes.AMBIENT_LIGHT_COLOR, WARM_AMBER_LIGHT)
                .set(EnvironmentAttributes.SKY_LIGHT_COLOR, 0xFFC98A)
                // Приглушённый скайлайт: центр зала освещают фонари, ниши уходят в тень.
                .set(EnvironmentAttributes.SKY_LIGHT_FACTOR, 0.45F)
                // Спать в лампе нельзя: времени суток нет. Кровать при этом не
                // взрывается — взрыв разрушил бы дворец, поэтому не BedRule.EXPLODES.
                .set(EnvironmentAttributes.BED_RULE, new BedRule(
                        BedRule.Rule.NEVER,
                        BedRule.Rule.NEVER,
                        false,
                        Optional.of(Component.translatable(
                                "message.kubanhorizons.genie.lamp.no_sleep"))))
                .set(EnvironmentAttributes.RESPAWN_ANCHOR_WORKS, false)
                // Рейды и патрули внутри личного пространства джиннии невозможны.
                .set(EnvironmentAttributes.CAN_START_RAID, false)
                .set(EnvironmentAttributes.CAN_PILLAGER_PATROL_SPAWN, false)
                .set(EnvironmentAttributes.MONSTERS_BURN, false)
                .set(EnvironmentAttributes.PIGLINS_ZOMBIFY, false)
                .build();
    }

    /** Настройки генерации: своя форма мира, а не проекция оверворлда. */
    public static ResourceKey<NoiseGeneratorSettings> noiseSettingsKey() {
        return KHEternalKubanNoise.ETERNAL_KUBAN;
    }

    /**
     * Ключ {@link LevelStem}: совпадает с ключом измерения, как того требует
     * {@code Registries.levelStemToLevel}.
     */
    public static final ResourceKey<LevelStem> ETERNAL_KUBAN_STEM =
            ResourceKey.create(Registries.LEVEL_STEM, KHIds.of("eternal_kuban"));

    /**
     * Регистрирует уровень измерения.
     *
     * <p>Биом фиксированный: внутри лампы география определяется дворцом и
     * островами, а не климатическими шумами, поэтому multi-noise здесь только
     * добавил бы непредсказуемости в детерминированную сцену.</p>
     */
    public static void bootstrapStem(BootstrapContext<LevelStem> context) {
        HolderGetter<Biome> biomes = context.lookup(Registries.BIOME);
        HolderGetter<DimensionType> types = context.lookup(Registries.DIMENSION_TYPE);
        HolderGetter<NoiseGeneratorSettings> noiseSettings = context.lookup(Registries.NOISE_SETTINGS);

        context.register(ETERNAL_KUBAN_STEM, new LevelStem(
                types.getOrThrow(ETERNAL_KUBAN_TYPE),
                new NoiseBasedChunkGenerator(
                        new FixedBiomeSource(biomes.getOrThrow(KHBiomes.KUBAN_STEPPE)),
                        noiseSettings.getOrThrow(KHEternalKubanNoise.ETERNAL_KUBAN))));

        // Простое карманное измерение — пустота + фиксированный биом
        context.register(POCKET_STEM, new LevelStem(
                types.getOrThrow(POCKET_TYPE),
                new FlatLevelSource(new FlatLevelGeneratorSettings(
                        Optional.empty(),
                        biomes.getOrThrow(Biomes.THE_VOID),
                        List.of(new FlatLayerInfo(1, Blocks.BEDROCK))))));

        KHMagicDimensions.bootstrapStem(context);
    }

}
