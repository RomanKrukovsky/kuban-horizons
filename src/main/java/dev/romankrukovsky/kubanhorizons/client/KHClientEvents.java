package dev.romankrukovsky.kubanhorizons.client;

import dev.romankrukovsky.kubanhorizons.KubanHorizons;
import dev.romankrukovsky.kubanhorizons.client.render.GroundBirdModel;
import dev.romankrukovsky.kubanhorizons.client.render.GroundBirdRenderer;
import dev.romankrukovsky.kubanhorizons.client.screen.OilPressScreen;
import dev.romankrukovsky.kubanhorizons.registry.KHEntities;
import dev.romankrukovsky.kubanhorizons.registry.KHMenus;
import dev.romankrukovsky.kubanhorizons.util.KHIds;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

/**
 * Клиентские подписчики мод-шины: регистрация экранов и рендереров.
 */
@EventBusSubscriber(modid = KubanHorizons.MOD_ID, value = Dist.CLIENT)
public final class KHClientEvents {
    /** Слои моделей птиц: у фазана длинный хвост, у перепела короткий. */
    public static final ModelLayerLocation PHEASANT_LAYER =
            new ModelLayerLocation(KHIds.of("pheasant"), "main");
    public static final ModelLayerLocation QUAIL_LAYER =
            new ModelLayerLocation(KHIds.of("quail"), "main");

    private KHClientEvents() {
    }

    @SubscribeEvent
    static void onRegisterScreens(RegisterMenuScreensEvent event) {
        event.register(KHMenus.OIL_PRESS.get(), OilPressScreen::new);
    }

    @SubscribeEvent
    static void onRegisterLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        // Фазан: корпус длиннее и хвост 9 пикселей — узнаваемый силуэт в степи.
        event.registerLayerDefinition(PHEASANT_LAYER,
                () -> GroundBirdModel.createBodyLayer(9, 8));
        // Перепел: мелкий, хвост почти отсутствует.
        event.registerLayerDefinition(QUAIL_LAYER,
                () -> GroundBirdModel.createBodyLayer(3, 6));
    }

    @SubscribeEvent
    static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(KHEntities.PHEASANT.get(),
                context -> new GroundBirdRenderer(context, PHEASANT_LAYER,
                        "pheasant", 0.35F));
        event.registerEntityRenderer(KHEntities.QUAIL.get(),
                context -> new GroundBirdRenderer(context, QUAIL_LAYER,
                        "quail", 0.25F));
    }
}
