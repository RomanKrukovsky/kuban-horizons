package dev.romankrukovsky.kubanhorizons.genie.ecology;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.romankrukovsky.kubanhorizons.KubanHorizons;
import dev.romankrukovsky.kubanhorizons.config.KHServerConfig;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Популяционный контроль гибридов по чанкам (Wishborne Ecology).
 *
 * <p>Ведёт счётчик живых гибридов на чанк и не позволяет размножению превысить
 * {@link KHServerConfig#hybridPopulationCapPerChunk()}. Счётчики переживают
 * сохранение мира и перезапуск сервера.</p>
 */
public final class PopulationControl extends SavedData {

    public static final Codec<ChunkCount> CHUNK_COUNT_CODEC = RecordCodecBuilder.create(
            instance -> instance.group(
                    Codec.INT.fieldOf("chunkX").forGetter(ChunkCount::chunkX),
                    Codec.INT.fieldOf("chunkZ").forGetter(ChunkCount::chunkZ),
                    Codec.INT.fieldOf("count").forGetter(ChunkCount::count)
            ).apply(instance, ChunkCount::new));

    public static final Codec<PopulationControl> CODEC = RecordCodecBuilder.create(
            instance -> instance.group(
                    Codec.list(CHUNK_COUNT_CODEC).fieldOf("chunks").forGetter(pc -> pc.hybrids.entrySet().stream()
                            .map(e -> new ChunkCount(e.getKey().x(), e.getKey().z(), e.getValue()))
                            .toList())
            ).apply(instance, PopulationControl::decode));

    public static final SavedDataType<PopulationControl> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(KubanHorizons.MOD_ID, "hybrid_population"),
            PopulationControl::new,
            CODEC);

    private final Map<ChunkPos, Integer> hybrids = new ConcurrentHashMap<>();

    public PopulationControl() {
    }

    public static PopulationControl get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(TYPE);
    }

    private static PopulationControl decode(List<ChunkCount> chunks) {
        PopulationControl control = new PopulationControl();
        for (ChunkCount entry : chunks) {
            control.hybrids.put(new ChunkPos(entry.chunkX, entry.chunkZ), entry.count);
        }
        return control;
    }

    /** Разрешено ли появление нового гибрида в чанке при заданном лимите. */
    public boolean canSpawn(ChunkPos pos, int cap) {
        return hybrids.getOrDefault(pos, 0) < cap;
    }

    public void registerSpawn(ChunkPos pos) {
        hybrids.merge(pos, 1, Integer::sum);
        setDirty();
    }

    public void registerDeath(ChunkPos pos) {
        hybrids.computeIfPresent(pos, (key, count) -> count > 1 ? count - 1 : null);
        setDirty();
    }

    public int count(ChunkPos pos) {
        return hybrids.getOrDefault(pos, 0);
    }

    public record ChunkCount(int chunkX, int chunkZ, int count) {
    }
}
