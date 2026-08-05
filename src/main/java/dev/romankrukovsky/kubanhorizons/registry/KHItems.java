package dev.romankrukovsky.kubanhorizons.registry;

import dev.romankrukovsky.kubanhorizons.KubanHorizons;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Регистрация предметов мода.
 */
public final class KHItems {
    private static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(KubanHorizons.MOD_ID);

    // --- Цепочка подсолнечника ---

    /**
     * Семена подсолнечника: сажают культуру, съедобны как слабый перекус,
     * сырьё для маслопресса.
     */
    public static final DeferredItem<Item> SUNFLOWER_SEEDS =
            ITEMS.registerItem("sunflower_seeds",
                    p -> new BlockItem(KHBlocks.SUNFLOWER_CROP.get(), p),
                    p -> p.useItemDescriptionPrefix().food(KHFoods.SUNFLOWER_SEEDS));

    /** Шляпка подсолнечника — урожай; в крафте распускается на семена. */
    public static final DeferredItem<Item> SUNFLOWER_HEAD =
            ITEMS.registerSimpleItem("sunflower_head");

    /** Бутылка подсолнечного масла — ключевой ингредиент кухни. */
    public static final DeferredItem<Item> SUNFLOWER_OIL =
            ITEMS.registerSimpleItem("sunflower_oil",
                    p -> p.stacksTo(16).craftRemainder(Items.GLASS_BOTTLE));

    /** Жмых — побочный продукт отжима; корм и компост. */
    public static final DeferredItem<Item> OIL_CAKE =
            ITEMS.registerSimpleItem("oil_cake");

    /** Жареные семечки — готовая еда. */
    public static final DeferredItem<Item> ROASTED_SUNFLOWER_SEEDS =
            ITEMS.registerSimpleItem("roasted_sunflower_seeds",
                    p -> p.food(KHFoods.ROASTED_SUNFLOWER_SEEDS));

    // --- Блок-предметы ---

    /** Маслопресс (предмет). */
    public static final DeferredItem<BlockItem> OIL_PRESS =
            ITEMS.registerSimpleBlockItem("oil_press", KHBlocks.OIL_PRESS);

    private KHItems() {
    }

    public static void register(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
    }
}
