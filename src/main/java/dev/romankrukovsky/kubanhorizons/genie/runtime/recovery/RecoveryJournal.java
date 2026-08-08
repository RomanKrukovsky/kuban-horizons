package dev.romankrukovsky.kubanhorizons.genie.runtime.recovery;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public final class RecoveryJournal {
    private static final int MAGIC = 0x4B48524A;
    private static final short V1 = 1;
    private static final short V2 = 2;
    private static final int CHECKSUM_BYTES = 32;
    private static final int HEADER_BYTES = 10;
    private static final int MAX_PAYLOAD_BYTES = 4 * 1024;
    private static final int MAX_DIMENSION_BYTES = 256;
    private static final int DIGEST_BYTES = 64;
    private static final int NO_PROGRESS_LIMIT = 8;

    private final Path file;
    private final JournalStorage storage;

    public RecoveryJournal(Path file) {
        this(file, new FileJournalStorage());
    }

    RecoveryJournal(Path file, Durability durability) {
        this(file, new FileJournalStorage() {
            @Override public void force(FileChannel channel) throws IOException { durability.force(channel); }
        });
    }

    RecoveryJournal(Path file, JournalStorage storage) {
        this.file = Objects.requireNonNull(file, "file");
        this.storage = Objects.requireNonNull(storage, "storage");
    }

    public void append(RecoveryRecord record) throws IOException {
        Objects.requireNonNull(record, "record");
        if (record.scope().isEmpty()) throw new IllegalArgumentException("new recovery records require affected scope");
        RecoveryScan current = scan();
        if (current.discardedInvalidTail()) throw new IOException("invalid recovery journal tail must be repaired before append");
        Path parent = file.toAbsolutePath().getParent();
        if (parent != null) storage.createDirectories(parent);
        byte[] payload = encodeV2(record);
        ByteBuffer frame = frame(V2, payload);
        try (FileChannel channel = storage.openAppend(file)) {
            writeFully(channel, frame);
            storage.force(channel);
        }
    }

    public RecoveryScan scan() throws IOException {
        if (!storage.exists(file) || storage.size(file) == 0) return new RecoveryScan(List.of(), false, 0);
        List<RecoveryRecord> records = new ArrayList<>();
        Map<UUID, Long> sequences = new HashMap<>();
        long proven = 0;
        try (FileChannel channel = storage.openRead(file)) {
            while (true) {
                ByteBuffer header = ByteBuffer.allocate(HEADER_BYTES).order(ByteOrder.BIG_ENDIAN);
                int first = storage.read(channel, header);
                if (first < 0) return new RecoveryScan(records, false, proven);
                if (!readFully(channel, header)) return new RecoveryScan(records, true, proven);
                header.flip();
                int magic = header.getInt(); short version = header.getShort(); int length = header.getInt();
                if (magic != MAGIC || (version != V1 && version != V2) || length < 0 || length > MAX_PAYLOAD_BYTES)
                    return new RecoveryScan(records, true, proven);
                ByteBuffer payloadBuffer = ByteBuffer.allocate(length);
                ByteBuffer checksumBuffer = ByteBuffer.allocate(CHECKSUM_BYTES);
                if (!readFully(channel, payloadBuffer) || !readFully(channel, checksumBuffer))
                    return new RecoveryScan(records, true, proven);
                byte[] payload = payloadBuffer.array();
                if (!MessageDigest.isEqual(checksumBuffer.array(), checksum(payload)))
                    return new RecoveryScan(records, true, proven);
                RecoveryRecord record;
                try { record = decode(payload, version); }
                catch (IOException | RuntimeException exception) { return new RecoveryScan(records, true, proven); }
                Long previous = sequences.get(record.transactionId());
                if (previous != null && record.sequence() <= previous) return new RecoveryScan(records, true, proven);
                sequences.put(record.transactionId(), record.sequence());
                records.add(record);
                proven = channel.position();
            }
        }
    }

    public void repairInvalidTail(RecoveryScan scan) throws IOException {
        Objects.requireNonNull(scan, "scan");
        if (!scan.discardedInvalidTail()) return;
        long size = storage.size(file);
        if (scan.provenLength() < 0 || scan.provenLength() >= size) throw new IOException("stale or implausible recovery scan boundary");
        RecoveryScan current = scan();
        if (!current.equals(scan)) throw new IOException("stale recovery scan");
        try (FileChannel channel = storage.openTruncate(file)) {
            storage.truncate(channel, scan.provenLength());
            storage.force(channel);
        }
    }

    private static ByteBuffer frame(short version, byte[] payload) {
        byte[] digest = checksum(payload);
        ByteBuffer frame = ByteBuffer.allocate(HEADER_BYTES + payload.length + CHECKSUM_BYTES).order(ByteOrder.BIG_ENDIAN);
        frame.putInt(MAGIC).putShort(version).putInt(payload.length).put(payload).put(digest).flip();
        return frame;
    }

    private static byte[] encodeV2(RecoveryRecord record) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (DataOutputStream output = new DataOutputStream(bytes)) {
            encodeCommon(output, record);
            AffectedScope scope = record.scope().orElseThrow();
            byte[] dimension = scope.dimension().getBytes(StandardCharsets.UTF_8);
            if (dimension.length == 0 || dimension.length > MAX_DIMENSION_BYTES) throw new IOException("invalid dimension length");
            output.writeShort(dimension.length); output.write(dimension);
            output.writeInt(scope.minChunkX()); output.writeInt(scope.minChunkZ());
            output.writeInt(scope.maxChunkX()); output.writeInt(scope.maxChunkZ());
        }
        if (bytes.size() > MAX_PAYLOAD_BYTES) throw new IOException("Recovery record exceeds maximum payload size");
        return bytes.toByteArray();
    }

    private static void encodeCommon(DataOutputStream output, RecoveryRecord record) throws IOException {
        output.writeLong(record.transactionId().getMostSignificantBits()); output.writeLong(record.transactionId().getLeastSignificantBits());
        output.writeLong(record.sequence()); output.writeLong(record.recordedAt().getEpochSecond()); output.writeInt(record.recordedAt().getNano());
        output.writeByte(record.state().ordinal()); byte[] digest = record.payloadDigest().getBytes(StandardCharsets.US_ASCII);
        output.writeByte(digest.length); output.write(digest);
    }

    private static RecoveryRecord decode(byte[] payload, short version) throws IOException {
        try (DataInputStream input = new DataInputStream(new ByteArrayInputStream(payload))) {
            UUID id = new UUID(input.readLong(), input.readLong()); long sequence = input.readLong();
            Instant at = Instant.ofEpochSecond(input.readLong(), input.readInt()); int ordinal = input.readUnsignedByte();
            int digestLength = input.readUnsignedByte();
            if (digestLength != DIGEST_BYTES) throw new IOException("invalid digest length");
            byte[] digest = input.readNBytes(digestLength);
            if (digest.length != digestLength || ordinal >= TransactionState.values().length) throw new IOException("malformed payload");
            String digestText = new String(digest, StandardCharsets.US_ASCII);
            if (version == V1) {
                if (input.read() != -1) throw new IOException("trailing bytes");
                return new RecoveryRecord(id, sequence, at, TransactionState.values()[ordinal], digestText);
            }
            int dimensionLength = input.readUnsignedShort();
            if (dimensionLength == 0 || dimensionLength > MAX_DIMENSION_BYTES) throw new IOException("invalid dimension length");
            byte[] dimensionBytes = input.readNBytes(dimensionLength);
            if (dimensionBytes.length != dimensionLength) throw new EOFException("truncated dimension");
            String dimension;
            try { dimension = StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(dimensionBytes)).toString(); }
            catch (CharacterCodingException exception) { throw new IOException("malformed UTF-8 dimension", exception); }
            AffectedScope scope = new AffectedScope(dimension, input.readInt(), input.readInt(), input.readInt(), input.readInt());
            if (input.read() != -1) throw new IOException("trailing bytes");
            return new RecoveryRecord(id, sequence, at, TransactionState.values()[ordinal], digestText, scope);
        }
    }

    private void writeFully(FileChannel channel, ByteBuffer buffer) throws IOException {
        int zeroes = 0;
        while (buffer.hasRemaining()) {
            int written = storage.write(channel, buffer);
            if (written < 0) throw new EOFException("Journal channel closed during write");
            zeroes = written == 0 ? zeroes + 1 : 0;
            if (zeroes >= NO_PROGRESS_LIMIT) throw new IOException("journal write made no progress");
        }
    }

    private boolean readFully(FileChannel channel, ByteBuffer buffer) throws IOException {
        int zeroes = 0;
        while (buffer.hasRemaining()) {
            int read = storage.read(channel, buffer);
            if (read < 0) return false;
            zeroes = read == 0 ? zeroes + 1 : 0;
            if (zeroes >= NO_PROGRESS_LIMIT) throw new IOException("journal read made no progress");
        }
        return true;
    }

    private static byte[] checksum(byte[] payload) {
        try { return MessageDigest.getInstance("SHA-256").digest(payload); }
        catch (NoSuchAlgorithmException exception) { throw new IllegalStateException(exception); }
    }

    @FunctionalInterface interface Durability { void force(FileChannel channel) throws IOException; }

    interface JournalStorage {
        void createDirectories(Path directory) throws IOException;
        boolean exists(Path path);
        long size(Path path) throws IOException;
        FileChannel openRead(Path path) throws IOException;
        FileChannel openAppend(Path path) throws IOException;
        FileChannel openTruncate(Path path) throws IOException;
        int read(FileChannel channel, ByteBuffer target) throws IOException;
        int write(FileChannel channel, ByteBuffer source) throws IOException;
        void truncate(FileChannel channel, long size) throws IOException;
        void force(FileChannel channel) throws IOException;
    }

    static class FileJournalStorage implements JournalStorage {
        public void createDirectories(Path directory) throws IOException { Files.createDirectories(directory); }
        public boolean exists(Path path) { return Files.exists(path); }
        public long size(Path path) throws IOException { return Files.size(path); }
        public FileChannel openRead(Path path) throws IOException { return FileChannel.open(path, StandardOpenOption.READ); }
        public FileChannel openAppend(Path path) throws IOException { return FileChannel.open(path, StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.APPEND); }
        public FileChannel openTruncate(Path path) throws IOException { return FileChannel.open(path, StandardOpenOption.WRITE); }
        public int read(FileChannel channel, ByteBuffer target) throws IOException { return channel.read(target); }
        public int write(FileChannel channel, ByteBuffer source) throws IOException { return channel.write(source); }
        public void truncate(FileChannel channel, long size) throws IOException { channel.truncate(size); }
        public void force(FileChannel channel) throws IOException { channel.force(true); }
    }
}
