package dev.romankrukovsky.kubanhorizons.weather;

import dev.romankrukovsky.kubanhorizons.config.KHServerConfig;
import dev.romankrukovsky.kubanhorizons.soil.SoilFertility;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FarmlandBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Половодье поймы: река поднимается и заливает низкие грядки.
 *
 * <p>Событие сознательно двойственное, а не штрафное. Залитая грядка теряет
 * посев, но получает речной ил ({@link SoilFertility#onFloodDeposit}), и после
 * схода воды земля плодороднее, чем была. Поэтому пойма — это ставка: сеешь
 * низко и рискуешь, сеешь выше и получаешь обычную землю.</p>
 *
 * <p>Работает только в биоме поймы: событие принадлежит географии, а не погоде
 * вообще, и в степи его быть не должно.</p>
 */
public final class Flooding {
    /** Радиус вокруг игрока, в котором ищутся низкие грядки. */
    private static final int RADIUS = 20;
    /** Насколько ниже уровня воды грядка считается затопляемой. */
    private static final int FLOOD_DEPTH = 1;
    /** Бюджет проверяемых грядок за волну. */
    private static final int BLOCK_BUDGET = 96;

    private Flooding() {
    }

    /**
     * Одна волна половодья вокруг игрока.
     *
     * @return число обогащённых илом грядок
     */
    public static int rise(ServerLevel level, ServerPlayer player) {
        if (!KHServerConfig.floodingEnabled() || KHServerConfig.pressureSeverity() <= 0.0D) {
            return 0;
        }
        BlockPos origin = player.blockPosition();
        if (!level.getBiome(origin).is(
                dev.romankrukovsky.kubanhorizons.util.KHIds.of("river_floodplain"))) {
            return 0;
        }
        int flooded = 0;
        int inspected = 0;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

        for (int dx = -RADIUS; dx <= RADIUS && inspected < BLOCK_BUDGET; dx++) {
            for (int dz = -RADIUS; dz <= RADIUS && inspected < BLOCK_BUDGET; dz++) {
                for (int dy = -3; dy <= 1; dy++) {
                    cursor.set(origin.getX() + dx, origin.getY() + dy, origin.getZ() + dz);
                    BlockState state = level.getBlockState(cursor);
                    if (!(state.getBlock() instanceof FarmlandBlock)) {
                        continue;
                    }
                    inspected++;
                    if (floodOne(level, cursor.immutable())) {
                        flooded++;
                    }
                    break;
                }
            }
        }
        return flooded;
    }

    /**
     * Заливает одну грядку: смывает посев, оставляет ил.
     *
     * @return true, если грядка была затоплена
     */
    private static boolean floodOne(ServerLevel level, BlockPos pos) {
        // Затопляется только то, что лежит ниже соседней воды.
        if (!hasNearbyWaterAbove(level, pos)) {
            return false;
        }
        BlockPos above = pos.above();
        BlockState crop = level.getBlockState(above);
        if (crop.is(net.minecraft.tags.BlockTags.CROPS)) {
            // Посев смыт — без дропа: это стихия, а не сбор урожая.
            level.destroyBlock(above, false);
        }
        // Ил: земля после схода воды плодороднее, чем была.
        SoilFertility.onFloodDeposit(level, pos);
        level.setBlock(pos, level.getBlockState(pos).setValue(FarmlandBlock.MOISTURE, 7), 2);
        return true;
    }

    /** Есть ли рядом вода на уровне выше грядки. */
    private static boolean hasNearbyWaterAbove(ServerLevel level, BlockPos pos) {
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                for (int dy = 0; dy <= FLOOD_DEPTH; dy++) {
                    cursor.set(pos.getX() + dx, pos.getY() + dy, pos.getZ() + dz);
                    if (level.getBlockState(cursor).is(Blocks.WATER)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
