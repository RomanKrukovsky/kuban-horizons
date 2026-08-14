package dev.romankrukovsky.kubanhorizons.worldgen;

import dev.romankrukovsky.kubanhorizons.util.KHIds;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.worldgen.SurfaceRuleData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.Noises;
import net.minecraft.world.level.levelgen.SurfaceRules;
import net.minecraft.world.level.levelgen.VerticalAnchor;

/** Noise settings ванильного Overworld с поверхностями всех кубанских биомов. */
public final class KHNoiseSettings {
    public static final ResourceKey<NoiseGeneratorSettings> OVERWORLD =
            ResourceKey.create(Registries.NOISE_SETTINGS, KHIds.of("overworld"));

    private KHNoiseSettings() {
    }

    public static void bootstrap(BootstrapContext<NoiseGeneratorSettings> context) {
        NoiseGeneratorSettings vanilla = NoiseGeneratorSettings.overworld(context, false, false);
        HolderGetter<Biome> biomes = context.lookup(Registries.BIOME);

        // Ванильные идиомы проверки воды (см. SurfaceRuleData).
        SurfaceRules.ConditionSource aboveWater = SurfaceRules.waterBlockCheck(0, 0);

        SurfaceRules.RuleSource plavniWater = SurfaceRules.ifTrue(
                SurfaceRules.isBiome(biomes, KHBiomes.PLAVNI),
                SurfaceRules.ifTrue(
                        SurfaceRules.ON_FLOOR,
                        SurfaceRules.ifTrue(
                                SurfaceRules.yBlockCheck(VerticalAnchor.absolute(62), 0),
                                SurfaceRules.ifTrue(
                                        SurfaceRules.noiseCondition2d(Noises.SWAMP, 0.0),
                                        SurfaceRules.state(Blocks.WATER.defaultBlockState())))));
        SurfaceRules.RuleSource limanMud = SurfaceRules.ifTrue(
                SurfaceRules.isBiome(biomes, KHBiomes.LIMAN),
                SurfaceRules.ifTrue(
                        SurfaceRules.ON_FLOOR,
                        SurfaceRules.state(Blocks.MUD.defaultBlockState())));

        // Пойма: под водой — речной ил (грязь и глина полосами), на суше —
        // редкие песчано-глинистые наносы поверх обычного дёрна.
        SurfaceRules.RuleSource floodplainSilt = SurfaceRules.ifTrue(
                SurfaceRules.isBiome(biomes, KHBiomes.RIVER_FLOODPLAIN),
                SurfaceRules.ifTrue(
                        SurfaceRules.ON_FLOOR,
                        SurfaceRules.sequence(
                                SurfaceRules.ifTrue(
                                        SurfaceRules.not(aboveWater),
                                        SurfaceRules.sequence(
                                                SurfaceRules.ifTrue(
                                                        SurfaceRules.noiseCondition2d(Noises.SURFACE, -0.35, 0.15),
                                                        SurfaceRules.state(Blocks.MUD.defaultBlockState())),
                                                SurfaceRules.ifTrue(
                                                        SurfaceRules.noiseCondition2d(Noises.SURFACE, 0.55, 0.95),
                                                        SurfaceRules.state(Blocks.CLAY.defaultBlockState())))),
                                SurfaceRules.ifTrue(
                                        aboveWater,
                                        SurfaceRules.sequence(
                                                SurfaceRules.ifTrue(
                                                        SurfaceRules.noiseCondition2d(Noises.SURFACE, 0.75, 1.0),
                                                        SurfaceRules.state(Blocks.COARSE_DIRT.defaultBlockState())),
                                                SurfaceRules.ifTrue(
                                                        SurfaceRules.noiseCondition2d(Noises.SURFACE, -1.0, -0.85),
                                                        SurfaceRules.state(Blocks.CLAY.defaultBlockState())))))));

        context.register(OVERWORLD, new NoiseGeneratorSettings(
                vanilla.noiseSettings(),
                vanilla.defaultBlock(),
                vanilla.defaultFluid(),
                vanilla.noiseRouter(),
                SurfaceRules.sequence(plavniWater, limanMud, floodplainSilt,
                        steppeChernozem(biomes), azovShellSand(biomes),
                        blackSeaShingle(biomes), foothillLitter(biomes),
                        mountainScree(biomes), vineyardStony(biomes),
                        teaRedSoil(biomes),
                        SurfaceRuleData.overworld(biomes)),
                vanilla.spawnTarget(),
                vanilla.seaLevel(),
                vanilla.disableMobGeneration(),
                vanilla.aquifersEnabled(),
                vanilla.oreVeinsEnabled(),
                vanilla.useLegacyRandomSource()));

        // Форма измерения лампы живёт в своём классе, но регистрируется здесь:
        // RegistrySetBuilder принимает один bootstrap на реестр.
        dev.romankrukovsky.kubanhorizons.worldgen.dimension.KHEternalKubanNoise.bootstrap(context);
    }

