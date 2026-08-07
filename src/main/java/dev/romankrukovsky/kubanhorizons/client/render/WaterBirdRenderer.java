package dev.romankrukovsky.kubanhorizons.client.render;

import dev.romankrukovsky.kubanhorizons.util.KHIds;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Mob;

/**
 * Рендерер водной птицы: чайка и цапля.
 *
 * <p>Модели у них разные (чайка летает, цапля ходит на ходулях), но состояние
 * рендера и способ его снятия общие, поэтому рендерер один и параметризован
 * фабрикой модели.</p>
 */
public class WaterBirdRenderer
        extends MobRenderer<Mob, WaterBirdRenderState, EntityModel<WaterBirdRenderState>> {
    private final Identifier texture;

    public WaterBirdRenderer(EntityRendererProvider.Context context,
                             ModelLayerLocation layer, String species,
                             float shadow, boolean flyer) {
        super(context, flyer
                ? new GullModel(context.bakeLayer(layer))
                : new HeronModel(context.bakeLayer(layer)), shadow);
        this.texture = KHIds.of("textures/entity/" + species + ".png");
    }

    @Override
    public WaterBirdRenderState createRenderState() {
        return new WaterBirdRenderState();
    }

    @Override
    public void extractRenderState(Mob entity, WaterBirdRenderState state,
                                   float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.flying = !entity.onGround() && !entity.isInWater();
        // Цапля «караулит», когда стоит в воде и не идёт: именно так она и
        // охотится. Отдельного серверного флага для этого нет, и он не нужен —
        // поза выводится из положения, которое клиент уже знает.
        state.stalking = entity.isInWater() && entity.walkAnimation.speed() < 0.01F;
    }

    @Override
    public Identifier getTextureLocation(WaterBirdRenderState state) {
        return texture;
    }
}
