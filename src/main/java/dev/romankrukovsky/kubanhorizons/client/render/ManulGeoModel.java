package dev.romankrukovsky.kubanhorizons.client.render;

import com.geckolib.constant.DataTickets;
import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.model.GeoModel;
import com.geckolib.renderer.base.GeoRenderState;
import dev.romankrukovsky.kubanhorizons.entity.Manul;
import dev.romankrukovsky.kubanhorizons.util.KHIds;
import net.minecraft.resources.Identifier;

/** Пути и окрас экспортированной из Blockbench модели манула. */
public final class ManulGeoModel extends GeoModel<Manul> {
    private static final Identifier MODEL = KHIds.of("manul");
    private static final Identifier ANIMATION = KHIds.of("manul");
    private static final DataTicket<String> COAT =
            DataTickets.create("kubanhorizons_manul_coat", String.class);

    @Override
    public Identifier getModelResource(GeoRenderState renderState) {
        return MODEL;
    }

    @Override
    public Identifier getTextureResource(GeoRenderState renderState) {
        String coat = renderState.getOrDefaultGeckolibData(COAT, "steppe");
        return KHIds.of("textures/entity/manul_" + coat + ".png");
    }

    @Override
    public Identifier getAnimationResource(Manul animatable) {
        return ANIMATION;
    }

    @Override
    public void addAdditionalStateData(Manul animatable, Object data, GeoRenderState renderState) {
        renderState.addGeckolibData(COAT, animatable.coat().key());
    }
}
