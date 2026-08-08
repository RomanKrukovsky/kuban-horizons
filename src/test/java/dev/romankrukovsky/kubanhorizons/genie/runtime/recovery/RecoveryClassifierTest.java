package dev.romankrukovsky.kubanhorizons.genie.runtime.recovery;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RecoveryClassifierTest {
    private static final String DIGEST = "a".repeat(64);
    private static final AffectedScope SCOPE = new AffectedScope("minecraft:overworld", 0, 0, 2, 2);

    @Test
    void preparingIsSafeAndUnrelatedWorkIsPermitted() {
        RecoveryGate gate = RecoveryClassifier.classify(new RecoveryScan(List.of(record(0, TransactionState.PREPARING, SCOPE)), false, 1));
        assertTrue(gate.check(SCOPE).permitted());
        assertTrue(gate.blockedTransactions().isEmpty());
    }

    @ParameterizedTest
    @EnumSource(value = TransactionState.class, names = {"PREPARED", "APPLYING", "VERIFYING", "ROLLING_BACK"})
    void activeStatesNeedRollback(TransactionState state) {
        RecoveryGate gate = RecoveryClassifier.classify(new RecoveryScan(historyTo(state), false, 1));
        BlockedTransaction blocked = gate.blockedTransactions().getFirst();
        assertEquals(RecoveryDisposition.NEEDS_ROLLBACK, blocked.disposition());
        assertEquals(RecoveryBlockReason.OVERLAPPING_RECOVERY, gate.check(SCOPE).reason().orElseThrow());
    }

    @ParameterizedTest
    @EnumSource(value = TransactionState.class, names = {"COMMITTED", "ROLLED_BACK", "RETIRED"})
    void terminalSuccessDoesNotBlockNewWork(TransactionState state) {
        RecoveryGate gate = RecoveryClassifier.classify(new RecoveryScan(historyTo(state), false, 1));
        assertTrue(gate.blockedTransactions().isEmpty());
        assertTrue(gate.check(SCOPE).permitted());
    }

    @Test
    void committedMayRetireButRolledBackMayNot() {
        RecoveryGate retired = RecoveryClassifier.classify(new RecoveryScan(
                historyTo(TransactionState.RETIRED), false, 1));
        assertTrue(retired.check(SCOPE).permitted());

        List<RecoveryRecord> illegal = new ArrayList<>(historyTo(TransactionState.ROLLED_BACK));
        illegal.add(record(illegal.size(), TransactionState.RETIRED, SCOPE));
        assertUnclassified(illegal);
    }

    @Test
    void failedSafeIsStickyAndBlocks() {
        List<RecoveryRecord> history = new ArrayList<>(historyTo(TransactionState.FAILED_SAFE));
        history.add(record(history.size(), TransactionState.ROLLING_BACK, SCOPE));
        RecoveryGate gate = RecoveryClassifier.classify(new RecoveryScan(history, false, 1));
        assertEquals(RecoveryDisposition.FAILED_SAFE, gate.blockedTransactions().getFirst().disposition());
        assertEquals(RecoveryBlockReason.FAILED_SAFE, gate.check(SCOPE).reason().orElseThrow());
    }

    @Test
    void corruptTailAndLegacyScopeFailClosedGlobally() {
        RecoveryGate corrupt = RecoveryClassifier.classify(new RecoveryScan(List.of(), true, 0));
        assertEquals(RecoveryBlockReason.CORRUPT_TAIL_UNCERTAINTY, corrupt.check(SCOPE).reason().orElseThrow());
        RecoveryRecord legacy = new RecoveryRecord(UUID.randomUUID(), 0, Instant.EPOCH, TransactionState.PREPARED, DIGEST);
        RecoveryGate legacyGate = RecoveryClassifier.classify(new RecoveryScan(List.of(legacy), false, 1));
        assertEquals(RecoveryBlockReason.LEGACY_UNKNOWN_SCOPE, legacyGate.check(SCOPE).reason().orElseThrow());
    }

    @Test
    void unrelatedDimensionsAndRegionsArePermittedAndResultsAreImmutable() {
        RecoveryGate gate = RecoveryClassifier.classify(new RecoveryScan(historyTo(TransactionState.PREPARED), false, 1));
        assertTrue(gate.check(new AffectedScope("minecraft:the_nether", 0, 0, 2, 2)).permitted());
        assertTrue(gate.check(new AffectedScope("minecraft:overworld", 3, 3, 4, 4)).permitted());
        RecoveryAdmission admission = gate.check(SCOPE);
        assertThrows(UnsupportedOperationException.class, () -> admission.blockingTransactionIds().clear());
        assertThrows(UnsupportedOperationException.class, () -> gate.blockedTransactions().clear());
        assertEquals(gate, RecoveryClassifier.classify(new RecoveryScan(historyTo(TransactionState.PREPARED), false, 1)));
    }

    @Test
    void skippedBackwardScopeChangingAndNonPreparingStartsFailClosed() {
        assertUnclassified(List.of(record(0, TransactionState.APPLYING, SCOPE)));
        assertUnclassified(List.of(record(0, TransactionState.PREPARING, SCOPE), record(1, TransactionState.APPLYING, SCOPE)));
        assertUnclassified(List.of(record(0, TransactionState.PREPARING, SCOPE), record(1, TransactionState.PREPARED, SCOPE), record(2, TransactionState.PREPARING, SCOPE)));
        assertUnclassified(List.of(record(0, TransactionState.PREPARING, SCOPE), record(1, TransactionState.PREPARED, new AffectedScope("minecraft:overworld", 10, 10, 11, 11))));
    }

    private static void assertUnclassified(List<RecoveryRecord> records) {
        RecoveryGate gate = RecoveryClassifier.classify(new RecoveryScan(records, false, 1));
        assertEquals(RecoveryBlockReason.STARTUP_UNCLASSIFIED, gate.check(SCOPE).reason().orElseThrow());
    }

    private static List<RecoveryRecord> historyTo(TransactionState state) {
        List<TransactionState> path = switch (state) {
            case PREPARING -> List.of(TransactionState.PREPARING);
            case PREPARED -> List.of(TransactionState.PREPARING, TransactionState.PREPARED);
            case APPLYING -> List.of(TransactionState.PREPARING, TransactionState.PREPARED, TransactionState.APPLYING);
            case VERIFYING -> List.of(TransactionState.PREPARING, TransactionState.PREPARED, TransactionState.APPLYING, TransactionState.VERIFYING);
            case ROLLING_BACK -> List.of(TransactionState.PREPARING, TransactionState.PREPARED, TransactionState.APPLYING, TransactionState.ROLLING_BACK);
            case COMMITTED -> List.of(TransactionState.PREPARING, TransactionState.PREPARED, TransactionState.APPLYING, TransactionState.VERIFYING, TransactionState.COMMITTED);
            case ROLLED_BACK -> List.of(TransactionState.PREPARING, TransactionState.PREPARED, TransactionState.APPLYING, TransactionState.ROLLING_BACK, TransactionState.ROLLED_BACK);
            case FAILED_SAFE -> List.of(TransactionState.PREPARING, TransactionState.PREPARED, TransactionState.FAILED_SAFE);
            case RETIRED -> List.of(TransactionState.PREPARING, TransactionState.PREPARED,
                    TransactionState.APPLYING, TransactionState.VERIFYING,
                    TransactionState.COMMITTED, TransactionState.RETIRED);
        };
        UUID id = UUID.fromString("12345678-1234-1234-1234-123456789abc");
        List<RecoveryRecord> records = new ArrayList<>();
        for (int i = 0; i < path.size(); i++) records.add(record(id, i, path.get(i), SCOPE));
        return records;
    }

    private static RecoveryRecord record(long sequence, TransactionState state, AffectedScope scope) {
        return record(UUID.fromString("12345678-1234-1234-1234-123456789abc"), sequence, state, scope);
    }

    private static RecoveryRecord record(UUID id, long sequence, TransactionState state, AffectedScope scope) {
        return new RecoveryRecord(id, sequence, Instant.EPOCH.plusSeconds(sequence), state, DIGEST, scope);
    }
}
