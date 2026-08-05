package dev.romankrukovsky.kubanhorizons.client;

import dev.romankrukovsky.kubanhorizons.KubanHorizons;
import dev.romankrukovsky.kubanhorizons.client.screen.OilPressScreen;
import dev.romankrukovsky.kubanhorizons.registry.KHMenus;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

/**
 * Клиентские подписчики мод-шины: регистрация экранов и рендереров.
 */
@EventBusSubscriber(modid = KubanHorizons.MOD_ID, value = Dist.CLIENT)
public final class KHClientEvents {
    private KHClientEvents() {
    }

    @SubscribeEvent
    static void onRegisterScreens(RegisterMenuScreensEvent event) {
        event.register(KHMenus.OIL_PRESS.get(), OilPressScreen::new);
    }
}
