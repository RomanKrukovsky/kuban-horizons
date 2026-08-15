package dev.romankrukovsky.kubanhorizons.genie.entity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.romankrukovsky.kubanhorizons.KubanHorizons;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/**
 * Устойчивая память исполненных желаний мобов (корова, волк, голем, крипер).
 *
 * <p>Каждая запись привязывает желание к UUID конкретного моба и к
 * владельцу-джиннии. Джинния обращается к памяти при новом разговоре с тем же
 * мобом: количество исполненных желаний выбирает следующий, более
 * требовательный квест, а история позволяет сослаться на прошлые желания.
 * Память переживает сохранение мира через {@link SavedDataType} MC 26.2.</p>
 */
public final class MobWishMemory extends SavedData {

    private static final int MAX_RECORDS = 2000;

    public static final Codec<MobWishRecord> RECORD_CODEC = RecordCodecBuilder.create(
            instance -> instance.group(
                    Codec.STRING.fieldOf("mobId").forGetter(r -> r.mobId.toString()),
                    Codec.STRING.fieldOf("mobType").forGetter(MobWishRecord::mobType),
                    Codec.STRING.fieldOf("owner").forGetter(r -> r.ownerUuid.toString()),
                    Codec.STRING.fieldOf("wish").forGetter(MobWishRecord::wishText),
                    Codec.LONG.fieldOf("createdAt").forGetter(MobWishRecord::createdAt),
                    Codec.LONG.fieldOf("completedAt").forGetter(MobWishRecord::completedAt),
                    Codec.BOOL.fieldOf("fulfilled").forGetter(MobWishRecord::fulfilled),
                    Codec.STRING.fieldOf("rewardKey").forGetter(MobWishRecord::rewardKey)
            ).apply(instance, MobWishMemory::decodeRecord));

    public static final Codec<MobWishMemory> CODEC = RecordCodecBuilder.create(
            instance -> instance.group(
                    Codec.list(RECORD_CODEC).fieldOf("wishes").forGetter(m -> m.records)
            ).apply(instance, MobWishMemory::new));

    public static final SavedDataType<MobWishMemory> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(KubanHorizons.MOD_ID, "mob_wish_memory"),
            MobWishMemory::new,
            CODEC);

    private final List<MobWishRecord> records;

    public MobWishMemory() {
        this(new ArrayList<>());
    }

    public MobWishMemory(List<MobWishRecord> records) {
        this.records = new ArrayList<>(records);
    }

    public static MobWishMemory get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(TYPE);
    }

    public void record(MobWishRecord record) {
        records.add(record);
        while (records.size() > MAX_RECORDS) {
            records.removeFirst();
        }
        setDirty();
    }

    public Optional<MobWishRecord> pendingFor(UUID mobId) {
        for (int i = records.size() - 1; i >= 0; i--) {
            MobWishRecord record = records.get(i);
            if (record.mobId().equals(mobId) && !record.fulfilled()) {
                return Optional.of(record);
            }
        }
        return Optional.empty();
    }

    public void markFulfilled(UUID mobId) {
        for (int i = records.size() - 1; i >= 0; i--) {
            MobWishRecord record = records.get(i);
            if (record.mobId().equals(mobId)) {
                records.set(i, new MobWishRecord(record.mobId(), record.mobType(), record.ownerUuid(),
                        record.wishText(), record.createdAt(), record.createdAt(), true, record.rewardKey()));
                setDirty();
                return;
            }
        }
    }

    public Optional<MobWishRecord> lastFulfilledFor(UUID mobId) {
        for (int i = records.size() - 1; i >= 0; i--) {
            MobWishRecord record = records.get(i);
            if (record.mobId().equals(mobId) && record.fulfilled()) {
                return Optional.of(record);
            }
        }
        return Optional.empty();
    }

    public int fulfilledCountFor(UUID mobId) {
        int count = 0;
        for (MobWishRecord record : records) {
            if (record.mobId().equals(mobId) && record.fulfilled()) {
                count++;
            }
        }
        return count;
    }

    public List<MobWishRecord> history(UUID ownerUuid) {
        List<MobWishRecord> result = new ArrayList<>();
        for (MobWishRecord record : records) {
            if (record.ownerUuid().equals(ownerUuid)) {
                result.add(record);
            }
        }
        return Collections.unmodifiableList(result);
    }

    private static MobWishRecord decodeRecord(String mobId, String mobType, String owner,
                                              String wish, long createdAt, long completedAt,
                                              boolean fulfilled, String rewardKey) {
        return new MobWishRecord(parseUuid(mobId), mobType, parseUuid(owner), wish,
                createdAt, completedAt, fulfilled, rewardKey);
    }

    private static UUID parseUuid(String raw) {
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException ignored) {
            return new UUID(0L, 0L);
        }
    }

    public record MobWishRecord(
            UUID mobId,
            String mobType,
            UUID ownerUuid,
            String wishText,
            long createdAt,
            long completedAt,
            boolean fulfilled,
            String rewardKey) {
    }
}
