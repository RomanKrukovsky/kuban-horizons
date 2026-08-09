package dev.romankrukovsky.kubanhorizons.worldgen.palace;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Переиспользуемые компоненты дворца.
 *
 * <p>Каждый метод строит законченный элемент из десятков или сотен блоков.
 * Вызывающий код расставляет элементы по anchors и вариантам и никогда не
 * ставит отдельные декоративные блоки сам: иначе композиция утонула бы в
 * тысячах строк, а правка орнамента требовала бы поиска по всему дворцу.</p>
 *
 * <p>Все компоненты детерминированы: вариант выбирается параметром, а не
 * случайным числом, поэтому один и тот же anchor всегда даёт один результат.</p>
 */
public final class PalaceComponents {
    private PalaceComponents() {
    }

    /**
     * Зона отдыха: низкие диваны, подушки, ковёр и столик.
     *
     * @param variant 0..2 — три раскладки, чтобы зоны не повторялись буквально
     */
    public static void placeLoungeZone(PalaceWriter w, int x, int y, int z, Direction facing, int variant) {
        int span = 7;
        // Ковёр под зоной: связывает мебель в одну композицию.
        placeCarpetPatch(w, x, y, z, span, span, variant);

        // Диван по дальней стороне: две ступени дают низкую посадку.
        int back = span / 2;
        for (int i = -back; i <= back; i++) {
            int sx = facing.getStepX() != 0 ? x + facing.getStepX() * back : x + i;
            int sz = facing.getStepZ() != 0 ? z + facing.getStepZ() * back : z + i;
            w.set(sx, y + 1, sz, PalacePalette.WOOD);
            w.set(sx, y + 2, sz, PalacePalette.EMBROIDERY_RED);
        }

        // Подушки: количество зависит от варианта, положение — нет.
        int cushions = 3 + variant;
        for (int i = 0; i < cushions; i++) {
            int cx = x + (i % 3) - 1;
            int cz = z + (i / 3) - 1;
            w.set(cx, y + 1, cz, i % 2 == 0
                    ? PalacePalette.CARPET_CRIMSON
                    : PalacePalette.CARPET_BLUE);
        }

        // Низкий столик со свечой и сосудом.
        int tx = x - facing.getStepX() * 2;
        int tz = z - facing.getStepZ() * 2;
        w.set(tx, y + 1, tz, PalacePalette.WOOD_SLAB);
        w.set(tx, y + 2, tz, variant == 1 ? PalacePalette.CERAMIC : PalacePalette.CANDLE);
    }

    /**
     * Ниша сокровищ: платформы, сундуки, золото и кристаллы.
     *
     * @param richness 1..3 — насколько плотно набита ниша; управляет ощущением
     *                 «собрано за сотни лет», а не одинаковой кучей
     */
    public static void placeTreasureAlcove(PalaceWriter w, int x, int y, int z, Direction facing, int richness) {
        int depth = 3;
        int width = 5;

        // Задняя стенка ниши золотая: подсвечивает содержимое.
        for (int i = -width / 2; i <= width / 2; i++) {
            for (int h = 0; h < 4; h++) {
                int bx = x + facing.getStepX() * depth + (facing.getStepZ() != 0 ? i : 0);
                int bz = z + facing.getStepZ() * depth + (facing.getStepX() != 0 ? i : 0);
                w.set(bx, y + h, bz, PalacePalette.GOLD);
            }
        }

        // Деревянные платформы разной высоты: срез коллекции, а не полка.
        for (int level = 0; level < richness; level++) {
            int py = y + level;
            int inset = level;
            for (int i = -width / 2 + inset; i <= width / 2 - inset; i++) {
                int px = x + facing.getStepX() * (depth - 1 - level) + (facing.getStepZ() != 0 ? i : 0);
                int pz = z + facing.getStepZ() * (depth - 1 - level) + (facing.getStepX() != 0 ? i : 0);
                w.set(px, py, pz, PalacePalette.WOOD_SLAB);

                // Содержимое чередуется, чтобы не читалось как один материал.
                BlockState treasure = switch ((i + level + 4) % 4) {
                    case 0 -> PalacePalette.CHEST;
                    case 1 -> PalacePalette.GOLD;
                    case 2 -> PalacePalette.SAPPHIRE_CRYSTAL;
                    default -> PalacePalette.CERAMIC;
                };
                if ((i + level) % 2 == 0) {
                    w.set(px, py + 1, pz, treasure);
                }
            }
        }
    }

