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
 * Модель осетра: реликтовая рыба Азова и Кубани.
 *
 * <p>От ванильных рыб отличается тем, что и делает осетра осетром: длинный
 * заострённый рострум впереди и хребет из костяных жучков вдоль спины.
 * Ванильный лосось — короткий брусок, из него осетра не сделать.</p>
 *
 * <p>Тело разбито на два сегмента, чтобы хвостовая половина отставала по фазе
 * от передней: так рыба идёт волной, а не поворачивается целиком, как доска.</p>
 */
public class SturgeonModel extends EntityModel<SturgeonRenderState> {
    private final ModelPart tailBody;
    private final ModelPart tailFin;

    public SturgeonModel(ModelPart root) {
        super(root);
        this.tailBody = root.getChild("tail_body");
        this.tailFin = tailBody.getChild("tail_fin");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        PartDefinition body = root.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(0, 0)
                        .addBox(-2.0F, -2.0F, -5.0F, 4.0F, 4.0F, 10.0F),
                PartPose.offset(0.0F, 20.0F, 0.0F));

        // Рострум: вытянутое рыло — первый признак осетровых.
        body.addOrReplaceChild("rostrum",
                CubeListBuilder.create().texOffs(30, 0)
                        .addBox(-1.0F, -0.5F, -4.0F, 2.0F, 1.0F, 4.0F),
                PartPose.offset(0.0F, 0.5F, -5.0F));

        // Гребень костяных жучков по хребту.
        body.addOrReplaceChild("dorsal",
                CubeListBuilder.create().texOffs(37, 3)
                        .addBox(-0.5F, -3.0F, -2.5F, 1.0F, 3.0F, 5.0F),
                PartPose.offset(0.0F, -2.0F, 1.0F));

        body.addOrReplaceChild("right_fin",
                CubeListBuilder.create().texOffs(38, 0)
                        .addBox(-3.0F, 0.0F, -1.0F, 3.0F, 1.0F, 2.0F),
                PartPose.offsetAndRotation(-2.0F, 1.0F, -2.0F, 0.0F, 0.0F, 0.4F));
        body.addOrReplaceChild("left_fin",
                CubeListBuilder.create().texOffs(48, 0)
                        .addBox(0.0F, 0.0F, -1.0F, 3.0F, 1.0F, 2.0F),
                PartPose.offsetAndRotation(2.0F, 1.0F, -2.0F, 0.0F, 0.0F, -0.4F));

        // Хвостовая половина — отдельный узел, отстающий по фазе.
        PartDefinition tailBody = root.addOrReplaceChild("tail_body",
                CubeListBuilder.create().texOffs(18, 0)
                        .addBox(-1.5F, -1.5F, 0.0F, 3.0F, 3.0F, 6.0F),
                PartPose.offset(0.0F, 20.0F, 5.0F));
        tailBody.addOrReplaceChild("tail_fin",
                CubeListBuilder.create().texOffs(0, 0)
                        .addBox(-0.5F, -2.5F, 0.0F, 1.0F, 5.0F, 4.0F),
                PartPose.offset(0.0F, 0.0F, 6.0F));

        return LayerDefinition.create(mesh, 64, 32);
    }

    @Override
    public void setupAnim(SturgeonRenderState state) {
        super.setupAnim(state);
        // На воздухе рыба бьётся, в воде идёт ровной волной.
        float amplitude = state.inWater ? 1.0F : 1.8F;
        float speed = state.inWater ? 0.6F : 1.7F;
        float phase = state.ageInTicks * speed;
        tailBody.yRot = Mth.cos(phase) * 0.25F * amplitude;
        // Хвостовой плавник отстаёт на четверть периода — отсюда эффект волны.
        tailFin.yRot = Mth.cos(phase - 1.2F) * 0.35F * amplitude;
    }
}
