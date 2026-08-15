package dev.romankrukovsky.kubanhorizons.client.input;

import dev.romankrukovsky.kubanhorizons.util.KHIds;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

/**
 * Клавиатурные привязки для Кубанской Джиннии.
 */
public final class GenieKeyBindings {

    public static final KeyMapping.Category CATEGORY =
            KeyMapping.Category.register(KHIds.of("genie"));

    public static final KeyMapping SNAP = new KeyMapping(
            "key.kubanhorizons.genie_snap",
            GLFW.GLFW_KEY_SPACE,
            CATEGORY
    );

    private GenieKeyBindings() {}
}
