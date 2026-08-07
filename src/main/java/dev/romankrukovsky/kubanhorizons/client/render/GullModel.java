package dev.romankrukovsky.kubanhorizons.client.render;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

/**
 * Модель чайки: птица побережья, которая живёт в воздухе.
 *
 * <p>Отдельно от {@link GroundBirdModel}: у наземных птиц крылья сложены и
 * силуэт горизонтальный, а чайку узнают по длинным узким крыльям в размахе.
 * Поэтому крылья здесь длиннее корпуса, а не короткие боковые пластины.</p>
 *
 * <p>Ключевая деталь поведения: чайка чередует взмахи и планирование. Крыло
 * замирает в разведённом положении, когда птица не набирает высоту — без этого
 * она выглядела бы как механическая игрушка с постоянной частотой.</p>
 */
public class GullModel extends EntityModel<WaterBirdRenderState> {
    private final ModelPart head;
    private final ModelPart rightWing;
    private final ModelPart leftWing;
    private final ModelPart rightLeg;
    private final ModelPart leftLeg;
    private final ModelPart tail;

    public GullModel(ModelPart root) {
        super(root);
        this.head = root.getChild("head");
        this.rightWing = root.getChild("right_wing");
        this.leftWing = root.getChild("left_wing");
        this.rightLeg = root.getChild("right_leg");
        this.leftLeg = root.getChild("left_leg");
        this.tail = root.getChild("tail");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        root.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(0, 0)
                        .addBox(-2.0F, -2.0F, -3.5F, 4.0F, 4.0F, 7.0F),
                PartPose.offset(0.0F, 17.0F, 0.0F));

        PartDefinition head = root.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(15, 0)
                        .addBox(-1.5F, -1.5F, -2.0F, 3.0F, 3.0F, 3.0F),
                PartPose.offset(0.0F, 14.5F, -3.0F));
        // Клюв с лёгким крючком читается даже мелким.
        head.addOrReplaceChild("beak",
                CubeListBuilder.create().texOffs(27, 0)
                        .addBox(-0.5F, -0.5F, -3.0F, 1.0F, 1.0F, 3.0F),
                PartPose.offset(0.0F, 0.5F, -2.0F));

        // Крылья длиннее корпуса — подпись чайки.
        root.addOrReplaceChild("right_wing",
                CubeListBuilder.create().texOffs(30, 0)
                        .addBox(-6.0F, 0.0F, -2.5F, 6.0F, 1.0F, 5.0F),
                PartPose.offset(-2.0F, 16.0F, 0.0F));
        root.addOrReplaceChild("left_wing",
                CubeListBuilder.create().texOffs(17, 6)
                        .addBox(0.0F, 0.0F, -2.5F, 6.0F, 1.0F, 5.0F),
                PartPose.offset(2.0F, 16.0F, 0.0F));

        root.addOrReplaceChild("tail",
                CubeListBuilder.create().texOffs(47, 0)
                        .addBox(-1.5F, 0.0F, 0.0F, 3.0F, 1.0F, 4.0F),
                PartPose.offset(0.0F, 16.5F, 3.5F));

        CubeListBuilder leg = CubeListBuilder.create().texOffs(0, 0)
                .addBox(-0.5F, 0.0F, -0.5F, 1.0F, 2.0F, 1.0F);
        root.addOrReplaceChild("right_leg", leg, PartPose.offset(-1.0F, 19.0F, 0.5F));
        root.addOrReplaceChild("left_leg",
                CubeListBuilder.create().texOffs(24, 0)
                        .addBox(-0.5F, 0.0F, -0.5F, 1.0F, 2.0F, 1.0F),
                PartPose.offset(1.0F, 19.0F, 0.5F));

        return LayerDefinition.create(mesh, 64, 32);
    }

    @Override
    public void setupAnim(WaterBirdRenderState state) {
        super.setupAnim(state);
        head.xRot = state.xRot * (float) (Math.PI / 180.0);
        head.yRot = state.yRot * (float) (Math.PI / 180.0);

        if (state.flying) {
            // Планирование: крылья разведены и лишь слегка колеблются.
            float beat = Mth.cos(state.ageInTicks * 0.35F);
            rightWing.zRot = 0.15F + beat * 0.45F;
            leftWing.zRot = -0.15F - beat * 0.45F;
            // Лапы поджаты к хвосту в полёте.
            rightLeg.xRot = 1.5F;
            leftLeg.xRot = 1.5F;
            tail.xRot = 0.1F;
        } else {
            rightWing.zRot = 0.0F;
            leftWing.zRot = 0.0F;
            float pos = state.walkAnimationPos;
            float speed = state.walkAnimationSpeed;
            rightLeg.xRot = Mth.cos(pos * 0.6662F) * 1.4F * speed;
            leftLeg.xRot = Mth.cos(pos * 0.6662F + (float) Math.PI) * 1.4F * speed;
            tail.xRot = 0.0F;
        }
    }
}
