package dev.romankrukovsky.kubanhorizons.soil;

import dev.romankrukovsky.kubanhorizons.config.KHServerConfig;
import dev.romankrukovsky.kubanhorizons.registry.KHAttachments;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.LevelChunk;

/**
 * Публичный API опыления.
 *
 * <p>Пчела помечает грядку; метка живёт {@link #MARK_LIFETIME_TICKS} и даёт
 * шанс дополнительного дропа при сборе. Влияние именно на выход, а не на
 * скорость роста — тем опыление и отличается от плодородия, и поэтому пасеку
 * имеет смысл ставить рядом с полем, а не «где-нибудь».</p>
 *
 * <p>Никаких tick-задач: метка проверяется по времени при сборе урожая.</p>
 */
public final class Pollination {
    /** Метка опыления живёт примерно игровой день. */
    public static final long MARK_LIFETIME_TICKS = 24000L;
    /** Шанс бонусного дропа с опылённой грядки. */
    public static final float BONUS_CHANCE = 0.34F;

    private Pollination() {
    }

    /** Помечает грядку опылённой на текущий игровой момент. */
    public static void mark(ServerLevel level, BlockPos farmlandPos) {
        if (!KHServerConfig.pollinationEnabled()) {
            return;
        }
        LevelChunk chunk = level.getChunkAt(farmlandPos);
        ChunkPollinationData data = chunk.getData(KHAttachments.CHUNK_POLLINATION.get());
        data.put(farmlandPos, level.getGameTime());
        chunk.markUnsaved();
    }

    /** Опылена ли грядка прямо сейчас (метка не истекла). */
    public static boolean isPollinated(ServerLevel level, BlockPos farmlandPos) {
        if (!KHServerConfig.pollinationEnabled()) {
            return false;
        }
        LevelChunk chunk = level.getChunkAt(farmlandPos);
        ChunkPollinationData data = chunk.getExistingDataOrNull(KHAttachments.CHUNK_POLLINATION.get());
        if (data == null) {
            return false;
        }
        long marked = data.get(farmlandPos);
        return marked > 0L && level.getGameTime() - marked <= MARK_LIFETIME_TICKS;
    }

    /**
     * Снимает метку после того, как она отработала на сборе: опыление —
     * разовый бонус за визит пчелы, а не постоянное свойство грядки.
     */
    public static void consume(ServerLevel level, BlockPos farmlandPos) {
        LevelChunk chunk = level.getChunkAt(farmlandPos);
        ChunkPollinationData data = chunk.getExistingDataOrNull(KHAttachments.CHUNK_POLLINATION.get());
        if (data == null) {
            return;
        }
        data.remove(farmlandPos);
        chunk.markUnsaved();
    }
}
