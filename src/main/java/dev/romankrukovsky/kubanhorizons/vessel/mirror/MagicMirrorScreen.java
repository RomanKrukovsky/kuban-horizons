package dev.romankrukovsky.kubanhorizons.vessel.mirror;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * Magic Mirror Smartphone — интерфейс для владельца Зеркала.
 * Вкладки: Wish Map, Contracts, Messages, History, Settlement, Archive.
 */
public class MagicMirrorScreen extends Screen {
    private static final int WIDTH = 320;
    private static final int HEIGHT = 240;

    private enum Tab { WISH_MAP, CONTRACTS, MESSAGES, HISTORY, SETTLEMENT, ARCHIVE }
    private Tab currentTab = Tab.WISH_MAP;

    public MagicMirrorScreen() {
        super(Component.literal("§bMagic Mirror"));
    }

    @Override
    public void render(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        int x = (width - WIDTH) / 2;
        int y = (height - HEIGHT) / 2;

        // Фон
        gfx.fill(x, y, x + WIDTH, y + HEIGHT, 0xAA000000);
        gfx.fill(x + 2, y + 2, x + WIDTH - 2, y + HEIGHT - 2, 0xFF1a1a2e);

        // Заголовок
        gfx.drawString(font, "§b✧ MAGIC MIRROR ✧", x + 20, y + 12, 0xFFFFFF, false);

        // Вкладки
        renderTabs(gfx, x, y);

        // Содержимое вкладки
        switch (currentTab) {
            case WISH_MAP -> renderWishMap(gfx, x, y);
            case CONTRACTS -> renderContracts(gfx, x, y);
            case MESSAGES -> renderMessages(gfx, x, y);
            case HISTORY -> renderHistory(gfx, x, y);
            case SETTLEMENT -> renderSettlement(gfx, x, y);
            case ARCHIVE -> renderArchive(gfx, x, y);
        }

        super.render(gfx, mouseX, mouseY, partialTick);
    }

    private void renderTabs(GuiGraphics gfx, int x, int y) {
        String[] labels = {"Карта", "Контракты", "Сообщения", "История", "Поселение", "Архив"};
        Tab[] tabs = Tab.values();
        int tabWidth = 48;

        for (int i = 0; i < tabs.length; i++) {
            int tx = x + 10 + i * tabWidth;
            boolean active = tabs[i] == currentTab;
            int color = active ? 0xFF4a90d9 : 0xFF666666;
            gfx.fill(tx, y + 28, tx + tabWidth - 2, y + 42, color);
            gfx.drawString(font, labels[i], tx + 4, y + 32, 0xFFFFFF, false);
        }
    }

    private void renderWishMap(GuiGraphics gfx, int x, int y) {
        gfx.drawString(font, "§eКАРТА ЖЕЛАНИЙ", x + 20, y + 55, 0xFFFFFF, false);
        gfx.drawString(font, "Активные желания в мире:", x + 20, y + 70, 0xAAAAAA, false);
        // TODO: Загрузить из сервера список желаний
        gfx.drawString(font, "• Дворец у реки — в процессе", x + 25, y + 85, 0x88FF88, false);
        gfx.drawString(font, "• Восстановление деревни — ожидает", x + 25, y + 97, 0xFFFF88, false);
    }

    private void renderContracts(GuiGraphics gfx, int x, int y) {
        gfx.drawString(font, "§eКОНТРАКТЫ", x + 20, y + 55, 0xFFFFFF, false);
        gfx.drawString(font, "Юридически обязывающие соглашения:", x + 20, y + 70, 0xAAAAAA, false);
    }

    private void renderMessages(GuiGraphics gfx, int x, int y) {
        gfx.drawString(font, "§eСООБЩЕНИЯ", x + 20, y + 55, 0xFFFFFF, false);
        gfx.drawString(font, "Входящие сообщения от других игроков:", x + 20, y + 70, 0xAAAAAA, false);
    }

    private void renderHistory(GuiGraphics gfx, int x, int y) {
        gfx.drawString(font, "§eИСТОРИЯ ЖЕЛАНИЙ", x + 20, y + 55, 0xFFFFFF, false);
        gfx.drawString(font, "#001 — Дворец желаний — ИСПОЛНЕНО", x + 20, y + 70, 0x88FF88, false);
        gfx.drawString(font, "#002 — Исцеление — ИСПОЛНЕНО", x + 20, y + 82, 0x88FF88, false);
    }

    private void renderSettlement(GuiGraphics gfx, int x, int y) {
        gfx.drawString(font, "§eСТАТУС ПОСЕЛЕНИЯ", x + 20, y + 55, 0xFFFFFF, false);
        gfx.drawString(font, "Ваше поселение процветает.", x + 20, y + 70, 0x88FF88, false);
    }

    private void renderArchive(GuiGraphics gfx, int x, int y) {
        gfx.drawString(font, "§eАРХИВ ЖЕЛАНИЙ", x + 20, y + 55, 0xFFFFFF, false);
        gfx.drawString(font, "Завершённые и отменённые желания:", x + 20, y + 70, 0xAAAAAA, false);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
