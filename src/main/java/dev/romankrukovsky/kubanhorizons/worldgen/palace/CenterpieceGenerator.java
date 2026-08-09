package dev.romankrukovsky.kubanhorizons.worldgen.palace;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;

/**
 * Композиционный центр зала: магический бассейн и церемониальная арка.
 *
 * <p>Это два главных ориентира, на которые игрок смотрит из точки входа,
 * поэтому их координаты зафиксированы в {@link PalaceGrid} и не зависят ни от
 * шума, ни от seed.</p>
 */
public final class CenterpieceGenerator {
    private CenterpieceGenerator() {
    }

    public static void generate(PalaceWriter w, int cx, int floorY, int cz) {
        fountain(w, cx, floorY, cz);
        ceremonialArch(w, cx, floorY, cz);
    }

    /**
     * Восьмиугольный бассейн с бортиком, фонарями и подсветкой воды.
     *
     * <p>Восьмиугольник, а не квадрат: круглая планировка зала требует, чтобы
     * центральный объект не спорил с ней прямыми углами.</p>
     */
    private static void fountain(PalaceWriter w, int cx, int floorY, int cz) {
        int half = PalaceGrid.POOL_HALF;

        for (int x = -half - 1; x <= half + 1; x++) {
            for (int z = -half - 1; z <= half + 1; z++) {
                int octagon = Math.abs(x) + Math.abs(z);
                boolean insidePool = octagon <= half + 2 && Math.abs(x) <= half && Math.abs(z) <= half;
                if (!insidePool) {
                    continue;
                }

                boolean rim = octagon > half - 1 || Math.abs(x) == half || Math.abs(z) == half;
                if (rim) {
                    // Бортик: белый камень с золотом по углам восьмиугольника.
                    boolean corner = Math.abs(x) == Math.abs(z);
                    w.set(cx + x, floorY, cz + z, corner
                            ? PalacePalette.GOLD
                            : PalacePalette.POOL_RIM);
                    continue;
                }

                // Чаша: вода на глубину, светящееся дно даёт свечение изнутри.
                w.set(cx + x, floorY - PalaceGrid.POOL_DEPTH, cz + z, PalacePalette.HIDDEN_LIGHT);
                for (int d = PalaceGrid.POOL_DEPTH - 1; d >= 0; d--) {
                    w.set(cx + x, floorY - d, cz + z, PalacePalette.POOL_WATER);
                }
            }
        }

        // Золотые фонари по четырём углам бассейна.
        int lanternOffset = half - 1;
        for (int sx = -1; sx <= 1; sx += 2) {
            for (int sz = -1; sz <= 1; sz += 2) {
                int lx = cx + sx * lanternOffset;
                int lz = cz + sz * lanternOffset;
                w.set(lx, floorY + 1, lz, PalacePalette.GOLD);
                w.set(lx, floorY + 2, lz, PalacePalette.LANTERN);
            }
        }

        magicSpiral(w, cx, floorY, cz);
    }

    /**
     * Спиральный поток магии над бассейном.
     *
     * <p>Блочная основа спирали: полупрозрачное стекло по параметрической
     * кривой. Она задаёт форму и держится без клиента, а живой дым, деление на
     * ленты и слияние добавляет клиентский рендер поверх этой оси.</p>
     *
     * <p>Кривая детерминирована: угол зависит только от высоты, поэтому спираль
     * одинакова при каждой генерации и проверяется тестом.</p>
     */
    private static void magicSpiral(PalaceWriter w, int cx, int floorY, int cz) {
        int height = PalaceGrid.POOL_SPIRAL_HEIGHT;

        for (int i = 0; i < height; i++) {
            double t = (double) i / height;
            // Два витка на всю высоту: S-образные завитки из брифа.
            double angle = t * Math.PI * 4.0D;
            // Радиус расширяется к середине и сужается к вершине, поэтому поток
            // выглядит живым, а не цилиндрической пружиной.
            double radius = 1.5D + 2.5D * Math.sin(t * Math.PI);

            int x = cx + (int) Math.round(Math.cos(angle) * radius);
            int z = cz + (int) Math.round(Math.sin(angle) * radius);
            int y = floorY + 1 + i;

            w.set(x, y, z, PalacePalette.ARCH_VOID);

            // Вторая лента в противофазе: поток иногда делится надвое.
            if (i > height / 3 && i < height * 2 / 3) {
                int x2 = cx + (int) Math.round(Math.cos(angle + Math.PI) * radius);
                int z2 = cz + (int) Math.round(Math.sin(angle + Math.PI) * radius);
                w.set(x2, y, z2, PalacePalette.ARCH_VOID);
            }
        }
    }

