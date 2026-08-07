package dev.romankrukovsky.kubanhorizons.weather;

import dev.romankrukovsky.kubanhorizons.KubanHorizons;
import dev.romankrukovsky.kubanhorizons.config.KHServerConfig;
import dev.romankrukovsky.kubanhorizons.entity.Locust;
import dev.romankrukovsky.kubanhorizons.registry.KHEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/**
 * Планировщик давления: налёты саранчи, суховей и половодье.
 *
 * <p>Всё давление тикает здесь, а не в сущностях и блоках, по двум причинам:
 * бюджет (один проход по игрокам раз в {@link #CHECK_INTERVAL} тиков вместо
 * per-block тикеров) и предсказуемость — один выключатель конфига гасит все
 * события сразу.</p>
 *
 * <p>События привязаны к игрокам, а не к миру: давление возникает там, где есть
 * кому его заметить, и не тратит тик на пустые чанки.</p>
 */
@EventBusSubscriber(modid = KubanHorizons.MOD_ID)
public final class PressureScheduler {
    /** Как часто проверяются условия событий. */
    private static final int CHECK_INTERVAL = 200;
    /** Сколько особей в налёте. */
    private static final int SWARM_SIZE = 7;
    /** Радиус появления роя вокруг игрока. */
    private static final int SWARM_RADIUS = 12;

    private PressureScheduler() {
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        if (!KHServerConfig.pressureEnabled() || KHServerConfig.pressureSeverity() <= 0.0D) {
            return;
        }
        if (level.getGameTime() % CHECK_INTERVAL != 0L) {
            return;
        }
        for (ServerPlayer player : level.players()) {
            if (player.isSpectator() || player.isCreative()) {
                continue;
            }
            maybeSwarm(level, player);
            maybeDryWind(level, player);
            maybeFlood(level, player);
        }
    }

    /** Налёт саранчи: редкое событие, шанс масштабируется интервалом конфига. */
    private static void maybeSwarm(ServerLevel level, ServerPlayer player) {
        if (!KHServerConfig.locustSwarmsEnabled()) {
            return;
        }
        // Дневное событие: саранча летает по теплу.
        if (!level.isBrightOutside()) {
            return;
        }
        int interval = Math.max(CHECK_INTERVAL, KHServerConfig.locustSwarmInterval());
        double chance = (double) CHECK_INTERVAL / interval * KHServerConfig.pressureSeverity();
        if (level.getRandom().nextDouble() >= chance) {
            return;
        }
        spawnSwarm(level, player);
    }

    /** Создаёт рой над открытым местом рядом с игроком. */
    private static void spawnSwarm(ServerLevel level, ServerPlayer player) {
        BlockPos origin = player.blockPosition();
        int spawned = 0;
        for (int attempt = 0; attempt < SWARM_SIZE * 3 && spawned < SWARM_SIZE; attempt++) {
            int dx = level.getRandom().nextInt(SWARM_RADIUS * 2 + 1) - SWARM_RADIUS;
            int dz = level.getRandom().nextInt(SWARM_RADIUS * 2 + 1) - SWARM_RADIUS;
            BlockPos ground = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    origin.offset(dx, 0, dz));
            BlockPos at = ground.above(2);
            if (!level.getBlockState(at).isAir()) {
                continue;
            }
            Locust locust = KHEntities.LOCUST.get().create(level, EntitySpawnReason.EVENT);
            if (locust == null) {
                return;
            }
            locust.snapTo(at.getX() + 0.5D, at.getY(), at.getZ() + 0.5D,
                    level.getRandom().nextFloat() * 360.0F, 0.0F);
            level.addFreshEntity(locust);
            spawned++;
        }
    }

    /** Суховей: ясная сухая погода в жарком биоме. */
    private static void maybeDryWind(ServerLevel level, ServerPlayer player) {
        if (!KHServerConfig.dryWindEnabled() || level.isRaining()) {
            return;
        }
        if (level.getRandom().nextDouble() >= 0.12D * KHServerConfig.pressureSeverity()) {
            return;
        }
        DryWind.blow(level, player);
    }

    /** Половодье: только в дождь и только в пойме — стихия по географии. */
    private static void maybeFlood(ServerLevel level, ServerPlayer player) {
        if (!KHServerConfig.floodingEnabled() || !level.isRaining()) {
            return;
        }
        if (level.getRandom().nextDouble() >= 0.10D * KHServerConfig.pressureSeverity()) {
            return;
        }
        Flooding.rise(level, player);
    }
}
