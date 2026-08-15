package dev.romankrukovsky.kubanhorizons.genie.wish;

import dev.romankrukovsky.kubanhorizons.genie.personality.TemperamentReactionEngine;

/**
 * Результат исполнения желания из {@link dev.romankrukovsky.kubanhorizons.genie.runtime.SafeStrongWishRuntime}.
 *
 * <p>Несёт статус ({@link Status}), человекочитаемое сообщение и — для
 * успешных исполнений — произвольный payload операции плюс реакцию
 * темперамента джиннии. UI диалога показывает сообщение; causality ledger
 * отдельно хранит записи об исполнении.</p>
 */
public record WishResult(
        Status status,
        String message,
        Object payload,
        TemperamentReactionEngine.Reaction reaction,
        WishPlanResult pendingPlan
) {

    public enum Status {
        SUCCESS,
        NEEDS_CONFIRMATION,
        TRUNCATED,
        REJECTED,
        FAILED
    }

    public static WishResult successWithReaction(Object payload, TemperamentReactionEngine.Reaction reaction) {
        return new WishResult(Status.SUCCESS, "Готово.", payload, reaction, null);
    }

    public static WishResult needsConfirmation(WishPlanResult plan) {
        return new WishResult(Status.NEEDS_CONFIRMATION, "Требуется подтверждение.", null, null, plan);
    }

    public static WishResult truncated(String reason) {
        return new WishResult(Status.TRUNCATED, reason, null, null, null);
    }

    public static WishResult rejected(String reason) {
        return new WishResult(Status.REJECTED, reason, null, null, null);
    }

    public static WishResult failed(String reason) {
        return new WishResult(Status.FAILED, reason, null, null, null);
    }
}