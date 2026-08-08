package dev.romankrukovsky.kubanhorizons.genie.runtime.snapshot;

import dev.romankrukovsky.kubanhorizons.genie.runtime.selection.RegionSelection;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.QuartPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.chunk.status.ChunkStatus;

/** Захватывает поддерживаемое состояние области без изменения мира. */
public final class SnapshotService {
    private final SnapshotStore store;

    public SnapshotService(SnapshotStore store) {
        this.store = Objects.requireNonNull(store, "store");
    }

    public RegionSnapshot capture(ServerLevel level, UUID ownerId, String name,
                                  RegionSelection selection, Instant now) throws IOException {
        return capture(level, ownerId, name, selection, now, false);
    }

    public RegionSnapshot captureInternal(ServerLevel level, UUID ownerId, String name,
                                          RegionSelection selection, Instant now) throws IOException {
        return capture(level, ownerId, name, selection, now, true);
    }

    private RegionSnapshot capture(ServerLevel level, UUID ownerId, String name,
                                   RegionSelection selection, Instant now,
                                   boolean internalName) throws IOException {
        Objects.requireNonNull(level, "level");
        Objects.requireNonNull(ownerId, "ownerId");
        Objects.requireNonNull(selection, "selection");
        if (!level.dimension().identifier().toString().equals(selection.dimension())) {
            throw new IllegalArgumentException("selection belongs to another dimension");
        }
        if (selection.min().getY() < level.getMinY() || selection.max().getY() >= level.getMaxY()) {
            throw new IllegalArgumentException("selection exceeds build height");
        }
        if (!internalName && (name.startsWith("u_") || name.startsWith("t_"))) {
            throw new IllegalArgumentException("snapshot name uses a reserved prefix");
        }
        if (store.findOwnedByName(ownerId, name).isPresent()) {
            throw new IllegalArgumentException("snapshot name already exists");
        }
        SnapshotState state = captureState(level, selection);
        String digest = digest(state);
        RegionSnapshot snapshot = new RegionSnapshot(RegionSnapshot.CURRENT_SCHEMA_VERSION,
                new SnapshotId(UUID.randomUUID(), name), ownerId, now, selection, state.blocks(),
                state.blockTicks(), state.fluidTicks(), state.entities(), state.biomes(), digest);
        store.publish(snapshot);
        return snapshot;
    }

