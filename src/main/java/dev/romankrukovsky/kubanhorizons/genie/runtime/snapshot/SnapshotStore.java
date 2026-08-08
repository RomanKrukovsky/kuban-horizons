package dev.romankrukovsky.kubanhorizons.genie.runtime.snapshot;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;

/** Manifest-last хранилище: снимок становится видимым только после атомарной публикации. */
public final class SnapshotStore {
    public static final long DEFAULT_WORLD_QUOTA_BYTES = 512L * 1024L * 1024L;
    private static final long MAX_SNAPSHOT_UNCOMPRESSED_BYTES = 128L * 1024L * 1024L;

    private final Path directory;
    private final long quotaBytes;

    public SnapshotStore(Path directory) {
        this(directory, DEFAULT_WORLD_QUOTA_BYTES);
    }

    public SnapshotStore(Path directory, long quotaBytes) {
        this.directory = Objects.requireNonNull(directory, "directory");
        if (quotaBytes < 1024L) {
            throw new IllegalArgumentException("quotaBytes is too small");
        }
        this.quotaBytes = quotaBytes;
    }

    public void publish(RegionSnapshot snapshot) throws IOException {
        Objects.requireNonNull(snapshot, "snapshot");
        Files.createDirectories(directory);
        Path target = path(snapshot.id().value());
        if (Files.exists(target)) {
            throw new IOException("snapshot already exists");
        }
        Path temporary = directory.resolve("." + snapshot.id().value() + ".tmp");
        try {
            NbtIo.writeCompressed(SnapshotCodec.encode(snapshot), temporary);
            forceFile(temporary);
            long projected = Math.addExact(usedBytes(), Files.size(temporary));
            if (projected > quotaBytes) {
                throw new IOException("snapshot quota exceeded");
            }
            moveAtomically(temporary, target);
            forceDirectory();
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    public Optional<RegionSnapshot> load(UUID id) throws IOException {
        Path file = path(Objects.requireNonNull(id, "id"));
        if (!Files.isRegularFile(file)) {
            return Optional.empty();
        }
        return Optional.of(SnapshotCodec.decode(NbtIo.readCompressed(file,
                NbtAccounter.create(MAX_SNAPSHOT_UNCOMPRESSED_BYTES))));
    }

    public Optional<RegionSnapshot> findByName(String name) throws IOException {
        for (RegionSnapshot snapshot : list()) {
            if (snapshot.id().name().equals(name)) {
                return Optional.of(snapshot);
            }
        }
        return Optional.empty();
    }

    public Optional<RegionSnapshot> findOwnedByName(UUID ownerId, String name) throws IOException {
        Objects.requireNonNull(ownerId, "ownerId");
        Objects.requireNonNull(name, "name");
        return list().stream()
                .filter(snapshot -> snapshot.ownerId().equals(ownerId) && snapshot.id().name().equals(name))
                .findFirst();
    }

    public List<RegionSnapshot> listOwned(UUID ownerId) throws IOException {
        Objects.requireNonNull(ownerId, "ownerId");
        return list().stream()
                .filter(snapshot -> snapshot.ownerId().equals(ownerId))
                .filter(snapshot -> !snapshot.id().name().startsWith("u_")
                        && !snapshot.id().name().startsWith("t_"))
                .toList();
    }

    public void remove(UUID id) throws IOException {
        if (Files.deleteIfExists(path(Objects.requireNonNull(id, "id")))) {
            forceDirectory();
        }
    }

    public List<RegionSnapshot> list() throws IOException {
        if (!Files.isDirectory(directory)) {
            return List.of();
        }
        List<RegionSnapshot> snapshots = new ArrayList<>();
        try (var files = Files.list(directory)) {
            for (Path file : files.filter(path -> path.getFileName().toString().endsWith(".nbt")).toList()) {
                snapshots.add(SnapshotCodec.decode(NbtIo.readCompressed(file,
                        NbtAccounter.create(MAX_SNAPSHOT_UNCOMPRESSED_BYTES))));
            }
        }
        snapshots.sort(Comparator.comparing(RegionSnapshot::capturedAt));
        return List.copyOf(snapshots);
    }

    public long usedBytes() throws IOException {
        if (!Files.isDirectory(directory)) {
            return 0L;
        }
        try (var files = Files.list(directory)) {
            return files.filter(Files::isRegularFile).mapToLong(file -> {
                try {
                    return Files.size(file);
                } catch (IOException exception) {
                    throw new SizeReadFailure(exception);
                }
            }).sum();
        } catch (SizeReadFailure failure) {
            throw failure.ioException;
        }
    }

    private Path path(UUID id) {
        return directory.resolve(id + ".nbt");
    }

    private static void forceFile(Path file) throws IOException {
        try (FileChannel channel = FileChannel.open(file, StandardOpenOption.WRITE)) {
            channel.force(true);
        }
    }

    private void forceDirectory() throws IOException {
        try (FileChannel channel = FileChannel.open(directory, StandardOpenOption.READ)) {
            channel.force(true);
        } catch (UnsupportedOperationException ignored) {
            // Некоторые файловые системы не поддерживают fsync каталога.
        }
    }

    private static void moveAtomically(Path source, Path target) throws IOException {
        try {
            Files.move(source, target, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException exception) {
            Files.move(source, target);
        }
    }

    private static final class SizeReadFailure extends RuntimeException {
        private final IOException ioException;

        private SizeReadFailure(IOException ioException) {
            this.ioException = ioException;
        }
    }
}
