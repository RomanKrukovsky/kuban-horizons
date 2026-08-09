package dev.romankrukovsky.kubanhorizons.genie.vessel;

import dev.romankrukovsky.kubanhorizons.entity.KubanGenie;
import dev.romankrukovsky.kubanhorizons.genie.GenieTemperament;
import dev.romankrukovsky.kubanhorizons.genie.player.PlayerGenieAttachment;
import dev.romankrukovsky.kubanhorizons.registry.KHAttachments;
import net.minecraft.util.RandomSource;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.entity.EntityTypeTest;

/**
 * Когда сосуд решает затянуть игрока обратно.
 *
 * <p>Время случайно, но <b>внутри окна</b>, а не из воздуха. Чистая случайность
 * игроками не прощается: два одинаковых прогона дают разный исход, и ощущение
 * не «я рискнул», а «не повезло». Здесь она допустима ровно потому, что
 * натяжение хвоста видно заранее — это не лотерея, а гроза на горизонте.</p>
 *
 * <p>Ширина окна выводится из искажения игрока: чем больше жестоких и крупных
 * желаний он себе позволил, тем короче окно. Так цена всемогущества становится
 * следствием поведения, а не броском кубика.</p>
 */
public final class VesselPull {
    /** Окно при нулевом искажении: примерно двадцать игровых минут. */
    private static final long BASE_WINDOW_TICKS = 24_000L;

    /** Окно при полном искажении: примерно две минуты. */
    private static final long MIN_WINDOW_TICKS = 2_400L;

    /** Разброс внутри окна: до четверти его длины в обе стороны. */
    private static final double JITTER = 0.25D;

    private VesselPull() {
    }

    /**
     * Длина окна для текущего искажения.
     *
     * <p>Линейно, а не по кривой: игрок должен уметь предсказать, что второе
     * жестокое желание стоит примерно столько же, сколько первое. Кривая
     * читалась бы как каприз.</p>
     */
    public static long windowFor(int corruption) {
        double ratio = Math.clamp(corruption, 0, 100) / 100.0D;
        return Math.round(BASE_WINDOW_TICKS - (BASE_WINDOW_TICKS - MIN_WINDOW_TICKS) * ratio);
    }

    /**
     * Назначает следующий момент затягивания.
     *
     * <p>Хранится в {@code nextTransformationTick} — то же поле, которым
     * контроллер превращения ведёт свою сцену. Конфликта нет: превращение к
     * этому моменту завершено и поле свободно, а второе поле того же смысла
     * означало бы два источника правды об одном и том же.</p>
     */
    public static void schedule(ServerLevel level, ServerPlayer player, RandomSource random) {
        PlayerGenieAttachment attachment = player.getData(KHAttachments.PLAYER_GENIE_DATA);
        long window = windowFor(attachment.getCorruption());
        // Разброс в обе стороны, поэтому назначенный момент не вычисляется
        // игроком с секундомером, но и не уезжает за пределы окна.
        double jitter = 1.0D + (random.nextDouble() * 2.0D - 1.0D) * JITTER;
        long at = level.getGameTime() + Math.max(1L, Math.round(window * jitter));
        attachment.setNextTransformationTick(at);
    }

    /** Пора ли затягивать. */
    public static boolean isDue(ServerLevel level, ServerPlayer player) {
        PlayerGenieAttachment attachment = player.getData(KHAttachments.PLAYER_GENIE_DATA);
        long at = attachment.getNextTransformationTick();
        return at > 0L && level.getGameTime() >= at;
    }

    /**
     * Предупреждает игрока — или молчит, по темпераменту джиннии.
     *
     * <p>Решает она, а не скрытая карма, и это принципиально: «мод решил, что ты
     * плохой» игрок не принимает, «она решила, что тебе не стоит говорить» —
     * принимает, потому что сам эти отношения и построил.</p>
     *
     * <p>Мотив у молчания настоящий. Она полторы тысячи лет в сосуде; игрок,
     * который её боялся и приказывал, даёт ей повод промолчать и остаться на
     * свободе. Тот, кто отпускал её погулять, слышит предупреждение — ей не
     * хочется, чтобы он занял её место.</p>
     *
     * @return {@code true}, если предупреждение было сказано
     */
    public static boolean warnIfWilling(ServerLevel level, ServerPlayer player) {
        KubanGenie genie = nearestOwnGenie(level, player);
        if (genie == null) {
            // Джиннии рядом нет — предупредить некому. Тишина здесь не выбор
            // характера, а отсутствие собеседника, и это честный исход.
            return false;
        }
        GenieTemperament temperament = genie.personality().temperament();
        String key = switch (temperament) {
            case KIND, PROUD, GUARDED -> "message.kubanhorizons.genie.pull.warned";
            case SARDONIC, CUNNING -> "message.kubanhorizons.genie.pull.hinted";
            case DANGEROUS -> null;
        };
        if (key == null) {
            return false;
        }
        player.sendSystemMessage(Component.translatable(key));
        return true;
    }

    /** Ближайшая джинния, принадлежащая этому игроку, либо {@code null}. */
    private static KubanGenie nearestOwnGenie(ServerLevel level, ServerPlayer player) {
        var found = level.getEntities(
                EntityTypeTest.forClass(KubanGenie.class),
                player.getBoundingBox().inflate(64.0D),
                candidate -> candidate.isOwnedBy(player));
        return found.isEmpty() ? null : found.getFirst();
    }
}
