package dev.romankrukovsky.kubanhorizons.rules;

import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ContractIntegrity — prevents a player from unilaterally breaching an active
 * contract (e.g., genie contract, wish pact) without mutual consent or
 * fulfillment of exit conditions.
 *
 * <p>The rule is consulted both on wish execution paths and on any
 * contract-termination attempt. Old-style event signatures are respected;
 * the rule only needs a predicate that tells it whether a contract exists
 * for the given player+genie pair.</p>
 */
public final class ContractIntegrityRule implements MetaRuleEngine.MetaRule {

    public interface ContractStore {
        /**
         * True if an active contract exists between player and genie.
         */
        boolean hasActiveContract(ServerPlayer player, int genieId);

        /**
         * True if the contract allows unilateral termination for the given reason.
         */
        boolean allowsUnilateralExit(ServerPlayer player, int genieId, String reason);
    }

    private final ContractStore store;
    private final Map<UUID, List<BreachAttempt>> breachLog = new ConcurrentHashMap<>();

    public ContractIntegrityRule(ContractStore store) {
        this.store = store;
    }

    @Override
    public boolean handlesWishRequests() {
        return true;
    }

    @Override
    public boolean testWish(ServerPlayer player, int genieId, String wishText,
                            MetaRuleEngine.EvaluationContext context) {
        if (!store.hasActiveContract(player, genieId)) {
            return true;
        }
        // If the wish text contains an explicit termination request without
        // satisfying exit conditions, veto.
        if (isTerminationRequest(wishText) && !store.allowsUnilateralExit(player, genieId, wishText)) {
            recordBreachAttempt(player.getUUID(), genieId, wishText);
            return false;
        }
        return true;
    }

    private boolean isTerminationRequest(String wishText) {
        if (wishText == null) return false;
        String lower = wishText.toLowerCase();
        return lower.contains("release") || lower.contains("free") || lower.contains("terminate")
                || lower.contains("break contract") || lower.contains("end pact");
    }

    private void recordBreachAttempt(UUID playerId, int genieId, String wish) {
        breachLog.computeIfAbsent(playerId, k -> new java.util.concurrent.CopyOnWriteArrayList<>())
                .add(new BreachAttempt(System.currentTimeMillis(), genieId, wish));
    }

    public record BreachAttempt(long timestamp, int genieId, String wishText) {
    }

    public List<BreachAttempt> getBreachLog(UUID playerId) {
        return breachLog.getOrDefault(playerId, List.of());
    }
}
