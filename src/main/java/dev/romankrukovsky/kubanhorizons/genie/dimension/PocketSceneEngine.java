package dev.romankrukovsky.kubanhorizons.genie.dimension;

import dev.romankrukovsky.kubanhorizons.genie.runtime.selection.RegionSelection;
import dev.romankrukovsky.kubanhorizons.genie.runtime.snapshot.RegionSnapshot;
import dev.romankrukovsky.kubanhorizons.genie.runtime.snapshot.SnapshotId;
import dev.romankrukovsky.kubanhorizons.genie.runtime.snapshot.SnapshotService;
import net.minecraft.nbt.NbtUtils;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;

/** Движок 1-минутных карманных временных сцен (пляж, ресторан, дворец) (Pocket Scene Engine). */
public final class PocketSceneEngine {
    private PocketSceneEngine() {
    }

    /** Строит target-снимок пляжной сцены без изменения мира. */
    public static RegionSnapshot buildBeachTarget(ServerLevel level, BlockPos origin,
                                                  UUID ownerId) throws IOException {
        RegionSelection selection = new RegionSelection(level.dimension().identifier().toString(),
                origin.offset(-3, 0, -3), origin.offset(3, 4, 3));
        SnapshotService.SnapshotState current = SnapshotService.captureState(level, selection);
        List<RegionSnapshot.BlockRecord> blocks = new ArrayList<>(current.blocks().size());
        for (RegionSnapshot.BlockRecord record : current.blocks()) {
            net.minecraft.world.level.block.state.BlockState state;
            if (record.relativeY() == 0) {
                state = Blocks.SANDSTONE.defaultBlockState();
            } else if (record.relativeY() == 1 && record.relativeZ() == 1
                    && record.relativeX() >= 1 && record.relativeX() <= 5) {
                state = Blocks.CONCRETE.pick(net.minecraft.world.item.DyeColor.BLUE).defaultBlockState();
            } else if (record.relativeY() == 1 && record.relativeX() == 1 && record.relativeZ() == 5) {
                state = Blocks.CONCRETE.pick(net.minecraft.world.item.DyeColor.ORANGE).defaultBlockState();
            } else if (record.relativeY() == 1 && record.relativeX() == 5 && record.relativeZ() == 5) {
                state = Blocks.OAK_PLANKS.defaultBlockState();
            } else {
                state = Blocks.AIR.defaultBlockState();
            }
            blocks.add(new RegionSnapshot.BlockRecord(record.relativeX(), record.relativeY(),
                    record.relativeZ(), NbtUtils.writeBlockState(state), null));
        }
        SnapshotService.SnapshotState target = new SnapshotService.SnapshotState(
                blocks, List.of(), List.of(), List.of(), current.biomes());
        return new RegionSnapshot(RegionSnapshot.CURRENT_SCHEMA_VERSION,
                new SnapshotId(UUID.randomUUID(), "pocket_beach"), ownerId, Instant.now(), selection,
                target.blocks(), target.blockTicks(), target.fluidTicks(), target.entities(), target.biomes(),
                SnapshotService.digest(target));
    }

}
