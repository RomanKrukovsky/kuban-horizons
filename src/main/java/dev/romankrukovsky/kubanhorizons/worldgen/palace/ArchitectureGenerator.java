package dev.romankrukovsky.kubanhorizons.worldgen.palace;

import net.minecraft.core.Direction;

/**
 * Ярусы зала: ниши первого уровня, балконы второго, окна третьего.
 *
 * <p>Центральная ось симметрична, боковые зоны — нет. Асимметрия задаётся
 * таблицами вариантов, а не случайностью, поэтому дворец выглядит обжитым и при
 * этом воспроизводится побайтово.</p>
 */
public final class ArchitectureGenerator {
    /**
     * Варианты содержимого ниш по кругу.
     *
     * <p>Длина массива взаимно проста с числом ниш, поэтому наполнение не
     * попадает в период и стены не читаются как повтор.</p>
     */
    private static final int[] ALCOVE_CONTENT = {0, 2, 1, 0, 1, 2, 0};

    /** Длины цепей фонарных гроздей: разная высота вместо ровной сетки. */
    private static final int[] LANTERN_PROFILE = {3, 6, 4, 8, 5, 7};

    private ArchitectureGenerator() {
    }

    public static void generate(PalaceWriter w, int cx, int floorY, int cz) {
        alcoves(w, cx, floorY, cz);
        balconies(w, cx, floorY, cz);
        voidWindows(w, cx, floorY, cz);
        lanterns(w, cx, floorY, cz);
        columns(w, cx, floorY, cz);
    }

    /** Ниши первого яруса по длинным стенам. */
    private static void alcoves(PalaceWriter w, int cx, int floorY, int cz) {
        int y = floorY + 1;
        int radiusZ = PalaceGrid.hallRadiusZAt(PalaceGrid.FLOOR_Y + 1);
        int count = PalaceGrid.ALCOVES_PER_SIDE;
        int step = (PalaceGrid.HALL_SPAN_X - 24) / (count - 1);
        int variant = 0;

        for (int side = -1; side <= 1; side += 2) {
            for (int i = 0; i < count; i++) {
                int x = cx - (PalaceGrid.HALL_SPAN_X - 24) / 2 + i * step;
                int z = cz + side * (radiusZ - 1);
                Direction inward = side < 0 ? Direction.SOUTH : Direction.NORTH;
                PalaceComponents.placeAlcove(w, x, y, z, inward,
                        ALCOVE_CONTENT[variant % ALCOVE_CONTENT.length]);
                variant++;
            }
        }
    }

    /** Балконы второго яруса. */
    private static void balconies(PalaceWriter w, int cx, int floorY, int cz) {
        int y = floorY + PalaceGrid.SECOND_TIER_Y;
        int radiusZ = PalaceGrid.hallRadiusZAt(PalaceGrid.SECOND_TIER_Y);
        int count = PalaceGrid.BALCONIES_PER_SIDE;
        int step = (PalaceGrid.HALL_SPAN_X - 32) / (count - 1);

        for (int side = -1; side <= 1; side += 2) {
            for (int i = 0; i < count; i++) {
                int x = cx - (PalaceGrid.HALL_SPAN_X - 32) / 2 + i * step;
                int z = cz + side * (radiusZ - 1);
                Direction outward = side < 0 ? Direction.NORTH : Direction.SOUTH;
                // Длина балкона чередуется: 9 и 7, чтобы ряд не был линейкой.
                int span = i % 2 == 0 ? 9 : 7;
                PalaceComponents.placeBalcony(w, x, y, z, outward, span);
            }
        }
    }

    /**
     * Окна третьего яруса в магическую пустоту.
     *
     * <p>Проёмы пробиваются насквозь через стену, поэтому в них видна
     * сапфировая бесконечность измерения, а не соседний блок кладки.</p>
     */
    private static void voidWindows(PalaceWriter w, int cx, int floorY, int cz) {
        int y = floorY + PalaceGrid.THIRD_TIER_Y;
        int radiusZ = PalaceGrid.hallRadiusZAt(PalaceGrid.THIRD_TIER_Y);

        for (int side = -1; side <= 1; side += 2) {
            for (int i = -2; i <= 2; i++) {
                int x = cx + i * 18;
                int z = cz + side * radiusZ;
                for (int h = 0; h < 4; h++) {
                    for (int t = -PalaceGrid.WALL_THICKNESS - 1; t <= PalaceGrid.WALL_THICKNESS + 1; t++) {
                        int wz = z + side * t;
                        w.set(x, y + h, wz, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
                        w.set(x + 1, y + h, wz, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
                    }
                }
                // Золотая решётка в проёме: окно, а не пробоина.
                w.set(x, y + 1, z, PalacePalette.GOLD_LATTICE);
                w.set(x + 1, y + 1, z, PalacePalette.GOLD_LATTICE);
            }
        }
    }

    /** Грозди фонарей под куполом на разной высоте. */
    private static void lanterns(PalaceWriter w, int cx, int floorY, int cz) {
        int ceilingY = floorY + PalaceGrid.DOME_START_Y - 1;

        // Центральная гроздь над бассейном — самая крупная.
        PalaceComponents.placeLanternCluster(w, cx, ceilingY, cz, 8, LANTERN_PROFILE);

        // Боковые грозди смещены неравномерно: композиция, а не сетка.
        PalaceComponents.placeLanternCluster(w, cx - 34, ceilingY - 2, cz - 14, 5, LANTERN_PROFILE);
        PalaceComponents.placeLanternCluster(w, cx + 30, ceilingY - 1, cz + 18, 6, LANTERN_PROFILE);
        PalaceComponents.placeLanternCluster(w, cx + 38, ceilingY - 3, cz - 20, 4, LANTERN_PROFILE);
        PalaceComponents.placeLanternCluster(w, cx - 26, ceilingY - 1, cz + 22, 5, LANTERN_PROFILE);
    }

    /** Колонны по периметру, поддерживающие второй ярус. */
    private static void columns(PalaceWriter w, int cx, int floorY, int cz) {
        int radiusZ = PalaceGrid.hallRadiusZAt(PalaceGrid.FLOOR_Y + 1);
        int height = PalaceGrid.SECOND_TIER_Y;

        for (int side = -1; side <= 1; side += 2) {
            for (int i = -3; i <= 3; i++) {
                // Пропуск центральной колонны: ось зала должна быть открыта.
                if (i == 0) {
                    continue;
                }
                int x = cx + i * 15;
                int z = cz + side * (radiusZ - 4);
                PalaceComponents.placeColumn(w, x, floorY + 1, z, height);
            }
        }
    }
}
