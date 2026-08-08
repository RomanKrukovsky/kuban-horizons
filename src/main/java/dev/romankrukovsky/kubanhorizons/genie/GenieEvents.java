package dev.romankrukovsky.kubanhorizons.genie;

import dev.romankrukovsky.kubanhorizons.KubanHorizons;
import dev.romankrukovsky.kubanhorizons.genie.defense.PhantomDeathController;
import dev.romankrukovsky.kubanhorizons.genie.defense.WishborneDefenseHandler;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/** Подписка на серверные события для отсчёта иронической защиты и ложной смерти. */
@EventBusSubscriber(modid = KubanHorizons.MOD_ID)
public final class GenieEvents {
    private GenieEvents() {
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (event.getLevel() instanceof ServerLevel level) {
            WishborneDefenseHandler.tickServer(level);
            PhantomDeathController.tickServer(level);
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
