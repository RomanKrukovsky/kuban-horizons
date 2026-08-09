package dev.romankrukovsky.kubanhorizons.worldgen.palace;

import dev.romankrukovsky.kubanhorizons.registry.KHBlocks;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Палитра дворца джиннии — единственный источник правды по материалам.
 *
 * <p>Все генераторы берут блоки только отсюда. Это даёт две вещи: цветовая
 * система брифа не расползается по десяткам файлов, и смена материала —
 * правка одной строки, а не поиск по всему дворцу.</p>
 *
 * <p>Основа кубанская и уже существует в моде: белёная штукатурка и
 * ракушечник дают требуемые оттенки слоновой кости и кремового камня, а не
 * стерильный белый.</p>
 */
public final class PalacePalette {
    private PalacePalette() {
    }

    // --- Камень оболочки -------------------------------------------------

    /** Основной камень стен: белёная штукатурка мода — тёплый кремовый тон. */
    public static final BlockState WALL = state(KHBlocks.WHITEWASHED_PLASTER.get());

    /** Уступы и карнизы стен. */
    public static final BlockState WALL_STEP = state(KHBlocks.WHITEWASHED_PLASTER_SLAB.get());

    /** Профиль карнизов и переходов к куполу. */
    public static final BlockState WALL_TRIM = state(KHBlocks.WHITEWASHED_PLASTER_STAIRS.get());

    /**
     * Камень купола: ракушечник.
     *
     * <p>Отличается от стен по тону, поэтому купол читается как отдельный
     * объём, а не продолжение стены вверх.</p>
     */
    public static final BlockState DOME = state(KHBlocks.SHELL_ROCK.get());

    /** Ступенчатые кольца купола. */
    public static final BlockState DOME_STEP = state(KHBlocks.SHELL_ROCK_SLAB.get());

    /** Пол-основа под мозаикой. */
    public static final BlockState FLOOR_BASE = state(KHBlocks.SHELL_ROCK.get());

    // --- Золото ----------------------------------------------------------

    /** Окантовки, колонны, карнизы. Не сплошная золотая коробка — только акценты. */
    public static final BlockState GOLD = state(Blocks.GOLD_BLOCK);

    /** Решётки балконных перил и оконных проёмов. */
    public static final BlockState GOLD_LATTICE = state(Blocks.IRON_BARS);

    /** Тонкий золотой профиль: цепи фонарей и вертикальные тяги. */
    public static final BlockState GOLD_CHAIN = state(Blocks.IRON_CHAIN);

    // --- Ковры и вышивка -------------------------------------------------
    // В 26.2 цветные блоки живут в ColorCollection, а не отдельными полями.

    /** Красный ковёр — основной цвет кубанской вышивки. */
    public static final BlockState CARPET_RED = state(Blocks.CARPET.red());

    /** Бордовый ковёр — второй тон орнамента. */
    public static final BlockState CARPET_CRIMSON = state(Blocks.CARPET.brown());

    /** Белый ковёр — фон геометрического узора. */
    public static final BlockState CARPET_WHITE = state(Blocks.CARPET.white());

    /** Синий ковёр — редкий акцент в орнаменте. */
    public static final BlockState CARPET_BLUE = state(Blocks.CARPET.blue());

    // --- Ткани стен ------------------------------------------------------

    /** Тёмно-синее полотно с золотым символом: основной вариант баннера. */
    public static final Block BANNER_SAPPHIRE = Blocks.WOOL.blue();

    /** Белое полотно с красно-бордовой вышивкой: второй вариант. */
    public static final Block BANNER_IVORY = Blocks.WOOL.white();

    /** Красная вышивка на полотнах и навесах. */
    public static final BlockState EMBROIDERY_RED = state(Blocks.WOOL.red());

    // --- Вода и магия ----------------------------------------------------

    /** Вода бассейна. Сапфировый оттенок задаётся атрибутами измерения. */
    public static final BlockState POOL_WATER = state(Blocks.WATER);

    /** Бортик бассейна. */
    public static final BlockState POOL_RIM = state(KHBlocks.WHITEWASHED_PLASTER.get());

    /**
     * Магический фон церемониальной арки.
     *
     * <p>Не портал: игрок не должен провалиться в другое измерение, глядя на
     * арку. Это плотный тёмно-синий объём, читающийся как бесконечность.</p>
     */
    public static final BlockState ARCH_VOID = state(Blocks.STAINED_GLASS.blue());

    // --- Свет ------------------------------------------------------------

    /** Основной тёплый источник: янтарный фонарь. */
    public static final BlockState LANTERN = state(Blocks.LANTERN);

    /** Редкий синий фонарь: акцент среди янтарных. */
    public static final BlockState LANTERN_BLUE = state(Blocks.SOUL_LANTERN);

    /** Скрытый источник света под мозаикой и в нишах. */
    public static final BlockState HIDDEN_LIGHT = state(Blocks.OCHRE_FROGLIGHT);

    /** Свечи на столиках и бортиках. */
    public static final BlockState CANDLE = state(Blocks.CANDLE);

    // --- Мебель и декор --------------------------------------------------

    /** Каркас низкой мебели и платформ сокровищницы. */
    public static final BlockState WOOD = state(Blocks.STRIPPED_SPRUCE_WOOD);

    /** Столешницы и полки. */
    public static final BlockState WOOD_SLAB = state(Blocks.SPRUCE_SLAB);

    /** Декоративная керамика мода: сосуды по нишам и чайному углу. */
    public static final BlockState CERAMIC = state(KHBlocks.DECORATIVE_CERAMIC.get());

    /** Сундук сокровищницы. */
    public static final BlockState CHEST = state(Blocks.CHEST);

    /** Сапфировые кристаллы сокровищницы. */
    public static final BlockState SAPPHIRE_CRYSTAL = state(Blocks.AMETHYST_BLOCK);

    /** Книжные шкафы архива желаний. */
    public static final BlockState BOOKSHELF = state(Blocks.BOOKSHELF);

    // --- Растительность --------------------------------------------------

    /** Подсолнух — повторяющийся символ джиннии. */
    public static final BlockState SUNFLOWER = state(Blocks.SUNFLOWER);

    /** Ваза под растение: сине-золотая керамика. */
    public static final BlockState PLANT_POT = state(Blocks.DECORATED_POT);

    /** Свисающие лозы на верхних балконах. */
    public static final BlockState VINE = state(Blocks.VINE);

    /** Трава магического сада. */
    public static final BlockState GARDEN_GRASS = state(Blocks.GRASS_BLOCK);

    private static BlockState state(Block block) {
        return block.defaultBlockState();
    }
}
