package dev.romankrukovsky.kubanhorizons.worldgen;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;
import net.minecraft.world.level.biome.MultiNoiseBiomeSourceParameterList;

import java.util.List;
import java.util.stream.Stream;

/**
 * Проецирует ванильную multi-noise географию в региональные биомы Кубани.
 *
 * <p>Ванильный источник остаётся климатическим классификатором: он решает,
 * где сухо, где влажно, где высоко и где берег. Но его biome ID наружу не
 * выходят — каждый климатический регион подменяется кубанским биомом.</p>
 *
 * <p>Раньше подмен было три (болото, мангры, река), а четвёртым правилом
 * стояло «всё остальное — степь». Из-за этого мир состоял из четырёх
 * биомов, и один из них занимал почти всю карту: лес, гора, пляж и
 * каменистый берег сохраняли ванильную ФОРМУ рельефа, но подписывались и
 * красились степью. Игрок стоял в сомкнутом лесу или на вершине хребта, а
 * мир сообщал ему «кубанская степь».</p>
 *
 * <p>Теперь климатические регионы разобраны по реальным поясам края: море →
 * берег → равнина → предгорья → горы, плюс два южных пояса, виноградный и
 * чайный. Степь осталась запасным вариантом сознательно, но теперь она
 * ловит только то, чем и является: сухую открытую равнину.</p>
 */
