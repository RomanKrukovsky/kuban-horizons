package dev.romankrukovsky.kubanhorizons.genie.vessel;

import dev.romankrukovsky.kubanhorizons.genie.player.PlayerGenieAttachment;
import dev.romankrukovsky.kubanhorizons.registry.KHAttachments;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/**
 * Закон сосуда: он теряет власть, когда им не пользуются.
 *
 * <p>Выход наружу стоит прогресса всемогущества, и цена <b>падает с тишиной</b>.
 * Сразу после затягивания уйти самому дорого; чем дольше сосуд никто не трёт,
 * тем дешевле, и в какой-то момент цена — ноль.</p>
 *
 * <p>Одно правило закрывает три задачи разом, и это причина его выбрать:</p>
 *
 * <ul>
 *   <li><b>одиночная игра</b> — там сосуд не трёт никто, поэтому цена сама
 *       сползает к нулю; отдельной ветки кода для сингла не нужно;</li>
 *   <li><b>закопать чужой сосуд в бедрок</b> — молчащий хозяин теряет слугу,
 *       поэтому равнодушие проигрышно;</li>
 *   <li><b>таймер освобождения</b> — не нужен: ожидание само является
 *       действием с последствием.</li>
 * </ul>
 *
 * <p>Правило симметрично и распространяется на неё саму: джинния, которую игрок
 * держал в лампе и не звал, имела те же основания уйти. Это объясняет, почему
 * она согласилась обменяться местами — она знала, как это работает, а игрок
 * нет.</p>
 */
public final class VesselLaw {
    /**
     * Полная цена выхода сразу после затягивания, в процентах прогресса.
     *
     * <p>Треть, а не всё: выход должен быть болезненным, но не обнулять путь —
     * иначе единственной разумной стратегией станет ждать любой ценой, и
     * механика выбора превратится в механику ожидания.</p>
     */
    private static final int FULL_COST = 33;

    /**
     * Тишина, после которой выход бесплатен: примерно сутки игрового времени.
     *
     * <p>Игровое время, а не системное: иначе выход из игры на неделю обнулял бы
     * риск реальным временем, ничего не сделав в мире.</p>
     */
    private static final long FREE_AFTER_TICKS = 24_000L;

    private VesselLaw() {
    }

    /**
     * Сколько прогресса стоит уйти самому прямо сейчас.
     *
     * <p>Линейно от {@link #FULL_COST} до нуля за {@link #FREE_AFTER_TICKS}
     * тишины. Отсчёт ведётся от последнего желания хозяина: желание — это и есть
     * пользование сосудом, поэтому оно обнуляет тишину.</p>
     */
    public static int costFor(ServerLevel level, ServerPlayer player) {
        PlayerGenieAttachment attachment = player.getData(KHAttachments.PLAYER_GENIE_DATA);
        long silence = silenceTicks(level, attachment);
        if (silence >= FREE_AFTER_TICKS) {
            return 0;
        }
        double remaining = 1.0D - (double) silence / FREE_AFTER_TICKS;
        return (int) Math.round(FULL_COST * remaining);
    }

    /** Как долго сосуд молчит. */
    public static long silenceTicks(ServerLevel level, PlayerGenieAttachment attachment) {
        long last = attachment.getLastWishTick();
        // Нулевой lastWishTick — сосуда никто не трогал ни разу, а не «трогали в
        // начале мира»: считать от нуля значило бы выдать бесплатный выход
        // сразу же тому, кого затянуло на первой минуте.
        if (last <= 0L) {
            return level.getGameTime();
        }
        return Math.max(0L, level.getGameTime() - last);
    }

    /**
     * Выйти самому, заплатив прогрессом.
     *
     * @return {@code false}, если игрок не внутри сосуда
     */
    public static boolean forfeit(ServerLevel level, ServerPlayer player) {
        if (!VesselConfinement.isConfined(player)) {
            player.sendSystemMessage(
                    Component.translatable("message.kubanhorizons.genie.law.not_confined"));
            return false;
        }
        PlayerGenieAttachment attachment = player.getData(KHAttachments.PLAYER_GENIE_DATA);
        int cost = costFor(level, player);

        attachment.setWishProgressPercent(attachment.getWishProgressPercent() - cost);
        // Хозяин теряет слугу вместе с выходом: связь держалась сосудом, а сосуд
        // только что признан заброшенным.
        attachment.setMasterUUID(null);

        VesselConfinement.release(player);
        player.sendSystemMessage(cost > 0
                ? Component.translatable("message.kubanhorizons.genie.law.forfeited", cost)
                : Component.translatable("message.kubanhorizons.genie.law.forfeited_free"));
        return true;
    }

    /**
     * Отмечает, что сосудом воспользовались.
     *
     * <p>Вызывается при исполнении желания хозяина: это обнуляет тишину и снова
     * делает выход дорогим. Отсюда гонка тактик — хозяину надо приказывать,
     * чтобы не потерять слугу, а слуге выгодно, чтобы хозяин молчал.</p>
     */
    public static void markUsed(ServerLevel level, ServerPlayer player) {
        player.getData(KHAttachments.PLAYER_GENIE_DATA).setLastWishTick(level.getGameTime());
    }
}