    public static SnapshotState captureState(ServerLevel level, RegionSelection selection) {
        ensureChunksLoaded(level, selection);
        List<RegionSnapshot.BlockRecord> blocks = captureBlocks(level, selection);
        List<RegionSnapshot.TickRecord> blockTicks = new ArrayList<>();
        List<RegionSnapshot.TickRecord> fluidTicks = new ArrayList<>();
        BlockPos origin = selection.min();
        long gameTime = level.getGameTime();
        ChunkPos first = ChunkPos.containing(selection.min());
        ChunkPos last = ChunkPos.containing(selection.max());
        for (int chunkX = first.x(); chunkX <= last.x(); chunkX++) {
            for (int chunkZ = first.z(); chunkZ <= last.z(); chunkZ++) {
                LevelChunk chunk = level.getChunk(chunkX, chunkZ);
                var ticks = chunk.getTicksForSerialization(gameTime);
                ticks.blocks().stream().filter(tick -> selection.contains(tick.pos())).forEach(tick ->
                        blockTicks.add(tickRecord(origin, BuiltInRegistries.BLOCK.getKey(tick.type()).toString(),
                                tick.pos(), tick.delay(), tick.priority().getValue())));
                ticks.fluids().stream().filter(tick -> selection.contains(tick.pos())).forEach(tick ->
                        fluidTicks.add(tickRecord(origin, BuiltInRegistries.FLUID.getKey(tick.type()).toString(),
                                tick.pos(), tick.delay(), tick.priority().getValue())));
            }
        }
        List<RegionSnapshot.EntityRecord> entities = new ArrayList<>();
        AABB bounds = AABB.encapsulatingFullBlocks(selection.min(), selection.max());
        for (Entity entity : level.getEntities((Entity) null, bounds, SnapshotService::eligibleEntity)) {
            if (entity.isPassenger() && bounds.contains(entity.getVehicle().position())) {
                continue;
            }
            TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, level.registryAccess());
            if (entity.save(output)) {
                entities.add(new RegionSnapshot.EntityRecord(output.buildResult()));
            }
        }
        entities.sort(java.util.Comparator.comparing(record -> record.data().getStringOr("id", "")));
        List<RegionSnapshot.BiomeRecord> biomes = captureBiomes(level, selection);
        blockTicks.sort(TICK_ORDER);
        fluidTicks.sort(TICK_ORDER);
        return new SnapshotState(blocks, blockTicks, fluidTicks, entities, biomes);
    }

    public static List<RegionSnapshot.BlockRecord> captureBlocks(ServerLevel level,
                                                                  RegionSelection selection) {
        ensureChunksLoaded(level, selection);
        List<RegionSnapshot.BlockRecord> blocks = new ArrayList<>((int) selection.volume());
        BlockPos origin = selection.min();
        for (BlockPos mutable : selection.positions()) {
            BlockPos pos = mutable.immutable();
            CompoundTag blockEntity = level.getBlockEntity(pos) == null ? null
                    : level.getBlockEntity(pos).saveWithFullMetadata(level.registryAccess());
            blocks.add(new RegionSnapshot.BlockRecord(pos.getX() - origin.getX(), pos.getY() - origin.getY(),
                    pos.getZ() - origin.getZ(), NbtUtils.writeBlockState(level.getBlockState(pos)), blockEntity));
        }
        return List.copyOf(blocks);
    }

    public static String digest(SnapshotState state) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                 DataOutputStream output = new DataOutputStream(bytes)) {
                for (RegionSnapshot.BlockRecord block : state.blocks()) {
                    output.writeInt(block.relativeX());
                    output.writeInt(block.relativeY());
                    output.writeInt(block.relativeZ());
                    NbtIo.write(block.blockState(), output);
                    output.writeBoolean(block.blockEntity() != null);
                    if (block.blockEntity() != null) {
                        NbtIo.write(block.blockEntity(), output);
                    }
                }
                writeTicks(output, state.blockTicks());
                writeTicks(output, state.fluidTicks());
                for (RegionSnapshot.EntityRecord entity : state.entities()) {
                    NbtIo.write(entity.data(), output);
                }
                for (RegionSnapshot.BiomeRecord biome : state.biomes()) {
                    output.writeInt(biome.quartX());
                    output.writeInt(biome.quartY());
                    output.writeInt(biome.quartZ());
                    output.writeUTF(biome.biomeId());
                }
                output.flush();
                return HexFormat.of().formatHex(digest.digest(bytes.toByteArray()));
            }
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }

    public static String digest(RegionSnapshot snapshot) throws IOException {
        return digest(new SnapshotState(snapshot.blocks(), snapshot.blockTicks(),
                snapshot.fluidTicks(), snapshot.entities(), snapshot.biomes()));
    }

    /** Оставлен для block-only проверок старых потребителей. */
    public static String digest(List<RegionSnapshot.BlockRecord> blocks) throws IOException {
        return digest(new SnapshotState(blocks, List.of(), List.of(), List.of(), List.of()));
    }

    private static final java.util.Comparator<RegionSnapshot.TickRecord> TICK_ORDER =
            java.util.Comparator.comparingInt(RegionSnapshot.TickRecord::relativeX)
                    .thenComparingInt(RegionSnapshot.TickRecord::relativeY)
                    .thenComparingInt(RegionSnapshot.TickRecord::relativeZ)
                    .thenComparing(RegionSnapshot.TickRecord::typeId)
                    .thenComparingInt(RegionSnapshot.TickRecord::delay)
                    .thenComparingInt(RegionSnapshot.TickRecord::priority);

    private static RegionSnapshot.TickRecord tickRecord(BlockPos origin, String typeId,
                                                         BlockPos pos, int delay, int priority) {
        return new RegionSnapshot.TickRecord(pos.getX() - origin.getX(), pos.getY() - origin.getY(),
                pos.getZ() - origin.getZ(), typeId, Math.max(0, delay), priority);
    }

    private static void writeTicks(DataOutputStream output,
                                   List<RegionSnapshot.TickRecord> ticks) throws IOException {
        for (RegionSnapshot.TickRecord tick : ticks) {
            output.writeInt(tick.relativeX());
            output.writeInt(tick.relativeY());
            output.writeInt(tick.relativeZ());
            output.writeUTF(tick.typeId());
            output.writeInt(tick.delay());
            output.writeInt(tick.priority());
        }
    }

    private static boolean eligibleEntity(Entity entity) {
        return !(entity instanceof Player)
                && !(entity instanceof dev.romankrukovsky.kubanhorizons.entity.KubanGenie)
                && entity.getEncodeId() != null;
    }

    private static List<RegionSnapshot.BiomeRecord> captureBiomes(ServerLevel level,
                                                                  RegionSelection selection) {
        List<RegionSnapshot.BiomeRecord> result = new ArrayList<>();
        int minX = QuartPos.fromBlock(selection.min().getX());
        int minY = QuartPos.fromBlock(selection.min().getY());
        int minZ = QuartPos.fromBlock(selection.min().getZ());
        int maxX = QuartPos.fromBlock(selection.max().getX());
        int maxY = QuartPos.fromBlock(selection.max().getY());
        int maxZ = QuartPos.fromBlock(selection.max().getZ());
        for (int quartX = minX; quartX <= maxX; quartX++) {
            for (int quartY = minY; quartY <= maxY; quartY++) {
                for (int quartZ = minZ; quartZ <= maxZ; quartZ++) {
                    var holder = level.getNoiseBiome(quartX, quartY, quartZ);
                    String id = holder.unwrapKey().orElseThrow().identifier().toString();
                    result.add(new RegionSnapshot.BiomeRecord(quartX, quartY, quartZ, id));
                }
            }
        }
        return List.copyOf(result);
    }

    private static void ensureChunksLoaded(ServerLevel level, RegionSelection selection) {
        ChunkPos first = ChunkPos.containing(selection.min());
        ChunkPos last = ChunkPos.containing(selection.max());
        for (int chunkX = first.x(); chunkX <= last.x(); chunkX++) {
            for (int chunkZ = first.z(); chunkZ <= last.z(); chunkZ++) {
                if (level.getChunk(chunkX, chunkZ, ChunkStatus.FULL, false) == null) {
                    throw new IllegalStateException("all selected chunks must be loaded before capture");
                }
            }
        }
    }

    public record SnapshotState(List<RegionSnapshot.BlockRecord> blocks,
                                List<RegionSnapshot.TickRecord> blockTicks,
                                List<RegionSnapshot.TickRecord> fluidTicks,
                                List<RegionSnapshot.EntityRecord> entities,
                                List<RegionSnapshot.BiomeRecord> biomes) {
        public SnapshotState {
            blocks = List.copyOf(blocks);
            blockTicks = List.copyOf(blockTicks);
            fluidTicks = List.copyOf(fluidTicks);
            entities = List.copyOf(entities);
            biomes = List.copyOf(biomes);
        }

        public SnapshotState(List<RegionSnapshot.BlockRecord> blocks,
                             List<RegionSnapshot.TickRecord> blockTicks,
                             List<RegionSnapshot.TickRecord> fluidTicks,
                             List<RegionSnapshot.EntityRecord> entities) {
            this(blocks, blockTicks, fluidTicks, entities, List.of());
        }
    }
}
