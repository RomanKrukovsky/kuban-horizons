package dev.romankrukovsky.kubanhorizons.vessel.reality;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

/**
 * Radial Menu "Change Reality" — 10 действий для владельцев сосудов.
 * Create, Modify, Move, Destroy, Restore, Animate, Enchant, Transform, Stop, Fulfill Wish.
 */
public class RadialRealityMenu extends Screen {
    private static final int RADIUS = 80;
    private static final int CENTER_SIZE = 40;

    public enum RealityAction {
        CREATE("Создать", 0x4CAF50),
        MODIFY("Изменить", 0x2196F3),
        MOVE("Переместить", 0x9C27B0),
        DESTROY("Разрушить", 0xF44336),
        RESTORE("Восстановить", 0xFF9800),
        ANIMATE("Оживить", 0xE91E63),
        ENCHANT("Зачаровать", 0x673AB7),
        TRANSFORM("Преобразовать", 0x00BCD4),
        STOP("Остановить", 0x607D8B),
        FULFILL_WISH("Исполнить желание", 0xFFD700);

        public final String label;
        public final int color;

        RealityAction(String label, int color) {
            this.label = label;
            this.color = color;
        }
    }

    private RealityAction hoveredAction = null;
    private final Player player;

    public RadialRealityMenu(Player player) {
        super(Component.literal("§6Change Reality"));
        this.player = player;
    }

    @Override
    public void render(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        int cx = width / 2;
        int cy = height / 2;

        // Полупрозрачный фон
        gfx.fill(0, 0, width, height, 0x88000000);

        // Центральный круг
        gfx.fill(cx - CENTER_SIZE/2, cy - CENTER_SIZE/2,
                 cx + CENTER_SIZE/2, cy + CENTER_SIZE/2, 0xAA1a1a2e);
        gfx.drawString(font, "§6РЕАЛЬНОСТЬ", cx - 35, cy - 4, 0xFFFFFF, false);

        // Радиальные кнопки
        RealityAction[] actions = RealityAction.values();
        double angleStep = 2 * Math.PI / actions.length;

        hoveredAction = null;

        for (int i = 0; i < actions.length; i++) {
            double angle = -Math.PI/2 + i * angleStep;
            int bx = (int)(cx + Math.cos(angle) * RADIUS);
            int by = (int)(cy + Math.sin(angle) * RADIUS);

            // Проверка наведения
            double dist = Math.sqrt(Math.pow(mouseX - bx, 2) + Math.pow(mouseY - by, 2));
            boolean hovered = dist < 22;

            if (hovered) {
                hoveredAction = actions[i];
            }

            int color = hovered ? 0xFFFFFFFF : actions[i].color;
            int bgColor = hovered ? 0xAAFFFFFF : 0xAA000000;

            // Круг кнопки
            gfx.fill(bx - 20, by - 20, bx + 20, by + 20, bgColor);
            gfx.drawString(font, actions[i].label.substring(0, Math.min(3, actions[i].label.length())),
                          bx - 12, by - 4, color, false);
        }

        // Подсказка
        if (hoveredAction != null) {
            gfx.drawString(font, "§e" + hoveredAction.label, cx - 40, cy + RADIUS + 30, 0xFFFFFF, false);
            gfx.drawString(font, "§7Нажмите для активации", cx - 55, cy + RADIUS + 42, 0xAAAAAA, false);
        }

        super.render(gfx, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (hoveredAction != null && button == 0) {
            // Отправляем действие на сервер
            // TODO: Network packet: KHNetwork.sendToServer(new RealityActionPacket(hoveredAction))
            player.sendSystemMessage(Component.literal(
                "§6Действие: " + hoveredAction.label + " — отправлено на сервер."
            ));
            onClose();
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
