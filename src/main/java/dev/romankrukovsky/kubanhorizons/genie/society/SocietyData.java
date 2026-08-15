package dev.romankrukovsky.kubanhorizons.genie.society;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.romankrukovsky.kubanhorizons.KubanHorizons;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/**
 * Персистентная репутация владельцев джиннии в глазах магии и общества.
 *
 * <p>Хранит репутацию 0..100 на игрока: каждое исполненное желание её
 * поднимает, безрассудное — опускает. Слухи и мифы строятся поверх неё
 * (см. {@link SocietySimulator#rumorFor}). Переживает перезапуск сервера
 * через SavedDataType, как {@link dev.romankrukovsky.kubanhorizons.genie.memory.ProvenanceJournal}.</p>
 */
public final class SocietyData extends SavedData {

    public static final Codec<SocietyData> CODEC = RecordCodecBuilder.create(
            instance -> instance.group(
                    Codec.unboundedMap(Codec.STRING, Codec.INT)
                            .fieldOf("reputation").forGetter(SocietyData::serializeReputation)
            ).apply(instance, SocietyData::deserialize));

    public static final SavedDataType<SocietyData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(KubanHorizons.MOD_ID, "society_data"),
            SocietyData::new,
            CODEC);

    private final Map<UUID, Integer> reputationByOwner = new HashMap<>();

    public SocietyData() {
    }

    private SocietyData(Map<UUID, Integer> reputation) {
        reputationByOwner.putAll(reputation);
    }

    public static SocietyData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(TYPE);
    }

    /**
     * Записывает исполненное желание: успешное безопасное +5, безрассудное −10.
     * Значение всегда зажимается в 0..100.
     */
    public void recordWish(UUID owner, boolean wellReceived) {
        int delta = wellReceived ? 5 : -10;
        int next = clamp(reputationByOwner.getOrDefault(owner, 0) + delta);
        reputationByOwner.put(owner, next);
        setDirty();
    }

    public int reputation(UUID owner) {
        return reputationByOwner.getOrDefault(owner, 0);
    }

    /** Владельцы, о которых магия уже успела составить мнение. */
    public List<UUID> knownOwners() {
        return new ArrayList<>(reputationByOwner.keySet());
    }

    private static int clamp(int value) {
        return Math.max(0, Math.min(100, value));
    }

    private Map<String, Integer> serializeReputation() {
        Map<String, Integer> flat = new HashMap<>();
        for (Map.Entry<UUID, Integer> entry : reputationByOwner.entrySet()) {
            flat.put(entry.getKey().toString(), entry.getValue());
        }
        return flat;
    }

    private static SocietyData deserialize(Map<String, Integer> reputation) {
        Map<UUID, Integer> decoded = new HashMap<>();
        for (Map.Entry<String, Integer> entry : reputation.entrySet()) {
            try {
                decoded.put(UUID.fromString(entry.getKey()), entry.getValue());
            } catch (IllegalArgumentException ignored) {
                // Битые ключи из старого сохранения не должны ронять мир.
            }
        }
        return new SocietyData(decoded);
    }
}
