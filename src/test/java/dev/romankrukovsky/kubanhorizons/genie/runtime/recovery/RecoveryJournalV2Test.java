package dev.romankrukovsky.kubanhorizons.genie.runtime.recovery;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RecoveryJournalV2Test {
    private static final UUID ID = UUID.fromString("67e50dce-2871-4ae7-b32f-753bd00a77a8");
    private static final String DIGEST = "1".repeat(64);
    private static final AffectedScope SCOPE = new AffectedScope("minecraft:overworld", -2, 3, 5, 9);

    @TempDir Path temp;

    @Test
    void scopedRecordsUseV2AndRoundTripAfterReopen() throws Exception {
        Path path = temp.resolve("v2.journal");
        RecoveryRecord record = new RecoveryRecord(ID, 0, Instant.EPOCH, TransactionState.PREPARING, DIGEST, SCOPE);
        new RecoveryJournal(path).append(record);
        byte[] bytes = Files.readAllBytes(path);
        assertEquals(2, ByteBuffer.wrap(bytes, 4, 2).getShort());
        RecoveryScan scan = new RecoveryJournal(path).scan();
        assertEquals(List.of(record), scan.provenRecords());
        assertEquals(bytes.length, scan.provenLength());
        assertFalse(scan.discardedInvalidTail());
    }

    @Test
    void readsCommittedLiteralV1Fixture() throws Exception {
        Path path = temp.resolve("literal-v1.journal");
        byte[] fixture = HexFormat.of().parseHex("4b48524a00010000006667e50dce28714ae7b32f753bd00a77a80000000000000000000000000000000000000000004031313131313131313131313131313131313131313131313131313131313131313131313131313131313131313131313131313131313131313131313131313131a02fb80b7631b65447f0e986020bad2d535c7b7e92e5cc579943c1264c2b3d59");
        Files.write(path, fixture);
        RecoveryScan scan = new RecoveryJournal(path).scan();
        assertFalse(scan.discardedInvalidTail());
        assertTrue(scan.provenRecords().getFirst().scope().isEmpty());
        assertEquals(fixture.length, scan.provenLength());
    }

    @Test
    void refusesAppendingLegacyUnscopedRecords() {
        RecoveryRecord legacy = new RecoveryRecord(ID, 0, Instant.EPOCH, TransactionState.PREPARING, DIGEST);
        assertThrows(IllegalArgumentException.class, () -> new RecoveryJournal(temp.resolve("x")).append(legacy));
    }

    @Test
    void malformedV2AndTrailingBytesStopAtPriorOffset() throws Exception {
        Path path = temp.resolve("malformed.journal");
        RecoveryJournal journal = new RecoveryJournal(path);
        journal.append(new RecoveryRecord(ID, 0, Instant.EPOCH, TransactionState.PREPARING, DIGEST, SCOPE));
        long proven = Files.size(path);
        Files.write(path, frame((short) 2, new byte[]{0}), StandardOpenOption.APPEND);
        RecoveryScan scan = journal.scan();
        assertTrue(scan.discardedInvalidTail());
        assertEquals(proven, scan.provenLength());
    }

    @Test
    void appendRefusesInvalidTailUntilRepairThenSucceedsIdempotently() throws Exception {
        Path path = temp.resolve("repair.journal");
        RecoveryJournal journal = new RecoveryJournal(path);
        journal.append(new RecoveryRecord(ID, 0, Instant.EPOCH, TransactionState.PREPARING, DIGEST, SCOPE));
        long proven = Files.size(path);
        Files.write(path, new byte[]{1, 2, 3}, StandardOpenOption.APPEND);
        RecoveryScan damaged = journal.scan();
        assertThrows(IOException.class, () -> journal.append(new RecoveryRecord(ID, 1, Instant.EPOCH, TransactionState.PREPARED, DIGEST, SCOPE)));
        journal.repairInvalidTail(damaged);
        journal.repairInvalidTail(journal.scan());
        assertEquals(proven, Files.size(path));
        journal.append(new RecoveryRecord(ID, 1, Instant.EPOCH, TransactionState.PREPARED, DIGEST, SCOPE));
        assertEquals(2, journal.scan().provenRecords().size());
    }

    @Test
    void rejectsStaleRepairBoundary() throws Exception {
        Path path = temp.resolve("stale.journal");
        Files.write(path, new byte[]{1, 2, 3});
        RecoveryJournal journal = new RecoveryJournal(path);
        RecoveryScan scan = journal.scan();
        Files.write(path, new byte[]{4}, StandardOpenOption.APPEND);
        RecoveryScan stale = new RecoveryScan(scan.provenRecords(), true, scan.provenLength() + 1);
        assertThrows(IOException.class, () -> journal.repairInvalidTail(stale));
    }

    private static byte[] frame(short version, byte[] payload) throws Exception {
        byte[] checksum = MessageDigest.getInstance("SHA-256").digest(payload);
        return ByteBuffer.allocate(10 + payload.length + checksum.length).order(ByteOrder.BIG_ENDIAN)
                .putInt(0x4B48524A).putShort(version).putInt(payload.length).put(payload).put(checksum).array();
    }
}
