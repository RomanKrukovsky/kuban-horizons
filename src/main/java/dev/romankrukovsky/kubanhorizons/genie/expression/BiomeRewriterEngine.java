package dev.romankrukovsky.kubanhorizons.genie.expression;

import dev.romankrukovsky.kubanhorizons.genie.runtime.selection.RegionSelection;
import dev.romankrukovsky.kubanhorizons.genie.runtime.snapshot.RegionSnapshot;
import dev.romankrukovsky.kubanhorizons.genie.runtime.snapshot.SnapshotId;
import dev.romankrukovsky.kubanhorizons.genie.runtime.snapshot.SnapshotService;
import dev.romankrukovsky.kubanhorizons.worldgen.KHBiomes;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.QuartPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;

/** Строит reversible target локального переписывания биома. */
public final class BiomeRewriterEngine {
    private BiomeRewriterEngine() {
    }

    public static BiomePlan buildSteppePlan(ServerLevel level, BlockPos center,
                                            UUID ownerId) throws IOException {
        int radius = 16;
        RegionSelection selection = new RegionSelection(level.dimension().identifier().toString(),
                center.offset(-radius, -2, -radius), center.offset(radius, 2, radius));
        SnapshotService.SnapshotState current = SnapshotService.captureState(level, selection);
        String targetId = KHBiomes.KUBAN_STEPPE.identifier().toString();
        List<RegionSnapshot.BiomeRecord> targetBiomes = current.biomes().stream()
                .map(record -> {
                    int blockX = QuartPos.toBlock(record.quartX()) + 2;
                    int blockZ = QuartPos.toBlock(record.quartZ()) + 2;
                    int dx = blockX - center.getX();
                    int dz = blockZ - center.getZ();
                    return dx * dx + dz * dz <= radius * radius
                            ? new RegionSnapshot.BiomeRecord(record.quartX(), record.quartY(),
                                    record.quartZ(), targetId)
                            : record;
                }).toList();
        SnapshotService.SnapshotState target = new SnapshotService.SnapshotState(current.blocks(),
                current.blockTicks(), current.fluidTicks(), current.entities(), targetBiomes);
        RegionSnapshot before = new RegionSnapshot(RegionSnapshot.CURRENT_SCHEMA_VERSION,
                new SnapshotId(UUID.randomUUID(), "biome_before"), ownerId, Instant.now(), selection,
                current.blocks(), current.blockTicks(), current.fluidTicks(), current.entities(),
                current.biomes(), SnapshotService.digest(current));
        RegionSnapshot after = new RegionSnapshot(RegionSnapshot.CURRENT_SCHEMA_VERSION,
                new SnapshotId(UUID.randomUUID(), "biome_steppe"), ownerId, Instant.now(), selection,
                target.blocks(), target.blockTicks(), target.fluidTicks(), target.entities(),
                target.biomes(), SnapshotService.digest(target));
        if (level.registryAccess().lookupOrThrow(Registries.BIOME).get(KHBiomes.KUBAN_STEPPE).isEmpty()) {
            throw new IOException("Kuban steppe biome is unavailable");
        }
        return new BiomePlan(before, after, center);
    }

    public record BiomePlan(RegionSnapshot current, RegionSnapshot target, BlockPos center) {
    }
}
