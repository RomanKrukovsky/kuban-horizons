package dev.romankrukovsky.kubanhorizons.client.render;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

/**
 * Состояние рендера наземной птицы.
 *
 * В 26.2 рендер отделён от сущности: рендерер читает не саму птицу, а снимок
 * её состояния, собранный на клиентском тике. Поэтому взмах крыльев нельзя
 * взять из сущности напрямую — его нужно перенести сюда в extractRenderState.
 */
public class GroundBirdRenderState extends LivingEntityRenderState {
    /** Фаза взмаха крыла, радианы. */
    public float flap;
    /** Амплитуда взмаха: 0 на земле, ~1 во время испуганного взлёта. */
    public float flapSpeed;
}
