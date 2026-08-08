package dev.romankrukovsky.kubanhorizons.genie.runtime.capability;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Закрытый реестр известных операций. Неизвестные имена никогда не исполняются. */
public final class CapabilityRegistry {
    public static final CapabilityId SNAPSHOT_CREATE = CapabilityId.parse("genie:snapshot.create@1");
    public static final CapabilityId SNAPSHOT_INSPECT = CapabilityId.parse("genie:snapshot.inspect@1");
    public static final CapabilityId SNAPSHOT_PREVIEW_RESTORE = CapabilityId.parse("genie:snapshot.preview_restore@1");
    public static final CapabilityId REGION_RESTORE = CapabilityId.parse("genie:region.restore@1");

    private final Map<CapabilityId, CapabilityDescriptor> descriptors = new LinkedHashMap<>();

    public CapabilityRegistry() {
        register(new CapabilityDescriptor(SNAPSHOT_CREATE, CapabilityCategory.SNAPSHOT,
                CapabilityRisk.LOW, 256, false, false,
                java.util.Set.of("name", "dimension", "min", "max"), java.util.Set.of()));
        register(new CapabilityDescriptor(SNAPSHOT_INSPECT, CapabilityCategory.READ_ONLY,
                CapabilityRisk.NONE, 0, false, false,
                java.util.Set.of("name"), java.util.Set.of()));
        register(new CapabilityDescriptor(SNAPSHOT_PREVIEW_RESTORE, CapabilityCategory.READ_ONLY,
                CapabilityRisk.NONE, 256, false, false,
                java.util.Set.of("name"), java.util.Set.of()));
        register(new CapabilityDescriptor(REGION_RESTORE, CapabilityCategory.RESTORE,
                CapabilityRisk.ELEVATED, 256, true, true,
                java.util.Set.of(), java.util.Set.of()));
    }

    public void register(CapabilityDescriptor descriptor) {
        Objects.requireNonNull(descriptor, "descriptor");
        if (descriptors.putIfAbsent(descriptor.id(), descriptor) != null) {
            throw new IllegalArgumentException("duplicate capability " + descriptor.id().serialized());
        }
    }

    public Optional<CapabilityDescriptor> find(CapabilityId id) {
        return Optional.ofNullable(descriptors.get(Objects.requireNonNull(id, "id")));
    }

    public Collection<CapabilityDescriptor> descriptors() {
        return ListCopy.copy(descriptors.values());
    }

    private static final class ListCopy {
        private ListCopy() {
        }

        private static <T> Collection<T> copy(Collection<T> values) {
            return java.util.List.copyOf(values);
        }
    }
}
