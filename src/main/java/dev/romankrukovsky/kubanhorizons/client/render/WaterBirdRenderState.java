package dev.romankrukovsky.kubanhorizons.client.render;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

/**
 * Состояние рендера водной птицы: чайка и цапля.
 *
 * <p>Чайке признак полёта нужен для планирования, цапле — чтобы вытянуть шею и
 * ноги в линию: стоящая цапля держит шею сложенной, летящая — прямой.</p>
 */
public class WaterBirdRenderState extends LivingEntityRenderState {
    /** Птица в воздухе. */
    public boolean flying;
    /** Цапля стоит в воде и караулит добычу — шея сложена, тело неподвижно. */
    public boolean stalking;
}
