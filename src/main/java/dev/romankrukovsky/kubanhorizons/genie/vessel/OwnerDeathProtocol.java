package dev.romankrukovsky.kubanhorizons.genie.vessel;

import dev.romankrukovsky.kubanhorizons.entity.KubanGenie;
import dev.romankrukovsky.kubanhorizons.genie.aura.MagicalSignature;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;

/** Протокол выбора реагирования при гибели хозяина (Owner Death Choice Protocol). */
public final class OwnerDeathProtocol {
    private OwnerDeathProtocol() {
    }

    public static void handleOwnerDeath(KubanGenie genie, ServerLevel level, Player player) {
        MagicalSignature.cast(level, player.position());
        player.sendSystemMessage(Component.translatable("message.kubanhorizons.genie.death_choice"));
    }
}
