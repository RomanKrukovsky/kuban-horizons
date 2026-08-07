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
 * Модель летающего насекомого: саранча и кавказская пчела.
 *
 * <p>Одна сетка на двоих: у обоих сегментированное тело, пара крыльев и усики,
 * а различия — в пропорциях и окрасе, что задаётся параметром и текстурой.
 * Ванильная пчела не подошла бы: она втрое крупнее и имеет жало-выпад, которого
 * у саранчи нет.</p>
 *
 * <p>Крылья машут всегда, пока насекомое в воздухе: у настоящих насекомых нет
 * планирования, и неподвижные крылья читались бы как «мёртвое зависшее тело».
 * Частота намеренно высокая — это единственный признак жизни на таком размере.</p>
 */
public class InsectModel extends EntityModel<InsectRenderState> {
    private final ModelPart rightWing;
    private final ModelPart leftWing;
    private final ModelPart rightLeg;
    private final ModelPart leftLeg;

    public InsectModel(ModelPart root) {
        super(root);
        this.rightWing = root.getChild("right_wing");
        this.leftWing = root.getChild("left_wing");
        this.rightLeg = root.getChild("right_leg");
        this.leftLeg = root.getChild("left_leg");
    }

    /**
     * @param bodyLength длина брюшка: саранча вытянутая (6), пчела короче (4)
     * @param legLength  длина прыжковой ноги: у саранчи она заметно длиннее
     */
    public static LayerDefinition createBodyLayer(int bodyLength, int legLength) {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // Брюшко. Модель висит около y=20: насекомое летает низко над грядкой.
        root.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(0, 0)
                        .addBox(-2.0F, -1.5F, -3.0F, 4.0F, 3.0F, bodyLength),
                PartPose.offset(0.0F, 20.0F, 0.0F));

        PartDefinition head = root.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(14, 0)
                        .addBox(-1.5F, -1.5F, -3.0F, 3.0F, 3.0F, 3.0F),
                PartPose.offset(0.0F, 20.0F, -3.0F));
        head.addOrReplaceChild("antenna",
                CubeListBuilder.create().texOffs(40, 0)
                        .addBox(-0.5F, -2.0F, -1.0F, 1.0F, 2.0F, 1.0F),
                PartPose.offsetAndRotation(0.0F, -1.5F, -2.0F, -0.4F, 0.0F, 0.0F));

        root.addOrReplaceChild("right_wing",
                CubeListBuilder.create().texOffs(26, 0)
                        .addBox(-5.0F, 0.0F, -2.0F, 5.0F, 1.0F, 4.0F),
                PartPose.offset(-1.5F, 18.5F, -1.0F));
        root.addOrReplaceChild("left_wing",
                CubeListBuilder.create().texOffs(44, 0)
                        .addBox(0.0F, 0.0F, -2.0F, 5.0F, 1.0F, 4.0F),
                PartPose.offset(1.5F, 18.5F, -1.0F));

        // Прыжковые ноги: у саранчи это её подпись, поэтому вынесены наружу.
        root.addOrReplaceChild("right_leg",
                CubeListBuilder.create().texOffs(0, 0)
                        .addBox(-0.5F, 0.0F, -0.5F, 1.0F, legLength, 1.0F),
                PartPose.offsetAndRotation(-1.5F, 21.0F, 1.0F, 0.5F, 0.0F, -0.4F));
        root.addOrReplaceChild("left_leg",
                CubeListBuilder.create().texOffs(26, 0)
                        .addBox(-0.5F, 0.0F, -0.5F, 1.0F, legLength, 1.0F),
                PartPose.offsetAndRotation(1.5F, 21.0F, 1.0F, 0.5F, 0.0F, 0.4F));

        return LayerDefinition.create(mesh, 64, 32);
    }

    @Override
    public void setupAnim(InsectRenderState state) {
        super.setupAnim(state);
        // Крылья: быстрый мелкий взмах, пока насекомое в воздухе.
        float flap = state.flying
                ? Mth.cos(state.ageInTicks * 2.1F) * 0.8F + 0.4F
                : 0.0F;
        rightWing.zRot = flap;
        leftWing.zRot = -flap;
        // Ноги поджимаются в полёте и опускаются при посадке.
        float tuck = state.flying ? 0.9F : 0.5F;
        rightLeg.xRot = tuck;
        leftLeg.xRot = tuck;
    }
}
