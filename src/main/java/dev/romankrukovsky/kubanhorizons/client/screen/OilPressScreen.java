package dev.romankrukovsky.kubanhorizons.client.screen;

import dev.romankrukovsky.kubanhorizons.menu.OilPressMenu;
import dev.romankrukovsky.kubanhorizons.util.KHIds;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

/**
 * Экран маслопресса: фон в стилистике ванильной печи, стрелка прогресса
 * между слотом сырья и слотом результата.
 */
public class OilPressScreen extends AbstractContainerScreen<OilPressMenu> {
    private static final Identifier TEXTURE = KHIds.of("textures/gui/container/oil_press.png");

    private static final int ARROW_X = 76;
    private static final int ARROW_Y = 35;
    private static final int ARROW_WIDTH = 22;
    private static final int ARROW_HEIGHT = 16;
    /** Позиция спрайта заполненной стрелки на листе текстуры. */
    private static final int ARROW_U = 176;
    private static final int ARROW_V = 0;

    public OilPressScreen(OilPressMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        int x = this.leftPos;
        int y = this.topPos;
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x, y, 0.0F, 0.0F,
                this.imageWidth, this.imageHeight, 256, 256);

        int progressWidth = Math.round(this.menu.progress() * ARROW_WIDTH);
        if (progressWidth > 0) {
            graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE,
                    x + ARROW_X, y + ARROW_Y,
                    (float) ARROW_U, (float) ARROW_V,
                    progressWidth, ARROW_HEIGHT, 256, 256);
        }
    }

}
