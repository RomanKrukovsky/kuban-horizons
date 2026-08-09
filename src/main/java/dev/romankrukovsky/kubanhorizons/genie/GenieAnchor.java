package dev.romankrukovsky.kubanhorizons.genie;

import dev.romankrukovsky.kubanhorizons.KubanHorizons;
import dev.romankrukovsky.kubanhorizons.entity.KubanGenie;
import dev.romankrukovsky.kubanhorizons.genie.memory.WorldGenieMemory;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

/**
 * Единственность Кубанской Джиннии.
 *
 * <p>Джинния — не вид, а одна личность мира. Якорь хранит UUID той
 * сущности, которая ею является. Любая другая сущность того же типа не
 * допускается в мир вообще, поэтому {@code /summon} второй джиннии тихо
 * ничего не даёт, а не создаёт вторую личность с собственной памятью.</p>
 *
 * <p>Якорь лежит в памяти оверворлда, а не текущего измерения: джинния
 * одна на сервер целиком. Иначе переход в Нижний мир позволял бы завести
 * там вторую.</p>
 */
public final class GenieAnchor {
    private GenieAnchor() {
    }

    /** Память, в которой живёт якорь: всегда оверворлд, в каком бы мире мы ни были. */
    private static WorldGenieMemory memory(MinecraftServer server) {
        return WorldGenieMemory.get(server.overworld());
    }

    /**
     * Решает, допустить ли сущность в мир.
     *
     * <p>Совпадение UUID означает ту же самую джиннию: прогрузку чанка или
     * переход между измерениями. Кросс-дименсионный телепорт в 26.2 создаёт
     * новый Java-объект, но {@code restoreFrom} переносит UUID, поэтому
     * проверка по UUID переход не ломает.</p>
     *
     * @return true, если сущность является якорной джиннией мира
     */
    public static boolean admit(KubanGenie genie, ServerLevel level) {
        WorldGenieMemory memory = memory(level.getServer());
        UUID anchored = memory.anchoredGenieId();
        UUID candidate = genie.getUUID();

        if (anchored == null) {
            memory.anchorGenie(candidate, level.dimension(), genie.blockPosition());
            return true;
        }
        if (anchored.equals(candidate)) {
            memory.updateAnchorLocation(level.dimension(), genie.blockPosition());
            return true;
        }
        KubanHorizons.LOGGER.debug(
                "Отклонена вторая Кубанская Джинния {}: мир уже привязан к {}.", candidate, anchored);
        return false;
    }

    /** Запоминает, где джинния находилась последний раз, чтобы её можно было найти после выгрузки чанка. */
    public static void rememberLocation(KubanGenie genie, ServerLevel level) {
        WorldGenieMemory memory = memory(level.getServer());
        if (genie.getUUID().equals(memory.anchoredGenieId())) {
            memory.updateAnchorLocation(level.dimension(), genie.blockPosition());
        }
    }

    /** UUID якорной джиннии мира либо null, если она ещё не появлялась. */
    public static UUID anchoredId(MinecraftServer server) {
        return memory(server).anchoredGenieId();
    }

    /** Измерение, в котором джиннию видели последний раз. */
    public static java.util.Optional<net.minecraft.resources.ResourceKey<Level>> anchoredDimension(
            MinecraftServer server) {
        return memory(server).anchoredGenieDimension();
    }

    /** Позиция, в которой джиннию видели последний раз. */
    public static BlockPos anchoredPosition(MinecraftServer server) {
        return memory(server).anchoredGeniePosition();
    }

    /**
     * Находит якорную джиннию, при необходимости прогружая её чанк.
     *
     * <p>Выгруженная джинния не тикает и не может сама догнать хозяина,
     * поэтому поводок обязан уметь её разбудить, а не считать пропавшей.</p>
     */
    public static KubanGenie find(MinecraftServer server) {
        UUID anchored = anchoredId(server);
        if (anchored == null) {
            return null;
        }

        KubanGenie found = lookup(server, anchored);
        if (found != null) {
            return found;
        }

        var dimension = anchoredDimension(server);
        if (dimension.isEmpty()) {
            return null;
        }
        ServerLevel level = server.getLevel(dimension.get());
        if (level == null) {
            return null;
        }
        BlockPos pos = anchoredPosition(server);
        // Синхронная прогрузка одного чанка: это происходит раз в секунду и
        // только когда джиннии нет в загруженных сущностях.
        level.getChunk(pos);
        return lookup(server, anchored);
    }

    private static KubanGenie lookup(MinecraftServer server, UUID anchored) {
        for (ServerLevel level : server.getAllLevels()) {
            Entity entity = level.getEntity(anchored);
            if (entity instanceof KubanGenie genie && genie.isAlive()) {
                return genie;
            }
        }
        return null;
    }

    /**
     * Переносит якорь на другую сущность.
     *
     * <p>Нужно, когда прежняя сущность действительно уничтожена и поводок
     * материализует джиннию заново из сохранённого состояния.</p>
     */
    public static void reanchor(KubanGenie genie, ServerLevel level) {
        memory(level.getServer()).anchorGenie(genie.getUUID(), level.dimension(), genie.blockPosition());
    }

    /**
     * Снимает якорь.
     *
     * <p>Вызывается только явно: истинным всемогуществом, где джинния
     * перестаёт существовать как сущность, и командой оператора. Выгрузка
     * чанка и {@code /kill} якорь не снимают — иначе неуязвимость джиннии
     * обходилась бы одной командой.</p>
     */
    public static void release(MinecraftServer server) {
        memory(server).releaseGenieAnchor();
    }

    /**
     * Снимает якорь перед сценарием, который спавнит джиннию заново.
     *
     * <p>Нужно GameTest: тесты выполняются в одном мире, и без сброса второй
     * же тест получил бы отказ во входе как «вторая джинния». Проверять
     * единственность в тестах нужно явным тестом, а не побочно.</p>
     */
    public static void releaseFor(ServerLevel level) {
        release(level.getServer());
    }
}
