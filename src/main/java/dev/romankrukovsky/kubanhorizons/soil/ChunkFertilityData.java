package dev.romankrukovsky.kubanhorizons.soil;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.common.util.ValueIOSerializable;

/**
 * Данные плодородия одного чанка.
 *
 * <p>Хранится как data attachment на {@code LevelChunk}. Ключ —
 * упакованная позиция грядки ({@link BlockPos#asLong()}), значение —
 * запись {@link Entry}: плодородие 0..100, код последней культуры и
 * игровое время последнего обновления (для ленивого восстановления).</p>
 *
 * <p>Никаких tick-обходов: записи изменяются только событиями (посадка,
 * сбор, компост) и лениво пересчитываются при чтении. Пустая карта не
 * сериализуется вовсе (attachment снимается).</p>
 */
public final class ChunkFertilityData implements ValueIOSerializable {
    /** Версия схемы данных (AD-006). */
    public static final int SCHEMA_VERSION = 1;

    /** Отсутствие культуры в истории. */
    public static final byte NO_CROP = 0;

    /**
     * Запись плодородия одной грядки.
     *
     * @param fertility  плодородие 0..100
     * @param lastCrop   код последней собранной культуры (см. {@link CropKind})
     * @param lastUpdate игровое время последнего изменения (gameTime)
     */
    public record Entry(int fertility, byte lastCrop, long lastUpdate) {
    }

    private final Long2ObjectMap<Entry> entries = new Long2ObjectOpenHashMap<>();

    public Entry get(BlockPos pos) {
        return entries.get(pos.asLong());
    }

    public void put(BlockPos pos, Entry entry) {
        entries.put(pos.asLong(), entry);
    }

    public void remove(BlockPos pos) {
        entries.remove(pos.asLong());
    }

    public boolean isEmpty() {
        return entries.isEmpty();
    }

    public int size() {
        return entries.size();
    }

    @Override
    public void serialize(ValueOutput output) {
        output.putInt("SchemaVersion", SCHEMA_VERSION);
        var list = output.childrenList("entries");
        entries.forEach((packedPos, entry) -> {
            var child = list.addChild();
            child.putLong("pos", packedPos);
            child.putInt("fertility", entry.fertility());
            child.putByte("crop", entry.lastCrop());
            child.putLong("time", entry.lastUpdate());
        });
    }

    @Override
    public void deserialize(ValueInput input) {
        int schema = input.getIntOr("SchemaVersion", 1);
        if (schema > SCHEMA_VERSION) {
            dev.romankrukovsky.kubanhorizons.KubanHorizons.LOGGER.warn(
                    "Данные плодородия: схема v{} новее поддерживаемой v{}; читаем в режиме совместимости.",
                    schema, SCHEMA_VERSION);
        }
        entries.clear();
        for (ValueInput child : input.childrenListOrEmpty("entries")) {
            long pos = child.getLongOr("pos", 0L);
            int fertility = child.getIntOr("fertility", 0);
            byte crop = (byte) child.getIntOr("crop", NO_CROP);
            long time = child.getLongOr("time", 0L);
            entries.put(pos, new Entry(fertility, crop, time));
        }
    }
}
