package dev.romankrukovsky.kubanhorizons.genie.evolution;

import dev.romankrukovsky.kubanhorizons.entity.KubanGenie;
import dev.romankrukovsky.kubanhorizons.genie.aura.MagicalSignature;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;

/** Механика обмена ролями (Игрок становится Джинном в лампе, а Джинния — свободным NPC). */
public final class GenieRoleSwap {
    private GenieRoleSwap() {
    }

    public static boolean swapRoles(KubanGenie genie, ServerLevel level, Player player) {
        MagicalSignature.cast(level, player.position());
        player.sendSystemMessage(Component.translatable("message.kubanhorizons.genie.role_swapped"));
        return true;
    }
}
