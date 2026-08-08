package dev.romankrukovsky.kubanhorizons.entity;

import net.minecraft.util.RandomSource;

/**
 * Скрытый характер манула: шесть типов, различия механические.
 *
 * <p>Смысл характеров в том, чтобы две встреченные особи вели себя по-разному
 * и запоминались отдельно. Поэтому здесь не флаг для реплик, а множители,
 * которые реально меняют игру: дистанцию побега, скорость роста доверия и
 * склонность подходить самому. Косметический характер игрок бы не заметил.</p>
 *
 * <p>Характер скрыт: он не выводится в интерфейс и узнаётся только по
 * поведению — тем, что один манул подпускает ближе, а другой сбегает с
 * полполя.</p>
 *
 * @param key           идентификатор для сохранения и локализации
 * @param fleeDistance  дистанция, с которой особь начинает уходить, в блоках
 * @param trustRate     множитель скорости роста доверия
 * @param curiosity     склонность подходить к игроку самому, 0..1
 * @param appetite      множитель ценности подношения
 * @param idleWeight    множитель длительности сидения на месте
 */
public record ManulPersonality(String key, float fleeDistance, float trustRate,
                               float curiosity, float appetite, float idleWeight) {
    /** Осторожный: держит дистанцию, доверяет медленно. */
    public static final ManulPersonality CAUTIOUS =
            new ManulPersonality("cautious", 14.0F, 0.7F, 0.15F, 1.0F, 0.8F);
    /** Ленивый: почти не убегает, но и подходить не станет. Сидит дольше всех. */
    public static final ManulPersonality LAZY =
            new ManulPersonality("lazy", 7.0F, 1.0F, 0.10F, 1.2F, 2.4F);
    /** Любопытный: подпускает близко и сам подходит первым. */
    public static final ManulPersonality CURIOUS =
            new ManulPersonality("curious", 8.0F, 1.3F, 0.55F, 1.0F, 0.9F);
    /** Ворчливый: шипит охотнее прочих, доверяет неохотно. */
    public static final ManulPersonality GRUMPY =
            new ManulPersonality("grumpy", 11.0F, 0.6F, 0.12F, 0.9F, 1.3F);
    /** Храбрый: не отступает, при угрозе отвечает. Сидит меньше всех. */
    public static final ManulPersonality BRAVE =
            new ManulPersonality("brave", 6.0F, 1.0F, 0.35F, 1.0F, 0.6F);
    /** Прожорливый: за еду прощает почти всё. */
    public static final ManulPersonality GREEDY =
            new ManulPersonality("greedy", 9.0F, 1.1F, 0.40F, 1.8F, 1.1F);

    private static final ManulPersonality[] ALL = {
            CAUTIOUS, LAZY, CURIOUS, GRUMPY, BRAVE, GREEDY,
    };

    /** Безопасное чтение из сохранения: неизвестный ключ даёт осторожного. */
    public static ManulPersonality byKey(String key) {
        for (ManulPersonality personality : ALL) {
            if (personality.key.equals(key)) {
                return personality;
            }
        }
        return CAUTIOUS;
    }

    public static ManulPersonality random(RandomSource random) {
        return ALL[random.nextInt(ALL.length)];
    }

    /** Ключ локализации — для достижений и подсказок, не для интерфейса. */
    public String translationKey() {
        return "manul.personality." + key;
    }
}
