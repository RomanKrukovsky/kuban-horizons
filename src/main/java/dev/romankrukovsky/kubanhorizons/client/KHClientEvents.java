package dev.romankrukovsky.kubanhorizons.client;

import dev.romankrukovsky.kubanhorizons.KubanHorizons;
import dev.romankrukovsky.kubanhorizons.client.render.GroundBirdModel;
import dev.romankrukovsky.kubanhorizons.client.render.GroundBirdRenderer;
import dev.romankrukovsky.kubanhorizons.client.render.GullModel;
import dev.romankrukovsky.kubanhorizons.client.render.HeronModel;
import dev.romankrukovsky.kubanhorizons.client.render.ManulModel;
import dev.romankrukovsky.kubanhorizons.client.render.ManulRenderer;
import dev.romankrukovsky.kubanhorizons.client.render.InsectModel;
import dev.romankrukovsky.kubanhorizons.client.render.InsectRenderer;
import dev.romankrukovsky.kubanhorizons.client.render.SturgeonModel;
import dev.romankrukovsky.kubanhorizons.client.render.SturgeonRenderer;
import dev.romankrukovsky.kubanhorizons.client.render.WaterBirdRenderer;
import dev.romankrukovsky.kubanhorizons.client.render.KubanQuadrupedModel;
import dev.romankrukovsky.kubanhorizons.client.render.KubanQuadrupedRenderer;
import dev.romankrukovsky.kubanhorizons.client.render.NutriaModel;
import dev.romankrukovsky.kubanhorizons.client.render.NutriaRenderer;
import dev.romankrukovsky.kubanhorizons.client.render.KubanGenieRenderer;
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
    /** Четвероногие: кабан приземистее, овчарка выше и лохматее. */
    public static final ModelLayerLocation WILD_BOAR_LAYER =
            new ModelLayerLocation(KHIds.of("wild_boar"), "main");
    public static final ModelLayerLocation CAUCASIAN_SHEPHERD_LAYER =
            new ModelLayerLocation(KHIds.of("caucasian_shepherd"), "main");
    /** Нутрия: своя сетка — горбатая спина и голый хвост. */
    public static final ModelLayerLocation NUTRIA_LAYER =
            new ModelLayerLocation(KHIds.of("nutria"), "main");
    /** Насекомые: одна сетка, разные пропорции брюшка и ног. */
    public static final ModelLayerLocation LOCUST_LAYER =
            new ModelLayerLocation(KHIds.of("locust"), "main");
    /** Водные птицы: чайка летает, цапля стоит на ходулях. */
    public static final ModelLayerLocation GULL_LAYER =
            new ModelLayerLocation(KHIds.of("gull"), "main");
    public static final ModelLayerLocation HERON_LAYER =
            new ModelLayerLocation(KHIds.of("heron"), "main");
    /** Манул: талисман мода, ванильная сетка четвероногого. */
    public static final ModelLayerLocation MANUL_LAYER =
            new ModelLayerLocation(KHIds.of("manul"), "main");
    /** Осётр: два сегмента тела для волнообразного хода. */
    public static final ModelLayerLocation STURGEON_LAYER =
            new ModelLayerLocation(KHIds.of("sturgeon"), "main");
    /** Хвост игрока-джиннии: слой поверх ванильной модели игрока. */
    public static final ModelLayerLocation GENIE_TAIL_LAYER =
            new ModelLayerLocation(KHIds.of("genie_tail"), "main");

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
        event.registerLayerDefinition(WILD_BOAR_LAYER,
                KubanQuadrupedModel::createBoarLayer);
        event.registerLayerDefinition(CAUCASIAN_SHEPHERD_LAYER,
                KubanQuadrupedModel::createShepherdLayer);
        event.registerLayerDefinition(NUTRIA_LAYER, NutriaModel::createBodyLayer);
        // Саранча: вытянутое брюшко и длинные прыжковые ноги.
        event.registerLayerDefinition(LOCUST_LAYER,
                () -> InsectModel.createBodyLayer(6, 3));
        event.registerLayerDefinition(GULL_LAYER, GullModel::createBodyLayer);
        event.registerLayerDefinition(HERON_LAYER, HeronModel::createBodyLayer);
        event.registerLayerDefinition(MANUL_LAYER, ManulModel::createBodyLayer);
        event.registerLayerDefinition(STURGEON_LAYER, SturgeonModel::createBodyLayer);
        event.registerLayerDefinition(GENIE_TAIL_LAYER,
                dev.romankrukovsky.kubanhorizons.client.render.GenieTailModel::createLayer);
    }

    @SubscribeEvent
    static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(KHEntities.PHEASANT.get(),
                context -> new GroundBirdRenderer(context, PHEASANT_LAYER,
                        "pheasant", 0.35F));
        event.registerEntityRenderer(KHEntities.QUAIL.get(),
                context -> new GroundBirdRenderer(context, QUAIL_LAYER,
                        "quail", 0.25F));
        // Кабан крупный и тяжёлый — тень шире; овчарка не приручена по умолчанию,
        // но умеет сидеть, поэтому у неё sittable = true.
        event.registerEntityRenderer(KHEntities.WILD_BOAR.get(),
                context -> new KubanQuadrupedRenderer(context, WILD_BOAR_LAYER,
                        "wild_boar", 0.7F, false));
        event.registerEntityRenderer(KHEntities.CAUCASIAN_SHEPHERD.get(),
                context -> new KubanQuadrupedRenderer(context, CAUCASIAN_SHEPHERD_LAYER,
                        "caucasian_shepherd", 0.6F, true));
        event.registerEntityRenderer(KHEntities.NUTRIA.get(), NutriaRenderer::new);
        event.registerEntityRenderer(KHEntities.LOCUST.get(),
                context -> new InsectRenderer(context, LOCUST_LAYER, "locust"));
        event.registerEntityRenderer(KHEntities.GULL.get(),
                context -> new WaterBirdRenderer(context, GULL_LAYER, "gull",
                        0.3F, true));
        event.registerEntityRenderer(KHEntities.HERON.get(),
                context -> new WaterBirdRenderer(context, HERON_LAYER, "heron",
                        0.4F, false));
        event.registerEntityRenderer(KHEntities.MANUL.get(),
                context -> new ManulRenderer(context, MANUL_LAYER));
        event.registerEntityRenderer(KHEntities.STURGEON.get(), SturgeonRenderer::new);
        event.registerEntityRenderer(KHEntities.KUBAN_GENIE.get(), KubanGenieRenderer::new);
    }

    /**
     * Кладёт стадию превращения в состояние рендера игрока.
     *
     * <p>Слой рендера получает только {@code AvatarRenderState} и не видит ни
     * сущность, ни attachment, поэтому стадия попадает туда здесь — один раз за
     * кадр, штатным путём NeoForge. Чтение серверных данных прямо из отрисовки
     * было бы гонкой.</p>
     */
    @SubscribeEvent
    static void onRegisterRenderStateModifiers(
            net.neoforged.neoforge.client.renderstate.RegisterRenderStateModifiersEvent event) {
        event.registerAvatarEntityModifier(
                new net.neoforged.neoforge.client.renderstate.AvatarRenderStateModifier() {
                    @Override
                    public <T extends net.minecraft.world.entity.Avatar
                                    & net.minecraft.client.entity.ClientAvatarEntity>
                            void accept(T avatar,
                                    net.minecraft.client.renderer.entity.state.AvatarRenderState state) {
                        var data = avatar.getData(
                                dev.romankrukovsky.kubanhorizons.registry.KHAttachments.PLAYER_GENIE_DATA);
                        // Не джинния — ключ не ставится вовсе, слой тогда просто
                        // ничего не рисует и не платит за проверку стадии.
                        if (data.isGenie()) {
                            state.setRenderData(
                                    dev.romankrukovsky.kubanhorizons.client.render.GenieRenderStateKeys.GENIE_STAGE,
                                    data.getStage());
                        }
                    }
                });
    }

    /**
     * Вешает хвост на все модели игрока — и обычную, и slim.
     *
     * <p>Обе, а не одна: {@code getSkins()} возвращает оба типа модели, и слой,
     * добавленный только к одному, у половины игроков не появился бы вовсе.</p>
     */
    @SubscribeEvent
    static void onAddLayers(EntityRenderersEvent.AddLayers event) {
        var tailLayer = event.getContext().getModelSet().bakeLayer(GENIE_TAIL_LAYER);
        for (var skin : event.getSkins()) {
            net.minecraft.client.renderer.entity.player.AvatarRenderer<
                            net.minecraft.client.player.AbstractClientPlayer> renderer =
                    event.getPlayerRenderer(skin);
            if (renderer != null) {
                renderer.addLayer(new dev.romankrukovsky.kubanhorizons.client.render.GenieTailLayer(
                        renderer,
                        new dev.romankrukovsky.kubanhorizons.client.render.GenieTailModel(tailLayer)));
            }
        }
    }
}
