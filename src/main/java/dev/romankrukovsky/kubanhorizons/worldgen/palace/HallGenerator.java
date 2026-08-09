package dev.romankrukovsky.kubanhorizons.worldgen.palace;

/**
 * Корень генерации дворца.
 *
 * <p>Порядок вызовов повторяет порядок читаемости из брифа: сначала крупные
 * формы, затем архитектура ярусов, затем центр композиции, затем декор. Обратный
 * порядок затирал бы мелкие элементы оболочкой.</p>
 */
public final class HallGenerator {
    private HallGenerator() {
    }

    /**
     * Строит зал целиком в пределах текущего чанка.
     *
     * <p>Метод вызывается по одному разу на каждый чанк, пересекающийся с
     * дворцом, и каждый раз проходит всю композицию: {@link PalaceWriter}
     * отбрасывает записи вне чанка, поэтому результат склеивается без швов и не
     * зависит от порядка загрузки чанков.</p>
     *
     * @param w      writer, привязанный к текущему чанку
     * @param cx     мировая X центра зала
     * @param floorY мировая Y пола зала
     * @param cz     мировая Z центра зала
     */
    public static void generate(PalaceWriter w, int cx, int floorY, int cz) {
        // 1. Крупные формы: пол, ступенчатые стены, купол.
        ShellGenerator.generate(w, cx, floorY, cz);

        // 2. Архитектура ярусов: ниши, балконы, окна, фонари, колонны.
        ArchitectureGenerator.generate(w, cx, floorY, cz);

        // 3. Композиционный центр: бассейн со спиралью и церемониальная арка.
        CenterpieceGenerator.generate(w, cx, floorY, cz);

        // 4. Средний и мелкий декор по anchors.
        DecorationComposer.generate(w, cx, floorY, cz);

        // 5. Площадка появления: ставится последней, чтобы гарантированно
        // расчистить воздух над точкой спавна после всего декора.
        CenterpieceGenerator.spawnPlatform(w, cx, floorY, cz);
    }
}
