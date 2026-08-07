package dev.romankrukovsky.kubanhorizons.soil;

import it.unimi.dsi.fastutil.longs.Long2LongMap;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.common.util.ValueIOSerializable;

/**
 * Метки опыления грядок одного чанка: позиция → игровое время опыления.
 *
 * <p>Отдельный attachment, а не поле в {@link ChunkFertilityData}: плодородие и
 * опыление живут по разным правилам — первое накапливается и восстанавливается,
 * второе истекает по времени. Сложи их в одну запись, и «последнее обновление»
 * начнёт означать две несовместимые вещи.</p>
 *
 * <p>Меток нет — attachment не сериализуется, поэтому пасека рядом с полем
 * ничего не стоит миру, если опыление отключено конфигом.</p>
 */
public final class ChunkPollinationData implements ValueIOSerializable {
    /** Версия схемы данных (AD-006). */
    public static final int SCHEMA_VERSION = 1;

    private final Long2LongMap marks = new Long2LongOpenHashMap();

    /** Игровое время последнего опыления позиции, или 0. */
    public long get(BlockPos pos) {
        return marks.get(pos.asLong());
    }

    public void put(BlockPos pos, long gameTime) {
        marks.put(pos.asLong(), gameTime);
    }

    public void remove(BlockPos pos) {
        marks.remove(pos.asLong());
    }

    public boolean isEmpty() {
        return marks.isEmpty();
    }

    public int size() {
        return marks.size();
    }

    @Override
    public void serialize(ValueOutput output) {
        output.putInt("SchemaVersion", SCHEMA_VERSION);
        var list = output.childrenList("marks");
        marks.forEach((packedPos, time) -> {
            var child = list.addChild();
            child.putLong("pos", packedPos);
            child.putLong("time", time);
        });
    }

    @Override
    public void deserialize(ValueInput input) {
        int schema = input.getIntOr("SchemaVersion", 1);
        if (schema > SCHEMA_VERSION) {
            dev.romankrukovsky.kubanhorizons.KubanHorizons.LOGGER.warn(
                    "Данные опыления: схема v{} новее поддерживаемой v{}; читаем в режиме совместимости.",
                    schema, SCHEMA_VERSION);
        }
        marks.clear();
        for (ValueInput child : input.childrenListOrEmpty("marks")) {
            marks.put(child.getLongOr("pos", 0L), child.getLongOr("time", 0L));
        }
    }
}
