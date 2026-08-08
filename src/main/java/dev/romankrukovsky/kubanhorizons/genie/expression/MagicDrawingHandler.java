package dev.romankrukovsky.kubanhorizons.genie.expression;

import dev.romankrukovsky.kubanhorizons.genie.runtime.selection.RegionSelection;
import dev.romankrukovsky.kubanhorizons.genie.runtime.snapshot.RegionSnapshot;
import dev.romankrukovsky.kubanhorizons.genie.runtime.snapshot.SnapshotId;
import dev.romankrukovsky.kubanhorizons.genie.runtime.snapshot.SnapshotService;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;

/** Строит target магической линии-моста без прямой мутации мира. */
public final class MagicDrawingHandler {
    private MagicDrawingHandler() {
    }

    public static DrawingPlan buildLinePlan(ServerLevel level, BlockPos start, BlockPos end,
                                            UUID ownerId) throws IOException {
        RegionSelection selection = new RegionSelection(level.dimension().identifier().toString(), start, end);
        if (selection.volume() > 4_096L) {
            throw new IllegalArgumentException("drawing selection is too large");
        }
        SnapshotService.SnapshotState current = SnapshotService.captureState(level, selection);
        var targetBlocks = new ArrayList<RegionSnapshot.BlockRecord>(current.blocks());
        int dx = end.getX() - start.getX();
        int dy = end.getY() - start.getY();
        int dz = end.getZ() - start.getZ();
        int steps = Math.max(Math.abs(dx), Math.max(Math.abs(dy), Math.abs(dz)));
        for (int step = 0; step <= steps; step++) {
            double progress = steps == 0 ? 0.0D : (double) step / steps;
            BlockPos absolute = new BlockPos(start.getX() + (int) Math.round(dx * progress),
                    start.getY() + (int) Math.round(dy * progress),
                    start.getZ() + (int) Math.round(dz * progress));
            int rx = absolute.getX() - selection.min().getX();
            int ry = absolute.getY() - selection.min().getY();
            int rz = absolute.getZ() - selection.min().getZ();
            int width = selection.max().getX() - selection.min().getX() + 1;
            int height = selection.max().getY() - selection.min().getY() + 1;
            int index = (rz * height + ry) * width + rx;
            targetBlocks.set(index, new RegionSnapshot.BlockRecord(rx, ry, rz,
                    NbtUtils.writeBlockState(Blocks.OAK_PLANKS.defaultBlockState()), null));
        }
        SnapshotService.SnapshotState target = new SnapshotService.SnapshotState(targetBlocks,
                current.blockTicks(), current.fluidTicks(), current.entities(), current.biomes());
        RegionSnapshot before = new RegionSnapshot(RegionSnapshot.CURRENT_SCHEMA_VERSION,
                new SnapshotId(UUID.randomUUID(), "drawing_before"), ownerId, Instant.now(), selection,
                current.blocks(), current.blockTicks(), current.fluidTicks(), current.entities(),
                current.biomes(), SnapshotService.digest(current));
        RegionSnapshot after = new RegionSnapshot(RegionSnapshot.CURRENT_SCHEMA_VERSION,
                new SnapshotId(UUID.randomUUID(), "drawing_line"), ownerId, Instant.now(), selection,
                target.blocks(), target.blockTicks(), target.fluidTicks(), target.entities(),
                target.biomes(), SnapshotService.digest(target));
        return new DrawingPlan(before, after, start, end);
    }

    public record DrawingPlan(RegionSnapshot current, RegionSnapshot target,
                              BlockPos start, BlockPos end) {
    }
}
