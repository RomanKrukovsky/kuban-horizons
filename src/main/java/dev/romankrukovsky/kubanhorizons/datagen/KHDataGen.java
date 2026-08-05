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

    @SubscribeEvent
    static void onGatherData(GatherDataEvent.Client event) {
        event.createBlockAndItemTags(KHBlockTagsProvider::new, KHItemTagsProvider::new);
        event.createProvider(KHModelProvider::new);
        event.createProvider(KHRecipeProvider.Runner::new);
        event.createProvider(KHLootTableProvider::new);
        event.createProvider(KHEnglishLanguageProvider::new);
        event.createProvider(KHRussianLanguageProvider::new);
        event.createProvider(KHAdvancementProvider::new);
        event.createProvider(KHSoundDefinitionsProvider::new);
        event.createProvider(KHDataMapProvider::new);
    }
}
