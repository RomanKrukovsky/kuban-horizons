package dev.romankrukovsky.kubanhorizons.client.render;

import dev.romankrukovsky.kubanhorizons.entity.Manul;
import dev.romankrukovsky.kubanhorizons.entity.ManulCoat;
import dev.romankrukovsky.kubanhorizons.util.KHIds;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.Identifier;

/**
 * Рендерер манула: одна модель, четыре текстуры окраса.
 *
 * <p>Пути текстур собираются один раз в статическую карту, а не на каждом
 * кадре: {@code Identifier} в горячем пути рендера — это аллокация на кадр на
 * каждую особь.</p>
 */
public class ManulRenderer extends MobRenderer<Manul, ManulRenderState, ManulModel> {
    private static final Map<ManulCoat, Identifier> TEXTURES = new EnumMap<>(ManulCoat.class);

    static {
        for (ManulCoat coat : ManulCoat.values()) {
            TEXTURES.put(coat, KHIds.of("textures/entity/manul_" + coat.key() + ".png"));
        }
    }

    public ManulRenderer(EntityRendererProvider.Context context, ModelLayerLocation layer) {
        super(context, new ManulModel(context.bakeLayer(layer)), 0.4F);
    }

    @Override
    public ManulRenderState createRenderState() {
        return new ManulRenderState();
    }

    @Override
    public void extractRenderState(Manul entity, ManulRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.coat = entity.coat().key();
        state.hissing = entity.isHissing();
        state.frozen = entity.isFrozen();
    }

    @Override
    public Identifier getTextureLocation(ManulRenderState state) {
        // Неизвестный окрас — базовый степной: испорченное сохранение не должно
        // давать отсутствующую текстуру.
        return TEXTURES.getOrDefault(ManulCoat.byKey(state.coat), TEXTURES.get(ManulCoat.STEPPE));
    }
}
