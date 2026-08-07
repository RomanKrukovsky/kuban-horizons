package dev.romankrukovsky.kubanhorizons.client.render;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

/** Состояние рендера осетра. */
public class SturgeonRenderState extends LivingEntityRenderState {
    /** В воде рыба идёт волной; на воздухе — бьётся резко. */
    public boolean inWater;
}
