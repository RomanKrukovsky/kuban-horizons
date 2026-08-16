package com.kuban.genie.pocket;

import com.kuban.genie.KubanGenie;
import com.kuban.genie.memory.WorldGenieMemory;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.*;

/**
 * Engine for flying structures that can be summoned and controlled by genies.
 * Structures can be anchored, moved, and resized within pocket dimensions.
 */
public class FlyingStructureEngine {

    private static final int MAX_STRUCTURES_PER_SCENE = 10;
    private static final double MAX_STRUCTURE_SIZE = 64.0; // blocks
    private static final double MAX_MOVE_DISTANCE = 128.0; // blocks
    private static final int STRUCTURE_COOLDOWN_TICKS = 20 * 5; // 5 seconds

    private final Map<ResourceKey<Level>, List<FlyingStructure>> structuresByDimension = new HashMap<>();
    private final Map<UUID, Long> lastSummonTime = new HashMap<>();

    /**
     * Create a new flying structure
     */
    public FlyingStructure createStructure(ServerLevel world, BlockPos anchorPos, String name, UUID owner) {
        ResourceKey<Level> dimensionId = world.dimension();
        List<FlyingStructure> structures = structuresByDimension.computeIfAbsent(dimensionId, k -> new ArrayList<>());

        if (structures.size() >= MAX_STRUCTURES_PER_SCENE) {
            KubanGenie.LOGGER.warn("Cannot create more structures in dimension {}", dimensionId.location());
            return null;
        }

        // Check structure size
        double size = calculateStructureSize(world, anchorPos);
        if (size > MAX_STRUCTURE_SIZE) {
            KubanGenie.LOGGER.warn("Structure too large: {} > {}", size, MAX_STRUCTURE_SIZE);
            return null;
        }

        FlyingStructure structure = new FlyingStructure(
            UUID.randomUUID(),
            name,
            owner,
            anchorPos,
            world.dimension(),
            size
        );

        structures.add(structure);
        KubanGenie.LOGGER.info("Created flying structure '{}' at {} in dimension {}",
            name, anchorPos, dimensionId.location());
        return structure;
    }

