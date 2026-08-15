package dev.romankrukovsky.kubanhorizons.genie.ability;

import dev.romankrukovsky.kubanhorizons.entity.KubanGenie;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

/**
 * Центральная способность Кубанской Джиннии — щелчок пальцами.
 * Открывает радиальное меню «ИЗМЕНИТЬ РЕАЛЬНОСТЬ».
 */
public final class GenieSnapAbility {

    private GenieSnapAbility() {}

    /**
     * Выполняет щелчок пальцами.
     * Проигрывает звук, частицы и отправляет пакет на открытие меню клиенту.
     */
    public static void performSnap(KubanGenie genie, ServerPlayer player) {
        ServerLevel level = (ServerLevel) genie.level();

        // Звук щелчка
        level.playSound(null, genie.getX(), genie.getY(), genie.getZ(),
                SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 0.8f, 1.2f);

        // TODO: Отправить пакет на клиент для открытия радиального меню
        // PacketHandler.sendToPlayer(new OpenRealityMenuPacket(), player);

        // Временная заглушка — просто сообщение
        player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§5[Джинния] §fЩелчок... (меню в разработке)"));
    }
}
