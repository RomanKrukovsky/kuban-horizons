package dev.romankrukovsky.kubanhorizons.genie.runtime.snapshot;

import dev.romankrukovsky.kubanhorizons.genie.runtime.selection.RegionSelection;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SnapshotCodecTest {
    @Test
    void roundTripPreservesAllSupportedDomainsAndChecksDigest() throws Exception {
        CompoundTag state = new CompoundTag();
        state.putString("Name", "minecraft:stone");
        CompoundTag entity = new CompoundTag();
        entity.putString("id", "minecraft:pig");
        RegionSelection selection = new RegionSelection("minecraft:overworld", BlockPos.ZERO, BlockPos.ZERO);
        SnapshotService.SnapshotState snapshotState = new SnapshotService.SnapshotState(
                List.of(new RegionSnapshot.BlockRecord(0, 0, 0, state, null)),
                List.of(new RegionSnapshot.TickRecord(0, 0, 0, "minecraft:stone", 12, 0)),
                List.of(new RegionSnapshot.TickRecord(0, 0, 0, "minecraft:water", 4, 1)),
                List.of(new RegionSnapshot.EntityRecord(entity))
        );
        RegionSnapshot snapshot = new RegionSnapshot(RegionSnapshot.CURRENT_SCHEMA_VERSION,
                new SnapshotId(UUID.randomUUID(), "home"), UUID.randomUUID(), Instant.EPOCH,
                selection, snapshotState.blocks(), snapshotState.blockTicks(), snapshotState.fluidTicks(),
                snapshotState.entities(), SnapshotService.digest(snapshotState));

        assertEquals(snapshot, SnapshotCodec.decode(SnapshotCodec.encode(snapshot)));

        CompoundTag corrupted = SnapshotCodec.encode(snapshot);
        corrupted.putString("ContentDigest", "0".repeat(64));
        assertThrows(java.io.IOException.class, () -> SnapshotCodec.decode(corrupted));
    }
}
