package dev.romankrukovsky.kubanhorizons.genie.spatial;

import dev.romankrukovsky.kubanhorizons.genie.aura.MagicalSignature;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;

/**
 * Материализация намерения «мост» — GENIE_VISION §Физика.
 *
 * <p>Джинния поднимает мост из досок в направлении взгляда игрока: идёт
 * вперёд, пока впереди не окажется твёрдая опора (берег/скала), и заполняет
 * пустоту над водой или пропастью. Максимум 16 блоков, чтобы мост не стал
 * бесконечным коридором.</p>
 */
public final class BridgeMaterializerEngine {

    private static final int MAX_BRIDGE = 16;

    private BridgeMaterializerEngine() {
    }

    /**
     * Строит мост в направлении {@code facing} от {@code start}.
     *
     * @return число построенных блоков, 0 если мост не нужен/нельзя
     */
    public static int buildBridge(ServerLevel level, BlockPos start, Direction facing) {
        int built = 0;
        BlockPos cursor = start.relative(facing);
        int groundY = start.getY();

        for (int step = 0; step < MAX_BRIDGE; step++) {
            BlockPos candidate = cursor.below(groundY - cursor.getY() == 0 ? 0 : 0)
                    .offset(0, 0, 0);
            // Идём по горизонтали, уровень моста — как у старта.
            BlockPos deck = new BlockPos(cursor.getX(), groundY, cursor.getZ());
            BlockPos below = deck.below();

            boolean overVoid = level.isEmptyBlock(deck)
                    && !isSolidSupport(level, below);
            if (!overVoid) {
                // Дошли до суши или уже есть пол — мост готов.
                break;
            }
            level.setBlock(deck, Blocks.OAK_PLANKS.defaultBlockState(), 3);
            built++;
            cursor = cursor.relative(facing);
        }
        if (built > 0) {
            MagicalSignature.cast(level,
                    net.minecraft.world.phys.Vec3.atBottomCenterOf(start));
        }
        return built;
    }

    private static boolean isSolidSupport(ServerLevel level, BlockPos below) {
        return level.getBlockState(below).isSolid();
    }
}