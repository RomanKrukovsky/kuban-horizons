package com.khornyiha.genie.memory;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

import java.util.*;

/**
 * Individual memory entry for WorldGenieMemory.
 * Stores information about events, wishes, contracts, and interactions.
 */
public class GenieMemoryEntry {

    private final UUID playerId;
    private final String eventType;
    private final String message;
    private final long timestamp;
    private final Map<String, String> metadata;

    public GenieMemoryEntry(UUID playerId, String eventType, String message, Map<String, String> metadata) {
        this.playerId = playerId;
        this.eventType = eventType;
        this.message = message;
        this.timestamp = System.currentTimeMillis();
        this.metadata = new HashMap<>(metadata);
    }

    /**
     * Save memory entry to NBT
     */
    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();

        tag.putUuid("playerId", playerId);
        tag.putString("eventType", eventType);
        tag.putString("message", message);
        tag.putLong("timestamp", timestamp);

        // Save metadata
        CompoundTag metadataTag = new CompoundTag();
        metadata.forEach(metadataTag::putString);
        tag.put("metadata", metadataTag);

        return tag;
    }

    /**
     * Load memory entry from NBT
     */
    public static GenieMemoryEntry load(CompoundTag tag) {
        UUID playerId = tag.getUuid("playerId");
        String eventType = tag.getString("eventType");
        String message = tag.getString("message");
        long timestamp = tag.getLong("timestamp");

        GenieMemoryEntry entry = new GenieMemoryEntry(playerId, eventType, message, Collections.emptyMap());
        entry.timestamp = timestamp;

        // Load metadata
        CompoundTag metadataTag = tag.getCompound("metadata");
        metadataTag.getAllKeys().forEach(key ->
            entry.metadata.put(key, metadataTag.getString(key))
        );

        return entry;
    }

    // Getters
    public UUID getPlayerId() {
        return playerId;
    }

    public String getEventType() {
        return eventType;
    }

    public String getMessage() {
        return message;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public Map<String, String> getMetadata() {
        return metadata;
    }

    @Override
    public String toString() {
        return "GenieMemoryEntry{" +
            "playerId=" + playerId +
            ", eventType='" + eventType + '\'' +
            ", message='" + message + '\'' +
            ", timestamp=" + timestamp +
            ", metadata=" + metadata +
            '}';
    }
}
