package dev.romankrukovsky.kubanhorizons.genie.music;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Распознаёт танец игрока и держит активные песни мира.
 *
 * <p>Движения записываются в очередь из последних четырёх: прыжки приходят
 * из события {@code LivingJumpEvent}, а крадучийся шаг и спринт отслеживаются
 * по переходам состояния. Совпадение с фигурой танца очищает очередь и
 * возвращает песню; исполненная песня живёт свою длительность и каждый тик
 * применяет эффект к миру.</p>
 */
public final class DanceEngine {
    public enum Movement {
        SNEAK,
        JUMP,
        SPRINT
    }

    private static final int SEQUENCE_LENGTH = 4;
    private static final int GROWTH_MELODY_INTERVAL_TICKS = 20;

    private static final Map<UUID, Deque<Movement>> DANCES = new ConcurrentHashMap<>();
    private static final Map<UUID, ActiveSong> ACTIVE_SONGS = new ConcurrentHashMap<>();

    private record ActiveSong(MusicSpell spell, BlockPos center, int remainingTicks) {
        ActiveSong advance() {
            return new ActiveSong(spell, center, remainingTicks - 1);
        }
    }

    private DanceEngine() {
    }

    /** Добавляет движение в очередь игрока, оставляя последние четыре. */
    public static void record(ServerPlayer player, Movement movement) {
        Deque<Movement> deque = DANCES.computeIfAbsent(player.getUUID(), uuid -> new ArrayDeque<>());
        deque.addLast(movement);
        while (deque.size() > SEQUENCE_LENGTH) {
            deque.removeFirst();
        }
    }

    /**
     * Ищет фигуру танца в последних движениях игрока.
     *
     * <p>При совпадении очередь очищается: одна фигура даёт одну песню, и
     * продолжение того же танца не должно кастовать её снова.</p>
     */
    public static Optional<MusicSpell> detect(ServerPlayer player) {
        Deque<Movement> deque = DANCES.get(player.getUUID());
        if (deque == null || deque.size() < SEQUENCE_LENGTH) {
            return Optional.empty();
        }
        Movement[] sequence = deque.toArray(new Movement[0]);
        MusicSpell spell = match(sequence);
        if (spell != null) {
            deque.clear();
            return Optional.of(spell);
        }
        return Optional.empty();
    }

    /** Исполняет распознанную песню: звук, немедленный эффект и активная песня мира. */
    public static void cast(ServerLevel level, ServerPlayer player, MusicSpell spell) {
        BlockPos center = player.blockPosition();
        level.playSound(null, center, spell.sound(), SoundSource.PLAYERS, 1.0F, 1.0F);
        spell.apply(level, center, player);
        ACTIVE_SONGS.put(player.getUUID(), new ActiveSong(spell, center, spell.durationTicks()));
        player.sendSystemMessage(Component.translatable("wish.kubanhorizons.dance.triggered"), true);
    }

    /** Тикает активные песни: каждый тик применяет эффект, пока не истечёт длительность. */
    public static void tick(ServerLevel level) {
        for (Iterator<Map.Entry<UUID, ActiveSong>> iterator = ACTIVE_SONGS.entrySet().iterator();
                iterator.hasNext();) {
            Map.Entry<UUID, ActiveSong> entry = iterator.next();
            ActiveSong song = entry.getValue();
            if (song.remainingTicks() <= 0) {
                iterator.remove();
                continue;
            }
            ActiveSong next = song.advance();
            entry.setValue(next);
            ServerPlayer caster = level.getServer().getPlayerList().getPlayer(entry.getKey());
            if (caster == null) {
                iterator.remove();
                continue;
            }
            if (next.spell() == MusicSpell.GROWTH_MELODY
                    && next.remainingTicks() % GROWTH_MELODY_INTERVAL_TICKS != 0) {
                continue;
            }
            next.spell().apply(level, next.center(), caster);
        }
    }

    /** Очищает следы танца и активную песню игрока. */
    public static void reset(UUID playerId) {
        DANCES.remove(playerId);
        ACTIVE_SONGS.remove(playerId);
    }

    private static MusicSpell match(Movement[] sequence) {
        if (matches(sequence, Movement.SNEAK, Movement.JUMP, Movement.SNEAK, Movement.JUMP)) {
            return MusicSpell.RAIN_SONG;
        }
        if (matches(sequence, Movement.JUMP, Movement.SNEAK, Movement.JUMP, Movement.SNEAK)) {
            return MusicSpell.DANCE_OF_FIRE;
        }
        if (matches(sequence, Movement.SPRINT, Movement.SNEAK, Movement.SPRINT, Movement.SNEAK)) {
            return MusicSpell.PEACE_LULLABY;
        }
        if (matches(sequence, Movement.JUMP, Movement.JUMP, Movement.SNEAK, Movement.SPRINT)) {
            return MusicSpell.GROWTH_MELODY;
        }
        return null;
    }

    private static boolean matches(Movement[] sequence, Movement... pattern) {
        for (int i = 0; i < pattern.length; i++) {
            if (sequence[i] != pattern[i]) {
                return false;
            }
        }
        return true;
    }
}
