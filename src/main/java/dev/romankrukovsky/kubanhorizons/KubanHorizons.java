package dev.romankrukovsky.kubanhorizons;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
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
    }
}
