package genie.visual;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * Model for the genie's tail
 * Uses GeckoLib 5.5.3 for advanced animations
 */
public class GenieTailModel extends EntityModel<GenieTailState> {

    // Main tail segments
    private final ModelPart tailBase;
    private final ModelPart[] tailSegments;
    private final ModelPart tailTip;

    // Animation controllers
    private float animationProgress = 0.0f;
    private float previousTailLength = 16.0f;

    public GenieTailModel(ModelPart root) {
        super(RenderType::entityCutoutNoCull);
        this.tailBase = root.getChild("tail_base");

        // Create tail segments array
        this.tailSegments = new ModelPart[32]; // Max segments
        for (int i = 0; i < 32; i++) {
            this.tailSegments[i] = root.getChild("tail_segment_" + i);
        }

        this.tailTip = root.getChild("tail_tip");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

        // Tail base
        partdefinition.addOrReplaceChild("tail_base",
            CubeListBuilder.create()
                .texOffs(0, 0)
                .addBox(-2.0F, -2.0F, -2.0F, 4.0F, 4.0F, 4.0F, new CubeDeformation(0.0F)),
            PartPose.offset(0.0F, 0.0F, 0.0F));

        // Tail segments
        for (int i = 0; i < 32; i++) {
            partdefinition.addOrReplaceChild("tail_segment_" + i,
                CubeListBuilder.create()
                    .texOffs(16, 0)
                    .addBox(-1.0F, -1.0F, -1.0F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        }

        // Tail tip
        partdefinition.addOrReplaceChild("tail_tip",
            CubeListBuilder.create()
                .texOffs(24, 0)
                .addBox(-0.5F, -0.5F, -0.5F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)),
            PartPose.offset(0.0F, 0.0F, 0.0F));

        return LayerDefinition.create(meshdefinition, 32, 32);
    }

    @Override
    public void setupAnim(GenieTailState tailState, float limbSwing, float limbSwingAmount,
                         float ageInTicks, float netHeadYaw, float headPitch) {

        // Update animation progress
        this.animationProgress = ageInTicks * 0.05f;

        // Calculate target length
        float targetLength = tailState.getLength();

        // Smooth transition between lengths
        if (Math.abs(targetLength - previousTailLength) > 1.0f) {
            previousTailLength = targetLength;
        }

        // Calculate sway animation
        float sway = (float) Math.sin(animationProgress) * 0.1f;
        float swaySpeed = tailState.getSwaySpeed();

        // Apply sway to all segments
        for (int i = 0; i < tailSegments.length; i++) {
            if (i < targetLength) {
                ModelPart segment = tailSegments[i];

                // Calculate segment-specific sway
                float segmentSway = sway * (1.0f - (float)i / targetLength);
                float segmentRotation = segmentSway * swaySpeed * 20.0f;

                // Apply rotation
                segment.xRot = segmentRotation;
                segment.yRot = segmentSway * 10.0f;

                // Position segments along tail curve
                float segmentProgress = (float)i / targetLength;
                float segmentScale = 1.0f - segmentProgress * 0.3f;

                segment.xScale = segmentScale;
                segment.yScale = segmentScale;
                segment.zScale = segmentScale;

                // Position segments in a curve
                float curveY = (float) Math.sin(segmentProgress * Math.PI) * 0.5f;
                float curveZ = (float) Math.cos(segmentProgress * Math.PI) * 0.2f;

                segment.y += curveY;
                segment.z += curveZ;

            } else {
                // Hide unused segments
                tailSegments[i].visible = false;
            }
        }

        // Position tail base
        tailBase.xRot = headPitch * 0.017453292f;
        tailBase.yRot = netHeadYaw * 0.017453292f * 0.5f;

        // Position tail tip
        if (targetLength > 0) {
            tailTip.visible = true;
            tailTip.x = tailSegments[(int)targetLength - 1].x;
            tailTip.y = tailSegments[(int)targetLength - 1].y;
            tailTip.z = tailSegments[(int)targetLength - 1].z;
        } else {
            tailTip.visible = false;
        }
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer,
                              int packedLight, int packedOverlay, float red, float green,
                              float blue, float alpha) {

        if (!visible) return;

        // Render tail base
        tailBase.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);

        // Render tail segments
        for (ModelPart segment : tailSegments) {
            if (segment.visible) {
                segment.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
            }
        }

        // Render tail tip
        tailTip.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
    }

    /**
     * Update tail visibility based on state
     */
    public void updateVisibility(GenieTailState tailState) {
        this.visible = tailState.isVisible();
    }

    /**
     * Get the tail base model part for attachment
     */
    public ModelPart getTailBase() {
        return tailBase;
    }
}
