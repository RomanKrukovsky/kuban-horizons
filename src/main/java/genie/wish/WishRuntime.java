package genie.wish;

import genie.GenieStateSnapshot;
import genie.capabilities.IGenieContainer;
import genie.genie.KubanGenie;
import genie.preview.PreviewService;
import genie.vessel.VesselKind;
import genie.vessel.VesselPull;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nullable;

/**
 * Main wish runtime orchestrator.
 * Coordinates wish execution, vessel binding, and genie interaction.
 */
public class WishRuntime {

    private final Player player;
    private final KubanGenie genie;
    private final SafeStrongWishRuntime safeRuntime;
    private final WishParser wishParser;
    private final PreviewService previewService;

    public WishRuntime(Player player, @Nullable KubanGenie genie) {
        this.player = player;
        this.genie = genie;
        this.safeRuntime = new SafeStrongWishRuntime();
        this.wishParser = new WishParser();
        this.previewService = new PreviewService();
    }

    /**
     * Attempt to execute a wish
     */
    public WishResult executeWish(String wishText) {
        // Parse wish
        WishIntent intent = wishParser.parseWish(wishText);
        if (intent == null) {
            return new WishResult(false, "invalid_wish", Component.translatable("wish.invalid"));
        }

        // Check vessel binding
        if (genie != null && genie.getVesselKind() != VesselKind.LAMP) {
            // Non-lamp vessels have different wish handling
            return executeBoundWish(intent, wishText);
        }

        // Standard wish execution
        BlockPos origin = player.blockPosition();
        return safeRuntime.executeWish(this, wishText, origin);
    }

    /**
     * Execute wish bound to a vessel
     */
    private WishResult executeBoundWish(WishIntent intent, String wishText) {
        BlockPos origin = player.blockPosition();

        // Check vessel laws
        if (genie != null && genie.getVesselConfinement() != null) {
            if (!genie.getVesselConfinement().isWishAllowed(genie, wishText)) {
                return new WishResult(false, "vessel_law_violation",
                    Component.translatable("wish.vessel.law_violation"));
            }

            // Apply vessel laws
            String modifiedWish = genie.getVesselConfinement().applyWishLaws(genie, wishText);
            if (modifiedWish == null) {
                return new WishResult(false, "wish_blocked",
                    Component.translatable("wish.blocked.by_law"));
            }
            wishText = modifiedWish;
        }

        // Execute with vessel-specific handling
        return safeRuntime.executeWish(this, wishText, origin);
    }

    /**
     * Bind a vessel to player
     */
    public boolean bindVessel(VesselKind kind) {
        if (player instanceof ServerPlayer serverPlayer) {
            return VesselPull.bindVesselToPlayer(serverPlayer, new ItemStack(genie.vessel.GenieLampItem.INSTANCE), kind);
        }
        return false;
    }

    /**
     * Get the player
     */
    public Player getPlayer() {
        return player;
    }

    /**
     * Get the genie
     */
    @Nullable
    public KubanGenie getGenie() {
        return genie;
    }

    /**
     * Get the level
     */
    public Level getLevel() {
        return player.level();
    }

    /**
     * Get the safe runtime
     */
    public SafeStrongWishRuntime getSafeRuntime() {
        return safeRuntime;
    }

    /**
     * Get the wish parser
     */
    public WishParser getWishParser() {
        return wishParser;
    }

    /**
     * Get the preview service
     */
    public PreviewService getPreviewService() {
        return previewService;
    }

    /**
     * Wish execution result
     */
    public static class WishResult {
        private final boolean success;
        private final String status;
        private final Component message;

        public WishResult(boolean success, String status, Component message) {
            this.success = success;
            this.status = status;
            this.message = message;
        }

        public boolean isSuccess() {
            return success;
        }

        public String getStatus() {
            return status;
        }

        public Component getMessage() {
            return message;
        }
    }
}
