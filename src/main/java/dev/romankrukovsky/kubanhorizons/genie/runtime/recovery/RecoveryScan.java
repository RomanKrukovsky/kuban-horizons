package dev.romankrukovsky.kubanhorizons.genie.runtime.recovery;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record RecoveryScan(
        List<RecoveryRecord> provenRecords,
        boolean discardedInvalidTail,
        long provenLength
) {
    public RecoveryScan {
        provenRecords = List.copyOf(Objects.requireNonNull(provenRecords, "provenRecords"));
        if (provenLength < 0) {
            throw new IllegalArgumentException("provenLength must not be negative");
        }
    }

    public RecoveryScan(List<RecoveryRecord> provenRecords, boolean discardedInvalidTail) {
        this(provenRecords, discardedInvalidTail, 0);
    }

    public Optional<RecoveryRecord> lastProvenRecord() {
        if (provenRecords.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(provenRecords.getLast());
    }
}
