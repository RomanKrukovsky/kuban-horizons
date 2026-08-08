package dev.romankrukovsky.kubanhorizons.genie.runtime.plan;

import dev.romankrukovsky.kubanhorizons.genie.runtime.capability.CapabilityRegistry;
import java.time.Instant;
import java.util.Objects;

/** Строгая граница между предложением и локально разрешённой возможностью. */
public final class PlanGate {
    private final CapabilityRegistry registry;

    public PlanGate(CapabilityRegistry registry) {
        this.registry = Objects.requireNonNull(registry, "registry");
    }

    public ValidatedWishPlan validate(WishPlan plan, Instant now) {
        Objects.requireNonNull(plan, "plan");
        Objects.requireNonNull(now, "now");
        if (!plan.expiresAt().isAfter(now)) {
            throw new IllegalArgumentException("plan expired");
        }
        var descriptor = registry.find(plan.capability())
                .orElseThrow(() -> new IllegalArgumentException("unknown capability"));
        if (!plan.arguments().keySet().containsAll(descriptor.requiredArguments())) {
            throw new IllegalArgumentException("plan is missing required arguments");
        }
        java.util.Set<String> allowed = new java.util.HashSet<>(descriptor.requiredArguments());
        allowed.addAll(descriptor.optionalArguments());
        if (!allowed.containsAll(plan.arguments().keySet())) {
            throw new IllegalArgumentException("plan contains unknown arguments");
        }
        return new ValidatedWishPlan(plan.planId(), plan.actorId(), descriptor,
                plan.arguments(), now, plan.expiresAt());
    }
}
