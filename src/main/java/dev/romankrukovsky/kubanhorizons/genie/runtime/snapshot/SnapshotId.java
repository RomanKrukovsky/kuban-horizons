package dev.romankrukovsky.kubanhorizons.genie.runtime.snapshot;

import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;

/** Идентификатор снимка: отображаемое имя отделено от уникального UUID. */
public record SnapshotId(UUID value, String name) {
    private static final Pattern NAME = Pattern.compile("[a-z0-9][a-z0-9_.-]{0,47}");

    public SnapshotId {
        Objects.requireNonNull(value, "value");
        Objects.requireNonNull(name, "name");
        if (!name.equals(name.toLowerCase(Locale.ROOT)) || !NAME.matcher(name).matches()) {
            throw new IllegalArgumentException("snapshot name must be a safe lowercase identifier");
        }
    }
}
