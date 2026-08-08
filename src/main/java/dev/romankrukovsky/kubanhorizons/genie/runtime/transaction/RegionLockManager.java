package dev.romankrukovsky.kubanhorizons.genie.runtime.transaction;

import dev.romankrukovsky.kubanhorizons.genie.runtime.selection.RegionSelection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/** Исключает одновременные пересекающиеся изменения одного измерения. */
public final class RegionLockManager {
    private final Map<UUID, RegionSelection> active = new HashMap<>();

    public synchronized boolean acquire(UUID transactionId, RegionSelection selection) {
        Objects.requireNonNull(transactionId, "transactionId");
        Objects.requireNonNull(selection, "selection");
        if (active.containsKey(transactionId) || active.values().stream().anyMatch(held -> overlaps(held, selection))) {
            return false;
        }
        active.put(transactionId, selection);
        return true;
    }

    public synchronized void release(UUID transactionId) {
        active.remove(Objects.requireNonNull(transactionId, "transactionId"));
    }

    private static boolean overlaps(RegionSelection left, RegionSelection right) {
        return left.dimension().equals(right.dimension())
                && left.min().getX() <= right.max().getX() && right.min().getX() <= left.max().getX()
                && left.min().getY() <= right.max().getY() && right.min().getY() <= left.max().getY()
                && left.min().getZ() <= right.max().getZ() && right.min().getZ() <= left.max().getZ();
    }
}
