package dev.romankrukovsky.kubanhorizons.genie.runtime.selection;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

/** Сервер принимает две точки отдельно и сам строит неизменяемую область. */
public final class SelectionService {
    private static final double MAX_REACH_SQUARED = 64.0D;
    private final Map<UUID, FirstPoint> firstPoints = new HashMap<>();
    private final Map<UUID, RegionSelection> completed = new HashMap<>();

    public synchronized SelectionUpdate select(ServerPlayer player, BlockPos pos) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(pos, "pos");
        if (player.distanceToSqr(Vec3.atCenterOf(pos)) > MAX_REACH_SQUARED
                || !player.level().mayInteract(player, pos)) {
            throw new IllegalArgumentException("selection point is out of reach or protected");
        }
        String dimension = player.level().dimension().identifier().toString();
        FirstPoint first = firstPoints.remove(player.getUUID());
        if (first == null || !first.dimension().equals(dimension)) {
            BlockPos point = pos.immutable();
            firstPoints.put(player.getUUID(), new FirstPoint(dimension, point));
            completed.remove(player.getUUID());
            return new SelectionUpdate(point, Optional.empty());
        }
        RegionSelection selection = new RegionSelection(dimension, first.pos(), pos);
        if (selection.min().getY() < player.level().getMinY()
                || selection.max().getY() >= player.level().getMaxY()) {
            throw new IllegalArgumentException("selection exceeds build height");
        }
        completed.put(player.getUUID(), selection);
        return new SelectionUpdate(pos.immutable(), Optional.of(selection));
    }

    public synchronized RegionSelection requireCompleted(UUID playerId) {
        RegionSelection selection = completed.get(Objects.requireNonNull(playerId, "playerId"));
        if (selection == null) {
            throw new IllegalStateException("select two corners with the magic mirror first");
        }
        return selection;
    }

    public synchronized void setCompleted(UUID playerId, RegionSelection selection) {
        Objects.requireNonNull(playerId, "playerId");
        completed.put(playerId, Objects.requireNonNull(selection, "selection"));
        firstPoints.remove(playerId);
    }

    public synchronized void clear(UUID playerId) {
        firstPoints.remove(playerId);
        completed.remove(playerId);
    }

    private record FirstPoint(String dimension, BlockPos pos) {
    }

    public record SelectionUpdate(BlockPos point, Optional<RegionSelection> completedSelection) {
        public SelectionUpdate {
            Objects.requireNonNull(point, "point");
            Objects.requireNonNull(completedSelection, "completedSelection");
        }
    }
}
