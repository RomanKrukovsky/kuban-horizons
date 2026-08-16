package dev.romankrukovsky.kubanhorizons.genie;

import dev.romankrukovsky.kubanhorizons.genie.memory.WorldGenieMemory;
import net.minecraft.server.level.ServerLevel;

/**
 * Титулы мира (GENIE_VISION §Компаньон-RPG): джинния обретает титул по мере
 * дел в конкретном мире — исполненных желаний, спасений и спасённых поселений.
 *
 * <p>Титул — не статичное имя, а отражение истории мира: у каждой джиннии в
 * каждом мире свой титул, зависящий от того, что она здесь совершила.</p>
 */
public final class GenieTitleSystem {

    private GenieTitleSystem() {
    }

    /** Перевод ключ титула по прогрессу джиннии в этом мире. */
    public static String titleKey(ServerLevel level) {
        WorldGenieMemory memory = WorldGenieMemory.get(level);
        int wishes = memory.totalWishesGranted();
        int rescues = memory.totalRescuesPerformed();
        int villages = memory.savedVillagesCount();

        if (wishes >= 20 && villages >= 1 && rescues >= 5) {
            return "title.kubanhorizons.genie.steppe_mistress";
        }
        if (wishes >= 10 && rescues >= 10) {
            return "title.kubanhorizons.genie.last_wish";
        }
        if (rescues >= 10) {
            return "title.kubanhorizons.genie.she_who_saves";
        }
        if (villages >= 1) {
            return "title.kubanhorizons.genie.village_guardian";
        }
        if (wishes >= 10) {
            return "title.kubanhorizons.genie.lady_of_wishes";
        }
        if (rescues >= 3) {
            return "title.kubanhorizons.genie.keeper_of_kuban";
        }
        return "title.kubanhorizons.genie.lamp_warden";
    }

    /** Человекочитаемое описание текущего титула (для диалога). */
    public static String currentTitle(ServerLevel level) {
        return titleKey(level).replace("title.kubanhorizons.genie.", "");
    }
}