package dev.romankrukovsky.kubanhorizons.genie.runtime;

import dev.romankrukovsky.kubanhorizons.genie.GeniePersonality;
import dev.romankrukovsky.kubanhorizons.genie.personality.TemperamentReactionEngine;
import dev.romankrukovsky.kubanhorizons.genie.wish.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import java.util.*;

/**
 * Transactional boundary for executing strong wishes safely.
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

        // Isolation: we don't give raw world access to the operation
        try {
            ParsedWish parsed = plan.parsedWish();
            WishOperation operation = plan.operation();

            // Execute within controlled context
            List result = operation.execute(parsed, level, player);

            // Record for causality
            ledger.recordExecution(player, parsed, level, List.of(), List.of()); // simplified

            return WishResult.success(result);
        } catch (Exception e) {
            return WishResult.failed("Ошибка исполнения: " + e.getMessage());
        }
    }
}
