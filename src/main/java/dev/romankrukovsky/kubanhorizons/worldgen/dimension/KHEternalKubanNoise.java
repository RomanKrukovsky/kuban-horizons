package dev.romankrukovsky.kubanhorizons.worldgen.dimension;

import dev.romankrukovsky.kubanhorizons.util.KHIds;
import dev.romankrukovsky.kubanhorizons.worldgen.KHBiomes;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.NoiseRouter;
import net.minecraft.world.level.levelgen.NoiseSettings;
import net.minecraft.world.level.levelgen.Noises;
import net.minecraft.world.level.levelgen.SurfaceRules;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.synth.NormalNoise;

import java.util.List;

/**
 * Форма «Вечной Кубани»: парящие степные острова в сапфировой бесконечности.
 *
 * <p>Это не перекрашенный оверворлд. Плотность обнуляется у пола и потолка
 * измерения, поэтому земля существует только полосами: острова висят и выше, и
 * ниже уровня дворца, а под ними — пустота. Обычного горизонта нет, потому что
 * нет непрерывной поверхности.</p>
 *
 * <p>Поверхности кубанские: чернозём степи и супесь, а не энд-камень. Ванильный
 * {@code floatingIslands} не переиспользуется напрямую, потому что он рассчитан
 * на высоту 256 от нуля, а измерение имеет высоту 512 от −128 и должно иметь
 * плотную «полку» под дворцом.</p>
 */
public final class KHEternalKubanNoise {
    public static final ResourceKey<NoiseGeneratorSettings> ETERNAL_KUBAN =
            ResourceKey.create(Registries.NOISE_SETTINGS, KHIds.of("eternal_kuban"));

    /**
     * Толщина плотной полки под главным островом.
     *
     * <p>Дворец стоит на самом крупном острове, поэтому под ним гарантируется
     * массив камня: иначе процедурный шум мог бы оставить зал висящим над
     * дырой, и генерация перестала бы быть воспроизводимой.</p>
     */
    private static final int PALACE_SHELF_HALF_SPAN = 96;

    private KHEternalKubanNoise() {
    }

    public static void bootstrap(BootstrapContext<NoiseGeneratorSettings> context) {
        HolderGetter<NormalNoise.NoiseParameters> noises = context.lookup(Registries.NOISE);
        HolderGetter<Biome> biomes = context.lookup(Registries.BIOME);

        context.register(ETERNAL_KUBAN, new NoiseGeneratorSettings(
                NoiseSettings.create(KHDimensions.MIN_Y, KHDimensions.HEIGHT, 2, 1),
                Blocks.STONE.defaultBlockState(),
                Blocks.WATER.defaultBlockState(),
                router(noises),
                surfaceRules(biomes),
                List.of(),
                // Уровень моря ниже пола дворца: вода существует только в
                // прудах на островах, а не как глобальный океан.
                KHDimensions.PALACE_FLOOR_Y - 48,
                // Мобов от генератора не спавним — здесь их нет по правилам измерения.
                true,
                // Аквиферы отключены: подземных водоносных слоёв у парящих
                // островов быть не может, иначе из их днища будет течь вода.
                false,
                false,
                false));
    }

