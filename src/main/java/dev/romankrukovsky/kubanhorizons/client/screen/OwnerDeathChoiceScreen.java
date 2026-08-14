package dev.romankrukovsky.kubanhorizons.client.screen;

import dev.romankrukovsky.kubanhorizons.genie.vessel.OwnerDeathProtocol.DeathChoice;
import net.minecraft.client.gui.GuiGraphics;
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
        dev.romankrukovsky.kubanhorizons.network.packet.c2s.C2SOwnerDeathChoice.send(choice);
        this.onClose();
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 40, 0xFFFFFF);
        guiGraphics.drawCenteredString(this.font,
                Component.translatable("screen.kubanhorizons.genie.owner_death.subtitle"),
                this.width / 2, 60, 0xAAAAAA);
    }

    @Override
    public boolean isPauseScreen() {
        return true;
    }
}
```