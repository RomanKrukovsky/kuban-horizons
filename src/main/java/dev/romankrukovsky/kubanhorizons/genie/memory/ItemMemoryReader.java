package dev.romankrukovsky.kubanhorizons.genie.memory;

import dev.romankrukovsky.kubanhorizons.entity.KubanGenie;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/** Подсистема чтения истории и памяти предметов (Item Memory & History Reader). */
public final class ItemMemoryReader {
    private ItemMemoryReader() {
    }

    public static void readItemMemory(KubanGenie genie, ServerPlayer player, ItemStack item) {
        String itemName = item.getHoverName().getString();

        int enchantCount = item.getOrDefault(DataComponents.ENCHANTMENTS, net.minecraft.world.item.enchantment.ItemEnchantments.EMPTY).size();
        int damage = item.getDamageValue();

        player.sendSystemMessage(Component.translatable("message.kubanhorizons.genie.memory.item_read",
                itemName, enchantCount, damage));
    }
}