    // =====================================================================
    // Поверхности: то, по чему игрок ходит и на что смотрит
    //
    // До этого у степи не было НИ ОДНОГО своего правила: она получала
    // ванильную траву на дёрне, то есть под ногами была ровно равнина
    // Minecraft. Чернозём — самое известное, что есть у Кубани, и в мире
    // его не существовало.
    // =====================================================================

    /**
     * Степь — чернозём: глубокий гумусовый горизонт и выгоревшие проплешины.
     *
     * <p>Отдельного блока «чернозём» в моде нет, и заводить его здесь
     * нельзя — блоки принадлежат другой части работы. Поэтому чернозём
     * показан двумя честными средствами вместо выдуманного цвета.</p>
     *
     * <p>Первое — глубина. Настоящий чернозём отличается от обычной почвы
     * не столько оттенком, сколько мощностью: гумусовый горизонт уходит на
     * метр и глубже, тогда как ванильный дёрн лежит слоем в один-два блока
     * и сразу переходит в камень. {@code DEEP_UNDER_FLOOR} продолжает
     * землю вниз, и это видно каждому, кто копнёт: в степи под ногами
     * метр земли, а не камень под тонкой коркой.</p>
     *
     * <p>Второе — проплешины. Выгоревшая трава {@code #b7a95c} из палитры
     * — это август в степи; {@code COARSE_DIRT} по шумовым пятнам даёт
     * открытую сухую землю среди травы. Цвет самой травы задаётся в
     * биоме и здесь не дублируется.</p>
     *
     * <p>Правило намеренно не трогает подводную часть и работает только
     * выше уровня воды: залитый чернозём — это уже пойма, у неё свои
     * правила выше в последовательности.</p>
     */
    private static SurfaceRules.RuleSource steppeChernozem(HolderGetter<Biome> biomes) {
        SurfaceRules.ConditionSource aboveWater = SurfaceRules.waterBlockCheck(-1, 0);
        return SurfaceRules.ifTrue(
                SurfaceRules.isBiome(biomes, KHBiomes.KUBAN_STEPPE),
                SurfaceRules.ifTrue(
                        aboveWater,
                        SurfaceRules.sequence(
                                // Выгоревшие проплешины открытой земли.
                                SurfaceRules.ifTrue(
                                        SurfaceRules.ON_FLOOR,
                                        SurfaceRules.ifTrue(
                                                SurfaceRules.noiseCondition2d(Noises.SURFACE, 0.62, 1.0),
                                                SurfaceRules.state(Blocks.COARSE_DIRT.defaultBlockState()))),
                                // Мощный гумусовый горизонт: земля идёт вглубь.
                                SurfaceRules.ifTrue(
                                        SurfaceRules.DEEP_UNDER_FLOOR,
                                        SurfaceRules.state(Blocks.DIRT.defaultBlockState())))));
    }

    /**
     * Азовский берег — ракушечник под песком.
     *
     * <p>Ракушечник {@code #d8cba8} у мода уже есть блоком: азовский берег
     * сложен спрессованными раковинами, и это первое место, где камень
     * побережья появляется в мире естественно, а не только в крафте и в
     * дворце. Сверху — песок отмели, под ним — ракушечная плита.</p>
     */
    private static SurfaceRules.RuleSource azovShellSand(HolderGetter<Biome> biomes) {
        return SurfaceRules.ifTrue(
                SurfaceRules.isBiome(biomes, KHBiomes.AZOV_COAST),
                SurfaceRules.sequence(
                        SurfaceRules.ifTrue(
                                SurfaceRules.ON_FLOOR,
                                SurfaceRules.state(Blocks.SAND.defaultBlockState())),
                        SurfaceRules.ifTrue(
                                SurfaceRules.UNDER_FLOOR,
                                SurfaceRules.state(dev.romankrukovsky.kubanhorizons.registry.KHBlocks
                                        .SHELL_ROCK.get().defaultBlockState()))));
    }

    /**
     * Черноморский берег — галька на камне.
     *
     * <p>Противоположность азовскому: там бледная ракушечная отмель, здесь
     * тёмная галька и скала у самой воды. Два берега на одной карте
     * читаются как разные места, а не как один «пляж».</p>
     */
    private static SurfaceRules.RuleSource blackSeaShingle(HolderGetter<Biome> biomes) {
        return SurfaceRules.ifTrue(
                SurfaceRules.isBiome(biomes, KHBiomes.BLACK_SEA_COAST),
                SurfaceRules.sequence(
                        SurfaceRules.ifTrue(
                                SurfaceRules.ON_FLOOR,
                                SurfaceRules.sequence(
                                        SurfaceRules.ifTrue(
                                                SurfaceRules.noiseCondition2d(Noises.SURFACE, -0.2, 1.0),
                                                SurfaceRules.state(Blocks.GRAVEL.defaultBlockState())),
                                        SurfaceRules.state(Blocks.STONE.defaultBlockState()))),
                        SurfaceRules.ifTrue(
                                SurfaceRules.UNDER_FLOOR,
                                SurfaceRules.state(Blocks.STONE.defaultBlockState()))));
    }

