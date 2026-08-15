package dev.romankrukovsky.kubanhorizons.network;

import dev.romankrukovsky.kubanhorizons.KubanHorizons;
import dev.romankrukovsky.kubanhorizons.network.packet.c2s.C2SGenieCommand;
import dev.romankrukovsky.kubanhorizons.network.packet.c2s.C2SOpenGenieDialog;
import dev.romankrukovsky.kubanhorizons.network.packet.c2s.C2SOwnerDeathChoice;
import dev.romankrukovsky.kubanhorizons.network.packet.c2s.C2SPolicyDecision;
import dev.romankrukovsky.kubanhorizons.network.packet.c2s.C2SPocketConfirm;
import dev.romankrukovsky.kubanhorizons.network.packet.c2s.C2SPocketRollback;
import dev.romankrukovsky.kubanhorizons.network.packet.c2s.C2SWishRequest;
import dev.romankrukovsky.kubanhorizons.network.packet.s2c.S2CGenieResponse;
import dev.romankrukovsky.kubanhorizons.network.packet.s2c.S2COpenGenieDialog;
import dev.romankrukovsky.kubanhorizons.network.packet.s2c.S2COpenOwnerDeathScreen;
import dev.romankrukovsky.kubanhorizons.network.packet.s2c.S2CPocketPreview;
import dev.romankrukovsky.kubanhorizons.network.packet.s2c.S2CPocketResult;
import dev.romankrukovsky.kubanhorizons.network.packet.s2c.S2CTransformationSync;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * Регистрация всех сетевых пакетов мода.
 *
 * <p>Сетевой набор диалога, приказов, правил и карманной сцены.
 */
public final class KHNetwork {

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(KHNetwork::registerPayloads);
    }

    private static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(KubanHorizons.MOD_ID)
                .versioned("1.0.0")
                .optional();

        // C2S
        registrar.playToServer(C2SWishRequest.TYPE, C2SWishRequest.CODEC, C2SWishRequest::handle);
        registrar.playToServer(C2SOpenGenieDialog.TYPE, C2SOpenGenieDialog.CODEC,
                C2SOpenGenieDialog::handle);
        registrar.playToServer(C2SPolicyDecision.TYPE, C2SPolicyDecision.CODEC,
                C2SPolicyDecision::handle);
        registrar.playToServer(C2SGenieCommand.TYPE, C2SGenieCommand.CODEC, C2SGenieCommand::handle);
        registrar.playToServer(C2SPocketConfirm.TYPE, C2SPocketConfirm.CODEC, C2SPocketConfirm::handle);
        registrar.playToServer(C2SPocketRollback.TYPE, C2SPocketRollback.CODEC, C2SPocketRollback::handle);
        registrar.playToServer(C2SOwnerDeathChoice.TYPE, C2SOwnerDeathChoice.CODEC, C2SOwnerDeathChoice::handle);

        // S2C
        // Клиентские обработчики регистрирует KHClientPayloadHandlers через
        // RegisterClientPayloadHandlersEvent. Здесь только общий протокол:
        // dedicated server не должен загружать Minecraft GUI и Screen.
        registrar.playToClient(S2CGenieResponse.TYPE, S2CGenieResponse.CODEC);
        registrar.playToClient(S2COpenGenieDialog.TYPE, S2COpenGenieDialog.CODEC);
        registrar.playToClient(S2CPocketPreview.TYPE, S2CPocketPreview.CODEC);
        registrar.playToClient(S2CPocketResult.TYPE, S2CPocketResult.CODEC);
        registrar.playToClient(S2COpenOwnerDeathScreen.TYPE, S2COpenOwnerDeathScreen.CODEC);
        registrar.playToClient(S2CTransformationSync.TYPE, S2CTransformationSync.CODEC);

        KubanHorizons.LOGGER.info("KHNetwork: зарегистрировано 12 пакетов джиннии.");
    }
}
