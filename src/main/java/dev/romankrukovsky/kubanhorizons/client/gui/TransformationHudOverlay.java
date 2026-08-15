package dev.romankrukovsky.kubanhorizons.client.gui;

import dev.romankrukovsky.kubanhorizons.KubanHorizons;
import dev.romankrukovsky.kubanhorizons.client.screen.TransformationClientState;
import dev.romankrukovsky.kubanhorizons.client.util.KHColors;
import dev.romankrukovsky.kubanhorizons.registry.KHAttachments;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

/**
 * Постоянный индикатор активной трансформации игрока в джиннию.
 *
 * <p>Рисуется в правом нижнем углу, пока у локального игрока есть
 * активная трансформация. Стадия и прогресс берутся из
 * {@link TransformationClientState} — его обновляет
 * {@link dev.romankrukovsky.kubanhorizons.network.packet.s2c.S2CTransformationSync}.
 * Дополнительно проверяем синхронизированный attachment: если сервер ещё не
 * прислал пакет, но attachment уже говорит, что игрок — джинния, индикатор
 * всё равно появится (стадия из attachment, прогресс 0).</p>
 *
 * <p>Оверлей рисуется в {@code Post} фазе HUD: в {@code Pre} его перекрыл бы
 * сам HUD. Полоса рисуется через {@code fill} — то же, что и в экранах.</p>
 */
@EventBusSubscriber(modid = KubanHorizons.MOD_ID, value = Dist.CLIENT)
public final class TransformationHudOverlay {
    private static final int MARGIN = 8;
    private static final int BAR_WIDTH = 90;
    private static final int BAR_HEIGHT = 6;
    private static final int TEXT_COLOR = 0xFFFFFFFF;

    private TransformationHudOverlay() {
    }

    @SubscribeEvent
    static void onRenderGui(RenderGuiEvent.Post event) {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null || client.player == null || client.getDebugOverlay().showDebugScreen()) {
            return;
        }
        if (!TransformationClientState.isActive()
                && !isGenieViaAttachment(client)) {
            return;
        }

        int stageIndex = TransformationClientState.isActive()
                ? TransformationClientState.stageIndex()
                : clientStageFromAttachment(client);
        float progress = TransformationClientState.isActive()
                ? TransformationClientState.progress()
                : 0.0f;

        GuiGraphicsExtractor gfx = event.getGuiGraphics();

        int right = client.getWindow().getGuiScaledWidth() - MARGIN;
        int bottom = client.getWindow().getGuiScaledHeight() - MARGIN;
        int barX = right - BAR_WIDTH;
        int barY = bottom - BAR_HEIGHT - 2;
        int labelY = barY - client.font.lineHeight - 2;

        gfx.text(client.font, Component.translatable(stageKey(stageIndex)).getString(),
                barX, labelY, TEXT_COLOR);

        gfx.fill(barX - 1, barY - 1, barX + BAR_WIDTH + 1, barY + BAR_HEIGHT + 1,
                KHColors.MAGIC_ACCENT);
        gfx.fill(barX, barY, barX + BAR_WIDTH, barY + BAR_HEIGHT, KHColors.MAGIC_DARK);
        int filled = Math.round(BAR_WIDTH * progress / 100.0f);
        gfx.fill(barX, barY, barX + filled, barY + BAR_HEIGHT, KHColors.MAGIC_GOLD);
    }

    private static boolean isGenieViaAttachment(Minecraft client) {
        return client.player.getData(KHAttachments.PLAYER_GENIE_DATA).isGenie();
    }

    private static int clientStageFromAttachment(Minecraft client) {
        return mapServerStageToClient(
                client.player.getData(KHAttachments.PLAYER_GENIE_DATA).getStage().ordinal());
    }

    /** 6 серверных стадий → 4 клиентские: HUMAN, AWAKENING, HALF_GENIE, GENIE. */
    public static int mapServerStageToClient(int serverOrdinal) {
        return switch (serverOrdinal) {
            case 0 -> 0;
            case 1, 2 -> 1;
            case 3, 4 -> 2;
            default -> 3;
        };
    }

    private static String stageKey(int stageIndex) {
        return switch (stageIndex) {
            case 0 -> "stage.kubanhorizons.transformation.human";
            case 1 -> "stage.kubanhorizons.transformation.awakening";
            case 2 -> "stage.kubanhorizons.transformation.half_genie";
            case 3 -> "stage.kubanhorizons.transformation.genie";
            default -> "stage.kubanhorizons.transformation.human";
        };
    }
}