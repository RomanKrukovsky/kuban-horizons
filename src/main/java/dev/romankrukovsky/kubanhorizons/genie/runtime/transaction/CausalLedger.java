package dev.romankrukovsky.kubanhorizons.genie.runtime.transaction;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** Простой долговечный append-only индекс завершённых причинных операций. */
public final class CausalLedger {
    private static final int MAX_LINE_BYTES = 2048;
    private final Path file;

    public CausalLedger(Path file) {
        this.file = Objects.requireNonNull(file, "file");
    }

    public synchronized void append(CausalLedgerEntry entry) throws IOException {
        Objects.requireNonNull(entry, "entry");
        Path parent = file.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        String line = entry.transactionId() + "\t" + entry.actorId() + "\t" + entry.targetSnapshotId()
                + "\t" + entry.beforeImageId() + "\t" + entry.dimension() + "\t"
                + entry.committedAt() + "\t" + entry.outcome() + "\n";
        byte[] bytes = line.getBytes(StandardCharsets.UTF_8);
        if (bytes.length > MAX_LINE_BYTES) {
            throw new IOException("ledger entry exceeds limit");
        }
        try (FileChannel channel = FileChannel.open(file, StandardOpenOption.CREATE,
                StandardOpenOption.WRITE, StandardOpenOption.APPEND)) {
            ByteBuffer buffer = ByteBuffer.wrap(bytes);
            while (buffer.hasRemaining()) {
                channel.write(buffer);
            }
            channel.force(true);
        }
    }

    public synchronized List<CausalLedgerEntry> readAll() throws IOException {
        if (!Files.isRegularFile(file)) {
            return List.of();
        }
        List<CausalLedgerEntry> entries = new ArrayList<>();
        for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
            if (line.isBlank()) {
                continue;
            }
            String[] fields = line.split("\\t", -1);
            if (fields.length != 7) {
                throw new IOException("malformed causal ledger");
            }
            try {
                entries.add(new CausalLedgerEntry(UUID.fromString(fields[0]), UUID.fromString(fields[1]),
                        UUID.fromString(fields[2]), UUID.fromString(fields[3]), fields[4],
                        java.time.Instant.parse(fields[5]), TransactionOutcome.valueOf(fields[6])));
            } catch (IllegalArgumentException exception) {
                throw new IOException("malformed causal ledger", exception);
            }
        }
        return List.copyOf(entries);
    }

    public synchronized java.util.Optional<CausalLedgerEntry> find(UUID transactionId) throws IOException {
        return readAll().stream().filter(entry -> entry.transactionId().equals(transactionId)).findFirst();
    }
}