    /**
     * Гроздь висящих фонарей на цепях разной длины.
     *
     * @param heightProfile длины цепей; определяет, насколько «живой» выглядит
     *                      потолок — одинаковые длины дали бы сетку
     */
    public static void placeLanternCluster(PalaceWriter w, int x, int ceilingY, int z,
            int radius, int[] heightProfile) {
        int index = 0;
        for (int dx = -radius; dx <= radius; dx += 2) {
            for (int dz = -radius; dz <= radius; dz += 2) {
                if (dx * dx + dz * dz > radius * radius) {
                    continue;
                }
                int drop = heightProfile[index % heightProfile.length];
                index++;

                for (int i = 0; i < drop; i++) {
                    w.set(x + dx, ceilingY - i, z + dz, PalacePalette.GOLD_CHAIN);
                }
                // Каждый пятый фонарь синий: редкий акцент из брифа.
                w.set(x + dx, ceilingY - drop, z + dz,
                        index % 5 == 0 ? PalacePalette.LANTERN_BLUE : PalacePalette.LANTERN);
            }
        }
    }

    /**
     * Арка с золотой окантовкой и красно-белым орнаментом.
     *
     * @param pattern 0 — плотная золотая окантовка, 1 — с вышивкой по краю
     */
    public static void placeEmbroideredArch(PalaceWriter w, int x, int y, int z,
            int width, int height, Direction facing, int pattern) {
        int half = width / 2;

        for (int i = -half; i <= half; i++) {
            // Высота проёма идёт по дуге: полукруг сверху, стойки по краям.
            double t = (double) Math.abs(i) / half;
            int columnTop = (int) Math.round(height * Math.cos(t * Math.PI / 2.0D));

            int px = facing.getStepZ() != 0 ? x + i : x;
            int pz = facing.getStepZ() != 0 ? z : z + i;

            // Окантовка проёма.
            w.set(px, y + columnTop, pz, PalacePalette.GOLD);
            if (pattern == 1 && Math.abs(i) % 2 == 0) {
                w.set(px, y + columnTop - 1, pz, PalacePalette.EMBROIDERY_RED);
            }

            // Стойки по краям арки.
            if (Math.abs(i) >= half - 1) {
                for (int h = 0; h < columnTop; h++) {
                    w.set(px, y + h, pz, PalacePalette.WALL);
                }
            }
        }
    }

