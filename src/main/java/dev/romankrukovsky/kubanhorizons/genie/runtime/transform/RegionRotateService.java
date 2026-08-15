package dev.romankrukovsky.kubanhorizons.genie.runtime.transform;

import dev.romankrukovsky.kubanhorizons.genie.runtime.selection.RegionSelection;
import dev.romankrukovsky.kubanhorizons.genie.runtime.snapshot.RegionSnapshot;
import dev.romankrukovsky.kubanhorizons.genie.runtime.snapshot.SnapshotService;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Поворот снимка выделенной области на 90/180/270° вокруг центра выделения.
 *
 * <p>Существа НЕ поворачиваются вместе с областью (GENIE_VISION: поворот
 * сущностей ещё не готов) — они остаются на своих абсолютных позициях, а
 * поворачиваются только блоки, block entity и отложенные tick'и. Биомы области
 * от оси поворота не зависят.</p>
 */
public final class RegionRotateService {
    private RegionRotateService() {
    }

    /** Чистый перенос снимка без изменения мира: блоки, block entity и tick'и. */
    public static SnapshotService.SnapshotState rotate(SnapshotService.SnapshotState state,
                                                       RegionSelection selection,
                                                       net.minecraft.world.level.block.Rotation rotation) {
        Objects.requireNonNull(state, "state");
        Objects.requireNonNull(selection, "selection");
        net.minecraft.world.level.block.Rotation rot = rotation != null
                ? rotation : net.minecraft.world.level.block.Rotation.NONE;
        int width = selection.max().getX() - selection.min().getX() + 1;
        int depth = selection.max().getZ() - selection.min().getZ() + 1;
        if ((rot == net.minecraft.world.level.block.Rotation.CLOCKWISE_90
                || rot == net.minecraft.world.level.block.Rotation.COUNTERCLOCKWISE_90)
                && width != depth) {
            throw new IllegalArgumentException("90/270 rotation requires a square selection footprint");
        }
        if (rot == net.minecraft.world.level.block.Rotation.NONE) {
            return state;
        }
        RegionSnapshot.BlockRecord[] rotated = new RegionSnapshot.BlockRecord[state.blocks().size()];
        for (RegionSnapshot.BlockRecord record : state.blocks()) {
            Rotated pos = rotated(record.relativeX(), record.relativeZ(), width, depth, rot);
            int index = index(selection, pos.x(), record.relativeY(), pos.z());
            rotated[index] = new RegionSnapshot.BlockRecord(pos.x(), record.relativeY(), pos.z(),
                    rotatedBlockState(record.blockState(), rot),
                    rotatedBlockEntity(record.blockEntity(), selection, pos.x(),
                            record.relativeY(), pos.z()));
        }
        return new SnapshotService.SnapshotState(
                Arrays.stream(rotated).toList(),
                rotatedTicks(state.blockTicks(), selection, width, depth, rot),
                rotatedTicks(state.fluidTicks(), selection, width, depth, rot),
                state.entities(), state.biomes());
    }

    /** Целевой снимок транзакции: выделенная область, повёрнутая вокруг центра. */
    public static SnapshotService.SnapshotState buildRotatedTarget(SnapshotService.SnapshotState current,
                                                                   RegionSelection selection,
                                                                   net.minecraft.world.level.block.Rotation rotation) {
        Objects.requireNonNull(current, "current");
        Objects.requireNonNull(selection, "selection");
        net.minecraft.world.level.block.Rotation rot = rotation != null
                ? rotation : net.minecraft.world.level.block.Rotation.NONE;
        if (rot == net.minecraft.world.level.block.Rotation.NONE) {
            throw new IllegalArgumentException("rotation must be one of 90/180/270 degrees");
        }
        return rotate(current, selection, rot);
    }

    private static CompoundTag rotatedBlockState(CompoundTag blockState,
                                                 net.minecraft.world.level.block.Rotation rotation) {
        BlockState state = NbtUtils.readBlockState(BuiltInRegistries.BLOCK, blockState);
        return NbtUtils.writeBlockState(state.rotate(rotation));
    }

    private static CompoundTag rotatedBlockEntity(CompoundTag blockEntity, RegionSelection selection,
                                                  int relativeX, int relativeY, int relativeZ) {
        if (blockEntity == null) {
            return null;
        }
        CompoundTag copy = blockEntity.copy();
        copy.putInt("x", selection.min().getX() + relativeX);
        copy.putInt("y", selection.min().getY() + relativeY);
        copy.putInt("z", selection.min().getZ() + relativeZ);
        return copy;
    }

    private static List<RegionSnapshot.TickRecord> rotatedTicks(
            List<RegionSnapshot.TickRecord> ticks, RegionSelection selection,
            int width, int depth, net.minecraft.world.level.block.Rotation rotation) {
        if (ticks.isEmpty()) {
            return ticks;
        }
        List<RegionSnapshot.TickRecord> result = new ArrayList<>(ticks.size());
        for (RegionSnapshot.TickRecord tick : ticks) {
            Rotated pos = rotated(tick.relativeX(), tick.relativeZ(), width, depth, rotation);
            result.add(new RegionSnapshot.TickRecord(pos.x(), tick.relativeY(), pos.z(),
                    tick.typeId(), tick.delay(), tick.priority()));
        }
        return List.copyOf(result);
    }

    private static Rotated rotated(int relativeX, int relativeZ, int width, int depth,
                                   net.minecraft.world.level.block.Rotation rotation) {
        return switch (rotation) {
            case CLOCKWISE_90 -> new Rotated(width - 1 - relativeZ, relativeX);
            case CLOCKWISE_180 -> new Rotated(width - 1 - relativeX, depth - 1 - relativeZ);
            case COUNTERCLOCKWISE_90 -> new Rotated(relativeZ, width - 1 - relativeX);
            case NONE -> new Rotated(relativeX, relativeZ);
        };
    }

    private static int index(RegionSelection selection, int relativeX, int relativeY, int relativeZ) {
        int width = selection.max().getX() - selection.min().getX() + 1;
        int height = selection.max().getY() - selection.min().getY() + 1;
        return (relativeZ * height + relativeY) * width + relativeX;
    }

    private record Rotated(int x, int z) {
    }

    /** План поворота: снимок «до» и повёрнутый целевой снимок одной области. */
    public record RotatePlan(RegionSnapshot current, RegionSnapshot target) {
    }
}
