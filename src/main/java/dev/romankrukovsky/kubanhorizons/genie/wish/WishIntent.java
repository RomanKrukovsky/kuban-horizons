package dev.romankrukovsky.kubanhorizons.genie.wish;

/** Намерение желания, разобранного локальным парсером или LLM. */
public record WishIntent(Target target, Amount amount, Placement placement, Category category,
        boolean safe, boolean polite, boolean commanding, int precision, String detailParam) {

    public enum Category {
        MATERIAL,
        GIGANTISM,
        META_RULE,
        MOB_WISH,
        CIVILIZATION,
        MUSIC,
        DISTORTED_HIGHER_WISH,
        PROVENANCE,
        HISTORY,
        UNKNOWN
    }

    public enum Target {
        DIAMONDS,
        BIG_PIE,
        BIG_CHICKEN,
        BIG_BED,
        META_LONGER_NIGHT,
        META_NO_CREEPER_DAMAGE,
        META_INSTANT_SMELT,
        MOB_WISH_COW,
        MOB_WISH_WOLF,
        MOB_WISH_CREEPER,
        MOB_WISH_GOLEM,
        VILLAGE_WEALTH,
        GENIE_FESTIVAL,
        FLYING_HOUSE,
        OMNIPOTENCE,
        ETERNITY,
        SELF_FULFILLMENT,
        MIRROR_TRANSFORMATION,
        POWER_TRANSFER,
        ABS_INTANGIBILITY,
        PROVENANCE_QUERY,
        WHAT_IF,
        THEATER_REENACTMENT,
        WORD_MATERIALIZATION,
        DRAWING,
        BIOME_REWRITE,
        BLOCK_WHISPER,
        NPC_PERSONALITY,
        ITEM_MEMORY,
        MAGIC_PHOTO,
        LIVING_PAINTING,
        MUSIC_SPELL,
        LLM_DELEGATED,
        UNKNOWN
    }

    public enum Amount {
        CHEST,
        STACK,
        MANY,
        GIANT,
        UNSPECIFIED
    }

    public enum Placement {
        IN_FRONT,
        AT_FEET,
        ABOVE,
        UNSPECIFIED
    }

    public boolean understood() {
        return target != Target.UNKNOWN && target != Target.LLM_DELEGATED;
    }

    public boolean isPreciseAndSafe() {
        return safe && (amount == Amount.CHEST || amount == Amount.GIANT) && (placement == Placement.IN_FRONT || placement == Placement.UNSPECIFIED);
    }
}
