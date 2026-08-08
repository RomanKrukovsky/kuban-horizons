package dev.romankrukovsky.kubanhorizons.genie.memory;

import dev.romankrukovsky.kubanhorizons.registry.KHAttachments;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.common.util.ValueIOSerializable;

/**
 * Долговременная память мира о Кубанской Джиннии: спасения, желания, встречи и места.
 */
public final class WorldGenieMemory implements ValueIOSerializable {
    public static final int SCHEMA_VERSION = 1;

    private BlockPos firstDiscoveredPos = BlockPos.ZERO;
    private UUID firstOwnerId;
    private int totalWishesGranted;
    private int totalRescuesPerformed;
    private int savedVillagesCount;
    private final List<MemoryEntry> entries = new ArrayList<>();

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
        String ownerStr = input.getStringOr("FirstOwner", "");
        if (!ownerStr.isEmpty()) {
            try {
                firstOwnerId = UUID.fromString(ownerStr);
            } catch (IllegalArgumentException ignored) {
                firstOwnerId = null;
            }
        }
        totalWishesGranted = input.getIntOr("TotalWishes", 0);
        totalRescuesPerformed = input.getIntOr("TotalRescues", 0);
        savedVillagesCount = input.getIntOr("SavedVillages", 0);

        entries.clear();
        for (ValueInput child : input.childrenListOrEmpty("Entries")) {
            BlockPos pos = BlockPos.of(child.getLongOr("pos", 0L));
            String type = child.getStringOr("type", "generic");
            String note = child.getStringOr("note", "");
            long time = child.getLongOr("time", 0L);
            entries.add(new MemoryEntry(pos, type, note, time));
        }
    }

    public record MemoryEntry(BlockPos pos, String type, String note, long gameTime) {
    }
}
