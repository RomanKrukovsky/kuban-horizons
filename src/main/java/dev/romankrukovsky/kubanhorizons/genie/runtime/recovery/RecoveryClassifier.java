package dev.romankrukovsky.kubanhorizons.genie.runtime.recovery;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class RecoveryClassifier {
    private RecoveryClassifier() {
    }

    public static RecoveryGate classify(RecoveryScan scan) {
        if (scan.discardedInvalidTail()) {
            return new RecoveryGate(List.of(), Optional.of(RecoveryBlockReason.CORRUPT_TAIL_UNCERTAINTY));
        }
        Map<UUID, List<RecoveryRecord>> histories = new LinkedHashMap<>();
        for (RecoveryRecord record : scan.provenRecords()) {
            histories.computeIfAbsent(record.transactionId(), ignored -> new ArrayList<>()).add(record);
        }
        List<BlockedTransaction> blocked = new ArrayList<>();
        for (Map.Entry<UUID, List<RecoveryRecord>> entry : histories.entrySet()) {
            List<RecoveryRecord> history = entry.getValue();
            Optional<AffectedScope> scope = history.getFirst().scope();
            if (scope.isEmpty()) {
                blocked.add(block(entry.getKey(), scope, history.getLast(), RecoveryDisposition.FAILED_SAFE,
                        RecoveryBlockReason.LEGACY_UNKNOWN_SCOPE));
                return new RecoveryGate(blocked, Optional.of(RecoveryBlockReason.LEGACY_UNKNOWN_SCOPE));
            }
            boolean failedSafeSeen = false;
            for (int index = 0; index < history.size(); index++) {
                RecoveryRecord current = history.get(index);
                if (!scope.equals(current.scope()) || (index == 0 && current.state() != TransactionState.PREPARING)) {
                    return new RecoveryGate(blocked, Optional.of(RecoveryBlockReason.STARTUP_UNCLASSIFIED));
                }
                if (index > 0 && !legal(history.get(index - 1).state(), current.state())) {
                    if (failedSafeSeen) break;
                    return new RecoveryGate(blocked, Optional.of(RecoveryBlockReason.STARTUP_UNCLASSIFIED));
                }
                failedSafeSeen |= current.state() == TransactionState.FAILED_SAFE;
            }
            RecoveryRecord last = history.getLast();
            TransactionState finalState = failedSafeSeen ? TransactionState.FAILED_SAFE : last.state();
            RecoveryDisposition disposition = disposition(finalState);
            if (disposition != RecoveryDisposition.SAFE_TO_IGNORE) {
                RecoveryBlockReason reason = disposition == RecoveryDisposition.FAILED_SAFE
                        ? RecoveryBlockReason.FAILED_SAFE : RecoveryBlockReason.OVERLAPPING_RECOVERY;
                blocked.add(new BlockedTransaction(entry.getKey(), scope, finalState, disposition, reason));
            }
        }
        return new RecoveryGate(blocked, Optional.empty());
    }

    private static BlockedTransaction block(UUID id, Optional<AffectedScope> scope, RecoveryRecord last,
                                             RecoveryDisposition disposition, RecoveryBlockReason reason) {
        return new BlockedTransaction(id, scope, last.state(), disposition, reason);
    }

    private static RecoveryDisposition disposition(TransactionState state) {
        return switch (state) {
            case PREPARING -> RecoveryDisposition.SAFE_TO_IGNORE;
            case PREPARED, APPLYING, VERIFYING, ROLLING_BACK -> RecoveryDisposition.NEEDS_ROLLBACK;
            case COMMITTED, ROLLED_BACK, RETIRED -> RecoveryDisposition.SAFE_TO_IGNORE;
            case FAILED_SAFE -> RecoveryDisposition.FAILED_SAFE;
        };
    }

    private static boolean legal(TransactionState from, TransactionState to) {
        if (to == TransactionState.FAILED_SAFE) {
            return from != TransactionState.COMMITTED && from != TransactionState.ROLLED_BACK;
        }
        return switch (from) {
            case PREPARING -> to == TransactionState.PREPARED;
            case PREPARED -> to == TransactionState.APPLYING || to == TransactionState.ROLLING_BACK;
            case APPLYING -> to == TransactionState.VERIFYING || to == TransactionState.ROLLING_BACK;
            case VERIFYING -> to == TransactionState.COMMITTED || to == TransactionState.ROLLING_BACK;
            case ROLLING_BACK -> to == TransactionState.ROLLED_BACK;
            case COMMITTED -> to == TransactionState.RETIRED;
            case ROLLED_BACK, FAILED_SAFE, RETIRED -> false;
        };
    }
}
