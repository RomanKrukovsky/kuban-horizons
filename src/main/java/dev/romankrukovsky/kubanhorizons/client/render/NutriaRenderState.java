package dev.romankrukovsky.kubanhorizons.client.render;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

/**
 * Состояние рендера нутрии.
 *
 * <p>Признак «плывёт» нужен модели, чтобы переключить хвост из шагающего
 * положения в гребное. Сущность на клиенте напрямую не читается (26.2),
 * поэтому снимок делается в extractRenderState.</p>
 */
public class NutriaRenderState extends LivingEntityRenderState {
    /** Нутрия в воде: хвост работает веслом. */
    public boolean swimming;
}
