package genie.vessel;

/**
 * Types of magic vessels that can contain a genie.
 * Each vessel type has unique properties and limitations.
 */
public enum VesselKind {
    /**
     * Magic lamp - classic vessel that grants wishes.
     * Can contain one genie at a time.
     * Provides light source when active.
     */
    LAMP,

    /**
     * Magic mirror - acts as smartphone replacement.
     * Can show visions, communicate across distances.
     * Can contain one genie.
     */
    MIRROR,

    /**
     * Magic ring - wearable vessel.
     * Can contain one genie.
     * Provides passive benefits when worn.
     */
    RING,

    /**
     * Kuban jug - decorative vessel with special properties.
     * Can contain one genie.
     * Can store liquids and grant water-related wishes.
     */
    JUG,

    /**
     * Music box - vessel that stores wishes in melodies.
     * Can contain one genie.
     * Plays music when active.
     */
    MUSIC_BOX
}