    /**
     * Предгорный лес — лесная подстилка.
     *
     * <p>Дубово-грабовый лес держит влагу, и почва под ним не степная:
     * {@code PODZOL} по шумовым пятнам даёт тёмную перегнойную подстилку,
     * а не ровный дёрн равнины. Пятнами, а не сплошь, — иначе лес стал бы
     * выглядеть тайгой.</p>
     */
    private static SurfaceRules.RuleSource foothillLitter(HolderGetter<Biome> biomes) {
        return SurfaceRules.ifTrue(
                SurfaceRules.isBiome(biomes, KHBiomes.FOOTHILL_FOREST),
                SurfaceRules.ifTrue(
                        SurfaceRules.ON_FLOOR,
                        SurfaceRules.ifTrue(
                                SurfaceRules.waterBlockCheck(-1, 0),
                                SurfaceRules.ifTrue(
                                        SurfaceRules.noiseCondition2d(Noises.SURFACE, 0.35, 1.0),
                                        SurfaceRules.state(Blocks.PODZOL.defaultBlockState())))));
    }

    /**
     * Горный лес — выходы камня.
     *
     * <p>Выше по склону скала пробивается сквозь дёрн: на крутизне
     * ({@code steep()}) земли не держится вовсе, а на пологих участках
     * камень выходит пятнами. Это то, что отличает гору от «зелёного
     * холма» — и раньше этой разницы в мире не было.</p>
     */
    private static SurfaceRules.RuleSource mountainScree(HolderGetter<Biome> biomes) {
        return SurfaceRules.ifTrue(
                SurfaceRules.isBiome(biomes, KHBiomes.MOUNTAIN_FOREST),
                SurfaceRules.ifTrue(
                        SurfaceRules.ON_FLOOR,
                        SurfaceRules.sequence(
                                SurfaceRules.ifTrue(
                                        SurfaceRules.steep(),
                                        SurfaceRules.state(Blocks.STONE.defaultBlockState())),
                                SurfaceRules.ifTrue(
                                        SurfaceRules.noiseCondition2d(Noises.SURFACE, 0.55, 1.0),
                                        SurfaceRules.state(Blocks.GRAVEL.defaultBlockState())))));
    }

    /**
     * Виноградные холмы — сухой каменистый склон.
     *
     * <p>Виноград Тамани растёт на прогретой скудной земле с камнем:
     * открытая сухая почва и щебень вместо густого дёрна. Заодно это
     * визуально отделяет виноградный пояс от степи, хотя оба сухие.</p>
     */
    private static SurfaceRules.RuleSource vineyardStony(HolderGetter<Biome> biomes) {
        return SurfaceRules.ifTrue(
                SurfaceRules.isBiome(biomes, KHBiomes.VINEYARD_HILLS),
                SurfaceRules.ifTrue(
                        SurfaceRules.ON_FLOOR,
                        SurfaceRules.ifTrue(
                                SurfaceRules.waterBlockCheck(-1, 0),
                                SurfaceRules.sequence(
                                        SurfaceRules.ifTrue(
                                                SurfaceRules.noiseCondition2d(Noises.SURFACE, 0.7, 1.0),
                                                SurfaceRules.state(Blocks.GRAVEL.defaultBlockState())),
                                        SurfaceRules.ifTrue(
                                                SurfaceRules.noiseCondition2d(Noises.SURFACE, 0.2, 0.7),
                                                SurfaceRules.state(Blocks.COARSE_DIRT.defaultBlockState()))))));
    }

    /**
     * Чайные склоны — краснозём.
     *
     * <p>Чай под Сочи растёт на красноземных почвах — это не литературная
     * деталь, а причина, по которой чайные плантации там вообще возможны:
     * кислая красная почва влажных субтропиков. {@code PODZOL} —
     * ближайший ванильный блок такого тона, и здесь он лежит основным
     * покровом, а не пятнами, как в предгорном лесу.</p>
     */
    private static SurfaceRules.RuleSource teaRedSoil(HolderGetter<Biome> biomes) {
        return SurfaceRules.ifTrue(
                SurfaceRules.isBiome(biomes, KHBiomes.TEA_SLOPES),
                SurfaceRules.ifTrue(
                        SurfaceRules.ON_FLOOR,
                        SurfaceRules.ifTrue(
                                SurfaceRules.waterBlockCheck(-1, 0),
                                SurfaceRules.ifTrue(
                                        SurfaceRules.noiseCondition2d(Noises.SURFACE, -0.4, 1.0),
                                        SurfaceRules.state(Blocks.PODZOL.defaultBlockState())))));
    }
}
