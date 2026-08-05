package dev.romankrukovsky.kubanhorizons.util;

import dev.romankrukovsky.kubanhorizons.KubanHorizons;
import net.minecraft.resources.Identifier;

/**
 * Утилиты для создания идентификаторов мода.
 */
public final class KHIds {
    private KHIds() {
    }

    /** Создаёт {@link Identifier} в namespace мода. */
    public static Identifier of(String path) {
        return Identifier.fromNamespaceAndPath(KubanHorizons.MOD_ID, path);
    }
}
