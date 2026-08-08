package dev.romankrukovsky.kubanhorizons.entity;

import dev.romankrukovsky.kubanhorizons.registry.KHBlocks;
import dev.romankrukovsky.kubanhorizons.util.KHIds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Общие проверки «это манул» и «это его двор» без ссылки на класс манула.
 *
 * <h2>Почему по идентификатору, а не по классу</h2>
 *
 * <p>Класс {@code entity.Manul} пишется параллельно другим агентом, а слой
 * мира (репутация, достижения, реплики жителей) должен собираться и
 * работать независимо от того, готов ли он. Сравнение по registry id
 * убирает зависимость компиляции: слой мира не ломается, если класс
 * сущности переименуют или поменяют его иерархию.</p>
 *
 * <p>Цена — потеря типизации, поэтому здесь нет ничего, кроме предикатов:
 * как только понадобится состояние манула, это делается через его
 * собственный API, а не здесь.</p>
 */
public final class ManulWorldHooks {
    /** Registry id манула. Совпадает с {@code KHEntities.MANUL}. */
    public static final Identifier MANUL_ID = KHIds.of("manul");

    private ManulWorldHooks() {
    }

    /** Манул ли это существо. */
    public static boolean isManul(Entity entity) {
        return entity != null && isManulType(entity.getType());
    }

    /** Манул ли это тип существа. */
    public static boolean isManulType(EntityType<?> type) {
        return type != null && MANUL_ID.equals(BuiltInRegistries.ENTITY_TYPE.getKey(type));
    }

    /** Укрытие манула ли этот блок. */
    public static boolean isShelter(BlockState state) {
        return state.is(KHBlocks.MANUL_SHELTER.get());
    }

    /**
     * Есть ли рядом укрытие манула.
     *
     * <p>Кубическая, а не сферическая проверка и небольшой радиус: вызов
     * идёт из достижений и реплик жителей, то есть редко, но в тике —
     * дешёвая форма важнее геометрической точности.</p>
     */
    public static boolean hasShelterNearby(BlockGetter level, BlockPos center, int radius) {
        for (BlockPos pos : BlockPos.betweenClosed(
                center.offset(-radius, -radius, -radius),
                center.offset(radius, radius, radius))) {
            if (isShelter(level.getBlockState(pos))) {
                return true;
            }
        }
        return false;
    }
}
