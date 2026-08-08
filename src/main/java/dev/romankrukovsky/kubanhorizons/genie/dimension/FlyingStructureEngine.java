package dev.romankrukovsky.kubanhorizons.genie.dimension;

import dev.romankrukovsky.kubanhorizons.genie.aura.MagicalSignature;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import dev.romankrukovsky.kubanhorizons.genie.runtime.selection.RegionSelection;
import dev.romankrukovsky.kubanhorizons.genie.runtime.snapshot.RegionSnapshot;
import dev.romankrukovsky.kubanhorizons.genie.runtime.snapshot.SnapshotId;
import dev.romankrukovsky.kubanhorizons.genie.runtime.snapshot.SnapshotService;
import java.io.IOException;
import java.time.Instant;
import java.util.UUID;
import java.util.ArrayList;

/** Движок создания летающих домов и перестройки деревень (Flying Structure & Village Wealth Engine). */
public final class FlyingStructureEngine {
    private FlyingStructureEngine() {
    }

    public static MovePlan buildMovePlan(ServerLevel level, BlockPos origin, UUID ownerId)
            throws IOException {
        return buildMovePlan(level, new RegionSelection(level.dimension().identifier().toString(),
                origin, origin.offset(1, 1, 1)), new BlockPos(0, 10, 0), ownerId);
    }

    public static MovePlan buildMovePlan(ServerLevel level, RegionSelection source,
                                         BlockPos offset, UUID ownerId) throws IOException {
        if (!source.dimension().equals(level.dimension().identifier().toString())) {
            throw new IllegalArgumentException("source belongs to another dimension");
        }
        if (offset.equals(BlockPos.ZERO)) {
            throw new IllegalArgumentException("move offset must not be zero");
        }
        RegionSelection destination = new RegionSelection(level.dimension().identifier().toString(),
                source.min().offset(offset), source.max().offset(offset));
        if (overlaps(source, destination)) {
            throw new IllegalArgumentException("overlapping moves are not supported yet");
        }
        for (BlockPos pos : destination.positions()) {
            if (!level.isEmptyBlock(pos)) {
                throw new IllegalArgumentException("destination region is not empty");
            }
        }
        SnapshotService.SnapshotState sourceState = SnapshotService.captureState(level, source);
        if (sourceState.blocks().stream().allMatch(record ->
                record.blockState().getStringOr("Name", "minecraft:air").equals("minecraft:air"))) {
            throw new IllegalArgumentException("source region is empty");
        }
        RegionSelection closure = new RegionSelection(level.dimension().identifier().toString(),
                new BlockPos(Math.min(source.min().getX(), destination.min().getX()),
                        Math.min(source.min().getY(), destination.min().getY()),
                        Math.min(source.min().getZ(), destination.min().getZ())),
                new BlockPos(Math.max(source.max().getX(), destination.max().getX()),
                        Math.max(source.max().getY(), destination.max().getY()),
                        Math.max(source.max().getZ(), destination.max().getZ())));
        SnapshotService.SnapshotState current = SnapshotService.captureState(level, closure);
        if (!current.entities().isEmpty()) {
            throw new IllegalArgumentException("move living entities out of the structure first");
        }
        net.minecraft.nbt.CompoundTag air = new net.minecraft.nbt.CompoundTag();
        air.putString("Name", "minecraft:air");
        var targetBlocks = new ArrayList<RegionSnapshot.BlockRecord>();
        for (RegionSnapshot.BlockRecord record : current.blocks()) {
            targetBlocks.add(record);
        }
        for (RegionSnapshot.BlockRecord record : sourceState.blocks()) {
            int sourceX = source.min().getX() - closure.min().getX() + record.relativeX();
            int sourceY = source.min().getY() - closure.min().getY() + record.relativeY();
            int sourceZ = source.min().getZ() - closure.min().getZ() + record.relativeZ();
            int index = index(closure, sourceX, sourceY, sourceZ);
            targetBlocks.set(index, new RegionSnapshot.BlockRecord(sourceX,
                    sourceY, sourceZ, air, null));
        }
        int xOffset = destination.min().getX() - closure.min().getX();
        int yOffset = destination.min().getY() - closure.min().getY();
        int zOffset = destination.min().getZ() - closure.min().getZ();
        for (RegionSnapshot.BlockRecord record : sourceState.blocks()) {
            int index = index(closure, xOffset + record.relativeX(),
                    yOffset + record.relativeY(), zOffset + record.relativeZ());
            var blockEntity = record.blockEntity();
            if (blockEntity != null) {
                blockEntity.putInt("x", destination.min().getX() + record.relativeX());
                blockEntity.putInt("y", destination.min().getY() + record.relativeY());
                blockEntity.putInt("z", destination.min().getZ() + record.relativeZ());
            }
            targetBlocks.set(index, new RegionSnapshot.BlockRecord(xOffset + record.relativeX(),
                    yOffset + record.relativeY(), zOffset + record.relativeZ(), record.blockState(), blockEntity));
        }
        var targetBlockTicks = new ArrayList<RegionSnapshot.TickRecord>();
        for (RegionSnapshot.TickRecord tick : current.blockTicks()) {
            if (!containsRelative(source, closure, tick.relativeX(), tick.relativeY(), tick.relativeZ())) {
                targetBlockTicks.add(tick);
            }
        }
        for (RegionSnapshot.TickRecord tick : sourceState.blockTicks()) {
            targetBlockTicks.add(new RegionSnapshot.TickRecord(xOffset + tick.relativeX(),
                    yOffset + tick.relativeY(), zOffset + tick.relativeZ(), tick.typeId(),
                    tick.delay(), tick.priority()));
        }
        var targetFluidTicks = new ArrayList<RegionSnapshot.TickRecord>();
        for (RegionSnapshot.TickRecord tick : current.fluidTicks()) {
            if (!containsRelative(source, closure, tick.relativeX(), tick.relativeY(), tick.relativeZ())) {
                targetFluidTicks.add(tick);
            }
        }
        for (RegionSnapshot.TickRecord tick : sourceState.fluidTicks()) {
            targetFluidTicks.add(new RegionSnapshot.TickRecord(xOffset + tick.relativeX(),
                    yOffset + tick.relativeY(), zOffset + tick.relativeZ(), tick.typeId(),
                    tick.delay(), tick.priority()));
        }
        SnapshotService.SnapshotState target = new SnapshotService.SnapshotState(
                targetBlocks, targetBlockTicks, targetFluidTicks, java.util.List.of(), current.biomes());
        target = normalizeBlockStates(level, closure, target);
        RegionSnapshot currentSnapshot = new RegionSnapshot(RegionSnapshot.CURRENT_SCHEMA_VERSION,
                new SnapshotId(UUID.randomUUID(), "move_before"), ownerId, Instant.now(), closure,
                current.blocks(), current.blockTicks(), current.fluidTicks(), current.entities(), current.biomes(),
                SnapshotService.digest(current));
        RegionSnapshot targetSnapshot = new RegionSnapshot(RegionSnapshot.CURRENT_SCHEMA_VERSION,
                new SnapshotId(UUID.randomUUID(), "flying_house"), ownerId, Instant.now(), closure,
                target.blocks(), target.blockTicks(), target.fluidTicks(), target.entities(), target.biomes(),
                SnapshotService.digest(target));
        return new MovePlan(currentSnapshot, targetSnapshot, source, offset);
    }

