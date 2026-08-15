package dev.romankrukovsky.kubanhorizons.client.screen;

import dev.romankrukovsky.kubanhorizons.entity.KubanGenie;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.TextAlignment;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

import java.util.List;

/**
 * Радиальное меню «Изменить реальность» — 10 действий для владельцев сосудов.
 *
 * <p>Прототип: находит ближайшую джиннию, рисует список действий и отправляет
 * сообщение игроку при выборе. Полный визуал и серверные пакеты — задача
 * клиентского UX-срез (см. GENIE_VISION.md §Радиальное меню).</p>
 */
public class RealityMenuScreen extends Screen {

    private static final List<String> ACTIONS = List.of(
            "Создать", "Изменить", "Переместить", "Уничтожить",
            "Восстановить", "Оживить", "Очаровать", "Трансформировать",
            "Остановить", "Исполнить желание"
    );

    private int selectedIndex = -1;
    private KubanGenie nearbyGenie = null;

    public RealityMenuScreen() {
        super(Component.literal("§5ИЗМЕНИТЬ РЕАЛЬНОСТЬ"));
        var mc = Minecraft.getInstance();
        if (mc.player != null && mc.level != null) {
            var genies = mc.level.getEntitiesOfClass(
                    dev.romankrukovsky.kubanhorizons.entity.KubanGenie.class,
                    mc.player.getBoundingBox().inflate(12)
            );
            if (genies.isEmpty()) {
                mc.setScreenAndShow(null);
                mc.player.sendSystemMessage(Component.literal("§cНет Джиннии поблизости..."));
                return;
            }
            nearbyGenie = genies.get(0);
            var origin = nearbyGenie.position().add(0, 1.8, 0);
            for (int i = 0; i < 45; i++) {
                mc.level.addParticle(ParticleTypes.END_ROD,
                        origin.x + (Math.random() - 0.5) * 1.2,
                        origin.y + (Math.random() - 0.5) * 1.2,
                        origin.z + (Math.random() - 0.5) * 1.2,
                        (Math.random() - 0.5) * 0.2, 0.08, (Math.random() - 0.5) * 0.2);
            }
            mc.level.playSound(mc.player, origin.x, origin.y, origin.z,
                    SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 0.7f, 1.35f);
        }
    }

    @Override
    protected void init() {
        super.init();
        int cx = this.width / 2;
        int startY = this.height / 2 - ACTIONS.size() * 11 / 2;
        for (int i = 0; i < ACTIONS.size(); i++) {
            final int idx = i;
            int y = startY + i * 12;
            this.addRenderableWidget(net.minecraft.client.gui.components.Button.builder(
                            Component.literal((i + 1) + ". " + ACTIONS.get(i)),
                            btn -> activateAction(idx))
                    .bounds(cx - 100, y, 200, 12)
                    .build());
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor gfx, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(gfx, mouseX, mouseY, partialTick);
        if (nearbyGenie != null && minecraft != null && minecraft.player != null) {
            if (minecraft.player.distanceToSqr(nearbyGenie) > 20 * 20) {
                this.onClose();
                return;
            }
        }
        gfx.fill(0, 0, this.width, this.height, 0xDD000000);
        var text = gfx.textRenderer();
        int cx = this.width / 2;
        text.accept(TextAlignment.CENTER, cx, 30, this.title);
        if (nearbyGenie != null) {
            text.accept(TextAlignment.CENTER, cx, 45,
                    Component.literal("§7" + nearbyGenie.getName().getString()));
        }
        text.accept(TextAlignment.CENTER, cx, this.height - 20,
                Component.literal("§7ESC — закрыть"));
    }

    private void activateAction(int index) {
        if (minecraft == null || minecraft.player == null) return;
        minecraft.player.sendSystemMessage(Component.literal(
                "§5[Джинния] §f" + ACTIONS.get(index) + "... (прототип)"));
        this.onClose();
    }

    @Override
    public boolean keyPressed(net.minecraft.client.input.KeyEvent event) {
        int keyCode = event.key();
        if (keyCode == 256) {
            this.onClose();
            return true;
        }
        int actionIndex = -1;
        if (keyCode >= 49 && keyCode <= 57) {
            actionIndex = keyCode - 49;
        } else if (keyCode == 48) {
            actionIndex = 9;
        }
        if (actionIndex != -1 && actionIndex < ACTIONS.size()) {
            activateAction(actionIndex);
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}