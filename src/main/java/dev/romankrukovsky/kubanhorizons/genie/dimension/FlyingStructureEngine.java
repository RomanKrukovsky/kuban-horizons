package dev.romankrukovsky.kubanhorizons.genie.dimension;

import dev.romankrukovsky.kubanhorizons.genie.aura.MagicalSignature;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
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
        return buildMovePlan(level, source, offset, Rotation.NONE, ownerId);
    }

    public static MovePlan buildMovePlan(ServerLevel level, RegionSelection source,
                                         BlockPos offset, Rotation rotation,
                                         UUID ownerId) throws IOException {
        if (!source.dimension().equals(level.dimension().identifier().toString())) {
            throw new IllegalArgumentException("source belongs to another dimension");
        }
        if (offset.equals(BlockPos.ZERO)) {
            throw new IllegalArgumentException("move offset must not be zero");
        }
        int sourceWidth = source.max().getX() - source.min().getX() + 1;
        int sourceHeight = source.max().getY() - source.min().getY() + 1;
        int sourceDepth = source.max().getZ() - source.min().getZ() + 1;
        int destinationWidth = swapsHorizontalAxes(rotation) ? sourceDepth : sourceWidth;
        int destinationDepth = swapsHorizontalAxes(rotation) ? sourceWidth : sourceDepth;
        BlockPos destinationMin = source.min().offset(offset);
        RegionSelection destination = new RegionSelection(level.dimension().identifier().toString(),
                destinationMin, destinationMin.offset(destinationWidth - 1,
                sourceHeight - 1, destinationDepth - 1));
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
        rejectProtectedEntities(level, source, destination);
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
            BlockPos transformed = transformBlock(record.relativeX(), record.relativeY(),
                    record.relativeZ(), sourceWidth, sourceDepth, rotation);
            int targetX = xOffset + transformed.getX();
            int targetY = yOffset + transformed.getY();
            int targetZ = zOffset + transformed.getZ();
            int index = index(closure, targetX, targetY, targetZ);
            var blockEntity = record.blockEntity();
            if (blockEntity != null) {
                blockEntity.putInt("x", destination.min().getX() + transformed.getX());
                blockEntity.putInt("y", destination.min().getY() + transformed.getY());
                blockEntity.putInt("z", destination.min().getZ() + transformed.getZ());
            }
            var state = net.minecraft.nbt.NbtUtils.readBlockState(
                    level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.BLOCK),
                    record.blockState()).rotate(rotation);
            targetBlocks.set(index, new RegionSnapshot.BlockRecord(targetX, targetY, targetZ,
                    net.minecraft.nbt.NbtUtils.writeBlockState(state), blockEntity));
        }
        var targetBlockTicks = new ArrayList<RegionSnapshot.TickRecord>();
        for (RegionSnapshot.TickRecord tick : current.blockTicks()) {
            if (!containsRelative(source, closure, tick.relativeX(), tick.relativeY(), tick.relativeZ())) {
                targetBlockTicks.add(tick);
            }
        }
        for (RegionSnapshot.TickRecord tick : sourceState.blockTicks()) {
            BlockPos transformed = transformBlock(tick.relativeX(), tick.relativeY(), tick.relativeZ(),
                    sourceWidth, sourceDepth, rotation);
            targetBlockTicks.add(new RegionSnapshot.TickRecord(xOffset + transformed.getX(),
                    yOffset + transformed.getY(), zOffset + transformed.getZ(), tick.typeId(),
                    tick.delay(), tick.priority()));
        }
        var targetFluidTicks = new ArrayList<RegionSnapshot.TickRecord>();
        for (RegionSnapshot.TickRecord tick : current.fluidTicks()) {
            if (!containsRelative(source, closure, tick.relativeX(), tick.relativeY(), tick.relativeZ())) {
                targetFluidTicks.add(tick);
            }
        }
        for (RegionSnapshot.TickRecord tick : sourceState.fluidTicks()) {
            BlockPos transformed = transformBlock(tick.relativeX(), tick.relativeY(), tick.relativeZ(),
                    sourceWidth, sourceDepth, rotation);
            targetFluidTicks.add(new RegionSnapshot.TickRecord(xOffset + transformed.getX(),
                    yOffset + transformed.getY(), zOffset + transformed.getZ(), tick.typeId(),
                    tick.delay(), tick.priority()));
        }
        var targetEntities = new ArrayList<RegionSnapshot.EntityRecord>();
        for (RegionSnapshot.EntityRecord record : current.entities()) {
            Vec3 position = entityPosition(record.data());
            if (destinationContains(destination, position) && !sourceContains(source, position)) {
                throw new IllegalArgumentException("destination region contains an entity");
            }
            if (!sourceContains(source, position)) {
                targetEntities.add(record);
            }
        }
        for (RegionSnapshot.EntityRecord record : sourceState.entities()) {
            var data = record.data();
            transformEntityTree(data, source.min(), destination.min(),
                    sourceWidth, sourceDepth, rotation);
            targetEntities.add(new RegionSnapshot.EntityRecord(data));
        }
        SnapshotService.SnapshotState target = new SnapshotService.SnapshotState(
                targetBlocks, targetBlockTicks, targetFluidTicks, targetEntities, current.biomes());
        target = normalizeBlockStates(level, closure, target);
        RegionSnapshot currentSnapshot = new RegionSnapshot(RegionSnapshot.CURRENT_SCHEMA_VERSION,
                new SnapshotId(UUID.randomUUID(), "move_before"), ownerId, Instant.now(), closure,
                current.blocks(), current.blockTicks(), current.fluidTicks(), current.entities(), current.biomes(),
                SnapshotService.digest(current));
        RegionSnapshot targetSnapshot = new RegionSnapshot(RegionSnapshot.CURRENT_SCHEMA_VERSION,
                new SnapshotId(UUID.randomUUID(), "flying_house"), ownerId, Instant.now(), closure,
                target.blocks(), target.blockTicks(), target.fluidTicks(), target.entities(), target.biomes(),
                SnapshotService.digest(target));
        return new MovePlan(currentSnapshot, targetSnapshot, source, offset, rotation);
    }

    private static boolean swapsHorizontalAxes(Rotation rotation) {
        return rotation == Rotation.CLOCKWISE_90 || rotation == Rotation.COUNTERCLOCKWISE_90;
    }

    private static BlockPos transformBlock(int x, int y, int z, int width, int depth,
                                           Rotation rotation) {
        return switch (rotation) {
            case NONE -> new BlockPos(x, y, z);
            case CLOCKWISE_90 -> new BlockPos(depth - 1 - z, y, x);
            case CLOCKWISE_180 -> new BlockPos(width - 1 - x, y, depth - 1 - z);
            case COUNTERCLOCKWISE_90 -> new BlockPos(z, y, width - 1 - x);
        };
    }

    private static Vec3 transformPoint(Vec3 relative, int width, int depth, Rotation rotation) {
        return switch (rotation) {
            case NONE -> relative;
            case CLOCKWISE_90 -> new Vec3(depth - relative.z, relative.y, relative.x);
            case CLOCKWISE_180 -> new Vec3(width - relative.x, relative.y, depth - relative.z);
            case COUNTERCLOCKWISE_90 -> new Vec3(relative.z, relative.y, width - relative.x);
        };
    }

    private static void transformEntityTree(net.minecraft.nbt.CompoundTag data,
                                            BlockPos sourceMin, BlockPos destinationMin,
                                            int width, int depth, Rotation rotation) {
        data.read("Pos", Vec3.CODEC).ifPresent(position -> {
            Vec3 relative = position.subtract(Vec3.atLowerCornerOf(sourceMin));
            Vec3 transformed = transformPoint(relative, width, depth, rotation)
                    .add(Vec3.atLowerCornerOf(destinationMin));
            data.store("Pos", Vec3.CODEC, transformed);
        });
        data.read("Rotation", Vec2.CODEC).ifPresent(entityRotation ->
                data.store("Rotation", Vec2.CODEC,
                        new Vec2(entityRotation.x + rotationDegrees(rotation), entityRotation.y)));
        for (var tag : data.getListOrEmpty("Passengers")) {
            if (tag instanceof net.minecraft.nbt.CompoundTag passenger) {
                transformEntityTree(passenger, sourceMin, destinationMin, width, depth, rotation);
            }
        }
    }

    private static float rotationDegrees(Rotation rotation) {
        return switch (rotation) {
            case NONE -> 0.0F;
            case CLOCKWISE_90 -> 90.0F;
            case CLOCKWISE_180 -> 180.0F;
            case COUNTERCLOCKWISE_90 -> -90.0F;
        };
    }

    private static Vec3 entityPosition(net.minecraft.nbt.CompoundTag data) {
        return data.read("Pos", Vec3.CODEC)
                .orElseThrow(() -> new IllegalArgumentException("entity snapshot has no position"));
    }

    private static boolean sourceContains(RegionSelection selection, Vec3 position) {
        return AABB.encapsulatingFullBlocks(selection.min(), selection.max()).contains(position);
    }

    private static boolean destinationContains(RegionSelection selection, Vec3 position) {
        return sourceContains(selection, position);
    }

    private static void rejectProtectedEntities(ServerLevel level, RegionSelection source,
                                                RegionSelection destination) {
        AABB sourceBounds = AABB.encapsulatingFullBlocks(source.min(), source.max());
        AABB destinationBounds = AABB.encapsulatingFullBlocks(destination.min(), destination.max());
        for (Entity entity : level.getEntities((Entity) null, sourceBounds.minmax(destinationBounds),
                candidate -> (candidate instanceof Player
                        || candidate instanceof dev.romankrukovsky.kubanhorizons.entity.KubanGenie)
                        && (sourceBounds.contains(candidate.position())
                        || destinationBounds.contains(candidate.position())))) {
            throw new IllegalArgumentException("players and Wishborne cannot be moved with a structure");
        }
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
                           RegionSelection source, BlockPos offset, Rotation rotation) {
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
