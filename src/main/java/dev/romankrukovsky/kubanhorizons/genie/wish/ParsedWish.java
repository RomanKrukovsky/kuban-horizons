package dev.romankrukovsky.kubanhorizons.genie.wish;

/**
 * Результат работы {@link WishParser}: разобранное желание вместе с
 * исходным текстом и именем операции, выбранной рантайом.
 *
 * <p>{@code rawText} и {@code operationName} нужны {@link dev.romankrukovsky.kubanhorizons.genie.runtime.CausalityLedger}
 * для журнала причинности: даже если операция не исполнилась, в журнале
 * остаётся, что именно просил игрок и какой обработчик пытался это сделать.</p>
 *
 * <p>Это неизменяемая запись: парсер возвращает новое значение, рантайм
 * не мутирует поля.</p>
 */
public record ParsedWish(
        String rawText,
        String operationName,
        WishIntent intent
) {

    /** Удобная фабрика для желания, у которого операция совпадает с целью. */
    public static ParsedWish of(String rawText, WishIntent intent) {
        return new ParsedWish(rawText, intent.target().name(), intent);
    }

    /** Желание без распознанной операции — используется в тестах и заглушках. */
    public static ParsedWish unknown(String rawText) {
        return new ParsedWish(rawText, "UNKNOWN", null);
    }
}