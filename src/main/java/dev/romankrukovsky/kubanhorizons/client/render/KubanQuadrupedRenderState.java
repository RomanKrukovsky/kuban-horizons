package dev.romankrukovsky.kubanhorizons.client.render;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

/**
 * Состояние рендера кубанского четвероногого.
 *
 * <p>В 26.2 рендерер не читает сущность напрямую, поэтому признак «сидит»
 * (у прирученной овчарки) переносится сюда снимком на клиентском тике.</p>
 */
public class KubanQuadrupedRenderState extends LivingEntityRenderState {
    /** Овчарка сидит по команде игрока; у кабана всегда false. */
    public boolean sitting;
}
