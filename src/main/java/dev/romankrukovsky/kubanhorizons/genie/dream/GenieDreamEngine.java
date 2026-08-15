package dev.romankrukovsky.kubanhorizons.genie.dream;

import dev.romankrukovsky.kubanhorizons.entity.KubanGenie;
import dev.romankrukovsky.kubanhorizons.genie.memory.UnfulfilledWishRoom;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.entity.EntityTypeTest;

import java.util.List;

/**
 * Движок снов Джиннии: пока игрок спит, джинния приходит во сне.
 *
 * <p>Сон — момент, когда джинния может без спешки напомнить о том, что осталось
 * неисполненным: невыполненные желания из {@link UnfulfilledWishRoom}. Если
 * желаний нет, джинния просто делится тёплым видением.</p>
 */
public final class GenieDreamEngine {
    private GenieDreamEngine() {
    }

    /** Вызывается при пробуждении игрока в кровати. */
    public static void onWake(ServerLevel level, ServerPlayer player) {
        List<KubanGenie> genies = level.getEntities(
                EntityTypeTest.forClass(KubanGenie.class),
                player.getBoundingBox().inflate(48.0D),
                genie -> genie.isOwnedBy(player));
        if (genies.isEmpty()) {
            return;
        }
        enterDreamState(genies.getFirst(), player);
    }

    public static void enterDreamState(KubanGenie genie, ServerPlayer player) {
        List<UnfulfilledWishRoom.UnfulfilledWish> pending =
                UnfulfilledWishRoom.get((ServerLevel) player.level())
                        .forOwner(player.getUUID())
                        .stream().filter(wish -> !wish.resolved()).toList();
        if (pending.isEmpty()) {
            player.sendSystemMessage(Component.translatable(
                    "message.kubanhorizons.genie.dream.vision"));
            return;
        }
        UnfulfilledWishRoom.UnfulfilledWish wish = pending.get(pending.size() - 1);
        player.sendSystemMessage(Component.translatable(
                "message.kubanhorizons.genie.dream.reminder", wish.wishText()));
    }
}