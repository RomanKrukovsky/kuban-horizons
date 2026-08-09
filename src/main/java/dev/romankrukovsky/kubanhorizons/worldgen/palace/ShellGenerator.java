package dev.romankrukovsky.kubanhorizons.worldgen.palace;

import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Оболочка зала: пол, ступенчатые стены и купол.
 *
 * <p>Крупные формы читаются первыми, поэтому оболочка строится до любого декора.
 * Стены не вертикальный цилиндр: радиус овала сужается уступами, а выше
 * {@link PalaceGrid#DOME_START_Y} переходит в свод по дуге окружности.</p>
 */
public final class ShellGenerator {
    /** Шаг уступа стены по высоте: каждые 4 блока стена отступает наружу. */
    private static final int LEDGE_STEP = 4;

    private ShellGenerator() {
    }

    /**
     * Строит оболочку зала.
     *
     * @param w      writer с обрезкой по чанку и подсчётом записей
     * @param originX мировая координата центра зала по X
     * @param floorY  мировая высота пола зала
     * @param originZ мировая координата центра зала по Z
     */
    public static void generate(PalaceWriter w, int originX, int floorY, int originZ) {
        floor(w, originX, floorY, originZ);
        walls(w, originX, floorY, originZ);
        dome(w, originX, floorY, originZ);
    }

    /**
     * Пол зала и плотное основание под ним.
     *
     * <p>Под полом кладётся ещё три слоя: дворец стоит на парящем острове,
     * поэтому снизу он обязан выглядеть массивом камня, а не тонкой плёнкой.</p>
     */
    private static void floor(PalaceWriter w, int cx, int floorY, int cz) {
        int radiusX = PalaceGrid.hallRadiusXAt(PalaceGrid.FLOOR_Y);
        int radiusZ = PalaceGrid.hallRadiusZAt(PalaceGrid.FLOOR_Y);

        for (int x = -radiusX - PalaceGrid.WALL_THICKNESS; x <= radiusX + PalaceGrid.WALL_THICKNESS; x++) {
            for (int z = -radiusZ - PalaceGrid.WALL_THICKNESS; z <= radiusZ + PalaceGrid.WALL_THICKNESS; z++) {
                boolean inside = PalaceGrid.insideHall(x, z, PalaceGrid.FLOOR_Y);
                boolean underWall = isWallRing(x, z, PalaceGrid.FLOOR_Y);
                if (!inside && !underWall) {
                    continue;
                }
                w.set(cx + x, floorY, cz + z, PalacePalette.FLOOR_BASE);
                for (int depth = 1; depth <= 3; depth++) {
                    w.set(cx + x, floorY - depth, cz + z, PalacePalette.DOME);
                }
            }
        }
    }

    /**
     * Ступенчатые стены с уступами и карнизами.
     *
     * <p>Каждый {@link #LEDGE_STEP}-й уровень получает карниз из плит: он ловит
     * свет фонарей и разбивает вертикаль, из-за которой зал выглядел бы
     * цилиндрической цистерной.</p>
     */
    private static void walls(PalaceWriter w, int cx, int floorY, int cz) {
        for (int y = 1; y < PalaceGrid.DOME_START_Y; y++) {
            int radiusX = PalaceGrid.hallRadiusXAt(y);
            int radiusZ = PalaceGrid.hallRadiusZAt(y);
            boolean ledge = y % LEDGE_STEP == 0;

            for (int x = -radiusX - PalaceGrid.WALL_THICKNESS; x <= radiusX + PalaceGrid.WALL_THICKNESS; x++) {
                for (int z = -radiusZ - PalaceGrid.WALL_THICKNESS; z <= radiusZ + PalaceGrid.WALL_THICKNESS; z++) {
                    if (!isWallRing(x, z, y)) {
                        continue;
                    }
                    // Карниз ставится плитой поверх кладки, поэтому силуэт стены
                    // получает горизонтальную линию без утолщения самой стены.
                    BlockState state = ledge ? PalacePalette.WALL_STEP : PalacePalette.WALL;
                    w.set(cx + x, floorY + y, cz + z, state);
                }
            }
        }
    }

    /**
     * Ступенчатый купол.
     *
     * <p>Кольца купола идут по дуге из {@link PalaceGrid#insideHall}, поэтому
     * форма замыкается сама и не требует отдельной сферы. Каждое четвёртое
     * кольцо — золотое: бриф требует золотых фрагментов на своде.</p>
     */
    private static void dome(PalaceWriter w, int cx, int floorY, int cz) {
        for (int y = PalaceGrid.DOME_START_Y; y <= PalaceGrid.DOME_TOP_Y; y++) {
            int radiusX = PalaceGrid.hallRadiusXAt(y);
            int radiusZ = PalaceGrid.hallRadiusZAt(y);
            if (radiusX <= 0 || radiusZ <= 0) {
                continue;
            }
            boolean goldRing = (y - PalaceGrid.DOME_START_Y) % 4 == 0;

            for (int x = -radiusX - PalaceGrid.WALL_THICKNESS; x <= radiusX + PalaceGrid.WALL_THICKNESS; x++) {
                for (int z = -radiusZ - PalaceGrid.WALL_THICKNESS; z <= radiusZ + PalaceGrid.WALL_THICKNESS; z++) {
                    if (!isWallRing(x, z, y)) {
                        continue;
                    }
                    BlockState state = goldRing ? PalacePalette.GOLD : PalacePalette.DOME;
                    w.set(cx + x, floorY + y, cz + z, state);
                }
            }
        }

        // Замковый камень: на вершине радиус вырождается, и без явной пробки
        // в куполе остаётся отверстие.
        int topY = floorY + PalaceGrid.DOME_TOP_Y + 1;
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                w.set(cx + x, topY, cz + z, PalacePalette.GOLD);
            }
        }

        // Магические трещины: несколько отверстий в своде, через которые видна
        // фиолетовая пустота измерения. Детерминированные позиции, не случайные.
        crack(w, cx + 18, floorY + PalaceGrid.DOME_START_Y + 6, cz - 12);
        crack(w, cx - 24, floorY + PalaceGrid.DOME_START_Y + 4, cz + 16);
        crack(w, cx + 8, floorY + PalaceGrid.DOME_START_Y + 10, cz + 26);
    }

    /** Пробивает в своде небольшое отверстие в пустоту. */
    private static void crack(PalaceWriter w, int x, int y, int z) {
        for (int dx = 0; dx <= 1; dx++) {
            for (int dz = 0; dz <= 1; dz++) {
                w.set(x + dx, y, z + dz, Blocks.AIR.defaultBlockState());
            }
        }
    }

    /**
     * Принадлежит ли точка кольцу стены на данной высоте.
     *
     * <p>Кольцо — это точки вне внутреннего объёма, но в пределах толщины
     * стены от него. Так одна формула описывает и наклонные стены, и купол.</p>
     */
    private static boolean isWallRing(int x, int z, int y) {
        if (PalaceGrid.insideHall(x, z, y)) {
            return false;
        }
        for (int t = 1; t <= PalaceGrid.WALL_THICKNESS; t++) {
            if (PalaceGrid.insideHall(shrink(x, t), shrink(z, t), y)) {
                return true;
            }
        }
        return false;
    }

    /** Сдвигает координату на t в сторону центра. */
    private static int shrink(int value, int t) {
        if (value > 0) {
            return Math.max(0, value - t);
        }
        return Math.min(0, value + t);
    }
}
