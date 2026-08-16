package genie.transaction;

import com.google.gson.*;
import genie.wish.WishIntent;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Type;
import java.util.UUID;

/**
 * Individual entry in the causality ledger.
 * Records a single transaction or event.
 */
public class CausalLedgerEntry {
    private String entryId;
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
    private String transactionId;
    private int blockCount;
    private String message;

    public CausalLedgerEntry() {
        this.entryId = UUID.randomUUID().toString();
        this.timestamp = System.currentTimeMillis();
    }

    public CausalLedgerEntry(TransactionManifest manifest) {
        this.entryId = UUID.randomUUID().toString();
        this.timestamp = manifest.getTimestamp();
        this.type = manifest.getType();
        this.playerId = manifest.getPlayerId();
        this.dimension = manifest.getDimension();
        this.position = manifest.getPosition();
        this.wishIntent = manifest.getWishIntent();
        this.anchorId = manifest.getAnchorId();
        this.oldState = manifest.getOldState();
        this.newState = manifest.getNewState();
        this.success = manifest.isSuccess();
        this.error = manifest.getError();
        this.transactionId = manifest.getTransactionId();
        this.blockCount = manifest.getAffectedBlocks();
        this.message = manifest.getMessage();
    }

    /**
     * Get entry ID
     */
    public String getEntryId() {
        return entryId;
    }

    /**
     * Get entry type
     */
    public String getType() {
        return type;
    }

    /**
     * Set entry type
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
     * Check if entry indicates success
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
     * Get transaction ID
     */
    @Nullable
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
     * Get block count
     */
    public int getBlockCount() {
        return blockCount;
    }

    /**
     * Set block count
     */
    public void setBlockCount(int blockCount) {
        this.blockCount = blockCount;
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
     * Serializer for Gson
     */
    public static class Serializer implements JsonSerializer<CausalLedgerEntry>, JsonDeserializer<CausalLedgerEntry> {
        @Override
        public JsonElement serialize(CausalLedgerEntry entry, Type typeOfSrc, JsonSerializationContext context) {
            JsonObject obj = new JsonObject();
            obj.addProperty("entryId", entry.getEntryId());
            obj.addProperty("type", entry.getType());
            obj.addProperty("timestamp", entry.getTimestamp());

            if (entry.getPlayerId() != null) {
                obj.addProperty("playerId", entry.getPlayerId().toString());
            }

            if (entry.getDimension() != null) {
                obj.addProperty("dimension", entry.getDimension().toString());
            }

            if (entry.getPosition() != null) {
                JsonObject pos = new JsonObject();
                pos.addProperty("x", entry.getPosition().getX());
                pos.addProperty("y", entry.getPosition().getY());
                pos.addProperty("z", entry.getPosition().getZ());
                obj.add("position", pos);
            }

            if (entry.getWishIntent() != null) {
                JsonObject intent = new JsonObject();
                intent.addProperty("text", entry.getWishIntent().getText());
                intent.addProperty("type", entry.getWishIntent().getWishType().name());
                obj.add("wishIntent", intent);
            }

            obj.addProperty("anchorId", entry.getAnchorId());
            obj.addProperty("oldState", entry.getOldState());
            obj.addProperty("newState", entry.getNewState());
            obj.addProperty("success", entry.isSuccess());
            obj.addProperty("error", entry.getError());
            obj.addProperty("transactionId", entry.getTransactionId());
            obj.addProperty("blockCount", entry.getBlockCount());
            obj.addProperty("message", entry.getMessage());

            return obj;
        }

        @Override
        public CausalLedgerEntry deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            JsonObject obj = json.getAsJsonObject();
            CausalLedgerEntry entry = new CausalLedgerEntry();

            entry.entryId = obj.get("entryId").getAsString();
            entry.type = obj.get("type").getAsString();
            entry.timestamp = obj.get("timestamp").getAsLong();

            if (obj.has("playerId")) {
                entry.playerId = UUID.fromString(obj.get("playerId").getAsString());
            }

            if (obj.has("dimension")) {
                entry.dimension = new ResourceLocation(obj.get("dimension").getAsString());
            }

            if (obj.has("position")) {
                JsonObject pos = obj.getAsJsonObject("position");
                entry.position = new BlockPos(
                    pos.get("x").getAsInt(),
                    pos.get("y").getAsInt(),
                    pos.get("z").getAsInt()
                );
            }

            if (obj.has("wishIntent")) {
                JsonObject intent = obj.getAsJsonObject("wishIntent");
                // WishIntent deserialization would go here
                // entry.wishIntent = context.deserialize(intent, WishIntent.class);
            }

            if (obj.has("anchorId")) {
                entry.anchorId = obj.get("anchorId").getAsString();
            }

            if (obj.has("oldState")) {
                entry.oldState = obj.get("oldState").getAsString();
            }

            if (obj.has("newState")) {
                entry.newState = obj.get("newState").getAsString();
            }

            entry.success = obj.get("success").getAsBoolean();

            if (obj.has("error")) {
                entry.error = obj.get("error").getAsString();
            }

            if (obj.has("transactionId")) {
                entry.transactionId = obj.get("transactionId").getAsString();
            }

            entry.blockCount = obj.get("blockCount").getAsInt();

            if (obj.has("message")) {
                entry.message = obj.get("message").getAsString();
            }

            return entry;
        }
    }
}
