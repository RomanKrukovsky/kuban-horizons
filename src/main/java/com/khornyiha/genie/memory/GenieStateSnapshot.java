package com.khornyiha.genie.memory;

import com.khornyiha.genie.KubanGenie;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.*;
import java.util.stream.Collectors;

/**
 * State capture system for Genie system.
 * Captures and restores entity states, block states, and world conditions.
 */
public class GenieStateSnapshot {

    private final long timestamp;
    private final UUID snapshotId;
    private final UUID playerId;
    private final String playerName;

    // Entity states
    private final Map<UUID, EntityState> entityStates = new HashMap<>();

    // Block states
    private final Map<BlockPos, BlockStateSnapshot> blockStates = new HashMap<>();

    // World state
    private final Map<String, String> worldState = new HashMap<>();

    // Inventory
    private final List<ItemStackSnapshot> inventory = new ArrayList<>();

    // Genie-specific state
    private final Map<String, String> genieState = new HashMap<>();

    public GenieStateSnapshot(UUID playerId, String playerName) {
        this.timestamp = System.currentTimeMillis();
        this.snapshotId = UUID.randomUUID();
        this.playerId = playerId;
        this.playerName = playerName;
    }

    /**
     * Create a snapshot from player
     */
    public static GenieStateSnapshot captureFromPlayer(Player player) {
        GenieStateSnapshot snapshot = new GenieStateSnapshot(
            player.getUUID(),
            player.getName().getString()
        );

        // Capture player position and rotation
        snapshot.worldState.put("position",
            player.getX() + "," + player.getY() + "," + player.getZ());
        snapshot.worldState.put("rotation",
            player.getYRot() + "," + player.getXRot());
        snapshot.worldState.put("dimension", player.level().dimension().location().toString());

        // Capture inventory
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (!stack.isEmpty()) {
                snapshot.inventory.add(new ItemStackSnapshot(stack, i));
            }
        }

        // Capture entity states
        for (Entity entity : player.level().entitiesForRendering()) {
            if (entity instanceof Player otherPlayer && !otherPlayer.getUUID().equals(player.getUUID())) {
                snapshot.entityStates.put(
                    entity.getUUID(),
                    EntityState.capture(entity)
                );
            }
        }

