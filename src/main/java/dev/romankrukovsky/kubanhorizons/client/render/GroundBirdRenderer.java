package dev.romankrukovsky.kubanhorizons.client.render;

import dev.romankrukovsky.kubanhorizons.entity.AbstractGroundBird;
import dev.romankrukovsky.kubanhorizons.util.KHIds;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.Identifier;

/**
 * Рендерер наземной птицы.
 *
 * Гейт полноты ассетов нашёл, что фазан и перепел зарегистрированы, но не имеют
 * ни модели, ни рендерера: на клиенте они были бы невидимы. Один рендерер на оба
 * вида — различаются только слой модели и текстура.
 */
public class GroundBirdRenderer
        extends MobRenderer<AbstractGroundBird, GroundBirdRenderState, GroundBirdModel> {
    private final Identifier texture;

    public GroundBirdRenderer(EntityRendererProvider.Context context,
                              ModelLayerLocation layer, String species, float shadow) {
        super(context, new GroundBirdModel(context.bakeLayer(layer)), shadow);
        this.texture = KHIds.of("textures/entity/" + species + ".png");
    }

    @Override
    public GroundBirdRenderState createRenderState() {
        return new GroundBirdRenderState();
    }

    @Override
    public void extractRenderState(AbstractGroundBird entity, GroundBirdRenderState state,
                                   float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        // Взмах привязан к возрасту сущности, а не к отдельному счётчику: птица
        // машет крыльями только во время испуганного взлёта, на земле крылья
        // сложены. Так анимация всегда согласована с серверным состоянием.
        boolean flushing = entity.isFlushing();
        state.flap = (entity.tickCount + partialTicks) * 1.4F;
        state.flapSpeed = flushing ? 1.0F : 0.0F;
    }

    @Override
    public Identifier getTextureLocation(GroundBirdRenderState state) {
        return texture;
    }
}
