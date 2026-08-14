package dev.romankrukovsky.kubanhorizons.client.render;

import dev.romankrukovsky.kubanhorizons.entity.Manul;
import com.geckolib.renderer.GeoEntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;

/**
 * Рендерер детальной Blockbench-модели манула.
 */
public final class ManulRenderer extends GeoEntityRenderer<Manul, EntityRenderState> {
    public ManulRenderer(EntityRendererProvider.Context context) {
        super(context, new ManulGeoModel());
        withScale(.42F);
        shadowRadius = .55F;
    }
}
