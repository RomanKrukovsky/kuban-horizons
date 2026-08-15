package dev.romankrukovsky.kubanhorizons.genie.memory;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.romankrukovsky.kubanhorizons.KubanHorizons;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/**
 * Персистентный provenance-журнал предметов и блоков, затронутых желаниями.
 *
 * <p>Каждая запись фиксирует, кто, когда, где и каким желанием создал, изменил
 * или уничтожил конкретный предмет или блок. Журнал переживает сохранение мира
 * и перезапуск сервера, а запросы {@link #queryById} и {@link #queryByPos}
 * позволяют джиннии отвечать на «откуда этот предмет?».</p>
 *
 * <p>Сериализация выполняется через {@link Codec} в формате MC 26.2
 * SavedDataType; UUID хранятся строками, BlockPos — packed long.</p>
 */
public final class ProvenanceJournal extends SavedData {

    private static final int MAX_RECORDS = 2000;

    public static final Codec<ProvenanceRecord> RECORD_CODEC = RecordCodecBuilder.create(
            instance -> instance.group(
                    Codec.LONG.fieldOf("time").forGetter(ProvenanceRecord::timestamp),
                    Codec.STRING.fieldOf("initiator").forGetter(r -> r.initiatorUuid.toString()),
                    Codec.STRING.fieldOf("action").forGetter(ProvenanceRecord::action),
                    Codec.STRING.fieldOf("target").forGetter(ProvenanceRecord::itemOrBlockId),
                    Codec.LONG.optionalFieldOf("pos", 0L).forGetter(r -> r.posOrSlot),
                    Codec.STRING.optionalFieldOf("wish", "").forGetter(ProvenanceRecord::wishText)
            ).apply(instance, ProvenanceJournal::decodeRecord));

    public static final Codec<ProvenanceJournal> CODEC = RecordCodecBuilder.create(
            instance -> instance.group(
                    Codec.list(RECORD_CODEC).fieldOf("records").forGetter(j -> j.records)
            ).apply(instance, ProvenanceJournal::new));

    public static final SavedDataType<ProvenanceJournal> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(KubanHorizons.MOD_ID, "provenance_journal"),
            ProvenanceJournal::new,
            CODEC);

    private final List<ProvenanceRecord> records;

    public ProvenanceJournal() {
        this(new ArrayList<>());
    }

    public ProvenanceJournal(List<ProvenanceRecord> records) {
        this.records = new ArrayList<>(records);
    }

    public static ProvenanceJournal get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(TYPE);
    }

    public void record(long timestamp, UUID initiatorUuid, String action,
                       String itemOrBlockId, long posOrSlot, String wishText) {
        records.add(new ProvenanceRecord(timestamp, initiatorUuid, action, itemOrBlockId, posOrSlot, wishText));
        while (records.size() > MAX_RECORDS) {
            records.removeFirst();
        }
        setDirty();
    }

    public List<ProvenanceRecord> queryById(String itemOrBlockId) {
        return records.stream()
                .filter(r -> r.itemOrBlockId.equals(itemOrBlockId))
                .toList();
    }

    public List<ProvenanceRecord> queryByPos(BlockPos pos) {
        long packed = pos.asLong();
        return records.stream()
                .filter(r -> r.posOrSlot == packed)
                .toList();
    }

    public List<ProvenanceRecord> recent(int n) {
        if (n <= 0) {
            return Collections.emptyList();
        }
        int from = Math.max(0, records.size() - n);
        return Collections.unmodifiableList(new ArrayList<>(records.subList(from, records.size())));
    }

    public Optional<ProvenanceRecord> last() {
        return records.isEmpty() ? Optional.empty() : Optional.of(records.get(records.size() - 1));
    }

    private static ProvenanceRecord decodeRecord(long time, String initiator, String action,
                                                 String target, long posOrSlot, String wishText) {
        UUID uuid;
        try {
            uuid = UUID.fromString(initiator);
        } catch (IllegalArgumentException ignored) {
            uuid = new UUID(0L, 0L);
        }
        return new ProvenanceRecord(time, uuid, action, target, posOrSlot, wishText);
    }

    public record ProvenanceRecord(
            long timestamp,
            UUID initiatorUuid,
            String action,
            String itemOrBlockId,
            long posOrSlot,
            String wishText) {
    }
}