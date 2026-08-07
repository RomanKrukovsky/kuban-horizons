package dev.romankrukovsky.kubanhorizons.client.render;

import dev.romankrukovsky.kubanhorizons.util.KHIds;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Mob;

/** Рендерер летающего насекомого: саранча и пчела. */
public class InsectRenderer extends MobRenderer<Mob, InsectRenderState, InsectModel> {
    private final Identifier texture;

    public InsectRenderer(EntityRendererProvider.Context context,
                          ModelLayerLocation layer, String species) {
        // Тень почти нулевая: насекомое мелкое, крупная тень читалась бы как
        // тень зверя и путала бы игрока.
        super(context, new InsectModel(context.bakeLayer(layer)), 0.1F);
        this.texture = KHIds.of("textures/entity/" + species + ".png");
    }

    @Override
    public InsectRenderState createRenderState() {
        return new InsectRenderState();
    }

    @Override
    public void extractRenderState(Mob entity, InsectRenderState state,
                                   float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        // Оба вида летают почти всегда; на земле (onGround) крылья складываются.
        state.flying = !entity.onGround();
    }

    @Override
    public Identifier getTextureLocation(InsectRenderState state) {
        return texture;
    }
}
