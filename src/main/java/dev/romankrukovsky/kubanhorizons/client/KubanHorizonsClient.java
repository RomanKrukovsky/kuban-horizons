package dev.romankrukovsky.kubanhorizons.client;

import dev.romankrukovsky.kubanhorizons.KubanHorizons;
import dev.romankrukovsky.kubanhorizons.config.KHClientConfig;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;

/**
 * Клиентская входная точка мода. Загружается ТОЛЬКО на клиенте —
 * dedicated server этот класс не видит.
 */
@Mod(value = KubanHorizons.MOD_ID, dist = Dist.CLIENT)
public final class KubanHorizonsClient {
    public KubanHorizonsClient(ModContainer container) {
        container.registerConfig(ModConfig.Type.CLIENT, KHClientConfig.SPEC);
    }
}
