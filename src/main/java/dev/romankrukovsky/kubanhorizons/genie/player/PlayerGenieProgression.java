package dev.romankrukovsky.kubanhorizons.genie.player;

import dev.romankrukovsky.kubanhorizons.registry.KHAttachments;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/**
 * Система 5-уровневой прогрессии сил игрока-Джиннии и прогресса Желания №1.
 */
public final class PlayerGenieProgression {
    private PlayerGenieProgression() {
    }

    public static void advanceProgress(ServerPlayer player, int amount) {
        PlayerGenieAttachment attachment = player.getData(KHAttachments.PLAYER_GENIE_DATA);
        if (!attachment.isGenie()) {
            return;
        }

        int oldProgress = attachment.getWishProgressPercent();
        int newProgress = Math.min(100, oldProgress + amount);
        attachment.setWishProgressPercent(newProgress);

        player.sendSystemMessage(Component.translatable("message.kubanhorizons.genie.progression.status", newProgress));

        int oldTier = attachment.getTierLevel();
        int newTier = newProgress >= 95 ? 5 : newProgress >= 85 ? 4 : newProgress >= 75 ? 3 : newProgress >= 68 ? 2 : 1;

        if (newTier > oldTier) {
            attachment.setTierLevel(newTier);
            player.sendSystemMessage(Component.translatable("message.kubanhorizons.genie.progression.tier_unlocked", newTier));
        }

        if (newProgress >= 100 && oldProgress < 100) {
            TrueOmnipotenceEnding.triggerEnding((ServerLevel) player.level(), player);
        }
    }

    /**
     * Выполнение Воли Джиннии (Tier V) над блоком или сущностью.
     */
    public static boolean executeGenieWill(ServerLevel level, ServerPlayer player, String commandText) {
        PlayerGenieAttachment attachment = player.getData(KHAttachments.PLAYER_GENIE_DATA);
        if (!attachment.isGenie() || attachment.getTierLevel() < 5) {
            player.sendSystemMessage(Component.translatable("message.kubanhorizons.genie.progression.will_requires_tier5"));
            return false;
        }

        player.sendSystemMessage(Component.translatable("message.kubanhorizons.genie.progression.will_executed", commandText));
        return true;
    }
}
