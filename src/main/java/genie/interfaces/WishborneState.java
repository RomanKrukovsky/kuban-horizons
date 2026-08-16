package genie.interfaces;

/**
 * Interface representing the wishborne state of an entity.
 * Wishborne entities have states that replace traditional HP systems.
 */
public interface WishborneState {

    /**
     * Gets the current wishborne state
     * @return The current state
     */
    WishborneStateEnum getState();

    /**
     * Sets the wishborne state
     * @param state The new state
     */
    void setState(WishborneStateEnum state);

    /**
     * Checks if the entity is in a manifested state
     * @return true if manifested
     */
    boolean isManifested();

    /**
     * Checks if the entity can be damaged
     * @return true if can be damaged
     */
    boolean canTakeDamage();

    /**
     * Gets the anchor level (0-100)
     * @return The anchor level
     */
    int getAnchorLevel();

    /**
     * Sets the anchor level
     * @param level The new anchor level (0-100)
     */
    void setAnchorLevel(int level);

    /**
     * Enum representing the four wishborne states
     */
    enum WishborneStateEnum {
        /**
         * Entity is fully manifested in the world
         */
        MANIFESTED,

        /**
         * Entity is dispersed (partially intangible)
         */
        DISPERSED,

        /**
         * Entity is sealed (contained within vessel)
         */
        SEALED,

        /**
         * Entity is banished (removed from world)
         */
        BANISHED
    }
}