package dev.romankrukovsky.kubanhorizons.registry;

import dev.romankrukovsky.kubanhorizons.KubanHorizons;
import dev.romankrukovsky.kubanhorizons.menu.OilPressMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Регистрация типов меню (контейнеров).
 */
public final class KHMenus {
    private static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, KubanHorizons.MOD_ID);

    public static final DeferredHolder<MenuType<?>, MenuType<OilPressMenu>> OIL_PRESS =
            MENUS.register("oil_press",
                    () -> IMenuTypeExtension.create((windowId, inv, buf) -> new OilPressMenu(windowId, inv)));

    private KHMenus() {
    }

    public static void register(IEventBus modEventBus) {
        MENUS.register(modEventBus);
    }
}
