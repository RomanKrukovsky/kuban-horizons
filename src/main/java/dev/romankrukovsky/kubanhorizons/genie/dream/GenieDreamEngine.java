package dev.romankrukovsky.kubanhorizons.genie.dream;

import dev.romankrukovsky.kubanhorizons.entity.KubanGenie;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/** Движок снов Джиннии во время сна игрока в кровати (Genie Dream Engine). */
public final class GenieDreamEngine {
    private GenieDreamEngine() {
    }

    public static void enterDreamState(KubanGenie genie, ServerPlayer player) {
        player.sendSystemMessage(Component.translatable("message.kubanhorizons.genie.dream.vision"));
    }
}
