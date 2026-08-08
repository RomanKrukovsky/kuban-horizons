package dev.romankrukovsky.kubanhorizons.genie.wish;

import dev.romankrukovsky.kubanhorizons.genie.aura.MagicalSignature;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Магические контракты с мелкими буквами и лазейками (Wish Contract Engine). */
public final class WishContractEngine {
    private WishContractEngine() {
    }

    public static ItemStack createContractBook(ServerLevel level, Player player, String wish) {
        ItemStack book = new ItemStack(Items.WRITTEN_BOOK);
        MagicalSignature.cast(level, player.position());
        player.sendSystemMessage(Component.translatable("message.kubanhorizons.genie.contract_issued"));
        return book;
    }
}
