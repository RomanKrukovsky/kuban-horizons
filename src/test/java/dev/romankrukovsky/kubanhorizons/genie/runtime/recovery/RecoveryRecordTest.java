package dev.romankrukovsky.kubanhorizons.genie.runtime.recovery;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RecoveryRecordTest {
    private static final String DIGEST = "0".repeat(64);

    @Test
    void acceptsCanonicalRecord() {
        UUID transactionId = UUID.randomUUID();
        Instant recordedAt = Instant.parse("2026-08-08T12:00:00Z");

        RecoveryRecord record = new RecoveryRecord(
                transactionId,
                0,
                recordedAt,
                TransactionState.PREPARING,
                DIGEST
        );

        assertEquals(transactionId, record.transactionId());
        assertEquals(0, record.sequence());
        assertEquals(recordedAt, record.recordedAt());
        assertEquals(TransactionState.PREPARING, record.state());
        assertEquals(DIGEST, record.payloadDigest());
    }

    @Test
    void rejectsInvalidFields() {
        UUID transactionId = UUID.randomUUID();
        Instant recordedAt = Instant.EPOCH;

        assertThrows(NullPointerException.class, () ->
                new RecoveryRecord(null, 0, recordedAt, TransactionState.PREPARING, DIGEST));
        assertThrows(IllegalArgumentException.class, () ->
                new RecoveryRecord(transactionId, -1, recordedAt, TransactionState.PREPARING, DIGEST));
        assertThrows(NullPointerException.class, () ->
                new RecoveryRecord(transactionId, 0, null, TransactionState.PREPARING, DIGEST));
        assertThrows(NullPointerException.class, () ->
                new RecoveryRecord(transactionId, 0, recordedAt, null, DIGEST));
        assertThrows(IllegalArgumentException.class, () ->
                new RecoveryRecord(transactionId, 0, recordedAt, TransactionState.PREPARING, "A".repeat(64)));
        assertThrows(IllegalArgumentException.class, () ->
                new RecoveryRecord(transactionId, 0, recordedAt, TransactionState.PREPARING, "0".repeat(63)));
    }

    @Test
    void recoveryScanDefensivelyCopiesRecords() {
        RecoveryRecord record = new RecoveryRecord(
                UUID.randomUUID(), 0, Instant.EPOCH, TransactionState.PREPARING, DIGEST);
        ArrayList<RecoveryRecord> mutable = new ArrayList<>(List.of(record));

        RecoveryScan scan = new RecoveryScan(mutable, false);
        mutable.clear();

        assertEquals(List.of(record), scan.provenRecords());
        assertEquals(record, scan.lastProvenRecord().orElseThrow());
        assertThrows(UnsupportedOperationException.class, () -> scan.provenRecords().clear());
        assertFalse(scan.discardedInvalidTail());
    }

    @Test
    void emptyScanHasNoLastRecord() {
        RecoveryScan scan = new RecoveryScan(List.of(), true);

        assertTrue(scan.lastProvenRecord().isEmpty());
        assertTrue(scan.discardedInvalidTail());
    }
}
