package dev.romankrukovsky.kubanhorizons.vessel.schools;

import dev.romankrukovsky.kubanhorizons.genie.wish.WishExecutor;
import dev.romankrukovsky.kubanhorizons.genie.wish.WishIntent;
import dev.romankrukovsky.kubanhorizons.genie.wish.WishParser;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

/**
 * Школа лампы — исполнение желаний (WISH_EXECUTION).
 *
 * <p>Владелец называет желание с переименованной бумаги в руке (как в диалоге
 * джиннии), лампа разбирает его локальным парсером и исполняет через общий
 * {@link WishExecutor} — тот же серверный рантайм, что и у самой джиннии.</p>
 */
public final class WishExecutionSchool implements VesselSchool {

    private static final String WISH_KEY = "LampWish";

    @Override
    public String cast(ServerLevel level, ServerPlayer owner, ItemStack stack) {
        String wording = readWish(stack);
        if (wording == null || wording.isBlank()) {
            return "message.kubanhorizons.genie.lamp.unbound";
        }
        WishIntent intent = WishParser.parse(wording);
        WishExecutor.Result result = WishExecutor.execute(level, owner, intent);
        clearWish(stack);
        return result.executed()
                ? result.messageKey()
                : result.messageKey();
    }

    private static String readWish(ItemStack stack) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        return tag.getStringOr(WISH_KEY, null);
    }

    private static void clearWish(ItemStack stack) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        tag.remove(WISH_KEY);
        CustomData.set(DataComponents.CUSTOM_DATA, stack, tag);
    }

    /** Владелец кладёт формулировку желания на лампу (переименованная бумага). */
    public static void storeWish(ItemStack stack, String wording) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        tag.putString(WISH_KEY, wording);
        CustomData.set(DataComponents.CUSTOM_DATA, stack, tag);
    }
}