package dev.romankrukovsky.kubanhorizons.genie.player;

import dev.romankrukovsky.kubanhorizons.genie.aura.MagicalSignature;
import dev.romankrukovsky.kubanhorizons.registry.KHAttachments;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.UUID;

/**
 * Менеджер системы «Хозяин — Сосуд — Джинния» для игрока-Джиннии.
 */
public final class GenieMasterManager {
    private GenieMasterManager() {
    }

    public enum WishReaction {
        FULFILL,
        INTERPRET,
        WARN,
        REFUSE,
        LOOPHOLE
    }

    /**
     * Потирание лампы игрока другим сущностью/игроком.
     */
    public static boolean summonGeniePlayer(ServerLevel level, Player master, ServerPlayer geniePlayer) {
        PlayerGenieAttachment attachment = geniePlayer.getData(KHAttachments.PLAYER_GENIE_DATA);
        if (!attachment.isGenie()) {
            return false;
        }

        attachment.setMasterUUID(master.getUUID());
        // Потирание — уже пользование, а не только желание после него: иначе
        // хозяин мог бы вызывать слугу и молчать, не платя за это тишиной.
        dev.romankrukovsky.kubanhorizons.genie.vessel.VesselLaw.markUsed(level, geniePlayer);

        geniePlayer.sendSystemMessage(Component.translatable("message.kubanhorizons.genie.vessel.summon_countdown"));

        // Телепортация перед Хозяином
        geniePlayer.teleportTo(level, master.getX() + master.getLookAngle().x * 2.0, master.getY() + 0.5,
                master.getZ() + master.getLookAngle().z * 2.0, java.util.Set.of(), master.getYRot(), master.getXRot(), false);

        MagicalSignature.cast(level, geniePlayer.position());
        level.sendParticles(ParticleTypes.WITCH, geniePlayer.getX(), geniePlayer.getY() + 1.0, geniePlayer.getZ(),
                60, 0.5, 1.0, 0.5, 0.1);

        geniePlayer.sendSystemMessage(Component.translatable("message.kubanhorizons.genie.vessel.new_rule_master"));
        master.sendSystemMessage(Component.translatable("message.kubanhorizons.genie.vessel.master_acquired", geniePlayer.getName().getString()));

        return true;
    }

    /**
     * Обработка ответа игрока-Джиннии на желание Хозяина.
     */
    public static void respondToMasterWish(ServerLevel level, ServerPlayer geniePlayer, String wishText, WishReaction reaction) {
        PlayerGenieAttachment attachment = geniePlayer.getData(KHAttachments.PLAYER_GENIE_DATA);
        if (!attachment.isGenie() || attachment.getMasterUUID().isEmpty()) {
            return;
        }

        UUID masterUuid = attachment.getMasterUUID().get();
        ServerPlayer master = level.getServer().getPlayerList().getPlayer(masterUuid);

        Component reactionMsg = switch (reaction) {
            case FULFILL -> Component.translatable("message.kubanhorizons.genie.vessel.fulfilled", wishText);
            case INTERPRET -> Component.translatable("message.kubanhorizons.genie.vessel.interpreted", wishText);
            case WARN -> Component.translatable("message.kubanhorizons.genie.vessel.warned", wishText);
            case REFUSE -> Component.translatable("message.kubanhorizons.genie.vessel.refused", wishText);
            case LOOPHOLE -> Component.translatable("message.kubanhorizons.genie.vessel.loophole_found", wishText);
        };

        geniePlayer.sendSystemMessage(reactionMsg);
        if (master != null) {
            master.sendSystemMessage(reactionMsg);
        }

        // Прогресс Всемогущества увеличивается при каждой обработке желания
        PlayerGenieProgression.advanceProgress(geniePlayer, 5);
        // Желание — это и есть пользование сосудом: тишина обнуляется, и выход
        // своими силами снова становится дорогим. Отсюда гонка тактик — хозяину
        // надо приказывать, чтобы не потерять слугу, слуге выгодно молчание.
        dev.romankrukovsky.kubanhorizons.genie.vessel.VesselLaw.markUsed(level, geniePlayer);
    }
}
