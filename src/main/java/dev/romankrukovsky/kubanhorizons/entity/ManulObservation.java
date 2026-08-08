package dev.romankrukovsky.kubanhorizons.entity;

import dev.romankrukovsky.kubanhorizons.KubanHorizons;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Наблюдение за диким манулом: «смотри, но не подходи».
 *
 * <h2>Что именно измеряется</h2>
 *
 * <p>Достижение «Не трогай кота» требует выдержки, а не действия, поэтому
 * здесь копится счётчик тиков, в течение которых одновременно верно:</p>
 *
 * <ul>
 *   <li>манул <em>дикий</em> — доверие {@link ManulTrust#WILD}: наблюдать
 *       за уже прикормленным зверем не подвиг;</li>
 *   <li>игрок <em>видит</em> его — расстояние в кольце
 *       {@link #MIN_DISTANCE}..{@link #MAX_DISTANCE}: ближе — это уже
 *       приближение, дальше — зверя не разглядеть;</li>
 *   <li>игрок <em>не бежит</em> на него ({@link Manul#isRushing}) —
 *       спокойное присутствие, а не погоня.</li>
 * </ul>
 *
 * <p>Нарушение любого условия сбрасывает счётчик: иначе достижение
 * набиралось бы случайно за несколько сессий, и «выдержка» перестала бы
 * что-либо значить.</p>
 *
 * <h2>Почему тик игрока, а не тик манула</h2>
 *
 * <p>Наблюдение — свойство игрока, и счётчик должен жить, даже если зверь
 * ненадолго вышел из загруженной зоны. Проверка идёт раз в
 * {@link #CHECK_INTERVAL} тиков (не каждый тик) — этого достаточно для
 * порога в полторы минуты и не создаёт нагрузки на сервер, как того требует
 * «сервер прежде всего» из GAME_DESIGN.md §3.</p>
 *
 * <p>Счётчики намеренно не сериализуются: наблюдение — это один непрерывный
 * эпизод, и переживать выход из мира ему незачем. Запись из карты удаляется
 * при выходе игрока, поэтому утечки нет.</p>
 */
@EventBusSubscriber(modid = KubanHorizons.MOD_ID)
public final class ManulObservation {
    /** Как часто проверяется наблюдение (в тиках). */
    private static final int CHECK_INTERVAL = 20;

    /**
     * Сколько тиков наблюдения нужно для достижения.
     *
     * <p>1800 тиков — полторы игровые минуты: достаточно долго, чтобы это
     * было решением «посидеть и посмотреть», и достаточно коротко, чтобы
     * уложиться в одну встречу со зверем.</p>
     */
    private static final int REQUIRED_TICKS = 1800;

    /** Ближе этого — это уже приближение, а не наблюдение. */
    private static final double MIN_DISTANCE = 6.0D;

    /** Дальше этого манула попросту не видно. */
    private static final double MAX_DISTANCE = 24.0D;

    /**
     * Накопленные тики наблюдения по игроку.
     *
     * <p>{@link ConcurrentHashMap}, потому что выход игрока обрабатывается
     * не обязательно в том же потоке, что и тик уровня.</p>
     */
    private static final Map<UUID, Integer> WATCHED_TICKS = new ConcurrentHashMap<>();

    private ManulObservation() {
    }

    /**
     * Копит выдержку наблюдения и выдаёт секретное достижение за серебристого
     * манула.
     *
     * <p>Серебристый окрас проверяется здесь же, а не отдельным событием:
     * обе проверки нужны в один момент — когда игрок смотрит на зверя.</p>
     */
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        if (player.tickCount % CHECK_INTERVAL != 0) {
            return;
        }

        AABB area = player.getBoundingBox().inflate(MAX_DISTANCE);
        List<Manul> visible = level.getEntitiesOfClass(Manul.class, area, Manul::isAlive);
        if (visible.isEmpty()) {
            WATCHED_TICKS.remove(player.getUUID());
            return;
        }

        // Секретное «Кубанский»: серебристый окрас — самый редкий, и его
        // достаточно просто увидеть рядом.
        for (Manul manul : visible) {
            if (manul.coat() == ManulCoat.SILVER
                    && player.distanceToSqr(manul) <= MAX_DISTANCE * MAX_DISTANCE) {
                ManulCriteria.MANUL_SILVER.get().trigger(player);
                break;
            }
        }

        if (Manul.isRushing(player) || !hasCalmWildManul(player, visible)) {
            WATCHED_TICKS.remove(player.getUUID());
            return;
        }

        int watched = WATCHED_TICKS.merge(player.getUUID(), CHECK_INTERVAL, Integer::sum);
        if (watched >= REQUIRED_TICKS) {
            ManulCriteria.MANUL_OBSERVED.get().trigger(player);
            WATCHED_TICKS.remove(player.getUUID());
        }
    }

    /** Есть ли в поле зрения дикий манул на «уважительной» дистанции. */
    private static boolean hasCalmWildManul(ServerPlayer player, List<Manul> visible) {
        double minSqr = MIN_DISTANCE * MIN_DISTANCE;
        double maxSqr = MAX_DISTANCE * MAX_DISTANCE;
        for (Manul manul : visible) {
            double distanceSqr = player.distanceToSqr(manul);
            if (manul.trust() == ManulTrust.WILD
                    && distanceSqr >= minSqr && distanceSqr <= maxSqr) {
                return true;
            }
        }
        return false;
    }

    /** Забывает счётчик при выходе игрока, чтобы карта не росла бесконечно. */
    @SubscribeEvent
    public static void onPlayerLogout(
            net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent event) {
        WATCHED_TICKS.remove(event.getEntity().getUUID());
    }

    /** Текущий прогресс наблюдения — для GameTest. */
    public static int watchedTicks(ServerPlayer player) {
        return WATCHED_TICKS.getOrDefault(player.getUUID(), 0);
    }

    /** Сбрасывает состояние — для GameTest, чтобы тесты не влияли друг на друга. */
    public static void reset() {
        WATCHED_TICKS.clear();
    }
}
