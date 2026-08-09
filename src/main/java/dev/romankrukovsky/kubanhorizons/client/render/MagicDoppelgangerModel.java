package dev.romankrukovsky.kubanhorizons.client.render;

import dev.romankrukovsky.kubanhorizons.entity.MagicDoppelgangerEntity;
import dev.romankrukovsky.kubanhorizons.util.KHIds;
import net.minecraft.resources.Identifier;
import com.geckolib.model.GeoModel;
import com.geckolib.renderer.base.GeoRenderState;

/**
 * Двойник намеренно использует ресурсы джиннии.
 *
 * <p>Он её отражение, а не отдельное существо: своя модель означала бы, что
 * игрок видит подделку и отличает клона от оригинала — тогда как весь смысл
 * в том, что отличить нельзя. Единственный признак — отсутствие жестов.</p>
 */
public final class MagicDoppelgangerModel extends GeoModel<MagicDoppelgangerEntity> {
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
    public Identifier getAnimationResource(MagicDoppelgangerEntity animatable) {
        return ANIMATION;
    }
}
