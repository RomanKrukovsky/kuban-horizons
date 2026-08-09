package dev.romankrukovsky.kubanhorizons.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.romankrukovsky.kubanhorizons.genie.player.PlayerGenieAttachment;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.neoforged.neoforge.client.extensions.IRenderStateExtension;

/**
 * Рисует дымовой хвост поверх обычного игрока, когда тот стал джиннией.
 *
 * <p>Слой, а не подмена модели: скин и броня игрока остаются, добавляется
 * только хвост. Модель хвоста берётся из {@link GenieTailModel}.</p>
 *
 * <p>Состояние читается не из attachment напрямую, а из
 * {@code AvatarRenderState} через {@link GenieRenderStateKeys#GENIE_STAGE}.
 * Так требует пайплайн 26.2: слой получает уже собранное состояние и не имеет
 * доступа к сущности, а обращение к серверному attachment из рендера было бы
 * гонкой — состояние собирается один раз за кадр, а не читается по ходу
 * отрисовки.</p>
 *
 * <p>Хвост появляется со стадии {@code TAIL_FORMATION} — той самой, на которой
 * контроллер трансформации включает полёт и объявляет анатомическое изменение.
 * До неё игрок ещё человек с переписанным телом, и хвоста быть не должно.</p>
 */
public final class GenieTailLayer extends RenderLayer<AvatarRenderState, PlayerModel> {
    private final GenieTailModel tail;

    public GenieTailLayer(RenderLayerParent<AvatarRenderState, PlayerModel> parent, GenieTailModel tail) {
        super(parent);
        this.tail = tail;
    }

    @Override
    public void submit(
            PoseStack poseStack,
            SubmitNodeCollector collector,
            int lightCoords,
            AvatarRenderState state,
            float yRot,
            float xRot) {
        if (state.isInvisible) {
            return;
        }
        PlayerGenieAttachment.Stage stage =
                ((IRenderStateExtension) state).getRenderData(GenieRenderStateKeys.GENIE_STAGE);
        if (stage == null || !hasTail(stage)) {
            return;
        }

        poseStack.pushPose();
        /*
         * Ноги убираются на время отрисовки этого слоя, а не навсегда: у джиннии
         * вместо ног хвост, и оставленные ноги торчали бы сквозь него. Прятать
         * их в самой модели нельзя — она общая для всех игроков на сервере, и
         * следующий кадр обычного игрока пришёл бы без ног.
         */
        PlayerModel model = getParentModel();
        boolean leftWasVisible = model.leftLeg.visible;
        boolean rightWasVisible = model.rightLeg.visible;
        model.leftLeg.visible = false;
        model.rightLeg.visible = false;

        tail.animate(state.ageInTicks, state.walkAnimationSpeed);
        tail.submit(poseStack, collector, lightCoords,
                net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY);

        model.leftLeg.visible = leftWasVisible;
        model.rightLeg.visible = rightWasVisible;
        poseStack.popPose();
    }

    /** Хвост есть на всех стадиях начиная с формирования хвоста. */
    private static boolean hasTail(PlayerGenieAttachment.Stage stage) {
        return switch (stage) {
            case HUMAN, BODY_REWRITE -> false;
            case TAIL_FORMATION, AVATAR_CUSTOMIZATION, INVULNERABILITY_TEST, FULL_GENIE -> true;
        };
    }
}
