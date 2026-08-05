package dev.romankrukovsky.kubanhorizons.soil;

import dev.romankrukovsky.kubanhorizons.config.KHServerConfig;
import dev.romankrukovsky.kubanhorizons.registry.KHAttachments;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.chunk.LevelChunk;

/**
 * Публичный API системы плодородия.
 *
 * <p>Все операции — O(1) по карте чанка; ленивое восстановление плодородия
 * рассчитывается по прошедшему игровому времени при чтении, без tick-задач.</p>
 *
 * <p>Шкала: 0..100. Базовые значения выводятся из блока почвы; текущее
 * значение хранится только если отличается от базового поведения
 * (после сборов/компоста).</p>
 */
public final class SoilFertility {
    /** Базовое плодородие обычной farmland. */
    public static final int BASE_FARMLAND = 40;
    /** Максимум шкалы. */
    public static final int MAX = 100;
    /** Штраф за повторный сбор той же культуры. */
    private static final int DEPLETION_SAME_CROP = 12;
    /** Штраф за сбор с ротацией культур (меньше). */
    private static final int DEPLETION_ROTATED = 4;
    /** Прибавка от компостирования (костной муки на почву и т.п.). */
    private static final int COMPOST_BONUS = 15;
    /** Восстановление: +1 плодородия за столько тиков простоя. */
    private static final long RECOVERY_TICKS_PER_POINT = 1200; // 1 минута

    private SoilFertility() {
    }

    /** Текущее плодородие грядки (с учётом ленивого восстановления). */
    public static int fertility(ServerLevel level, BlockPos farmlandPos) {
        if (!KHServerConfig.fertilityEnabled()) {
            return BASE_FARMLAND;
        }
        LevelChunk chunk = level.getChunkAt(farmlandPos);
        ChunkFertilityData data = chunk.getExistingDataOrNull(KHAttachments.CHUNK_FERTILITY.get());
        if (data == null) {
            return BASE_FARMLAND;
        }
        ChunkFertilityData.Entry entry = data.get(farmlandPos);
        if (entry == null) {
            return BASE_FARMLAND;
        }
        return recoveredFertility(entry, level.getGameTime());
    }

    /**
     * Множитель скорости роста по плодородию: 0.6 при нуле, 1.0 при базе,
     * 1.6 при максимуме.
     */
    public static float growthMultiplier(ServerLevel level, BlockPos farmlandPos) {
        if (!KHServerConfig.fertilityEnabled()) {
            return 1.0F;
        }
        int fertility = fertility(level, farmlandPos);
        if (fertility <= BASE_FARMLAND) {
            return Mth.lerp(fertility / (float) BASE_FARMLAND, 0.6F, 1.0F);
        }
        return Mth.lerp((fertility - BASE_FARMLAND) / (float) (MAX - BASE_FARMLAND), 1.0F, 1.6F);
    }

    /**
     * Регистрирует сбор урожая: истощает грядку с учётом севооборота.
     *
     * @param cropBlock блок собранной культуры
     */
    public static void onHarvest(ServerLevel level, BlockPos farmlandPos, Block cropBlock) {
        if (!KHServerConfig.fertilityEnabled()) {
            return;
        }
        LevelChunk chunk = level.getChunkAt(farmlandPos);
        ChunkFertilityData data = chunk.getData(KHAttachments.CHUNK_FERTILITY.get());
        long now = level.getGameTime();

        ChunkFertilityData.Entry old = data.get(farmlandPos);
        int current = old != null ? recoveredFertility(old, now) : BASE_FARMLAND;
        byte crop = CropKind.ofBlock(cropBlock).code();
        boolean sameCrop = old != null && old.lastCrop() == crop && crop != ChunkFertilityData.NO_CROP;

        int depletion = (int) Math.round(
                (sameCrop ? DEPLETION_SAME_CROP : DEPLETION_ROTATED)
                        * KHServerConfig.fertilityDepletionRate());
        int updated = Mth.clamp(current - depletion, 0, MAX);

        data.put(farmlandPos, new ChunkFertilityData.Entry(updated, crop, now));
        chunk.markUnsaved();
    }

    /** Компостирование грядки: восстанавливает плодородие. */
    public static void onCompost(ServerLevel level, BlockPos farmlandPos) {
        if (!KHServerConfig.fertilityEnabled()) {
            return;
        }
        LevelChunk chunk = level.getChunkAt(farmlandPos);
        ChunkFertilityData data = chunk.getData(KHAttachments.CHUNK_FERTILITY.get());
        long now = level.getGameTime();

        ChunkFertilityData.Entry old = data.get(farmlandPos);
        int current = old != null ? recoveredFertility(old, now) : BASE_FARMLAND;
        int bonus = (int) Math.round(COMPOST_BONUS * KHServerConfig.fertilityRecoveryRate());
        int updated = Mth.clamp(current + bonus, 0, MAX);
        byte crop = old != null ? old.lastCrop() : ChunkFertilityData.NO_CROP;

        data.put(farmlandPos, new ChunkFertilityData.Entry(updated, crop, now));
        chunk.markUnsaved();
    }

    /** Ленивое восстановление: +1 за каждые RECOVERY_TICKS_PER_POINT тиков. */
    private static int recoveredFertility(ChunkFertilityData.Entry entry, long now) {
        long elapsed = Math.max(0, now - entry.lastUpdate());
        double rate = KHServerConfig.fertilityRecoveryRate();
        if (rate <= 0) {
            return entry.fertility();
        }
        int recovered = (int) (elapsed / (long) (RECOVERY_TICKS_PER_POINT / rate));
        return Mth.clamp(entry.fertility() + recovered, 0, MAX);
    }
}
