package dev.romankrukovsky.kubanhorizons.client.render;

import dev.romankrukovsky.kubanhorizons.entity.Nutria;
import dev.romankrukovsky.kubanhorizons.util.KHIds;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.Identifier;

/** Рендерер нутрии. */
public class NutriaRenderer extends MobRenderer<Nutria, NutriaRenderState, NutriaModel> {
    private static final Identifier TEXTURE = KHIds.of("textures/entity/nutria.png");

    public NutriaRenderer(EntityRendererProvider.Context context) {
        super(context, new NutriaModel(
                context.bakeLayer(dev.romankrukovsky.kubanhorizons.client
                        .KHClientEvents.NUTRIA_LAYER)), 0.4F);
    }

    @Override
    public NutriaRenderState createRenderState() {
        return new NutriaRenderState();
    }

    @Override
    public void extractRenderState(Nutria entity, NutriaRenderState state,
                                   float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.swimming = entity.isInWater();
    }

    @Override
    public Identifier getTextureLocation(NutriaRenderState state) {
        return TEXTURE;
    }
}
