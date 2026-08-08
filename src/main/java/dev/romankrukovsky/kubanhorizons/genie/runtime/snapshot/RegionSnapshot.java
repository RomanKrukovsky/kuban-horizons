package dev.romankrukovsky.kubanhorizons.genie.runtime.snapshot;

import dev.romankrukovsky.kubanhorizons.genie.runtime.selection.RegionSelection;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;
import net.minecraft.nbt.CompoundTag;

/** Полный поддерживаемый before/target image выбранной области. */
public record RegionSnapshot(
        int schemaVersion,
        SnapshotId id,
        UUID ownerId,
        Instant capturedAt,
        RegionSelection selection,
        List<BlockRecord> blocks,
        List<TickRecord> blockTicks,
        List<TickRecord> fluidTicks,
        List<EntityRecord> entities,
        List<BiomeRecord> biomes,
        String contentDigest
) {
    public static final int CURRENT_SCHEMA_VERSION = 3;
    private static final Pattern DIGEST = Pattern.compile("[0-9a-f]{64}");

    public RegionSnapshot {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(ownerId, "ownerId");
        Objects.requireNonNull(capturedAt, "capturedAt");
        Objects.requireNonNull(selection, "selection");
        blocks = List.copyOf(Objects.requireNonNull(blocks, "blocks"));
        blockTicks = List.copyOf(Objects.requireNonNull(blockTicks, "blockTicks"));
        fluidTicks = List.copyOf(Objects.requireNonNull(fluidTicks, "fluidTicks"));
        entities = List.copyOf(Objects.requireNonNull(entities, "entities"));
        biomes = List.copyOf(Objects.requireNonNull(biomes, "biomes"));
        Objects.requireNonNull(contentDigest, "contentDigest");
        if (schemaVersion != CURRENT_SCHEMA_VERSION || !DIGEST.matcher(contentDigest).matches()) {
            throw new IllegalArgumentException("invalid snapshot metadata");
        }
        if (blocks.size() != selection.volume()) {
            throw new IllegalArgumentException("snapshot block count does not match selection volume");
        }
        blockTicks.forEach(tick -> requireInside(selection, tick.relativeX(), tick.relativeY(), tick.relativeZ()));
        fluidTicks.forEach(tick -> requireInside(selection, tick.relativeX(), tick.relativeY(), tick.relativeZ()));
    }

    public RegionSnapshot(int schemaVersion, SnapshotId id, UUID ownerId, Instant capturedAt,
                          RegionSelection selection, List<BlockRecord> blocks, String contentDigest) {
        this(schemaVersion, id, ownerId, capturedAt, selection, blocks,
                List.of(), List.of(), List.of(), List.of(), contentDigest);
    }

    public RegionSnapshot(int schemaVersion, SnapshotId id, UUID ownerId, Instant capturedAt,
                          RegionSelection selection, List<BlockRecord> blocks,
                          List<TickRecord> blockTicks, List<TickRecord> fluidTicks,
                          List<EntityRecord> entities, String contentDigest) {
        this(schemaVersion, id, ownerId, capturedAt, selection, blocks, blockTicks,
                fluidTicks, entities, List.of(), contentDigest);
    }

    private static void requireInside(RegionSelection selection, int x, int y, int z) {
        long width = (long) selection.max().getX() - selection.min().getX();
        long height = (long) selection.max().getY() - selection.min().getY();
        long depth = (long) selection.max().getZ() - selection.min().getZ();
        if (x < 0 || y < 0 || z < 0 || x > width || y > height || z > depth) {
            throw new IllegalArgumentException("snapshot record lies outside selection");
        }
    }

    public record BlockRecord(int relativeX, int relativeY, int relativeZ,
                              CompoundTag blockState, CompoundTag blockEntity) {
        public BlockRecord {
            blockState = Objects.requireNonNull(blockState, "blockState").copy();
            blockEntity = blockEntity == null ? null : blockEntity.copy();
        }

        @Override
        public CompoundTag blockState() {
            return blockState.copy();
        }

        @Override
        public CompoundTag blockEntity() {
            return blockEntity == null ? null : blockEntity.copy();
        }
    }

    /** Отложенный tick блока или жидкости с задержкой относительно момента снимка. */
    public record TickRecord(int relativeX, int relativeY, int relativeZ,
                             String typeId, int delay, int priority) {
        public TickRecord {
            Objects.requireNonNull(typeId, "typeId");
            if (!typeId.matches("[a-z0-9_.-]+:[a-z0-9_./-]+") || delay < 0) {
                throw new IllegalArgumentException("invalid scheduled tick");
            }
        }
    }

    /** Корневая сущность вместе с деревом пассажиров. Игроки и джиннии не попадают в снимок. */
    public record EntityRecord(CompoundTag data) {
        public EntityRecord {
            data = Objects.requireNonNull(data, "data").copy();
        }

        @Override
        public CompoundTag data() {
            return data.copy();
        }
    }

    /** Биом одной quart-ячейки (4×4×4 блока) в абсолютных quart-координатах. */
    public record BiomeRecord(int quartX, int quartY, int quartZ, String biomeId) {
        public BiomeRecord {
            Objects.requireNonNull(biomeId, "biomeId");
            if (!biomeId.matches("[a-z0-9_.-]+:[a-z0-9_./-]+")) {
                throw new IllegalArgumentException("invalid biome id");
            }
        }
    }
}
