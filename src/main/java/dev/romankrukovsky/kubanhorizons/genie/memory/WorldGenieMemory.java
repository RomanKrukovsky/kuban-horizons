package dev.romankrukovsky.kubanhorizons.genie.memory;

import dev.romankrukovsky.kubanhorizons.registry.KHAttachments;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.common.util.ValueIOSerializable;

/**
 * Долговременная память мира о Кубанской Джиннии: спасения, желания, встречи и места.
 *
 * <p>Здесь же живёт якорь единственности: UUID той сущности, которая является
 * джиннией этого мира, её последнее известное место и снимок личности. Якорь
 * хранится вместе с памятью, потому что это одна и та же сущность: «кто именно
 * джинния» — такой же факт о мире, как «где мы познакомились».</p>
 */
public final class WorldGenieMemory implements ValueIOSerializable {
    public static final int SCHEMA_VERSION = 2;

    private BlockPos firstDiscoveredPos = BlockPos.ZERO;
    private UUID firstOwnerId;
    private int totalWishesGranted;
    private int totalRescuesPerformed;
    private int savedVillagesCount;
    private final List<MemoryEntry> entries = new ArrayList<>();

    private UUID anchoredGenieId;
    private ResourceKey<Level> anchoredGenieDimension;
    private BlockPos anchoredGeniePos = BlockPos.ZERO;
    private GenieStateSnapshot anchoredGenieState;

    public static WorldGenieMemory get(ServerLevel level) {
        return level.getData(KHAttachments.GENIE_WORLD_MEMORY);
    }

    public void recordFirstDiscovery(BlockPos pos, UUID ownerId) {
        if (firstDiscoveredPos.equals(BlockPos.ZERO)) {
            firstDiscoveredPos = pos;
            firstOwnerId = ownerId;
            recordEvent(pos, "first_discovery", "message.kubanhorizons.genie.memory.first_discovery", 0L);
        }
    }

    public void recordWish(BlockPos pos, String wording, int precision, long gameTime) {
        totalWishesGranted++;
        recordEvent(pos, "wish", wording, gameTime);
    }

    public void recordRescue(BlockPos pos, long gameTime) {
        totalRescuesPerformed++;
        recordEvent(pos, "rescue", "message.kubanhorizons.genie.memory.rescue", gameTime);
    }

    public void recordVillageSaved(BlockPos pos, long gameTime) {
        savedVillagesCount++;
        recordEvent(pos, "village_saved", "message.kubanhorizons.genie.memory.village", gameTime);
    }

    public void recordEvent(BlockPos pos, String type, String note, long gameTime) {
        entries.add(new MemoryEntry(pos, type, note, gameTime));
        if (entries.size() > 500) {
            entries.removeFirst();
        }
    }

    public Optional<MemoryEntry> findNearbyMemory(BlockPos pos, double radiusBlocks) {
        double rSq = radiusBlocks * radiusBlocks;
        for (MemoryEntry entry : entries) {
            if (entry.pos().distSqr(pos) <= rSq) {
                return Optional.of(entry);
            }
        }
        return Optional.empty();
    }

    public BlockPos firstDiscoveredPos() {
        return firstDiscoveredPos;
    }

    public UUID firstOwnerId() {
        return firstOwnerId;
    }

    public int totalWishesGranted() {
        return totalWishesGranted;
    }

    public int totalRescuesPerformed() {
        return totalRescuesPerformed;
    }

    public int savedVillagesCount() {
        return savedVillagesCount;
    }

    public List<MemoryEntry> entries() {
        return Collections.unmodifiableList(entries);
    }

    // --- Якорь единственности ---

    /** UUID сущности, которая является джиннией этого мира, либо null. */
    public UUID anchoredGenieId() {
        return anchoredGenieId;
    }

    /** Измерение, в котором джиннию видели последний раз. */
    public Optional<ResourceKey<Level>> anchoredGenieDimension() {
        return Optional.ofNullable(anchoredGenieDimension);
    }

    /** Позиция, в которой джиннию видели последний раз. */
    public BlockPos anchoredGeniePosition() {
        return anchoredGeniePos;
    }

    /** Снимок личности якорной джиннии, если он уже сохранялся. */
    public Optional<GenieStateSnapshot> anchoredGenieState() {
        return Optional.ofNullable(anchoredGenieState);
    }

