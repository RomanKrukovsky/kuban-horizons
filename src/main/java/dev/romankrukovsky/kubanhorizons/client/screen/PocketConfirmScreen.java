package dev.romankrukovsky.kubanhorizons.client.screen;

import dev.romankrukovsky.kubanhorizons.client.util.KHColors;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.TextAlignment;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Экран подтверждения для 3-стадийного потока карманного измерения:
 * SNAPSHOT → PREVIEW → CONFIRM/ROLLBACK
 */
public class PocketConfirmScreen extends Screen {

    private final int changedBlocks;
    private final int durationTicks;
    private final String risk;

    public PocketConfirmScreen(int changedBlocks, int durationTicks, String risk) {
        super(Component.translatable("screen.kubanhorizons.pocket.title"));
        this.changedBlocks = changedBlocks;
        this.durationTicks = durationTicks;
        this.risk = risk;
    }

    @Override
    protected void init() {
        super.init();

        int centerX = this.width / 2;
        int centerY = this.height / 2;

        // Кнопка подтверждения
        this.addRenderableWidget(Button.builder(
                Component.translatable("screen.kubanhorizons.pocket.confirm"),
                btn -> confirmChanges()
        )
        .bounds(centerX - 100, centerY + 40, 95, 20)
        .build());

        // Кнопка отката
        this.addRenderableWidget(Button.builder(
                Component.translatable("screen.kubanhorizons.pocket.cancel"),
                btn -> rollbackChanges()
        )
        .bounds(centerX + 5, centerY + 40, 95, 20)
        .build());
    }

    private void confirmChanges() {
        // Отправляем подтверждение на сервер
        dev.romankrukovsky.kubanhorizons.network.packet.c2s.C2SPocketConfirm.send();
        this.onClose();
    }

    private void rollbackChanges() {
        // Отправляем откат на сервер
        dev.romankrukovsky.kubanhorizons.network.packet.c2s.C2SPocketRollback.send();
        this.onClose();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY,
                                   float partialTick) {
        super.extractRenderState(guiGraphics, mouseX, mouseY, partialTick);

        int centerX = this.width / 2;
        int centerY = this.height / 2;

        var text = guiGraphics.textRenderer();
        text.accept(TextAlignment.CENTER, centerX, centerY - 60, this.title);

        // Показываем превью изменений
        text.accept(TextAlignment.CENTER, centerX, centerY - 30,
                Component.translatable("screen.kubanhorizons.pocket.blocks", changedBlocks));
        text.accept(TextAlignment.CENTER, centerX, centerY - 15,
                Component.translatable("screen.kubanhorizons.pocket.duration", durationTicks / 20));
        Component riskText = Component.translatable("screen.kubanhorizons.pocket.risk", risk);
        int riskColor = getRiskColor();
        Font font = this.font;
        int textWidth = font.width(riskText);
        int textX = centerX - textWidth / 2;
        int textY = centerY;

        // Draw small colored square indicator
        int squareSize = 6;
        int squarePadding = 4;
        int squareX = textX - squareSize - squarePadding;
        int squareY = textY + (font.lineHeight - squareSize) / 2;
        guiGraphics.fill(squareX, squareY, squareX + squareSize, squareY + squareSize, riskColor);

        // Draw risk text in color via text renderer
        text.accept(TextAlignment.LEFT, textX, textY, Component.literal("§" + Integer.toHexString(riskColor & 0xFFFFFF).substring(0, 1) + riskText.getString()));

        text.accept(TextAlignment.CENTER, centerX, centerY + 15,
                Component.literal(getRiskDescription()));
    }

    private String getRiskDescription() {
        return switch (risk == null ? "" : risk.toLowerCase()) {
            case "low" -> "Low risk: minimal block changes, safe to confirm";
            case "medium" -> "Medium risk: moderate changes, review before confirming";
            case "high" -> "High risk: significant changes, proceed with caution";
            default -> "";
        };
    }

    private int getRiskColor() {
        if (risk == null) return 0xFFFFFFFF;
        return switch (risk.toUpperCase()) {
            case "LOW" -> KHColors.RISK_LOW;
            case "MEDIUM" -> KHColors.RISK_MEDIUM;
            case "HIGH" -> KHColors.RISK_HIGH;
            default -> 0xFFFFFFFF;
        };
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
