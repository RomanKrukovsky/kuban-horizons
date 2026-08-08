package dev.romankrukovsky.kubanhorizons.genie.evolution;

import dev.romankrukovsky.kubanhorizons.entity.KubanGenie;
import dev.romankrukovsky.kubanhorizons.genie.aura.MagicalSignature;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import dev.romankrukovsky.kubanhorizons.registry.KHAttachments;
import dev.romankrukovsky.kubanhorizons.genie.player.PlayerGenieAttachment;

/** Механика обмена ролями (Игрок становится Джинном в лампе, а Джинния — свободным NPC). */
public final class GenieRoleSwap {
    private GenieRoleSwap() {
    }

    public static boolean swapRoles(KubanGenie genie, ServerLevel level, Player player) {
        if (!(player instanceof ServerPlayer serverPlayer) || !genie.isOwnedBy(player)) {
            return false;
        }
        PlayerGenieAttachment attachment = serverPlayer.getData(KHAttachments.PLAYER_GENIE_DATA);
        attachment.setGenie(true);
        attachment.setStage(PlayerGenieAttachment.Stage.FULL_GENIE);
        attachment.setWishProgressPercent(Math.max(attachment.getWishProgressPercent(), 63));
        attachment.setTierLevel(Math.max(attachment.getTierLevel(), 2));
        attachment.setMasterUUID(null);
        attachment.setVesselCreated(false);
        genie.releaseOwner();
        serverPlayer.getAbilities().mayfly = true;
        serverPlayer.getAbilities().flying = true;
        serverPlayer.onUpdateAbilities();
        MagicalSignature.cast(level, player.position());
        player.sendSystemMessage(Component.translatable("message.kubanhorizons.genie.role_swapped"));
        return true;
    }
}
