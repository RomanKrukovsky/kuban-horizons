package dev.romankrukovsky.kubanhorizons.genie;

import dev.romankrukovsky.kubanhorizons.KubanHorizons;
import dev.romankrukovsky.kubanhorizons.entity.KubanGenie;
import dev.romankrukovsky.kubanhorizons.genie.memory.GenieStateSnapshot;
import dev.romankrukovsky.kubanhorizons.genie.memory.WorldGenieMemory;
import dev.romankrukovsky.kubanhorizons.registry.KHEntities;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;

/**
 * Поводок джиннии: она никогда не теряет хозяина.
 *
 * <p>{@code FollowGenieOwnerGoal} держит спутницу рядом только пока она
 * прогружена и находится в том же измерении. Этого недостаточно: игрок может
 * телепортироваться за миллион блоков, уйти за барьер или в Нижний мир, и
 * тогда чанк джиннии выгружается, она перестаёт тикать и физически не может
 * себя догнать. Поводок работает от лица мира, а не от лица сущности:
 * находит джиннию по якорному UUID, при необходимости прогружает её чанк, при
 * необходимости переносит между измерениями, а если сущность действительно
 * уничтожена — воссоздаёт её из снимка личности.</p>
 *
 * <p>Телепорт не использует поиск пути и не проверяет препятствия на пути,
 * поэтому барьер, бедрок, Пустота и любое расстояние значения не имеют.</p>
 */
public final class GenieLeash {
    /** Раз в секунду: чаще незачем, реже игрок заметит отсутствие спутницы. */
    private static final int CHECK_INTERVAL_TICKS = 20;

    /** Дальше этого расстояния джиннию возвращают телепортом, а не полётом. */
    private static final double LEASH_DISTANCE = 32.0D;
    private static final double LEASH_DISTANCE_SQUARED = LEASH_DISTANCE * LEASH_DISTANCE;

    /** Радиус поиска свободной точки рядом с игроком. */
    private static final int MATERIALIZE_RADIUS = 3;

    private GenieLeash() {
    }

    public static void tickServer(ServerLevel level) {
        MinecraftServer server = level.getServer();
        // Проверка идёт только в оверворлде: якорь и снимок живут там, а
        // повторять один и тот же обход в каждом измерении незачем.
        if (level.dimension() != Level.OVERWORLD
                || level.getGameTime() % CHECK_INTERVAL_TICKS != 0L) {
            return;
        }

        UUID anchored = GenieAnchor.anchoredId(server);
        if (anchored == null) {
            return;
        }

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (player.isSpectator()) {
                continue;
            }
            KubanGenie genie = GenieAnchor.find(server);
            if (genie == null) {
                materializeFor(server, player);
                return;
            }
            if (!genie.isOwnedBy(player)) {
                continue;
            }
            rememberState(server, genie);
            keepNear(genie, player);
            return;
        }
    }

    /** Возвращает джиннию к хозяину, если она отстала или оказалась в другом измерении. */
    private static void keepNear(KubanGenie genie, ServerPlayer player) {
        // Явный приказ стоять важнее поводка: иначе режим STAY невозможно было
        // бы использовать, чтобы оставить джиннию охранять место.
        if (genie.brain().mode() == GenieBehaviorMode.STAY) {
            return;
        }
        if (!(player.level() instanceof ServerLevel playerLevel)) {
            return;
        }

        boolean otherDimension = genie.level().dimension() != playerLevel.dimension();
        if (!otherDimension && genie.distanceToSqr(player) <= LEASH_DISTANCE_SQUARED) {
            return;
        }

        Vec3 target = freeSpotNear(playerLevel, player);
        if (otherDimension) {
            // Кросс-дименсионный телепорт создаёт новый Java-объект, но
            // restoreFrom переносит UUID, поэтому якорь остаётся валидным.
            genie.teleport(new TeleportTransition(playerLevel, target, Vec3.ZERO,
                    player.getYRot(), 0.0F, Set.<Relative>of(), TeleportTransition.DO_NOTHING));
        } else {
            genie.snapTo(target.x(), target.y(), target.z(), player.getYRot(), 0.0F);
        }
        genie.getNavigation().stop();
        genie.playCast();
    }

    /**
     * Воссоздаёт джиннию рядом с игроком.
     *
     * <p>Срабатывает, когда сущность действительно перестала существовать:
     * например, мир загрузился из сохранения, где её чанк был повреждён.
     * Личность берётся из снимка, поэтому возвращается та же джинния с
     * накопленными отношениями, а не новая.</p>
     */
    private static void materializeFor(MinecraftServer server, ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        WorldGenieMemory memory = WorldGenieMemory.get(server.overworld());
        var snapshot = memory.anchoredGenieState();
        if (snapshot.isEmpty()) {
            // Джинния ещё ни разу не жила в этом мире; её нужно найти, а не
            // выдать бесплатно.
            return;
        }
        if (snapshot.get().owner().filter(owner -> owner.equals(player.getUUID())).isEmpty()) {
            return;
        }

        KubanGenie genie = KHEntities.KUBAN_GENIE.get().create(level, EntitySpawnReason.TRIGGERED);
        if (genie == null) {
            return;
        }
        Vec3 spot = freeSpotNear(level, player);
        genie.snapTo(spot.x(), spot.y(), spot.z(), player.getYRot(), 0.0F);
        genie.restoreFromSnapshot(snapshot.get(), level.registryAccess());

        // Якорь освобождается прямо перед добавлением: иначе обработчик входа
        // в мир отклонил бы новую сущность как «вторую джиннию».
        GenieAnchor.release(server);
        if (!level.addFreshEntity(genie)) {
            KubanHorizons.LOGGER.warn("Не удалось вернуть Кубанскую Джиннию к игроку {}.",
                    player.getGameProfile().name());
            return;
        }
        genie.playSpawn();
        KubanHorizons.LOGGER.debug("Кубанская Джинния восстановлена из снимка для {}.",
                player.getGameProfile().name());
    }

    private static void rememberState(MinecraftServer server, KubanGenie genie) {
        WorldGenieMemory.get(server.overworld()).rememberGenieState(genie.captureSnapshot());
    }

    /**
     * Ищет место рядом с игроком, где джинния не окажется внутри блоков.
     *
     * <p>Если свободного места нет вообще, джинния появляется над игроком:
     * висящая в воздухе спутница лучше застрявшей в бедроке.</p>
     */
    private static Vec3 freeSpotNear(ServerLevel level, ServerPlayer player) {
        BlockPos origin = player.blockPosition();
        for (int dy = 1; dy <= MATERIALIZE_RADIUS; dy++) {
            for (int dx = -MATERIALIZE_RADIUS; dx <= MATERIALIZE_RADIUS; dx++) {
                for (int dz = -MATERIALIZE_RADIUS; dz <= MATERIALIZE_RADIUS; dz++) {
                    BlockPos candidate = origin.offset(dx, dy, dz);
                    if (level.getBlockState(candidate).isAir()
                            && level.getBlockState(candidate.above()).isAir()) {
                        return new Vec3(candidate.getX() + 0.5D, candidate.getY(), candidate.getZ() + 0.5D);
                    }
                }
            }
        }
        return new Vec3(player.getX(), player.getY() + 2.0D, player.getZ());
    }
}
