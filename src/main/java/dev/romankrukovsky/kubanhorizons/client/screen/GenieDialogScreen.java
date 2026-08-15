package dev.romankrukovsky.kubanhorizons.client.screen;

import dev.romankrukovsky.kubanhorizons.client.util.KHColors;
import dev.romankrukovsky.kubanhorizons.client.widget.RadialGenieMenu;
import dev.romankrukovsky.kubanhorizons.genie.GenieBehaviorMode;
import dev.romankrukovsky.kubanhorizons.network.packet.c2s.C2SGenieCommand;
import dev.romankrukovsky.kubanhorizons.network.packet.c2s.C2SPolicyDecision;
import dev.romankrukovsky.kubanhorizons.network.packet.c2s.C2SWishRequest;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.TextAlignment;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/** Рабочий экран разговора: свободный текст и быстрые приказы спутнице. */
public final class GenieDialogScreen extends Screen {
    private final int genieId;
    private final Component genieName;
    private GenieBehaviorMode currentMode;
    private EditBox inputBox;
    private RadialGenieMenu radialMenu;
    private Button confirmButton;
    private Button cancelButton;
    private Component response = Component.translatable("screen.kubanhorizons.genie.greeting");
    private boolean waiting;

    public GenieDialogScreen(int genieId, Component genieName, GenieBehaviorMode currentMode) {
        super(Component.translatable("screen.kubanhorizons.genie.title", genieName));
        this.genieId = genieId;
        this.genieName = genieName;
        this.currentMode = currentMode;
    }

    @Override
    protected void init() {
        super.init();
        int centerX = width / 2;
        int centerY = height / 2;
        inputBox = new EditBox(font, centerX - 145, centerY + 48, 220, 20,
                Component.translatable("screen.kubanhorizons.genie.input"));
        inputBox.setMaxLength(256);
        addRenderableWidget(inputBox);
        addRenderableWidget(Button.builder(Component.translatable("screen.kubanhorizons.genie.send"),
                button -> submit()).bounds(centerX + 80, centerY + 48, 65, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("screen.kubanhorizons.genie.orders"),
                button -> radialMenu.open()).bounds(centerX - 70, centerY + 76, 140, 20).build());
        confirmButton = addRenderableWidget(Button.builder(
                Component.translatable("screen.kubanhorizons.genie.confirm"),
                button -> decidePolicy(true)).bounds(centerX - 145, centerY + 40, 140, 20).build());
        cancelButton = addRenderableWidget(Button.builder(
                Component.translatable("screen.kubanhorizons.genie.cancel"),
                button -> decidePolicy(false)).bounds(centerX + 5, centerY + 40, 140, 20).build());
        setPolicyButtonsVisible(false);
        radialMenu = new RadialGenieMenu(centerX, centerY - 18);
        setInitialFocus(inputBox);
    }

    private void submit() {
        String text = inputBox.getValue().trim();
        if (text.isEmpty() || waiting) {
            return;
        }
        waiting = true;
        response = Component.translatable("screen.kubanhorizons.genie.waiting")
                .withStyle(ChatFormatting.GRAY);
        // TODO: add subtle spinner / dim effect on inputBox when waiting == true
        C2SWishRequest.send(genieId, text);
        inputBox.setValue("");
    }

    public void acceptResponse(Component message, int emotionLevel, boolean confirmationRequired) {
        waiting = false;
        response = switch (emotionLevel) {
            case 1 -> message.copy().withStyle(ChatFormatting.AQUA);
            case 2 -> message.copy().withStyle(ChatFormatting.LIGHT_PURPLE);
            default -> message;
        };
        setPolicyButtonsVisible(confirmationRequired);
    }

    private void decidePolicy(boolean confirmed) {
        setPolicyButtonsVisible(false);
        waiting = true;
        response = Component.translatable("screen.kubanhorizons.genie.waiting")
                .withStyle(ChatFormatting.GRAY);
        C2SPolicyDecision.send(genieId, confirmed);
    }

    private void setPolicyButtonsVisible(boolean visible) {
        confirmButton.visible = visible;
        confirmButton.active = visible;
        cancelButton.visible = visible;
        cancelButton.active = visible;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (radialMenu.isOpen()) {
            if (event.key() == 256) { // Escape
                radialMenu.close();
                return true;
            }
            GenieBehaviorMode byKey = radialMenu.selectByKey(event.key());
            if (byKey != null) {
                currentMode = byKey;
                C2SGenieCommand.send(genieId, byKey);
                response = Component.translatable("message.kubanhorizons.genie.ai.mode",
                        Component.translatable(byKey.translationKey()));
                radialMenu.close();
                return true;
            }
        }
        if ((event.key() == 257 || event.key() == 335) && inputBox.isFocused()) {
            submit();
            return true;
        }
        if (event.key() == 256 && radialMenu.isOpen()) {
            radialMenu.close();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (radialMenu.isOpen()) {
            if (event.button() == 0) {
                GenieBehaviorMode selected = radialMenu.select(event.x(), event.y());
                if (selected != null) {
                    currentMode = selected;
                    C2SGenieCommand.send(genieId, selected);
                    response = Component.translatable("message.kubanhorizons.genie.ai.mode",
                            Component.translatable(selected.translationKey()));
                }
            }
            radialMenu.close();
            return true;
        }
        if (event.button() == 1) {
            radialMenu.open();
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY,
                                  float partialTick) {
        graphics.fill(0, 0, width, height, KHColors.MAGIC_DARK);
        graphics.fill(width / 2 - 170, height / 2 - 105,
                width / 2 + 170, height / 2 + 108, KHColors.MAGIC_PANEL);
        graphics.fill(width / 2 - 166, height / 2 - 101,
                width / 2 + 166, height / 2 + 104, KHColors.MAGIC_ACCENT);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY,
                                   float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        int centerX = width / 2;
        int centerY = height / 2;
        var text = graphics.textRenderer();
        text.accept(TextAlignment.CENTER, centerX, centerY - 90, title);
        text.accept(TextAlignment.CENTER, centerX, centerY - 73,
                Component.translatable("screen.kubanhorizons.genie.mode",
                        Component.translatable(currentMode.translationKey()))
                        .withStyle(ChatFormatting.GOLD));
        text.acceptScrolling(response, centerX, centerX - 145, centerX + 145,
                centerY + 16, centerY + 35);
        text.accept(TextAlignment.CENTER, centerX, centerY + 100,
                Component.translatable("screen.kubanhorizons.genie.hint")
                        .withStyle(ChatFormatting.DARK_GRAY));
        radialMenu.render(graphics, mouseX, mouseY);
        radialMenu.renderTooltip(graphics, mouseX, mouseY);

        // Waiting state indicator
        if (waiting) {
            // Draw semi-transparent overlay over input area
            int overlayColor = 0x80000000; // semi-transparent black
            graphics.fill(centerX - 150, centerY + 45, centerX + 150, centerY + 72, overlayColor);
            // Draw border using TEXT_WAITING color
            int borderColor = KHColors.TEXT_WAITING;
            graphics.fill(centerX - 150, centerY + 45, centerX + 150, centerY + 46, borderColor);
            graphics.fill(centerX - 150, centerY + 71, centerX + 150, centerY + 72, borderColor);
            graphics.fill(centerX - 150, centerY + 45, centerX - 149, centerY + 72, borderColor);
            graphics.fill(centerX + 149, centerY + 45, centerX + 150, centerY + 72, borderColor);
        }
    }

    public int genieId() {
        return genieId;
    }

    public Component genieName() {
        return genieName;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