    /** Назначает якорную джиннию мира. */
    public void anchorGenie(UUID genieId, ResourceKey<Level> dimension, BlockPos pos) {
        anchoredGenieId = genieId;
        anchoredGenieDimension = dimension;
        anchoredGeniePos = pos;
    }

    /** Обновляет последнее известное место якорной джиннии. */
    public void updateAnchorLocation(ResourceKey<Level> dimension, BlockPos pos) {
        anchoredGenieDimension = dimension;
        anchoredGeniePos = pos;
    }

    /**
     * Запоминает личность джиннии отдельно от сущности.
     *
     * <p>Снимок нужен, чтобы восстановить характер и историю, если сущность
     * всё-таки была уничтожена: без него джинния возвращалась бы чистым
     * листом и теряла накопленные отношения.</p>
     */
    public void rememberGenieState(GenieStateSnapshot snapshot) {
        anchoredGenieState = snapshot;
    }

    /** Снимает якорь, позволяя миру привязаться к новой джиннии. */
    public void releaseGenieAnchor() {
        anchoredGenieId = null;
        anchoredGenieDimension = null;
        anchoredGeniePos = BlockPos.ZERO;
    }

    @Override
    public void serialize(ValueOutput output) {
        output.putInt("SchemaVersion", SCHEMA_VERSION);
        output.putLong("FirstPos", firstDiscoveredPos.asLong());
        if (firstOwnerId != null) {
            output.putString("FirstOwner", firstOwnerId.toString());
        }
        output.putInt("TotalWishes", totalWishesGranted);
        output.putInt("TotalRescues", totalRescuesPerformed);
        output.putInt("SavedVillages", savedVillagesCount);

        if (anchoredGenieId != null) {
            output.putString("AnchoredGenie", anchoredGenieId.toString());
        }
        if (anchoredGenieDimension != null) {
            output.putString("AnchoredGenieDimension", anchoredGenieDimension.identifier().toString());
        }
        output.putLong("AnchoredGeniePos", anchoredGeniePos.asLong());
        if (anchoredGenieState != null) {
            anchoredGenieState.save(output.child("AnchoredGenieState"));
        }

        var list = output.childrenList("Entries");
        for (MemoryEntry entry : entries) {
            var child = list.addChild();
            child.putLong("pos", entry.pos().asLong());
            child.putString("type", entry.type());
            child.putString("note", entry.note());
            child.putLong("time", entry.gameTime());
        }
    }

    @Override
    public void deserialize(ValueInput input) {
        firstDiscoveredPos = BlockPos.of(input.getLongOr("FirstPos", 0L));
        firstOwnerId = readUuid(input, "FirstOwner");
        totalWishesGranted = input.getIntOr("TotalWishes", 0);
        totalRescuesPerformed = input.getIntOr("TotalRescues", 0);
        savedVillagesCount = input.getIntOr("SavedVillages", 0);

        // Миры версии 1 якоря не содержат: там джинния станет якорной при
        // первой же прогрузке, что и требуется.
        anchoredGenieId = readUuid(input, "AnchoredGenie");
        anchoredGenieDimension = readDimension(input);
        anchoredGeniePos = BlockPos.of(input.getLongOr("AnchoredGeniePos", 0L));
        anchoredGenieState = input.child("AnchoredGenieState")
                .map(GenieStateSnapshot::load)
                .orElse(null);

        entries.clear();
        for (ValueInput child : input.childrenListOrEmpty("Entries")) {
            BlockPos pos = BlockPos.of(child.getLongOr("pos", 0L));
            String type = child.getStringOr("type", "generic");
            String note = child.getStringOr("note", "");
            long time = child.getLongOr("time", 0L);
            entries.add(new MemoryEntry(pos, type, note, time));
        }
    }

    private static UUID readUuid(ValueInput input, String key) {
        String raw = input.getStringOr(key, "");
        if (raw.isEmpty()) {
            return null;
        }
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private static ResourceKey<Level> readDimension(ValueInput input) {
        String raw = input.getStringOr("AnchoredGenieDimension", "");
        if (raw.isEmpty()) {
            return null;
        }
        Identifier id = Identifier.tryParse(raw);
        return id == null ? null : ResourceKey.create(Registries.DIMENSION, id);
    }

    public record MemoryEntry(BlockPos pos, String type, String note, long gameTime) {
    }
}
