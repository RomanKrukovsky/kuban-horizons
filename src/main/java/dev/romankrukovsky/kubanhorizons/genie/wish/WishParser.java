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
        // 4.5 Магическая музыка: песня как язык изменения мира
        else if (containsAny(normalized, "песн", "спой", "сыграй", "музык", "мелоди", "song", "sing", "music", "melody")) {
            target = WishIntent.Target.MUSIC_SPELL;
            category = WishIntent.Category.MUSIC;
            if (containsAny(normalized, "дождь", "дождя", "rain")) {
                detailParam = "RAIN";
            } else if (containsAny(normalized, "рост", "урожай", "crop", "grow", "growth")) {
                detailParam = "GROWTH";
            } else if (containsAny(normalized, "покой", "спокойств", "колыбел", "peace", "calm", "lullaby")) {
                detailParam = "PEACE";
            } else if (containsAny(normalized, "огонь", "огн", "пожар", "fire")) {
                detailParam = "FIRE";
            }
        }
        // 5. Социум: ежегодный праздник джиннии
        else if (containsAny(normalized, "праздник", "фестиваль", "торжество", "festival")) {
            target = WishIntent.Target.GENIE_FESTIVAL;
            category = WishIntent.Category.CIVILIZATION;
        }
        // 6. Стандартный материальный алмазный срез
        else if (containsAny(normalized, "алмаз", "diamond")) {
            target = WishIntent.Target.DIAMONDS;
            category = WishIntent.Category.MATERIAL;
        }
        // 7. Provenance-запрос: «откуда этот предмет?» / «where did this come from?»
        else if (containsAny(normalized, "откуда этот предмет", "откуда этот блок", "откуда взялся",
                "where did this come from", "where is this from", "provenance")) {
            target = WishIntent.Target.PROVENANCE_QUERY;
            category = WishIntent.Category.PROVENANCE;
        }
        // 8. История: «А что если?» — альтернативные версии мира
        else if (containsAny(normalized, "что если", "а что если", "what if")) {
            target = WishIntent.Target.WHAT_IF;
            category = WishIntent.Category.HISTORY;
            detailParam = text;
        }
        // 8а. История: «Покажи, что здесь было» — театр реальности
        else if (containsAny(normalized, "театр", "что здесь было", "что тут было", "reenact", "theater")) {
            target = WishIntent.Target.THEATER_REENACTMENT;
            category = WishIntent.Category.HISTORY;
            detailParam = text;
        }
        // 9. Материализация слов и рисунков, переписывание биома
        else if (containsAny(normalized, "напиши слово", "написать слово", "материализ", "write the word", "materialize")) {
            target = WishIntent.Target.WORD_MATERIALIZATION;
            category = WishIntent.Category.MATERIAL;
            detailParam = text;
        } else if (containsAny(normalized, "нарисуй", "рисун", "нарисовать", "draw", "drawing")) {
            target = WishIntent.Target.DRAWING;
            category = WishIntent.Category.MATERIAL;
            detailParam = text;
        } else if (containsAny(normalized, "перепиши биом", "биом в", "сделай степь", "поменяй биом", "rewrite biome", "biome to")) {
            target = WishIntent.Target.BIOME_REWRITE;
            category = WishIntent.Category.CIVILIZATION;
            detailParam = text;
        }
        // 9а. Память блоков: «О чём говорит блок?»
        else if (containsAny(normalized, "о чём говорит блок", "что говорит блок", "шепни", "whisper", "о чём блок")) {
            target = WishIntent.Target.BLOCK_WHISPER;
            category = WishIntent.Category.PROVENANCE;
            detailParam = text;
        }
        // 9б. Склонности NPC: «сделай моба спокойным/деятельным»
        else if (containsAny(normalized, "сделай спокойным", "сделай мирным", "моба спокойным", "моба спокойн",
                "успокой моба", "сделай деятельным", "calm the mob", "make peaceful", "make active")) {
            target = WishIntent.Target.NPC_PERSONALITY;
            category = WishIntent.Category.CIVILIZATION;
            detailParam = text;
        }
        // 9в. Память предмета: «Что помнит этот предмет?»
        else if (containsAny(normalized, "что помнит предмет", "что помнит этот предмет", "память предмета",
                "item memory", "what does this item remember")) {
            target = WishIntent.Target.ITEM_MEMORY;
            category = WishIntent.Category.PROVENANCE;
            detailParam = text;
        }
        // 9г. Магическая фотография: «Сфотографируй это»
        else if (containsAny(normalized, "сфотограф", "фотограф", "сделай фото", "снимок", "photo", "photograph", "snapshot")) {
            target = WishIntent.Target.MAGIC_PHOTO;
            category = WishIntent.Category.MATERIAL;
            detailParam = text;
        }
        // 9д. Живые картины и зеркальный мир: «Войди в картину»
        else if (containsAny(normalized, "войди в картину", "живая картина", "живую картину", "в картину",
                "войди в зеркальный мир", "зеркальный мир", "enter the painting", "living painting", "mirror world")) {
            target = WishIntent.Target.LIVING_PAINTING;
            category = WishIntent.Category.CIVILIZATION;
            detailParam = text;
        }
        // 9е. Летающий дом: «Подними мой дом в небо»
        else if ((containsAny(normalized, "летающ") && containsAny(normalized, "дом"))
                || (containsAny(normalized, "flying") && containsAny(normalized, "house"))
                || (containsAny(normalized, "подними") && containsAny(normalized, "дом"))) {
            target = WishIntent.Target.FLYING_HOUSE;
            category = WishIntent.Category.CIVILIZATION;
            detailParam = text;
        }
        // 9ж. Магический двойник: «Создай моего двойника»
        else if (containsAny(normalized, "двойник", "клона", "клон", "копия меня", "двойника",
                "doppelganger", "clone me", "copy of me")) {
            target = WishIntent.Target.MAGIC_DOPPELGANGER;
            category = WishIntent.Category.CIVILIZATION;
            detailParam = text;
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
