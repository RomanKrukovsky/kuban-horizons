package dev.romankrukovsky.kubanhorizons.genie.runtime.plan;

import dev.romankrukovsky.kubanhorizons.genie.runtime.capability.CapabilityId;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/** Непривилегированный план: его может предложить парсер или LLM. */
public record WishPlan(
        int schemaVersion,
        UUID planId,
        UUID actorId,
        CapabilityId capability,
        Map<String, String> arguments,
        Instant expiresAt
) {
    public static final int CURRENT_SCHEMA_VERSION = 1;
    public static final int MAX_ARGUMENTS = 16;
    public static final int MAX_TEXT_LENGTH = 256;

    public WishPlan {
        Objects.requireNonNull(planId, "planId");
        Objects.requireNonNull(actorId, "actorId");
        Objects.requireNonNull(capability, "capability");
        Objects.requireNonNull(arguments, "arguments");
        Objects.requireNonNull(expiresAt, "expiresAt");
        if (schemaVersion != CURRENT_SCHEMA_VERSION) {
            throw new IllegalArgumentException("unsupported plan schema");
        }
        if (arguments.size() > MAX_ARGUMENTS) {
            throw new IllegalArgumentException("too many plan arguments");
        }
        arguments = Map.copyOf(arguments);
        for (Map.Entry<String, String> entry : arguments.entrySet()) {
            if (entry.getKey().isBlank() || entry.getKey().length() > 64
                    || entry.getValue().length() > MAX_TEXT_LENGTH) {
                throw new IllegalArgumentException("invalid plan argument");
            }
        }
    }
}
