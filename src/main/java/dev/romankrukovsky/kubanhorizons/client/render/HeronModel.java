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
 * Модель серой цапли: часовой плавней.
 *
 * <p>Цапля — самая высокая птица мода (хитбокс 0.6×1.4), и вся её узнаваемость
 * в вертикали: ходульные ноги, S-образная шея и клюв-кинжал. Поэтому шея здесь
 * отдельная часть с двумя позами, а не жёстко приклеенная к корпусу голова.</p>
 *
 * <p>Две позы решают главную читаемость: стоящая цапля складывает шею в S и
 * замирает над водой, летящая — вытягивает шею и ноги в одну линию. Без этого
 * силуэт в полёте выглядел бы как ошибка модели.</p>
 */
public class HeronModel extends EntityModel<WaterBirdRenderState> {
    private final ModelPart neck;
    private final ModelPart head;
    private final ModelPart rightWing;
    private final ModelPart leftWing;
    private final ModelPart rightLeg;
    private final ModelPart leftLeg;

    public HeronModel(ModelPart root) {
        super(root);
        this.neck = root.getChild("neck");
        this.head = neck.getChild("head");
        this.rightWing = root.getChild("right_wing");
        this.leftWing = root.getChild("left_wing");
        this.rightLeg = root.getChild("right_leg");
        this.leftLeg = root.getChild("left_leg");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // Корпус висит высоко: ноги занимают почти половину роста.
        root.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(0, 0)
                        .addBox(-2.0F, -2.5F, -4.0F, 4.0F, 5.0F, 8.0F),
                PartPose.offset(0.0F, 10.0F, 0.0F));

        PartDefinition neck = root.addOrReplaceChild("neck",
                CubeListBuilder.create().texOffs(24, 0)
                        .addBox(-1.0F, -7.0F, -1.0F, 2.0F, 7.0F, 2.0F),
                PartPose.offset(0.0F, 8.0F, -2.5F));
        PartDefinition head = neck.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(32, 0)
                        .addBox(-1.5F, -2.0F, -1.5F, 3.0F, 2.0F, 3.0F),
                PartPose.offset(0.0F, -7.0F, 0.0F));
        // Клюв-кинжал: длинный и прямой, главный признак цапли.
        head.addOrReplaceChild("beak",
                CubeListBuilder.create().texOffs(39, 0)
                        .addBox(-0.5F, -1.0F, -5.0F, 1.0F, 1.0F, 5.0F),
                PartPose.offset(0.0F, 0.0F, -1.5F));

        root.addOrReplaceChild("right_wing",
                CubeListBuilder.create().texOffs(25, 6)
                        .addBox(-5.0F, 0.0F, -3.5F, 5.0F, 1.0F, 7.0F),
                PartPose.offset(-2.0F, 8.5F, 0.0F));
        root.addOrReplaceChild("left_wing",
                CubeListBuilder.create().texOffs(0, 13)
                        .addBox(0.0F, 0.0F, -3.5F, 5.0F, 1.0F, 7.0F),
                PartPose.offset(2.0F, 8.5F, 0.0F));

        // Ходульные ноги: 8 пикселей — половина роста птицы.
        root.addOrReplaceChild("right_leg",
                CubeListBuilder.create().texOffs(51, 0)
                        .addBox(-0.5F, 0.0F, -0.5F, 1.0F, 8.0F, 1.0F),
                PartPose.offset(-1.0F, 12.0F, 0.5F));
        root.addOrReplaceChild("left_leg",
                CubeListBuilder.create().texOffs(55, 0)
                        .addBox(-0.5F, 0.0F, -0.5F, 1.0F, 8.0F, 1.0F),
                PartPose.offset(1.0F, 12.0F, 0.5F));

        return LayerDefinition.create(mesh, 64, 32);
    }

    @Override
    public void setupAnim(WaterBirdRenderState state) {
        super.setupAnim(state);
        head.yRot = state.yRot * (float) (Math.PI / 180.0);

        if (state.flying) {
            // В полёте шея и ноги вытянуты в линию, крылья машут медленно и широко.
            neck.xRot = 0.25F;
            head.xRot = -0.25F;
            float beat = Mth.cos(state.ageInTicks * 0.25F);
            rightWing.zRot = beat * 0.7F;
            leftWing.zRot = -beat * 0.7F;
            rightLeg.xRot = 1.55F;
            leftLeg.xRot = 1.55F;
            return;
        }

        rightWing.zRot = 0.0F;
        leftWing.zRot = 0.0F;
        if (state.stalking) {
            // Караулит: шея сложена назад, голова опущена к воде, ноги неподвижны.
            neck.xRot = 0.7F;
            head.xRot = -0.9F;
            rightLeg.xRot = 0.0F;
            leftLeg.xRot = 0.0F;
            return;
        }

        neck.xRot = state.xRot * (float) (Math.PI / 180.0) * 0.5F;
        head.xRot = 0.0F;
        float pos = state.walkAnimationPos;
        float speed = state.walkAnimationSpeed;
        // Шаг цапли высокий и медленный — амплитуда больше, чем у мелких птиц.
        rightLeg.xRot = Mth.cos(pos * 0.4F) * 1.0F * speed;
        leftLeg.xRot = Mth.cos(pos * 0.4F + (float) Math.PI) * 1.0F * speed;
    }
}
