package dev.romankrukovsky.kubanhorizons.client.widget;

import dev.romankrukovsky.kubanhorizons.client.util.KHColors;
import dev.romankrukovsky.kubanhorizons.genie.GenieBehaviorMode;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.TextAlignment;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

/** Радиальное меню четырёх реально существующих приказов джиннии. */
public final class RadialGenieMenu {
    private static final int OUTER_RADIUS = 76;
    private static final int INNER_RADIUS = 20;
    private static final GenieBehaviorMode[] MODES = GenieBehaviorMode.values();

    private final int centerX;
    private final int centerY;
    private boolean open;
    private int keyboardSelected = -1;

    public RadialGenieMenu(int centerX, int centerY) {
        this.centerX = centerX;
        this.centerY = centerY;
    }

    public void open() {
        open = true;
    }

    public void close() {
        open = false;
    }

    public boolean isOpen() {
        return open;
    }

    public void render(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        if (!open) {
            return;
        }
        graphics.fill(centerX - OUTER_RADIUS, centerY - OUTER_RADIUS,
                centerX + OUTER_RADIUS, centerY + OUTER_RADIUS, KHColors.MAGIC_DARK);
        graphics.fill(centerX - INNER_RADIUS, centerY - INNER_RADIUS,
                centerX + INNER_RADIUS, centerY + INNER_RADIUS, KHColors.MAGIC_ACCENT);

        int hovered = selectedIndex(mouseX, mouseY);
        int active = hovered >= 0 ? hovered : keyboardSelected;
        for (int index = 0; index < MODES.length; index++) {
            double angle = -Math.PI / 2.0D + index * (Math.PI * 2.0D / MODES.length);
            int labelX = (int) Math.round(centerX + Math.cos(angle) * 50.0D);
            int labelY = (int) Math.round(centerY + Math.sin(angle) * 50.0D) - 4;
            int halfWidth = 30;
            boolean isActive = index == active;
            graphics.fill(labelX - halfWidth, labelY - 6, labelX + halfWidth, labelY + 14,
                    isActive ? KHColors.MAGIC_GOLD : KHColors.MAGIC_PURPLE);
            graphics.textRenderer().accept(TextAlignment.CENTER, labelX, labelY,
                    Component.translatable(MODES[index].translationKey()));
            if (isActive) {
                graphics.fill(labelX - halfWidth - 2, labelY - 8, labelX + halfWidth + 2, labelY - 6, KHColors.FOCUS_RING);
                graphics.fill(labelX - halfWidth - 2, labelY + 14, labelX + halfWidth + 2, labelY + 16, KHColors.FOCUS_RING);
                graphics.fill(labelX - halfWidth - 2, labelY - 6, labelX - halfWidth, labelY + 14, KHColors.FOCUS_RING);
                graphics.fill(labelX + halfWidth, labelY - 6, labelX + halfWidth + 2, labelY + 14, KHColors.FOCUS_RING);
            }
        }
    }

    public @Nullable GenieBehaviorMode select(double mouseX, double mouseY) {
        int index = selectedIndex(mouseX, mouseY);
        return index < 0 ? null : MODES[index];
    }

    /** Возвращает режим по направлению клавиш (для клавиатурной навигации). */
    public @Nullable GenieBehaviorMode selectByKey(int key) {
        // 0 = вверх, 1 = вправо, 2 = вниз, 3 = влево
        int idx = switch (key) {
            case 265, 87  -> 0; // ↑ или W
            case 262, 68  -> 1; // → или D
            case 264, 83  -> 2; // ↓ или S
            case 263, 65  -> 3; // ← или A
            default       -> -1;
        };
        if (idx < 0 || idx >= MODES.length) return null;
        keyboardSelected = idx;
        return MODES[idx];
    }

    private int selectedIndex(double mouseX, double mouseY) {
        double dx = mouseX - centerX;
        double dy = mouseY - centerY;
        double distance = Math.sqrt(dx * dx + dy * dy);
        if (distance < INNER_RADIUS || distance > OUTER_RADIUS) {
            return -1;
        }
        double relative = Math.atan2(dy, dx) + Math.PI / 2.0D;
        if (relative < 0.0D) {
            relative += Math.PI * 2.0D;
        }
        double step = Math.PI * 2.0D / MODES.length;
        return Math.floorMod((int) Math.floor((relative + step / 2.0D) / step), MODES.length);
    }

    /** Рисует подсказку над выбранным сектором (вызывается из экрана). */
    public void renderTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        if (!open) return;
        int idx = selectedIndex(mouseX, mouseY);
        if (idx < 0) return;
        Component tip = Component.translatable(MODES[idx].translationKey() + ".desc");
        graphics.textRenderer().accept(TextAlignment.CENTER, centerX, centerY + OUTER_RADIUS + 8, tip);
    }
}
