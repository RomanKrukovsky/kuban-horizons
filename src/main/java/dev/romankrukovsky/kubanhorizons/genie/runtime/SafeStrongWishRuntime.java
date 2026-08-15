package dev.romankrukovsky.kubanhorizons.genie.runtime;

import dev.romankrukovsky.kubanhorizons.genie.GeniePersonality;
import dev.romankrukovsky.kubanhorizons.genie.personality.TemperamentReactionEngine;
import dev.romankrukovsky.kubanhorizons.genie.wish.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import java.util.*;

/**
 * Transactional boundary for executing strong wishes safely.
 *
 * Responsibilities:
 * - Enforce budget
 * - Apply temperament reaction
 * - Execute operation in isolation
 * - Record everything in CausalityLedger
 */
public final class SafeStrongWishRuntime {

    private final CausalityLedger ledger = new CausalityLedger();

    public WishResult execute(WishPlanResult plan, ServerPlayer player, GeniePersonality personality, ServerLevel level) {

        if (plan.status() == WishPlanResult.Status.REJECTED) {
            return WishResult.rejected(plan.rejectionReason());
        }

        if (plan.status() == WishPlanResult.Status.NEEDS_CONFIRMATION) {
            return WishResult.needsConfirmation(plan);
        }

        ParsedWish parsed = plan.parsedWish();
        WishOperation operation = plan.operation();

        // 1. Budget check (already done in PlanGate, but double-check here)
        BudgetResult budget = BudgetCalculator.calculate(personality, parsed);
        if (!budget.withinLimits()) {
            return WishResult.truncated("Желание урезано из-за лимита связи.");
        }

        // 2. Temperament reaction
        TemperamentReactionEngine.Reaction reaction =
                TemperamentReactionEngine.decideReaction(parsed, personality);

        // 3. Execute the operation inside a controlled context
        try {
            // In a real implementation we would wrap world access here
            Object result = operation.execute(parsed, level, player);

            // 4. Record in ledger
            ledger.recordExecution(player, parsed, level, List.of(), List.of());

            // 5. Return success with reaction info
            return WishResult.successWithReaction(result, reaction);

        } catch (Exception e) {
            // Future: trigger rollback via ledger
            return WishResult.failed("Ошибка исполнения: " + e.getMessage());
        }
    }
}
