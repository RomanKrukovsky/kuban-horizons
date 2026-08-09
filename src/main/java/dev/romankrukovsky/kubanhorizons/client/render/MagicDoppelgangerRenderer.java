package dev.romankrukovsky.kubanhorizons.client.render;

import dev.romankrukovsky.kubanhorizons.entity.MagicDoppelgangerEntity;
import dev.romankrukovsky.kubanhorizons.util.KHIds;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.Identifier;
import com.geckolib.renderer.GeoEntityRenderer;

/**
 * Рендерит магического двойника внешностью джиннии.
 *
 * <p>Слоя свечения здесь нет намеренно: свечение — признак настоящей
 * силы, и клон, лишённый личности, не должен её излучать. Различие
 * тонкое и не сразу заметное — таким и задумано.</p>
 */
public final class MagicDoppelgangerRenderer
        extends GeoEntityRenderer<MagicDoppelgangerEntity, EntityRenderState> {
    private static final Identifier TEXTURE = KHIds.of("textures/entity/kuban_genie.png");

    public MagicDoppelgangerRenderer(EntityRendererProvider.Context context) {
        super(context, new MagicDoppelgangerModel());
    }

    @Override
    public RenderType getRenderType(EntityRenderState renderState, Identifier texture) {
        return RenderTypes.entityCutout(TEXTURE);
    }
}
