package dev.romankrukovsky.kubanhorizons.genie.wish;

import dev.romankrukovsky.kubanhorizons.genie.GeniePersonality;

/**
 * Калькулятор стоимости желания.
 *
 * <p>Точность формулировки ({@link WishIntent#precision()}) — это «бюджет
 * связи»: чем выше точность, тем больше операций рантайм может себе
 * позволить. Лимит берётся из темперамента: осторожные характеры
 * ({@link dev.romankrukovsky.kubanhorizons.genie.GenieTemperament#GUARDED},
 * {@code DANGEROUS}) дают меньше пространства для буквальных трактовок,
 * добрые — больше.</p>
 */
public final class BudgetCalculator {

    /** Максимальный бюджет по умолчанию для готового характера. */
    public static final int DEFAULT_LIMIT = 60;
    /** Бонус для безопасных формулировок. */
    public static final int SAFE_BONUS = 25;

    private BudgetCalculator() {
    }

    public static BudgetResult calculate(GeniePersonality personality, ParsedWish wish) {
        if (wish == null || wish.intent() == null) {
            return BudgetResult.ok(0);
        }
        int precision = wish.intent().precision();
        int limit = limitFor(personality);
        if (wish.intent().safe()) {
            precision += SAFE_BONUS;
        }
        if (precision > limit + SAFE_BONUS) {
            return BudgetResult.over(precision, "Желание выходит за лимит связи (" + limit + ").");
        }
        return BudgetResult.ok(Math.min(100, precision));
    }

    private static int limitFor(GeniePersonality personality) {
        if (personality == null) {
            return DEFAULT_LIMIT;
        }
        return switch (personality.temperament()) {
            case KIND, SARDONIC -> DEFAULT_LIMIT + 20;
            case PROUD, CUNNING -> DEFAULT_LIMIT + 5;
            case GUARDED, DANGEROUS -> DEFAULT_LIMIT - 15;
        };
    }
}