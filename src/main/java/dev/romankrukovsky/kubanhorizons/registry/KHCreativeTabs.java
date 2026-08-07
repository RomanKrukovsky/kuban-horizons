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
                        output.accept(KHItems.TOMATO_SEEDS.get());
                        output.accept(KHItems.TOMATO.get());
                        output.accept(KHItems.GRAPE_TRELLIS.get());
                        output.accept(KHItems.GRAPE_CUTTING.get());
                        output.accept(KHItems.GRAPES.get());
                        output.accept(KHItems.RICE_SEEDLINGS.get());
                        output.accept(KHItems.RICE_PANICLE.get());
                        output.accept(KHItems.RICE.get());
                        output.accept(KHItems.COOKED_RICE.get());
                        output.accept(KHItems.PEACH_SAPLING.get());
                        output.accept(KHItems.PEACH.get());
                        output.accept(KHItems.APRICOT_SAPLING.get());
                        output.accept(KHItems.APRICOT.get());
                        output.accept(KHItems.PLUM_SAPLING.get());
                        output.accept(KHItems.PLUM.get());
                        output.accept(KHItems.WALNUT_SAPLING.get());
                        output.accept(KHItems.WALNUT.get());
                        output.accept(KHItems.SOIL_PROBE.get());
                        output.accept(KHItems.OIL_PRESS.get());
                        output.accept(KHItems.IRRIGATION_CHANNEL.get());
                        output.accept(KHItems.WATER_INTAKE.get());
                        output.accept(KHItems.DRYING_RACK.get());
                        output.accept(KHItems.DRIED_TEA.get());
                        output.accept(KHItems.DRIED_FRUIT.get());
                        output.accept(KHItems.HAND_MILL.get());
                        output.accept(KHItems.FLOUR.get());
                        output.accept(KHItems.CORNMEAL.get());
                        output.accept(KHItems.HOMEMADE_BREAD.get());
                        output.accept(KHItems.BORSCHT.get());
                        output.accept(KHItems.MAMALYGA.get());
                        output.accept(KHItems.TEA_CUP.get());
                        output.accept(KHItems.HONEY_WALNUTS.get());
                        output.accept(KHItems.VEGETABLE_SPREAD.get());
                        output.accept(KHItems.PHEASANT_SPAWN_EGG.get());
                        output.accept(KHItems.QUAIL_SPAWN_EGG.get());
                        output.accept(KHItems.RAW_PHEASANT.get());
                        output.accept(KHItems.COOKED_PHEASANT.get());
                        output.accept(KHItems.RAW_QUAIL.get());
                        output.accept(KHItems.COOKED_QUAIL.get());
                        output.accept(KHItems.ADOBE_BRICKS.get());
                        output.accept(KHItems.ADOBE_BRICK_STAIRS.get());
                        output.accept(KHItems.ADOBE_BRICK_SLAB.get());
                        output.accept(KHItems.ADOBE_BRICK_WALL.get());
                        output.accept(KHItems.SHELL_ROCK.get());
                        output.accept(KHItems.SHELL_ROCK_STAIRS.get());
                        output.accept(KHItems.SHELL_ROCK_SLAB.get());
                        output.accept(KHItems.SHELL_ROCK_WALL.get());
                        output.accept(KHItems.WHITEWASHED_PLASTER.get());
                        output.accept(KHItems.WHITEWASHED_PLASTER_STAIRS.get());
                        output.accept(KHItems.WHITEWASHED_PLASTER_SLAB.get());
                        output.accept(KHItems.ROOF_TILES.get());
                        output.accept(KHItems.ROOF_TILE_STAIRS.get());
                        output.accept(KHItems.ROOF_TILE_SLAB.get());
                        output.accept(KHItems.DECORATIVE_CERAMIC.get());
                        output.accept(KHItems.CARVED_WINDOW_CASING.get());
                        output.accept(KHItems.WATTLE.get());
                        output.accept(KHItems.WATTLE_GATE.get());
                    })
                    .build());

    private KHCreativeTabs() {
    }

    public static void register(IEventBus modEventBus) {
        TABS.register(modEventBus);
    }
}
