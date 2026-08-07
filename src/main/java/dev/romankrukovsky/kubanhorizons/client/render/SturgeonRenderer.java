package dev.romankrukovsky.kubanhorizons.client.render;

import dev.romankrukovsky.kubanhorizons.entity.Sturgeon;
import dev.romankrukovsky.kubanhorizons.util.KHIds;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import com.mojang.blaze3d.vertex.PoseStack;

/**
 * Рендерер осетра.
 *
 * <p>Как и ванильные рыбы, на воздухе он переворачивается на бок: это
 * единственный визуальный сигнал, что рыба вытащена из воды и погибает.</p>
 */
public class SturgeonRenderer
        extends MobRenderer<Sturgeon, SturgeonRenderState, SturgeonModel> {
    private static final Identifier TEXTURE = KHIds.of("textures/entity/sturgeon.png");

    public SturgeonRenderer(EntityRendererProvider.Context context) {
        super(context, new SturgeonModel(
                context.bakeLayer(dev.romankrukovsky.kubanhorizons.client
                        .KHClientEvents.STURGEON_LAYER)), 0.35F);
    }

    @Override
    public SturgeonRenderState createRenderState() {
        return new SturgeonRenderState();
    }

    @Override
    public void extractRenderState(Sturgeon entity, SturgeonRenderState state,
                                   float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.inWater = entity.isInWater();
    }

    @Override
    protected void setupRotations(SturgeonRenderState state, PoseStack poseStack,
                                  float bodyRot, float scale) {
        super.setupRotations(state, poseStack, bodyRot, scale);
        if (!state.inWater) {
            // Выброшенная на берег рыба валится на бок и трепыхается.
            poseStack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(
                    90.0F + Mth.sin(state.ageInTicks * 0.9F) * 12.0F));
        }
    }

    @Override
    public Identifier getTextureLocation(SturgeonRenderState state) {
        return TEXTURE;
    }
}
