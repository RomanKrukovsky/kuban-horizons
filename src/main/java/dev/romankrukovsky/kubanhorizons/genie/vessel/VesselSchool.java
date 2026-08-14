package dev.romankrukovsky.kubanhorizons.genie.vessel;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * VesselSchool - A magical school of vessels (genie lamps) bound together by a shared rule.
 *
 * <p>Schools coordinate behavior modifiers across member vessels. A school contains
 * 3-7 members with one designated leader. The shared rule determines how the school
 * reacts to events affecting any member.</p>
 *
 * <p>Supported shared rules:
 * <ul>
 *   <li>{@link SharedRule#PROTECT_LEADER} - All members prioritize protecting the leader</li>
 *   <li>{@link SharedRule#SHARED_TEMPERAMENT} - Temperament changes propagate to all members</li>
 *   <li>{@link SharedRule#CONTRACT_MANDATORY} - Contract obligations are shared across the school</li>
 * </ul>
 * </p>
 */
public final class VesselSchool {

    private static final int MIN_MEMBERS = 3;
    private static final int MAX_MEMBERS = 7;

    private final UUID schoolId;
    private UUID leaderId;
    private final Set<UUID> memberIds;
    private SharedRule sharedRule;

    // Behavior modifiers applied on school events
    private final Map<SchoolEvent, BehaviorModifier> activeModifiers;

    public VesselSchool(UUID schoolId, UUID leaderId, SharedRule sharedRule) {
        this.schoolId = schoolId;
        this.leaderId = leaderId;
        this.memberIds = new HashSet<>();
        this.sharedRule = sharedRule;
        this.activeModifiers = new EnumMap<>(SchoolEvent.class);

        // Leader is always a member
        this.memberIds.add(leaderId);
    }

    // ========== Getters ==========

    public UUID getSchoolId() {
        return schoolId;
    }

    public UUID getLeaderId() {
        return leaderId;
    }

    public Set<UUID> getMemberIds() {
        return Collections.unmodifiableSet(memberIds);
    }

    public int getMemberCount() {
        return memberIds.size();
    }

    public SharedRule getSharedRule() {
        return sharedRule;
    }

    public boolean isLeader(UUID vesselId) {
        return leaderId.equals(vesselId);
    }

    public boolean isMember(UUID vesselId) {
        return memberIds.contains(vesselId);
    }

    // ========== Member Management ==========

    /**
     * Adds a member to the school.
     *
     * @return true if added successfully, false if school is full or already contains member
     */
    public boolean addMember(UUID vesselId) {
        if (memberIds.size() >= MAX_MEMBERS) {
            return false;
        }
        if (memberIds.contains(vesselId)) {
            return false;
        }
        memberIds.add(vesselId);
        return true;
    }

    /**
     * Removes a member from the school.
     *
     * @return true if removed, false if not a member or is the leader
     */
    public boolean removeMember(UUID vesselId) {
        if (vesselId.equals(leaderId)) {
            return false; // Cannot remove leader
        }
        return memberIds.remove(vesselId);
    }

    /**
     * Changes the school leader.
     *
     * @return true if leadership transferred successfully
     */
    public boolean transferLeadership(UUID newLeaderId) {
        if (!memberIds.contains(newLeaderId)) {
            return false;
        }
        this.leaderId = newLeaderId;
        return true;
    }

    // ========== Shared Rule Management ==========

    public void setSharedRule(SharedRule newRule) {
        this.sharedRule = newRule;
    }

    // ========== Behavior Modifiers ==========

    /**
     * Applies a behavior modifier triggered by a school event.
     */
    public void applyModifier(SchoolEvent event, BehaviorModifier modifier) {
        activeModifiers.put(event, modifier);
    }

    /**
     * Gets the active modifier for a given event, if any.
     */
    public @Nullable BehaviorModifier getModifier(SchoolEvent event) {
        return activeModifiers.get(event);
    }

    /**
     * Clears all active modifiers.
     */
    public void clearModifiers() {
        activeModifiers.clear();
    }

    /**
     * Checks if a modifier is currently active for an event.
     */
    public boolean hasActiveModifier(SchoolEvent event) {
        return activeModifiers.containsKey(event);
    }

    // ========== Event Handling ==========

    /**
     * Handles a school-wide event, applying appropriate behavior modifiers
     * based on the shared rule.
     *
     * @param event the school event that occurred
     * @param affectedVessel the vessel directly affected by the event
     * @param level the server level for context
     * @return list of vessels that should receive behavior modifications
     */
    public List<UUID> handleSchoolEvent(SchoolEvent event, UUID affectedVessel, ServerLevel level) {
        List<UUID> affectedVessels = new ArrayList<>();

        switch (sharedRule) {
            case PROTECT_LEADER -> {
                if (event == SchoolEvent.THREAT_DETECTED) {
                    // All members prioritize protecting the leader
                    if (!affectedVessel.equals(leaderId)) {
                        affectedVessels.add(leaderId);
                        applyModifier(event, BehaviorModifier.PROTECTIVE_FORMATION);
                    }
                }
                if (event == SchoolEvent.LEADER_DAMAGED) {
                    // All members converge on leader's position
                    affectedVessels.addAll(memberIds);
                    applyModifier(event, BehaviorModifier.DEFENSIVE_STANCE);
                }
            }
            case SHARED_TEMPERAMENT -> {
                if (event == SchoolEvent.TEMPERAMENT_SHIFT) {
                    // Propagate temperament change to all members
                    affectedVessels.addAll(memberIds);
                    applyModifier(event, BehaviorModifier.TEMPERAMENT_SYNC);
                }
            }
            case CONTRACT_MANDATORY -> {
                if (event == SchoolEvent.CONTRACT_BREACH) {
                    // All members share the contract obligation
                    affectedVessels.addAll(memberIds);
                    applyModifier(event, BehaviorModifier.CONTRACT_ENFORCEMENT);
                }
                if (event == SchoolEvent.CONTRACT_FULFILLED) {
                    // All members receive fulfillment benefits
                    affectedVessels.addAll(memberIds);
                    applyModifier(event, BehaviorModifier.REWARD_DISTRIBUTION);
                }
            }
        }

        return affectedVessels;
    }

    // ========== Serialization ==========

    private static final String KEY_SCHOOL_ID = "SchoolId";
    private static final String KEY_LEADER_ID = "LeaderId";
    private static final String KEY_MEMBERS = "Members";
    private static final String KEY_SHARED_RULE = "SharedRule";
    private static final String KEY_MODIFIERS = "Modifiers";

    /**
     * Serializes the school to a CompoundTag.
     */
    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();

        tag.putString(KEY_SCHOOL_ID, schoolId.toString());
        tag.putString(KEY_LEADER_ID, leaderId.toString());
        tag.putString(KEY_SHARED_RULE, sharedRule.name());

        // Serialize members
        ListTag membersTag = new ListTag();
        for (UUID memberId : memberIds) {
            CompoundTag memberTag = new CompoundTag();
            memberTag.putString("UUID", memberId.toString());
            membersTag.add(memberTag);
        }
        tag.put(KEY_MEMBERS, membersTag);

        // Serialize active modifiers
        CompoundTag modifiersTag = new CompoundTag();
        for (Map.Entry<SchoolEvent, BehaviorModifier> entry : activeModifiers.entrySet()) {
            modifiersTag.putString(entry.getKey().name(), entry.getValue().name());
        }
        tag.put(KEY_MODIFIERS, modifiersTag);

        return tag;
    }

    /**
     * Deserializes a school from a CompoundTag.
     */
    public static @Nullable VesselSchool load(CompoundTag tag) {
        try {
            UUID schoolId = UUID.fromString(tag.getString(KEY_SCHOOL_ID));
            UUID leaderId = UUID.fromString(tag.getString(KEY_LEADER_ID));
            SharedRule sharedRule = SharedRule.valueOf(tag.getString(KEY_SHARED_RULE));

            VesselSchool school = new VesselSchool(schoolId, leaderId, sharedRule);

            // Load members
            if (tag.contains(KEY_MEMBERS, Tag.TAG_LIST)) {
                ListTag membersTag = tag.getList(KEY_MEMBERS, Tag.TAG_COMPOUND);
                for (int i = 0; i < membersTag.size(); i++) {
                    CompoundTag memberTag = membersTag.getCompound(i);
                    UUID memberId = UUID.fromString(memberTag.getString("UUID"));
                    school.memberIds.add(memberId);
                }
            }

            // Load modifiers
            if (tag.contains(KEY_MODIFIERS, Tag.TAG_COMPOUND)) {
                CompoundTag modifiersTag = tag.getCompound(KEY_MODIFIERS);
                for (String key : modifiersTag.getAllKeys()) {
                    try {
                        SchoolEvent event = SchoolEvent.valueOf(key);
                        BehaviorModifier modifier = BehaviorModifier.valueOf(modifiersTag.getString(key));
                        school.activeModifiers.put(event, modifier);
                    } catch (IllegalArgumentException ignored) {
                        // Skip invalid enum values
                    }
                }
            }

            return school;
        } catch (Exception e) {
            return null;
        }
    }

    // ========== Validation ==========

    /**
     * Validates that the school meets minimum requirements (3-7 members).
     */
    public boolean isValid() {
        return memberIds.size() >= MIN_MEMBERS
                && memberIds.size() <= MAX_MEMBERS
                && memberIds.contains(leaderId);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VesselSchool that = (VesselSchool) o;
        return schoolId.equals(that.schoolId);
    }

    @Override
    public int hashCode() {
        return schoolId.hashCode();
    }

    @Override
    public String toString() {
        return "VesselSchool{" +
                "schoolId=" + schoolId +
                ", leaderId=" + leaderId +
                ", members=" + memberIds.size() +
                ", rule=" + sharedRule +
                '}';
    }
}
