package dev.romankrukovsky.kubanhorizons.genie.dimension;

import dev.romankrukovsky.kubanhorizons.genie.aura.MagicalSignature;
import dev.romankrukovsky.kubanhorizons.genie.runtime.selection.RegionSelection;
import dev.romankrukovsky.kubanhorizons.genie.runtime.snapshot.RegionSnapshot;
import dev.romankrukovsky.kubanhorizons.genie.runtime.snapshot.SnapshotId;
import dev.romankrukovsky.kubanhorizons.genie.runtime.snapshot.SnapshotService;
import dev.romankrukovsky.kubanhorizons.genie.runtime.transaction.RegionRestorer;
import net.minecraft.nbt.NbtUtils;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;

/** Движок 1-минутных карманных временных сцен (пляж, ресторан, дворец) (Pocket Scene Engine). */
public final class PocketSceneEngine {
    private static final int DEFAULT_DURATION_TICKS = 20 * 60;
    private static final Map<ServerLevel, List<ActiveScene>> ACTIVE = new IdentityHashMap<>();

    private PocketSceneEngine() {
    }

    public static boolean spawnPocketScene(ServerLevel level, BlockPos origin, Player player, String type) {
        return spawnPocketScene(level, origin, player, type, DEFAULT_DURATION_TICKS);
    }

    public static boolean spawnPocketScene(ServerLevel level, BlockPos origin, Player player,
                                           String type, int durationTicks) {
        if (durationTicks < 1) {
            return false;
        }
        RegionSelection selection = new RegionSelection(level.dimension().identifier().toString(),
                origin.offset(-3, 0, -3), origin.offset(3, 4, 3));
        synchronized (ACTIVE) {
            if (ACTIVE.getOrDefault(level, List.of()).stream()
                    .anyMatch(active -> overlaps(active.snapshot().selection(), selection))) {
                return false;
            }
        }
        RegionSnapshot before;
        try {
            SnapshotService.SnapshotState state = SnapshotService.captureState(level, selection);
            before = new RegionSnapshot(RegionSnapshot.CURRENT_SCHEMA_VERSION,
                    new SnapshotId(UUID.randomUUID(), "pocket_scene"), player.getUUID(), Instant.now(), selection,
                    state.blocks(), state.blockTicks(), state.fluidTicks(), state.entities(),
                    SnapshotService.digest(state));
        } catch (IOException exception) {
            return false;
        }
        MagicalSignature.cast(level, net.minecraft.world.phys.Vec3.atCenterOf(origin));

        for (int x = -3; x <= 3; x++) {
            for (int z = -3; z <= 3; z++) {
                BlockPos pos = origin.offset(x, 0, z);
                level.setBlock(pos, Blocks.SAND.defaultBlockState(), 3);
                for (int y = 1; y <= 4; y++) {
                    level.setBlock(pos.above(y), Blocks.AIR.defaultBlockState(), 3);
                }
            }
        }
        for (int x = -2; x <= 2; x++) {
            level.setBlock(origin.offset(x, 1, -2), Blocks.WATER.defaultBlockState(), 3);
        }
        level.setBlock(origin.offset(-2, 1, 2), Blocks.CAMPFIRE.defaultBlockState(), 3);
        level.setBlock(origin.offset(2, 1, 2), Blocks.OAK_STAIRS.defaultBlockState(), 3);

        level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, origin.getX() + 0.5D, origin.getY() + 1.0D, origin.getZ() + 0.5D,
                30, 1.0D, 0.5D, 1.0D, 0.05D);
        player.sendSystemMessage(Component.translatable("message.kubanhorizons.genie.pocket_scene", type));
        synchronized (ACTIVE) {
            ACTIVE.computeIfAbsent(level, ignored -> new ArrayList<>())
                    .add(new ActiveScene(before, level.getGameTime() + durationTicks));
        }
        return true;
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
                blocks, List.of(), List.of(), List.of());
        return new RegionSnapshot(RegionSnapshot.CURRENT_SCHEMA_VERSION,
                new SnapshotId(UUID.randomUUID(), "pocket_beach"), ownerId, Instant.now(), selection,
                target.blocks(), target.blockTicks(), target.fluidTicks(), target.entities(),
                SnapshotService.digest(target));
    }


    public static void tickServer(ServerLevel level) {
        List<ActiveScene> due;
        synchronized (ACTIVE) {
            List<ActiveScene> scenes = ACTIVE.get(level);
            if (scenes == null) {
                return;
            }
            due = scenes.stream().filter(scene -> scene.restoreAt() <= level.getGameTime()).toList();
            scenes.removeAll(due);
            if (scenes.isEmpty()) {
                ACTIVE.remove(level);
            }
        }
        for (ActiveScene scene : due) {
            try {
                RegionRestorer.apply(level, scene.snapshot());
            } catch (IOException exception) {
                dev.romankrukovsky.kubanhorizons.KubanHorizons.LOGGER.error(
                        "Failed to restore pocket scene {}", scene.snapshot().id().value(), exception);
            }
        }
    }

    private static boolean overlaps(RegionSelection left, RegionSelection right) {
        return left.dimension().equals(right.dimension())
                && left.min().getX() <= right.max().getX() && right.min().getX() <= left.max().getX()
                && left.min().getY() <= right.max().getY() && right.min().getY() <= left.max().getY()
                && left.min().getZ() <= right.max().getZ() && right.min().getZ() <= left.max().getZ();
    }

    private record ActiveScene(RegionSnapshot snapshot, long restoreAt) {
    }
}
