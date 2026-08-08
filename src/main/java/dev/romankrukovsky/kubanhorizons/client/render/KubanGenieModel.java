package dev.romankrukovsky.kubanhorizons.client.render;

import dev.romankrukovsky.kubanhorizons.entity.KubanGenie;
import dev.romankrukovsky.kubanhorizons.util.KHIds;
import net.minecraft.resources.Identifier;
import com.geckolib.model.GeoModel;
import com.geckolib.renderer.base.GeoRenderState;

/** Пути к экспортированным ресурсам джиннии. */
public final class KubanGenieModel extends GeoModel<KubanGenie> {
    private static final Identifier MODEL = KHIds.of("kuban_genie");
    private static final Identifier TEXTURE = KHIds.of("textures/entity/kuban_genie.png");
    private static final Identifier ANIMATION = KHIds.of("kuban_genie");

    @Override
    public Identifier getModelResource(GeoRenderState renderState) {
        return MODEL;
    }

    @Override
    public Identifier getTextureResource(GeoRenderState renderState) {
        return TEXTURE;
    }

    @Override
    public Identifier getAnimationResource(KubanGenie animatable) {
        return ANIMATION;
    }
}
