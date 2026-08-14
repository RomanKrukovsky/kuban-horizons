package com.worldgenie.attachment;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.nbt.CompoundTag;

import java.util.UUID;

/**
 * A single causal entry representing a state change caused by a wish.
 * Stores before/after CompoundTag deltas for rollback support.
 */
public record CausalEntry(
        UUID id,
        long timestamp,
        UUID wishId,
        CompoundTag beforeState,    // Full entity/player state before wish
        CompoundTag afterState,     // Full entity/player state after wish
        RollbackChoice rollbackChoice
) {

    // Custom codec for CompoundTag (old MC 1.20.6 style)
    private static final Codec<CompoundTag> COMPOUND_TAG_CODEC =
            Codec.PASSTHROUGH.xmap(
                    dynamic -> (CompoundTag) dynamic.convert(CompoundTag.TYPE).getValue(),
                    tag -> new com.mojang.serialization.Dynamic<>(CompoundTag.TYPE, tag)
            );

    public static final Codec<CausalEntry> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.STRING.xmap(UUID::fromString, UUID::toString)
                            .fieldOf("id").forGetter(CausalEntry::id),
                    Codec.LONG.fieldOf("timestamp").forGetter(CausalEntry::timestamp),
                    Codec.STRING.xmap(UUID::fromString, UUID::toString)
                            .fieldOf("wish_id").forGetter(CausalEntry::wishId),
                    COMPOUND_TAG_CODEC.fieldOf("before_state")
                            .forGetter(CausalEntry::beforeState),
                    COMPOUND_TAG_CODEC.fieldOf("after_state")
                            .forGetter(CausalEntry::afterState),
                    RollbackChoice.CODEC.fieldOf("rollback_choice")
                            .forGetter(CausalEntry::rollbackChoice)
            ).apply(instance, CausalEntry::new)
    );

    /**
     * Convenience constructor that generates a UUID.
     */
    public CausalEntry(long timestamp, UUID wishId, CompoundTag beforeState,
                       CompoundTag afterState, RollbackChoice choice) {
        this(UUID.randomUUID(), timestamp, wishId, beforeState, afterState, choice);
    }

    /**
     * Returns a new entry with the rollback choice updated.
     */
    public CausalEntry withRollbackChoice(RollbackChoice choice) {
        return new CausalEntry(id, timestamp, wishId, beforeState, afterState, choice);
    }

    /**
     * Checks if this entry has been rolled back.
     */
    public boolean isRolledBack() {
        return rollbackChoice != RollbackChoice.NONE;
    }
}
