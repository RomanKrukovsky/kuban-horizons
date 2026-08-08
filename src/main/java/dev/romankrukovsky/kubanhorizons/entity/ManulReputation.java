package dev.romankrukovsky.kubanhorizons.entity;

import dev.romankrukovsky.kubanhorizons.KubanHorizons;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.gossip.GossipType;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

import java.util.List;

/**
 * Репутация за убийство манула: кот станицы под защитой людей.
 *
 * <h2>Почему ванильные сплетни, а не своя система</h2>
 *
 * <p>В моде нет системы репутации, и заводить её ради одного существа —
 * ошибка. Ванильные {@code GossipContainer} жителей уже дают ровно то, что
 * нужно, и дают это <em>наблюдаемо</em>:</p>
 *
 * <ul>
 *   <li>цены у торговцев растут — {@code Villager.getPlayerReputation()}
 *       напрямую входит в расчёт скидок, так что наказание игрок видит
 *       в интерфейсе торговли, а не в скрытом счётчике;</li>
 *   <li>сплетня расходится по поселению сама — жители обмениваются
 *       {@code gossips.transferFrom} при встречах, поэтому «местное»
 *       осуждение получается без собственного кода распространения;</li>
 *   <li>она забывается — {@code decayPerDay} у {@code MINOR_NEGATIVE}
 *       гасит запись со временем: убийство не становится вечным клеймом,
 *       что соответствует «мягким штрафам» из GAME_DESIGN.md §3.</li>
 * </ul>
 *
 * <p>Своя привязка к игроку (attachment) была бы дешевле по коду, но
 * молча: число в NBT, которое ни на что не влияет, — это в точности тот
 * мёртвый код, из-за которого была удалена кавказская пчела. Ванильные
 * сплетни уже подключены к экономике, поэтому выбраны они.</p>
 *
 * <h2>Величина наказания</h2>
 *
 * <p>{@link GossipType#MINOR_NEGATIVE} (вес −1, до 200), а не
 * {@code MAJOR_NEGATIVE} (вес −5): манул — не житель. Приравнять его
 * убийство к убийству человека значило бы сломать шкалу, в которой
 * {@code MAJOR_NEGATIVE} зарезервирован за смертью жителя. Величина
 * {@link #GOSSIP_PENALTY} равна ванильной реакции на удар по жителю: кота
 * станицы обидели — это заметно, но это не убийство.</p>
 *
 * <p>Реагируют только те жители, которые <em>видят</em> смерть
 * ({@link #WITNESS_RADIUS}), как и в ванили для убийства жителя: репутация
 * должна падать за замеченное, а не за всё, что произошло в мире.</p>
 */
@EventBusSubscriber(modid = KubanHorizons.MOD_ID)
public final class ManulReputation {
    /**
     * Радиус, в котором житель считается свидетелем.
     *
     * <p>Ванильное убийство жителя оповещает свидетелей в радиусе 16 —
     * та же величина, чтобы «заметность» манула не отличалась от
     * привычной игроку.</p>
     */
    private static final double WITNESS_RADIUS = 16.0D;

    /**
     * Насколько падает репутация у каждого свидетеля.
     *
     * <p>25 — ванильная константа {@code REPUTATION_CHANGE_PER_EVENT}: то
     * же, что житель запоминает за удар по себе.</p>
     */
    private static final int GOSSIP_PENALTY = GossipType.REPUTATION_CHANGE_PER_EVENT;

    /** Сколько тиков житель показывает недовольство (ванильное значение — 40). */
    private static final int UNHAPPY_TICKS = 40;

    private ManulReputation() {
    }

    /**
     * Ловит смерть манула от руки игрока и портит ему репутацию у свидетелей.
     *
     * <p>{@link LivingDeathEvent}, а не собственный вызов из класса манула:
     * файл сущности принадлежит другому агенту, и хук в событии не требует
     * его правок. Побочная выгода — смерть от чужой руки (волк, стрела
     * скелета) сюда не попадает, потому что виновник берётся из
     * {@code DamageSource}.</p>
     */
    @SubscribeEvent
    public static void onManulDeath(LivingDeathEvent event) {
        LivingEntity dead = event.getEntity();
        if (!ManulWorldHooks.isManul(dead)) {
            return;
        }
        if (!(dead.level() instanceof ServerLevel level)) {
            return;
        }
        // Виновен тот, кто нанёс удар: косвенный урон (упал, утонул) не
        // должен превращаться в вину игрока, стоящего рядом.
        if (!(event.getSource().getEntity() instanceof ServerPlayer killer)) {
            return;
        }
        punish(level, killer, dead.blockPosition());
    }

    /**
     * Записывает сплетню всем жителям-свидетелям и отмечает их недовольство.
     *
     * @return число жителей, заметивших убийство (для тестов и отладки)
     */
    public static int punish(ServerLevel level, ServerPlayer killer, BlockPos where) {
        AABB witnessBox = new AABB(where).inflate(WITNESS_RADIUS);
        List<Villager> witnesses = level.getEntitiesOfClass(Villager.class, witnessBox,
                villager -> villager.isAlive() && !villager.isBaby());
        for (Villager villager : witnesses) {
            villager.getGossips().add(killer.getUUID(), GossipType.MINOR_NEGATIVE, GOSSIP_PENALTY);
            // Видимая реакция: житель сердится и хмыкает. Без неё падение
            // репутации осталось бы невидимым до следующей торговли.
            villager.setUnhappyCounter(UNHAPPY_TICKS);
            level.playSound(null, villager.blockPosition(), SoundEvents.VILLAGER_NO,
                    SoundSource.NEUTRAL, 1.0F, 1.0F);
        }
        if (!witnesses.isEmpty()) {
            // Прямая обратная связь игроку: иначе он не свяжет выросшие
            // цены с котом, которого убил десять минут назад. overlay=true —
            // строка над горячей панелью, как ванильные подсказки.
            killer.sendSystemMessage(
                    Component.translatable("message.kubanhorizons.manul.killed_witnessed"), true);
        }
        return witnesses.size();
    }
}
