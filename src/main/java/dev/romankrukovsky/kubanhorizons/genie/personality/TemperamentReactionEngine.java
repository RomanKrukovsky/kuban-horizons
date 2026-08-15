package dev.romankrukovsky.kubanhorizons.genie.personality;

import dev.romankrukovsky.kubanhorizons.genie.GeniePersonality;
import dev.romankrukovsky.kubanhorizons.genie.GenieTemperament;
import dev.romankrukovsky.kubanhorizons.genie.wish.ParsedWish;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * Decides how the genie reacts to a wish based on current temperament.
 */
public final class TemperamentReactionEngine {

    public static Reaction decideReaction(ParsedWish wish, GeniePersonality personality) {
        GenieTemperament temp = personality.temperament();

        return switch (temp) {
            case KIND -> Reaction.EXECUTE_HONESTLY;
            case PROUD -> Reaction.EXECUTE_WITH_COMMENT;
            case GUARDED -> Reaction.WARN_THEN_EXECUTE;
            case SARDONIC -> Reaction.LITERAL_INTERPRETATION;
            case CUNNING -> Reaction.FIND_LOOPHOLE;
            case DANGEROUS -> Reaction.REFUSE_OR_DEMAND_PRICE;
        };
    }

    public static Component getWarningMessage(Reaction reaction, ParsedWish wish) {
        return switch (reaction) {
            case EXECUTE_HONESTLY -> Component.literal("Как пожелаешь...");
            case EXECUTE_WITH_COMMENT -> Component.literal("Очень... точная формулировка.");
            case WARN_THEN_EXECUTE -> Component.literal("Ты уверен? Это может иметь последствия.");
            case LITERAL_INTERPRETATION -> Component.literal("Как скажешь. Буквально.");
            case FIND_LOOPHOLE -> Component.literal("...Как пожелаешь.");
            case REFUSE_OR_DEMAND_PRICE -> Component.literal("За это придётся заплатить.");
        };
    }

    public enum Reaction {
        EXECUTE_HONESTLY,
        EXECUTE_WITH_COMMENT,
        WARN_THEN_EXECUTE,
        LITERAL_INTERPRETATION,
        FIND_LOOPHOLE,
        REFUSE_OR_DEMAND_PRICE
    }
}
