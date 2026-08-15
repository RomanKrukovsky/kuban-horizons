package dev.romankrukovsky.kubanhorizons.client;

import dev.romankrukovsky.kubanhorizons.client.input.GenieKeyBindings;
import dev.romankrukovsky.kubanhorizons.client.screen.RealityMenuScreen;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.fml.common.Mod;

/**
 * Client-side initialization for Kuban Horizons.
 */
@EventBusSubscriber(modid = "kubanhorizons", value = Dist.CLIENT)
public class KubanHorizonsClient {

    @SubscribeEvent
    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(GenieKeyBindings.SNAP);
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (GenieKeyBindings.SNAP.consumeClick()) {
            Minecraft.getInstance().setScreenAndShow(new RealityMenuScreen());
        }
    }
}
