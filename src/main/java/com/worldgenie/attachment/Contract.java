package com.worldgenie.attachment;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.UUID;

/**
 * Represents a binding contract between the player and a wish-granting entity.
 */
public record Contract(
        UUID id,
        UUID relatedWishId,
        String terms,
        String partyA,              // Usually the player
        String partyB,              // The granting entity
        long signedAt,
        boolean active,
        int penaltyLevel            // Severity of breaking the contract
) {

    public static final Codec<Contract> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.STRING.xmap(UUID::fromString, UUID::toString)
                            .fieldOf("id").forGetter(Contract::id),
                    Codec.STRING.xmap(UUID::fromString, UUID::toString)
                            .fieldOf("related_wish_id").forGetter(Contract::relatedWishId),
                    Codec.STRING.fieldOf("terms").forGetter(Contract::terms),
                    Codec.STRING.fieldOf("party_a").forGetter(Contract::partyA),
                    Codec.STRING.fieldOf("party_b").forGetter(Contract::partyB),
                    Codec.LONG.fieldOf("signed_at").forGetter(Contract::signedAt),
                    Codec.BOOL.fieldOf("active").forGetter(Contract::active),
                    Codec.INT.fieldOf("penalty_level").forGetter(Contract::penaltyLevel)
            ).apply(instance, Contract::new)
    );

    public static Contract create(UUID wishId, String terms, String partyA, String partyB, int penaltyLevel) {
        return new Contract(
                UUID.randomUUID(),
                wishId,
                terms,
                partyA,
                partyB,
                System.currentTimeMillis(),
                true,
                penaltyLevel
        );
    }

    public Contract terminate() {
        return new Contract(id, relatedWishId, terms, partyA, partyB, signedAt, false, penaltyLevel);
    }
}
