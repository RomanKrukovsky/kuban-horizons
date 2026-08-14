package com.worldgenie.attachment;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Tracks changes to the player's temperament/personality over time
 * as a result of wish granting and contract interactions.
 */
public record TemperamentDrift(
        long timestamp,
        String attribute,           // e.g., "greed", "empathy", "ambition"
        float previousValue,
        float newValue,
        String cause,               // Description of what caused the drift
        UUID relatedWishId
) {

    public static final Codec<TemperamentDrift> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.LONG.fieldOf("timestamp").forGetter(TemperamentDrift::timestamp),
                    Codec.STRING.fieldOf("attribute").forGetter(TemperamentDrift::attribute),
                    Codec.FLOAT.fieldOf("previous_value").forGetter(TemperamentDrift::previousValue),
                    Codec.FLOAT.fieldOf("new_value").forGetter(TemperamentDrift::newValue),
                    Codec.STRING.fieldOf("cause").forGetter(TemperamentDrift::cause),
                    Codec.STRING.xmap(java.util.UUID::fromString, java.util.UUID::toString)
                            .optionalFieldOf("related_wish_id", null)
                            .forGetter(TemperamentDrift::relatedWishId)
            ).apply(instance, TemperamentDrift::new)
    );

    public static TemperamentDrift create(String attribute, float previous, float current,
                                          String cause, java.util.UUID wishId) {
        return new TemperamentDrift(
                System.currentTimeMillis(),
                attribute,
                previous,
                current,
                cause,
                wishId
        );
    }

    public float getDelta() {
        return newValue - previousValue;
    }
}
