package dev.romankrukovsky.kubanhorizons.item;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.Filterable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.WrittenBookContent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import java.util.List;

/**
 * «Путеводитель по Кубани» — внутриигровое руководство.
 *
 * <p>Реализован как письменная книга со страницами-переводами: каждая
 * страница — {@link Component#translatable}, поэтому книга автоматически
 * отображается на языке клиента (ru/en) без дублирования контента.</p>
 */
@EventBusSubscriber(modid = dev.romankrukovsky.kubanhorizons.KubanHorizons.MOD_ID)
public final class KubanGuide {
    /** Число страниц руководства (ключи guide.kubanhorizons.page1..N). */
    public static final int PAGES = 8;

    private KubanGuide() {
    }

    /** Создаёт экземпляр путеводителя. */
    public static ItemStack create() {
        ItemStack book = new ItemStack(Items.WRITTEN_BOOK);
        List<Filterable<Component>> pages = java.util.stream.IntStream.rangeClosed(1, PAGES)
                .mapToObj(i -> Filterable.passThrough(
                        (Component) Component.translatable("guide.kubanhorizons.page" + i)))
                .toList();
        book.set(DataComponents.WRITTEN_BOOK_CONTENT, new WrittenBookContent(
                Filterable.passThrough("Путеводитель по Кубани"),
                "Kuban Horizons",
                0,
                pages,
                true));
        book.set(DataComponents.CUSTOM_NAME,
                Component.translatable("guide.kubanhorizons.title"));
        return book;
    }

    /** Выдаёт книгу игроку, если у него её ещё не было (флаг — attachment). */
    public static void giveIfFirstJoin(ServerPlayer player) {
        if (player.getData(dev.romankrukovsky.kubanhorizons.registry.KHAttachments.GUIDE_GIVEN.get())) {
            return;
        }
        player.setData(dev.romankrukovsky.kubanhorizons.registry.KHAttachments.GUIDE_GIVEN.get(), Boolean.TRUE);
        ItemStack book = create();
        if (!player.getInventory().add(book)) {
            player.drop(book, false);
        }
    }

    @SubscribeEvent
    static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            giveIfFirstJoin(player);
        }
    }
}