    /**
     * Summon a structure to follow a player
     */
    public boolean summonStructure(Player player, FlyingStructure structure) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return false;
        }

        // Check cooldown
        Long lastSummoned = lastSummonTime.get(serverPlayer.getUUID());
        if (lastSummoned != null && System.currentTimeMillis() - lastSummoned < STRUCTURE_COOLDOWN_TICKS) {
            return false;
        }

        // Check ownership
        if (!structure.getOwner().equals(serverPlayer.getUUID())) {
            serverPlayer.sendSystemMessage(Component.literal("§cThis is not your structure"));
            return false;
        }

        // Check energy
        WorldGenieMemory memory = KubanGenie.getGenieMemory();
        if (memory.getGenieEnergy(serverPlayer.getUUID()) < 50) {
            serverPlayer.sendSystemMessage(Component.literal("§cNot enough energy to summon structure"));
            return false;
        }

        // Summon the structure
        structure.setSummoned(true);
        structure.setTargetPosition(player.position());
        lastSummonTime.put(serverPlayer.getUUID(), System.currentTimeMillis());

        // Deduct energy
        memory.consumeGenieEnergy(serverPlayer.getUUID(), 50);

        // Record event
        memory.recordEvent(
            serverPlayer.getUUID(),
            "summoned_flying_structure",
            Map.of("structure", structure.getName(), "size", structure.getSize())
        );

        return true;
    }

    /**
     * Move a structure to a new position
     */
    public boolean moveStructure(Player player, FlyingStructure structure, Vec3 newPosition) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return false;
        }

        if (!structure.getOwner().equals(serverPlayer.getUUID())) {
            serverPlayer.sendSystemMessage(Component.literal("§cYou can only move your own structures"));
            return false;
        }

        double distance = player.position().distanceTo(new Vec3(structure.getAnchorPos().getX(), structure.getAnchorPos().getY(), structure.getAnchorPos().getZ()));
        if (distance > MAX_MOVE_DISTANCE) {
            serverPlayer.sendSystemMessage(Component.literal("§cToo far to move this structure"));
            return false;
        }

        structure.setAnchorPos(new BlockPos(newPosition.x, newPosition.y, newPosition.z));
        structure.setTargetPosition(newPosition);

        // Record event
        WorldGenieMemory memory = KubanGenie.getGenieMemory();
        memory.recordEvent(
            serverPlayer.getUUID(),
            "moved_flying_structure",
            Map.of("structure", structure.getName(), "new_pos", newPosition.toString())
        );

        return true;
    }

    /**
     * Resize a structure
     */
    public boolean resizeStructure(Player player, FlyingStructure structure, double newSize) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return false;
        }

        if (!structure.getOwner().equals(serverPlayer.getUUID())) {
            serverPlayer.sendSystemMessage(Component.literal("§cYou can only resize your own structures"));
            return false;
        }

        if (newSize > MAX_STRUCTURE_SIZE) {
            serverPlayer.sendSystemMessage(Component.literal("§cStructure too large"));
            return false;
        }

        structure.setSize(newSize);

        // Record event
        WorldGenieMemory memory = KubanGenie.getGenieMemory();
        memory.recordEvent(
            serverPlayer.getUUID(),
            "resized_flying_structure",
            Map.of("structure", structure.getName(), "new_size", newSize)
        );

        return true;
    }

    /**
     * Anchor a structure to prevent movement
     */
    public boolean anchorStructure(Player player, FlyingStructure structure) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return false;
        }

        if (!structure.getOwner().equals(serverPlayer.getUUID())) {
            serverPlayer.sendSystemMessage(Component.literal("§cYou can only anchor your own structures"));
            return false;
        }

        structure.setAnchored(!structure.isAnchored());

        // Record event
        WorldGenieMemory memory = KubanGenie.getGenieMemory();
        memory.recordEvent(
            serverPlayer.getUUID(),
            "toggled_structure_anchor",
            Map.of("structure", structure.getName(), "anchored", structure.isAnchored())
        );

        return true;
    }

    /**
     * Delete a structure
     */
    public boolean deleteStructure(Player player, FlyingStructure structure) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return false;
        }

        if (!structure.getOwner().equals(serverPlayer.getUUID())) {
            serverPlayer.sendSystemMessage(Component.literal("§cYou can only delete your own structures"));
            return false;
        }

        ResourceKey<Level> dimensionId = structure.getDimension();
        List<FlyingStructure> structures = structuresByDimension.get(dimensionId);
        if (structures != null) {
            structures.remove(structure);
        }

        // Record event
        WorldGenieMemory memory = KubanGenie.getGenieMemory();
        memory.recordEvent(
            serverPlayer.getUUID(),
            "deleted_flying_structure",
            Map.of("structure", structure.getName())
        );

        return true;
    }

    /**
     * Update all structures - move summoned ones toward targets
     */
    public void updateStructures(ServerLevel world) {
        ResourceKey<Level> dimensionId = world.dimension();
        List<FlyingStructure> structures = structuresByDimension.get(dimensionId);

        if (structures == null) {
            return;
        }

        for (FlyingStructure structure : structures) {
            if (structure.isSummoned() && !structure.isAnchored()) {
                Vec3 currentPos = new Vec3(
                    structure.getAnchorPos().getX() + 0.5,
                    structure.getAnchorPos().getY() + 0.5,
                    structure.getAnchorPos().getZ() + 0.5
                );

                Vec3 targetPos = structure.getTargetPosition();
                double distance = currentPos.distanceTo(targetPos);

                if (distance > 0.5) {
                    // Move toward target
                    Vec3 direction = targetPos.subtract(currentPos).normalize().scale(0.2);
                    BlockPos newPos = new BlockPos(
                        currentPos.x + direction.x,
                        currentPos.y + direction.y,
                        currentPos.z + direction.z
                    );

                    structure.setAnchorPos(newPos);
                }
            }
        }
    }

    /**
     * Get all structures in a dimension
     */
    public List<FlyingStructure> getStructuresInDimension(ResourceKey<Level> dimensionId) {
        return structuresByDimension.getOrDefault(dimensionId, Collections.emptyList());
    }

    /**
     * Get a structure by ID
     */
    @Nullable
    public FlyingStructure getStructureById(UUID structureId) {
        for (List<FlyingStructure> structures : structuresByDimension.values()) {
            for (FlyingStructure structure : structures) {
                if (structure.getId().equals(structureId)) {
                    return structure;
                }
            }
        }
        return null;
    }

    /**
     * Check if position is part of any structure
     */
    public boolean isStructureBlock(BlockPos pos) {
        for (List<FlyingStructure> structures : structuresByDimension.values()) {
            for (FlyingStructure structure : structures) {
                if (structure.getBoundingBox().contains(pos)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Get structure statistics
     */
    public Map<String, Integer> getStatistics() {
        Map<String, Integer> stats = new HashMap<>();
        int totalStructures = 0;
        int summonedStructures = 0;
        int anchoredStructures = 0;

        for (List<FlyingStructure> structures : structuresByDimension.values()) {
            totalStructures += structures.size();
            summonedStructures += (int) structures.stream().filter(FlyingStructure::isSummoned).count();
            anchoredStructures += (int) structures.stream().filter(FlyingStructure::isAnchored).count();
        }

        stats.put("total_structures", totalStructures);
        stats.put("max_limit", MAX_STRUCTURES_PER_SCENE);
        stats.put("summoned_structures", summonedStructures);
        stats.put("anchored_structures", anchoredStructures);
        return stats;
    }

    /**
     * Calculate structure size by scanning blocks
     */
    private double calculateStructureSize(ServerLevel world, BlockPos anchorPos) {
        AABB scanArea = new AABB(anchorPos).inflate(8);
        List<BlockState> blocks = new ArrayList<>();

        for (BlockPos pos : BlockPos.betweenClosed((int) scanArea.minX, (int) scanArea.minY, (int) scanArea.minZ,
                (int) scanArea.maxX, (int) scanArea.maxY, (int) scanArea.maxZ)) {
            BlockState state = world.getBlockState(pos);
            if (!state.isAir()) {
                blocks.add(state);
            }
        }

        return blocks.size() * 0.1; // Approximate size metric
    }

    /**
     * Save all structures to NBT
     */
    public CompoundTag saveAllStructures() {
        CompoundTag tag = new CompoundTag();
        int index = 0;

        for (List<FlyingStructure> structures : structuresByDimension.values()) {
            for (FlyingStructure structure : structures) {
                CompoundTag structureTag = new CompoundTag();
                structure.save(structureTag);
                tag.put("structure_" + index++, structureTag);
            }
        }

        tag.putInt("count", structuresByDimension.values().stream().mapToInt(List::size).sum());
        return tag;
    }

    /**
     * Load structures from NBT
     */
    public void loadAllStructures(CompoundTag tag) {
        structuresByDimension.clear();
        int count = tag.getInt("count");

        for (int i = 0; i < count; i++) {
            CompoundTag structureTag = tag.getCompound("structure_" + i);
            FlyingStructure structure = new FlyingStructure();
            structure.load(structureTag);

            structuresByDimension.computeIfAbsent(structure.getDimension(), k -> new ArrayList<>()).add(structure);
        }
    }
}
