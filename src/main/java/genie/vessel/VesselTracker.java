package genie.vessel;

import genie.genie.KubanGenie;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Tracks vessel ownership and location across the world.
 * Persists vessel data between game sessions.
 */
public class VesselTracker extends SavedData {

    private static final String DATA_NAME = "KubanGenieVessels";

    private final Map<UUID, VesselData> vesselRegistry = new HashMap<>();
    private final Map<BlockPos, UUID> blockPositionRegistry = new HashMap<>();
    private final Map<UUID, UUID> ownerToVesselRegistry = new HashMap<>();

    public VesselTracker() {
        super();
    }

    /**
     * Register a new vessel
     */
    public void registerVessel(UUID vesselId, VesselKind kind, VesselSchool school, @Nullable UUID ownerId) {
        VesselData data = new VesselData(vesselId, kind, school, ownerId);
        vesselRegistry.put(vesselId, data);
        if (ownerId != null) {
            ownerToVesselRegistry.put(ownerId, vesselId);
        }
        this.setDirty();
    }

    /**
     * Bind a vessel to a block position
     */
    public void bindVesselToBlock(UUID vesselId, BlockPos pos) {
        blockPositionRegistry.put(pos, vesselId);
        this.setDirty();
    }

    /**
     * Set vessel owner
     */
    public void setVesselOwner(UUID vesselId, UUID ownerId) {
        VesselData data = vesselRegistry.get(vesselId);
        if (data != null) {
            if (data.ownerId != null) {
                ownerToVesselRegistry.remove(data.ownerId);
            }
            data.ownerId = ownerId;
            if (ownerId != null) {
                ownerToVesselRegistry.put(ownerId, vesselId);
            }
            this.setDirty();
        }
    }

    /**
     * Get vessel data by ID
     */
    @Nullable
    public VesselData getVesselData(UUID vesselId) {
        return vesselRegistry.get(vesselId);
    }

    /**
     * Get vessel ID by block position
     */
    @Nullable
    public UUID getVesselIdAtPosition(BlockPos pos) {
        return blockPositionRegistry.get(pos);
    }

    /**
     * Get vessel owner ID
     */
    @Nullable
    public UUID getVesselOwner(UUID vesselId) {
        VesselData data = vesselRegistry.get(vesselId);
        return data != null ? data.ownerId : null;
    }

    /**
     * Check if a player owns a vessel
     */
    public boolean isPlayerOwner(Player player, UUID vesselId) {
        UUID ownerId = getVesselOwner(vesselId);
        return ownerId != null && ownerId.equals(player.getUUID());
    }

    /**
     * Get vessel ID owned by player
     */
    @Nullable
    public UUID getVesselOwnedByPlayer(UUID playerId) {
        return ownerToVesselRegistry.get(playerId);
    }

    /**
     * Update vessel location
     */
    public void updateVesselLocation(UUID vesselId, BlockPos pos) {
        VesselData data = vesselRegistry.get(vesselId);
        if (data != null) {
            data.lastKnownPosition = pos;
            this.setDirty();
        }
    }

    /**
     * Remove a vessel from tracking
     */
    public void removeVessel(UUID vesselId) {
        VesselData data = vesselRegistry.remove(vesselId);
        if (data != null && data.ownerId != null) {
            ownerToVesselRegistry.remove(data.ownerId);
        }
        // Remove from position registry
        blockPositionRegistry.values().removeIf(id -> id.equals(vesselId));
        this.setDirty();
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        CompoundTag vesselsTag = new CompoundTag();

        for (Map.Entry<UUID, VesselData> entry : vesselRegistry.entrySet()) {
            CompoundTag vesselTag = new CompoundTag();
            vesselTag.putString("kind", entry.getValue().kind.name());
            vesselTag.putString("school", entry.getValue().school.name());
            if (entry.getValue().ownerId != null) {
                vesselTag.putUUID("owner", entry.getValue().ownerId);
            }
            if (entry.getValue().lastKnownPosition != null) {
                CompoundTag posTag = new CompoundTag();
                posTag.putInt("x", entry.getValue().lastKnownPosition.getX());
                posTag.putInt("y", entry.getValue().lastKnownPosition.getY());
                posTag.putInt("z", entry.getValue().lastKnownPosition.getZ());
                vesselTag.put("position", posTag);
            }
            vesselsTag.put(entry.getKey().toString(), vesselTag);
        }

        tag.put("vessels", vesselsTag);
        return tag;
    }

    public static VesselTracker load(CompoundTag tag) {
        VesselTracker tracker = new VesselTracker();
        CompoundTag vesselsTag = tag.getCompound("vessels");

        for (String key : vesselsTag.getAllKeys()) {
            try {
                UUID vesselId = UUID.fromString(key);
                CompoundTag vesselTag = vesselsTag.getCompound(key);
                VesselKind kind = VesselKind.valueOf(vesselTag.getString("kind"));
                VesselSchool school = VesselSchool.valueOf(vesselTag.getString("school"));
                UUID ownerId = vesselTag.contains("owner") ? vesselTag.getUUID("owner") : null;

                VesselData data = new VesselData(vesselId, kind, school, ownerId);
                tracker.vesselRegistry.put(vesselId, data);

                if (ownerId != null) {
                    tracker.ownerToVesselRegistry.put(ownerId, vesselId);
                }

                if (vesselTag.contains("position")) {
                    CompoundTag posTag = vesselTag.getCompound("position");
                    int x = posTag.getInt("x");
                    int y = posTag.getInt("y");
                    int z = posTag.getInt("z");
                    data.lastKnownPosition = new BlockPos(x, y, z);
                }
            } catch (Exception e) {
                // Skip invalid entries
            }
        }

        return tracker;
    }

    /**
     * Get the global vessel tracker for a world
     */
    @Nonnull
    public static VesselTracker get(Level level) {
        if (level instanceof ServerLevel serverLevel) {
            return serverLevel.getDataStorage().computeIfAbsent(
                VesselTracker::load,
                VesselTracker::new,
                DATA_NAME
            );
        }
        return new VesselTracker();
    }

    /**
     * Vessel data container
     */
    public static class VesselData {
        public final UUID vesselId;
        public final VesselKind kind;
        public final VesselSchool school;
        public UUID ownerId;
        public BlockPos lastKnownPosition;

        public VesselData(UUID vesselId, VesselKind kind, VesselSchool school, @Nullable UUID ownerId) {
            this.vesselId = vesselId;
            this.kind = kind;
            this.school = school;
            this.ownerId = ownerId;
            this.lastKnownPosition = BlockPos.ZERO;
        }
    }
}