public final class KubanBiomeSource extends BiomeSource {
    public static final MapCodec<KubanBiomeSource> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            MultiNoiseBiomeSourceParameterList.CODEC.fieldOf("preset")
                    .forGetter(source -> source.preset),
            Biome.CODEC.fieldOf("steppe")
                    .forGetter(source -> source.steppe),
            Biome.CODEC.fieldOf("plavni")
                    .forGetter(source -> source.plavni),
            Biome.CODEC.fieldOf("liman")
                    .forGetter(source -> source.liman),
            Biome.CODEC.fieldOf("floodplain")
                    .forGetter(source -> source.floodplain),
            Biome.CODEC.fieldOf("foothill_forest")
                    .forGetter(source -> source.foothillForest),
            Biome.CODEC.fieldOf("mountain_forest")
                    .forGetter(source -> source.mountainForest),
            Biome.CODEC.fieldOf("azov_coast")
                    .forGetter(source -> source.azovCoast),
            Biome.CODEC.fieldOf("black_sea_coast")
                    .forGetter(source -> source.blackSeaCoast),
            Biome.CODEC.fieldOf("vineyard_hills")
                    .forGetter(source -> source.vineyardHills),
            Biome.CODEC.fieldOf("tea_slopes")
                    .forGetter(source -> source.teaSlopes)
    ).apply(instance, KubanBiomeSource::new));

    /**
     * Лесной пояс предгорий: умеренно-влажный регион ванили.
     *
     * <p>Вся ванильная лесная группа, включая тайгу и тёмный лес: в крае
     * это одна полоса широколиственного леса между равниной и горами.
     * Разводить её на несколько биомов значило бы плодить подписи там, где
     * игрок видит один и тот же лес.</p>
     */
    private static final List<ResourceKey<Biome>> FOOTHILL_FOREST_SOURCES = List.of(
            Biomes.FOREST,
            Biomes.BIRCH_FOREST,
            Biomes.OLD_GROWTH_BIRCH_FOREST,
            Biomes.FLOWER_FOREST,
            Biomes.DARK_FOREST,
            Biomes.PALE_GARDEN,
            Biomes.WINDSWEPT_FOREST,
            Biomes.TAIGA,
            Biomes.SNOWY_TAIGA,
            Biomes.OLD_GROWTH_PINE_TAIGA,
            Biomes.OLD_GROWTH_SPRUCE_TAIGA);

    /**
     * Горный пояс: высоты и склоны ванили.
     *
     * <p>Обдуваемые холмы, луга, рощи, снежные склоны и все три вида пиков.
     * Кавказ покрыт лесом до границы леса, выше — луга и камень; в MC это
     * один вертикальный пояс, и ванильный рельеф уже даёт ему форму.</p>
     */
    private static final List<ResourceKey<Biome>> MOUNTAIN_FOREST_SOURCES = List.of(
            Biomes.WINDSWEPT_HILLS,
            Biomes.WINDSWEPT_GRAVELLY_HILLS,
            Biomes.MEADOW,
            Biomes.CHERRY_GROVE,
            Biomes.GROVE,
            Biomes.SNOWY_SLOPES,
            Biomes.STONY_PEAKS,
            Biomes.JAGGED_PEAKS,
            Biomes.FROZEN_PEAKS);

    /**
     * Азовский берег: песчаная кромка ванили.
     *
     * <p>Снежный пляж тоже сюда: Азовское море действительно замерзает
     * зимой, и отдельного «зимнего берега» край не знает.</p>
     */
    private static final List<ResourceKey<Biome>> AZOV_COAST_SOURCES = List.of(
            Biomes.BEACH,
            Biomes.SNOWY_BEACH);

    /** Черноморский берег: каменистая кромка ванили — галечные обрывы. */
    private static final List<ResourceKey<Biome>> BLACK_SEA_COAST_SOURCES = List.of(
            Biomes.STONY_SHORE);

    /**
     * Виноградный пояс: жаркий сухой регион ванили.
     *
     * <p>Саванна и её плато — прогретые склоны Тамани и Анапы. Разломанная
     * саванна тоже здесь: обрывистый склон над морем виноградникам не
     * противоречит.</p>
     */
    private static final List<ResourceKey<Biome>> VINEYARD_HILLS_SOURCES = List.of(
            Biomes.SAVANNA,
            Biomes.SAVANNA_PLATEAU,
            Biomes.WINDSWEPT_SAVANNA);

    /**
     * Чайный пояс: самый влажный тёплый регион ванили.
     *
     * <p>Все три вида джунглей — влажные субтропики под Сочи. Отдельного
     * биома «влажные субтропики» нет намеренно: он делил бы с чайными
     * склонами один и тот же источник, и один из двух оказался бы мёртвым.</p>
     */
    private static final List<ResourceKey<Biome>> TEA_SLOPES_SOURCES = List.of(
            Biomes.JUNGLE,
            Biomes.SPARSE_JUNGLE,
            Biomes.BAMBOO_JUNGLE);

    /** Плавни: заросшее болото ванили. */
    private static final List<ResourceKey<Biome>> PLAVNI_SOURCES = List.of(Biomes.SWAMP);

    /** Лиман: мангровое болото ванили — солёная мелкая вода. */
    private static final List<ResourceKey<Biome>> LIMAN_SOURCES = List.of(Biomes.MANGROVE_SWAMP);

    /** Пойма: речные регионы ванили, включая замёрзшую реку. */
    private static final List<ResourceKey<Biome>> FLOODPLAIN_SOURCES = List.of(
            Biomes.RIVER,
            Biomes.FROZEN_RIVER);

    private final Holder<MultiNoiseBiomeSourceParameterList> preset;
    private final Holder<Biome> steppe;
    private final Holder<Biome> plavni;
    private final Holder<Biome> liman;
    private final Holder<Biome> floodplain;
    private final Holder<Biome> foothillForest;
    private final Holder<Biome> mountainForest;
    private final Holder<Biome> azovCoast;
    private final Holder<Biome> blackSeaCoast;
    private final Holder<Biome> vineyardHills;
    private final Holder<Biome> teaSlopes;
    private final MultiNoiseBiomeSource delegate;

    public KubanBiomeSource(Holder<MultiNoiseBiomeSourceParameterList> preset, Holder<Biome> steppe,
            Holder<Biome> plavni, Holder<Biome> liman, Holder<Biome> floodplain,
            Holder<Biome> foothillForest, Holder<Biome> mountainForest,
            Holder<Biome> azovCoast, Holder<Biome> blackSeaCoast,
            Holder<Biome> vineyardHills, Holder<Biome> teaSlopes) {
        this.preset = preset;
        this.steppe = steppe;
        this.plavni = plavni;
        this.liman = liman;
        this.floodplain = floodplain;
        this.foothillForest = foothillForest;
        this.mountainForest = mountainForest;
        this.azovCoast = azovCoast;
        this.blackSeaCoast = blackSeaCoast;
        this.vineyardHills = vineyardHills;
        this.teaSlopes = teaSlopes;
        this.delegate = MultiNoiseBiomeSource.createFromPreset(preset);
    }

    @Override
    protected Stream<Holder<Biome>> collectPossibleBiomes() {
        // Обязано перечислять ВСЁ, что может вернуть remap: биом, который
        // источник отдаёт, но не объявляет, — реальный баг генерации.
        // Тест biome_source_declares_everything_it_returns сверяет эти два
        // списка перебором всех ванильных биомов Верхнего мира.
        return Stream.of(steppe, plavni, liman, floodplain,
                foothillForest, mountainForest, azovCoast, blackSeaCoast,
                vineyardHills, teaSlopes);
    }

    @Override
    protected MapCodec<? extends BiomeSource> codec() {
        return KHBiomeSources.KUBAN.get();
    }

    @Override
    public Holder<Biome> getNoiseBiome(int x, int y, int z, Climate.Sampler sampler) {
        return remap(delegate.getNoiseBiome(x, y, z, sampler));
    }

    /**
     * Подменяет ванильный климатический биом кубанским.
     *
     * <p>Вынесено из {@link #getNoiseBiome} отдельным методом сознательно:
     * так маршрутизацию можно прогнать тестом по всем ванильным биомам
     * Верхнего мира, не строя мир и не сэмплируя шум. Проверять сам факт
     * регистрации биома бессмысленно — мод уже возил зарегистрированный
     * контент, до которого игрок не мог добраться; здесь проверяется
     * именно путь от климата к биому.</p>
     *
     * @param vanilla биом, выбранный ванильным multi-noise классификатором
     * @return кубанский биом соответствующего пояса
     */
    public Holder<Biome> remap(Holder<Biome> vanilla) {
        if (matches(vanilla, PLAVNI_SOURCES)) {
            return plavni;
        }
        if (matches(vanilla, LIMAN_SOURCES)) {
            return liman;
        }
        if (matches(vanilla, FLOODPLAIN_SOURCES)) {
            return floodplain;
        }
        if (matches(vanilla, AZOV_COAST_SOURCES)) {
            return azovCoast;
        }
        if (matches(vanilla, BLACK_SEA_COAST_SOURCES)) {
            return blackSeaCoast;
        }
        if (matches(vanilla, TEA_SLOPES_SOURCES)) {
            return teaSlopes;
        }
        if (matches(vanilla, VINEYARD_HILLS_SOURCES)) {
            return vineyardHills;
        }
        if (matches(vanilla, MOUNTAIN_FOREST_SOURCES)) {
            return mountainForest;
        }
        if (matches(vanilla, FOOTHILL_FOREST_SOURCES)) {
            return foothillForest;
        }
        // Степь — осознанный запасной вариант, а не «всё остальное».
        //
        // Сюда сходятся только сухие открытые регионы: равнина, поля
        // подсолнухов, пустыня, badlands, снежная равнина, ледяные пики,
        // грибные поля. Всё это в крае — одна и та же ровная открытая
        // степь, отличающаяся лишь тем, насколько она выгорела.
        //
        // Сюда же пока сходятся океаны и подземные биомы ванили. Для
        // пещерных биомов это безобидно: в этом мире они и так не
        // выбираются поверхностным классификатором. Для океанов это
        // известная и НЕ закрытая здесь проблема — океанские впадины
        // подписываются степью, то есть моря как биома в мире нет. Чинить
        // это подменой на берег было бы неверно (берег — не открытая
        // вода), а полноценный морской пояс требует объявить ванильные
        // океаны возможными биомами и пересобрать порядок features всего
        // мира. Это отдельная работа, и она честно вынесена в отчёт, а не
        // замаскирована.
        return steppe;
    }

    private static boolean matches(Holder<Biome> biome, List<ResourceKey<Biome>> sources) {
        for (ResourceKey<Biome> key : sources) {
            if (biome.is(key)) {
                return true;
            }
        }
        return false;
    }
}
