package dev.romankrukovsky.kubanhorizons.genie.vessel;

import dev.romankrukovsky.kubanhorizons.genie.player.PlayerGenieAttachment;
import dev.romankrukovsky.kubanhorizons.registry.KHAttachments;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

/**
 * Поводок между хвостом игрока-джиннии и его сосудом.
 *
 * <p>Заменяет таймер. Концепт прямо отказался от «обычного мага с двадцатью
 * единицами маны», поэтому приближение затягивания показывается натяжением
 * хвоста, а не полоской и не цифрой: кончик разворачивается к сосуду, сегменты
 * вытягиваются, хвост выпрямляется в струну, и только потом затягивание.</p>
 *
 * <p>Из этого следует то, чего у таймера быть не могло: <b>расстояние до сосуда
 * влияет на скорость</b>. Сосуд, унесённый далеко, натягивает хвост сильнее,
 * поэтому прятать его — не суеверие, а игровое решение с последствиями. Сосуд,
 * который вообще не найден в радиусе, натяжения не даёт: спрятать по-настоящему
 * далеко значит выиграть время.</p>
 */
public final class VesselLeash {
    /** Раз в секунду: чаще незачем, реже игрок не заметит смены стадии. */
    public static final int CHECK_INTERVAL_TICKS = 20;

    /** Дальше этого расстояния хвост считается натянутым до предела. */
    private static final double FULL_TENSION_DISTANCE = 64.0D;

    /** Ближе этого расстояния натяжения нет вовсе: сосуд под рукой. */
    private static final double SLACK_DISTANCE = 8.0D;

    private VesselLeash() {
    }

    /**
     * Четыре стадии натяжения — то, что игрок читает с экрана вместо цифр.
     *
     * <p>Порядок объявления совпадает с ростом натяжения, поэтому
     * {@code ordinal()} годится для сравнения «стало хуже».</p>
     */
    public enum Tension {
        /** Провис: хвост висит свободно, сосуд рядом. */
        SLACK,
        /** Кончик развернулся в сторону сосуда. */
        TURNED,
        /** Сегменты вытянулись, провис исчез. */
        STRETCHED,
        /** Струна: двигаться тяжело, затягивание близко. */
        TAUT
    }

    /**
     * Считает натяжение по расстоянию до сосуда.
     *
     * <p>{@code null} на входе — сосуд не найден в радиусе — означает провис, а
     * не максимум: ненайденный сосуд не тянет. Это и есть награда за то, что
     * его хорошо спрятали.</p>
     */
    public static Tension tensionFor(VesselTracker.Located vessel, Vec3 playerPos) {
        if (vessel == null) {
            return Tension.SLACK;
        }
        double distance = vessel.position().distanceTo(playerPos);
        if (distance <= SLACK_DISTANCE) {
            return Tension.SLACK;
        }
        double span = FULL_TENSION_DISTANCE - SLACK_DISTANCE;
        double ratio = Math.min(1.0D, (distance - SLACK_DISTANCE) / span);
        if (ratio < 0.34D) {
            return Tension.TURNED;
        }
        return ratio < 0.75D ? Tension.STRETCHED : Tension.TAUT;
    }

    /**
     * Обновляет натяжение одного игрока и рисует его.
     *
     * <p>Сообщение отправляется только при смене стадии: натяжение — состояние,
     * а не поток событий, и строка в чат каждую секунду превратила бы драму в
     * спам. Частицы, наоборот, идут постоянно от натянутых стадий — они и есть
     * непрерывная обратная связь.</p>
     *
     * @return текущая стадия натяжения
     */
    public static Tension tick(ServerLevel level, ServerPlayer player) {
        PlayerGenieAttachment attachment = player.getData(KHAttachments.PLAYER_GENIE_DATA);
        if (!attachment.isGenie() || VesselConfinement.isConfined(player)) {
            return Tension.SLACK;
        }

        VesselTracker.Located vessel = VesselTracker.findFor(player);
        Tension tension = tensionFor(vessel, player.position());

        if (tension != Tension.SLACK && vessel != null) {
            drawTether(level, player.position(), vessel.position(), tension);
        }
        return tension;
    }

    /**
     * Рисует нить от игрока к сосуду.
     *
     * <p>Частиц тем больше, чем выше натяжение: на {@code TURNED} видна лишь
     * пара искр у самого хвоста, на {@code TAUT} — сплошная линия до сосуда.
     * Так игрок понимает направление и силу одним взглядом, без интерфейса.</p>
     */
    private static void drawTether(ServerLevel level, Vec3 from, Vec3 to, Tension tension) {
        // Только часть пути на слабых стадиях: нить «прорастает» к сосуду.
        double reach = switch (tension) {
            case SLACK -> 0.0D;
            case TURNED -> 0.25D;
            case STRETCHED -> 0.6D;
            case TAUT -> 1.0D;
        };
        Vec3 tailBase = from.add(0.0D, 0.4D, 0.0D);
        Vec3 path = to.subtract(tailBase).scale(reach);
        int steps = Math.max(2, (int) (path.length() * 1.5D));

        for (int i = 0; i <= steps; i++) {
            Vec3 point = tailBase.add(path.scale((double) i / steps));
            level.sendParticles(
                    tension == Tension.TAUT ? ParticleTypes.SOUL_FIRE_FLAME : ParticleTypes.PORTAL,
                    point.x, point.y, point.z, 1, 0.02D, 0.02D, 0.02D, 0.0D);
        }
    }

    /** Сообщение о смене стадии; вызывается тем, кто хранит прошлую стадию. */
    public static void announce(ServerPlayer player, Tension tension) {
        String key = switch (tension) {
            case SLACK -> "message.kubanhorizons.genie.leash.slack";
            case TURNED -> "message.kubanhorizons.genie.leash.turned";
            case STRETCHED -> "message.kubanhorizons.genie.leash.stretched";
            case TAUT -> "message.kubanhorizons.genie.leash.taut";
        };
        player.sendSystemMessage(Component.translatable(key));
    }
}
