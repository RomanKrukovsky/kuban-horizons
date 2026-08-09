package dev.romankrukovsky.kubanhorizons;

import com.mojang.logging.LogUtils;
import dev.romankrukovsky.kubanhorizons.config.KHServerConfig;
import dev.romankrukovsky.kubanhorizons.registry.KHAttachments;
import dev.romankrukovsky.kubanhorizons.registry.KHBlockEntities;
import dev.romankrukovsky.kubanhorizons.registry.KHBlocks;
import dev.romankrukovsky.kubanhorizons.registry.KHCreativeTabs;
import dev.romankrukovsky.kubanhorizons.registry.KHDataComponents;
import dev.romankrukovsky.kubanhorizons.registry.KHEntities;
import dev.romankrukovsky.kubanhorizons.registry.KHItems;
import dev.romankrukovsky.kubanhorizons.registry.KHMenus;
import dev.romankrukovsky.kubanhorizons.registry.KHRecipes;
import dev.romankrukovsky.kubanhorizons.registry.KHSounds;
import dev.romankrukovsky.kubanhorizons.worldgen.KHBiomeSources;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import org.slf4j.Logger;

/**
 * Главный класс мода «Кубанские горизонты».
 *
 * <p>Здесь выполняется только подключение регистраций и конфигурации.
 * Вся содержательная логика находится в специализированных пакетах.</p>
 */
@Mod(KubanHorizons.MOD_ID)
public final class KubanHorizons {
    public static final String MOD_ID = "kubanhorizons";
    public static final Logger LOGGER = LogUtils.getLogger();

    public KubanHorizons(IEventBus modEventBus, ModContainer modContainer) {
        LOGGER.info("Кубанские горизонты: инициализация ({}).", MOD_ID);

        KHBlocks.register(modEventBus);
        KHEntities.register(modEventBus);
        KHItems.register(modEventBus);
        KHBlockEntities.register(modEventBus);
        KHMenus.register(modEventBus);
        KHRecipes.register(modEventBus);
        KHSounds.register(modEventBus);
        KHCreativeTabs.register(modEventBus);
        KHAttachments.register(modEventBus);
        KHDataComponents.register(modEventBus);
        KHBiomeSources.register(modEventBus);
        dev.romankrukovsky.kubanhorizons.worldgen.palace.KHPalaceStructures.register(modEventBus);
        dev.romankrukovsky.kubanhorizons.registry.KHLootModifiers.register(modEventBus);
        dev.romankrukovsky.kubanhorizons.trade.KHProfessions.register(modEventBus);
        // Триггеры достижений про манула: без регистрации критерии не
        // существуют, и достижения не могут сработать в принципе.
        dev.romankrukovsky.kubanhorizons.entity.ManulCriteria.register(modEventBus);
        dev.romankrukovsky.kubanhorizons.gametest.KHGameTests.register(modEventBus);

        modContainer.registerConfig(ModConfig.Type.SERVER, KHServerConfig.SPEC);
    }
}
