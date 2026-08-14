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
    public static void onLevelTickPre(LevelTickEvent.Pre event) {
        if (event.getLevel() instanceof ServerLevel level) {
            dev.romankrukovsky.kubanhorizons.genie.runtime.WishRuntime
                    .get(level.getServer()).prepareInstantSmelt(level);
        }
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (event.getLevel() instanceof ServerLevel level) {
            if (level.dimension().equals(
                    dev.romankrukovsky.kubanhorizons.worldgen.dimension.KHDimensions.ETERNAL_KUBAN)
                    || dev.romankrukovsky.kubanhorizons.worldgen.dimension.KHMagicDimensions
                            .isPocketDimension(level.dimension())) {
                level.setRainLevel(0.0F);
                level.setThunderLevel(0.0F);
            }
            WishborneDefenseHandler.tickServer(level);
            // Каждый тик, а не вместе с аурой: аура срабатывает раз в секунду,
            // и остановленные снаряды рассыпались бы с заметной задержкой.
            dev.romankrukovsky.kubanhorizons.genie.aura.GenieAuraOfLaws.tickHeldProjectiles(level);
            dev.romankrukovsky.kubanhorizons.genie.dimension.PocketSceneService.tick(level);
            GenieLeash.tickServer(level);
            for (var player : level.players()) {
                dev.romankrukovsky.kubanhorizons.genie.player.PlayerGenieTransformationController
                        .tickTransformation(level, player);
            }
            tickVesselLeash(level);
        }
    }

    /**
     * Натяжение хвоста к сосуду — раз в секунду, а не каждый тик.
     *
     * <p>Поиск сосуда обходит инвентари, предметы и контейнеры вокруг игрока,
     * поэтому шестьдесят раз в секунду он был бы дороже всего остального в этом
     * обработчике вместе. Стадия натяжения меняется медленно, и раз в секунду
     * игрок не замечает разницы.</p>
     *
     * <p>Прошлая стадия хранится здесь, а не в attachment: это кадр показа, а не
     * состояние персонажа — его не нужно ни сохранять в мир, ни синхронизировать.
     * Игрок, вышедший и вернувшийся, просто получит сообщение о текущей стадии
     * заново, что честнее, чем тишина.</p>
     */
    private static void tickVesselLeash(ServerLevel level) {
        if (level.getGameTime()
                % dev.romankrukovsky.kubanhorizons.genie.vessel.VesselLeash.CHECK_INTERVAL_TICKS != 0L) {
            return;
        }
        for (var player : level.players()) {
            var tension = dev.romankrukovsky.kubanhorizons.genie.vessel.VesselLeash.tick(level, player);
            var previous = LAST_TENSION.get(player.getUUID());
            if (previous != tension) {
                LAST_TENSION.put(player.getUUID(), tension);
                // Сообщение только при смене стадии: натяжение — состояние, а
                // строка в чат каждую секунду превратила бы драму в спам.
                if (tension != dev.romankrukovsky.kubanhorizons.genie.vessel.VesselLeash.Tension.SLACK
                        || previous != null) {
                    dev.romankrukovsky.kubanhorizons.genie.vessel.VesselLeash.announce(player, tension);
                }
                // Струна — последняя стадия перед затягиванием, и единственный
                // момент, когда предупреждение ещё имеет смысл.
                if (tension == dev.romankrukovsky.kubanhorizons.genie.vessel.VesselLeash.Tension.TAUT) {
                    dev.romankrukovsky.kubanhorizons.genie.vessel.VesselPull.warnIfWilling(level, player);
                }
            }
            tickPull(level, player);
        }
    }

    /**
     * Назначает и исполняет затягивание в сосуд.
     *
     * <p>Момент назначается один раз — при первом тике свободного игрока-джиннии
     * с ненулевым искажением. Нулевое искажение не назначает ничего: игрок,
     * который не злоупотреблял желаниями, живёт снаружи спокойно, и это то
     * различие, ради которого искажение вообще считается.</p>
     */
    private static void tickPull(ServerLevel level, net.minecraft.server.level.ServerPlayer player) {
        var attachment = player.getData(
                dev.romankrukovsky.kubanhorizons.registry.KHAttachments.PLAYER_GENIE_DATA);
        if (!attachment.isGenie()
                || dev.romankrukovsky.kubanhorizons.genie.vessel.VesselConfinement.isConfined(player)) {
            return;
        }
        if (attachment.getCorruption() <= 0) {
            return;
        }
        if (attachment.getNextTransformationTick() <= 0L) {
            dev.romankrukovsky.kubanhorizons.genie.vessel.VesselPull.schedule(
                    level, player, level.getRandom());
            return;
        }
        if (dev.romankrukovsky.kubanhorizons.genie.vessel.VesselPull.isDue(level, player)) {
            // Обнулить до затягивания, а не после: confine() телепортирует, и
            // повторный вызов на следующем тике назначил бы новое окно изнутри.
            attachment.setNextTransformationTick(0L);
            dev.romankrukovsky.kubanhorizons.genie.vessel.VesselConfinement.confine(player);
        }
    }

    /** Последняя показанная стадия натяжения на игрока. */
    private static final java.util.Map<java.util.UUID,
            dev.romankrukovsky.kubanhorizons.genie.vessel.VesselLeash.Tension> LAST_TENSION =
            new java.util.concurrent.ConcurrentHashMap<>();

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
