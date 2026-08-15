package dev.romankrukovsky.kubanhorizons.genie.dimension;

import dev.romankrukovsky.kubanhorizons.genie.runtime.selection.RegionSelection;
import dev.romankrukovsky.kubanhorizons.genie.runtime.snapshot.RegionSnapshot;
import dev.romankrukovsky.kubanhorizons.genie.runtime.snapshot.SnapshotId;
import dev.romankrukovsky.kubanhorizons.genie.runtime.snapshot.SnapshotService;
import dev.romankrukovsky.kubanhorizons.worldgen.dimension.KHDimensions;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;

/** Движок карманных временных сцен (пляж, ресторан, дворец) в отдельном измерении. */
public final class PocketSceneEngine {
    private PocketSceneEngine() {
    }

    public static String pocketDimensionId() {
        return PocketDimension.POCKET.identifier().toString();
    }

    public static ServerLevel pocketLevel(ServerLevel anyLevel) {
        return anyLevel.getServer().getLevel(PocketDimension.POCKET);
    }

    public static boolean isPocketLevel(ServerLevel level) {
        return level.dimension().equals(PocketDimension.POCKET);
    }

    public static boolean isPocketLevelKey(net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> key) {
        return key.equals(PocketDimension.POCKET);
    }

    public static boolean isRecursivePocket(ServerLevel level) {
        return level.dimension().equals(PocketDimension.POCKET)
                || level.dimension().equals(KHDimensions.ETERNAL_KUBAN);
    }

    public static BlockPos sceneOriginFor(UUID sceneId) {
        long hash = sceneId.getMostSignificantBits() ^ sceneId.getLeastSignificantBits();
        int region = Math.floorMod((int) (hash >>> 32), 1024);
        int chunkX = region * 16;
        int chunkZ = Math.floorMod((int) hash, 1024) * 16;
        return new BlockPos(chunkX + 4, PocketDimension.FLOOR_Y + 1, chunkZ + 4);
    }

    public static RegionSnapshot buildBeachTarget(ServerLevel level, BlockPos origin,
                                                   UUID ownerId) throws IOException {
        RegionSelection selection = new RegionSelection(level.dimension().identifier().toString(),
                origin.offset(-3, 0, -3), origin.offset(3, 4, 3));
        SnapshotService.SnapshotState current = SnapshotService.captureState(level, selection);
        List<RegionSnapshot.BlockRecord> blocks = new ArrayList<>(current.blocks().size());
        for (RegionSnapshot.BlockRecord record : current.blocks()) {
            BlockState state;
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
                    record.relativeZ(), net.minecraft.nbt.NbtUtils.writeBlockState(state), null));
        }
        SnapshotService.SnapshotState target = new SnapshotService.SnapshotState(
                blocks, List.of(), List.of(), List.of(), current.biomes());
        return new RegionSnapshot(RegionSnapshot.CURRENT_SCHEMA_VERSION,
                new SnapshotId(UUID.randomUUID(), "pocket_beach"), ownerId, Instant.now(), selection,
                target.blocks(), target.blockTicks(), target.fluidTicks(), target.entities(), target.biomes(),
                SnapshotService.digest(target));
    }

    public static boolean enterPocketScene(ServerPlayer player, BlockPos sceneOrigin) {
        ServerLevel pocket = player.level().getServer().getLevel(PocketDimension.POCKET);
        if (pocket == null) {
            player.sendSystemMessage(Component.translatable(
                    "wish.kubanhorizons.pocket.missing"));
            return false;
        }
        if (isRecursivePocket((ServerLevel) player.level())) {
            player.sendSystemMessage(Component.translatable(
                    "wish.kubanhorizons.pocket.recursive"));
            return false;
        }
        pocket.getChunk(sceneOrigin.getX() >> 4, sceneOrigin.getZ() >> 4);
        Vec3 destination = Vec3.atBottomCenterOf(sceneOrigin.above());
        player.teleport(new TeleportTransition(pocket, destination, Vec3.ZERO,
                player.getYRot(), 0.0F, Set.<Relative>of(), TeleportTransition.DO_NOTHING));
        player.sendSystemMessage(Component.translatable("wish.kubanhorizons.pocket.enter"));
        return true;
    }

    public static boolean exitPocketScene(ServerPlayer player,
                                          net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> returnDimension,
                                          BlockPos returnPos) {
        ServerLevel target = player.level().getServer().getLevel(returnDimension);
        if (target == null) {
            return false;
        }
        Vec3 destination = Vec3.atBottomCenterOf(returnPos == null
                ? BlockPos.ZERO : returnPos.above());
        player.teleport(new TeleportTransition(target, destination, Vec3.ZERO,
                player.getYRot(), 0.0F, Set.<Relative>of(), TeleportTransition.DO_NOTHING));
        player.sendSystemMessage(Component.translatable("wish.kubanhorizons.pocket.exit"));
        return true;
    }
}