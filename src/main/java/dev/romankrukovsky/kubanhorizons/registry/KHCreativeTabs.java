package dev.romankrukovsky.kubanhorizons.registry;

import dev.romankrukovsky.kubanhorizons.KubanHorizons;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Вкладка творческого режима «Кубанские горизонты».
 */
public final class KHCreativeTabs {
    private static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, KubanHorizons.MOD_ID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN =
            TABS.register("main", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.kubanhorizons"))
                    .withTabsBefore(CreativeModeTabs.SPAWN_EGGS.identifier())
                    .icon(() -> KHItems.SUNFLOWER_SEEDS.get().getDefaultInstance())
                    .displayItems((parameters, output) -> {
                        output.accept(KHItems.SUNFLOWER_SEEDS.get());
                        output.accept(KHItems.SUNFLOWER_HEAD.get());
                        output.accept(KHItems.SUNFLOWER_OIL.get());
                        output.accept(KHItems.OIL_CAKE.get());
                        output.accept(KHItems.ROASTED_SUNFLOWER_SEEDS.get());
                        output.accept(KHItems.CORN_KERNELS.get());
                        output.accept(KHItems.CORN_COB.get());
                        output.accept(KHItems.GRILLED_CORN.get());
                        output.accept(KHItems.TEA_SAPLING.get());
                        output.accept(KHItems.TEA_LEAVES.get());
                        output.accept(KHItems.GRAPE_TRELLIS.get());
                        output.accept(KHItems.GRAPE_CUTTING.get());
                        output.accept(KHItems.GRAPES.get());
                        output.accept(KHItems.RICE_SEEDLINGS.get());
                        output.accept(KHItems.RICE_PANICLE.get());
                        output.accept(KHItems.RICE.get());
                        output.accept(KHItems.COOKED_RICE.get());
                        output.accept(KHItems.SOIL_PROBE.get());
                        output.accept(KHItems.OIL_PRESS.get());
                        output.accept(KHItems.IRRIGATION_CHANNEL.get());
                        output.accept(KHItems.WATER_INTAKE.get());
                    })
                    .build());

    private KHCreativeTabs() {
    }

    public static void register(IEventBus modEventBus) {
        TABS.register(modEventBus);
    }
}
