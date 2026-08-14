package dev.romankrukovsky.kubanhorizons.genie.wish;

import java.util.Locale;
import java.util.regex.Pattern;

/** Расширенный двуязычный парсер Закона точности для любых категорий желаний. */
public final class WishParser {
    private static final Pattern NUMBER = Pattern.compile("(^|\\D)(64|sixty[- ]?four)(\\D|$)");

    private WishParser() {
    }

    public static WishIntent parse(String text) {
        String normalized = text.toLowerCase(Locale.ROOT).trim();

        boolean safe = containsAny(normalized, "безопас", "не навред", "аккурат", "safely", "without harm");
        boolean polite = containsAny(normalized, "желаю", "пожалуйста", "прошу", "i wish", "please");
        boolean commanding = containsAny(normalized, "немедленно", "приказываю", "сейчас же", "immediately", "i command");

        WishIntent.Target target = WishIntent.Target.UNKNOWN;
        WishIntent.Category category = WishIntent.Category.UNKNOWN;
        String detailParam = "";

        // 0. Искажённые желания высшего порядка (превращение в джиннию)
        if (containsAny(normalized, "всемогущ", "omnipotent", "всесильн")) {
            target = WishIntent.Target.OMNIPOTENCE;
            category = WishIntent.Category.DISTORTED_HIGHER_WISH;
        } else if (containsAny(normalized, "жить вечно", "вечно жить", "бессмерти", "live forever", "eternal life")) {
            target = WishIntent.Target.ETERNITY;
            category = WishIntent.Category.DISTORTED_HIGHER_WISH;
        } else if (containsAny(normalized, "свои желания", "любые свои", "own wishes")) {
            target = WishIntent.Target.SELF_FULFILLMENT;
            category = WishIntent.Category.DISTORTED_HIGHER_WISH;
        } else if (containsAny(normalized, "такой же как ты", "такой же, как ты", "как ты", "just like you", "be a genie")) {
            target = WishIntent.Target.MIRROR_TRANSFORMATION;
            category = WishIntent.Category.DISTORTED_HIGHER_WISH;
        } else if (containsAny(normalized, "твою силу", "твою мощь", "your power")) {
            target = WishIntent.Target.POWER_TRANSFER;
            category = WishIntent.Category.DISTORTED_HIGHER_WISH;
        } else if (containsAny(normalized, "причини", "не мог причинить", "no one can harm")) {
            target = WishIntent.Target.ABS_INTANGIBILITY;
            category = WishIntent.Category.DISTORTED_HIGHER_WISH;
        }
        // 1. Мета-желания (изменение Minecraft)
        else if (containsAny(normalized, "ночь", "night") && containsAny(normalized, "дольше", "длиннее", "longer")) {
            target = WishIntent.Target.META_LONGER_NIGHT;
            category = WishIntent.Category.META_RULE;
        } else if (containsAny(normalized, "крипер", "creeper") && containsAny(normalized, "не взрыв", "не руш", "разруш", "не ломал", "no damage")) {
            target = WishIntent.Target.META_NO_CREEPER_DAMAGE;
            category = WishIntent.Category.META_RULE;
        } else if (containsAny(normalized, "плав", "smelt", "печь", "furnace") && containsAny(normalized, "быстр", "мгнов", "instant")) {
            target = WishIntent.Target.META_INSTANT_SMELT;
            category = WishIntent.Category.META_RULE;
        }
        // 2. Желания гигантизма
        else if (containsAny(normalized, "гигант", "увелич", "огромн", "giant", "huge", "big")) {
            category = WishIntent.Category.GIGANTISM;
            if (containsAny(normalized, "пирог", "pie")) {
                target = WishIntent.Target.BIG_PIE;
            } else if (containsAny(normalized, "куриц", "курица", "chicken")) {
                target = WishIntent.Target.BIG_CHICKEN;
            } else if (containsAny(normalized, "кроват", "bed")) {
                target = WishIntent.Target.BIG_BED;
            }
        }
        // 3. Желания мобов
        else if (containsAny(normalized, "корова", "cow") && containsAny(normalized, "снег", "snow")) {
            target = WishIntent.Target.MOB_WISH_COW;
            category = WishIntent.Category.MOB_WISH;
        } else if (containsAny(normalized, "волк", "wolf") && containsAny(normalized, "хозяин", "owner", "master")) {
            target = WishIntent.Target.MOB_WISH_WOLF;
            category = WishIntent.Category.MOB_WISH;
        } else if (containsAny(normalized, "крипер", "creeper") && containsAny(normalized, "не взрываться", "не взрывайся", "dont explode")) {
            target = WishIntent.Target.MOB_WISH_CREEPER;
            category = WishIntent.Category.MOB_WISH;
        } else if (containsAny(normalized, "голем", "golem") && containsAny(normalized, "смысл", "зачем", "purpose")) {
            target = WishIntent.Target.MOB_WISH_GOLEM;
            category = WishIntent.Category.MOB_WISH;
        }
        // 4. Развитие поселений
        else if (containsAny(normalized, "деревн", "village") && containsAny(normalized, "богат", "процвета", "rich")) {
            target = WishIntent.Target.VILLAGE_WEALTH;
            category = WishIntent.Category.CIVILIZATION;
        }
        // 5. Стандартный материальный алмазный срез
        else if (containsAny(normalized, "алмаз", "diamond")) {
            target = WishIntent.Target.DIAMONDS;
            category = WishIntent.Category.MATERIAL;
        }

        WishIntent.Amount amount = amount(normalized, category);
        WishIntent.Placement placement = placement(normalized);

        // If no specific target was matched, delegate to LLMWishExecutor
        // This ensures WishParser NEVER returns an UNKNOWN target
        if (target == WishIntent.Target.UNKNOWN) {
            target = WishIntent.Target.LLM_DELEGATED;
            category = WishIntent.Category.UNKNOWN;
            detailParam = text; // Pass original text for LLM interpretation
        }

        int precision = target == WishIntent.Target.LLM_DELEGATED ? 10 : 20;
        precision += switch (amount) {
            case CHEST -> 25;
            case STACK -> 20;
            case GIANT -> 30;
            case MANY -> 5;
            case UNSPECIFIED -> 0;
        };
        precision += switch (placement) {
            case IN_FRONT -> 25;
            case AT_FEET -> 15;
            case ABOVE -> 5;
            case UNSPECIFIED -> 0;
        };
        if (safe) {
            precision += 25;
        }
        if (polite) {
            precision += 5;
        }

        return new WishIntent(target, amount, placement, category, safe, polite, commanding,
                Math.min(100, precision), detailParam);
    }

