package com.khornyiha.genie.memory;

import com.khornyiha.genie.KubanGenie;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Persistent event journal for Genie system.
 * Stores all significant events related to genies, wishes, and world interactions.
 * Implements efficient querying and automatic cleanup.
 */
public class WorldGenieMemory extends SavedData {

    private static final String DATA_NAME = "KubanGenieMemory";
    private static final int MAX_EVENTS = 10000;
    private static final int CLEANUP_THRESHOLD = 8000;

    private final Map<UUID, List<GenieMemoryEntry>> playerMemories = new ConcurrentHashMap<>();
    private final Map<String, List<GenieMemoryEntry>> globalEvents = new ArrayList<>();
    private final Map<String, List<GenieMemoryEntry>> wishEvents = new ArrayList<>();
    private final Map<String, List<GenieMemoryEntry>> contractEvents = new ArrayList<>();

    public WorldGenieMemory() {
        super();
    }

    public static WorldGenieMemory get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
            WorldGenieMemory::load,
            WorldGenieMemory::new,
            DATA_NAME
        );
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag playerMemoryList = new ListTag();
        for (Map.Entry<UUID, List<GenieMemoryEntry>> entry : playerMemories.entrySet()) {
            CompoundTag playerTag = new CompoundTag();
            playerTag.putUuid("playerId", entry.getKey());

            ListTag eventsTag = new ListTag();
            for (GenieMemoryEntry event : entry.getValue()) {
                eventsTag.add(event.save());
            }
            playerTag.put("events", eventsTag);
            playerMemoryList.add(playerTag);
        }
        tag.put("playerMemories", playerMemoryList);

        // Save global events
        saveEventList(tag, "globalEvents", globalEvents);
        saveEventList(tag, "wishEvents", wishEvents);
        saveEventList(tag, "contractEvents", contractEvents);

        return tag;
    }

    private void saveEventList(CompoundTag tag, String key, List<GenieMemoryEntry> events) {
        ListTag eventsTag = new ListTag();
        for (GenieMemoryEntry event : events) {
            eventsTag.add(event.save());
        }
        tag.put(key, eventsTag);
    }

    public static WorldGenieMemory load(CompoundTag tag) {
        WorldGenieMemory memory = new WorldGenieMemory();

        // Load player memories
        ListTag playerMemoriesTag = tag.getList("playerMemories", Tag.TAG_COMPOUND);
        for (Tag playerTag : playerMemoriesTag) {
            CompoundTag playerCompound = (CompoundTag) playerTag;
            UUID playerId = playerCompound.getUuid("playerId");

            List<GenieMemoryEntry> events = new ArrayList<>();
            ListTag eventsTag = playerCompound.getList("events", Tag.TAG_COMPOUND);
            for (Tag eventTag : eventsTag) {
                events.add(GenieMemoryEntry.load((CompoundTag) eventTag));
            }

            memory.playerMemories.put(playerId, events);
        }

        // Load global events
        memory.globalEvents.addAll(loadEventList(tag, "globalEvents"));
        memory.wishEvents.addAll(loadEventList(tag, "wishEvents"));
        memory.contractEvents.addAll(loadEventList(tag, "contractEvents"));

        return memory;
    }

    private static List<GenieMemoryEntry> loadEventList(CompoundTag tag, String key) {
        List<GenieMemoryEntry> events = new ArrayList<>();
        ListTag eventsTag = tag.getList(key, Tag.TAG_COMPOUND);
        for (Tag eventTag : eventsTag) {
            events.add(GenieMemoryEntry.load((CompoundTag) eventTag));
        }
        return events;
    }

    /**
     * Record a new memory entry
     */
    public void recordMemory(GenieMemoryEntry entry) {
        // Add to player memories
        playerMemories.computeIfAbsent(entry.getPlayerId(), k -> new ArrayList<>()).add(entry);

        // Add to appropriate category
        switch (entry.getEventType()) {
            case "GLOBAL" -> globalEvents.add(entry);
            case "WISH" -> wishEvents.add(entry);
            case "CONTRACT" -> contractEvents.add(entry);
        }

        // Auto-cleanup if exceeding threshold
        if (globalEvents.size() > CLEANUP_THRESHOLD || wishEvents.size() > CLEANUP_THRESHOLD ||
            contractEvents.size() > CLEANUP_THRESHOLD) {
            cleanupOldEvents();
        }

        setDirty();
    }

    /**
     * Query memories by player
     */
    public List<GenieMemoryEntry> queryPlayerMemories(UUID playerId) {
        return playerMemories.getOrDefault(playerId, Collections.emptyList());
    }

    /**
     * Query memories by event type
     */
    public List<GenieMemoryEntry> queryMemoriesByType(String eventType) {
        return switch (eventType) {
            case "GLOBAL" -> new ArrayList<>(globalEvents);
            case "WISH" -> new ArrayList<>(wishEvents);
            case "CONTRACT" -> new ArrayList<>(contractEvents);
            default -> Collections.emptyList();
        };
    }

    /**
     * Query memories by keyword
     */
    public List<GenieMemoryEntry> queryMemoriesByKeyword(String keyword) {
        String lowerKeyword = keyword.toLowerCase();

        List<GenieMemoryEntry> results = new ArrayList<>();

        // Search player memories
        for (List<GenieMemoryEntry> entries : playerMemories.values()) {
            results.addAll(entries.stream()
                .filter(entry -> entry.getMessage().toLowerCase().contains(lowerKeyword))
                .toList());
        }

        // Search global events
        results.addAll(globalEvents.stream()
            .filter(entry -> entry.getMessage().toLowerCase().contains(lowerKeyword))
            .toList());

        // Search wish events
        results.addAll(wishEvents.stream()
            .filter(entry -> entry.getMessage().toLowerCase().contains(lowerKeyword))
            .toList());

        // Search contract events
        results.addAll(contractEvents.stream()
            .filter(entry -> entry.getMessage().toLowerCase().contains(lowerKeyword))
            .toList());

        return results;
    }

    /**
     * Cleanup oldest events when exceeding maximum
     */
    private void cleanupOldEvents() {
        // Cleanup global events
        if (globalEvents.size() > MAX_EVENTS) {
            globalEvents.subList(0, globalEvents.size() - MAX_EVENTS).clear();
        }

        // Cleanup wish events
        if (wishEvents.size() > MAX_EVENTS) {
            wishEvents.subList(0, wishEvents.size() - MAX_EVENTS).clear();
        }

        // Cleanup contract events
        if (contractEvents.size() > MAX_EVENTS) {
            contractEvents.subList(0, contractEvents.size() - MAX_EVENTS).clear();
        }

        // Cleanup player memories
        for (List<GenieMemoryEntry> entries : playerMemories.values()) {
            if (entries.size() > MAX_EVENTS) {
                entries.subList(0, entries.size() - MAX_EVENTS).clear();
            }
        }
    }

    /**
     * Get recent wishes for a player
     */
    public List<GenieMemoryEntry> getRecentWishes(UUID playerId, int count) {
        List<GenieMemoryEntry> wishes = playerMemories.getOrDefault(playerId, Collections.emptyList());
        return wishes.stream()
            .filter(entry -> "WISH".equals(entry.getEventType()))
            .sorted(Comparator.comparingLong(GenieMemoryEntry::getTimestamp).reversed())
            .limit(count)
            .toList();
    }

    /**
     * Get active contracts
     */
    public List<GenieMemoryEntry> getActiveContracts() {
        return contractEvents.stream()
            .filter(entry -> "ACTIVE".equals(entry.getMetadata().get("status")))
            .toList();
    }

    /**
     * Record wish fulfillment
     */
    public void recordWishFulfillment(UUID playerId, String wishText, String result) {
        GenieMemoryEntry entry = new GenieMemoryEntry(
            playerId,
            "WISH_FULFILLED",
            "Wish fulfilled: " + wishText.substring(0, Math.min(100, wishText.length())) +
            " -> " + result.substring(0, Math.min(50, result.length())),
            Map.of(
                "wishText", wishText,
                "result", result,
                "timestamp", String.valueOf(System.currentTimeMillis())
            )
        );
        recordMemory(entry);
    }

    /**
     * Record contract creation
     */
    public void recordContractCreation(UUID playerId, String contractId, String terms) {
        GenieMemoryEntry entry = new GenieMemoryEntry(
            playerId,
            "CONTRACT_CREATED",
            "Contract created: " + contractId,
            Map.of(
                "contractId", contractId,
                "terms", terms,
                "status", "ACTIVE"
            )
        );
        recordMemory(entry);
    }

    /**
     * Update contract status
     */
    public void updateContractStatus(String contractId, String newStatus) {
        for (GenieMemoryEntry entry : contractEvents) {
            if (contractId.equals(entry.getMetadata().get("contractId"))) {
                entry.getMetadata().put("status", newStatus);
                setDirty();
                break;
            }
        }
    }
}
