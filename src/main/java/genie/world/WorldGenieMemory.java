package genie.world;

import genie.genie.KubanGenie;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Persistent memory system for Kuban Genie.
 * Tracks events, wishes, and genie states across game sessions.
 */
public class WorldGenieMemory extends SavedData {

    private static final String DATA_NAME = "KubanGenieMemory";

    private final Map<UUID, GenieMemoryEntry> genieMemory = new HashMap<>();
    private final List<WishEvent> wishEvents = new ArrayList<>();
    private final List<BlockEvent> blockEvents = new ArrayList<>();

    public WorldGenieMemory() {
        super();
    }

    /**
     * Record a wish event
     */
    public void recordWish(UUID genieId, UUID playerId, String wishText, BlockPos location, boolean success) {
        WishEvent event = new WishEvent(genieId, playerId, wishText, location, success);
        wishEvents.add(event);
        this.setDirty();

        // Update genie memory
        GenieMemoryEntry entry = genieMemory.computeIfAbsent(genieId, GenieMemoryEntry::new);
        entry.addWish(wishText, success);
    }

    /**
     * Record a block event
     */
    public void recordBlockEvent(BlockPos pos, String blockType, String action) {
        BlockEvent event = new BlockEvent(pos, blockType, action);
        blockEvents.add(event);
        this.setDirty();
    }

    /**
     * Record genie state
     */
    public void recordGenieState(UUID genieId, String state, String aura, int power) {
        GenieMemoryEntry entry = genieMemory.computeIfAbsent(genieId, GenieMemoryEntry::new);
        entry.setState(state);
        entry.setEmotionalAura(aura);
        entry.setWishPower(power);
        this.setDirty();
    }

    /**
     * Get genie memory entry
     */
    @Nullable
    public GenieMemoryEntry getGenieMemory(UUID genieId) {
        return genieMemory.get(genieId);
    }

    /**
     * Get wish events for a genie
     */
    public List<WishEvent> getWishEventsForGenie(UUID genieId) {
        return wishEvents.stream()
            .filter(event -> event.genieId.equals(genieId))
            .toList();
    }

    /**
     * Get recent wishes (last 100)
     */
    public List<WishEvent> getRecentWishes(int limit) {
        int start = Math.max(0, wishEvents.size() - limit);
        return new ArrayList<>(wishEvents.subList(start, wishEvents.size()));
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        // Save genie memory
        ListTag genieMemoryTag = new ListTag();
        for (GenieMemoryEntry entry : genieMemory.values()) {
            genieMemoryTag.add(entry.saveToNBT());
        }
        tag.put("genie_memory", genieMemoryTag);

        // Save wish events
        ListTag wishEventsTag = new ListTag();
        for (WishEvent event : wishEvents) {
            wishEventsTag.add(event.saveToNBT());
        }
        tag.put("wish_events", wishEventsTag);

        // Save block events
        ListTag blockEventsTag = new ListTag();
        for (BlockEvent event : blockEvents) {
            blockEventsTag.add(event.saveToNBT());
        }
        tag.put("block_events", blockEventsTag);

        return tag;
    }

    public static WorldGenieMemory load(CompoundTag tag) {
        WorldGenieMemory memory = new WorldGenieMemory();

        // Load genie memory
        ListTag genieMemoryTag = tag.getList("genie_memory", 10);
        for (int i = 0; i < genieMemoryTag.size(); i++) {
            CompoundTag entryTag = genieMemoryTag.getCompound(i);
            GenieMemoryEntry entry = GenieMemoryEntry.loadFromNBT(entryTag);
            if (entry != null) {
                memory.genieMemory.put(entry.genieId, entry);
            }
        }

        // Load wish events
        ListTag wishEventsTag = tag.getList("wish_events", 10);
        for (int i = 0; i < wishEventsTag.size(); i++) {
            CompoundTag eventTag = wishEventsTag.getCompound(i);
            WishEvent event = WishEvent.loadFromNBT(eventTag);
            if (event != null) {
                memory.wishEvents.add(event);
            }
        }

        // Load block events
        ListTag blockEventsTag = tag.getList("block_events", 10);
        for (int i = 0; i < blockEventsTag.size(); i++) {
            CompoundTag eventTag = blockEventsTag.getCompound(i);
            BlockEvent event = BlockEvent.loadFromNBT(eventTag);
            if (event != null) {
                memory.blockEvents.add(event);
            }
        }

        return memory;
    }

    /**
     * Get the global genie memory for a world
     */
    public static WorldGenieMemory get(Level level) {
        if (level instanceof ServerLevel serverLevel) {
            return serverLevel.getDataStorage().computeIfAbsent(
                WorldGenieMemory::load,
                WorldGenieMemory::new,
                DATA_NAME
            );
        }
        return new WorldGenieMemory();
    }

