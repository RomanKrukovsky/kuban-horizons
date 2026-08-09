package dev.romankrukovsky.kubanhorizons.genie.player;

import dev.romankrukovsky.kubanhorizons.genie.aura.MagicalSignature;
import dev.romankrukovsky.kubanhorizons.registry.KHAttachments;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/**
 * Секретная концовка 100% выполнения Желания №1 («Я хочу стать всемогущим»).
 */
public final class TrueOmnipotenceEnding {
    private TrueOmnipotenceEnding() {
    }

    public static void triggerEnding(ServerLevel level, ServerPlayer player) {
        PlayerGenieAttachment attachment = player.getData(KHAttachments.PLAYER_GENIE_DATA);
        attachment.setWishProgressPercent(100);
        attachment.setTierLevel(5);

        MagicalSignature.cast(level, player.position());

        level.sendParticles(ParticleTypes.END_ROD, player.getX(), player.getY() + 1.0, player.getZ(), 200, 1.5, 2.0, 1.5, 0.2);
        level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, player.getX(), player.getY() + 1.0, player.getZ(), 50, 0.5, 1.0, 0.5, 0.1);

        player.sendSystemMessage(Component.translatable("message.kubanhorizons.genie.ending.wish1_complete"));
        player.sendSystemMessage(Component.translatable("message.kubanhorizons.genie.ending.no_interface_needed"));
        player.sendSystemMessage(Component.translatable("message.kubanhorizons.genie.ending.supreme_djinni"));

        // Игрок сам стал джинном, и прежняя привязка мира больше не нужна.
        // Это единственный игровой путь снять якорь единственности: пока он
        // держится, второй джиннии в мире появиться не может.
        dev.romankrukovsky.kubanhorizons.genie.GenieAnchor.release(level.getServer());
    }
}
