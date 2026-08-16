package genie.transaction;

import genie.GenieAnchor;
import genie.wish.WishIntent;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Comprehensive report of wish execution results.
 * Contains detailed information about what happened during wish execution.
 */
public class TransactionReport {
    private boolean success;
    private String transactionId;
    private String message;
    private String error;
    private long timestamp;
    private UUID playerId;
    private ResourceLocation dimension;
    private BlockPos position;
    private WishIntent wishIntent;
    private String anchorId;
    private int affectedBlocks;
    private int affectedEntities;
    private int affectedChunks;
    private List<String> effects;
    private List<String> warnings;
    private List<String> changes;

    public TransactionReport() {
        this.success = false;
        this.timestamp = System.currentTimeMillis();
        this.effects = new ArrayList<>();
        this.warnings = new ArrayList<>();
        this.changes = new ArrayList<>();
    }

    /**
     * Create a successful report
     */
    public static TransactionReport success(String message) {
        TransactionReport report = new TransactionReport();
        report.success = true;
        report.message = message;
        return report;
    }

    /**
     * Create a failed report
     */
    public static TransactionReport failed(String error) {
        TransactionReport report = new TransactionReport();
        report.success = false;
        report.error = error;
        return report;
    }

    /**
     * Add an effect to the report
     */
    public void addEffect(String effect) {
        effects.add(effect);
    }

    /**
     * Add a warning to the report
     */
    public void addWarning(String warning) {
        warnings.add(warning);
    }

    /**
     * Add a change to the report
     */
    public void addChange(String change) {
        changes.add(change);
    }

    /**
     * Check if report indicates success
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
     * Get error
     */
    @Nullable
    public String getError() {
        return error;
    }

    /**
     * Set error
     */
    public void setError(String error) {
        this.error = error;
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
     * Get player ID
     */
    @Nullable
    public UUID getPlayerId() {
        return playerId;
    }

    /**
     * Set player ID
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
     * Get affected entities count
     */
    public int getAffectedEntities() {
        return affectedEntities;
    }

    /**
     * Set affected entities count
     */
    public void setAffectedEntities(int affectedEntities) {
        this.affectedEntities = affectedEntities;
    }

    /**
     * Get affected chunks count
     */
    public int getAffectedChunks() {
        return affectedChunks;
    }

    /**
     * Set affected chunks count
     */
    public void setAffectedChunks(int affectedChunks) {
        this.affectedChunks = affectedChunks;
    }

    /**
     * Get all effects
     */
    public List<String> getEffects() {
        return effects;
    }

    /**
     * Get all warnings
     */
    public List<String> getWarnings() {
        return warnings;
    }

    /**
     * Get all changes
     */
    public List<String> getChanges() {
        return changes;
    }

    /**
     * Format report as string
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== Transaction Report ===\n");
        sb.append("ID: ").append(transactionId).append("\n");
        sb.append("Status: ").append(success ? "SUCCESS" : "FAILED").append("\n");
        sb.append("Message: ").append(message != null ? message : "N/A").append("\n");

        if (error != null) {
            sb.append("Error: ").append(error).append("\n");
        }

        sb.append("\n=== Details ===\n");
        sb.append("Player: ").append(playerId != null ? playerId.toString() : "N/A").append("\n");
        sb.append("Dimension: ").append(dimension != null ? dimension.toString() : "N/A").append("\n");
        sb.append("Position: ").append(position != null ? position.toString() : "N/A").append("\n");
        sb.append("Anchor: ").append(anchorId != null ? anchorId : "N/A").append("\n");
        sb.append("Blocks: ").append(affectedBlocks).append("\n");
        sb.append("Entities: ").append(affectedEntities).append("\n");
        sb.append("Chunks: ").append(affectedChunks).append("\n");

        if (!effects.isEmpty()) {
            sb.append("\n=== Effects ===\n");
            effects.forEach(effect -> sb.append("- ").append(effect).append("\n"));
        }

        if (!warnings.isEmpty()) {
            sb.append("\n=== Warnings ===\n");
            warnings.forEach(warning -> sb.append("- ").append(warning).append("\n"));
        }

        if (!changes.isEmpty()) {
            sb.append("\n=== Changes ===\n");
            changes.forEach(change -> sb.append("- ").append(change).append("\n"));
        }

        return sb.toString();
    }
}
