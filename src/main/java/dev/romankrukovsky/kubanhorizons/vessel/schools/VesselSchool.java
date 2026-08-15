package dev.romankrukovsky.kubanhorizons.vessel.schools;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/**
 * Школа магии сосуда (Vessel System): каждая из пяти школ — свой способ
 * менять мир вокруг владельца.
 *
 * <p>Вызов идёт с сервера после проверки владения, поэтому каждая школа
 * работает только у настоящего хозяина сосуда.</p>
 */
public interface VesselSchool {

    /** Применяет эффект школы. Возвращает сообщение для владельца или null. */
    String cast(ServerLevel level, ServerPlayer owner, ItemStack stack);
}