package com.worldgenie.attachment;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.UUID;

/**
 * Records when the player and a wish-granting entity swap roles temporarily
 * or permanently. This is a key narrative mechanic.
 */
public record RoleSwapEvent(
        UUID id,
        long timestamp,
        String originalRole,        // Player's original role (e.g., "mortal")
        String newRole,             // New role after swap (e.g., "wish_granter")
        String swappedWith,         // Entity the player swapped with
        UUID relatedWishId,
        boolean permanent,
        long durationMs             // If not permanent, how long the swap lasts
) {

    public static final Codec<RoleSwapEvent> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.STRING.xmap(UUID::fromString, UUID::toString)
                            .fieldOf("id").forGetter(RoleSwapEvent::id),
                    Codec.LONG.fieldOf("timestamp").forGetter(RoleSwapEvent::timestamp),
                    Codec.STRING.fieldOf("original_role").forGetter(RoleSwapEvent::originalRole),
                    Codec.STRING.fieldOf("new_role").forGetter(RoleSwapEvent::newRole),
                    Codec.STRING.fieldOf("swapped_with").forGetter(RoleSwapEvent::swappedWith),
                    Codec.STRING.xmap(UUID::fromString, UUID::toString)
                            .fieldOf("related_wish_id").forGetter(RoleSwapEvent::relatedWishId),
                    Codec.BOOL.fieldOf("permanent").forGetter(RoleSwapEvent::permanent),
                    Codec.LONG.fieldOf("duration_ms").forGetter(RoleSwapEvent::durationMs)
            ).apply(instance, RoleSwapEvent::new)
    );

    public static RoleSwapEvent createTemporary(String originalRole, String newRole,
                                                 String swappedWith, UUID wishId, long durationMs) {
        return new RoleSwapEvent(
                UUID.randomUUID(),
                System.currentTimeMillis(),
                originalRole,
                newRole,
                swappedWith,
                wishId,
                false,
                durationMs
        );
    }

    public static RoleSwapEvent createPermanent(String originalRole, String newRole,
                                                 String swappedWith, UUID wishId) {
        return new RoleSwapEvent(
                UUID.randomUUID(),
                System.currentTimeMillis(),
                originalRole,
                newRole,
                swappedWith,
                wishId,
                true,
                -1
        );
    }
}
