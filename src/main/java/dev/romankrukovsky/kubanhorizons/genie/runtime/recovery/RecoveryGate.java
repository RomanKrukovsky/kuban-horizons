package dev.romankrukovsky.kubanhorizons.genie.runtime.recovery;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public record RecoveryGate(List<BlockedTransaction> blockedTransactions, Optional<RecoveryBlockReason> globalReason) {
    public RecoveryGate {
        blockedTransactions = List.copyOf(Objects.requireNonNull(blockedTransactions, "blockedTransactions"));
        Objects.requireNonNull(globalReason, "globalReason");
    }

    public RecoveryAdmission check(AffectedScope scope) {
        Objects.requireNonNull(scope, "scope");
        if (globalReason.isPresent()) {
            return new RecoveryAdmission(false, globalReason.get(), blockedTransactions.stream()
                    .map(BlockedTransaction::transactionId).toList());
        }
        List<BlockedTransaction> overlapping = blockedTransactions.stream()
                .filter(blocked -> blocked.scope().orElseThrow().overlaps(scope)).toList();
        if (overlapping.isEmpty()) {
            return new RecoveryAdmission(true, Optional.empty(), List.of());
        }
        RecoveryBlockReason reason = overlapping.stream()
                .anyMatch(blocked -> blocked.reason() == RecoveryBlockReason.FAILED_SAFE)
                ? RecoveryBlockReason.FAILED_SAFE : RecoveryBlockReason.OVERLAPPING_RECOVERY;
        List<UUID> ids = new ArrayList<>();
        for (BlockedTransaction blocked : overlapping) ids.add(blocked.transactionId());
        return new RecoveryAdmission(false, reason, ids);
    }
}
