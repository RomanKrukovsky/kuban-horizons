package dev.romankrukovsky.kubanhorizons.genie.runtime.capability;

import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

/** Стабильный версионированный идентификатор серверной возможности. */
public record CapabilityId(String namespace, String name, int version) {
    private static final Pattern PART = Pattern.compile("[a-z0-9_.-]+");

    public CapabilityId {
        namespace = requirePart(namespace, "namespace");
        name = requirePart(name, "name");
        if (version < 1) {
            throw new IllegalArgumentException("version must be positive");
        }
    }

    public static CapabilityId parse(String value) {
        Objects.requireNonNull(value, "value");
        int colon = value.indexOf(':');
        int at = value.lastIndexOf('@');
        if (colon <= 0 || at <= colon + 1 || at == value.length() - 1) {
            throw new IllegalArgumentException("capability id must be namespace:name@version");
        }
        try {
            return new CapabilityId(value.substring(0, colon), value.substring(colon + 1, at),
                    Integer.parseInt(value.substring(at + 1)));
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("capability version must be an integer", exception);
        }
    }

    public String serialized() {
        return namespace + ":" + name + "@" + version;
    }

    private static String requirePart(String value, String field) {
        Objects.requireNonNull(value, field);
        String normalized = value.toLowerCase(Locale.ROOT);
        if (!normalized.equals(value) || !PART.matcher(value).matches()) {
            throw new IllegalArgumentException(field + " must be a lowercase identifier");
        }
        return value;
    }
}
