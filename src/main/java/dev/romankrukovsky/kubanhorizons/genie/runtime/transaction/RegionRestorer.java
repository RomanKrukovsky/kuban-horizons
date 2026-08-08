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
import net.minecraft.network.protocol.game.ClientboundChunksBiomesPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
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
        restoreBiomes(level, snapshot);
        for (RegionSnapshot.BlockRecord record : snapshot.blocks()) {
            BlockPos pos = origin.offset(record.relativeX(), record.relativeY(), record.relativeZ());
            level.updateNeighborsAt(pos, level.getBlockState(pos).getBlock());
        }
    }

    private static void restoreBiomes(ServerLevel level, RegionSnapshot snapshot) throws IOException {
        if (snapshot.biomes().isEmpty()) {
            return;
        }
        var biomeRegistry = level.registryAccess().lookupOrThrow(Registries.BIOME);
        java.util.Map<Long, java.util.Map<QuartCell, net.minecraft.core.Holder<net.minecraft.world.level.biome.Biome>>>
                bySection = new java.util.LinkedHashMap<>();
        for (RegionSnapshot.BiomeRecord record : snapshot.biomes()) {
            Identifier id = Identifier.tryParse(record.biomeId());
            if (id == null) {
                throw new IOException("invalid biome id " + record.biomeId());
            }
            var key = net.minecraft.resources.ResourceKey.create(Registries.BIOME, id);
            var holder = biomeRegistry.get(key)
                    .orElseThrow(() -> new IOException("unknown biome " + record.biomeId()));
            long sectionKey = net.minecraft.core.SectionPos.asLong(
                    net.minecraft.core.QuartPos.toSection(record.quartX()),
                    net.minecraft.core.QuartPos.toSection(record.quartY()),
                    net.minecraft.core.QuartPos.toSection(record.quartZ()));
            bySection.computeIfAbsent(sectionKey, ignored -> new java.util.HashMap<>())
                    .put(new QuartCell(record.quartX() & 3, record.quartY() & 3,
                            record.quartZ() & 3), holder);
        }
        java.util.Set<LevelChunk> changed = new java.util.LinkedHashSet<>();
        for (var entry : bySection.entrySet()) {
            var sectionPos = net.minecraft.core.SectionPos.of(entry.getKey());
            LevelChunk chunk = level.getChunk(sectionPos.x(), sectionPos.z());
            int sectionIndex = chunk.getSectionIndexFromSectionY(sectionPos.y());
            if (sectionIndex < 0 || sectionIndex >= chunk.getSections().length) {
                throw new IOException("biome section lies outside chunk height");
            }
            var section = chunk.getSection(sectionIndex);
            var replacements = entry.getValue();
            var existing = new java.util.HashMap<QuartCell,
                    net.minecraft.core.Holder<net.minecraft.world.level.biome.Biome>>();
            for (int x = 0; x < 4; x++) {
                for (int y = 0; y < 4; y++) {
                    for (int z = 0; z < 4; z++) {
                        existing.put(new QuartCell(x, y, z), section.getNoiseBiome(x, y, z));
                    }
                }
            }
            section.fillBiomesFromNoise((quartX, quartY, quartZ, sampler) ->
                    replacements.getOrDefault(new QuartCell(quartX & 3, quartY & 3, quartZ & 3),
                            existing.get(new QuartCell(quartX & 3, quartY & 3, quartZ & 3))),
                    null, net.minecraft.core.QuartPos.fromSection(sectionPos.x()),
                    net.minecraft.core.QuartPos.fromSection(sectionPos.y()),
                    net.minecraft.core.QuartPos.fromSection(sectionPos.z()));
            chunk.markUnsaved();
            changed.add(chunk);
        }
        ClientboundChunksBiomesPacket packet = ClientboundChunksBiomesPacket.forChunks(
                java.util.List.copyOf(changed));
        for (var player : level.players()) {
            player.connection.send(packet);
        }
    }

    private record QuartCell(int x, int y, int z) {
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
