package dev.romankrukovsky.kubanhorizons.client.widget;

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
                centerX + OUTER_RADIUS, centerY + OUTER_RADIUS, 0xE0181027);
        graphics.fill(centerX - INNER_RADIUS, centerY - INNER_RADIUS,
                centerX + INNER_RADIUS, centerY + INNER_RADIUS, 0xFF2A1B3D);

        int hovered = selectedIndex(mouseX, mouseY);
        for (int index = 0; index < MODES.length; index++) {
            double angle = -Math.PI / 2.0D + index * (Math.PI * 2.0D / MODES.length);
            int labelX = (int) Math.round(centerX + Math.cos(angle) * 50.0D);
            int labelY = (int) Math.round(centerY + Math.sin(angle) * 50.0D) - 4;
            int halfWidth = 30;
            graphics.fill(labelX - halfWidth, labelY - 6, labelX + halfWidth, labelY + 14,
                    index == hovered ? 0xFFE2B84D : 0xFF59406F);
            graphics.textRenderer().accept(TextAlignment.CENTER, labelX, labelY,
                    Component.translatable(MODES[index].translationKey()));
        }
    }

    public @Nullable GenieBehaviorMode select(double mouseX, double mouseY) {
        int index = selectedIndex(mouseX, mouseY);
        return index < 0 ? null : MODES[index];
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
}