        // Capture block states in vicinity
        BlockPos playerPos = player.blockPosition();
        int radius = 8;
        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    BlockPos pos = playerPos.offset(x, y, z);
                    BlockState state = player.level().getBlockState(pos);
                    if (!state.isAir()) {
                        snapshot.blockStates.put(pos, new BlockStateSnapshot(pos, state));
                    }
                }
            }
        }

        return snapshot;
    }

    /**
     * Restore snapshot to player
     */
    public void restoreToPlayer(Player player, boolean restorePosition) {
        if (restorePosition) {
            String[] posParts = worldState.get("position").split(",");
            if (posParts.length == 3) {
                try {
                    double x = Double.parseDouble(posParts[0]);
                    double y = Double.parseDouble(posParts[1]);
                    double z = Double.parseDouble(posParts[2]);
                    player.teleportTo(x, y, z);
                } catch (Exception e) {
                    KubanGenie.LOGGER.error("Failed to restore position: " + e.getMessage());
                }
            }

            String[] rotParts = worldState.get("rotation").split(",");
            if (rotParts.length == 2) {
                try {
                    float yRot = Float.parseFloat(rotParts[0]);
                    float xRot = Float.parseFloat(rotParts[1]);
                    player.setYRot(yRot);
                    player.setXRot(xRot);
                } catch (Exception e) {
                    KubanGenie.LOGGER.error("Failed to restore rotation: " + e.getMessage());
                }
            }
        }

        // Restore inventory
        player.getInventory().clearContent();
        for (ItemStackSnapshot snapshot : inventory) {
            player.getInventory().setItem(snapshot.getSlot(), snapshot.createItemStack());
        }

        // Restore entity states
        for (Map.Entry<UUID, EntityState> entry : entityStates.entrySet()) {
            Entity entity = player.level().getEntity(entry.getKey());
            if (entity != null) {
                entry.getValue().restore(entity);
            }
        }

        // Restore block states
        for (BlockStateSnapshot snapshot : blockStates.values()) {
            player.level().setBlock(snapshot.getPosition(), snapshot.getState(), 3);
        }
    }

    /**
     * Get snapshot as NBT
     */
    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();

        tag.putLong("timestamp", timestamp);
        tag.putUuid("snapshotId", snapshotId);
        tag.putUuid("playerId", playerId);
        tag.putString("playerName", playerName);

        // Save world state
        CompoundTag worldStateTag = new CompoundTag();
        worldState.forEach(worldStateTag::putString);
        tag.put("worldState", worldStateTag);

        // Save entity states
        ListTag entityStatesTag = new ListTag();
        for (EntityState state : entityStates.values()) {
            entityStatesTag.add(state.save());
        }
        tag.put("entityStates", entityStatesTag);

        // Save block states
        ListTag blockStatesTag = new ListTag();
        for (BlockStateSnapshot snapshot : blockStates.values()) {
            blockStatesTag.add(snapshot.save());
        }
        tag.put("blockStates", blockStatesTag);

        // Save inventory
        ListTag inventoryTag = new ListTag();
        for (ItemStackSnapshot snapshot : inventory) {
            inventoryTag.add(snapshot.save());
        }
        tag.put("inventory", inventoryTag);

        // Save genie state
        CompoundTag genieStateTag = new CompoundTag();
        genieState.forEach(genieStateTag::putString);
        tag.put("genieState", genieStateTag);

        return tag;
    }

    /**
     * Load snapshot from NBT
     */
    public static GenieStateSnapshot load(CompoundTag tag) {
        GenieStateSnapshot snapshot = new GenieStateSnapshot(
            tag.getUuid("playerId"),
            tag.getString("playerName")
        );

        snapshot.timestamp = tag.getLong("timestamp");
        snapshot.snapshotId = tag.getUuid("snapshotId");

        // Load world state
        CompoundTag worldStateTag = tag.getCompound("worldState");
        worldStateTag.getAllKeys().forEach(key ->
            snapshot.worldState.put(key, worldStateTag.getString(key))
        );

        // Load entity states
        ListTag entityStatesTag = tag.getList("entityStates", Tag.TAG_COMPOUND);
        for (Tag entityTag : entityStatesTag) {
            EntityState state = EntityState.load((CompoundTag) entityTag);
            snapshot.entityStates.put(state.getEntityId(), state);
        }

        // Load block states
        ListTag blockStatesTag = tag.getList("blockStates", Tag.TAG_COMPOUND);
        for (Tag blockTag : blockStatesTag) {
            BlockStateSnapshot snapshotBlock = BlockStateSnapshot.load((CompoundTag) blockTag);
            snapshot.blockStates.put(snapshotBlock.getPosition(), snapshotBlock);
        }

        // Load inventory
        ListTag inventoryTag = tag.getList("inventory", Tag.TAG_COMPOUND);
        for (Tag itemTag : inventoryTag) {
            snapshot.inventory.add(ItemStackSnapshot.load((CompoundTag) itemTag));
        }

        // Load genie state
        CompoundTag genieStateTag = tag.getCompound("genieState");
        genieStateTag.getAllKeys().forEach(key ->
            snapshot.genieState.put(key, genieStateTag.getString(key))
        );

        return snapshot;
    }

    /**
     * Add genie-specific state
     */
    public void addGenieState(String key, String value) {
        genieState.put(key, value);
    }

    /**
     * Get genie state
     */
    public String getGenieState(String key) {
        return genieState.get(key);
    }

    // Getters
    public long getTimestamp() {
        return timestamp;
    }

    public UUID getSnapshotId() {
        return snapshotId;
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public String getPlayerName() {
        return playerName;
    }

    public Map<UUID, EntityState> getEntityStates() {
        return entityStates;
    }

    public Map<BlockPos, BlockStateSnapshot> getBlockStates() {
        return blockStates;
    }

    public List<ItemStackSnapshot> getInventory() {
        return inventory;
    }

    public Map<String, String> getWorldState() {
        return worldState;
    }

    /**
     * Entity state snapshot
     */
    public static class EntityState {
        private final UUID entityId;
        private final String entityType;
        private final double x, y, z;
        private final float yRot, xRot;
        private final CompoundTag entityData;

        public EntityState(UUID entityId, String entityType, double x, double y, double z,
                          float yRot, float xRot, CompoundTag entityData) {
            this.entityId = entityId;
            this.entityType = entityType;
            this.x = x;
            this.y = y;
            this.z = z;
            this.yRot = yRot;
            this.xRot = xRot;
            this.entityData = entityData;
        }

        public static EntityState capture(Entity entity) {
            CompoundTag data = new CompoundTag();
            entity.saveWithoutId(data);
            return new EntityState(
                entity.getUUID(),
                entity.getType().toString(),
                entity.getX(),
                entity.getY(),
                entity.getZ(),
                entity.getYRot(),
                entity.getXRot(),
                data
            );
        }

        public void restore(Entity entity) {
            entity.setPos(x, y, z);
            entity.setYRot(yRot);
            entity.setXRot(xRot);
            entity.load(entityData);
        }

        public CompoundTag save() {
            CompoundTag tag = new CompoundTag();
            tag.putUuid("entityId", entityId);
            tag.putString("entityType", entityType);
            tag.putDouble("x", x);
            tag.putDouble("y", y);
            tag.putDouble("z", z);
            tag.putFloat("yRot", yRot);
            tag.putFloat("xRot", xRot);
            tag.put("entityData", entityData);
            return tag;
        }

        public static EntityState load(CompoundTag tag) {
            return new EntityState(
                tag.getUuid("entityId"),
                tag.getString("entityType"),
                tag.getDouble("x"),
                tag.getDouble("y"),
                tag.getDouble("z"),
                tag.getFloat("yRot"),
                tag.getFloat("xRot"),
                tag.getCompound("entityData")
            );
        }

        public UUID getEntityId() {
            return entityId;
        }
    }

    /**
     * Block state snapshot
     */
    public static class BlockStateSnapshot {
        private final BlockPos position;
        private final BlockState state;

        public BlockStateSnapshot(BlockPos position, BlockState state) {
            this.position = position;
            this.state = state;
        }

        public CompoundTag save() {
            CompoundTag tag = new CompoundTag();
            tag.putLong("position", position.asLong());
            tag.putString("blockState", state.toString());
            return tag;
        }

        public static BlockStateSnapshot load(CompoundTag tag) {
            BlockPos pos = BlockPos.of(tag.getLong("position"));
            // Note: BlockState serialization requires more complex handling
            // This is simplified for the snapshot system
            return new BlockStateSnapshot(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
        }

        public BlockPos getPosition() {
            return position;
        }

        public BlockState getState() {
            return state;
        }
    }

    /**
     * Item stack snapshot
     */
    public static class ItemStackSnapshot {
        private final int slot;
        private final CompoundTag itemData;

        public ItemStackSnapshot(ItemStack stack, int slot) {
            this.slot = slot;
            this.itemData = new CompoundTag();
            stack.save(itemData);
        }

        public ItemStack createItemStack() {
            return ItemStack.of(itemData);
        }

        public CompoundTag save() {
            CompoundTag tag = new CompoundTag();
            tag.putInt("slot", slot);
            tag.put("itemData", itemData);
            return tag;
        }

        public static ItemStackSnapshot load(CompoundTag tag) {
            int slot = tag.getInt("slot");
            CompoundTag itemData = tag.getCompound("itemData");
            ItemStack stack = ItemStack.of(itemData);
            return new ItemStackSnapshot(stack, slot);
        }

        public int getSlot() {
            return slot;
        }
    }
}
