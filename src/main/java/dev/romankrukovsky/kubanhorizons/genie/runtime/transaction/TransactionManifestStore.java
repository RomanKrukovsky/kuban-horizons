package dev.romankrukovsky.kubanhorizons.genie.runtime.transaction;

import dev.romankrukovsky.kubanhorizons.genie.runtime.selection.RegionSelection;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;

/** Атомарно публикует recovery-манифест до выдачи права на мутацию. */
public final class TransactionManifestStore {
    private static final int SCHEMA_VERSION = 1;
    private final Path directory;

    public TransactionManifestStore(Path directory) {
        this.directory = Objects.requireNonNull(directory, "directory");
    }

    public void publish(TransactionManifest manifest) throws IOException {
        Files.createDirectories(directory);
        Path target = path(manifest.transactionId());
        if (Files.exists(target)) {
            throw new IOException("transaction manifest already exists");
        }
        Path temporary = directory.resolve("." + manifest.transactionId() + ".tmp");
        try {
            NbtIo.writeCompressed(encode(manifest), temporary);
            try (FileChannel channel = FileChannel.open(temporary, StandardOpenOption.WRITE)) {
                channel.force(true);
            }
            try {
                Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException exception) {
                Files.move(temporary, target);
            }
            try (FileChannel channel = FileChannel.open(directory, StandardOpenOption.READ)) {
                channel.force(true);
            } catch (UnsupportedOperationException ignored) {
                // fsync каталога недоступен на части файловых систем.
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    public Optional<TransactionManifest> load(UUID transactionId) throws IOException {
        Path file = path(transactionId);
        return Files.isRegularFile(file)
                ? Optional.of(decode(NbtIo.readCompressed(file, NbtAccounter.unlimitedHeap())))
                : Optional.empty();
    }

    public List<TransactionManifest> list() throws IOException {
        if (!Files.isDirectory(directory)) {
            return List.of();
        }
        List<TransactionManifest> manifests = new ArrayList<>();
        try (var files = Files.list(directory)) {
            for (Path file : files.filter(path -> path.getFileName().toString().endsWith(".nbt")).toList()) {
                manifests.add(decode(NbtIo.readCompressed(file, NbtAccounter.unlimitedHeap())));
            }
        }
        return List.copyOf(manifests);
    }

    public boolean referencesSnapshot(UUID snapshotId) throws IOException {
        Objects.requireNonNull(snapshotId, "snapshotId");
        return list().stream().anyMatch(manifest -> manifest.targetSnapshotId().equals(snapshotId)
                || manifest.beforeImageId().equals(snapshotId));
    }

    public void remove(UUID transactionId) throws IOException {
        if (Files.deleteIfExists(path(transactionId))) {
            try (FileChannel channel = FileChannel.open(directory, StandardOpenOption.READ)) {
                channel.force(true);
            } catch (UnsupportedOperationException ignored) {
                // fsync каталога недоступен на части файловых систем.
            }
        }
    }

    private Path path(UUID transactionId) {
        return directory.resolve(Objects.requireNonNull(transactionId, "transactionId") + ".nbt");
    }

    private static CompoundTag encode(TransactionManifest manifest) {
        CompoundTag tag = new CompoundTag();
        tag.putInt("SchemaVersion", SCHEMA_VERSION);
        tag.putString("TransactionId", manifest.transactionId().toString());
        tag.putString("ActorId", manifest.actorId().toString());
        tag.putString("TargetSnapshotId", manifest.targetSnapshotId().toString());
        tag.putString("BeforeImageId", manifest.beforeImageId().toString());
        tag.putString("Dimension", manifest.selection().dimension());
        tag.putLong("Min", manifest.selection().min().asLong());
        tag.putLong("Max", manifest.selection().max().asLong());
        tag.putString("TargetDigest", manifest.targetDigest());
        tag.putString("BeforeDigest", manifest.beforeDigest());
        tag.putString("PreviewDigest", manifest.previewDigest());
        tag.putLong("CreatedSecond", manifest.createdAt().getEpochSecond());
        tag.putInt("CreatedNano", manifest.createdAt().getNano());
        return tag;
    }

    private static TransactionManifest decode(CompoundTag tag) throws IOException {
        if (tag.getIntOr("SchemaVersion", -1) != SCHEMA_VERSION) {
            throw new IOException("unsupported transaction manifest schema");
        }
        try {
            return new TransactionManifest(
                    UUID.fromString(required(tag, "TransactionId")),
                    UUID.fromString(required(tag, "ActorId")),
                    UUID.fromString(required(tag, "TargetSnapshotId")),
                    UUID.fromString(required(tag, "BeforeImageId")),
                    new RegionSelection(required(tag, "Dimension"),
                            BlockPos.of(tag.getLongOr("Min", Long.MIN_VALUE)),
                            BlockPos.of(tag.getLongOr("Max", Long.MIN_VALUE))),
                    required(tag, "TargetDigest"), required(tag, "BeforeDigest"),
                    required(tag, "PreviewDigest"),
                    Instant.ofEpochSecond(tag.getLongOr("CreatedSecond", 0L), tag.getIntOr("CreatedNano", 0))
            );
        } catch (IllegalArgumentException exception) {
            throw new IOException("malformed transaction manifest", exception);
        }
    }

    private static String required(CompoundTag tag, String field) throws IOException {
        String value = tag.getStringOr(field, "");
        if (value.isEmpty()) {
            throw new IOException("manifest is missing " + field);
        }
        return value;
    }
}
