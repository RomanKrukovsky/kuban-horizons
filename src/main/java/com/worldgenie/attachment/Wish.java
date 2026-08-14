package com.worldgenie.attachment;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.nbt.CompoundTag;

import java.util.UUID;

/**
 * Represents a single wish made by the player.
 * Serialized via Codec for AttachmentType persistence.
 */
public record Wish(
        UUID id,
        String description,
        long timestamp,
        int powerLevel,
        String grantedBy,           // Entity or force that granted the wish
        boolean fulfilled
) {

    public static final Codec<Wish> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.STRING.xmap(UUID::fromString, UUID::toString)
                            .fieldOf("id").forGetter(Wish::id),
                    Codec.STRING.fieldOf("description").forGetter(Wish::description),
                    Codec.LONG.fieldOf("timestamp").forGetter(Wish::timestamp),
                    Codec.INT.fieldOf("power_level").forGetter(Wish::powerLevel),
                    Codec.STRING.fieldOf("granted_by").forGetter(Wish::grantedBy),
                    Codec.BOOL.fieldOf("fulfilled").forGetter(Wish::fulfilled)
            ).apply(instance, Wish::new)
    );

    /**
     * Creates a new wish with a generated UUID.
     */
    public static Wish create(String description, int powerLevel, String grantedBy) {
        return new Wish(
                UUID.randomUUID(),
                description,
                System.currentTimeMillis(),
                powerLevel,
                grantedBy,
                false
        );
    }

    /**
     * Marks this wish as fulfilled.
     */
    public Wish fulfill() {
        return new Wish(id, description, timestamp, powerLevel, grantedBy, true);
    }

    /**
     * Converts to a CompoundTag (for legacy NBT usage if needed).
     */
    public CompoundTag toCompoundTag() {
        CompoundTag tag = new CompoundTag();
        tag.putString("id", id.toString());
        tag.putString("description", description);
        tag.putLong("timestamp", timestamp);
        tag.putInt("power_level", powerLevel);
        tag.putString("granted_by", grantedBy);
        tag.putBoolean("fulfilled", fulfilled);
        return tag;
    }

    /**
     * Creates a Wish from a CompoundTag.
     */
    public static Wish fromCompoundTag(CompoundTag tag) {
        return new Wish(
                UUID.fromString(tag.getString("id")),
                tag.getString("description"),
                tag.getLong("timestamp"),
                tag.getInt("power_level"),
                tag.getString("granted_by"),
                tag.getBoolean("fulfilled")
        );
    }
}