    /**
     * Кровать под тканевым навесом — личное пространство джиннии.
     *
     * @param variant 0 — навес на четырёх стойках, 1 — полузакрытый с боковиной
     */
    public static void placeCanopyBed(PalaceWriter w, int x, int y, int z, Direction facing, int variant) {
        int width = 5;
        int depth = 6;
        int postHeight = 5;

        // Ложе: приподнято на одну ступень.
        for (int dx = 0; dx < width; dx++) {
            for (int dz = 0; dz < depth; dz++) {
                w.set(x + dx, y, z + dz, PalacePalette.WOOD);
                w.set(x + dx, y + 1, z + dz, PalacePalette.CARPET_WHITE);
            }
        }

        // Подушки у изголовья.
        for (int dx = 1; dx < width - 1; dx++) {
            w.set(x + dx, y + 2, z, PalacePalette.CARPET_CRIMSON);
        }

        // Стойки навеса.
        int[][] posts = {{0, 0}, {width - 1, 0}, {0, depth - 1}, {width - 1, depth - 1}};
        for (int[] p : posts) {
            for (int h = 1; h <= postHeight; h++) {
                w.set(x + p[0], y + h, z + p[1], PalacePalette.GOLD_CHAIN);
            }
        }

        // Полотно навеса: кремовое с красной каймой по периметру.
        int canopyY = y + postHeight + 1;
        for (int dx = 0; dx < width; dx++) {
            for (int dz = 0; dz < depth; dz++) {
                boolean edge = dx == 0 || dz == 0 || dx == width - 1 || dz == depth - 1;
                w.set(x + dx, canopyY, z + dz, edge
                        ? PalacePalette.EMBROIDERY_RED
                        : PalacePalette.BANNER_IVORY.defaultBlockState());
            }
        }

        // Боковина полузакрытого варианта.
        if (variant == 1) {
            for (int dz = 1; dz < depth - 1; dz++) {
                for (int h = 2; h <= postHeight; h++) {
                    w.set(x, y + h, z + dz, PalacePalette.BANNER_IVORY.defaultBlockState());
                }
            }
        }
    }

    /**
     * Вертикальное полотно на стене.
     *
     * @param sapphire true — тёмно-синее с золотым символом, false — белое с вышивкой
     */
    public static void placeEmbroideryPanel(PalaceWriter w, int x, int y, int z,
            int height, boolean sapphire) {
        BlockState cloth = sapphire
                ? PalacePalette.BANNER_SAPPHIRE.defaultBlockState()
                : PalacePalette.BANNER_IVORY.defaultBlockState();
        BlockState accent = sapphire ? PalacePalette.GOLD : PalacePalette.EMBROIDERY_RED;

        for (int h = 0; h < height; h++) {
            for (int dx = 0; dx < 2; dx++) {
                // Символ в середине полотна: читается с расстояния.
                boolean symbol = h == height / 2 || h == height / 2 - 1;
                w.set(x + dx, y + h, z, symbol ? accent : cloth);
            }
        }
    }

    /** Подсолнух в сине-золотой вазе — символ джиннии. */
    public static void placeSunflowerPot(PalaceWriter w, int x, int y, int z) {
        w.set(x, y, z, PalacePalette.PLANT_POT);
        w.set(x, y + 1, z, PalacePalette.SUNFLOWER);
    }

    /**
     * Балкон второго яруса с золотыми перилами.
     *
     * @param span длина балкона вдоль стены
     */
    public static void placeBalcony(PalaceWriter w, int x, int y, int z,
            Direction outward, int span) {
        int depth = 3;
        int half = span / 2;

        for (int i = -half; i <= half; i++) {
            for (int d = 0; d < depth; d++) {
                int bx = x - outward.getStepX() * d + (outward.getStepZ() != 0 ? i : 0);
                int bz = z - outward.getStepZ() * d + (outward.getStepX() != 0 ? i : 0);
                w.set(bx, y, bz, PalacePalette.WALL_STEP);

                // Перила по внутреннему краю: с самого балкона не упасть.
                if (d == depth - 1) {
                    w.set(bx, y + 1, bz, PalacePalette.GOLD_LATTICE);
                }
            }
        }
        // Свисающие лозы по краю: бриф просит зелень на верхних балконах.
        for (int i = -half; i <= half; i += 4) {
            int vx = x - outward.getStepX() * (depth - 1) + (outward.getStepZ() != 0 ? i : 0);
            int vz = z - outward.getStepZ() * (depth - 1) + (outward.getStepX() != 0 ? i : 0);
            w.set(vx, y - 1, vz, PalacePalette.VINE);
            w.set(vx, y - 2, vz, PalacePalette.VINE);
        }
    }

