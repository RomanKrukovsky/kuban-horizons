package dev.romankrukovsky.kubanhorizons.rules;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

/**
 * MetaRuleEngine — singleton governance layer for MC 26.2.
 *
 * <p>Centralized rule evaluation for critical game actions. Rules are evaluated
 * in registration order; the first veto wins. All rules are thread-safe and
 * may be called from network or tick threads.</p>
 *
 * <p>Usage:
 * <pre>{@code
 *   MetaRuleEngine engine = MetaRuleEngine.get();
 *   engine.register(new NoDirectHarmRule());
 *   engine.register(new TemperamentBoundsRule(temperamentStore));
 *   engine.register(new ContractIntegrityRule(contractStore));
 * }</pre>
 */
@EventBusSubscriber(modid = "kubanhorizons")
public final class MetaRuleEngine {

    private static final MetaRuleEngine INSTANCE = new MetaRuleEngine();

    private final List<MetaRule> rules = Collections.synchronizedList(new ArrayList<>());
    private final Map<Class<? extends Event>, List<MetaRule>> ruleIndex = new ConcurrentHashMap<>();

    private MetaRuleEngine() {
    }

    public static MetaRuleEngine get() {
        return INSTANCE;
    }

    /**
     * Register a rule. Rules are evaluated in the order they are registered.
     */
    public void register(MetaRule rule) {
        rules.add(rule);
        // Index by event type for fast lookup
        for (Class<? extends Event> type : rule.getHandledEventTypes()) {
            ruleIndex.computeIfAbsent(type, k -> new ArrayList<>()).add(rule);
        }
    }

    /**
     * Evaluate all rules applicable to the given event/context.
     * Returns true if the action should be allowed, false if any rule vetoes.
     */
    public boolean evaluate(Event event, EvaluationContext context) {
        List<MetaRule> applicable = ruleIndex.getOrDefault(event.getClass(), List.of());
        for (MetaRule rule : applicable) {
            if (!rule.test(event, context)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Evaluate rules for a C2S wish request (network thread context).
     */
    public boolean evaluateWish(ServerPlayer player, int genieId, String wishText) {
        EvaluationContext ctx = new EvaluationContext(player, genieId, wishText, null);
        // Synthetic event for indexing; rules that care about wishes will match
        for (MetaRule rule : rules) {
            if (rule.handlesWishRequests() && !rule.testWish(player, genieId, wishText, ctx)) {
                return false;
            }
        }
        return true;
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLivingIncomingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        EvaluationContext ctx = new EvaluationContext(player, -1, null, event);
        boolean allowed = get().evaluate(event, ctx);
        if (!allowed) {
            event.setCanceled(true);
        }
    }

    /**
     * Context passed to rules during evaluation.
     */
    public record EvaluationContext(
            ServerPlayer player,
            int genieId,
            String wishText,
            LivingIncomingDamageEvent damageEvent
    ) {
    }

    /**
     * Base contract for a meta-rule.
     */
    public interface MetaRule {

        /**
         * Event types this rule is interested in (for indexing).
         */
        default List<Class<? extends Event>> getHandledEventTypes() {
            return List.of();
        }

        /**
         * Whether this rule participates in wish-request evaluation.
         */
        default boolean handlesWishRequests() {
            return false;
        }

        /**
         * Test against a generic event.
         */
        default boolean test(Event event, EvaluationContext context) {
            return true;
        }

        /**
         * Test a wish request (called from network handler).
         */
        default boolean testWish(ServerPlayer player, int genieId, String wishText,
                                 EvaluationContext context) {
            return true;
        }
    }
}
