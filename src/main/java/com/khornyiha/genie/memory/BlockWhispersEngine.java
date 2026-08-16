package com.khornyiha.genie.memory;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Block state whisper system.
 * Records and replays block state changes and interactions.
 */
public class BlockWhispersEngine {

    private final Map<BlockPos, List<BlockWhisper>> whispers = new ConcurrentHashMap<>();
    private final Map<UUID, List<BlockWhisper>> playerWhispers = new ConcurrentHashMap<>();

    /**
     * Record a block whisper
     */
    public void recordWhisper(BlockPos pos, BlockWhisper whisper) {
        whispers.computeIfAbsent(pos, k -> new ArrayList<>()).add(whisper);
    }

    /**
     * Record a block whisper for player
     */
    public void recordPlayerWhisper(UUID playerId, BlockWhisper whisper) {
        playerWhispers.computeIfAbsent(playerId, k -> new ArrayList<>()).add(whisper);
    }

    /**
     * Get whispers for a block
     */
    public List<BlockWhisper> getBlockWhispers(BlockPos pos) {
        return whispers.getOrDefault(pos, Collections.emptyList());
    }

    /**
     * Get whispers for a player
     */
    public List<BlockWhisper> getPlayerWhispers(UUID playerId) {
        return playerWhispers.getOrDefault(playerId, Collections.emptyList());
    }

    /**
     * Query whispers by keyword
     */
    public List<BlockWhisper> queryWhispersByKeyword(String keyword) {
        String lowerKeyword = keyword.toLowerCase();
        List<BlockWhisper> results = new ArrayList<>();

        // Search all whispers
        for (List<BlockWhisper> whispersList : whispers.values()) {
            results.addAll(whispersList.stream()
                .filter(whisper -> whisper.getMessage().toLowerCase().contains(lowerKeyword))
                .toList());
        }

        for (List<BlockWhisper> whispersList : playerWhispers.values()) {
            results.addAll(whispersList.stream()
                .filter(whisper -> whisper.getMessage().toLowerCase().contains(lowerKeyword))
                .toList());
        }

        return results;
    }

    /**
     * Get recent whispers for a block
     */
    public List<BlockWhisper> getRecentBlockWhispers(BlockPos pos, int count) {
        List<BlockWhisper> blockWhispers = whispers.get(pos);
        if (blockWhispers == null) {
            return Collections.emptyList();
        }

        return blockWhispers.stream()
            .sorted(Comparator.comparingLong(BlockWhisper::getTimestamp).reversed())
            .limit(count)
            .toList();
    }

    /**
     * Replay whispers for a block
     */
    public void replayBlockWhispers(BlockPos pos, ServerLevel level) {
        List<BlockWhisper> whispersList = getBlockWhispers(pos);
        if (whispersList.isEmpty()) {
            return;
        }

        // Sort by timestamp
        whispersList.stream()
            .sorted(Comparator.comparingLong(BlockWhisper::getTimestamp))
            .forEach(whisper -> {
                // Apply block state changes
                if (whisper.getNewState() != null) {
                    level.setBlock(pos, whisper.getNewState(), 3);
                }

                // Trigger effects
                whisper.triggerEffects(level, pos);
            });
    }

    /**
     * Save engine state
     */
    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();

        // Save block whispers
        ListTag blockWhispersTag = new ListTag();
        for (Map.Entry<BlockPos, List<BlockWhisper>> entry : whispers.entrySet()) {
            CompoundTag posTag = new CompoundTag();
            posTag.putLong("position", entry.getKey().asLong());

            ListTag whispersListTag = new ListTag();
            for (BlockWhisper whisper : entry.getValue()) {
                whispersListTag.add(whisper.save());
            }
            posTag.put("whispers", whispersListTag);
            blockWhispersTag.add(posTag);
        }
        tag.put("blockWhispers", blockWhispersTag);

        // Save player whispers
        ListTag playerWhispersTag = new ListTag();
        for (Map.Entry<UUID, List<BlockWhisper>> entry : playerWhispers.entrySet()) {
            CompoundTag playerTag = new CompoundTag();
            playerTag.putUuid("playerId", entry.getKey());

            ListTag whispersListTag = new ListTag();
            for (BlockWhisper whisper : entry.getValue()) {
                whispersListTag.add(whisper.save());
            }
            playerTag.put("whispers", whispersListTag);
            playerWhispersTag.add(playerTag);
        }
        tag.put("playerWhispers", playerWhispersTag);

