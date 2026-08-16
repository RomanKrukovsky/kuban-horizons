package com.khornyiha.genie.memory;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.*;

/**
 * Reality theater engine.
 * Reconstructs past events and allows visualization of historical moments.
 */
public class VisualReenactmentEngine {

    private final Map<UUID, List<ReenactmentEvent>> reenactments = new HashMap<>();
    private final Map<UUID, ReenactmentSession> activeSessions = new HashMap<>();

    /**
     * Record a new reenactment event
     */
    public void recordReenactment(UUID playerId, ReenactmentEvent event) {
        reenactments.computeIfAbsent(playerId, k -> new ArrayList<>()).add(event);
    }

    /**
     * Create a new reenactment session
     */
    public ReenactmentSession createSession(Player player, long startTime, long endTime) {
        ReenactmentSession session = new ReenactmentSession(
            UUID.randomUUID(),
            player.getUUID(),
            startTime,
            endTime,
            player.level().dimension()
        );

        activeSessions.put(session.getSessionId(), session);
        return session;
    }

    /**
     * Play a reenactment session
     */
    public boolean playSession(ReenactmentSession session, Player viewer) {
        if (!activeSessions.containsKey(session.getSessionId())) {
            return false;
        }

        // Load historical data
        List<ReenactmentEvent> events = reenactments.get(session.getPlayerId());
        if (events == null) {
            return false;
        }

        // Filter events within time range
        List<ReenactmentEvent> relevantEvents = events.stream()
            .filter(event -> event.getTimestamp() >= session.getStartTime() &&
                          event.getTimestamp() <= session.getEndTime())
            .sorted(Comparator.comparingLong(ReenactmentEvent::getTimestamp))
            .toList();

        if (relevantEvents.isEmpty()) {
            return false;
        }

        // Apply effects to viewer
        session.applyEffects(viewer);

        return true;
    }

    /**
     * Stop a reenactment session
     */
    public void stopSession(UUID sessionId) {
        activeSessions.remove(sessionId);
    }

    /**
     * Get active sessions for player
     */
    public List<ReenactmentSession> getPlayerSessions(Player player) {
        return activeSessions.values().stream()
            .filter(session -> session.getPlayerId().equals(player.getUUID()))
            .toList();
    }

    /**
     * Save engine state
     */
    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();

        // Save reenactments
        ListTag reenactmentsTag = new ListTag();
        for (Map.Entry<UUID, List<ReenactmentEvent>> entry : reenactments.entrySet()) {
            CompoundTag playerTag = new CompoundTag();
            playerTag.putUuid("playerId", entry.getKey());

            ListTag eventsTag = new ListTag();
            for (ReenactmentEvent event : entry.getValue()) {
                eventsTag.add(event.save());
            }
            playerTag.put("events", eventsTag);
            reenactmentsTag.add(playerTag);
        }
        tag.put("reenactments", reenactmentsTag);

        // Save active sessions
        ListTag sessionsTag = new ListTag();
        for (ReenactmentSession session : activeSessions.values()) {
            sessionsTag.add(session.save());
        }
        tag.put("activeSessions", sessionsTag);

