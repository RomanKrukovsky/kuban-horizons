package dev.romankrukovsky.kubanhorizons.client.render;

import dev.romankrukovsky.kubanhorizons.entity.KubanGenie;
import dev.romankrukovsky.kubanhorizons.util.KHIds;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.Identifier;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.layer.builtin.AutoGlowingGeoLayer;

/** Рендерит бинарно-дизеренный хвост без отсечения обратных граней. */
public final class KubanGenieRenderer extends GeoEntityRenderer<KubanGenie, EntityRenderState> {
    private static final Identifier TEXTURE = KHIds.of("textures/entity/kuban_genie.png");

    @SuppressWarnings({"rawtypes", "unchecked"})
    public KubanGenieRenderer(EntityRendererProvider.Context context) {
        super(context, new KubanGenieModel());
        // GeckoLib injects GeoRenderState into EntityRenderState at runtime;
        // ModDevGradle cannot express that mixin-added interface to javac.
        withRenderLayer((com.geckolib.renderer.layer.GeoRenderLayer)
                new AutoGlowingGeoLayer((com.geckolib.renderer.base.GeoRenderer) this));
    }

    @Override
    public RenderType getRenderType(EntityRenderState renderState, Identifier texture) {
        return RenderTypes.entityCutout(TEXTURE);
    }
}
