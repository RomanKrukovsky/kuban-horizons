package dev.romankrukovsky.kubanhorizons.registry;

import dev.romankrukovsky.kubanhorizons.KubanHorizons;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.component.TypedEntityData;
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

    // --- Цепочка томата ---

    /** Семена томата. */
    public static final DeferredItem<Item> TOMATO_SEEDS =
            ITEMS.registerItem("tomato_seeds",
                    p -> new BlockItem(KHBlocks.TOMATO_BUSH.get(), p),
                    p -> p.useItemDescriptionPrefix());

    /** Томат — овощ. */
    public static final DeferredItem<Item> TOMATO =
            ITEMS.registerSimpleItem("tomato",
                    p -> p.food(KHFoods.TOMATO));

    // --- Цепочка винограда ---

    /** Черенок винограда — прививается на шпалеру. */
    public static final DeferredItem<Item> GRAPE_CUTTING =
            ITEMS.registerSimpleItem("grape_cutting");

    /** Гроздь винограда — еда и сырьё для сока. */
    public static final DeferredItem<Item> GRAPES =
            ITEMS.registerSimpleItem("grapes",
                    p -> p.food(KHFoods.GRAPES));

    /** Виноградная шпалера (предмет). */
    public static final DeferredItem<BlockItem> GRAPE_TRELLIS =
            ITEMS.registerSimpleBlockItem("grape_trellis", KHBlocks.GRAPE_TRELLIS);

    // --- Цепочка риса ---

    /** Рассада риса: посадка в затопленный чек. */
    public static final DeferredItem<Item> RICE_SEEDLINGS =
            ITEMS.registerItem("rice_seedlings",
                    p -> new BlockItem(KHBlocks.RICE_CROP.get(), p),
                    p -> p.useItemDescriptionPrefix());

    /** Необрушенный рис (урожай). */
    public static final DeferredItem<Item> RICE_PANICLE =
            ITEMS.registerSimpleItem("rice_panicle");

    /** Рис (крупа) — еда-ингредиент. */
    public static final DeferredItem<Item> RICE =
            ITEMS.registerSimpleItem("rice",
                    p -> p.food(KHFoods.RICE));

    /** Отварной рис — готовая еда в миске. */
    public static final DeferredItem<Item> COOKED_RICE =
            ITEMS.registerSimpleItem("cooked_rice",
                    p -> p.stacksTo(16)
                            .food(KHFoods.COOKED_RICE)
                            .usingConvertsTo(Items.BOWL));

    // --- Цепочка чая ---

    /** Саженец чайного куста. */
    public static final DeferredItem<Item> TEA_SAPLING =
            ITEMS.registerItem("tea_sapling",
                    p -> new BlockItem(KHBlocks.TEA_BUSH.get(), p),
                    p -> p.useItemDescriptionPrefix());

    /** Свежий чайный лист. */
    public static final DeferredItem<Item> TEA_LEAVES =
            ITEMS.registerSimpleItem("tea_leaves");

    // --- Плодовые деревья ---

    /** Саженец персика. */
    public static final DeferredItem<Item> PEACH_SAPLING =
            ITEMS.registerItem("peach_sapling",
                    p -> new BlockItem(KHBlocks.PEACH_SAPLING.get(), p),
                    p -> p.useItemDescriptionPrefix());

    /** Саженец абрикоса. */
    public static final DeferredItem<Item> APRICOT_SAPLING =
            ITEMS.registerItem("apricot_sapling",
                    p -> new BlockItem(KHBlocks.APRICOT_SAPLING.get(), p),
                    p -> p.useItemDescriptionPrefix());

    /** Саженец сливы. */
    public static final DeferredItem<Item> PLUM_SAPLING =
            ITEMS.registerItem("plum_sapling",
                    p -> new BlockItem(KHBlocks.PLUM_SAPLING.get(), p),
                    p -> p.useItemDescriptionPrefix());

    /** Саженец грецкого ореха. */
    public static final DeferredItem<Item> WALNUT_SAPLING =
            ITEMS.registerItem("walnut_sapling",
                    p -> new BlockItem(KHBlocks.WALNUT_SAPLING.get(), p),
                    p -> p.useItemDescriptionPrefix());

    /** Персик. */
    public static final DeferredItem<Item> PEACH =
            ITEMS.registerSimpleItem("peach",
                    p -> p.food(KHFoods.PEACH));

    /** Абрикос. */
    public static final DeferredItem<Item> APRICOT =
            ITEMS.registerSimpleItem("apricot",
                    p -> p.food(KHFoods.APRICOT));

    /** Слива. */
    public static final DeferredItem<Item> PLUM =
            ITEMS.registerSimpleItem("plum",
                    p -> p.food(KHFoods.PLUM));

    /** Грецкий орех. */
    public static final DeferredItem<Item> WALNUT =
            ITEMS.registerSimpleItem("walnut",
                    p -> p.food(KHFoods.WALNUT));

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

    /** Каменный оросительный желоб (предмет). */
    public static final DeferredItem<BlockItem> STONE_IRRIGATION_CHANNEL =
            ITEMS.registerSimpleBlockItem("stone_irrigation_channel",
                    KHBlocks.STONE_IRRIGATION_CHANNEL);

    /** Водозабор (предмет). */
    public static final DeferredItem<BlockItem> WATER_INTAKE =
            ITEMS.registerSimpleBlockItem("water_intake", KHBlocks.WATER_INTAKE);

    /** Сушильная рама (предмет). */
    public static final DeferredItem<BlockItem> DRYING_RACK =
            ITEMS.registerSimpleBlockItem("drying_rack", KHBlocks.DRYING_RACK);

    /** Ручная мельница (предмет). */
    public static final DeferredItem<BlockItem> HAND_MILL =
            ITEMS.registerSimpleBlockItem("hand_mill", KHBlocks.HAND_MILL);

    public static final DeferredItem<BlockItem> CUTTING_BOARD =
            ITEMS.registerSimpleBlockItem("cutting_board", KHBlocks.CUTTING_BOARD);

    /** Укрытие для манула (предмет). */
    public static final DeferredItem<BlockItem> MANUL_SHELTER =
            ITEMS.registerSimpleBlockItem("manul_shelter", KHBlocks.MANUL_SHELTER);

    /**
     * Ведро с осетром: ванильный способ переноса рыбы.
     *
     * <p>Без него осётр не ловится ведром, и игрок, привыкший к ванильной рыбе,
     * получает пустое ведро — молчаливый сбой вместо ожидаемого действия.</p>
     */
    public static final DeferredItem<net.minecraft.world.item.MobBucketItem> STURGEON_BUCKET =
            ITEMS.registerItem("sturgeon_bucket",
                    p -> new net.minecraft.world.item.MobBucketItem(
                            KHEntities.STURGEON.get(),
                            net.minecraft.world.level.material.Fluids.WATER,
                            net.minecraft.sounds.SoundEvents.BUCKET_EMPTY_FISH, p),
                    p -> p.stacksTo(1));

    // --- Строительные материалы (предметы) ---

    /** Саманный кирпич (предмет). */
    public static final DeferredItem<BlockItem> ADOBE_BRICKS =
            ITEMS.registerSimpleBlockItem("adobe_bricks", KHBlocks.ADOBE_BRICKS);

    /** Саманная ступенька (предмет). */
    public static final DeferredItem<BlockItem> ADOBE_BRICK_STAIRS =
            ITEMS.registerSimpleBlockItem("adobe_brick_stairs", KHBlocks.ADOBE_BRICK_STAIRS);

    /** Саманная плита (предмет). */
    public static final DeferredItem<BlockItem> ADOBE_BRICK_SLAB =
            ITEMS.registerSimpleBlockItem("adobe_brick_slab", KHBlocks.ADOBE_BRICK_SLAB);

    /** Саманная стенка (предмет). */
    public static final DeferredItem<BlockItem> ADOBE_BRICK_WALL =
            ITEMS.registerSimpleBlockItem("adobe_brick_wall", KHBlocks.ADOBE_BRICK_WALL);

    /** Ракушечник (предмет). */
    public static final DeferredItem<BlockItem> SHELL_ROCK =
            ITEMS.registerSimpleBlockItem("shell_rock", KHBlocks.SHELL_ROCK);

    /** Ступенька из ракушечника (предмет). */
    public static final DeferredItem<BlockItem> SHELL_ROCK_STAIRS =
            ITEMS.registerSimpleBlockItem("shell_rock_stairs", KHBlocks.SHELL_ROCK_STAIRS);

    /** Плита из ракушечника (предмет). */
    public static final DeferredItem<BlockItem> SHELL_ROCK_SLAB =
            ITEMS.registerSimpleBlockItem("shell_rock_slab", KHBlocks.SHELL_ROCK_SLAB);

    /** Стенка из ракушечника (предмет). */
    public static final DeferredItem<BlockItem> SHELL_ROCK_WALL =
            ITEMS.registerSimpleBlockItem("shell_rock_wall", KHBlocks.SHELL_ROCK_WALL);

    /** Белёная штукатурка (предмет). */
    public static final DeferredItem<BlockItem> WHITEWASHED_PLASTER =
            ITEMS.registerSimpleBlockItem("whitewashed_plaster", KHBlocks.WHITEWASHED_PLASTER);

    /** Ступенька из белёной штукатурки (предмет). */
    public static final DeferredItem<BlockItem> WHITEWASHED_PLASTER_STAIRS =
            ITEMS.registerSimpleBlockItem("whitewashed_plaster_stairs", KHBlocks.WHITEWASHED_PLASTER_STAIRS);

    /** Плита из белёной штукатурки (предмет). */
    public static final DeferredItem<BlockItem> WHITEWASHED_PLASTER_SLAB =
            ITEMS.registerSimpleBlockItem("whitewashed_plaster_slab", KHBlocks.WHITEWASHED_PLASTER_SLAB);

    /** Черепица (предмет). */
    public static final DeferredItem<BlockItem> ROOF_TILES =
            ITEMS.registerSimpleBlockItem("roof_tiles", KHBlocks.ROOF_TILES);

    /** Ступеньки из черепицы (предмет). */
    public static final DeferredItem<BlockItem> ROOF_TILE_STAIRS =
            ITEMS.registerSimpleBlockItem("roof_tile_stairs", KHBlocks.ROOF_TILE_STAIRS);

    /** Плита из черепицы (предмет). */
    public static final DeferredItem<BlockItem> ROOF_TILE_SLAB =
            ITEMS.registerSimpleBlockItem("roof_tile_slab", KHBlocks.ROOF_TILE_SLAB);

    /** Декоративная керамика (предмет). */
    public static final DeferredItem<BlockItem> DECORATIVE_CERAMIC =
            ITEMS.registerSimpleBlockItem("decorative_ceramic", KHBlocks.DECORATIVE_CERAMIC);

    /** Резной оконный наличник (предмет). */
    public static final DeferredItem<BlockItem> CARVED_WINDOW_CASING =
            ITEMS.registerSimpleBlockItem("carved_window_casing", KHBlocks.CARVED_WINDOW_CASING);

    /** Плетень (предмет). */
    public static final DeferredItem<BlockItem> WATTLE =
            ITEMS.registerSimpleBlockItem("wattle", KHBlocks.WATTLE);

    /** Калитка плетня (предмет). */
    public static final DeferredItem<BlockItem> WATTLE_GATE =
            ITEMS.registerSimpleBlockItem("wattle_gate", KHBlocks.WATTLE_GATE);

    // --- Мучные продукты ---

    /** Мука — основа выпечки. */
    public static final DeferredItem<Item> FLOUR =
            ITEMS.registerSimpleItem("flour");

    /** Кукурузная крупа. */
    public static final DeferredItem<Item> CORNMEAL =
            ITEMS.registerSimpleItem("cornmeal");

    // --- Кухня ---

    /** Домашний хлеб. */
    public static final DeferredItem<Item> HOMEMADE_BREAD =
            ITEMS.registerSimpleItem("homemade_bread",
                    p -> p.food(KHFoods.HOMEMADE_BREAD));

    /** Кубанский борщ (миска). */
    public static final DeferredItem<Item> BORSCHT =
            ITEMS.registerSimpleItem("borscht",
                    p -> p.stacksTo(1)
                            .food(KHFoods.BORSCHT, KHFoods.BORSCHT_CONSUMABLE)
                            .usingConvertsTo(Items.BOWL));

    /** Мамалыга (миска). */
    public static final DeferredItem<Item> MAMALYGA =
            ITEMS.registerSimpleItem("mamalyga",
                    p -> p.stacksTo(16)
                            .food(KHFoods.MAMALYGA)
                            .usingConvertsTo(Items.BOWL));

    /** Чашка чая. */
    public static final DeferredItem<Item> TEA_CUP =
            ITEMS.registerSimpleItem("tea_cup",
                    p -> p.stacksTo(16)
                            .food(KHFoods.TEA_DRINK, KHFoods.TEA_DRINK_CONSUMABLE)
                            .usingConvertsTo(Items.GLASS_BOTTLE));

    /** Мёд с орехами. */
    public static final DeferredItem<Item> HONEY_WALNUTS =
            ITEMS.registerSimpleItem("honey_walnuts",
                    p -> p.stacksTo(16)
                            .food(KHFoods.HONEY_WALNUTS, KHFoods.HONEY_WALNUTS_CONSUMABLE)
                            .usingConvertsTo(Items.BOWL));

    /** Овощная закуска (икра). */
    public static final DeferredItem<Item> VEGETABLE_SPREAD =
            ITEMS.registerSimpleItem("vegetable_spread",
                    p -> p.stacksTo(16)
                            .food(KHFoods.VEGETABLE_SPREAD)
                            .usingConvertsTo(Items.BOWL));

    // --- Сушёные продукты ---

    /** Сушёный чай — заварка. */
    public static final DeferredItem<Item> DRIED_TEA =
            ITEMS.registerSimpleItem("dried_tea");

    /** Сушёные фрукты — походная еда. */
    public static final DeferredItem<Item> DRIED_FRUIT =
            ITEMS.registerSimpleItem("dried_fruit",
                    p -> p.food(KHFoods.DRIED_FRUIT));

    // --- Региональная фауна ---

    public static final DeferredItem<SpawnEggItem> PHEASANT_SPAWN_EGG =
            ITEMS.registerItem("pheasant_spawn_egg", SpawnEggItem::new,
                    p -> p.component(DataComponents.ENTITY_DATA,
                            TypedEntityData.of(KHEntities.PHEASANT.get(), new net.minecraft.nbt.CompoundTag())));
    public static final DeferredItem<SpawnEggItem> QUAIL_SPAWN_EGG =
            ITEMS.registerItem("quail_spawn_egg", SpawnEggItem::new,
                    p -> p.component(DataComponents.ENTITY_DATA,
                            TypedEntityData.of(KHEntities.QUAIL.get(), new net.minecraft.nbt.CompoundTag())));
    public static final DeferredItem<Item> RAW_PHEASANT =
            ITEMS.registerSimpleItem("raw_pheasant", p -> p.food(KHFoods.RAW_PHEASANT));
    public static final DeferredItem<Item> COOKED_PHEASANT =
            ITEMS.registerSimpleItem("cooked_pheasant", p -> p.food(KHFoods.COOKED_PHEASANT));
    public static final DeferredItem<Item> RAW_QUAIL =
            ITEMS.registerSimpleItem("raw_quail", p -> p.food(KHFoods.RAW_QUAIL));
    public static final DeferredItem<Item> COOKED_QUAIL =
            ITEMS.registerSimpleItem("cooked_quail", p -> p.food(KHFoods.COOKED_QUAIL));

    public static final DeferredItem<SpawnEggItem> WILD_BOAR_SPAWN_EGG =
            ITEMS.registerItem("wild_boar_spawn_egg", SpawnEggItem::new,
                    p -> p.component(DataComponents.ENTITY_DATA,
                            TypedEntityData.of(KHEntities.WILD_BOAR.get(), new net.minecraft.nbt.CompoundTag())));
    public static final DeferredItem<SpawnEggItem> NUTRIA_SPAWN_EGG =
            ITEMS.registerItem("nutria_spawn_egg", SpawnEggItem::new,
                    p -> p.component(DataComponents.ENTITY_DATA,
                            TypedEntityData.of(KHEntities.NUTRIA.get(), new net.minecraft.nbt.CompoundTag())));
    public static final DeferredItem<SpawnEggItem> CAUCASIAN_SHEPHERD_SPAWN_EGG =
            ITEMS.registerItem("caucasian_shepherd_spawn_egg", SpawnEggItem::new,
                    p -> p.component(DataComponents.ENTITY_DATA,
                            TypedEntityData.of(KHEntities.CAUCASIAN_SHEPHERD.get(), new net.minecraft.nbt.CompoundTag())));
    public static final DeferredItem<SpawnEggItem> LOCUST_SPAWN_EGG =
            ITEMS.registerItem("locust_spawn_egg", SpawnEggItem::new,
                    p -> p.component(DataComponents.ENTITY_DATA,
                            TypedEntityData.of(KHEntities.LOCUST.get(), new net.minecraft.nbt.CompoundTag())));
    public static final DeferredItem<SpawnEggItem> MANUL_SPAWN_EGG =
            ITEMS.registerItem("manul_spawn_egg", SpawnEggItem::new,
                    p -> p.component(DataComponents.ENTITY_DATA,
                            TypedEntityData.of(KHEntities.MANUL.get(), new net.minecraft.nbt.CompoundTag())));
    public static final DeferredItem<SpawnEggItem> STURGEON_SPAWN_EGG =
            ITEMS.registerItem("sturgeon_spawn_egg", SpawnEggItem::new,
                    p -> p.component(DataComponents.ENTITY_DATA,
                            TypedEntityData.of(KHEntities.STURGEON.get(), new net.minecraft.nbt.CompoundTag())));
    public static final DeferredItem<SpawnEggItem> GULL_SPAWN_EGG =
            ITEMS.registerItem("gull_spawn_egg", SpawnEggItem::new,
                    p -> p.component(DataComponents.ENTITY_DATA,
                            TypedEntityData.of(KHEntities.GULL.get(), new net.minecraft.nbt.CompoundTag())));
    public static final DeferredItem<SpawnEggItem> HERON_SPAWN_EGG =
            ITEMS.registerItem("heron_spawn_egg", SpawnEggItem::new,
                    p -> p.component(DataComponents.ENTITY_DATA,
                            TypedEntityData.of(KHEntities.HERON.get(), new net.minecraft.nbt.CompoundTag())));

    /** Сырая кабанина — трофей защищённого поля. */
    public static final DeferredItem<Item> RAW_BOAR =
            ITEMS.registerSimpleItem("raw_boar", p -> p.food(KHFoods.RAW_BOAR));
    public static final DeferredItem<Item> COOKED_BOAR =
            ITEMS.registerSimpleItem("cooked_boar", p -> p.food(KHFoods.COOKED_BOAR));
    public static final DeferredItem<Item> RAW_STURGEON =
            ITEMS.registerSimpleItem("raw_sturgeon", p -> p.food(KHFoods.RAW_STURGEON));
    public static final DeferredItem<Item> COOKED_STURGEON =
            ITEMS.registerSimpleItem("cooked_sturgeon", p -> p.food(KHFoods.COOKED_STURGEON));

    /** Шкура нутрии: ремесленное сырьё, а не еда. */
    public static final DeferredItem<Item> NUTRIA_PELT =
            ITEMS.registerSimpleItem("nutria_pelt");

    /** Деревянная ложка — иронический предмет подмены при атаке мечом. */
    public static final DeferredItem<Item> WOODEN_SPOON =
            ITEMS.registerSimpleItem("wooden_spoon");

    /** Застывший звуковой вал — материализованная звуковая волна Вардена. */
    public static final DeferredItem<Item> SONIC_BOOM_ITEM =
            ITEMS.registerSimpleItem("sonic_boom_item");

    /** Магическое зеркало — смартфон Джиннии. */
    public static final DeferredItem<Item> MAGIC_MIRROR =
            ITEMS.registerItem("magic_mirror",
                    dev.romankrukovsky.kubanhorizons.genie.item.MagicMirrorItem::new,
                    p -> p.stacksTo(1));

    /** Сжатый карманный мир — 100x100 область мира на ладони. */
    public static final DeferredItem<Item> MINIATURE_WORLD =
            ITEMS.registerItem("miniature_world",
                    dev.romankrukovsky.kubanhorizons.genie.item.MiniatureWorldItem::new,
                    p -> p.stacksTo(1));

    /** Лампа превращённого игрока-Джиннии (сосуд существования). */
    public static final DeferredItem<Item> PLAYER_GENIE_LAMP =
            ITEMS.registerItem("player_genie_lamp",
                    p -> new dev.romankrukovsky.kubanhorizons.genie.player.PlayerGenieLampItem(p),
                    p -> p.stacksTo(1));

    private KHItems() {
    }

    public static void register(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
    }
}
