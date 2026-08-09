package dev.romankrukovsky.kubanhorizons.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import dev.romankrukovsky.kubanhorizons.util.KHIds;

/**
 * Дымовой хвост игрока-джиннии: семь сужающихся сегментов вместо ног.
 *
 * <p>Геометрия ванильная, а не GeckoLib, хотя у самой джиннии модель именно
 * гекколибовская с костями {@code tail1}–{@code tail7}. Причина в том, что
 * рендер игрока в 26.2 — это {@code AvatarRenderer} с ванильной
 * {@code PlayerModel}, и {@code GeoRenderState} для игрока никто не собирает:
 * игрок не {@code GeoAnimatable}. Подмена всей модели лишила бы игрока скина и
 * брони, поэтому хвост — отдельный слой поверх обычного игрока.</p>
 *
 * <p>Сегменты повторяют пропорции хвоста джиннии: от 5 единиц у бёдер до 2 у
 * кончика. Каждый следующий короче и уже предыдущего, поэтому хвост читается
 * как сужающийся дым, а не как цепь одинаковых кубов.</p>
 */
public final class GenieTailModel {
    /** Слой хвоста; та же текстура, что у самой джиннии — облик один. */
    public static final Identifier TEXTURE = KHIds.of("textures/entity/kuban_genie.png");

    /** Сегментов столько же, сколько костей tail1–tail7 в модели джиннии. */
    public static final int SEGMENTS = 7;

    private final ModelPart root;
    private final ModelPart[] segments = new ModelPart[SEGMENTS];

    public GenieTailModel(ModelPart root) {
        this.root = root;
        ModelPart current = root;
        for (int i = 0; i < SEGMENTS; i++) {
            current = current.getChild(segmentName(i));
            segments[i] = current;
        }
    }

    private static String segmentName(int index) {
        return "tail" + (index + 1);
    }

    /**
     * Строит вложенную цепочку сегментов по геометрии самой джиннии.
     *
     * <p>Размеры и UV скопированы из {@code kuban_genie.geo.json} — костей
     * {@code tail1}–{@code tail7}. Придумывать их заново было бы двойной работой
     * и дало бы хвост, не совпадающий с хвостом джиннии на той же текстуре: UV
     * там не по сетке, каждая кость лежит в своём месте атласа.</p>
     *
     * <p>Вложенность, а не список кубов рядом: поворот второго сегмента должен
     * тянуть за собой все последующие, иначе хвост при движении распадается на
     * несвязанные звенья. Смещение ребёнка по Y — разница origin'ов соседних
     * костей из того же файла.</p>
     */
    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition parent = mesh.getRoot();

        // width, height, depth, uvX, uvY — как в kuban_genie.geo.json.
        float[][] boxes = {
                {8.0F, 5.0F, 6.4F, 66.0F, 16.0F},
                {7.4F, 5.0F, 6.0F, 36.0F, 26.0F},
                {6.8F, 4.5F, 5.6F, 64.0F, 28.0F},
                {6.2F, 4.5F, 5.0F, 50.0F, 39.0F},
                {5.4F, 4.0F, 4.4F, 106.0F, 51.0F},
                {4.6F, 4.0F, 3.8F, 62.0F, 57.0F},
                {3.6F, 4.0F, 3.0F, 74.0F, 39.0F},
        };

        for (int i = 0; i < SEGMENTS; i++) {
            float width = boxes[i][0];
            float height = boxes[i][1];
            float depth = boxes[i][2];
            parent = parent.addOrReplaceChild(
                    segmentName(i),
                    CubeListBuilder.create()
                            .texOffs((int) boxes[i][3], (int) boxes[i][4])
                            .addBox(-width / 2.0F, 0.0F, -depth / 2.0F, width, height, depth),
                    // Первый сегмент крепится к бёдрам игрока (12 единиц вниз от
                    // центра модели), каждый следующий — к низу предыдущего.
                    i == 0
                            ? PartPose.offset(0.0F, 12.0F, 0.0F)
                            : PartPose.offset(0.0F, boxes[i - 1][1], 0.0F));
        }
        // 128×128 — фактический размер kuban_genie.png. При 64×64 UV разъехались
        // бы вдвое и на хвост попал бы фрагмент лица.
        return LayerDefinition.create(mesh, 128, 128);
    }

    /**
     * Волна по хвосту: каждый сегмент отстаёт от предыдущего по фазе.
     *
     * @param ageInTicks время для колебания в покое
     * @param speed      скорость игрока — чем быстрее, тем сильнее хвост тянется назад
     */
    public void animate(float ageInTicks, float speed) {
        // Фазовый сдвиг даёт бегущую волну вместо синхронного качания всей цепи.
        float amplitude = 0.12F + Math.min(0.25F, speed * 1.5F);
        for (int i = 0; i < SEGMENTS; i++) {
            float phase = ageInTicks * 0.12F - i * 0.55F;
            segments[i].yRot = (float) Math.sin(phase) * amplitude;
            // Небольшой наклон вниз накапливается к кончику: хвост провисает.
            segments[i].xRot = 0.08F + (float) Math.cos(phase) * amplitude * 0.4F;
        }
    }

    public void submit(PoseStack poseStack, SubmitNodeCollector collector, int lightCoords, int overlay) {
        collector.submitModelPart(root, poseStack, renderType(), lightCoords, overlay, null);
    }

    private static RenderType renderType() {
        // Cutout, а не solid: текстура джиннии содержит прозрачные пиксели.
        return RenderTypes.entityCutout(TEXTURE);
    }
}
