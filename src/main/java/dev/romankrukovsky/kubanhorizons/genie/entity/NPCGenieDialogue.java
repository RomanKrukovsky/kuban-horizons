package dev.romankrukovsky.kubanhorizons.genie.entity;

import dev.romankrukovsky.kubanhorizons.entity.KubanGenie;
import dev.romankrukovsky.kubanhorizons.genie.GeniePersonality;
import dev.romankrukovsky.kubanhorizons.genie.wish.WishIntent;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * NPCGenieDialogue — простая система диалогов и реакций NPC-джинний на желания.
 * Вызывается из пути исполнения желаний для NPC-джинний (гибридных видов).
 */
public final class NPCGenieDialogue {

    private NPCGenieDialogue() {
    }

    /**
     * Реакция NPC-джиннии на полученное желание.
     * @param genie NPC-джинния
     * @param owner владелец (игрок)
     * @param intent желание
     */
    public static void onWishReceived(KubanGenie genie, ServerPlayer owner, WishIntent intent) {
        if (genie == null || owner == null || intent == null) return;

        GeniePersonality p = genie.personality();
        String reaction = generateReaction(p, intent);
        owner.sendSystemMessage(Component.literal("[NPC Genie] " + reaction));

        // Дополнительно: можно обновлять personality на основе реакции (доверие, страх и т.д.)
        // p.observeWording(intent.polite(), intent.commanding(), intent.precision());
    }

    private static String generateReaction(GeniePersonality personality, WishIntent intent) {
        String temper = personality.temperament().name();
        boolean polite = intent.polite();
        boolean precise = intent.isPreciseAndSafe();

        if (polite && precise) {
            return "Как пожелаете, хозяин. (" + temper + ")";
        } else if (polite) {
            return "Я постараюсь, но будьте осторожны... (" + temper + ")";
        } else if (precise) {
            return "Желание принято. (" + temper + ")";
        } else {
            return "Это... рискованно. (" + temper + ")";
        }
    }
}
