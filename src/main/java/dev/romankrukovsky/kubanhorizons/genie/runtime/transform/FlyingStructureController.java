package dev.romankrukovsky.kubanhorizons.genie.runtime.transform;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.romankrukovsky.kubanhorizons.KubanHorizons;
import dev.romankrukovsky.kubanhorizons.genie.runtime.selection.RegionSelection;
import dev.romankrukovsky.kubanhorizons.genie.runtime.snapshot.RegionSnapshot;
import dev.romankrukovsky.kubanhorizons.genie.runtime.snapshot.SnapshotService;
import dev.romankrukovsky.kubanhorizons.genie.runtime.snapshot.SnapshotStore;
import dev.romankrukovsky.kubanhorizons.genie.runtime.transaction.RegionRestorer;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.Vec3;

/**
 * Персистентный контроллер движущихся структур: снимок области летит по миру с
 * постоянной скоростью и приземляется по истечении срока.
 *
 * <p>Полёт переживает перезапуск сервера: реестр активных полётов хранится в
 * SavedData, сам снимок — в {@link SnapshotStore}. Живые существа в снимке
 * не поддерживаются (их перенос не реализован, как и в {@code FlyingStructureEngine}).</p>
 */
public final class FlyingStructureController extends SavedData {

    private static final String STORE_ROOT = "kubanhorizons/genie_runtime/snapshots";

