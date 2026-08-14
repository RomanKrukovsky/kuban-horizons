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
    /**
     * Порог «богатой» почвы: с него культуры, умеющие благодарить за землю,
     * дают прибавку к урожаю (TECH_SPEC.md §3 — «при 80+ шанс бонусного
     * дропа»).
     *
     * <p>80 выбрано выше базы чернозёма минус один сбор: свежий чернозём (85)
     * порог проходит, но уже первый повторный сбор той же культуры сбивает
     * его ниже. Богатый урожай — состояние ухоженной земли, а не постоянное
     * свойство блока.</p>
     */
    public static final int RICH_THRESHOLD = 80;
    /** Штраф за повторный сбор той же культуры. */
    private static final int DEPLETION_SAME_CROP = 12;
    /** Штраф за сбор с ротацией культур (меньше). */
    private static final int DEPLETION_ROTATED = 4;
    /** Прибавка от компостирования (костной муки на почву и т.п.). */
    private static final int COMPOST_BONUS = 15;
    /** Потеря плодородия от вытаптывания грядки животным. */
    private static final int TRAMPLE_LOSS = 25;
    /** Прибавка от речного ила после схода половодья. */
    private static final int FLOOD_SILT_BONUS = 20;
    /** Восстановление: +1 плодородия за столько тиков простоя. */
    private static final long RECOVERY_TICKS_PER_POINT = 1200; // 1 минута

    private SoilFertility() {
    }

    /**
     * Базовое плодородие грядки по её ярусу почвы.
     *
     * <p>Единая точка: все операции ниже спрашивают базу здесь, а не берут
     * константу обычной грядки. Иначе новый ярус читался бы в одном месте и
     * молча игнорировался в остальных — плодородие «возвращалось» бы к 40
     * при первом же сборе на чернозёме.</p>
     */
    public static int baseFertility(ServerLevel level, BlockPos farmlandPos) {
        return SoilTier.at(level, farmlandPos).baseFertility();
    }

    /** Текущее плодородие грядки (с учётом ленивого восстановления). */
    public static int fertility(ServerLevel level, BlockPos farmlandPos) {
        int base = baseFertility(level, farmlandPos);
        if (!KHServerConfig.fertilityEnabled()) {
            return base;
        }
        LevelChunk chunk = level.getChunkAt(farmlandPos);
        ChunkFertilityData data = chunk.getExistingDataOrNull(KHAttachments.CHUNK_FERTILITY.get());
        if (data == null) {
            return base;
        }
        ChunkFertilityData.Entry entry = data.get(farmlandPos);
        if (entry == null) {
            return base;
        }
        return recoveredFertility(entry, level.getGameTime(), base);
    }

    /**
     * Множитель скорости роста по плодородию: 0.6 при нуле, 1.0 при базе
     * обычной грядки, 1.6 при максимуме.
     *
     * <p>Точка перелома осталась на {@link #BASE_FARMLAND}, а не на базе
     * яруса: иначе чернозём с его 85 давал бы ровно 1.0, и весь ярус не
     * значил бы ничего. Шкала одна на все ярусы, ярус лишь определяет, где
     * на ней грядка стоит по умолчанию.</p>
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
     * Богата ли земля настолько, чтобы дать прибавку к урожаю.
     *
     * <p>Свойство почвы, а не культуры: культуры лишь решают, пользуются они
     * этим или нет. Так «награда за хорошую землю» остаётся общим правилом
     * мира и честно исчезает вместе с истощением.</p>
     */
    public static boolean isRichHarvest(ServerLevel level, BlockPos farmlandPos) {
        return fertility(level, farmlandPos) >= RICH_THRESHOLD;
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

        int base = baseFertility(level, farmlandPos);
        ChunkFertilityData.Entry old = data.get(farmlandPos);
        int current = old != null ? recoveredFertility(old, now, base) : base;
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
        adjust(level, farmlandPos,
                (int) Math.round(COMPOST_BONUS * KHServerConfig.fertilityRecoveryRate()));
    }

    /**
     * Вытаптывание грядки животным: резкая потеря плодородия без смены
     * культуры-истории. Сила масштабируется конфигом давления.
     *
     * @return плодородие после вытаптывания
     */
    public static int onTrample(ServerLevel level, BlockPos farmlandPos) {
        if (!KHServerConfig.fertilityEnabled()) {
            return baseFertility(level, farmlandPos);
        }
        int loss = (int) Math.round(TRAMPLE_LOSS * KHServerConfig.pressureSeverity());
        return adjust(level, farmlandPos, -loss);
    }

    /**
     * Обогащение почвы после схода половодья: заливной луг становится
     * плодороднее, чем был. Прибавка не зависит от давности события.
     *
     * @return плодородие после обогащения
     */
    public static int onFloodDeposit(ServerLevel level, BlockPos farmlandPos) {
        if (!KHServerConfig.fertilityEnabled()) {
            return baseFertility(level, farmlandPos);
        }
        int bonus = (int) Math.round(FLOOD_SILT_BONUS * KHServerConfig.fertilityRecoveryRate());
        return adjust(level, farmlandPos, bonus);
    }

    /**
     * Общий путь изменения плодородия на дельту с сохранением истории культуры.
     * Все внешние операции проходят здесь, поэтому ленивое восстановление и
     * запись в чанк реализованы один раз.
     */
    private static int adjust(ServerLevel level, BlockPos farmlandPos, int delta) {
        LevelChunk chunk = level.getChunkAt(farmlandPos);
        ChunkFertilityData data = chunk.getData(KHAttachments.CHUNK_FERTILITY.get());
        long now = level.getGameTime();

        int base = baseFertility(level, farmlandPos);
        ChunkFertilityData.Entry old = data.get(farmlandPos);
        int current = old != null ? recoveredFertility(old, now, base) : base;
        int updated = Mth.clamp(current + delta, 0, MAX);
        byte crop = old != null ? old.lastCrop() : ChunkFertilityData.NO_CROP;

        data.put(farmlandPos, new ChunkFertilityData.Entry(updated, crop, now));
        chunk.markUnsaved();
        return updated;
    }

    /**
     * Ленивое восстановление: +1 за каждые RECOVERY_TICKS_PER_POINT тиков
     * простоя, но <b>не выше базы своего яруса</b>.
     *
     * <p>Потолок по ярусу — то, что вообще делает ярусы осмысленными. Если бы
     * пар восстанавливал до максимума шкалы, любая грядка мира приходила бы к
     * 100 сама собой, и чернозём означал бы лишь «дойти до сотни быстрее».
     * Поэтому земля сама возвращается только к своей норме (40 / 60 / 85), а
     * всё, что выше нормы, остаётся результатом вложений — компоста и
     * речного ила. Разница между ярусами сохраняется навсегда.</p>
     *
     * <p>При этом уже вложенное не отбирается: если значение выше базы, оно
     * берётся как есть, а не срезается до нормы. Компост на обычной грядке не
     * должен исчезать только потому, что грядка «обычная».</p>
     */
    private static int recoveredFertility(ChunkFertilityData.Entry entry, long now, int base) {
        long elapsed = Math.max(0, now - entry.lastUpdate());
        double rate = KHServerConfig.fertilityRecoveryRate();
        if (rate <= 0) {
            return entry.fertility();
        }
        int recovered = (int) (elapsed / (long) (RECOVERY_TICKS_PER_POINT / rate));
        // Потолок восстановления — максимум из нормы яруса и уже достигнутого
        // значения: пар подтягивает к норме, но не сбрасывает вложенное.
        int ceiling = Math.max(base, entry.fertility());
        return Mth.clamp(entry.fertility() + recovered, 0, Math.min(MAX, ceiling));
    }
}
