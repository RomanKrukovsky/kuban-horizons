package dev.romankrukovsky.kubanhorizons.rules;

import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * TemperamentBounds — prevents wish execution that would push any temperament
 * attribute outside its configured [min, max] range.
 *
 * <p>Before a C2SWishRequest is processed, the rule consults the player's
 * current temperament profile (via the provided TemperamentProvider) and
 * rejects the wish if any resulting drift would violate bounds.</p>
 */
public final class TemperamentBoundsRule implements MetaRuleEngine.MetaRule {

    public record TemperamentDrift(
            long timestamp,
            String attribute,
            float previousValue,
            float newValue,
            String cause,
            UUID relatedWishId
    ) {
        public static TemperamentDrift create(String attribute, float previous, float current,
                                              String cause, UUID wishId) {
            return new TemperamentDrift(System.currentTimeMillis(), attribute, previous, current, cause, wishId);
        }
    }

    public interface TemperamentProvider {
        /**
         * Return current temperament values for the player.
         * Keys are attribute names (e.g., "greed", "empathy").
         */
        Map<String, Float> getTemperament(ServerPlayer player);

        /**
         * Return allowed [min, max] for each attribute.
         */
        Map<String, float[]> getBounds();
    }

    private final TemperamentProvider provider;
    private final Map<UUID, List<TemperamentDrift>> driftLog = new ConcurrentHashMap<>();

    public TemperamentBoundsRule(TemperamentProvider provider) {
        this.provider = provider;
    }

    @Override
    public boolean handlesWishRequests() {
        return true;
    }

    @Override
    public boolean testWish(ServerPlayer player, int genieId, String wishText,
                            MetaRuleEngine.EvaluationContext context) {
        Map<String, Float> current = provider.getTemperament(player);
        Map<String, float[]> bounds = provider.getBounds();

        // Simulate the wish effect (lightweight heuristic).
        // Real implementation would invoke a wish-effect predictor.
        Map<String, Float> projected = simulateWishEffect(current, wishText);

        for (Map.Entry<String, Float> entry : projected.entrySet()) {
            String attr = entry.getKey();
            float value = entry.getValue();
            float[] range = bounds.get(attr);
            if (range != null && (value < range[0] || value > range[1])) {
                // Record drift attempt for audit
                recordDriftAttempt(player.getUUID(), attr, current.getOrDefault(attr, 0f), value, wishText);
                return false;
            }
        }
        return true;
    }

    private Map<String, Float> simulateWishEffect(Map<String, Float> current, String wishText) {
        // Placeholder: in production this would call a predictor or contract evaluator.
        // For now we return the current values unchanged (rule is a no-op unless extended).
        return new java.util.HashMap<>(current);
    }

    private void recordDriftAttempt(UUID playerId, String attribute, float previous, float attempted, String cause) {
        TemperamentDrift drift = TemperamentDrift.create(attribute, previous, attempted, "REJECTED:" + cause, null);
        driftLog.computeIfAbsent(playerId, k -> new java.util.concurrent.CopyOnWriteArrayList<>()).add(drift);
    }

    public List<TemperamentDrift> getDriftLog(UUID playerId) {
        return driftLog.getOrDefault(playerId, List.of());
    }
}
