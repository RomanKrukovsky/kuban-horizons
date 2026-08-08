package dev.romankrukovsky.kubanhorizons.entity;

import net.minecraft.util.RandomSource;

/**
 * Окрас манула: четыре природных варианта, без фантастических расцветок.
 *
 * <p>Вес варианта задан здесь, а не в спавне: редкость — свойство окраса, и
 * держать её рядом с текстурой честнее, чем разбрасывать проценты по коду
 * спавна. Серебристый намеренно очень редок — на нём висит секретное
 * достижение, и встреча должна быть событием.</p>
 */
public enum ManulCoat {
    /** Степной серый — базовый, самый частый. */
    STEPPE("steppe", 60),
    /** Светло-песочный: выгоревшая августовская степь. */
    SAND("sand", 25),
    /** Тёмный горный: предгорья и каменистые балки. */
    MOUNTAIN("mountain", 13),
    /** Серебристый: очень редкий, цель секретного достижения. */
    SILVER("silver", 2);

    private static final ManulCoat[] VALUES = values();
    private static final int TOTAL_WEIGHT;

    static {
        int sum = 0;
        for (ManulCoat coat : VALUES) {
            sum += coat.weight;
        }
        TOTAL_WEIGHT = sum;
    }

    private final String key;
    private final int weight;

    ManulCoat(String key, int weight) {
        this.key = key;
        this.weight = weight;
    }

    /** Суффикс текстуры: {@code textures/entity/manul_<key>.png}. */
    public String key() {
        return key;
    }

    public int weight() {
        return weight;
    }

    /** Безопасное чтение из сохранения: неизвестный индекс даёт базовый окрас. */
    public static ManulCoat byIndex(int index) {
        return index >= 0 && index < VALUES.length ? VALUES[index] : STEPPE;
    }

    /** Поиск по суффиксу текстуры; неизвестный ключ даёт базовый окрас. */
    public static ManulCoat byKey(String key) {
        for (ManulCoat coat : VALUES) {
            if (coat.key.equals(key)) {
                return coat;
            }
        }
        return STEPPE;
    }

    /** Случайный окрас по весам редкости. */
    public static ManulCoat random(RandomSource random) {
        int roll = random.nextInt(TOTAL_WEIGHT);
        for (ManulCoat coat : VALUES) {
            roll -= coat.weight;
            if (roll < 0) {
                return coat;
            }
        }
        return STEPPE;
    }
}
