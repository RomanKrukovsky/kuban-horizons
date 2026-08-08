package dev.romankrukovsky.kubanhorizons.genie.runtime.policy;

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
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;

/** Атомарное хранилище lifecycle глобальных reversible policy. */
public final class PolicyManifestStore {
    private static final int SCHEMA = 1;
    private final Path directory;

    public PolicyManifestStore(Path directory) {
        this.directory = Objects.requireNonNull(directory, "directory");
    }

    public void save(PolicyManifest manifest) throws IOException {
        Files.createDirectories(directory);
        Path target = directory.resolve(manifest.transactionId() + ".nbt");
        Path temp = directory.resolve("." + manifest.transactionId() + ".tmp");
        try {
            NbtIo.writeCompressed(encode(manifest), temp);
            try (FileChannel channel = FileChannel.open(temp, StandardOpenOption.WRITE)) {
                channel.force(true);
            }
            try {
                Files.move(temp, target, StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException exception) {
                Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temp);
        }
    }

    public Optional<PolicyManifest> load(UUID id) throws IOException {
        Path path = directory.resolve(id + ".nbt");
        return Files.isRegularFile(path) ? Optional.of(decode(NbtIo.readCompressed(path,
                NbtAccounter.create(64 * 1024L)))) : Optional.empty();
    }

    public List<PolicyManifest> list() throws IOException {
        if (!Files.isDirectory(directory)) return List.of();
        List<PolicyManifest> result = new ArrayList<>();
        try (var files = Files.list(directory)) {
            for (Path path : files.filter(file -> file.getFileName().toString().endsWith(".nbt")).toList()) {
                result.add(decode(NbtIo.readCompressed(path, NbtAccounter.create(64 * 1024L))));
            }
        }
        return List.copyOf(result);
    }

    public void remove(UUID id) throws IOException {
        Files.deleteIfExists(directory.resolve(id + ".nbt"));
    }

    private static CompoundTag encode(PolicyManifest value) {
        CompoundTag tag = new CompoundTag();
        tag.putInt("SchemaVersion", SCHEMA);
        tag.putString("TransactionId", value.transactionId().toString());
        tag.putString("ActorId", value.actorId().toString());
        tag.putString("RuleId", value.ruleId());
        tag.putString("Before", value.beforeValue());
        tag.putString("Target", value.targetValue());
        tag.putLong("CreatedSecond", value.createdAt().getEpochSecond());
        tag.putString("State", value.state().name());
        return tag;
    }

    private static PolicyManifest decode(CompoundTag tag) throws IOException {
        try {
            if (tag.getIntOr("SchemaVersion", -1) != SCHEMA) throw new IOException("unsupported policy schema");
            return new PolicyManifest(UUID.fromString(tag.getStringOr("TransactionId", "")),
                    UUID.fromString(tag.getStringOr("ActorId", "")), tag.getStringOr("RuleId", ""),
                    tag.getStringOr("Before", "false"), tag.getStringOr("Target", "false"),
                    Instant.ofEpochSecond(tag.getLongOr("CreatedSecond", 0L)),
                    PolicyManifest.PolicyState.valueOf(tag.getStringOr("State", "PREPARED")));
        } catch (IllegalArgumentException exception) {
            throw new IOException("malformed policy manifest", exception);
        }
    }
}
