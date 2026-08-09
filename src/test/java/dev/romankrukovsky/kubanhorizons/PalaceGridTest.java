package dev.romankrukovsky.kubanhorizons;

import dev.romankrukovsky.kubanhorizons.worldgen.palace.PalaceGrid;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Тесты координатной сетки дворца.
 *
 * <p>Сетка — чистая геометрия без зависимостей от Minecraft, поэтому она
 * проверяется обычным unit-тестом, а не GameTest'ом на сервере.</p>
 */
class PalaceGridTest {

    @Test
    void hallDimensionsAreFixedNotRanges() {
        // Бриф требует твёрдых чисел вместо диапазона 90-120: композиция
        // проектируется по сетке, поэтому любое расхождение — ошибка.
        assertEquals(112, PalaceGrid.HALL_SPAN_X);
        assertEquals(96, PalaceGrid.HALL_SPAN_Z);
        assertEquals(56, PalaceGrid.HALL_HEIGHT);
    }

    @Test
    void landmarkCoordinatesMatchTheBrief() {
        assertEquals(15, PalaceGrid.POOL_SIZE, "бассейн 15x15");
        assertEquals(-40, PalaceGrid.ARCH_Z, "арка около z=-40");
        assertEquals(38, PalaceGrid.SPAWN_Z, "вход около z=+38");
        assertEquals(13, PalaceGrid.SECOND_TIER_Y, "второй ярус y=+13");
        assertEquals(27, PalaceGrid.DOME_START_Y, "начало купола y=+27");
    }

    @Test
    void centreIsInsideAndBeyondSpanIsOutside() {
        assertTrue(PalaceGrid.insideHall(0, 0, PalaceGrid.FLOOR_Y));
        assertFalse(PalaceGrid.insideHall(PalaceGrid.HALL_RADIUS_X + 1, 0, PalaceGrid.FLOOR_Y));
        assertFalse(PalaceGrid.insideHall(0, PalaceGrid.HALL_RADIUS_Z + 1, PalaceGrid.FLOOR_Y));
    }

    @Test
    void domeNarrowsWithHeight() {
        // Купол обязан сужаться: иначе стены остаются цилиндром, что бриф
        // запрещает прямо.
        int atStart = PalaceGrid.hallRadiusXAt(PalaceGrid.DOME_START_Y);
        int midway = PalaceGrid.hallRadiusXAt(
                (PalaceGrid.DOME_START_Y + PalaceGrid.DOME_TOP_Y) / 2);
        int atTop = PalaceGrid.hallRadiusXAt(PalaceGrid.DOME_TOP_Y);

        assertTrue(midway < atStart, "середина купола уже основания");
        assertTrue(atTop < midway, "вершина уже середины");
    }

    @Test
    void wallsDoNotNarrowBelowTheDome() {
        // Ниже купола радиус постоянен: сужение начинается ровно на DOME_START_Y.
        int atFloor = PalaceGrid.hallRadiusXAt(PalaceGrid.FLOOR_Y);
        int belowDome = PalaceGrid.hallRadiusXAt(PalaceGrid.DOME_START_Y - 1);
        assertEquals(atFloor, belowDome);
    }

    @Test
    void archAndSpawnSitOnOppositeSidesOfThePool() {
        // Игрок при появлении смотрит через бассейн на арку: если знаки совпадут,
        // композиция первого кадра развалится.
        assertTrue(PalaceGrid.ARCH_Z < 0, "арка в дальнем конце");
        assertTrue(PalaceGrid.SPAWN_Z > 0, "вход в противоположном конце");
    }

    @Test
    void landmarksStayInsideTheHall() {
        // Арка и точка входа обязаны лежать внутри овала, иначе они окажутся
        // в стене или снаружи зала.
        assertTrue(PalaceGrid.insideHall(0, PalaceGrid.ARCH_Z, PalaceGrid.FLOOR_Y),
                "арка внутри зала");
        assertTrue(PalaceGrid.insideHall(0, PalaceGrid.SPAWN_Z, PalaceGrid.SECOND_TIER_Y),
                "площадка входа внутри зала");
        assertTrue(PalaceGrid.insideHall(PalaceGrid.POOL_HALF, PalaceGrid.POOL_HALF,
                PalaceGrid.FLOOR_Y), "бассейн внутри зала");
    }

    @Test
    void spiralStaysBelowTheDome() {
        // Спираль поднимается на 22 блока от пола и не должна пробивать свод.
        assertTrue(PalaceGrid.POOL_SPIRAL_HEIGHT < PalaceGrid.DOME_START_Y + 4,
                "спираль не протыкает купол");
    }

    @Test
    void gridIsDeterministic() {
        // Одна и та же точка обязана давать один ответ при повторных вызовах:
        // в сетке не должно появиться ни случайности, ни состояния.
        for (int i = 0; i < 100; i++) {
            assertEquals(PalaceGrid.insideHall(20, 20, 10),
                    PalaceGrid.insideHall(20, 20, 10));
            assertEquals(PalaceGrid.hallRadiusXAt(30), PalaceGrid.hallRadiusXAt(30));
        }
    }
}
