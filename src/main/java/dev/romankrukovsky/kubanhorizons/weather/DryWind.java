package dev.romankrukovsky.kubanhorizons.weather;

import dev.romankrukovsky.kubanhorizons.config.KHServerConfig;
import dev.romankrukovsky.kubanhorizons.registry.KHSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FarmlandBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Суховей: сухой степной ветер, снимающий влажность с открытых грядок.
 *
 * <p>Смысл механики — сделать орошение срочным, а не бонусным. Пока ветра нет,
 * систему желобов можно игнорировать; с суховеем неполитое поле сохнет само, и
 * водозабор превращается из оптимизации в страховку.</p>
 *
 * <p>Сушит только грядки под открытым небом и вне действия оросительной сети:
 * желоб отдаёт {@link net.minecraft.world.level.material.FluidState} воды,
 * поэтому политая земля переживает ветер — именно в этом награда за сеть.</p>
 */
public final class DryWind {
    /** Радиус вокруг игрока, в котором ветер сушит грядки. */
    private static final int RADIUS = 24;
    /** Сколько грядок за одну волну проверяется максимум (бюджет). */
    private static final int BLOCK_BUDGET = 96;

    private DryWind() {
    }

    /**
     * Одна волна суховея вокруг игрока.
     *
     * @return число иссушённых грядок
     */
    public static int blow(ServerLevel level, ServerPlayer player) {
        return blow(level, player.blockPosition());
    }

    /**
     * Одна волна суховея вокруг произвольной точки.
     *
     * <p>Отдельная перегрузка по координате, а не по игроку: ветру нужен только
     * центр волны, и привязка к {@link ServerPlayer} делала механику
     * непроверяемой — в тестовом окружении настоящего серверного игрока нет.</p>
     *
     * @return число иссушённых грядок
     */
    public static int blow(ServerLevel level, BlockPos origin) {
        if (!KHServerConfig.dryWindEnabled() || KHServerConfig.pressureSeverity() <= 0.0D) {
            return 0;
        }
        if (level.isRaining()) {
            return 0;
        }
        int dried = 0;
        int inspected = 0;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

        for (int dx = -RADIUS; dx <= RADIUS && inspected < BLOCK_BUDGET; dx++) {
            for (int dz = -RADIUS; dz <= RADIUS && inspected < BLOCK_BUDGET; dz++) {
                for (int dy = -3; dy <= 3; dy++) {
                    cursor.set(origin.getX() + dx, origin.getY() + dy, origin.getZ() + dz);
                    BlockState state = level.getBlockState(cursor);
                    if (!(state.getBlock() instanceof FarmlandBlock)) {
                        continue;
                    }
                    inspected++;
                    if (dryOne(level, cursor.immutable(), state)) {
                        dried++;
                    }
                    break;
                }
            }
        }
        if (dried > 0) {
            level.playSound(null, origin, KHSounds.DRY_WIND.get(), SoundSource.WEATHER, 0.7F, 1.0F);
        }
        return dried;
    }

    /**
     * Сушит одну грядку на шаг влажности.
     *
     * @return true, если влажность действительно снизилась
     */
    private static boolean dryOne(ServerLevel level, BlockPos pos, BlockState state) {
        // Под крышей ветра нет.
        if (!level.canSeeSky(pos)) {
            return false;
        }
        // Политая сетью или дождём земля переживает суховей — это награда за сеть.
        if (level.getBlockState(pos.above()).getFluidState().isSourceOfType(
                net.minecraft.world.level.material.Fluids.WATER)) {
            return false;
        }
        int moisture = state.getValue(FarmlandBlock.MOISTURE);
        if (moisture <= 0) {
            // Полностью сухая грядка со временем осыпается в землю.
            if (level.getRandom().nextFloat() < 0.15F * KHServerConfig.pressureSeverity()
                    && level.getBlockState(pos.above()).isAir()) {
                level.setBlockAndUpdate(pos, Blocks.DIRT.defaultBlockState());
                return true;
            }
            return false;
        }
        level.setBlock(pos, state.setValue(FarmlandBlock.MOISTURE, moisture - 1), 2);
        return true;
    }
}
