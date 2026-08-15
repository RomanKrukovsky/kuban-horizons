package dev.romankrukovsky.kubanhorizons.client.screen;

import dev.romankrukovsky.kubanhorizons.client.util.KHColors;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.TextAlignment;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Клиентский экран прогресса трансформации игрока в джиннию.
 *
 * <p>Показывает текущую стадию (HUMAN → AWAKENING → HALF_GENIE → GENIE),
 * полосу прогресса 0..100% и описание стадии. Серверный контроллер уже
 * продвигает стадии по таймерам; экран только рисует то, что пришло в
 * {@link dev.romankrukovsky.kubanhorizons.network.packet.s2c.S2CTransformationSync}.</p>
 */
public final class PlayerTransformationScreen extends Screen {

    private static final int BAR_WIDTH = 200;
    private static final int BAR_HEIGHT = 12;

    private final int stageIndex;
    private final float progress;

    public PlayerTransformationScreen(int stageIndex, float progress) {
        super(Component.translatable("screen.kubanhorizons.transformation.title"));
        this.stageIndex = Math.max(0, Math.min(3, stageIndex));
        this.progress = Math.max(0.0f, Math.min(100.0f, progress));
    }

    @Override
    protected void init() {
        super.init();
        int centerX = this.width / 2;
        int centerY = this.height / 2;
        this.addRenderableWidget(Button.builder(
                Component.translatable("screen.kubanhorizons.transformation.close"),
                btn -> this.onClose()
        ).bounds(centerX - 100, centerY + 70, 200, 20).build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(guiGraphics, mouseX, mouseY, partialTick);
        var text = guiGraphics.textRenderer();
        int centerX = this.width / 2;

        text.accept(TextAlignment.CENTER, centerX, 30, this.title);

        text.accept(TextAlignment.CENTER, centerX, 60,
                Component.translatable(stageKey(stageIndex)));

        text.accept(TextAlignment.CENTER, centerX, 80,
                Component.translatable(stageDescriptionKey(stageIndex)));

        int barX = centerX - BAR_WIDTH / 2;
        int barY = this.height / 2 - 10;
        guiGraphics.fill(barX - 1, barY - 1, barX + BAR_WIDTH + 1, barY + BAR_HEIGHT + 1,
                KHColors.MAGIC_ACCENT);
        guiGraphics.fill(barX, barY, barX + BAR_WIDTH, barY + BAR_HEIGHT, KHColors.MAGIC_DARK);
        int filled = Math.round(BAR_WIDTH * progress / 100.0f);
        guiGraphics.fill(barX, barY, barX + filled, barY + BAR_HEIGHT, progressColor(progress));

        text.accept(TextAlignment.CENTER, centerX, barY + BAR_HEIGHT + 6,
                Component.translatable("screen.kubanhorizons.transformation.progress",
                        Math.round(progress)));
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

    private static String stageDescriptionKey(int stageIndex) {
        return stageKey(stageIndex) + ".desc";
    }

    private static int progressColor(float progress) {
        if (progress >= 100.0f) {
            return KHColors.MAGIC_GOLD;
        }
        if (progress >= 66.0f) {
            return KHColors.RISK_MEDIUM;
        }
        return KHColors.TEXT_SUCCESS;
    }

    @Override
    public boolean isPauseScreen() {
        return true;
    }
}