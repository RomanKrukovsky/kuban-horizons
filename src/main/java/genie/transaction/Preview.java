package genie.transaction;

import genie.wish.WishIntent;
import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Preview of wish effects for player confirmation.
 * Contains visual and textual representation of what will happen.
 */
public class Preview {
    private final String type;
    private final String title;
    private final String description;
    private final int color;
    private final String icon;
    private final long timestamp;
    private final UUID previewId;
    private final WishIntent intent;
    private final BlockPos position;
    private final int affectedBlocks;
    private final List<String> effects;
    private final List<String> warnings;

    private Preview(Builder builder) {
        this.type = builder.type;
        this.title = builder.title;
        this.description = builder.description;
        this.color = builder.color;
        this.icon = builder.icon;
        this.timestamp = builder.timestamp;
        this.previewId = UUID.randomUUID();
        this.intent = builder.intent;
        this.position = builder.position;
        this.affectedBlocks = builder.affectedBlocks;
        this.effects = List.copyOf(builder.effects);
        this.warnings = List.copyOf(builder.warnings);
    }

    /**
     * Get preview type
     */
    public String getType() {
        return type;
    }

    /**
     * Get preview title
     */
    public String getTitle() {
        return title;
    }

    /**
     * Get preview description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Get preview color (ARGB format)
     */
    public int getColor() {
        return color;
    }

    /**
     * Get preview icon
     */
    public String getIcon() {
        return icon;
    }

    /**
     * Get timestamp
     */
    public long getTimestamp() {
        return timestamp;
    }

    /**
     * Get preview ID
     */
    public UUID getPreviewId() {
        return previewId;
    }

    /**
     * Get wish intent
     */
    @Nullable
    public WishIntent getIntent() {
        return intent;
    }

    /**
     * Get position
     */
    public BlockPos getPosition() {
        return position;
    }

    /**
     * Get affected blocks count
     */
    public int getAffectedBlocks() {
        return affectedBlocks;
    }

    /**
     * Get effects list
     */
    public List<String> getEffects() {
        return effects;
    }

    /**
     * Get warnings list
     */
    public List<String> getWarnings() {
        return warnings;
    }

    /**
     * Check if preview has warnings
     */
    public boolean hasWarnings() {
        return !warnings.isEmpty();
    }

    /**
     * Builder pattern for Preview
     */
    public static class Builder {
        private String type = "word";
        private String title = "Wish Preview";
        private String description = "";
        private int color = 0xFFAA00; // Default gold color
        private String icon = "✨";
        private long timestamp = System.currentTimeMillis();
        private WishIntent intent;
        private BlockPos position;
        private int affectedBlocks = 0;
        private final List<String> effects = new ArrayList<>();
        private final List<String> warnings = new ArrayList<>();

        public Builder(WishIntent intent, UUID playerId, BlockPos position) {
            this.intent = intent;
            this.position = position;
        }

        public Builder setType(String type) {
            this.type = type;
            return this;
        }

        public Builder setTitle(String title) {
            this.title = title;
            return this;
        }

        public Builder setDescription(String description) {
            this.description = description;
            return this;
        }

        public Builder setColor(int color) {
            this.color = color;
            return this;
        }

        public Builder setIcon(String icon) {
            this.icon = icon;
            return this;
        }

        public Builder setTimestamp(long timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public Builder setPosition(BlockPos position) {
            this.position = position;
            return this;
        }

        public Builder setAffectedBlocks(int affectedBlocks) {
            this.affectedBlocks = affectedBlocks;
            return this;
        }

        public Builder addEffect(String effect) {
            this.effects.add(effect);
            return this;
        }

        public Builder addWarning(String warning) {
            this.warnings.add(warning);
            return this;
        }

        public Preview build() {
            return new Preview(this);
        }
    }
}
