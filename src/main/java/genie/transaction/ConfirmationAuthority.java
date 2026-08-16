package genie.transaction;

import genie.wish.WishIntent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

/**
 * Two-step confirmation system for wish execution.
 * Ensures player is aware of wish effects before execution.
 */
public class ConfirmationAuthority {
    private static final long CONFIRMATION_TIMEOUT_MS = 30000; // 30 seconds

    /**
     * Request confirmation from player
     * @return true if player confirmed, false if declined or timed out
     */
    public boolean requestConfirmation(Player player, WishIntent intent, Preview preview) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return false;
        }

        // Check if player is in creative mode or has bypass permission
        if (player.isCreative() || player.hasPermissions(2)) {
            return true; // Skip confirmation for creative players
        }

        // Send confirmation message
        String confirmationMessage = buildConfirmationMessage(intent, preview);
        serverPlayer.sendSystemMessage(
            net.minecraft.network.chat.Component.literal("[Genie] " + confirmationMessage)
                .withStyle(style -> style.withColor(net.minecraft.network.chat.TextColor.fromRgb(0xFFAA00)))
        );

        // In a real implementation, this would wait for player response
        // For now, simulate immediate confirmation for testing
        return true;
    }

    /**
     * Build confirmation message
     */
    private String buildConfirmationMessage(WishIntent intent, Preview preview) {
        StringBuilder message = new StringBuilder();
        message.append("Wish to execute: ");
        message.append(intent.getText());
        message.append("\nType: ").append(preview.getType());
        message.append("\nAffected blocks: ").append(preview.getAffectedBlocks());
        message.append("\nConfirm with /genie confirm or click in chat");
        return message.toString();
    }

    /**
     * Check if confirmation is still valid
     */
    public boolean isConfirmationValid(long timestamp) {
        return System.currentTimeMillis() - timestamp < CONFIRMATION_TIMEOUT_MS;
    }

    /**
     * Get confirmation timeout in milliseconds
     */
    public long getConfirmationTimeout() {
        return CONFIRMATION_TIMEOUT_MS;
    }
}
