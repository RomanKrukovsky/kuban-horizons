package dev.romankrukovsky.kubanhorizons.entity;

/**
 * Ступени доверия манула.
 *
 * <p>Доверие — не приручение. Манул не становится домашним котом ни на одной
 * ступени: даже на высшей он остаётся независимым, уходит охотиться и
 * возвращается сам. Поэтому шкала описывает не «прирученность», а то, насколько
 * близко зверь готов подпустить и признать участок игрока безопасным.</p>
 *
 * <p>Порог задан в очках, а не в игровых днях: день можно проспать, а очки
 * набираются подношениями и спокойным поведением. Зато очки начисляются не
 * чаще раза в игровой день ({@link Manul#OFFER_COOLDOWN_TICKS}) — именно это
 * растягивает знакомство на несколько дней, как и задумано.</p>
 */
public enum ManulTrust {
    /** Дикий: убегает, шипит, подойти нельзя. */
    WILD(0),
    /** Перестал убегать: терпит присутствие на расстоянии. */
    WARY(4),
    /** Подпускает: можно подойти вплотную, не спугнув. */
    ACCEPTING(9),
    /** Подходит сам: инициатива на стороне зверя. */
    FRIENDLY(15),
    /** Признал участок: может поселиться у усадьбы. */
    RESIDENT(24);

    private static final ManulTrust[] VALUES = values();

    private final int threshold;

    ManulTrust(int threshold) {
        this.threshold = threshold;
    }

    public int threshold() {
        return threshold;
    }

    /** Максимум шкалы: выше набирать бессмысленно. */
    public static int maxPoints() {
        return RESIDENT.threshold;
    }

    /** Ступень по накопленным очкам. */
    public static ManulTrust ofPoints(int points) {
        ManulTrust best = WILD;
        for (ManulTrust stage : VALUES) {
            if (points >= stage.threshold) {
                best = stage;
            }
        }
        return best;
    }

    /** Достигнута ли ступень (для достижений и условий поведения). */
    public boolean atLeast(ManulTrust other) {
        return ordinal() >= other.ordinal();
    }
}