    private static SnapshotService.SnapshotState normalizeBlockStates(
            ServerLevel level, RegionSelection selection, SnapshotService.SnapshotState target) {
        var blocks = level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.BLOCK);
        var normalized = new ArrayList<RegionSnapshot.BlockRecord>(target.blocks().size());
        for (RegionSnapshot.BlockRecord record : target.blocks()) {
            var state = net.minecraft.nbt.NbtUtils.readBlockState(blocks, record.blockState());
            BlockPos pos = selection.min().offset(record.relativeX(), record.relativeY(), record.relativeZ());
            if (!state.canSurvive(level, pos) || state.is(Blocks.GRASS_BLOCK)
                    || state.is(Blocks.FARMLAND) || state.is(Blocks.DIRT_PATH)) {
                state = Blocks.AIR.defaultBlockState();
            }
            normalized.add(new RegionSnapshot.BlockRecord(record.relativeX(), record.relativeY(),
                    record.relativeZ(), net.minecraft.nbt.NbtUtils.writeBlockState(state),
                    state.hasBlockEntity() ? record.blockEntity() : null));
        }
        return new SnapshotService.SnapshotState(normalized, target.blockTicks(),
                target.fluidTicks(), target.entities(), target.biomes());
    }

    private static int index(RegionSelection closure, int relativeX, int relativeY, int relativeZ) {
        int width = closure.max().getX() - closure.min().getX() + 1;
        int height = closure.max().getY() - closure.min().getY() + 1;
        return (relativeZ * height + relativeY) * width + relativeX;
    }

    private static boolean containsRelative(RegionSelection source, RegionSelection closure,
                                            int x, int y, int z) {
        BlockPos absolute = closure.min().offset(x, y, z);
        return source.contains(absolute);
    }

    private static boolean overlaps(RegionSelection left, RegionSelection right) {
        return left.min().getX() <= right.max().getX() && right.min().getX() <= left.max().getX()
                && left.min().getY() <= right.max().getY() && right.min().getY() <= left.max().getY()
                && left.min().getZ() <= right.max().getZ() && right.min().getZ() <= left.max().getZ();
    }

    public record MovePlan(RegionSnapshot current, RegionSnapshot target,
                           RegionSelection source, BlockPos offset) {
    }

    public static boolean makeVillageWealthy(ServerLevel level, BlockPos villageCenter,
                                             net.minecraft.world.entity.player.Player player) {
        MagicalSignature.cast(level, net.minecraft.world.phys.Vec3.atCenterOf(villageCenter));

        for (int x = -5; x <= 5; x += 2) {
            for (int z = -5; z <= 5; z += 2) {
                BlockPos chestPos = villageCenter.offset(x, 0, z);
                if (level.isEmptyBlock(chestPos)) {
                    level.setBlock(chestPos, Blocks.GOLD_BLOCK.defaultBlockState(), 3);
                }
            }
        }

        player.sendSystemMessage(Component.translatable("message.kubanhorizons.genie.wealthy_village"));
        return true;
    }
}
