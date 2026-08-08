package dev.romankrukovsky.kubanhorizons.genie.runtime;

import dev.romankrukovsky.kubanhorizons.KubanHorizons;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;

/** Жизненный цикл world-scoped wish runtime. */
@EventBusSubscriber(modid = KubanHorizons.MOD_ID)
public final class WishRuntimeEvents {
    private WishRuntimeEvents() {
    }

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        WishRuntime.get(event.getServer()).recover();
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        WishRuntime.remove(event.getServer());
    }
}
