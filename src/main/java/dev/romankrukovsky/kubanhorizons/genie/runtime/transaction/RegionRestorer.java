package dev.romankrukovsky.kubanhorizons.genie.runtime.transaction;

import dev.romankrukovsky.kubanhorizons.genie.runtime.snapshot.RegionSnapshot;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.ticks.ScheduledTick;
import net.minecraft.world.ticks.TickPriority;

/** Minecraft-адаптер блоков и block entity для restore и recovery. */
public final class RegionRestorer {
    private static final int RESTORE_FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE
            | Block.UPDATE_SUPPRESS_DROPS | Block.UPDATE_SKIP_BLOCK_ENTITY_SIDEEFFECTS;

    private RegionRestorer() {
    }

    public static void apply(ServerLevel level, RegionSnapshot snapshot) throws IOException {
        var blocks = level.registryAccess().lookupOrThrow(Registries.BLOCK);
        BlockPos origin = snapshot.selection().min();
        AABB bounds = AABB.encapsulatingFullBlocks(snapshot.selection().min(), snapshot.selection().max());
        for (Entity entity : level.getEntities((Entity) null, bounds, RegionRestorer::eligibleEntity)) {
            entity.discard();
        }
        BoundingBox tickBounds = BoundingBox.fromCorners(snapshot.selection().min(), snapshot.selection().max());
        level.getBlockTicks().clearArea(tickBounds);
        level.getFluidTicks().clearArea(tickBounds);

        for (RegionSnapshot.BlockRecord record : snapshot.blocks()) {
            BlockPos pos = origin.offset(record.relativeX(), record.relativeY(), record.relativeZ());
            level.removeBlockEntity(pos);
        }
        for (RegionSnapshot.BlockRecord record : snapshot.blocks()) {
            BlockPos pos = origin.offset(record.relativeX(), record.relativeY(), record.relativeZ());
            level.setBlock(pos, NbtUtils.readBlockState(blocks, record.blockState()), RESTORE_FLAGS);
        }
        for (RegionSnapshot.BlockRecord record : snapshot.blocks()) {
            if (record.blockEntity() == null) {
                continue;
            }
            BlockPos pos = origin.offset(record.relativeX(), record.relativeY(), record.relativeZ());
            BlockEntity restored = BlockEntity.loadStatic(pos, level.getBlockState(pos),
                    record.blockEntity(), level.registryAccess());
            if (restored == null) {
                throw new IOException("failed to restore block entity at " + pos);
            }
            level.removeBlockEntity(pos);
            level.setBlockEntity(restored);
            restored.setChanged();
        }
        restoreTicks(level, snapshot, origin);
        restoreEntities(level, snapshot);
        for (RegionSnapshot.BlockRecord record : snapshot.blocks()) {
            BlockPos pos = origin.offset(record.relativeX(), record.relativeY(), record.relativeZ());
            level.updateNeighborsAt(pos, level.getBlockState(pos).getBlock());
        }
    }

    private static void restoreTicks(ServerLevel level, RegionSnapshot snapshot,
                                     BlockPos origin) throws IOException {
        long now = level.getGameTime();
        long subOrder = 0L;
        for (RegionSnapshot.TickRecord tick : snapshot.blockTicks()) {
            Identifier id = Identifier.tryParse(tick.typeId());
            if (id == null || !BuiltInRegistries.BLOCK.containsKey(id)) {
                throw new IOException("unknown scheduled block type " + tick.typeId());
            }
            level.getBlockTicks().schedule(new ScheduledTick<>(BuiltInRegistries.BLOCK.getValue(id),
                    origin.offset(tick.relativeX(), tick.relativeY(), tick.relativeZ()),
                    now + tick.delay(), TickPriority.byValue(tick.priority()), subOrder++));
        }
        for (RegionSnapshot.TickRecord tick : snapshot.fluidTicks()) {
            Identifier id = Identifier.tryParse(tick.typeId());
            if (id == null || !BuiltInRegistries.FLUID.containsKey(id)) {
                throw new IOException("unknown scheduled fluid type " + tick.typeId());
            }
            level.getFluidTicks().schedule(new ScheduledTick<>(BuiltInRegistries.FLUID.getValue(id),
                    origin.offset(tick.relativeX(), tick.relativeY(), tick.relativeZ()),
                    now + tick.delay(), TickPriority.byValue(tick.priority()), subOrder++));
        }
    }

    private static void restoreEntities(ServerLevel level, RegionSnapshot snapshot) throws IOException {
        Set<UUID> restoredIds = new HashSet<>();
        for (RegionSnapshot.EntityRecord record : snapshot.entities()) {
            Entity root = EntityType.loadEntityRecursive(record.data(), level,
                    new net.minecraft.world.entity.EntitySpawnRequest(EntitySpawnReason.LOAD, false), entity -> entity);
            if (root == null) {
                throw new IOException("failed to restore entity tree");
            }
            for (Entity entity : root.getSelfAndPassengers().toList()) {
                UUID id = entity.getUUID();
                Entity collision = level.getEntity(id);
                if (collision != null || !restoredIds.add(id)) {
                    UUID remapped;
                    do {
                        remapped = UUID.randomUUID();
                    } while (level.getEntity(remapped) != null || !restoredIds.add(remapped));
                    entity.setUUID(remapped);
                }
            }
            level.addFreshEntityWithPassengers(root);
            if (root.isRemoved()) {
                throw new IOException("restored entity was rejected by the level");
            }
        }
    }

    private static boolean eligibleEntity(Entity entity) {
        return !(entity instanceof Player)
                && !(entity instanceof dev.romankrukovsky.kubanhorizons.entity.KubanGenie)
                && entity.getEncodeId() != null;
    }
}
