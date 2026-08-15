package dev.romankrukovsky.kubanhorizons.vessel.reality;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.TextAlignment;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

/**
 * Радиальное меню «Изменить реальность» — 10 действий для владельцев сосудов.
 *
 * <p>Прототип: вертикальный список кнопок по действиям {@link RealityAction}.
 * Полный круговой визуал и серверные пакеты — задача клиентского UX-среза.</p>
 */
public class RadialRealityMenu extends Screen {

    public enum RealityAction {
        CREATE("Создать"),
        MODIFY("Изменить"),
        MOVE("Переместить"),
        DESTROY("Разрушить"),
        RESTORE("Восстановить"),
        ANIMATE("Оживить"),
        ENCHANT("Зачаровать"),
        TRANSFORM("Преобразовать"),
        STOP("Остановить"),
        FULFILL_WISH("Исполнить желание");

        public final String label;

        RealityAction(String label) {
            this.label = label;
        }
    }

    private final Player player;

    public RadialRealityMenu(Player player) {
        super(Component.literal("§6Изменить реальность"));
        this.player = player;
    }

    @Override
    protected void init() {
        super.init();
        int cx = this.width / 2;
        int startY = this.height / 2 - RealityAction.values().length * 11 / 2;
        RealityAction[] actions = RealityAction.values();
        for (int i = 0; i < actions.length; i++) {
            final RealityAction action = actions[i];
            this.addRenderableWidget(Button.builder(
                            Component.literal(action.label),
                            btn -> activate(action))
                    .bounds(cx - 100, startY + i * 12, 200, 12)
                    .build());
        }
    }

    private void activate(RealityAction action) {
        player.sendSystemMessage(Component.literal(
                "§6Действие: " + action.label + " — отправлено на сервер (прототип)."));
        onClose();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor gfx, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(gfx, mouseX, mouseY, partialTick);
        gfx.fill(0, 0, this.width, this.height, 0x88000000);
        var text = gfx.textRenderer();
        text.accept(TextAlignment.CENTER, this.width / 2, 20, this.title);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}