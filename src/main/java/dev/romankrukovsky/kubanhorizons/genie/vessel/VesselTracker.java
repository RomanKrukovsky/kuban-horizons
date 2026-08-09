package dev.romankrukovsky.kubanhorizons.genie.vessel;

import dev.romankrukovsky.kubanhorizons.registry.KHItems;
import java.util.UUID;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Где сейчас лежит сосуд конкретного игрока-джиннии.
 *
 * <p>Нужен, потому что сосуд — это предмет, а не блок с фиксированными
 * координатами: лампа выдаётся в инвентарь при завершении превращения и дальше
 * живёт своей жизнью. Её можно выбросить, положить в сундук, отдать другому
 * игроку или закопать. Хвост-поводок обязан тянуться туда, где лампа
 * действительно находится, иначе он тянется в пустоту.</p>
 *
 * <p>Поиск, а не хранение позиции в attachment: позиция мгновенно устареет —
 * предмет переносят, а событий «предмет переехал» в Minecraft нет. Дешевле
 * искать по требованию в ограниченном радиусе, чем поддерживать индекс,
 * который всё равно будет врать.</p>
 *
 * <p>Радиус ограничен сознательно: сосуд, унесённый за пределы прогруженных
 * чанков, не находится вовсе — и это игровой факт, а не недостаток. Спрятать
 * сосуд далеко значит ослабить натяжение поводка, что и делает пряталки
 * осмысленной механикой.</p>
 */
public final class VesselTracker {
    /**
     * Радиус поиска в блоках.
     *
     * <p>96 — примерно шесть чанков: дальше сервер обычно уже не держит чанк
     * прогруженным, поэтому больший радиус давал бы ложное чувство точности.</p>
     */
    private static final double SEARCH_RADIUS = 96.0D;

    private VesselTracker() {
    }

    /** Результат поиска: где сосуд и в каком виде найден. */
    public record Located(Vec3 position, Holder holder) {
    }

    /** В каком виде сосуд обнаружен — от этого зависит, что с ним можно сделать. */
    public enum Holder {
        /** В инвентаре игрока — своего или чужого. */
        PLAYER,
        /** Лежит на земле как выброшенный предмет. */
        GROUND,
        /** В сундуке, бочке или другом контейнере. */
        CONTAINER
    }

    /**
     * Ищет сосуд указанного игрока в радиусе от точки.
     *
     * <p>Порядок проверки — от самого вероятного к самому дорогому: инвентари
     * игроков, затем выброшенные предметы, затем контейнеры. Контейнеры
     * последними потому, что их обход требует чтения блок-сущностей, а это
     * дороже, чем перебор сущностей поблизости.</p>
     *
     * @param genieId UUID игрока, чей сосуд ищется
     * @return {@code null}, если сосуд не найден в радиусе
     */
    public static Located find(ServerLevel level, Vec3 origin, UUID genieId) {
        AABB box = AABB.ofSize(origin, SEARCH_RADIUS * 2, SEARCH_RADIUS * 2, SEARCH_RADIUS * 2);

        for (Player player : level.players()) {
            if (!box.contains(player.position())) {
                continue;
            }
            if (containerHasVessel(player.getInventory(), genieId)) {
                return new Located(player.position(), Holder.PLAYER);
            }
        }

        for (ItemEntity dropped : level.getEntitiesOfClass(ItemEntity.class, box)) {
            if (isVesselOf(dropped.getItem(), genieId)) {
                return new Located(dropped.position(), Holder.GROUND);
            }
        }

        Located inContainer = findInContainers(level, origin, genieId);
        return inContainer;
    }

    /**
     * Обходит блок-сущности вокруг точки.
     *
     * <p>Шаг по чанкам, а не по блокам: перебирать 96³ позиций бессмысленно,
     * когда блок-сущности перечислимы прямо из чанка.</p>
     */
    private static Located findInContainers(ServerLevel level, Vec3 origin, UUID genieId) {
        int chunkRadius = (int) Math.ceil(SEARCH_RADIUS / 16.0D);
        int originChunkX = (int) Math.floor(origin.x) >> 4;
        int originChunkZ = (int) Math.floor(origin.z) >> 4;

        for (int dx = -chunkRadius; dx <= chunkRadius; dx++) {
            for (int dz = -chunkRadius; dz <= chunkRadius; dz++) {
                // Только уже прогруженные чанки: подгрузка ради поиска сосуда
                // превратила бы поводок в генератор мира.
                var chunk = level.getChunkSource().getChunkNow(originChunkX + dx, originChunkZ + dz);
                if (chunk == null) {
                    continue;
                }
                for (BlockEntity blockEntity : chunk.getBlockEntities().values()) {
                    if (blockEntity instanceof Container container
                            && containerHasVessel(container, genieId)) {
                        return new Located(Vec3.atCenterOf(blockEntity.getBlockPos()), Holder.CONTAINER);
                    }
                }
            }
        }
        return null;
    }

    private static boolean containerHasVessel(Container container, UUID genieId) {
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            if (isVesselOf(container.getItem(slot), genieId)) {
                return true;
            }
        }
        return false;
    }

    /** Это сосуд именно того игрока? */
    public static boolean isVesselOf(ItemStack stack, UUID genieId) {
        if (!stack.is(KHItems.PLAYER_GENIE_LAMP.get())) {
            return false;
        }
        String bound = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
                .copyTag().getStringOr("GeniePlayer", "");
        return bound.equals(genieId.toString());
    }

    /** Удобная перегрузка: искать сосуд самого игрока вокруг него же. */
    public static Located findFor(ServerPlayer player) {
        return find((ServerLevel) player.level(), player.position(), player.getUUID());
    }
}