    /**
     * Церемониальная арка в дальнем конце зала с лестницей.
     *
     * <p>За аркой — плотный тёмно-синий объём, а не портал: игрок не должен
     * провалиться в другое измерение, просто подойдя к ней.</p>
     */
    private static void ceremonialArch(PalaceWriter w, int cx, int floorY, int cz) {
        int archZ = cz + PalaceGrid.ARCH_Z;

        // Лестница к арке: поднимается в сторону арки, то есть на −Z.
        PalaceComponents.placeGrandStairs(w, cx, floorY + 1,
                archZ + PalaceGrid.ARCH_STAIR_STEPS + 2,
                PalaceGrid.ARCH_WIDTH + 8, PalaceGrid.ARCH_STAIR_STEPS, Direction.NORTH);

        int podiumY = floorY + PalaceGrid.ARCH_STAIR_STEPS;

        // Сама арка с золотой окантовкой и вышивкой по краю.
        PalaceComponents.placeEmbroideredArch(w, cx, podiumY, archZ,
                PalaceGrid.ARCH_WIDTH, PalaceGrid.ARCH_HEIGHT, Direction.SOUTH, 1);

        // Заполнение проёма: бесконечность внутри лампы.
        int half = PalaceGrid.ARCH_WIDTH / 2;
        for (int i = -half + 1; i <= half - 1; i++) {
            double t = (double) Math.abs(i) / half;
            int top = (int) Math.round(PalaceGrid.ARCH_HEIGHT * Math.cos(t * Math.PI / 2.0D));
            for (int h = 0; h < top; h++) {
                w.set(cx + i, podiumY + h, archZ, PalacePalette.ARCH_VOID);
            }
        }

        // Колонны по сторонам арки: подчёркивают ось и держат взгляд.
        for (int side = -1; side <= 1; side += 2) {
            PalaceComponents.placeColumn(w, cx + side * (half + 3), podiumY, archZ + 2,
                    PalaceGrid.ARCH_HEIGHT - 4);
        }

        // Ковровая дорожка от лестницы к центру: ведёт взгляд к бассейну.
        int runnerFrom = archZ + PalaceGrid.ARCH_STAIR_STEPS + 3;
        int runnerTo = cz - PalaceGrid.POOL_HALF - 2;
        for (int z = runnerFrom; z <= runnerTo; z++) {
            for (int x = -2; x <= 2; x++) {
                w.set(cx + x, floorY + 1, z, Math.abs(x) == 2
                        ? PalacePalette.CARPET_CRIMSON
                        : PalacePalette.CARPET_RED);
            }
        }
    }

    /** Площадка появления игрока: балкон с видом на весь зал. */
    public static void spawnPlatform(PalaceWriter w, int cx, int floorY, int cz) {
        int sz = cz + PalaceGrid.SPAWN_Z;
        int sy = floorY + PalaceGrid.SPAWN_Y;

        // Площадка: игрок появляется выше пола и видит зал целиком.
        for (int x = -5; x <= 5; x++) {
            for (int z = -3; z <= 3; z++) {
                w.set(cx + x, sy - 1, sz + z, PalacePalette.WALL_STEP);
            }
        }
        // Перила по внешнему краю, чтобы не шагнуть в пустоту при появлении.
        for (int x = -5; x <= 5; x++) {
            w.set(cx + x, sy, sz - 3, PalacePalette.GOLD_LATTICE);
        }
        // Воздух над площадкой: спавн не должен оказаться внутри блока.
        for (int x = -4; x <= 4; x++) {
            for (int z = -2; z <= 2; z++) {
                for (int h = 0; h < 3; h++) {
                    w.set(cx + x, sy + h, sz + z, Blocks.AIR.defaultBlockState());
                }
            }
        }
        // Лестница с площадки на пол зала.
        PalaceComponents.placeGrandStairs(w, cx, floorY + 1, sz - 4,
                7, PalaceGrid.SPAWN_Y - 1, Direction.NORTH);
    }
}
