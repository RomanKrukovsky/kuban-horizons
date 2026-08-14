package dev.romankrukovsky.kubanhorizons.genie.vessel;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import org.jspecify.annotations.Nullable;

import java.util.*;

/**
 * SchoolRegistry - Persistent registry managing all VesselSchools in the world.
 *
 * <p>Stores school definitions, handles school lifecycle (creation, dissolution,
 * membership changes), and provides lookup by school ID or member vessel ID.</p>
 *
 * <p>Serialized via CompoundTag in the world's saved data (level.dat).</p>
 */
public final class SchoolRegistry {

    private static final String DATA_NAME = "vessel_schools";
    private static final SchoolRegistry INSTANCE = new SchoolRegistry();

    private final Map<UUID, VesselSchool> schoolsById;
    private final Map<UUID, UUID> vesselToSchool; // vesselId -> schoolId

    public SchoolRegistry() {
        this.schoolsById = new HashMap<>();
        this.vesselToSchool = new HashMap<>();
    }

    // ========== School Management ==========

    /**
     * Creates a new school with the given leader and rule.
     *
     * @return the created school, or null if creation failed
     */
    public @Nullable VesselSchool createSchool(UUID leaderId, SharedRule rule) {
        // Check if leader is already in a school
        if (vesselToSchool.containsKey(leaderId)) {
            return null;
        }

        UUID schoolId = UUID.randomUUID();
        VesselSchool school = new VesselSchool(schoolId, leaderId, rule);

        schoolsById.put(schoolId, school);
        vesselToSchool.put(leaderId, schoolId);

        setDirty();
        return school;
    }

    /**
     * Dissolves a school, removing all members and the school itself.
     *
     * @return true if dissolved successfully
     */
    public boolean dissolveSchool(UUID schoolId) {
        VesselSchool school = schoolsById.remove(schoolId);
        if (school == null) {
            return false;
        }

        // Remove all member mappings
        for (UUID memberId : school.getMemberIds()) {
            vesselToSchool.remove(memberId);
        }

        setDirty();
        return true;
    }

    // ========== Member Management ==========

    /**
     * Adds a vessel to an existing school.
     *
     * @return true if added successfully
     */
    public boolean addMemberToSchool(UUID schoolId, UUID vesselId) {
        VesselSchool school = schoolsById.get(schoolId);
        if (school == null) {
            return false;
        }

        // Check if vessel is already in another school
        if (vesselToSchool.containsKey(vesselId)) {
            return false;
        }

        if (school.addMember(vesselId)) {
            vesselToSchool.put(vesselId, schoolId);
            setDirty();
            return true;
        }
        return false;
    }

    /**
     * Removes a vessel from its school.
     *
     * @return true if removed successfully
     */
    public boolean removeMemberFromSchool(UUID vesselId) {
        UUID schoolId = vesselToSchool.get(vesselId);
        if (schoolId == null) {
            return false;
        }

        VesselSchool school = schoolsById.get(schoolId);
        if (school == null) {
            vesselToSchool.remove(vesselId);
            return false;
        }

        if (school.removeMember(vesselId)) {
            vesselToSchool.remove(vesselId);
            setDirty();
            return true;
        }
        return false;
    }

    /**
     * Transfers leadership of a school.
     *
     * @return true if transfer successful
     */
    public boolean transferLeadership(UUID schoolId, UUID newLeaderId) {
        VesselSchool school = schoolsById.get(schoolId);
        if (school == null) {
            return false;
        }

        if (school.transferLeadership(newLeaderId)) {
            setDirty();
            return true;
        }
        return false;
    }

    // ========== Lookups ==========

    public @Nullable VesselSchool getSchool(UUID schoolId) {
        return schoolsById.get(schoolId);
    }

    public @Nullable VesselSchool getSchoolForVessel(UUID vesselId) {
        UUID schoolId = vesselToSchool.get(vesselId);
        return schoolId != null ? schoolsById.get(schoolId) : null;
    }

    public boolean isVesselInSchool(UUID vesselId) {
        return vesselToSchool.containsKey(vesselId);
    }

    public Collection<VesselSchool> getAllSchools() {
        return Collections.unmodifiableCollection(schoolsById.values());
    }

    public int getSchoolCount() {
        return schoolsById.size();
    }

    // ========== Event Handling ==========

    /**
     * Handles a school event for a specific vessel, propagating to the school if applicable.
     *
     * @return list of affected vessels, or empty list if vessel not in a school
     */
    public List<UUID> handleVesselEvent(UUID vesselId, SchoolEvent event, ServerLevel level) {
        VesselSchool school = getSchoolForVessel(vesselId);
        if (school == null) {
            return Collections.emptyList();
        }

        return school.handleSchoolEvent(event, vesselId, level);
    }

    // ========== Serialization ==========

    private static final String KEY_SCHOOLS = "Schools";
    private static final String KEY_VESSEL_MAP = "VesselMap";

    public CompoundTag save(CompoundTag tag) {
        // Serialize schools
        ListTag schoolsTag = new ListTag();
        for (VesselSchool school : schoolsById.values()) {
            schoolsTag.add(school.save());
        }
        tag.put(KEY_SCHOOLS, schoolsTag);

        // Serialize vessel-to-school mapping
        CompoundTag vesselMapTag = new CompoundTag();
        for (Map.Entry<UUID, UUID> entry : vesselToSchool.entrySet()) {
            vesselMapTag.putString(entry.getKey().toString(), entry.getValue().toString());
        }
        tag.put(KEY_VESSEL_MAP, vesselMapTag);

        return tag;
    }

    /**
     * Loads the registry from saved data.
     */
    public static SchoolRegistry load(CompoundTag tag) {
        SchoolRegistry registry = new SchoolRegistry();

        // Load schools
        if (tag.contains(KEY_SCHOOLS)) {
            ListTag schoolsTag = tag.getListOrEmpty(KEY_SCHOOLS);
            for (int i = 0; i < schoolsTag.size(); i++) {
                CompoundTag schoolTag = schoolsTag.getCompoundOrEmpty(i);
                VesselSchool school = VesselSchool.load(schoolTag);
                if (school != null && school.isValid()) {
                    registry.schoolsById.put(school.getSchoolId(), school);
                }
            }
        }

        // Load vessel-to-school mapping
        if (tag.contains(KEY_VESSEL_MAP)) {
            CompoundTag vesselMapTag = tag.getCompoundOrEmpty(KEY_VESSEL_MAP);
            for (String key : vesselMapTag.keySet()) {
                try {
                    UUID vesselId = UUID.fromString(key);
                    UUID schoolId = UUID.fromString(vesselMapTag.getStringOr(key, ""));
                    registry.vesselToSchool.put(vesselId, schoolId);
                } catch (IllegalArgumentException ignored) {
                    // Skip invalid UUIDs
                }
            }
        }

        return registry;
    }

    // ========== Factory / Access ==========

    public static SchoolRegistry get(MinecraftServer server) {
        return INSTANCE;
    }

    public static SchoolRegistry getInstance() {
        return INSTANCE;
    }

    private void setDirty() {
    }
}
