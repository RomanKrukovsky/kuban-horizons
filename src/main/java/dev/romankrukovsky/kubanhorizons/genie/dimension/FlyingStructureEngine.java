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
        RegionSelection source = new RegionSelection(level.dimension().identifier().toString(),
                origin, origin.offset(1, 1, 1));
        RegionSelection destination = new RegionSelection(level.dimension().identifier().toString(),
                source.min().above(10), source.max().above(10));
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
                source.min(), destination.max());
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
            int index = index(closure, record.relativeX(), record.relativeY(), record.relativeZ());
            targetBlocks.set(index, new RegionSnapshot.BlockRecord(record.relativeX(),
                    record.relativeY(), record.relativeZ(), air, null));
        }
        int yOffset = destination.min().getY() - closure.min().getY();
        for (RegionSnapshot.BlockRecord record : sourceState.blocks()) {
            int index = index(closure, record.relativeX(), yOffset + record.relativeY(), record.relativeZ());
            var blockEntity = record.blockEntity();
            if (blockEntity != null) {
                blockEntity.putInt("x", destination.min().getX() + record.relativeX());
                blockEntity.putInt("y", destination.min().getY() + record.relativeY());
                blockEntity.putInt("z", destination.min().getZ() + record.relativeZ());
            }
            targetBlocks.set(index, new RegionSnapshot.BlockRecord(record.relativeX(),
                    yOffset + record.relativeY(), record.relativeZ(), record.blockState(), blockEntity));
        }
        var targetBlockTicks = new ArrayList<RegionSnapshot.TickRecord>();
        for (RegionSnapshot.TickRecord tick : current.blockTicks()) {
            if (tick.relativeY() > source.max().getY() - source.min().getY()) {
                targetBlockTicks.add(tick);
            }
        }
        for (RegionSnapshot.TickRecord tick : sourceState.blockTicks()) {
            targetBlockTicks.add(new RegionSnapshot.TickRecord(tick.relativeX(),
                    yOffset + tick.relativeY(), tick.relativeZ(), tick.typeId(),
                    tick.delay(), tick.priority()));
        }
        var targetFluidTicks = new ArrayList<RegionSnapshot.TickRecord>();
        for (RegionSnapshot.TickRecord tick : current.fluidTicks()) {
            if (tick.relativeY() > source.max().getY() - source.min().getY()) {
                targetFluidTicks.add(tick);
            }
        }
        for (RegionSnapshot.TickRecord tick : sourceState.fluidTicks()) {
            targetFluidTicks.add(new RegionSnapshot.TickRecord(tick.relativeX(),
                    yOffset + tick.relativeY(), tick.relativeZ(), tick.typeId(),
                    tick.delay(), tick.priority()));
        }
        SnapshotService.SnapshotState target = new SnapshotService.SnapshotState(
                targetBlocks, targetBlockTicks, targetFluidTicks, java.util.List.of());
        target = normalizeBlockStates(level, closure, target);
        RegionSnapshot currentSnapshot = new RegionSnapshot(RegionSnapshot.CURRENT_SCHEMA_VERSION,
                new SnapshotId(UUID.randomUUID(), "move_before"), ownerId, Instant.now(), closure,
                current.blocks(), current.blockTicks(), current.fluidTicks(), current.entities(),
                SnapshotService.digest(current));
        RegionSnapshot targetSnapshot = new RegionSnapshot(RegionSnapshot.CURRENT_SCHEMA_VERSION,
                new SnapshotId(UUID.randomUUID(), "flying_house"), ownerId, Instant.now(), closure,
                target.blocks(), target.blockTicks(), target.fluidTicks(), target.entities(),
                SnapshotService.digest(target));
        return new MovePlan(currentSnapshot, targetSnapshot, origin);
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
                target.fluidTicks(), target.entities());
    }

    private static int index(RegionSelection closure, int relativeX, int relativeY, int relativeZ) {
        int width = closure.max().getX() - closure.min().getX() + 1;
        int height = closure.max().getY() - closure.min().getY() + 1;
        return (relativeZ * height + relativeY) * width + relativeX;
    }

    public record MovePlan(RegionSnapshot current, RegionSnapshot target, BlockPos origin) {
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
