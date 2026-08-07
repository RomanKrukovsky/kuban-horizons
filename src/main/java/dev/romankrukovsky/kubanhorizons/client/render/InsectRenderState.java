package dev.romankrukovsky.kubanhorizons.client.render;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

/** Состояние рендера летающего насекомого. */
public class InsectRenderState extends LivingEntityRenderState {
    /** В воздухе: крылья машут, ноги поджаты. */
    public boolean flying;
}
