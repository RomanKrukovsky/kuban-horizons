package dev.romankrukovsky.kubanhorizons.genie.wish;

import dev.romankrukovsky.kubanhorizons.genie.aura.MagicalSignature;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.network.Filterable;
import net.minecraft.world.item.component.WrittenBookContent;
import java.util.List;

/** Магические контракты с мелкими буквами и лазейками (Wish Contract Engine). */
public final class WishContractEngine {
    private WishContractEngine() {
    }

    public static ItemStack createContractBook(ServerLevel level, Player player, String wish) {
        ItemStack book = new ItemStack(Items.WRITTEN_BOOK);
        String wording = wish == null || wish.isBlank() ? "Unspecified wish" : wish.trim();
        long expiresAt = level.getGameTime() + 24_000L * 7L;
        List<Filterable<Component>> pages = List.of(
                Filterable.passThrough(Component.literal("WISH CONTRACT\n\n" + wording)),
                Filterable.passThrough(Component.literal("Term: valid until world tick " + expiresAt
                        + ".\nScope: only the explicitly named result.")),
                Filterable.passThrough(Component.literal("Fine print: no player control, inventory rewrite,"
                        + " or unlisted collateral effects are authorized.")),
                Filterable.passThrough(Component.literal("Loophole: a materially changed preview voids consent."))
        );
        book.set(DataComponents.WRITTEN_BOOK_CONTENT, new WrittenBookContent(
                Filterable.passThrough("Wish Contract"), "Kuban Genie", 0, pages, true));
        book.set(DataComponents.CUSTOM_NAME,
                Component.translatable("item.kubanhorizons.wish_contract"));
        MagicalSignature.cast(level, player.position());
        player.sendSystemMessage(Component.translatable("message.kubanhorizons.genie.contract_issued"));
        return book;
    }
}
