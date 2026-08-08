package dev.romankrukovsky.kubanhorizons.genie.runtime.recovery;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RecoveryJournalStorageTest {
    private static final RecoveryRecord RECORD = new RecoveryRecord(UUID.randomUUID(), 0, Instant.EPOCH,
            TransactionState.PREPARING, "f".repeat(64), new AffectedScope("minecraft:overworld", 0, 0, 0, 0));
    @TempDir Path temp;

    @Test
    void boundedZeroProgressWriteFailsInsteadOfHanging() {
        RecoveryJournal.FileJournalStorage storage = new RecoveryJournal.FileJournalStorage() {
            @Override public int write(FileChannel channel, ByteBuffer source) { return 0; }
        };
        IOException failure = assertThrows(IOException.class, () -> new RecoveryJournal(temp.resolve("zero-write"), storage).append(RECORD));
        assertEquals("journal write made no progress", failure.getMessage());
    }

    @Test
    void boundedZeroProgressReadFailsInsteadOfHanging() throws Exception {
        Path path = temp.resolve("zero-read");
        new RecoveryJournal(path).append(RECORD);
        RecoveryJournal.FileJournalStorage storage = new RecoveryJournal.FileJournalStorage() {
            @Override public int read(FileChannel channel, ByteBuffer target) { return 0; }
        };
        IOException failure = assertThrows(IOException.class, () -> new RecoveryJournal(path, storage).scan());
        assertEquals("journal read made no progress", failure.getMessage());
    }

    @Test
    void openWriteDirectoryForceAndTruncateFailuresPropagate() throws Exception {
        RecoveryJournal.FileJournalStorage directoryFailure = new RecoveryJournal.FileJournalStorage() {
            @Override public void createDirectories(Path directory) throws IOException { throw new IOException("directory"); }
        };
        assertEquals("directory", assertThrows(IOException.class,
                () -> new RecoveryJournal(temp.resolve("nested/a"), directoryFailure).append(RECORD)).getMessage());

        RecoveryJournal.FileJournalStorage openFailure = new RecoveryJournal.FileJournalStorage() {
            @Override public FileChannel openAppend(Path path) throws IOException { throw new IOException("open"); }
        };
        assertEquals("open", assertThrows(IOException.class,
                () -> new RecoveryJournal(temp.resolve("open"), openFailure).append(RECORD)).getMessage());

        Path damaged = temp.resolve("damaged");
        new RecoveryJournal(damaged).append(RECORD);
        java.nio.file.Files.write(damaged, new byte[]{1}, java.nio.file.StandardOpenOption.APPEND);
        RecoveryScan scan = new RecoveryJournal(damaged).scan();
        RecoveryJournal.FileJournalStorage truncateFailure = new RecoveryJournal.FileJournalStorage() {
            @Override public void truncate(FileChannel channel, long size) throws IOException { throw new IOException("truncate"); }
        };
        assertEquals("truncate", assertThrows(IOException.class,
                () -> new RecoveryJournal(damaged, truncateFailure).repairInvalidTail(scan)).getMessage());
    }
}
