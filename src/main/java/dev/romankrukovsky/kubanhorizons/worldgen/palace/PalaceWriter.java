package dev.romankrukovsky.kubanhorizons.worldgen.palace;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelWriter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

/**
 * Единственная точка записи блоков дворца.
 *
 * <p>Существует по трём причинам, каждая из которых иначе решалась бы копипастой
 * в каждом генераторе:</p>
 *
 * <ul>
 *   <li>обрезка по границам чанка: {@code postProcess} вызывается для каждого
 *       чанка отдельно, поэтому запись вне текущего бокса обязана отбрасываться,
 *       иначе дворец продублируется по швам;</li>
 *   <li>обрезка по границам мира: запись выше потолка или ниже пола измерения
 *       молча теряется движком, и такую ошибку легко не заметить;</li>
 *   <li>подсчёт реальных block writes: единственный честный способ узнать объём
 *       дворца — посчитать записи, а не объём параллелепипеда.</li>
 * </ul>
 */
public final class PalaceWriter {
    private final LevelWriter level;
    private final BoundingBox chunkBounds;
    private final int worldMinY;
    private final int worldMaxY;

    private int writes;
    private int clippedByChunk;
    private int clippedByWorld;

    public PalaceWriter(LevelWriter level, BoundingBox chunkBounds, int worldMinY, int worldMaxY) {
        this.level = level;
        this.chunkBounds = chunkBounds;
        this.worldMinY = worldMinY;
        this.worldMaxY = worldMaxY;
    }

    /**
     * Ставит блок, если позиция попадает в текущий чанк и в границы мира.
     *
     * @return true, если запись действительно произошла
     */
    public boolean set(BlockPos pos, BlockState state) {
        if (pos.getY() < worldMinY || pos.getY() > worldMaxY) {
            clippedByWorld++;
            return false;
        }
        if (!chunkBounds.isInside(pos)) {
            clippedByChunk++;
            return false;
        }
        // Флаг 2 — без обновлений соседей: во время генерации они не нужны и
        // приводят к каскадам физики на сотнях тысяч блоков.
        level.setBlock(pos, state, Block.UPDATE_CLIENTS);
        writes++;
        return true;
    }

    /** Ставит блок по абсолютным координатам. */
    public boolean set(int x, int y, int z, BlockState state) {
        return set(new BlockPos(x, y, z), state);
    }

    /**
     * Заполняет прямоугольный объём.
     *
     * <p>Границы задаются в любом порядке: метод сам нормализует их, поэтому
     * вызывающий код не обязан помнить, какой угол меньше.</p>
     */
    public void fill(int x1, int y1, int z1, int x2, int y2, int z2, BlockState state) {
        int minX = Math.min(x1, x2);
        int maxX = Math.max(x1, x2);
        int minY = Math.min(y1, y2);
        int maxY = Math.max(y1, y2);
        int minZ = Math.min(z1, z2);
        int maxZ = Math.max(z1, z2);

        // Отсечение целого объёма до поблочного цикла: без этого генератор
        // перебирал бы весь дворец в каждом из его чанков.
        if (maxX < chunkBounds.minX() || minX > chunkBounds.maxX()
                || maxZ < chunkBounds.minZ() || minZ > chunkBounds.maxZ()) {
            return;
        }

        for (int y = minY; y <= maxY; y++) {
            for (int x = Math.max(minX, chunkBounds.minX()); x <= Math.min(maxX, chunkBounds.maxX()); x++) {
                for (int z = Math.max(minZ, chunkBounds.minZ()); z <= Math.min(maxZ, chunkBounds.maxZ()); z++) {
                    set(x, y, z, state);
                }
            }
        }
    }

    /** Число реально записанных блоков. */
    public int writes() {
        return writes;
    }

    /** Сколько записей отброшено как принадлежащие другому чанку. */
    public int clippedByChunk() {
        return clippedByChunk;
    }

    /**
     * Сколько записей отброшено как выходящие за границы мира.
     *
     * <p>Должно быть нулём: ненулевое значение означает, что композиция не
     * помещается в измерение, и это ловится тестом.</p>
     */
    public int clippedByWorld() {
        return clippedByWorld;
    }
}
