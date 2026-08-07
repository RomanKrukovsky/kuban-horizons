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
 * Модель нутрии: болотный грызун плавней.
 *
 * <p>Ванильная сетка четвероногого здесь не годится: нутрию узнают по трём
 * приметам, которых у неё нет — горбатая спина, длинный голый хвост и крупные
 * оранжевые резцы. Поэтому сетка своя, а UV-слоты посчитаны по раскладке
 * граней, чтобы части не перекрывались.</p>
 *
 * <p>Хвост в воде работает веслом: он виляет из стороны в сторону, когда
 * зверь плывёт, и почти неподвижен на земле. Это самый дешёвый способ
 * показать, что нутрия — водное животное, без отдельной модели для плавания.</p>
 */
public class NutriaModel extends EntityModel<NutriaRenderState> {
    private final ModelPart head;
    private final ModelPart tail;
    private final ModelPart rightHindLeg;
    private final ModelPart leftHindLeg;
    private final ModelPart rightFrontLeg;
    private final ModelPart leftFrontLeg;

    public NutriaModel(ModelPart root) {
        super(root);
        this.head = root.getChild("head");
        this.tail = root.getChild("tail");
        this.rightHindLeg = root.getChild("right_hind_leg");
        this.leftHindLeg = root.getChild("left_hind_leg");
        this.rightFrontLeg = root.getChild("right_front_leg");
        this.leftFrontLeg = root.getChild("left_front_leg");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // Корпус горбатый: у нутрии спина заметно выгнута, это её силуэт.
        root.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(0, 0)
                        .addBox(-3.0F, -3.0F, -3.5F, 6.0F, 8.0F, 7.0F),
                PartPose.offsetAndRotation(0.0F, 18.0F, 0.0F,
                        (float) (Math.PI / 2), 0.0F, 0.0F));

        PartDefinition head = root.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(26, 0)
                        .addBox(-2.0F, -2.0F, -2.0F, 4.0F, 4.0F, 4.0F),
                PartPose.offset(0.0F, 17.0F, -4.0F));
        // Тупое рыло вперёд.
        head.addOrReplaceChild("snout",
                CubeListBuilder.create().texOffs(19, 0)
                        .addBox(-1.5F, -0.5F, -2.0F, 3.0F, 2.0F, 2.0F),
                PartPose.offset(0.0F, 0.5F, -2.0F));
        // Резцы: узкая пластинка под рылом. Оранжевые зубы — примета вида.
        head.addOrReplaceChild("incisors",
                CubeListBuilder.create().texOffs(58, 0)
                        .addBox(-1.0F, 1.0F, -1.0F, 2.0F, 1.0F, 1.0F),
                PartPose.offset(0.0F, 0.5F, -3.5F));
        // Маленькие круглые уши высоко на голове.
        head.addOrReplaceChild("right_ear",
                CubeListBuilder.create().texOffs(0, 0)
                        .addBox(-1.0F, -1.5F, -0.5F, 2.0F, 2.0F, 1.0F),
                PartPose.offset(-1.5F, -2.0F, 0.5F));
        head.addOrReplaceChild("left_ear",
                CubeListBuilder.create().texOffs(38, 0)
                        .addBox(-1.0F, -1.5F, -0.5F, 2.0F, 2.0F, 1.0F),
                PartPose.offset(1.5F, -2.0F, 0.5F));

        // Хвост длинный и голый — вторая примета после резцов.
        root.addOrReplaceChild("tail",
                CubeListBuilder.create().texOffs(44, 0)
                        .addBox(-1.0F, 0.0F, 0.0F, 2.0F, 9.0F, 2.0F),
                PartPose.offsetAndRotation(0.0F, 19.0F, 3.5F, 1.2F, 0.0F, 0.0F));

        root.addOrReplaceChild("right_hind_leg",
                CubeListBuilder.create().texOffs(52, 0)
                        .addBox(-1.0F, 0.0F, -1.0F, 2.0F, 3.0F, 2.0F),
                PartPose.offset(-2.0F, 21.0F, 2.5F));
        root.addOrReplaceChild("left_hind_leg",
                CubeListBuilder.create().texOffs(52, 5)
                        .addBox(-1.0F, 0.0F, -1.0F, 2.0F, 3.0F, 2.0F),
                PartPose.offset(2.0F, 21.0F, 2.5F));
        root.addOrReplaceChild("right_front_leg",
                CubeListBuilder.create().texOffs(26, 8)
                        .addBox(-1.0F, 0.0F, -1.0F, 2.0F, 3.0F, 2.0F),
                PartPose.offset(-2.0F, 21.0F, -2.5F));
        root.addOrReplaceChild("left_front_leg",
                CubeListBuilder.create().texOffs(34, 8)
                        .addBox(-1.0F, 0.0F, -1.0F, 2.0F, 3.0F, 2.0F),
                PartPose.offset(2.0F, 21.0F, -2.5F));

        return LayerDefinition.create(mesh, 64, 32);
    }

    @Override
    public void setupAnim(NutriaRenderState state) {
        super.setupAnim(state);
        head.xRot = state.xRot * (float) (Math.PI / 180.0);
        head.yRot = state.yRot * (float) (Math.PI / 180.0);

        float pos = state.walkAnimationPos;
        float speed = state.walkAnimationSpeed;
        rightHindLeg.xRot = Mth.cos(pos * 0.6662F) * 1.2F * speed;
        leftHindLeg.xRot = Mth.cos(pos * 0.6662F + (float) Math.PI) * 1.2F * speed;
        rightFrontLeg.xRot = Mth.cos(pos * 0.6662F + (float) Math.PI) * 1.2F * speed;
        leftFrontLeg.xRot = Mth.cos(pos * 0.6662F) * 1.2F * speed;

        if (state.swimming) {
            // В воде хвост работает веслом — виляет вбок, корпус выпрямляется.
            tail.yRot = Mth.cos(state.ageInTicks * 0.4F) * 0.5F;
            tail.xRot = 1.55F;
        } else {
            tail.yRot = Mth.cos(pos * 0.6662F) * 0.2F * speed;
            tail.xRot = 1.2F;
        }
    }
}