    public static final Codec<ActiveFlight> FLIGHT_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("id").forGetter(flight -> flight.id.toString()),
            Codec.STRING.fieldOf("owner").forGetter(flight -> flight.ownerId.toString()),
            Codec.STRING.fieldOf("dimension").forGetter(ActiveFlight::dimension),
            Codec.STRING.fieldOf("snapshot").forGetter(flight -> flight.snapshotId.toString()),
            Codec.DOUBLE.fieldOf("x").forGetter(ActiveFlight::x),
            Codec.DOUBLE.fieldOf("y").forGetter(ActiveFlight::y),
            Codec.DOUBLE.fieldOf("z").forGetter(ActiveFlight::z),
            Codec.DOUBLE.fieldOf("vx").forGetter(ActiveFlight::vx),
            Codec.DOUBLE.fieldOf("vy").forGetter(ActiveFlight::vy),
            Codec.DOUBLE.fieldOf("vz").forGetter(ActiveFlight::vz),
            Codec.LONG.fieldOf("remainingTicks").forGetter(ActiveFlight::remainingTicks),
            Codec.STRING.fieldOf("blockStateTag").forGetter(ActiveFlight::blockStateTag)
    ).apply(instance, FlyingStructureController::decodeFlight));

    public static final Codec<FlyingStructureController> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.list(FLIGHT_CODEC).fieldOf("flights").forGetter(controller -> controller.flights)
    ).apply(instance, FlyingStructureController::new));

    public static final SavedDataType<FlyingStructureController> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(KubanHorizons.MOD_ID, "flying_structure_controller"),
            FlyingStructureController::new,
            CODEC);

    private final List<ActiveFlight> flights;
    private final ConcurrentMap<UUID, RegionSnapshot> cache = new ConcurrentHashMap<>();
    private SnapshotStore store;

    public FlyingStructureController() {
        this(new ArrayList<>());
    }

    public FlyingStructureController(List<ActiveFlight> flights) {
        this.flights = new ArrayList<>(Objects.requireNonNull(flights, "flights"));
    }

    public static FlyingStructureController get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(TYPE);
    }

    /** Регистрирует новый полёт: снимок публикуется в хранилище и удерживается в полёте. */
    public void start(ServerLevel level, RegionSnapshot snapshot, Vec3 velocity,
                      long durationTicks) throws IOException {
        Objects.requireNonNull(snapshot, "snapshot");
        Objects.requireNonNull(velocity, "velocity");
        if (!snapshot.selection().dimension().equals(level.dimension().identifier().toString())) {
            throw new IllegalArgumentException("snapshot belongs to another dimension");
        }
        if (durationTicks <= 0L) {
            throw new IllegalArgumentException("flight duration must be positive");
        }
        if (!snapshot.entities().isEmpty()) {
            throw new IllegalArgumentException("move living entities out of the structure first");
        }
        RegionSnapshot stored = withoutBiomes(snapshot);
        if (store(level).load(stored.id().value()).isEmpty()) {
            store(level).publish(stored);
        }
        cache.put(stored.id().value(), stored);
        BlockPos origin = stored.selection().min();
        ActiveFlight flight = new ActiveFlight(UUID.randomUUID(), stored.ownerId(),
                stored.selection().dimension(), stored.id().value(),
                origin.getX(), origin.getY(), origin.getZ(),
                velocity.x(), velocity.y(), velocity.z(), durationTicks,
                NbtUtils.structureToSnbt(NbtUtils.writeBlockState(Blocks.AIR.defaultBlockState())));
        flights.add(flight);
        setDirty();
    }

    /**
     * Продвигает активные полёты этого измерения на один шаг: старая позиция
     * очищается до воздуха, снимок переносится на новую, срок уменьшается.
     */
    public void tick(ServerLevel level) throws IOException {
        if (flights.isEmpty()) {
            return;
        }
        String dimension = level.dimension().identifier().toString();
        List<ActiveFlight> live = new ArrayList<>(flights.size());
        for (ActiveFlight flight : flights) {
            if (!flight.dimension().equals(dimension)) {
                live.add(flight);
                continue;
            }
            RegionSnapshot snapshot = loadSnapshot(level, flight);
            RegionRestorer.apply(level, clearTarget(snapshot, flight,
                    shiftedSelection(snapshot.selection(), originOf(flight))));

            double nextX = flight.x() + flight.vx();
            double nextY = flight.y() + flight.vy();
            double nextZ = flight.z() + flight.vz();
            long remaining = flight.remainingTicks() - 1L;
            ActiveFlight next = new ActiveFlight(flight.id(), flight.ownerId(), flight.dimension(),
                    flight.snapshotId(), nextX, nextY, nextZ,
                    flight.vx(), flight.vy(), flight.vz(), remaining, flight.blockStateTag());

            RegionRestorer.apply(level, shiftedSnapshot(snapshot,
                    shiftedSelection(snapshot.selection(), originOf(next))));

            if (remaining > 0L) {
                live.add(next);
            }
        }
        flights.clear();
        flights.addAll(live);
        setDirty();
    }

    public boolean isActive(ServerLevel level, UUID ownerId) {
        String dimension = level.dimension().identifier().toString();
        return flights.stream().anyMatch(flight ->
                flight.dimension().equals(dimension) && flight.ownerId().equals(ownerId));
    }

    public List<ActiveFlight> activeFlights() {
        return List.copyOf(flights);
    }

    private RegionSnapshot loadSnapshot(ServerLevel level, ActiveFlight flight) throws IOException {
        RegionSnapshot cached = cache.get(flight.snapshotId());
        if (cached != null) {
            return cached;
        }
        RegionSnapshot loaded = store(level).load(flight.snapshotId())
                .orElseThrow(() -> new IOException("flight snapshot "
                        + flight.snapshotId() + " is missing"));
        cache.put(flight.snapshotId(), loaded);
        return loaded;
    }

    private SnapshotStore store(ServerLevel level) {
        SnapshotStore current = store;
        if (current == null) {
            synchronized (this) {
                current = store;
                if (current == null) {
                    current = new SnapshotStore(level.getServer()
                            .getWorldPath(LevelResource.ROOT).resolve(STORE_ROOT));
                    store = current;
                }
            }
        }
        return current;
    }

    private static BlockPos originOf(ActiveFlight flight) {
        return new BlockPos((int) flight.x(), (int) flight.y(), (int) flight.z());
    }

    private static RegionSelection shiftedSelection(RegionSelection base, BlockPos origin) {
        int dx = origin.getX() - base.min().getX();
        int dy = origin.getY() - base.min().getY();
        int dz = origin.getZ() - base.min().getZ();
        return new RegionSelection(base.dimension(),
                base.min().offset(dx, dy, dz), base.max().offset(dx, dy, dz));
    }

    private static RegionSnapshot shiftedSnapshot(RegionSnapshot base, RegionSelection shifted) {
        return new RegionSnapshot(base.schemaVersion(), base.id(), base.ownerId(),
                base.capturedAt(), shifted, base.blocks(), base.blockTicks(), base.fluidTicks(),
                base.entities(), base.biomes(), base.contentDigest());
    }

    /** Тот же объём, что и у снимка, но из одного воздуха: убирает структуру с места. */
    private static RegionSnapshot clearTarget(RegionSnapshot snapshot, ActiveFlight flight,
                                              RegionSelection shifted) throws IOException {
        CompoundTag air;
        try {
            air = NbtUtils.snbtToStructure(flight.blockStateTag());
        } catch (CommandSyntaxException exception) {
            throw new IOException("invalid stored air state tag", exception);
        }
        List<RegionSnapshot.BlockRecord> cleared = new ArrayList<>(snapshot.blocks().size());
        for (RegionSnapshot.BlockRecord record : snapshot.blocks()) {
            cleared.add(new RegionSnapshot.BlockRecord(record.relativeX(), record.relativeY(),
                    record.relativeZ(), air, null));
        }
        SnapshotService.SnapshotState state = new SnapshotService.SnapshotState(
                cleared, List.of(), List.of(), List.of(), List.of());
        return new RegionSnapshot(RegionSnapshot.CURRENT_SCHEMA_VERSION, snapshot.id(),
                snapshot.ownerId(), snapshot.capturedAt(), shifted,
                cleared, List.of(), List.of(), List.of(), List.of(),
                SnapshotService.digest(state));
    }

    /** Летающая структура не несёт биом с собой: он остаётся на земле под ней. */
    private static RegionSnapshot withoutBiomes(RegionSnapshot snapshot) throws IOException {
        if (snapshot.biomes().isEmpty()) {
            return snapshot;
        }
        SnapshotService.SnapshotState state = new SnapshotService.SnapshotState(
                snapshot.blocks(), snapshot.blockTicks(), snapshot.fluidTicks(),
                snapshot.entities(), List.of());
        return new RegionSnapshot(RegionSnapshot.CURRENT_SCHEMA_VERSION, snapshot.id(),
                snapshot.ownerId(), snapshot.capturedAt(), snapshot.selection(),
                state.blocks(), state.blockTicks(), state.fluidTicks(), state.entities(),
                List.of(), SnapshotService.digest(state));
    }

    private static ActiveFlight decodeFlight(String id, String owner, String dimension,
                                             String snapshot, double x, double y, double z,
                                             double vx, double vy, double vz, long remainingTicks,
                                             String blockStateTag) {
        return new ActiveFlight(uuidOrNil(id), uuidOrNil(owner), dimension, uuidOrNil(snapshot),
                x, y, z, vx, vy, vz, remainingTicks, blockStateTag);
    }

    private static UUID uuidOrNil(String raw) {
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException ignored) {
            return new UUID(0L, 0L);
        }
    }

    /** Активный полёт одной структуры. snapshotId — ключ снимка в {@link SnapshotStore}. */
    public record ActiveFlight(
            UUID id,
            UUID ownerId,
            String dimension,
            UUID snapshotId,
            double x,
            double y,
            double z,
            double vx,
            double vy,
            double vz,
            long remainingTicks,
            String blockStateTag
    ) {
    }
}
