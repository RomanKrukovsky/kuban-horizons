package dev.romankrukovsky.kubanhorizons.genie;

import dev.romankrukovsky.kubanhorizons.KubanHorizons;
import dev.romankrukovsky.kubanhorizons.entity.KubanGenie;
import dev.romankrukovsky.kubanhorizons.genie.defense.WishborneDefenseHandler;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/** Подписка на серверные события джиннии: единственность, поводок, защита, превращение игрока. */
@EventBusSubscriber(modid = KubanHorizons.MOD_ID)
public final class GenieEvents {
    private GenieEvents() {
    }

    /**
     * Не пускает в мир вторую джиннию.
     *
     * <p>Отмена входа означает, что сущность не добавляется в мир вообще: не
     * тикает, не рендерится и не сохраняется. Поэтому {@code /summon} второй
     * джиннии тихо ничего не даёт вместо создания второй личности.</p>
     */
    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (event.getEntity() instanceof KubanGenie genie
                && event.getLevel() instanceof ServerLevel level
                && !GenieAnchor.admit(genie, level)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (event.getLevel() instanceof ServerLevel level) {
            WishborneDefenseHandler.tickServer(level);
            GenieLeash.tickServer(level);
            for (var player : level.players()) {
                dev.romankrukovsky.kubanhorizons.genie.player.PlayerGenieTransformationController
                        .tickTransformation(level, player);
            }
        }
    }

    @SubscribeEvent
    public static void onLivingIncomingDamage(net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent event) {
        if (dev.romankrukovsky.kubanhorizons.genie.player.PlayerGenieTransformationController.handleGenieDamage(
                event.getEntity(), event.getSource(), event.getAmount())) {
            event.setCanceled(true);
        }
        if (event.getEntity() instanceof net.minecraft.server.level.ServerPlayer player
                && event.getAmount() >= player.getHealth()) {
            var genies = player.level().getEntities(
                    net.minecraft.world.level.entity.EntityTypeTest.forClass(
                            dev.romankrukovsky.kubanhorizons.entity.KubanGenie.class),
                    player.getBoundingBox().inflate(32.0D), genie -> genie.isOwnedBy(player));
            if (!genies.isEmpty()
                    && dev.romankrukovsky.kubanhorizons.genie.vessel.OwnerDeathProtocol
                            .rescueNow(genies.getFirst(), player.level(), player)) {
                event.setCanceled(true);
            }
        }
    }
}
