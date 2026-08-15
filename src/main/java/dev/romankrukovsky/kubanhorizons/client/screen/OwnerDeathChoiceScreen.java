package dev.romankrukovsky.kubanhorizons.client.screen;

import dev.romankrukovsky.kubanhorizons.client.util.KHColors;
import dev.romankrukovsky.kubanhorizons.genie.vessel.OwnerDeathProtocol.DeathChoice;
import dev.romankrukovsky.kubanhorizons.network.packet.c2s.C2SOwnerDeathChoice;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.TextAlignment;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.UUID;

/**
 * Экран выбора после смерти владельца джиннии.
 * 4 варианта: Воскресить, Сохранить душу, Откатить желание, Освободить джиннию.
 */
public class OwnerDeathChoiceScreen extends Screen {

    private final UUID genieId;

    public OwnerDeathChoiceScreen(UUID genieId) {
        super(Component.translatable("screen.kubanhorizons.genie.owner_death.title"));
        this.genieId = genieId;
    }

    @Override
    protected void init() {
        super.init();
        int centerX = this.width / 2;
        int centerY = this.height / 2;

        // 4 варианта
        this.addRenderableWidget(Button.builder(
                Component.translatable("choice.kubanhorizons.genie.owner_death.resurrect"),
                btn -> sendChoice(DeathChoice.RESURRECT_OWNER)
        ).bounds(centerX - 200, centerY - 40, 180, 20).build());

        this.addRenderableWidget(Button.builder(
                Component.translatable("choice.kubanhorizons.genie.owner_death.save_soul"),
                btn -> sendChoice(DeathChoice.SAVE_SOUL)
        ).bounds(centerX + 20, centerY - 40, 180, 20).build());

        this.addRenderableWidget(Button.builder(
                Component.translatable("choice.kubanhorizons.genie.owner_death.rollback"),
                btn -> sendChoice(DeathChoice.ROLLBACK_LAST_WISH)
        ).bounds(centerX - 200, centerY + 10, 180, 20).build());

        this.addRenderableWidget(Button.builder(
                Component.translatable("choice.kubanhorizons.genie.owner_death.respawn_free"),
                btn -> sendChoice(DeathChoice.RESPAWN_FREE)
        ).bounds(centerX + 20, centerY + 10, 180, 20).build());
    }

    private void sendChoice(DeathChoice choice) {
        C2SOwnerDeathChoice.send(choice);
        this.onClose();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(guiGraphics, mouseX, mouseY, partialTick);
        var text = guiGraphics.textRenderer();
        text.accept(TextAlignment.CENTER, this.width / 2, 40, this.title);
        text.accept(TextAlignment.CENTER, this.width / 2, 60,
                Component.translatable("screen.kubanhorizons.genie.owner_death.subtitle"));

        int centerX = this.width / 2;
        int centerY = this.height / 2;

        // Consequence descriptions below each button
        text.accept(TextAlignment.LEFT, centerX - 200, centerY - 15,
                Component.translatable("choice.kubanhorizons.genie.owner_death.resurrect.desc"));
        text.accept(TextAlignment.LEFT, centerX + 20, centerY - 15,
                Component.translatable("choice.kubanhorizons.genie.owner_death.save_soul.desc"));
        text.accept(TextAlignment.LEFT, centerX - 200, centerY + 35,
                Component.translatable("choice.kubanhorizons.genie.owner_death.rollback.desc"));
        text.accept(TextAlignment.LEFT, centerX + 20, centerY + 35,
                Component.translatable("choice.kubanhorizons.genie.owner_death.respawn_free.desc"));

        // Subtle distinct borders for each choice button using KHColors
        drawButtonBorder(guiGraphics, centerX - 200, centerY - 40, 180, 20, KHColors.DEATH_RESURRECT); // green
        drawButtonBorder(guiGraphics, centerX + 20, centerY - 40, 180, 20, KHColors.DEATH_SAVE_SOUL);   // violet
        drawButtonBorder(guiGraphics, centerX - 200, centerY + 10, 180, 20, KHColors.DEATH_ROLLBACK);   // amber
        drawButtonBorder(guiGraphics, centerX + 20, centerY + 10, 180, 20, KHColors.DEATH_RESPAWN);     // teal
    }

    private void drawButtonBorder(GuiGraphicsExtractor g, int x, int y, int w, int h, int color) {
        // 1px border using thin fill rects (top, bottom, left, right)
        g.fill(x, y, x + w, y + 1, color);           // top
        g.fill(x, y + h - 1, x + w, y + h, color);   // bottom
        g.fill(x, y, x + 1, y + h, color);           // left
        g.fill(x + w - 1, y, x + w, y + h, color);   // right
    }

    @Override
    public boolean isPauseScreen() {
        return true;
    }
}