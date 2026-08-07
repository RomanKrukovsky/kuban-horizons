package dev.romankrukovsky.kubanhorizons.client.render;

import dev.romankrukovsky.kubanhorizons.util.KHIds;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;

/**
 * Рендерер кубанского четвероногого: один класс на кабана и овчарку.
 *
 * <p>Различаются только слоем модели, текстурой и радиусом тени, поэтому
 * отдельные классы дали бы два почти одинаковых файла.</p>
 */
public class KubanQuadrupedRenderer
        extends MobRenderer<Mob, KubanQuadrupedRenderState, KubanQuadrupedModel> {
    private final Identifier texture;

    public KubanQuadrupedRenderer(EntityRendererProvider.Context context,
                                  ModelLayerLocation layer, String species,
                                  float shadow, boolean sittable) {
        super(context, new KubanQuadrupedModel(context.bakeLayer(layer), sittable), shadow);
        this.texture = KHIds.of("textures/entity/" + species + ".png");
    }

    @Override
    public KubanQuadrupedRenderState createRenderState() {
        return new KubanQuadrupedRenderState();
    }

    @Override
    public void extractRenderState(Mob entity, KubanQuadrupedRenderState state,
                                   float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        // Кабан не приручается, поэтому проверяем тип, а не заводим два рендерера.
        state.sitting = entity instanceof TamableAnimal tamable && tamable.isInSittingPose();
    }

    @Override
    public Identifier getTextureLocation(KubanQuadrupedRenderState state) {
        return texture;
    }
}