    /**
     * Ниша первого яруса: углубление в стене со скрытым светом.
     *
     * @param content 0 — керамика, 1 — свеча, 2 — подсолнух
     */
    public static void placeAlcove(PalaceWriter w, int x, int y, int z,
            Direction inward, int content) {
        int height = 4;
        int width = 3;
        int depth = 2;

        for (int i = -width / 2; i <= width / 2; i++) {
            for (int h = 0; h < height; h++) {
                for (int d = 0; d < depth; d++) {
                    int nx = x + inward.getStepX() * d + (inward.getStepZ() != 0 ? i : 0);
                    int nz = z + inward.getStepZ() * d + (inward.getStepX() != 0 ? i : 0);
                    w.set(nx, y + h, nz, Blocks.AIR.defaultBlockState());
                }
            }
        }

        // Скрытый свет в глубине: ниша светится, источник не виден.
        int lx = x + inward.getStepX() * (depth - 1);
        int lz = z + inward.getStepZ() * (depth - 1);
        w.set(lx, y + height - 1, lz, PalacePalette.HIDDEN_LIGHT);

        BlockState item = switch (content) {
            case 0 -> PalacePalette.CERAMIC;
            case 1 -> PalacePalette.CANDLE;
            default -> PalacePalette.SUNFLOWER;
        };
        w.set(lx, y, lz, item);
    }

    /**
     * Мозаичный участок пола.
     *
     * <p>Узор геометрический и детерминированный: концентрические ромбы из
     * красного, белого и бордового с редкими синими и золотыми вставками.</p>
     */
    public static void placeCarpetPatch(PalaceWriter w, int cx, int y, int cz,
            int spanX, int spanZ, int pattern) {
        for (int x = -spanX / 2; x <= spanX / 2; x++) {
            for (int z = -spanZ / 2; z <= spanZ / 2; z++) {
                int ring = Math.abs(x) + Math.abs(z) + pattern;
                BlockState carpet = switch (ring % 5) {
                    case 0 -> PalacePalette.CARPET_RED;
                    case 1 -> PalacePalette.CARPET_WHITE;
                    case 2 -> PalacePalette.CARPET_CRIMSON;
                    case 3 -> PalacePalette.CARPET_BLUE;
                    default -> PalacePalette.CARPET_RED;
                };
                w.set(cx + x, y, cz + z, carpet);
            }
        }
    }

    /** Книжный шкаф архива желаний с золотой табличкой. */
    public static void placeBookshelfBay(PalaceWriter w, int x, int y, int z,
            int width, int height) {
        for (int i = 0; i < width; i++) {
            for (int h = 0; h < height; h++) {
                // Каждая третья полка золотая: таблички исполненных желаний.
                w.set(x + i, y + h, z, h % 3 == 2
                        ? PalacePalette.GOLD
                        : PalacePalette.BOOKSHELF);
            }
        }
    }

    /** Колонна от пола до заданной высоты с золотыми капителью и базой. */
    public static void placeColumn(PalaceWriter w, int x, int y, int z, int height) {
        w.set(x, y, z, PalacePalette.GOLD);
        for (int h = 1; h < height - 1; h++) {
            w.set(x, y + h, z, PalacePalette.WALL);
        }
        w.set(x, y + height - 1, z, PalacePalette.GOLD);
    }

    /** Лестница из нескольких широких ступеней. */
    public static void placeGrandStairs(PalaceWriter w, int cx, int y, int z,
            int width, int steps, Direction rise) {
        int half = width / 2;
        for (int s = 0; s < steps; s++) {
            int sz = z + rise.getStepZ() * s;
            int sx = cx + rise.getStepX() * s;
            for (int i = -half; i <= half; i++) {
                int px = rise.getStepZ() != 0 ? cx + i : sx;
                int pz = rise.getStepZ() != 0 ? sz : z + i;
                for (int h = 0; h <= s; h++) {
                    w.set(px, y + h, pz, PalacePalette.WALL_STEP);
                }
            }
        }
    }
}