    /**
     * Genie memory entry
     */
    public static class GenieMemoryEntry {
        public final UUID genieId;
        private String state = "MANIFESTED";
        private String emotionalAura = "none";
        private int wishPower = 100;
        private int totalWishes = 0;
        private int successfulWishes = 0;
        private final List<String> wishHistory = new ArrayList<>();

        public GenieMemoryEntry(UUID genieId) {
            this.genieId = genieId;
        }

        public void addWish(String wishText, boolean success) {
            totalWishes++;
            if (success) successfulWishes++;
            wishHistory.add(wishText);
            if (wishHistory.size() > 50) {
                wishHistory.remove(0);
            }
        }

        public void setState(String state) {
            this.state = state;
        }

        public void setEmotionalAura(String aura) {
            this.emotionalAura = aura;
        }

        public void setWishPower(int power) {
            this.wishPower = power;
        }

        public CompoundTag saveToNBT() {
            CompoundTag tag = new CompoundTag();
            tag.putUUID("genie_id", genieId);
            tag.putString("state", state);
            tag.putString("emotional_aura", emotionalAura);
            tag.putInt("wish_power", wishPower);
            tag.putInt("total_wishes", totalWishes);
            tag.putInt("successful_wishes", successfulWishes);

            ListTag wishHistoryTag = new ListTag();
            for (String wish : wishHistory) {
                wishHistoryTag.add(CompoundTag.stringTag(wish));
            }
            tag.put("wish_history", wishHistoryTag);

            return tag;
        }

        @Nullable
        public static GenieMemoryEntry loadFromNBT(CompoundTag tag) {
            try {
                UUID genieId = tag.getUUID("genie_id");
                GenieMemoryEntry entry = new GenieMemoryEntry(genieId);

                entry.state = tag.getString("state");
                entry.emotionalAura = tag.getString("emotional_aura");
                entry.wishPower = tag.getInt("wish_power");
                entry.totalWishes = tag.getInt("total_wishes");
                entry.successfulWishes = tag.getInt("successful_wishes");

                ListTag wishHistoryTag = tag.getList("wish_history", 8);
                for (int i = 0; i < wishHistoryTag.size(); i++) {
                    entry.wishHistory.add(wishHistoryTag.getString(i));
                }

                return entry;
            } catch (Exception e) {
                return null;
            }
        }
    }

    /**
     * Wish event record
     */
    public static class WishEvent {
        public final UUID genieId;
        public final UUID playerId;
        public final String wishText;
        public final BlockPos location;
        public final boolean success;
        public final long timestamp;

        public WishEvent(UUID genieId, UUID playerId, String wishText, BlockPos location, boolean success) {
            this.genieId = genieId;
            this.playerId = playerId;
            this.wishText = wishText;
            this.location = location;
            this.success = success;
            this.timestamp = System.currentTimeMillis();
        }

        public CompoundTag saveToNBT() {
            CompoundTag tag = new CompoundTag();
            tag.putUUID("genie_id", genieId);
            tag.putUUID("player_id", playerId);
            tag.putString("wish_text", wishText);
            tag.putInt("x", location.getX());
            tag.putInt("y", location.getY());
            tag.putInt("z", location.getZ());
            tag.putBoolean("success", success);
            tag.putLong("timestamp", timestamp);
            return tag;
        }

        @Nullable
        public static WishEvent loadFromNBT(CompoundTag tag) {
            try {
                UUID genieId = tag.getUUID("genie_id");
                UUID playerId = tag.getUUID("player_id");
                String wishText = tag.getString("wish_text");
                int x = tag.getInt("x");
                int y = tag.getInt("y");
                int z = tag.getInt("z");
                boolean success = tag.getBoolean("success");
                long timestamp = tag.getLong("timestamp");

                return new WishEvent(genieId, playerId, wishText, new BlockPos(x, y, z), success);
            } catch (Exception e) {
                return null;
            }
        }
    }

    /**
     * Block event record
     */
    public static class BlockEvent {
        public final BlockPos position;
        public final String blockType;
        public final String action;
        public final long timestamp;

        public BlockEvent(BlockPos position, String blockType, String action) {
            this.position = position;
            this.blockType = blockType;
            this.action = action;
            this.timestamp = System.currentTimeMillis();
        }

        public CompoundTag saveToNBT() {
            CompoundTag tag = new CompoundTag();
            tag.putInt("x", position.getX());
            tag.putInt("y", position.getY());
            tag.putInt("z", position.getZ());
            tag.putString("block_type", blockType);
            tag.putString("action", action);
            tag.putLong("timestamp", timestamp);
            return tag;
        }

        @Nullable
        public static BlockEvent loadFromNBT(CompoundTag tag) {
            try {
                int x = tag.getInt("x");
                int y = tag.getInt("y");
                int z = tag.getInt("z");
                String blockType = tag.getString("block_type");
                String action = tag.getString("action");
                long timestamp = tag.getLong("timestamp");

                return new BlockEvent(new BlockPos(x, y, z), blockType, action);
            } catch (Exception e) {
                return null;
            }
        }
    }
}
