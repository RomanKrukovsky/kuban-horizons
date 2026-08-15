package dev.romankrukovsky.kubanhorizons.vessel.mirror;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.TextAlignment;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Магическое зеркало-смартфон — интерфейс владельца зеркала-сосуда.
 *
 * <p>Прототип: вкладки Карта желаний, Контракты, Сообщения, История,
 * Поселение, Архив. Полное содержимое и серверная синхронизация — задача
 * клиентского UX-среза (см. GENIE_VISION.md §Магическое зеркало).</p>
 */
public class MagicMirrorScreen extends Screen {

    private enum Tab { WISH_MAP, CONTRACTS, MESSAGES, HISTORY, SETTLEMENT, ARCHIVE }

    private Tab currentTab = Tab.WISH_MAP;

    public MagicMirrorScreen() {
        super(Component.literal("§bМагическое зеркало"));
    }

    @Override
    protected void init() {
        super.init();
        int cx = this.width / 2;
        int tabY = 40;
        Tab[] tabs = Tab.values();
        int tabWidth = 100;
        int startX = cx - tabs.length * tabWidth / 2;
        for (int i = 0; i < tabs.length; i++) {
            final Tab tab = tabs[i];
            this.addRenderableWidget(Button.builder(
                            Component.literal(tabLabel(tab)),
                            btn -> currentTab = tab)
                    .bounds(startX + i * tabWidth, tabY, tabWidth - 2, 14)
                    .build());
        }
    }

    private String tabLabel(Tab tab) {
        return switch (tab) {
            case WISH_MAP -> "Карта";
            case CONTRACTS -> "Контракты";
            case MESSAGES -> "Сообщения";
            case HISTORY -> "История";
            case SETTLEMENT -> "Поселение";
            case ARCHIVE -> "Архив";
        };
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor gfx, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(gfx, mouseX, mouseY, partialTick);
        int cx = this.width / 2;
        int cy = this.height / 2;
        gfx.fill(cx - 160, cy - 120, cx + 160, cy + 120, 0xFF1a1a2e);
        var text = gfx.textRenderer();
        text.accept(TextAlignment.CENTER, cx, 25, this.title);
        text.accept(TextAlignment.CENTER, cx, cy - 20,
                Component.literal("§e" + tabLabel(currentTab).toUpperCase()));
        text.accept(TextAlignment.CENTER, cx, cy,
                Component.literal("§7Содержимое вкладки — прототип."));
        text.accept(TextAlignment.CENTER, cx, cy + 20,
                Component.literal("§7Полная реализация в клиентском UX-срезе."));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}