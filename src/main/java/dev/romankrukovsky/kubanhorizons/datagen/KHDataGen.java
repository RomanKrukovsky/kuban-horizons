package dev.romankrukovsky.kubanhorizons.datagen;

import dev.romankrukovsky.kubanhorizons.KubanHorizons;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.data.event.GatherDataEvent;

/**
 * Точка входа datagen. Все JSON-данные мода генерируются здесь (AD-005).
 */
@EventBusSubscriber(modid = KubanHorizons.MOD_ID)
public final class KHDataGen {
    private KHDataGen() {
    }

    /** Datapack-реестры worldgen (регистрируются первыми — см. api-notes). */
    private static final net.minecraft.core.RegistrySetBuilder WORLDGEN_BUILDER =
            new net.minecraft.core.RegistrySetBuilder()
                    .add(net.minecraft.core.registries.Registries.CONFIGURED_FEATURE,
                            dev.romankrukovsky.kubanhorizons.worldgen.KHConfiguredFeatures::bootstrap)
                    .add(net.minecraft.core.registries.Registries.PLACED_FEATURE,
                            dev.romankrukovsky.kubanhorizons.worldgen.KHPlacedFeatures::bootstrap)
                    .add(net.neoforged.neoforge.registries.NeoForgeRegistries.Keys.BIOME_MODIFIERS,
                            dev.romankrukovsky.kubanhorizons.worldgen.KHBiomeModifiers::bootstrap)
                    .add(net.minecraft.core.registries.Registries.VILLAGER_TRADE,
                            dev.romankrukovsky.kubanhorizons.trade.KHTrades::bootstrapTrades)
                    .add(net.minecraft.core.registries.Registries.TRADE_SET,
                            dev.romankrukovsky.kubanhorizons.trade.KHTrades::bootstrapTradeSets);

    @SubscribeEvent
    static void onGatherData(GatherDataEvent.Client event) {
        event.createDatapackRegistryObjects(WORLDGEN_BUILDER);
        event.createBlockAndItemTags(KHBlockTagsProvider::new, KHItemTagsProvider::new);
        event.createProvider(KHModelProvider::new);
        event.createProvider(KHRecipeProvider.Runner::new);
        event.createProvider(KHLootTableProvider::new);
        event.createProvider(KHEnglishLanguageProvider::new);
        event.createProvider(KHRussianLanguageProvider::new);
        event.createProvider(KHAdvancementProvider::new);
        event.createProvider(KHSoundDefinitionsProvider::new);
        event.createProvider(KHDataMapProvider::new);
        event.createProvider(KHLootModifierProvider::new);
    }
}
