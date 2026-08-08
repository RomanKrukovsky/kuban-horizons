package dev.romankrukovsky.kubanhorizons.entity;

import dev.romankrukovsky.kubanhorizons.KubanHorizons;
import dev.romankrukovsky.kubanhorizons.block.ManulShelterBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

import java.util.List;

/**
 * Связь манула с двором: расселение в укрытии и реплики жителей.
 *
 * <h2>Почему опрос, а не обратный вызов</h2>
 *
 * <p>Доверие манула растёт в двух местах его собственного класса (кормление
 * с руки и подбор корма с земли), и оба ведут в {@code acceptOffering()}.
 * Врезаться туда значило бы править файл сущности; вместо этого слой мира
 * раз в {@link #CHECK_INTERVAL} тиков смотрит на результат — доверие и
 * положение зверя. Опрос здесь дешевле связности: одна проверка на пять
 * секунд против правок в чужом классе.</p>
 *
 * <p>Проход идёт по <em>укрытиям рядом с игроками</em>, а не по всем
 * сущностям мира: без поставленного игроком укрытия расселяться некуда, и
 * пустой мир не стоит ни одного тика.</p>
 */
@EventBusSubscriber(modid = KubanHorizons.MOD_ID)
public final class ManulWorldEvents {
    /** Как часто проверяются расселение и реплики (в тиках). */
    private static final int CHECK_INTERVAL = 100;

    /**
     * Радиус, в котором манул считается «живущим» в укрытии.
     *
     * <p>8 блоков — двор, а не точка: зверь бродит вокруг лежанки, и
     * требовать стоять в блоке значило бы, что достижение выпадает
     * случайно.</p>
     */
    private static final int SETTLE_RADIUS = 8;

    /** В каком радиусе вокруг игрока искать укрытия. */
    private static final int SHELTER_SEARCH_RADIUS = 24;

    /** Радиус, в котором житель замечает манула и может обронить реплику. */
    private static final double VILLAGER_SIGHT = 12.0D;

    /**
     * Шанс реплики за проверку (1 к N).
     *
     * <p>Редкость — часть замысла: примета работает, только если её слышно
     * не каждый раз. При проверке раз в 5 секунд это в среднем одна реплика
     * на пару минут рядом с манулом.</p>
     */
    private static final int LINE_CHANCE = 24;

    /** Сколько всего вариантов легенд про манула переведено. */
    private static final int LINE_VARIANTS = 4;

    private ManulWorldEvents() {
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        if (level.getGameTime() % CHECK_INTERVAL != 0L) {
            return;
        }
        for (ServerPlayer player : level.players()) {
            checkSettlement(level, player);
            checkVillagerLines(level, player);
        }
    }

    /**
     * Ищет укрытия рядом с игроком и отмечает те, что манул считает домом.
     *
     * <p>Здесь же выдаются оба «мирных» достижения: доверие максимально —
     * «Манул тебя терпит», и зверь при этом живёт у укрытия игрока —
     * «Опора станицы». Проверка в одном месте гарантирует, что состояние
     * блока и состояние достижения не разойдутся.</p>
     */
    private static void checkSettlement(ServerLevel level, ServerPlayer player) {
        BlockPos center = player.blockPosition();
        for (BlockPos pos : BlockPos.betweenClosed(
                center.offset(-SHELTER_SEARCH_RADIUS, -SHELTER_SEARCH_RADIUS, -SHELTER_SEARCH_RADIUS),
                center.offset(SHELTER_SEARCH_RADIUS, SHELTER_SEARCH_RADIUS, SHELTER_SEARCH_RADIUS))) {
            if (!ManulWorldHooks.isShelter(level.getBlockState(pos))) {
                continue;
            }
            // immutable(): betweenClosed отдаёт переиспользуемый курсор,
            // а позиция уходит в setBlock и в лямбды.
            BlockPos shelter = pos.immutable();
            Manul resident = findResident(level, shelter, player);
            ManulShelterBlock.setOccupied(level, shelter, resident != null);
            if (resident != null) {
                ManulCriteria.MANUL_TRUSTED.get().trigger(player);
                ManulCriteria.MANUL_SETTLED.get().trigger(player);
            }
        }
    }

    /**
     * Манул, поселившийся в этом укрытии.
     *
     * <p>Условия жёсткие: максимум доверия и владелец — этот игрок. Иначе
     * «поселился» означало бы «пробежал мимо», и достижение потеряло бы
     * смысл, а укрытие выглядело бы занятым от любого дикого зверя.</p>
     */
    private static Manul findResident(ServerLevel level, BlockPos shelter, ServerPlayer player) {
        AABB area = new AABB(shelter).inflate(SETTLE_RADIUS);
        List<Manul> nearby = level.getEntitiesOfClass(Manul.class, area, Manul::isAlive);
        for (Manul manul : nearby) {
            if (!manul.trust().atLeast(ManulTrust.RESIDENT)) {
                continue;
            }
            EntityReference<LivingEntity> owner = manul.getOwnerReference();
            if (owner != null && player.getUUID().equals(owner.getUUID())) {
                return manul;
            }
        }
        return null;
    }

    /**
     * Реплики и приметы жителей о мануле.
     *
     * <p>Житель должен одновременно видеть и манула, и игрока — иначе
     * реплика прозвучала бы в пустоту. Текст уходит только тому игроку,
     * который рядом: это разговор, а не объявление на весь сервер.</p>
     */
    private static void checkVillagerLines(ServerLevel level, ServerPlayer player) {
        if (level.getRandom().nextInt(LINE_CHANCE) != 0) {
            return;
        }
        AABB area = player.getBoundingBox().inflate(VILLAGER_SIGHT);
        List<Manul> manuls = level.getEntitiesOfClass(Manul.class, area, Manul::isAlive);
        if (manuls.isEmpty()) {
            return;
        }
        List<Villager> villagers = level.getEntitiesOfClass(Villager.class, area,
                villager -> villager.isAlive() && !villager.isBaby());
        if (villagers.isEmpty()) {
            return;
        }
        Villager speaker = villagers.get(level.getRandom().nextInt(villagers.size()));
        // Хорошая примета: житель рад встрече, а не просто произносит текст.
        speaker.playSound(net.minecraft.sounds.SoundEvents.VILLAGER_YES, 0.8F, 1.0F);
        int variant = level.getRandom().nextInt(LINE_VARIANTS) + 1;
        player.sendSystemMessage(Component.translatable(
                "message.kubanhorizons.manul.legend." + variant), false);
    }
}
