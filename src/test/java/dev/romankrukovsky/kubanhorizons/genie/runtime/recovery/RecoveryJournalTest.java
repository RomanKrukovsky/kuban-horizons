package dev.romankrukovsky.kubanhorizons.genie.runtime.recovery;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RecoveryJournalTest {
    private static final int PAYLOAD_BYTES = 16 + 8 + 8 + 4 + 1 + 1 + 64 + 2 + 19 + 16;
    private static final int FRAME_BYTES = 4 + 2 + 4 + PAYLOAD_BYTES + 32;
    private static final String DIGEST = "1".repeat(64);
    private static final UUID TRANSACTION_ID =
            UUID.fromString("67e50dce-2871-4ae7-b32f-753bd00a77a8");

    @TempDir
    Path tempDirectory;

    @Test
    void appendThenReopenAndScanReturnsEquivalentRecord() throws IOException {
        Path journalPath = tempDirectory.resolve("transaction.journal");
        RecoveryRecord record = record(TRANSACTION_ID, 0, TransactionState.PREPARING);

        new RecoveryJournal(journalPath).append(record);
        RecoveryScan scan = new RecoveryJournal(journalPath).scan();

        assertEquals(List.of(record), scan.provenRecords());
        assertFalse(scan.discardedInvalidTail());
    }

    @Test
    void multipleAppendsRemainInPhysicalOrder() throws IOException {
        Path journalPath = tempDirectory.resolve("transaction.journal");
        RecoveryRecord first = record(TRANSACTION_ID, 0, TransactionState.PREPARING);
        RecoveryRecord second = record(TRANSACTION_ID, 1, TransactionState.PREPARED);
        RecoveryJournal journal = new RecoveryJournal(journalPath);

        journal.append(first);
        journal.append(second);

        assertEquals(List.of(first, second), new RecoveryJournal(journalPath).scan().provenRecords());
    }

    @Test
    void appendForcesDataBeforeReturning() throws IOException {
        AtomicInteger forceCalls = new AtomicInteger();
        RecoveryJournal journal = new RecoveryJournal(
                tempDirectory.resolve("transaction.journal"),
                channel -> forceCalls.incrementAndGet()
        );

        journal.append(record(TRANSACTION_ID, 0, TransactionState.PREPARING));

        assertEquals(1, forceCalls.get());
    }

    @Test
    void durabilityFailurePropagates() {
        RecoveryJournal journal = new RecoveryJournal(
                tempDirectory.resolve("transaction.journal"),
                channel -> { throw new IOException("injected force failure"); }
        );

        IOException failure = assertThrows(IOException.class, () ->
                journal.append(record(TRANSACTION_ID, 0, TransactionState.PREPARING)));

        assertEquals("injected force failure", failure.getMessage());
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 5, 9, 12, 40})
    void tornSecondFrameReturnsOnlyProvenPrefix(int cutBytes) throws IOException {
        Path journalPath = twoRecordJournal();

        try (FileChannel channel = FileChannel.open(journalPath, StandardOpenOption.WRITE)) {
            channel.truncate(FRAME_BYTES + cutBytes);
        }

        RecoveryScan scan = new RecoveryJournal(journalPath).scan();
        assertEquals(List.of(record(TRANSACTION_ID, 0, TransactionState.PREPARING)), scan.provenRecords());
        assertTrue(scan.discardedInvalidTail());
    }

    @Test
    void corruptChecksumReturnsOnlyProvenPrefix() throws IOException {
        Path journalPath = twoRecordJournal();
        flipByte(journalPath, FRAME_BYTES + 10L);

        RecoveryScan scan = new RecoveryJournal(journalPath).scan();

        assertEquals(List.of(record(TRANSACTION_ID, 0, TransactionState.PREPARING)), scan.provenRecords());
        assertTrue(scan.discardedInvalidTail());
    }

    @Test
    void rejectsUnsupportedVersion() throws IOException {
        assertInvalidFirstFrame(header(0x4B48524A, (short) 2, 0));
    }

    @Test
    void rejectsOversizedPayloadBeforeAllocation() throws IOException {
        assertInvalidFirstFrame(header(0x4B48524A, (short) 1, 4 * 1024 + 1));
    }

    @Test
    void rejectsIncorrectMagic() throws IOException {
        assertInvalidFirstFrame(header(0x12345678, (short) 1, 0));
    }

    @Test
    void duplicateSequenceTerminatesProvenPrefix() throws IOException {
        Path journalPath = tempDirectory.resolve("duplicate.journal");
        RecoveryJournal journal = new RecoveryJournal(journalPath);
        RecoveryRecord first = record(TRANSACTION_ID, 0, TransactionState.PREPARING);
        RecoveryRecord second = record(TRANSACTION_ID, 1, TransactionState.PREPARED);
        RecoveryRecord duplicate = record(TRANSACTION_ID, 1, TransactionState.APPLYING);
        journal.append(first);
        journal.append(second);
        journal.append(duplicate);

        RecoveryScan scan = journal.scan();

        assertEquals(List.of(first, second), scan.provenRecords());
        assertTrue(scan.discardedInvalidTail());
    }

    @Test
    void decreasingSequenceTerminatesProvenPrefix() throws IOException {
        Path journalPath = tempDirectory.resolve("decreasing.journal");
        RecoveryJournal journal = new RecoveryJournal(journalPath);
        RecoveryRecord first = record(TRANSACTION_ID, 0, TransactionState.PREPARING);
        RecoveryRecord second = record(TRANSACTION_ID, 2, TransactionState.PREPARED);
        RecoveryRecord decreasing = record(TRANSACTION_ID, 1, TransactionState.APPLYING);
        journal.append(first);
        journal.append(second);
        journal.append(decreasing);

        RecoveryScan scan = journal.scan();

        assertEquals(List.of(first, second), scan.provenRecords());
        assertTrue(scan.discardedInvalidTail());
    }

    @Test
    void sequenceIsTrackedPerTransaction() throws IOException {
        Path journalPath = tempDirectory.resolve("interleaved.journal");
        UUID other = UUID.fromString("cecb554e-3e21-42f9-97ed-bfe07a3f5e69");
        List<RecoveryRecord> records = List.of(
                record(TRANSACTION_ID, 0, TransactionState.PREPARING),
                record(other, 0, TransactionState.PREPARING),
                record(TRANSACTION_ID, 1, TransactionState.PREPARED),
                record(other, 1, TransactionState.PREPARED)
        );
        RecoveryJournal journal = new RecoveryJournal(journalPath);
        for (RecoveryRecord record : records) {
            journal.append(record);
        }

        RecoveryScan scan = journal.scan();

        assertEquals(records, scan.provenRecords());
        assertFalse(scan.discardedInvalidTail());
    }

    @Test
    void damagedScanIsIdempotentAndDoesNotRepairFile() throws IOException {
        Path journalPath = twoRecordJournal();
        flipByte(journalPath, FRAME_BYTES + 10L);
        long damagedSize = Files.size(journalPath);

        RecoveryScan first = new RecoveryJournal(journalPath).scan();
        RecoveryScan second = new RecoveryJournal(journalPath).scan();

        assertEquals(first, second);
        assertEquals(damagedSize, Files.size(journalPath));
    }

    private Path twoRecordJournal() throws IOException {
        Path journalPath = tempDirectory.resolve("two-records-" + UUID.randomUUID() + ".journal");
        RecoveryJournal journal = new RecoveryJournal(journalPath);
        journal.append(record(TRANSACTION_ID, 0, TransactionState.PREPARING));
        journal.append(record(TRANSACTION_ID, 1, TransactionState.PREPARED));
        return journalPath;
    }

    private void assertInvalidFirstFrame(byte[] bytes) throws IOException {
        Path journalPath = tempDirectory.resolve("invalid-" + UUID.randomUUID() + ".journal");
        Files.write(journalPath, bytes);

        RecoveryScan scan = new RecoveryJournal(journalPath).scan();

        assertTrue(scan.provenRecords().isEmpty());
        assertTrue(scan.discardedInvalidTail());
    }

    private static byte[] header(int magic, short version, int payloadLength) {
        return ByteBuffer.allocate(10)
                .order(ByteOrder.BIG_ENDIAN)
                .putInt(magic)
                .putShort(version)
                .putInt(payloadLength)
                .array();
    }

    private static void flipByte(Path path, long offset) throws IOException {
        try (FileChannel channel = FileChannel.open(path, StandardOpenOption.READ, StandardOpenOption.WRITE)) {
            ByteBuffer value = ByteBuffer.allocate(1);
            channel.position(offset);
            assertEquals(1, channel.read(value));
            value.flip();
            value.put(0, (byte) (value.get(0) ^ 1));
            channel.position(offset);
            while (value.hasRemaining()) {
                channel.write(value);
            }
        }
    }

    private static RecoveryRecord record(UUID transactionId, long sequence, TransactionState state) {
        return new RecoveryRecord(
                transactionId,
                sequence,
                Instant.EPOCH.plusSeconds(sequence),
                state,
                DIGEST,
                new AffectedScope("minecraft:overworld", 0, 0, 0, 0)
        );
    }
}
