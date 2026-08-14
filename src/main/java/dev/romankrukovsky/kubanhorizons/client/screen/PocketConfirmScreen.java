package dev.romankrukovsky.kubanhorizons.client.screen;

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
        ).bounds(centerX - 100, centerY + 40, 95, 20).build());

        // Кнопка отката
        this.addRenderableWidget(Button.builder(
                Component.translatable("screen.kubanhorizons.pocket.cancel"),
                btn -> rollbackChanges()
        ).bounds(centerX + 5, centerY + 40, 95, 20).build());
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
        text.accept(TextAlignment.CENTER, centerX, centerY,
                Component.translatable("screen.kubanhorizons.pocket.risk", risk));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
