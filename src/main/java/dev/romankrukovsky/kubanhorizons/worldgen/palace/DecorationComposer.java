package dev.romankrukovsky.kubanhorizons.worldgen.palace;

import net.minecraft.core.Direction;

/**
 * Композитор декора: расставляет готовые компоненты по anchors.
 *
 * <p>Ни один декоративный блок здесь не ставится напрямую — только вызовы
 * компонентов из {@link PalaceComponents}. Левая половина зала сокровищная,
 * правая домашняя, как требует бриф, и обе несимметричны относительно друг
 * друга.</p>
 */
public final class DecorationComposer {
    /**
     * Anchors зон отдыха в правой половине: {@code {x, z, вариант, поворот}}.
     *
     * <p>Смещения намеренно неровные: ряд одинаково расставленных диванов
     * читался бы как мебельный магазин, а не как жилое пространство.</p>
     */
    private static final int[][] LOUNGE_ANCHORS = {
            {22, 14, 0, 0},
            {34, 6, 1, 1},
            {20, 30, 2, 2},
            {38, 24, 1, 0},
            {30, -8, 0, 3},
    };

    /** Anchors ниш сокровищ в левой половине: {@code {x, z, богатство}}. */
    private static final int[][] TREASURE_ANCHORS = {
            {-24, 12, 3},
            {-36, 2, 2},
            {-20, 26, 3},
            {-42, 18, 1},
            {-30, -12, 2},
            {-46, -4, 3},
    };

    /** Anchors подсолнухов: 8 штук, разбросаны по обеим половинам. */
    private static final int[][] SUNFLOWER_ANCHORS = {
            {-14, 34}, {16, 36}, {-40, 26}, {44, 12},
            {-8, -30}, {12, -34}, {-46, 8}, {40, -18},
    };

    /** Anchors полотен на стенах: {@code {x, z, sapphire}}. */
    private static final int[][] PANEL_ANCHORS = {
            {-30, 46, 1}, {-10, 46, 0}, {14, 46, 1}, {34, 46, 0},
            {-34, -46, 0}, {-12, -46, 1}, {18, -46, 0}, {36, -46, 1},
    };

    private DecorationComposer() {
    }

    public static void generate(PalaceWriter w, int cx, int floorY, int cz) {
        floorMosaic(w, cx, floorY, cz);
        loungeZones(w, cx, floorY, cz);
        treasureAlcoves(w, cx, floorY, cz);
        personalQuarter(w, cx, floorY, cz);
        wishArchive(w, cx, floorY, cz);
        sunflowers(w, cx, floorY, cz);
        wallPanels(w, cx, floorY, cz);
    }

    /**
     * Мозаика пола: центральный медальон и пересекающиеся дорожки.
     *
     * <p>Не один гигантский ковёр: медальон вокруг бассейна, дорожки по осям и
     * отдельные ковры у мебели — как требует бриф.</p>
     */
    private static void floorMosaic(PalaceWriter w, int cx, int floorY, int cz) {
        int y = floorY + 1;

        // Центральный медальон вокруг бассейна.
        int inner = PalaceGrid.POOL_HALF + 2;
        int outer = PalaceGrid.POOL_HALF + 10;
        for (int x = -outer; x <= outer; x++) {
            for (int z = -outer; z <= outer; z++) {
                int d = Math.abs(x) + Math.abs(z);
                if (d < inner || d > outer) {
                    continue;
                }
                int ring = d - inner;
                w.set(cx + x, y, cz + z, switch (ring % 4) {
                    case 0 -> PalacePalette.CARPET_RED;
                    case 1 -> PalacePalette.CARPET_WHITE;
                    case 2 -> PalacePalette.CARPET_CRIMSON;
                    default -> PalacePalette.CARPET_BLUE;
                });
            }
        }

        // Поперечная дорожка по оси X: связывает левую и правую половины.
        for (int x = -PalaceGrid.HALL_RADIUS_X + 8; x <= PalaceGrid.HALL_RADIUS_X - 8; x++) {
            if (Math.abs(x) <= PalaceGrid.POOL_HALF + 1) {
                continue;
            }
            for (int z = -2; z <= 2; z++) {
                w.set(cx + x, y, cz + z, Math.abs(z) == 2
                        ? PalacePalette.CARPET_CRIMSON
                        : PalacePalette.CARPET_WHITE);
            }
        }
    }

    /** Зоны отдыха правой половины. */
    private static void loungeZones(PalaceWriter w, int cx, int floorY, int cz) {
        Direction[] facings = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};
        for (int[] a : LOUNGE_ANCHORS) {
            PalaceComponents.placeLoungeZone(w, cx + a[0], floorY + 1, cz + a[1],
                    facings[a[3]], a[2]);
        }
    }

    /** Ниши сокровищ левой половины на разных ярусах. */
    private static void treasureAlcoves(PalaceWriter w, int cx, int floorY, int cz) {
        for (int i = 0; i < TREASURE_ANCHORS.length; i++) {
            int[] a = TREASURE_ANCHORS[i];
            // Каждая третья ниша поднята на второй ярус: коллекция идёт вверх.
            int y = floorY + 1 + (i % 3 == 2 ? PalaceGrid.SECOND_TIER_Y : 0);
            PalaceComponents.placeTreasureAlcove(w, cx + a[0], y, cz + a[1],
                    Direction.WEST, a[2]);
        }
    }

    /** Личный угол джиннии: кровать под навесом, чайный столик, керамика. */
    private static void personalQuarter(PalaceWriter w, int cx, int floorY, int cz) {
        int x = cx + 26;
        int z = cz - 26;

        PalaceComponents.placeCanopyBed(w, x, floorY + 1, z, Direction.WEST, 1);
        PalaceComponents.placeCarpetPatch(w, x + 2, floorY + 1, z + 8, 9, 7, 2);

        // Чайный угол рядом с кроватью.
        PalaceComponents.placeLoungeZone(w, x - 8, floorY + 1, z + 6, Direction.EAST, 1);
        w.set(x - 8, floorY + 2, z + 6, PalacePalette.CERAMIC);
    }

    /** Архив желаний: книжные бухты вдоль дальней левой стены. */
    private static void wishArchive(PalaceWriter w, int cx, int floorY, int cz) {
        int z = cz - 34;
        for (int i = 0; i < 4; i++) {
            int x = cx - 44 + i * 9;
            PalaceComponents.placeBookshelfBay(w, x, floorY + 1, z, 6, 6);
        }
        // Ковёр перед архивом, чтобы зона не висела в пустом полу.
        PalaceComponents.placeCarpetPatch(w, cx - 30, floorY + 1, z + 5, 25, 5, 1);
    }

    /** Подсолнухи как повторяющийся символ. */
    private static void sunflowers(PalaceWriter w, int cx, int floorY, int cz) {
        for (int[] a : SUNFLOWER_ANCHORS) {
            PalaceComponents.placeSunflowerPot(w, cx + a[0], floorY + 1, cz + a[1]);
        }
    }

    /** Вертикальные полотна на стенах первого и второго ярусов. */
    private static void wallPanels(PalaceWriter w, int cx, int floorY, int cz) {
        for (int[] a : PANEL_ANCHORS) {
            // Полотна начинаются выше человеческого роста: они фон, а не мебель.
            PalaceComponents.placeEmbroideryPanel(w, cx + a[0], floorY + 4, cz + a[1],
                    7, a[2] == 1);
        }
    }
}
