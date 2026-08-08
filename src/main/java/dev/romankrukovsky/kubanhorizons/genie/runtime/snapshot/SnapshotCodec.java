package dev.romankrukovsky.kubanhorizons.genie.runtime.snapshot;

import dev.romankrukovsky.kubanhorizons.genie.runtime.selection.RegionSelection;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;

/** Строгий NBT-формат снимка, независимый от runtime numeric IDs. */
public final class SnapshotCodec {
    private SnapshotCodec() {
    }

    public static CompoundTag encode(RegionSnapshot snapshot) {
        CompoundTag root = new CompoundTag();
        root.putInt("SchemaVersion", snapshot.schemaVersion());
        root.putString("SnapshotId", snapshot.id().value().toString());
        root.putString("Name", snapshot.id().name());
        root.putString("Owner", snapshot.ownerId().toString());
        root.putLong("CapturedEpochSecond", snapshot.capturedAt().getEpochSecond());
        root.putInt("CapturedNano", snapshot.capturedAt().getNano());
        root.putString("Dimension", snapshot.selection().dimension());
        root.putLong("Min", snapshot.selection().min().asLong());
        root.putLong("Max", snapshot.selection().max().asLong());
        root.putString("ContentDigest", snapshot.contentDigest());
        ListTag blocks = new ListTag();
        for (RegionSnapshot.BlockRecord record : snapshot.blocks()) {
            CompoundTag entry = new CompoundTag();
            entry.putInt("X", record.relativeX());
            entry.putInt("Y", record.relativeY());
            entry.putInt("Z", record.relativeZ());
            entry.put("State", record.blockState());
            if (record.blockEntity() != null) {
                entry.put("BlockEntity", record.blockEntity());
            }
            blocks.add(entry);
        }
        root.put("Blocks", blocks);
        root.put("BlockTicks", encodeTicks(snapshot.blockTicks()));
        root.put("FluidTicks", encodeTicks(snapshot.fluidTicks()));
        ListTag entities = new ListTag();
        for (RegionSnapshot.EntityRecord entity : snapshot.entities()) {
            entities.add(entity.data());
        }
        root.put("Entities", entities);
        ListTag biomes = new ListTag();
        for (RegionSnapshot.BiomeRecord biome : snapshot.biomes()) {
            CompoundTag entry = new CompoundTag();
            entry.putInt("QuartX", biome.quartX());
            entry.putInt("QuartY", biome.quartY());
            entry.putInt("QuartZ", biome.quartZ());
            entry.putString("Biome", biome.biomeId());
            biomes.add(entry);
        }
        root.put("Biomes", biomes);
        return root;
    }

    public static RegionSnapshot decode(CompoundTag root) throws IOException {
        try {
            int schema = root.getIntOr("SchemaVersion", -1);
            if (schema != RegionSnapshot.CURRENT_SCHEMA_VERSION) {
                throw new IOException("unsupported snapshot schema");
            }
            SnapshotId id = new SnapshotId(UUID.fromString(required(root, "SnapshotId")), required(root, "Name"));
            UUID owner = UUID.fromString(required(root, "Owner"));
            Instant captured = Instant.ofEpochSecond(root.getLongOr("CapturedEpochSecond", 0L),
                    root.getIntOr("CapturedNano", 0));
            RegionSelection selection = new RegionSelection(required(root, "Dimension"),
                    BlockPos.of(root.getLongOr("Min", Long.MIN_VALUE)),
                    BlockPos.of(root.getLongOr("Max", Long.MIN_VALUE)));
            List<RegionSnapshot.BlockRecord> records = new ArrayList<>();
            for (var tag : root.getListOrEmpty("Blocks")) {
                if (!(tag instanceof CompoundTag entry)) {
                    throw new IOException("snapshot contains non-compound block record");
                }
                CompoundTag state = entry.getCompound("State")
                        .orElseThrow(() -> new IOException("snapshot block has no state"));
                records.add(new RegionSnapshot.BlockRecord(entry.getIntOr("X", 0), entry.getIntOr("Y", 0),
                        entry.getIntOr("Z", 0), state, entry.getCompound("BlockEntity").orElse(null)));
            }
            List<RegionSnapshot.EntityRecord> entities = new ArrayList<>();
            for (var tag : root.getListOrEmpty("Entities")) {
                if (!(tag instanceof CompoundTag entity)) {
                    throw new IOException("snapshot contains non-compound entity record");
                }
                entities.add(new RegionSnapshot.EntityRecord(entity));
            }
            List<RegionSnapshot.BiomeRecord> biomes = new ArrayList<>();
            for (var tag : root.getListOrEmpty("Biomes")) {
                if (!(tag instanceof CompoundTag entry)) {
                    throw new IOException("snapshot contains non-compound biome record");
                }
                biomes.add(new RegionSnapshot.BiomeRecord(entry.getIntOr("QuartX", 0),
                        entry.getIntOr("QuartY", 0), entry.getIntOr("QuartZ", 0),
                        required(entry, "Biome")));
            }
            RegionSnapshot snapshot = new RegionSnapshot(schema, id, owner, captured, selection, records,
                    decodeTicks(root, "BlockTicks"), decodeTicks(root, "FluidTicks"), entities, biomes,
                    required(root, "ContentDigest"));
            if (!SnapshotService.digest(snapshot).equals(snapshot.contentDigest())) {
                throw new IOException("snapshot content digest mismatch");
            }
            return snapshot;
        } catch (IllegalArgumentException exception) {
            throw new IOException("malformed snapshot", exception);
        }
    }

    private static ListTag encodeTicks(List<RegionSnapshot.TickRecord> ticks) {
        ListTag result = new ListTag();
        for (RegionSnapshot.TickRecord tick : ticks) {
            CompoundTag entry = new CompoundTag();
            entry.putInt("X", tick.relativeX());
            entry.putInt("Y", tick.relativeY());
            entry.putInt("Z", tick.relativeZ());
            entry.putString("Type", tick.typeId());
            entry.putInt("Delay", tick.delay());
            entry.putInt("Priority", tick.priority());
            result.add(entry);
        }
        return result;
    }

    private static List<RegionSnapshot.TickRecord> decodeTicks(CompoundTag root,
                                                                String name) throws IOException {
        List<RegionSnapshot.TickRecord> result = new ArrayList<>();
        for (var tag : root.getListOrEmpty(name)) {
            if (!(tag instanceof CompoundTag entry)) {
                throw new IOException("snapshot contains non-compound tick record");
            }
            result.add(new RegionSnapshot.TickRecord(entry.getIntOr("X", 0), entry.getIntOr("Y", 0),
                    entry.getIntOr("Z", 0), required(entry, "Type"), entry.getIntOr("Delay", 0),
                    entry.getIntOr("Priority", 0)));
        }
        return List.copyOf(result);
    }

    private static String required(CompoundTag tag, String key) throws IOException {
        String value = tag.getStringOr(key, "");
        if (value.isEmpty()) {
            throw new IOException("snapshot is missing " + key);
        }
        return value;
    }
}
