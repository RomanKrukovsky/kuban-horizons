package dev.romankrukovsky.kubanhorizons.genie.runtime.plan;

import dev.romankrukovsky.kubanhorizons.genie.runtime.capability.CapabilityDescriptor;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/** Результат серверной проверки, не содержащий права на мутацию. */
public record ValidatedWishPlan(
        UUID planId,
        UUID actorId,
        CapabilityDescriptor capability,
        Map<String, String> arguments,
        Instant validatedAt,
        Instant expiresAt
) {
    public ValidatedWishPlan {
        Objects.requireNonNull(planId, "planId");
        Objects.requireNonNull(actorId, "actorId");
        Objects.requireNonNull(capability, "capability");
        arguments = Map.copyOf(Objects.requireNonNull(arguments, "arguments"));
        Objects.requireNonNull(validatedAt, "validatedAt");
        Objects.requireNonNull(expiresAt, "expiresAt");
    }
}
