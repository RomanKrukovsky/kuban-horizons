package dev.romankrukovsky.kubanhorizons.genie.runtime.policy;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record PolicyManifest(UUID transactionId, UUID actorId, String ruleId,
                             String beforeValue, String targetValue,
                             Instant createdAt, PolicyState state) {
    public PolicyManifest {
        Objects.requireNonNull(transactionId, "transactionId");
        Objects.requireNonNull(actorId, "actorId");
        Objects.requireNonNull(ruleId, "ruleId");
        Objects.requireNonNull(beforeValue, "beforeValue");
        Objects.requireNonNull(targetValue, "targetValue");
        Objects.requireNonNull(createdAt, "createdAt");
        Objects.requireNonNull(state, "state");
    }

    public enum PolicyState { PREPARED, COMMITTED, ROLLED_BACK, RETIRED }
}
