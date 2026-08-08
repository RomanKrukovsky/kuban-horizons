package dev.romankrukovsky.kubanhorizons.genie.spatial;

import dev.romankrukovsky.kubanhorizons.genie.runtime.selection.RegionSelection;
import dev.romankrukovsky.kubanhorizons.genie.runtime.snapshot.RegionSnapshot;
import dev.romankrukovsky.kubanhorizons.genie.runtime.snapshot.SnapshotCodec;
import dev.romankrukovsky.kubanhorizons.genie.runtime.snapshot.SnapshotId;
import dev.romankrukovsky.kubanhorizons.genie.runtime.snapshot.SnapshotService;
import dev.romankrukovsky.kubanhorizons.genie.runtime.transaction.RegionRestorer;
import dev.romankrukovsky.kubanhorizons.registry.KHItems;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.Blocks;

/** Движок сжатия и миниатюризации мира (Spatial Compression & World Miniaturization Engine). */
public final class MiniaturizationEngine {
    private static final String PAYLOAD_KEY = "KubanMiniatureSnapshot";

    private MiniaturizationEngine() {
    }

    public static RegionSnapshot captureSelection(ServerLevel level, RegionSelection selection,
                                                  UUID ownerId) throws IOException {
        if (!selection.dimension().equals(level.dimension().identifier().toString())
                || selection.volume() > 32_768L) {
            throw new IllegalArgumentException("selection exceeds miniaturization limits");
        }
        SnapshotService.SnapshotState state = SnapshotService.captureState(level, selection);
        RegionSnapshot snapshot = new RegionSnapshot(RegionSnapshot.CURRENT_SCHEMA_VERSION,
                new SnapshotId(UUID.randomUUID(), "miniature"), ownerId, Instant.now(), selection,
                state.blocks(), state.blockTicks(), state.fluidTicks(), state.entities(),
                SnapshotService.digest(state));
        if (snapshot.blocks().stream().allMatch(record ->
                record.blockState().getStringOr("Name", "minecraft:air").equals("minecraft:air"))) {
            throw new IllegalArgumentException("selected region is empty");
        }
        return snapshot;
    }

    public static RegionSnapshot emptyTarget(RegionSnapshot snapshot) throws IOException {
        return emptySnapshot(snapshot);
    }

    public static ItemStack createMiniatureItem(RegionSnapshot snapshot) {
        ItemStack result = new ItemStack(KHItems.MINIATURE_WORLD.get());
        CompoundTag data = new CompoundTag();
        data.put(PAYLOAD_KEY, SnapshotCodec.encode(snapshot));
        CustomData.set(DataComponents.CUSTOM_DATA, result, data);
        return result;
    }

    public static boolean uncompressRegion(ServerLevel level, BlockPos target, ItemStack stack) {
        if (!stack.is(KHItems.MINIATURE_WORLD.get())) {
            return false;
        }
        CompoundTag data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        CompoundTag encoded = data.getCompound(PAYLOAD_KEY).orElse(null);
        if (encoded == null) {
            return false;
        }
        try {
            RegionSnapshot source = SnapshotCodec.decode(encoded);
            BlockPos max = target.offset(source.selection().max().subtract(source.selection().min()));
            RegionSelection destination = new RegionSelection(level.dimension().identifier().toString(), target, max);
            for (BlockPos pos : destination.positions()) {
                if (!level.isEmptyBlock(pos)) {
                    return false;
                }
            }
            RegionSnapshot moved = moveSnapshot(source, destination);
            RegionRestorer.apply(level, moved);
            stack.shrink(1);
            return true;
        } catch (IOException | IllegalArgumentException exception) {
            return false;
        }
    }

    private static RegionSnapshot emptySnapshot(RegionSnapshot source) throws IOException {
        CompoundTag air = new CompoundTag();
        air.putString("Name", "minecraft:air");
        List<RegionSnapshot.BlockRecord> blocks = source.blocks().stream()
                .map(record -> new RegionSnapshot.BlockRecord(record.relativeX(), record.relativeY(),
                        record.relativeZ(), air, null)).toList();
        SnapshotService.SnapshotState state = new SnapshotService.SnapshotState(blocks,
                List.of(), List.of(), List.of());
        return new RegionSnapshot(RegionSnapshot.CURRENT_SCHEMA_VERSION, source.id(), source.ownerId(),
                source.capturedAt(), source.selection(), blocks, List.of(), List.of(), List.of(),
                SnapshotService.digest(state));
    }

    private static RegionSnapshot moveSnapshot(RegionSnapshot source,
                                                RegionSelection destination) throws IOException {
        List<RegionSnapshot.EntityRecord> entities = new ArrayList<>();
        int dx = destination.min().getX() - source.selection().min().getX();
        int dy = destination.min().getY() - source.selection().min().getY();
        int dz = destination.min().getZ() - source.selection().min().getZ();
        List<RegionSnapshot.BlockRecord> blocks = new ArrayList<>();
        for (RegionSnapshot.BlockRecord record : source.blocks()) {
            CompoundTag blockEntity = record.blockEntity();
            if (blockEntity != null) {
                blockEntity.putInt("x", destination.min().getX() + record.relativeX());
                blockEntity.putInt("y", destination.min().getY() + record.relativeY());
                blockEntity.putInt("z", destination.min().getZ() + record.relativeZ());
            }
            blocks.add(new RegionSnapshot.BlockRecord(record.relativeX(), record.relativeY(),
                    record.relativeZ(), record.blockState(), blockEntity));
        }
        for (RegionSnapshot.EntityRecord record : source.entities()) {
            CompoundTag data = record.data();
            offsetEntityTree(data, dx, dy, dz);
            entities.add(new RegionSnapshot.EntityRecord(data));
        }
        SnapshotService.SnapshotState state = new SnapshotService.SnapshotState(blocks,
                source.blockTicks(), source.fluidTicks(), entities);
        return new RegionSnapshot(RegionSnapshot.CURRENT_SCHEMA_VERSION,
                new SnapshotId(UUID.randomUUID(), "unpacked"), source.ownerId(), Instant.now(), destination,
                state.blocks(), state.blockTicks(), state.fluidTicks(), state.entities(), SnapshotService.digest(state));
    }

    private static void offsetEntityTree(CompoundTag data, int dx, int dy, int dz) {
        data.read("Pos", net.minecraft.world.phys.Vec3.CODEC).ifPresent(pos ->
                data.store("Pos", net.minecraft.world.phys.Vec3.CODEC, pos.add(dx, dy, dz)));
        for (var tag : data.getListOrEmpty("Passengers")) {
            if (tag instanceof CompoundTag passenger) {
                offsetEntityTree(passenger, dx, dy, dz);
            }
        }
    }
}
