package dev.romankrukovsky.kubanhorizons.genie.wish;

/**
 * Результат планирования желания из {@link dev.romankrukovsky.kubanhorizons.genie.runtime.plan.PlanGate}.
 *
 * <p>Планировщик решает, можно ли желание исполнить сразу
 * ({@link Status#READY}), оно требует подтверждения игрока
 * ({@link Status#NEEDS_CONFIRMATION}) или отклонено по лимитам/безопасности
 * ({@link Status#REJECTED}). Вместе со статусом рантайм получает разобранное
 * желание и выбранную операцию для исполнения.</p>
 */
public record WishPlanResult(
        Status status,
        ParsedWish parsedWish,
        WishOperation operation,
        String rejectionReason
) {

    public enum Status {
        READY,
        NEEDS_CONFIRMATION,
        REJECTED
    }

    public static WishPlanResult ready(ParsedWish wish, WishOperation operation) {
        return new WishPlanResult(Status.READY, wish, operation, null);
    }

    public static WishPlanResult needsConfirmation(ParsedWish wish, WishOperation operation) {
        return new WishPlanResult(Status.NEEDS_CONFIRMATION, wish, operation, null);
    }

    public static WishPlanResult rejected(String reason) {
        return new WishPlanResult(Status.REJECTED, null, null, reason);
    }
}