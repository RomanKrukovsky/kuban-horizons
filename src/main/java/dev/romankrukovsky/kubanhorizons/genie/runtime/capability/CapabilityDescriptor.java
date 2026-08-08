package dev.romankrukovsky.kubanhorizons.genie.runtime.capability;

import java.util.Objects;
import java.util.Set;

/** Публичное описание возможности без ссылки на её реализацию. */
public record CapabilityDescriptor(
        CapabilityId id,
        CapabilityCategory category,
        CapabilityRisk baseRisk,
        int maximumChunks,
        boolean requiresPreview,
        boolean reversible,
        Set<String> requiredArguments,
        Set<String> optionalArguments
) {
    public CapabilityDescriptor {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(category, "category");
        Objects.requireNonNull(baseRisk, "baseRisk");
        requiredArguments = Set.copyOf(Objects.requireNonNull(requiredArguments, "requiredArguments"));
        optionalArguments = Set.copyOf(Objects.requireNonNull(optionalArguments, "optionalArguments"));
        if (maximumChunks < 0 || maximumChunks > 256) {
            throw new IllegalArgumentException("maximumChunks must be between 0 and 256");
        }
        if (!java.util.Collections.disjoint(requiredArguments, optionalArguments)) {
            throw new IllegalArgumentException("required and optional arguments overlap");
        }
    }
}
