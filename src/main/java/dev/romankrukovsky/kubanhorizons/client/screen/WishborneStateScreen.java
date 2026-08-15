package dev.romankrukovsky.kubanhorizons.client.screen;

import dev.romankrukovsky.kubanhorizons.client.util.KHColors;
import dev.romankrukovsky.kubanhorizons.genie.WishborneState.Presence;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.TextAlignment;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Клиентский экран состояния Wishborne-сущности: проявление, рассеивание,
 * запечатывание, изгнание и полоса якорения реальности 0..100.
 */
public final class WishborneStateScreen extends Screen {

    private static final int BAR_WIDTH = 200;
    private static final int BAR_HEIGHT = 12;

    private final Presence presence;
    private final int anchoring;

    public WishborneStateScreen(Presence presence, int anchoring) {
        super(Component.translatable("screen.kubanhorizons.genie.wishborne.title"));
        this.presence = presence;
        this.anchoring = clamp(anchoring, 0, 100);
    }

    @Override
    protected void init() {
        super.init();
        int centerX = this.width / 2;
        int centerY = this.height / 2;
        this.addRenderableWidget(Button.builder(
                Component.translatable("screen.kubanhorizons.genie.wishborne.close"),
                btn -> this.onClose()
        ).bounds(centerX - 100, centerY + 70, 200, 20).build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(guiGraphics, mouseX, mouseY, partialTick);
        var text = guiGraphics.textRenderer();
        int centerX = this.width / 2;

        text.accept(TextAlignment.CENTER, centerX, 30, this.title);

        Component stateName = Component.translatable(stateKey(presence));
        text.accept(TextAlignment.CENTER, centerX, 60, stateName);

        text.accept(TextAlignment.CENTER, centerX, 80,
                Component.translatable(stateDescriptionKey(presence)));

        int barX = centerX - BAR_WIDTH / 2;
        int barY = this.height / 2 - 10;
        guiGraphics.fill(barX - 1, barY - 1, barX + BAR_WIDTH + 1, barY + BAR_HEIGHT + 1,
                KHColors.MAGIC_ACCENT);
        guiGraphics.fill(barX, barY, barX + BAR_WIDTH, barY + BAR_HEIGHT, KHColors.MAGIC_DARK);
        int filled = BAR_WIDTH * anchoring / 100;
        guiGraphics.fill(barX, barY, barX + filled, barY + BAR_HEIGHT, anchoringColor(anchoring));

        text.accept(TextAlignment.CENTER, centerX, barY + BAR_HEIGHT + 6,
                Component.translatable("screen.kubanhorizons.genie.wishborne.anchoring", anchoring));
    }

    private static String stateKey(Presence presence) {
        return switch (presence) {
            case MANIFESTED -> "state.kubanhorizons.genie.wishborne.manifested";
            case DISPERSED -> "state.kubanhorizons.genie.wishborne.dispersed";
            case SEALED -> "state.kubanhorizons.genie.wishborne.sealed";
            case BANISHED -> "state.kubanhorizons.genie.wishborne.banished";
        };
    }

    private static String stateDescriptionKey(Presence presence) {
        return switch (presence) {
            case MANIFESTED -> "state.kubanhorizons.genie.wishborne.manifested.desc";
            case DISPERSED -> "state.kubanhorizons.genie.wishborne.dispersed.desc";
            case SEALED -> "state.kubanhorizons.genie.wishborne.sealed.desc";
            case BANISHED -> "state.kubanhorizons.genie.wishborne.banished.desc";
        };
    }

    private static int anchoringColor(int anchoring) {
        if (anchoring >= 100) {
            return KHColors.RISK_HIGH;
        }
        if (anchoring >= 66) {
            return KHColors.RISK_MEDIUM;
        }
        return KHColors.TEXT_SUCCESS;
    }

    private static int clamp(int value, int min, int max) {
        return value < min ? min : (value > max ? max : value);
    }

    @Override
    public boolean isPauseScreen() {
        return true;
    }
}