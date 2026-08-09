package dev.romankrukovsky.kubanhorizons.worldgen.palace;

/**
 * Зафиксированная координатная сетка дворца.
 *
 * <p>Все размеры — константы, а не диапазоны: зал проектируется как сцена по
 * сетке, поэтому положение бассейна, арки, ярусов и точки входа обязано быть
 * воспроизводимым и проверяемым тестом.</p>
 *
 * <p>Координаты заданы относительно центра зала. Мировая привязка добавляется
 * генератором один раз, поэтому дворец можно поставить в любую точку измерения
 * не переписывая композицию.</p>
 */
public final class PalaceGrid {
    private PalaceGrid() {
    }

    // --- Габариты зала ---------------------------------------------------

    /** Пролёт зала по X. */
    public static final int HALL_SPAN_X = 112;

    /** Пролёт зала по Z. */
    public static final int HALL_SPAN_Z = 96;

    /** Полная высота зала от пола до вершины купола. */
    public static final int HALL_HEIGHT = 56;

    /** Половина пролёта по X — рабочий радиус овала. */
    public static final int HALL_RADIUS_X = HALL_SPAN_X / 2;

    /** Половина пролёта по Z. */
    public static final int HALL_RADIUS_Z = HALL_SPAN_Z / 2;

    // --- Вертикальные уровни (от пола зала) ------------------------------

    /** Пол зала. */
    public static final int FLOOR_Y = 0;

    /** Второй ярус: балконы и обзорные площадки. */
    public static final int SECOND_TIER_Y = 13;

    /** Третий ярус: декоративные ниши, окна в пустоту, висящие фонари. */
    public static final int THIRD_TIER_Y = 21;

    /** Начало купола: выше стены переходят в свод. */
    public static final int DOME_START_Y = 27;

    /** Вершина купола. */
    public static final int DOME_TOP_Y = HALL_HEIGHT - 1;

    // --- Бассейн ---------------------------------------------------------

    /** Сторона бассейна. */
    public static final int POOL_SIZE = 15;

    /** Половина стороны бассейна. */
    public static final int POOL_HALF = POOL_SIZE / 2;

    /** Глубина воды бассейна. */
    public static final int POOL_DEPTH = 3;

    /** Высота спирального потока магии над бассейном. */
    public static final int POOL_SPIRAL_HEIGHT = 22;

    // --- Церемониальная арка ---------------------------------------------

    /** Ось арки по Z: дальний конец зала. */
    public static final int ARCH_Z = -40;

    /** Высота арки. */
    public static final int ARCH_HEIGHT = 23;

    /** Ширина проёма арки. */
    public static final int ARCH_WIDTH = 13;

    /** Число ступеней лестницы к арке. */
    public static final int ARCH_STAIR_STEPS = 5;

    // --- Точка входа -----------------------------------------------------

    /** Ось точки появления по Z: противоположный конец от арки. */
    public static final int SPAWN_Z = 38;

    /**
     * Высота площадки появления.
     *
     * <p>Игрок появляется выше пола, поэтому при первом входе видит зал
     * целиком: бассейн со спиралью, арку в глубине, фонари сверху.</p>
     */
    public static final int SPAWN_Y = SECOND_TIER_Y + 3;

    // --- Ниши и балконы --------------------------------------------------

    /** Число балконов второго яруса по каждой длинной стороне. */
    public static final int BALCONIES_PER_SIDE = 4;

    /** Число ниш первого яруса по каждой длинной стороне. */
    public static final int ALCOVES_PER_SIDE = 5;

    /** Толщина стены оболочки. */
    public static final int WALL_THICKNESS = 2;

    /**
     * Проверяет, лежит ли точка внутри овала зала на данном уровне.
     *
     * <p>Овал сужается к куполу, поэтому один и тот же метод описывает и
     * стены, и свод: выше {@link #DOME_START_Y} радиус уменьшается по
     * окружности, давая ступенчатый купол без отдельной формулы.</p>
     *
     * @param x смещение от центра по X
     * @param z смещение от центра по Z
     * @param y высота над полом
     * @return true, если точка внутри внутреннего объёма зала
     */
    public static boolean insideHall(int x, int z, int y) {
        double radiusX = HALL_RADIUS_X;
        double radiusZ = HALL_RADIUS_Z;

        if (y >= DOME_START_Y) {
            // Купол: радиус идёт по дуге окружности от полного пролёта к нулю.
            double t = (double) (y - DOME_START_Y) / (DOME_TOP_Y - DOME_START_Y);
            double shrink = Math.cos(t * Math.PI / 2.0D);
            radiusX *= shrink;
            radiusZ *= shrink;
        }

        if (radiusX < 1.0D || radiusZ < 1.0D) {
            return false;
        }

        double nx = x / radiusX;
        double nz = z / radiusZ;
        return nx * nx + nz * nz <= 1.0D;
    }

    /** Радиус овала по X на данной высоте, в блоках. */
    public static int hallRadiusXAt(int y) {
        for (int x = HALL_RADIUS_X; x >= 0; x--) {
            if (insideHall(x, 0, y)) {
                return x;
            }
        }
        return 0;
    }

    /** Радиус овала по Z на данной высоте, в блоках. */
    public static int hallRadiusZAt(int y) {
        for (int z = HALL_RADIUS_Z; z >= 0; z--) {
            if (insideHall(0, z, y)) {
                return z;
            }
        }
        return 0;
    }
}
