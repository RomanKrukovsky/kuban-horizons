package dev.romankrukovsky.kubanhorizons.client;

import dev.romankrukovsky.kubanhorizons.KubanHorizons;
import dev.romankrukovsky.kubanhorizons.config.KHClientConfig;
import dev.romankrukovsky.kubanhorizons.registry.KHItems;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Игровые подсказки на предметах мода.
 *
 * <p>Настройка {@code tooltips.detailed} обещала «расширенные игровые
 * подсказки», но в моде не было ни одной: галочку можно было включить и
 * выключить без всякого следствия. Здесь она наконец делает то, что
 * написано в её описании.</p>
 *
 * <p>Подсказки даются только там, где предмет сам себя не объясняет.
 * У «жареных семечек» назначение видно из названия — подсказка была бы
 * шумом. А вот что чайный лист надо сушить на раме, что черенок сажают
 * в шпалеру, а не в грядку, и что щуп показывает плодородие, — из
 * названия и модели не следует никак.</p>
 *
 * <p>Ключи переводов лежат в {@code KHTranslations} как
 * {@code tooltip.kubanhorizons.<item>}: подсказка — часть интерфейса,
 * и она обязана существовать на двух языках, как и всё остальное.</p>
 */
@EventBusSubscriber(modid = KubanHorizons.MOD_ID, value = Dist.CLIENT)
public final class KHTooltips {
    /**
     * Предметы, чьё применение неочевидно, и ключ их подсказки.
     *
     * <p>Сознательно неполный список: подсказка на каждом предмете
     * превратилась бы в фон, который перестают читать.</p>
     *
     * <p>Строится лениво, при первой подсказке. Статическая инициализация
     * здесь падала бы: карта обращается к {@code KHItems} через
     * {@code get()}, а класс может загрузиться раньше, чем реестр предметов
     * заполнен — например во время datagen.</p>
     */
    private static Map<Item, String> hints;

    private KHTooltips() {
    }

    private static Map<Item, String> buildHints() {
        Map<Item, String> hints = new LinkedHashMap<>();
        // Посадочный материал: куда именно сажать — из модели не видно.
        hints.put(KHItems.GRAPE_CUTTING.get(), "grape_cutting");
        hints.put(KHItems.RICE_SEEDLINGS.get(), "rice_seedlings");
        hints.put(KHItems.TEA_SAPLING.get(), "tea_sapling");
        // Сырьё, требующее переработки на конкретном устройстве.
        hints.put(KHItems.TEA_LEAVES.get(), "tea_leaves");
        hints.put(KHItems.SUNFLOWER_HEAD.get(), "sunflower_head");
        hints.put(KHItems.OIL_CAKE.get(), "oil_cake");
        // Инструмент, чей смысл целиком в невидимой механике.
        hints.put(KHItems.SOIL_PROBE.get(), "soil_probe");
        // Устройства: чем отличаются похожие желоба и зачем водозабор.
        hints.put(KHItems.IRRIGATION_CHANNEL.get(), "irrigation_channel");
        hints.put(KHItems.STONE_IRRIGATION_CHANNEL.get(), "stone_irrigation_channel");
        hints.put(KHItems.WATER_INTAKE.get(), "water_intake");
        hints.put(KHItems.DRYING_RACK.get(), "drying_rack");
        hints.put(KHItems.MANUL_SHELTER.get(), "manul_shelter");
        return Map.copyOf(hints);
    }

    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        if (!KHClientConfig.detailedTooltips()) {
            return;
        }
        String key = hints().get(event.getItemStack().getItem());
        if (key == null) {
            return;
        }
        event.getToolTip().add(Component
                .translatable("tooltip.kubanhorizons." + key)
                .withStyle(ChatFormatting.GRAY));
    }

    /** Список предметов с подсказками — для проверки полноты переводов. */
    public static Map<Item, String> hints() {
        Map<Item, String> local = hints;
        if (local == null) {
            local = buildHints();
            hints = local;
        }
        return local;
    }
}
