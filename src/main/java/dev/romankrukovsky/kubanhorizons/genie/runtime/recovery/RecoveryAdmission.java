package dev.romankrukovsky.kubanhorizons.genie.runtime.recovery;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public record RecoveryAdmission(
        boolean permitted,
        Optional<RecoveryBlockReason> reason,
        List<UUID> blockingTransactionIds
) {
    public RecoveryAdmission {
        Objects.requireNonNull(reason, "reason");
        blockingTransactionIds = List.copyOf(Objects.requireNonNull(blockingTransactionIds, "blockingTransactionIds"));
        if (permitted == reason.isPresent()) {
            throw new IllegalArgumentException("permitted admissions have no reason; blocked admissions require one");
        }
    }

    public RecoveryAdmission(boolean permitted, RecoveryBlockReason reason, List<UUID> blockingTransactionIds) {
        this(permitted, Optional.ofNullable(reason), blockingTransactionIds);
    }
}