    private static WishIntent.Amount amount(String text, WishIntent.Category category) {
        if (category == WishIntent.Category.GIGANTISM || containsAny(text, "гигант", "огромн", "giant", "huge")) {
            return WishIntent.Amount.GIANT;
        }
        if (containsAny(text, "сундук", "chest")) {
            return WishIntent.Amount.CHEST;
        }
        if (NUMBER.matcher(text).find() || containsAny(text, "стак", "stack")) {
            return WishIntent.Amount.STACK;
        }
        if (containsAny(text, "много", "гора", "куча", "many", "mountain", "pile")) {
            return WishIntent.Amount.MANY;
        }
        return WishIntent.Amount.UNSPECIFIED;
    }

    private static WishIntent.Placement placement(String text) {
        if (containsAny(text, "передо мной", "перед собой", "in front of me", "in front")) {
            return WishIntent.Placement.IN_FRONT;
        }
        if (containsAny(text, "под ноги", "у ног", "at my feet", "by my feet")) {
            return WishIntent.Placement.AT_FEET;
        }
        if (containsAny(text, "надо мной", "над головой", "above me", "over my head")) {
            return WishIntent.Placement.ABOVE;
        }
        return WishIntent.Placement.UNSPECIFIED;
    }

    private static boolean containsAny(String text, String... fragments) {
        for (String fragment : fragments) {
            if (text.contains(fragment)) {
                return true;
            }
        }
        return false;
    }
}
