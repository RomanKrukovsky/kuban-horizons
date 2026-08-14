package dev.romankrukovsky.kubanhorizons.rules;

import dev.romankrukovsky.kubanhorizons.KubanHorizons;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;

/**
 * Bootstraps the MetaRuleEngine singleton for MC 26.2.
 *
 * <p>Call {@link #init()} once during common setup. The three core rules
 * (NoDirectHarm, TemperamentBounds, ContractIntegrity) are registered with
 * sensible defaults. Replace the default stores with your persistent
 * implementations as needed.</p>
 */
@EventBusSubscriber(modid = KubanHorizons.MOD_ID)
public final class MetaRuleBootstrap {

    private MetaRuleBootstrap() {
    }

    @SubscribeEvent
    public static void onCommonSetup(FMLCommonSetupEvent event) {
        init();
    }

    /**
     * Initialize the singleton with the three required rules.
     */
    public static void init() {
        MetaRuleEngine engine = MetaRuleEngine.get();

        // 1) NoDirectHarm — protects owned genies from direct damage by owner
        engine.register(new NoDirectHarmRule());

        // 2) TemperamentBounds — consults the attachment system for current temperament
        engine.register(new TemperamentBoundsRule(new DefaultTemperamentProvider()));

        // 3) ContractIntegrity — prevents unilateral wish-based contract termination
        engine.register(new ContractIntegrityRule(new DefaultContractStore()));

        KubanHorizons.LOGGER.info("[MetaRuleEngine] MC 26.2 rules initialized: NoDirectHarm, TemperamentBounds, ContractIntegrity");
    }

    // ---------------------------------------------------------------------
    // Default in-memory providers (replace with persistent stores in production)
    // ---------------------------------------------------------------------

    private static final class DefaultTemperamentProvider implements TemperamentBoundsRule.TemperamentProvider {
        @Override
        public java.util.Map<String, Float> getTemperament(net.minecraft.server.level.ServerPlayer player) {
            // In production: read from PLAYER_TEMPERAMENT attachment
            return java.util.Map.of("greed", 0.5f, "empathy", 0.5f, "ambition", 0.5f);
        }

        @Override
        public java.util.Map<String, float[]> getBounds() {
            return java.util.Map.of(
                    "greed", new float[]{0f, 1f},
                    "empathy", new float[]{0f, 1f},
                    "ambition", new float[]{0f, 1f}
            );
        }
    }

    private static final class DefaultContractStore implements ContractIntegrityRule.ContractStore {
        @Override
        public boolean hasActiveContract(net.minecraft.server.level.ServerPlayer player, int genieId) {
            // In production: check PLAYER_GENIE_DATA attachment for active pact
            return false;
        }

        @Override
        public boolean allowsUnilateralExit(net.minecraft.server.level.ServerPlayer player, int genieId, String reason) {
            return false;
        }
    }
}