        return tag;
    }

    /**
     * Load engine state
     */
    public void load(CompoundTag tag) {
        // Load block whispers
        ListTag blockWhispersTag = tag.getList("blockWhispers", Tag.TAG_COMPOUND);
        for (Tag posTag : blockWhispersTag) {
            CompoundTag posCompound = (CompoundTag) posTag;
            BlockPos pos = BlockPos.of(posCompound.getLong("position"));

            List<BlockWhisper> whispersList = new ArrayList<>();
            ListTag whispersListTag = posCompound.getList("whispers", Tag.TAG_COMPOUND);
            for (Tag whisperTag : whispersListTag) {
                whispersList.add(BlockWhisper.load((CompoundTag) whisperTag));
            }

            whispers.put(pos, whispersList);
        }

        // Load player whispers
        ListTag playerWhispersTag = tag.getList("playerWhispers", Tag.TAG_COMPOUND);
        for (Tag playerTag : playerWhispersTag) {
            CompoundTag playerCompound = (CompoundTag) playerTag;
            UUID playerId = playerCompound.getUuid("playerId");

            List<BlockWhisper> whispersList = new ArrayList<>();
            ListTag whispersListTag = playerCompound.getList("whispers", Tag.TAG_COMPOUND);
            for (Tag whisperTag : whispersListTag) {
                whispersList.add(BlockWhisper.load((CompoundTag) whisperTag));
            }

            playerWhispers.put(playerId, whispersList);
        }
    }

    /**
     * Block whisper
     */
    public static class BlockWhisper {
        private final UUID whisperId;
        private final long timestamp;
        private final String message;
        private final BlockState originalState;
        private final BlockState newState;
        private final UUID playerId;
        private final Map<String, String> metadata;

        public BlockWhisper(String message, BlockState originalState, BlockState newState, UUID playerId, Map<String, String> metadata) {
            this.whisperId = UUID.randomUUID();
            this.timestamp = System.currentTimeMillis();
            this.message = message;
            this.originalState = originalState;
            this.newState = newState;
            this.playerId = playerId;
            this.metadata = new HashMap<>(metadata);
        }

        /**
         * Trigger effects when whisper is replayed
         */
        public void triggerEffects(ServerLevel level, BlockPos pos) {
            // Play sound effect
            level.playSound(null, pos, SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.BLOCKS, 1.0F, 1.0F);

            // Spawn particles
            level.sendParticles(
                ParticleTypes.END_ROD,
                pos.getX() + 0.5,
                pos.getY() + 1.0,
                pos.getZ() + 0.5,
                10,
                0.2, 0.2, 0.2,
                0.0
            );
        }

        public CompoundTag save() {
            CompoundTag tag = new CompoundTag();
            tag.putUuid("whisperId", whisperId);
            tag.putLong("timestamp", timestamp);
            tag.putString("message", message);
            tag.putLong("originalState", Block.getId(originalState));
            tag.putLong("newState", Block.getId(newState));
            tag.putUuid("playerId", playerId);

            CompoundTag metadataTag = new CompoundTag();
            metadata.forEach(metadataTag::putString);
            tag.put("metadata", metadataTag);

            return tag;
        }

        public static BlockWhisper load(CompoundTag tag) {
            BlockWhisper whisper = new BlockWhisper(
                tag.getString("message"),
                Block.stateById(tag.getInt("originalState")),
                Block.stateById(tag.getInt("newState")),
                tag.getUuid("playerId"),
                Collections.emptyMap()
            );
            whisper.whisperId = tag.getUuid("whisperId");
            whisper.timestamp = tag.getLong("timestamp");

            CompoundTag metadataTag = tag.getCompound("metadata");
            metadataTag.getAllKeys().forEach(key ->
                whisper.metadata.put(key, metadataTag.getString(key))
            );

            return whisper;
        }

        // Getters
        public UUID getWhisperId() {
            return whisperId;
        }

        public long getTimestamp() {
            return timestamp;
        }

        public String getMessage() {
            return message;
        }

        public BlockState getOriginalState() {
            return originalState;
        }

        public BlockState getNewState() {
            return newState;
        }

        public UUID getPlayerId() {
            return playerId;
        }

        public Map<String, String> getMetadata() {
            return metadata;
        }
    }
}