    /**
     * Плотность, дающая парящие острова.
     *
     * <p>Трёхмерный шум сам по себе заполнил бы весь объём. Чтобы получить
     * острова, плотность гасится градиентами у пола и потолка и усиливается
     * узкой полосой на уровне дворца.</p>
     */
    private static NoiseRouter router(HolderGetter<NormalNoise.NoiseParameters> noises) {
        // Крупный трёхмерный шум — основа формы островов.
        DensityFunction base = DensityFunctions.noise(
                noises.getOrThrow(Noises.CAVE_CHEESE), 1.0D, 0.6D);

        // Гашение у нижней границы: под самыми низкими островами обязана быть
        // пустота, чтобы игрок видел бесконечность под собой.
        DensityFunction fadeBottom = DensityFunctions.yClampedGradient(
                KHDimensions.MIN_Y, KHDimensions.MIN_Y + 48, -1.0D, 0.0D);

        // Гашение у верхней границы: над верхними островами тоже пустота, иначе
        // измерение упрётся в каменный потолок и потеряет ощущение простора.
        int top = KHDimensions.MIN_Y + KHDimensions.HEIGHT;
        DensityFunction fadeTop = DensityFunctions.yClampedGradient(
                top - 64, top, 0.0D, -1.0D);

        // Полоса вокруг уровня дворца: здесь островов заметно больше, поэтому
        // главный ярус читается как обитаемый слой, а не редкая сыпь камней.
        DensityFunction mainTier = DensityFunctions.mul(
                DensityFunctions.constant(0.35D),
                DensityFunctions.yClampedGradient(
                        KHDimensions.PALACE_FLOOR_Y - 40,
                        KHDimensions.PALACE_FLOOR_Y + 8,
                        0.0D, 1.0D));

        DensityFunction shaped = DensityFunctions.add(
                DensityFunctions.add(base, mainTier),
                DensityFunctions.add(fadeBottom, fadeTop));

        DensityFunction finalDensity = DensityFunctions.interpolated(
                DensityFunctions.blendDensity(shaped)).squeeze();

        // Климат берётся плоским: биомы внутри лампы задаёт не температура, а
        // сама джинния, поэтому multi-noise география здесь не нужна.
        DensityFunction zero = DensityFunctions.zero();
        return new NoiseRouter(
                zero, zero, zero, zero,
                zero, zero, zero, zero,
                zero, zero, zero,
                finalDensity,
                zero, zero, zero);
    }

    /**
     * Кубанские поверхности парящих островов.
     *
     * <p>Дёрн и чернозём сверху, супесь в разрезе, камень в глубине — тот же
     * визуальный язык, что у степи мода, поэтому острова читаются как Кубань, а
     * не как обломки Края.</p>
     */
    private static SurfaceRules.RuleSource surfaceRules(HolderGetter<Biome> biomes) {
        SurfaceRules.RuleSource grass = SurfaceRules.state(Blocks.GRASS_BLOCK.defaultBlockState());
        SurfaceRules.RuleSource dirt = SurfaceRules.state(Blocks.DIRT.defaultBlockState());
        SurfaceRules.RuleSource coarse = SurfaceRules.state(Blocks.COARSE_DIRT.defaultBlockState());

        // Верхний блок: дёрн на воздухе, но под водой прудов — грязь.
        SurfaceRules.RuleSource top = SurfaceRules.sequence(
                SurfaceRules.ifTrue(SurfaceRules.waterBlockCheck(0, 0), grass),
                SurfaceRules.state(Blocks.MUD.defaultBlockState()));

        // Разрез острова: под дёрном земля, глубже — камень генератора.
        SurfaceRules.RuleSource profile = SurfaceRules.sequence(
                SurfaceRules.ifTrue(SurfaceRules.ON_FLOOR, top),
                SurfaceRules.ifTrue(SurfaceRules.UNDER_FLOOR, dirt),
                SurfaceRules.ifTrue(SurfaceRules.DEEP_UNDER_FLOOR, coarse));

        return SurfaceRules.sequence(
                // Днище островов остаётся камнем: срез должен выглядеть как
                // вырванный кусок земли, а не как аккуратный газон снизу.
                SurfaceRules.ifTrue(
                        SurfaceRules.verticalGradient("eternal_kuban_bedrock_floor",
                                VerticalAnchor.absolute(KHDimensions.MIN_Y),
                                VerticalAnchor.absolute(KHDimensions.MIN_Y + 4)),
                        SurfaceRules.state(Blocks.STONE.defaultBlockState())),
                profile);
    }

    /** Половина пролёта гарантированной полки под дворцом, в блоках. */
    public static int palaceShelfHalfSpan() {
        return PALACE_SHELF_HALF_SPAN;
    }
}