        return tag;
    }

    /**
     * Load engine state
     */
    public void load(CompoundTag tag) {
        // Load reenactments
        ListTag reenactmentsTag = tag.getList("reenactments", Tag.TAG_COMPOUND);
        for (Tag playerTag : reenactmentsTag) {
            CompoundTag playerCompound = (CompoundTag) playerTag;
            UUID playerId = playerCompound.getUuid("playerId");

            List<ReenactmentEvent> events = new ArrayList<>();
            ListTag eventsTag = playerCompound.getList("events", Tag.TAG_COMPOUND);
            for (Tag eventTag : eventsTag) {
                events.add(ReenactmentEvent.load((CompoundTag) eventTag));
            }

            reenactments.put(playerId, events);
        }

        // Load active sessions
        ListTag sessionsTag = tag.getList("activeSessions", Tag.TAG_COMPOUND);
        for (Tag sessionTag : sessionsTag) {
            ReenactmentSession session = ReenactmentSession.load((CompoundTag) sessionTag);
            activeSessions.put(session.getSessionId(), session);
        }
    }

    /**
     * Reenactment event
     */
    public static class ReenactmentEvent {
        private final UUID eventId;
        private final long timestamp;
        private final String eventType;
        private final String description;
        private final Map<String, String> metadata;

        public ReenactmentEvent(long timestamp, String eventType, String description, Map<String, String> metadata) {
            this.eventId = UUID.randomUUID();
            this.timestamp = timestamp;
            this.eventType = eventType;
            this.description = description;
            this.metadata = new HashMap<>(metadata);
        }

        public CompoundTag save() {
            CompoundTag tag = new CompoundTag();
            tag.putUuid("eventId", eventId);
            tag.putLong("timestamp", timestamp);
            tag.putString("eventType", eventType);
            tag.putString("description", description);

            CompoundTag metadataTag = new CompoundTag();
            metadata.forEach(metadataTag::putString);
            tag.put("metadata", metadataTag);

            return tag;
        }

        public static ReenactmentEvent load(CompoundTag tag) {
            ReenactmentEvent event = new ReenactmentEvent(
                tag.getLong("timestamp"),
                tag.getString("eventType"),
                tag.getString("description"),
                Collections.emptyMap()
            );
            event.eventId = tag.getUuid("eventId");

            CompoundTag metadataTag = tag.getCompound("metadata");
            metadataTag.getAllKeys().forEach(key ->
                event.metadata.put(key, metadataTag.getString(key))
            );

            return event;
        }

        // Getters
        public UUID getEventId() {
            return eventId;
        }

        public long getTimestamp() {
            return timestamp;
        }

        public String getEventType() {
            return eventType;
        }

        public String getDescription() {
            return description;
        }

        public Map<String, String> getMetadata() {
            return metadata;
        }
    }

    /**
     * Reenactment session
     */
    public static class ReenactmentSession {
        private final UUID sessionId;
        private final UUID playerId;
        private final long startTime;
        private final long endTime;
        private final ResourceKey<Level> dimension;

        public ReenactmentSession(UUID sessionId, UUID playerId, long startTime, long endTime, ResourceKey<Level> dimension) {
            this.sessionId = sessionId;
            this.playerId = playerId;
            this.startTime = startTime;
            this.endTime = endTime;
            this.dimension = dimension;
        }

        /**
         * Apply visual effects to viewer
         */
        public void applyEffects(Player viewer) {
            // Apply temporal distortion effect
            viewer.addEffect(new MobEffectInstance(
                MobEffects.SLOW_FALLING,
                (int) (endTime - startTime) / 20,
                2,
                false,
                false
            ));

            // Apply night vision for darkness
            viewer.addEffect(new MobEffectInstance(
                MobEffects.NIGHT_VISION,
                (int) (endTime - startTime) / 20,
                0,
                false,
                false
            ));
        }

        public CompoundTag save() {
            CompoundTag tag = new CompoundTag();
            tag.putUuid("sessionId", sessionId);
            tag.putUuid("playerId", playerId);
            tag.putLong("startTime", startTime);
            tag.putLong("endTime", endTime);
            tag.putString("dimension", dimension.location().toString());
            return tag;
        }

        public static ReenactmentSession load(CompoundTag tag) {
            return new ReenactmentSession(
                tag.getUuid("sessionId"),
                tag.getUuid("playerId"),
                tag.getLong("startTime"),
                tag.getLong("endTime"),
                ResourceKey.create(Registries.DIMENSION, new ResourceLocation(tag.getString("dimension")))
            );
        }

        // Getters
        public UUID getSessionId() {
            return sessionId;
        }

        public UUID getPlayerId() {
            return playerId;
        }

        public long getStartTime() {
            return startTime;
        }

        public long getEndTime() {
            return endTime;
        }

        public ResourceKey<Level> getDimension() {
            return dimension;
        }
    }
}
