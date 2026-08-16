package genie;

import genie.visual.GenieVisualConfig;
import genie.visual.GenieTailEngine;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Main mod class for Kuban Genie
 * Handles mod initialization and registration
 */
@Mod(KubanGenie.MOD_ID)
public class KubanGenie {

    public static final String MOD_ID = "kubanhorizons";
    public static final String MOD_NAME = "Kuban Genie";
    public static final String VERSION = "0.1.0";

    public static final Logger LOGGER = LogManager.getLogger(MOD_NAME);

    public KubanGenie() {
        LOGGER.info("Initializing Kuban Genie v{}", VERSION);

        // Register mod configuration
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, GenieVisualConfig.SPEC);
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, GenieVisualConfig.SPEC);

        // Register ourselves for modloading events
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        // Register client setup
        modEventBus.addListener(this::clientSetup);

        // Register common setup
        modEventBus.addListener(this::commonSetup);

        // Register ourselves for server and other game events
        MinecraftForge.EVENT_BUS.register(this);

        LOGGER.info("Kuban Genie initialized");
    }

    private void clientSetup(final FMLClientSetupEvent event) {
        // Client-side initialization
        LOGGER.info("Kuban Genie client setup");

        // Initialize tail engine
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> GenieTailEngine::getInstance);
        LOGGER.info("Initialized Genie visual systems");
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        // Common setup code
        LOGGER.info("Kuban Genie common setup");
    }

    /**
     * Get the mod instance
     */
    public static KubanGenie getInstance() {
        // Will be set by Forge
        return null;
    }
}
