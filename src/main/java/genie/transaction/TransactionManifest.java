package genie.transaction;

import genie.GenieAnchor;
import genie.wish.WishIntent;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Transaction manifest recording wish execution details.
 * Used for recovery, analysis, and undo operations.
 */
public class TransactionManifest {
    private String transactionId;
    private String type;
    private long timestamp;
    private UUID playerId;
    private ResourceLocation dimension;
    private BlockPos position;
    private WishIntent wishIntent;
    private String anchorId;
    private String oldState;
    private String newState;
    private boolean success;
    private String error;
    private Preview preview;
    private int affectedBlocks;
    private String message;

    public TransactionManifest() {
        this.transactionId = UUID.randomUUID().toString();
        this.timestamp = System.currentTimeMillis();
        this.type = "wish";
        this.success = false;
    }

    public TransactionManifest(WishIntent intent, UUID playerId, ResourceLocation dimension) {
        this();
        this.type = "wish";
        this.wishIntent = intent;
        this.playerId = playerId;
        this.dimension = dimension;
        this.success = false;
    }

    public TransactionManifest(GenieAnchor anchor, String oldState, String newState) {
        this();
        this.type = "anchor_state_change";
        this.anchorId = anchor.getAnchorId();
        this.oldState = oldState;
        this.newState = newState;
        this.success = true;
    }

    /**
     * Get transaction ID
     */
    public String getTransactionId() {
        return transactionId;
    }

    /**
     * Set transaction ID
     */
    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    /**
     * Get transaction type
     */
    public String getType() {
        return type;
    }

    /**
     * Set transaction type
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * Get timestamp
     */
    public long getTimestamp() {
        return timestamp;
    }

    /**
     * Set timestamp
     */
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    /**
     * Get player UUID
     */
    @Nullable
    public UUID getPlayerId() {
        return playerId;
    }

    /**
     * Set player UUID
     */
    public void setPlayerId(UUID playerId) {
        this.playerId = playerId;
    }

    /**
     * Get dimension
     */
    @Nullable
    public ResourceLocation getDimension() {
        return dimension;
    }

    /**
     * Set dimension
     */
    public void setDimension(ResourceLocation dimension) {
        this.dimension = dimension;
    }

    /**
     * Get position
     */
    @Nullable
    public BlockPos getPosition() {
        return position;
    }

    /**
     * Set position
     */
    public void setPosition(BlockPos position) {
        this.position = position;
    }

    /**
     * Get wish intent
     */
    @Nullable
    public WishIntent getWishIntent() {
        return wishIntent;
    }

    /**
     * Set wish intent
     */
    public void setWishIntent(WishIntent wishIntent) {
        this.wishIntent = wishIntent;
    }

    /**
     * Get anchor ID
     */
    @Nullable
    public String getAnchorId() {
        return anchorId;
    }

    /**
     * Set anchor ID
     */
    public void setAnchorId(String anchorId) {
        this.anchorId = anchorId;
    }

    /**
     * Get old state
     */
    @Nullable
    public String getOldState() {
        return oldState;
    }

    /**
     * Set old state
     */
    public void setOldState(String oldState) {
        this.oldState = oldState;
    }

    /**
     * Get new state
     */
    @Nullable
    public String getNewState() {
        return newState;
    }

    /**
     * Set new state
     */
    public void setNewState(String newState) {
        this.newState = newState;
    }

    /**
     * Check if transaction was successful
     */
    public boolean isSuccess() {
        return success;
    }

    /**
     * Set success flag
     */
    public void setSuccess(boolean success) {
        this.success = success;
    }

    /**
     * Get error message
     */
    @Nullable
    public String getError() {
        return error;
    }

    /**
     * Set error message
     */
    public void setError(String error) {
        this.error = error;
    }

    /**
     * Get preview
     */
    @Nullable
    public Preview getPreview() {
        return preview;
    }

    /**
     * Set preview
     */
    public void setPreview(Preview preview) {
        this.preview = preview;
    }

    /**
     * Get affected blocks count
     */
    public int getAffectedBlocks() {
        return affectedBlocks;
    }

    /**
     * Set affected blocks count
     */
    public void setAffectedBlocks(int affectedBlocks) {
        this.affectedBlocks = affectedBlocks;
    }

    /**
     * Get message
     */
    @Nullable
    public String getMessage() {
        return message;
    }

    /**
     * Set message
     */
    public void setMessage(String message) {
        this.message = message;
    }

    /**
     * Serialize to string
     */
    @Override
    public String toString() {
        return String.format("TransactionManifest[id=%s, type=%s, success=%s, blocks=%d]",
            transactionId, type, success, affectedBlocks);
    }
}
