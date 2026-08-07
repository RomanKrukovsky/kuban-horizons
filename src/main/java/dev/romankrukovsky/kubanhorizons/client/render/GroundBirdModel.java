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
 * Модель наземной птицы: фазан и перепел.
 *
 * Геометрия своя, а не копия ванильной курицы: у фазана длинный хвост, который
 * и делает силуэт узнаваемым в степи, а курица его лишена. Пропорции задаются
 * параметрами, поэтому перепел — та же сетка в меньшем масштабе и с коротким
 * хвостом, что честнее двух почти одинаковых файлов.
 *
 * Хвост поднимается при взлёте вместе с крыльями: у куриных птиц это заметнее
 * всего именно в момент, когда их подняли с земли.
 */
public class GroundBirdModel extends EntityModel<GroundBirdRenderState> {
    private final ModelPart head;
    private final ModelPart rightLeg;
    private final ModelPart leftLeg;
    private final ModelPart rightWing;
    private final ModelPart leftWing;
    private final ModelPart tail;

    public GroundBirdModel(ModelPart root) {
        super(root);
        this.head = root.getChild("head");
        this.rightLeg = root.getChild("right_leg");
        this.leftLeg = root.getChild("left_leg");
        this.rightWing = root.getChild("right_wing");
        this.leftWing = root.getChild("left_wing");
        this.tail = root.getChild("tail");
    }

    /**
     * Сетка птицы.
     *
     * @param tailLength длина хвоста в пикселях модели: фазану 9, перепелу 3
     * @param bodyLength длина корпуса: фазан крупнее
     */
    public static LayerDefinition createBodyLayer(int tailLength, int bodyLength) {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        PartDefinition head = root.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(0, 0)
                        .addBox(-1.5F, -5.0F, -2.0F, 3.0F, 5.0F, 3.0F),
                PartPose.offset(0.0F, 15.0F, -3.0F));
        head.addOrReplaceChild("beak",
                CubeListBuilder.create().texOffs(14, 0)
                        .addBox(-1.0F, -3.5F, -3.5F, 2.0F, 1.0F, 2.0F),
                PartPose.ZERO);

        root.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(0, 9)
                        .addBox(-2.5F, -3.0F, -3.0F, 5.0F, bodyLength, 6.0F),
                PartPose.offsetAndRotation(0.0F, 16.0F, 0.0F,
                        (float) (Math.PI / 2), 0.0F, 0.0F));

        // Хвост — подпись силуэта фазана. Растёт назад от корпуса.
        root.addOrReplaceChild("tail",
                CubeListBuilder.create().texOffs(22, 20)
                        .addBox(-1.5F, 0.0F, 0.0F, 3.0F, 1.0F, tailLength),
                PartPose.offset(0.0F, 13.5F, 3.0F));

        CubeListBuilder leg = CubeListBuilder.create().texOffs(26, 0)
                .addBox(-1.0F, 0.0F, -2.0F, 2.0F, 5.0F, 2.0F);
        root.addOrReplaceChild("right_leg", leg, PartPose.offset(-1.5F, 19.0F, 1.0F));
        root.addOrReplaceChild("left_leg", leg, PartPose.offset(1.5F, 19.0F, 1.0F));

        root.addOrReplaceChild("right_wing",
                CubeListBuilder.create().texOffs(24, 13)
                        .addBox(0.0F, 0.0F, -3.0F, 1.0F, 3.0F, 6.0F),
                PartPose.offset(-3.0F, 13.5F, 0.0F));
        root.addOrReplaceChild("left_wing",
                CubeListBuilder.create().texOffs(24, 13)
                        .addBox(-1.0F, 0.0F, -3.0F, 1.0F, 3.0F, 6.0F),
                PartPose.offset(3.0F, 13.5F, 0.0F));

        return LayerDefinition.create(mesh, 64, 32);
    }

    @Override
    public void setupAnim(GroundBirdRenderState state) {
        super.setupAnim(state);
        head.xRot = state.xRot * (float) (Math.PI / 180.0);
        head.yRot = state.yRot * (float) (Math.PI / 180.0);

        float animationSpeed = state.walkAnimationSpeed;
        float animationPos = state.walkAnimationPos;
        rightLeg.xRot = Mth.cos(animationPos * 0.6662F) * 1.4F * animationSpeed;
        leftLeg.xRot = Mth.cos(animationPos * 0.6662F + (float) Math.PI) * 1.4F
                * animationSpeed;

        float flapAngle = (Mth.sin(state.flap) + 1.0F) * state.flapSpeed;
        rightWing.zRot = flapAngle;
        leftWing.zRot = -flapAngle;
        // Хвост задирается пропорционально взмаху — читаемый признак взлёта.
        tail.xRot = -0.35F * state.flapSpeed;
    }
}
