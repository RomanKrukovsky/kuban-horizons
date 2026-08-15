package dev.romankrukovsky.kubanhorizons.genie.wish;

/**
 * Результат проверки бюджета желания из {@link BudgetCalculator}.
 *
 * @param withinLimits true — желание укладывается в лимиты связи/мощности,
 *                     false — должно быть урезано или отклонено
 * @param budget       численная оценка стоимости (0..100); используется
 *                     для логирования и будущих метрик балансировки
 * @param reason       человекочитаемая причина, если {@code withinLimits == false}
 */
public record BudgetResult(boolean withinLimits, int budget, String reason) {

    public static BudgetResult ok(int budget) {
        return new BudgetResult(true, budget, null);
    }

    public static BudgetResult over(int budget, String reason) {
        return new BudgetResult(false, budget, reason);
    }
}