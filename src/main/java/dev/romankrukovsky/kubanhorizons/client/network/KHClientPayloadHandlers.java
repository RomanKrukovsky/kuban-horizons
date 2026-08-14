package dev.romankrukovsky.kubanhorizons.client.network;

import dev.romankrukovsky.kubanhorizons.KubanHorizons;
import dev.romankrukovsky.kubanhorizons.client.screen.GenieDialogScreen;
import dev.romankrukovsky.kubanhorizons.client.screen.OwnerDeathChoiceScreen;
import dev.romankrukovsky.kubanhorizons.client.screen.PocketConfirmScreen;
import dev.romankrukovsky.kubanhorizons.network.packet.s2c.S2CGenieResponse;
import dev.romankrukovsky.kubanhorizons.network.packet.s2c.S2COpenGenieDialog;
import dev.romankrukovsky.kubanhorizons.network.packet.s2c.S2COpenOwnerDeathScreen;
import dev.romankrukovsky.kubanhorizons.network.packet.s2c.S2CPocketPreview;
import dev.romankrukovsky.kubanhorizons.network.packet.s2c.S2CPocketResult;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Все S2C-обработчики, которые имеют право обращаться к клиентскому UI. */
@EventBusSubscriber(modid = KubanHorizons.MOD_ID, value = Dist.CLIENT)
public final class KHClientPayloadHandlers {
    private KHClientPayloadHandlers() {
    }

    @SubscribeEvent
    public static void register(RegisterClientPayloadHandlersEvent event) {
        event.register(S2CGenieResponse.TYPE, KHClientPayloadHandlers::handleGenieResponse);
        event.register(S2COpenGenieDialog.TYPE, KHClientPayloadHandlers::handleOpenDialog);
        event.register(S2CPocketPreview.TYPE, KHClientPayloadHandlers::handlePocketPreview);
        event.register(S2CPocketResult.TYPE, KHClientPayloadHandlers::handlePocketResult);
        event.register(S2COpenOwnerDeathScreen.TYPE, KHClientPayloadHandlers::handleOpenOwnerDeathScreen);
    }

    private static void handleOpenDialog(S2COpenGenieDialog packet, IPayloadContext context) {
        Minecraft.getInstance().gui.setScreen(new GenieDialogScreen(
                packet.genieId(), packet.genieName(), packet.mode()));
    }

    private static void handleGenieResponse(S2CGenieResponse packet, IPayloadContext context) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.gui.screen() instanceof GenieDialogScreen screen
                && screen.genieId() == packet.genieId()) {
            screen.acceptResponse(packet.message(), packet.emotionLevel(),
                    packet.confirmationRequired());
        } else if (minecraft.player != null) {
            minecraft.player.sendSystemMessage(packet.message());
        }
    }

    private static void handlePocketPreview(S2CPocketPreview packet, IPayloadContext context) {
        Minecraft.getInstance().gui.setScreen(new PocketConfirmScreen(
                packet.changedBlocks(), packet.durationTicks(), packet.risk()));
    }

    private static void handlePocketResult(S2CPocketResult packet, IPayloadContext context) {
        var player = Minecraft.getInstance().player;
        if (player != null) {
            player.sendSystemMessage(packet.message());
        }
    }

    private static void handleOpenOwnerDeathScreen(S2COpenOwnerDeathScreen packet, IPayloadContext context) {
        Minecraft.getInstance().gui.setScreen(new OwnerDeathChoiceScreen(packet.genieId()));
    }
}
