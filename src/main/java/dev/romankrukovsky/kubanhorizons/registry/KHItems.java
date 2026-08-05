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

    // --- Цепочка кукурузы ---

    /** Зёрна кукурузы: посадка + сырьё мельницы. */
    public static final DeferredItem<Item> CORN_KERNELS =
            ITEMS.registerItem("corn_kernels",
                    p -> new BlockItem(KHBlocks.CORN_CROP.get(), p),
                    p -> p.useItemDescriptionPrefix());

    /** Початок кукурузы — урожай; еда и источник зёрен. */
    public static final DeferredItem<Item> CORN_COB =
            ITEMS.registerSimpleItem("corn_cob",
                    p -> p.food(KHFoods.CORN_COB));

    /** Печёная кукуруза. */
    public static final DeferredItem<Item> GRILLED_CORN =
            ITEMS.registerSimpleItem("grilled_corn",
                    p -> p.food(KHFoods.GRILLED_CORN));

    // --- Цепочка чая ---

    /** Саженец чайного куста. */
    public static final DeferredItem<Item> TEA_SAPLING =
            ITEMS.registerItem("tea_sapling",
                    p -> new BlockItem(KHBlocks.TEA_BUSH.get(), p),
                    p -> p.useItemDescriptionPrefix());

    /** Свежий чайный лист. */
    public static final DeferredItem<Item> TEA_LEAVES =
            ITEMS.registerSimpleItem("tea_leaves");

    // --- Инструменты ---

    /** Почвенный щуп — анализатор плодородия грядок. */
    public static final DeferredItem<dev.romankrukovsky.kubanhorizons.item.SoilProbeItem> SOIL_PROBE =
            ITEMS.registerItem("soil_probe",
                    dev.romankrukovsky.kubanhorizons.item.SoilProbeItem::new,
                    p -> p.stacksTo(1));

    // --- Блок-предметы ---

    /** Маслопресс (предмет). */
    public static final DeferredItem<BlockItem> OIL_PRESS =
            ITEMS.registerSimpleBlockItem("oil_press", KHBlocks.OIL_PRESS);

    /** Оросительный желоб (предмет). */
    public static final DeferredItem<BlockItem> IRRIGATION_CHANNEL =
            ITEMS.registerSimpleBlockItem("irrigation_channel", KHBlocks.IRRIGATION_CHANNEL);

    /** Водозабор (предмет). */
    public static final DeferredItem<BlockItem> WATER_INTAKE =
            ITEMS.registerSimpleBlockItem("water_intake", KHBlocks.WATER_INTAKE);

    private KHItems() {
    }

    public static void register(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
    }
}
