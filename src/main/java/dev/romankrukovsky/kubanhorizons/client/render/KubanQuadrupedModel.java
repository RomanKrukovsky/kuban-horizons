package dev.romankrukovsky.kubanhorizons.client.render;

import net.minecraft.client.model.QuadrupedModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

/**
 * Модель кубанского четвероногого: кабан и кавказская овчарка.
 *
 * <p>Сетка строится на ванильной {@link QuadrupedModel#createBodyMesh}, а не
 * пишется с нуля: у неё уже выверенная UV-развёртка 64×32 (голова 0,0; корпус
 * 28,8; лапа 0,16), поэтому текстуры этих зверей раскладываются по той же
 * схеме, что у ванильной свиньи и волка. Своя сетка означала бы свою
 * развёртку — и любой промах в texOffs дал бы шерсть на морде.</p>
 *
 * <p>Поверх ванильного корпуса добавляются части, которые и делают силуэт
 * узнаваемым: кабану — клинья-загривок и рыло, овчарке — уши и пушистый хвост.
 * По ART_BIBLE §4 силуэт важнее внутренней детализации, поэтому лишних кубов
 * нет.</p>
 */
public class KubanQuadrupedModel extends QuadrupedModel<KubanQuadrupedRenderState> {
    /** Хвост есть у обоих, но ведёт себя по-разному. */
    private final ModelPart tail;
    private final boolean sittable;

    public KubanQuadrupedModel(ModelPart root, boolean sittable) {
        super(root);
        this.tail = root.getChild("tail");
        this.sittable = sittable;
    }

    /**
     * Кабан: приземистый, с тяжёлым передом и загривком.
     *
     * <p>Кабана от свиньи отличает именно горб на загривке и вытянутое рыло —
     * без них в игре получилась бы тёмная свинья.</p>
     */
    public static LayerDefinition createBoarLayer() {
        MeshDefinition mesh = QuadrupedModel.createBodyMesh(10, false, false,
                CubeDeformation.NONE);
        PartDefinition root = mesh.getRoot();

        // Загривок: клин над корпусом, приподнимающий силуэт спереди.
        // texOffs ниже y=32 — ванильная зона развёртки (голова 0,0; корпус 28,8;
        // лапа 0,16) занята целиком, и любое пересечение с ней дало бы шерсть
        // корпуса на морде. Свободные слоты посчитаны по раскладке граней.
        root.addOrReplaceChild("withers",
                CubeListBuilder.create().texOffs(0, 32)
                        .addBox(-4.0F, -3.0F, -4.0F, 8.0F, 4.0F, 7.0F),
                PartPose.offset(0.0F, 8.0F, -6.0F));

        // Рыло — отдельным кубом на голове, чтобы читалось в профиль.
        PartDefinition head = root.getChild("head");
        head.addOrReplaceChild("snout",
                CubeListBuilder.create().texOffs(23, 32)
                        .addBox(-2.0F, 0.0F, -3.0F, 4.0F, 3.0F, 3.0F),
                PartPose.offset(0.0F, 0.0F, -8.0F));

        root.addOrReplaceChild("tail",
                CubeListBuilder.create().texOffs(37, 32)
                        .addBox(-1.0F, 0.0F, 0.0F, 2.0F, 6.0F, 2.0F),
                PartPose.offsetAndRotation(0.0F, 9.0F, 7.0F, 0.4F, 0.0F, 0.0F));

        return LayerDefinition.create(mesh, 64, 64);
    }

    /**
     * Кавказская овчарка: крупная, лохматая, с широкой головой.
     *
     * <p>Уши — короткие треугольные клинья, а не висячие: у породы они
     * купированы, и это самый узнаваемый признак вблизи.</p>
     */
    public static LayerDefinition createShepherdLayer() {
        MeshDefinition mesh = QuadrupedModel.createBodyMesh(12, false, false,
                CubeDeformation.NONE);
        PartDefinition root = mesh.getRoot();
        PartDefinition head = root.getChild("head");

        head.addOrReplaceChild("right_ear",
                CubeListBuilder.create().texOffs(0, 32)
                        .addBox(-1.5F, -2.0F, -1.0F, 3.0F, 3.0F, 1.0F),
                PartPose.offset(-2.5F, -3.0F, -3.0F));
        head.addOrReplaceChild("left_ear",
                CubeListBuilder.create().texOffs(8, 32)
                        .addBox(-1.5F, -2.0F, -1.0F, 3.0F, 3.0F, 1.0F),
                PartPose.offset(2.5F, -3.0F, -3.0F));
        head.addOrReplaceChild("muzzle",
                CubeListBuilder.create().texOffs(16, 32)
                        .addBox(-2.0F, 0.0F, -3.0F, 4.0F, 3.0F, 3.0F),
                PartPose.offset(0.0F, 0.0F, -8.0F));

        // Пушистый хвост: у овчарки он толще и держится выше кабаньего.
        root.addOrReplaceChild("tail",
                CubeListBuilder.create().texOffs(30, 32)
                        .addBox(-1.5F, 0.0F, 0.0F, 3.0F, 7.0F, 3.0F),
                PartPose.offsetAndRotation(0.0F, 8.0F, 7.0F, -0.6F, 0.0F, 0.0F));

        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(KubanQuadrupedRenderState state) {
        super.setupAnim(state);
        if (sittable && state.sitting) {
            // Сидячая поза: корпус откинут назад, задние лапы подобраны.
            // Ванильный setupAnim уже развернул лапы по ходьбе, поэтому позу
            // задаём после него, перезаписывая углы.
            body.xRot = (float) (Math.PI / 2) - 0.35F;
            body.y = 20.0F;
            head.y = 20.0F;
            rightHindLeg.xRot = -1.4F;
            leftHindLeg.xRot = -1.4F;
            rightFrontLeg.xRot = 0.0F;
            leftFrontLeg.xRot = 0.0F;
            rightHindLeg.y = 22.0F;
            leftHindLeg.y = 22.0F;
            tail.xRot = -0.2F;
            return;
        }
        // Хвост качается в такт шагу — дешёвый признак живого зверя.
        tail.yRot = Mth.cos(state.walkAnimationPos * 0.6662F) * 0.3F
                * state.walkAnimationSpeed;
    }
}
