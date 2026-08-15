package dev.romankrukovsky.kubanhorizons.registry;

import dev.romankrukovsky.kubanhorizons.KubanHorizons;
import dev.romankrukovsky.kubanhorizons.block.CarvedWindowCasingBlock;
import dev.romankrukovsky.kubanhorizons.crop.SunflowerCropBlock;
import dev.romankrukovsky.kubanhorizons.genie.vessel.KubanJugBlock;
import dev.romankrukovsky.kubanhorizons.processing.OilPressBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Регистрация блоков мода.
 *
 * <p>Блоки-предметы регистрируются отдельно в {@link KHItems}, чтобы
 * сохранять контроль над свойствами предметов.</p>
 */
public final class KHBlocks {
    static final DeferredRegister.Blocks BLOCKS =
            DeferredRegister.createBlocks(KubanHorizons.MOD_ID);

    /** Культурный подсолнечник (двухблочная культура, без предмета — сажается семенами). */
    public static final DeferredBlock<SunflowerCropBlock> SUNFLOWER_CROP =
            BLOCKS.registerBlock("sunflower_crop", SunflowerCropBlock::new,
                    p -> p.mapColor(MapColor.PLANT)
                            .noCollision()
                            .randomTicks()
                            .instabreak()
                            .sound(SoundType.CROP)
                            .pushReaction(PushReaction.DESTROY));

    /** Кукуруза (двухблочная культура, сажается зёрнами). */
    public static final DeferredBlock<dev.romankrukovsky.kubanhorizons.crop.CornCropBlock> CORN_CROP =
            BLOCKS.registerBlock("corn_crop", dev.romankrukovsky.kubanhorizons.crop.CornCropBlock::new,
                    p -> p.mapColor(MapColor.PLANT)
                            .noCollision()
                            .randomTicks()
                            .instabreak()
                            .sound(SoundType.CROP)
                            .pushReaction(PushReaction.DESTROY));

    /** Томатный куст (многосборный). */
    public static final DeferredBlock<dev.romankrukovsky.kubanhorizons.crop.TomatoBushBlock> TOMATO_BUSH =
            BLOCKS.registerBlock("tomato_bush", dev.romankrukovsky.kubanhorizons.crop.TomatoBushBlock::new,
                    p -> p.mapColor(MapColor.PLANT)
                            .noCollision()
                            .randomTicks()
                            .instabreak()
                            .sound(SoundType.CROP)
                            .pushReaction(PushReaction.DESTROY));

    /** Виноградная шпалера (пустая или с лозой). */
    public static final DeferredBlock<dev.romankrukovsky.kubanhorizons.crop.GrapeTrellisBlock> GRAPE_TRELLIS =
            BLOCKS.registerBlock("grape_trellis", dev.romankrukovsky.kubanhorizons.crop.GrapeTrellisBlock::new,
                    p -> p.mapColor(MapColor.WOOD)
                            .strength(0.8F)
                            .randomTicks()
                            .sound(SoundType.WOOD)
                            .noOcclusion()
                            .pushReaction(PushReaction.DESTROY));

    /** Рис (затопленная культура). */
    public static final DeferredBlock<dev.romankrukovsky.kubanhorizons.crop.RiceCropBlock> RICE_CROP =
            BLOCKS.registerBlock("rice_crop", dev.romankrukovsky.kubanhorizons.crop.RiceCropBlock::new,
                    p -> p.mapColor(MapColor.PLANT)
                            .noCollision()
                            .randomTicks()
                            .instabreak()
                            .sound(SoundType.CROP)
                            .pushReaction(PushReaction.DESTROY));

    /** Чайный куст (многолетний, многосборный). */
    public static final DeferredBlock<dev.romankrukovsky.kubanhorizons.crop.TeaBushBlock> TEA_BUSH =
            BLOCKS.registerBlock("tea_bush", dev.romankrukovsky.kubanhorizons.crop.TeaBushBlock::new,
                    p -> p.mapColor(MapColor.PLANT)
                            .noCollision()
                            .randomTicks()
                            .strength(0.2F)
                            .sound(SoundType.SWEET_BERRY_BUSH)
                            .pushReaction(PushReaction.DESTROY));

    /** Деревянный оросительный желоб. */
    public static final DeferredBlock<dev.romankrukovsky.kubanhorizons.irrigation.IrrigationChannelBlock> IRRIGATION_CHANNEL =
            BLOCKS.registerBlock("irrigation_channel",
                    dev.romankrukovsky.kubanhorizons.irrigation.IrrigationChannelBlock::new,
                    p -> p.mapColor(MapColor.WOOD)
                            .strength(2.0F)
                            .sound(SoundType.WOOD)
                            .noOcclusion());

    /** Каменный оросительный желоб: та же гидравлика, но нутрия его не грызёт. */
    public static final DeferredBlock<dev.romankrukovsky.kubanhorizons.irrigation.StoneIrrigationChannelBlock> STONE_IRRIGATION_CHANNEL =
            BLOCKS.registerBlock("stone_irrigation_channel",
                    dev.romankrukovsky.kubanhorizons.irrigation.StoneIrrigationChannelBlock::new,
                    p -> p.mapColor(MapColor.STONE)
                            .strength(3.0F)
                            .sound(SoundType.STONE)
                            .requiresCorrectToolForDrops()
                            .noOcclusion());

    /** Водозабор — источник оросительной сети. */
    public static final DeferredBlock<dev.romankrukovsky.kubanhorizons.irrigation.WaterIntakeBlock> WATER_INTAKE =
            BLOCKS.registerBlock("water_intake",
                    dev.romankrukovsky.kubanhorizons.irrigation.WaterIntakeBlock::new,
                    p -> p.mapColor(MapColor.STONE)
                            .strength(3.0F)
                            .sound(SoundType.STONE)
                            .noOcclusion());

    /** Ручная мельница-жёрнов. */
    public static final DeferredBlock<dev.romankrukovsky.kubanhorizons.processing.HandMillBlock> HAND_MILL =
            BLOCKS.registerBlock("hand_mill",
                    dev.romankrukovsky.kubanhorizons.processing.HandMillBlock::new,
                    p -> p.mapColor(MapColor.STONE)
                            .strength(2.5F)
                            .sound(SoundType.STONE)
                            .noOcclusion());

    /** Сушильная рама (чай, фрукты, рыба). */
    public static final DeferredBlock<dev.romankrukovsky.kubanhorizons.processing.DryingRackBlock> DRYING_RACK =
            BLOCKS.registerBlock("drying_rack",
                    dev.romankrukovsky.kubanhorizons.processing.DryingRackBlock::new,
                    p -> p.mapColor(MapColor.WOOD)
                            .strength(1.5F)
                            .sound(SoundType.WOOD)
                            .noOcclusion());

    /** Разделочный стол — нарезка продуктов ножом, без GUI. */
    public static final DeferredBlock<dev.romankrukovsky.kubanhorizons.processing.CuttingBoardBlock> CUTTING_BOARD =
            BLOCKS.registerBlock("cutting_board",
                    dev.romankrukovsky.kubanhorizons.processing.CuttingBoardBlock::new,
                    p -> p.mapColor(MapColor.WOOD)
                            .strength(1.5F)
                            .sound(SoundType.WOOD)
                            .noOcclusion());

    /**
     * Коптильня — рыба и мясо в копчёности (GAME_DESIGN.md §7).
     *
     * <p>Светится, пока топится: {@code lightLevel} читает состояние
     * {@code LIT}, поэтому работающая коптильня заметна в темноте, а
     * остывшая — нет. Свет слабый (7), это топка, а не факел.</p>
     */
    public static final DeferredBlock<dev.romankrukovsky.kubanhorizons.processing.SmokehouseBlock> SMOKEHOUSE =
            BLOCKS.registerBlock("smokehouse",
                    dev.romankrukovsky.kubanhorizons.processing.SmokehouseBlock::new,
                    p -> p.mapColor(MapColor.WOOD)
                            .strength(2.0F)
                            .sound(SoundType.WOOD)
                            .lightLevel(state -> state.getValue(
                                    dev.romankrukovsky.kubanhorizons.processing.SmokehouseBlock.LIT) ? 7 : 0)
                            .noOcclusion());
    /** Маслопресс — первое перерабатывающее устройство мода. */
    public static final DeferredBlock<OilPressBlock> OIL_PRESS =
            BLOCKS.registerBlock("oil_press", OilPressBlock::new,
                    p -> p.mapColor(MapColor.WOOD)
                            .strength(2.5F)
                            .sound(SoundType.WOOD)
                            .noOcclusion());

    /**
     * Виноградный пресс — давильный чан (GAME_DESIGN.md §7).
     *
     * <p>Коллизия у чана обычная и намеренно <b>не</b> отключена: механика
     * топтания опирается на то, что игрок стоит именно на этом блоке. С
     * {@code noCollision()} игрок проваливался бы сквозь чан и «наступал» бы на
     * блок под ним — давка не срабатывала бы никогда. Низкий силуэт задаётся
     * формой блока, а не отсутствием коллизии.</p>
     */
    public static final DeferredBlock<dev.romankrukovsky.kubanhorizons.processing.GrapePressBlock> GRAPE_PRESS =
            BLOCKS.registerBlock("grape_press",
                    dev.romankrukovsky.kubanhorizons.processing.GrapePressBlock::new,
                    p -> p.mapColor(MapColor.WOOD)
                            .strength(2.0F)
                            .sound(SoundType.WOOD)
                            .noOcclusion());

    // --- Плодовые деревья ---

    /** Персиковая листва (цветёт и плодоносит). */
    public static final DeferredBlock<dev.romankrukovsky.kubanhorizons.crop.FruitLeavesBlock> PEACH_LEAVES =
            BLOCKS.registerBlock("peach_leaves",
                    p -> new dev.romankrukovsky.kubanhorizons.crop.FruitLeavesBlock(
                            () -> KHItems.PEACH.get(), p),
                    fruitLeavesProperties());

    /** Абрикосовая листва. */
    public static final DeferredBlock<dev.romankrukovsky.kubanhorizons.crop.FruitLeavesBlock> APRICOT_LEAVES =
            BLOCKS.registerBlock("apricot_leaves",
                    p -> new dev.romankrukovsky.kubanhorizons.crop.FruitLeavesBlock(
                            () -> KHItems.APRICOT.get(), p),
                    fruitLeavesProperties());

    /** Сливовая листва. */
    public static final DeferredBlock<dev.romankrukovsky.kubanhorizons.crop.FruitLeavesBlock> PLUM_LEAVES =
            BLOCKS.registerBlock("plum_leaves",
                    p -> new dev.romankrukovsky.kubanhorizons.crop.FruitLeavesBlock(
                            () -> KHItems.PLUM.get(), p),
                    fruitLeavesProperties());

    /** Листва грецкого ореха. */
    public static final DeferredBlock<dev.romankrukovsky.kubanhorizons.crop.FruitLeavesBlock> WALNUT_LEAVES =
            BLOCKS.registerBlock("walnut_leaves",
                    p -> new dev.romankrukovsky.kubanhorizons.crop.FruitLeavesBlock(
                            () -> KHItems.WALNUT.get(), p),
                    fruitLeavesProperties());

    /** Саженец персика. */
    public static final DeferredBlock<dev.romankrukovsky.kubanhorizons.crop.FruitSaplingBlock> PEACH_SAPLING =
            BLOCKS.registerBlock("peach_sapling",
                    p -> new dev.romankrukovsky.kubanhorizons.crop.FruitSaplingBlock(
                            () -> PEACH_LEAVES.get(), p),
                    fruitSaplingProperties());

    /** Саженец абрикоса. */
    public static final DeferredBlock<dev.romankrukovsky.kubanhorizons.crop.FruitSaplingBlock> APRICOT_SAPLING =
            BLOCKS.registerBlock("apricot_sapling",
                    p -> new dev.romankrukovsky.kubanhorizons.crop.FruitSaplingBlock(
                            () -> APRICOT_LEAVES.get(), p),
                    fruitSaplingProperties());

    /** Саженец сливы. */
    public static final DeferredBlock<dev.romankrukovsky.kubanhorizons.crop.FruitSaplingBlock> PLUM_SAPLING =
            BLOCKS.registerBlock("plum_sapling",
                    p -> new dev.romankrukovsky.kubanhorizons.crop.FruitSaplingBlock(
                            () -> PLUM_LEAVES.get(), p),
                    fruitSaplingProperties());

    /** Саженец грецкого ореха. */
    public static final DeferredBlock<dev.romankrukovsky.kubanhorizons.crop.FruitSaplingBlock> WALNUT_SAPLING =
            BLOCKS.registerBlock("walnut_sapling",
                    p -> new dev.romankrukovsky.kubanhorizons.crop.FruitSaplingBlock(
                            () -> WALNUT_LEAVES.get(), p),
                    fruitSaplingProperties());

    /** Свойства плодовой листвы — по образцу ванильной вишни. */
    private static java.util.function.UnaryOperator<BlockBehaviour.Properties> fruitLeavesProperties() {
        return p -> p.mapColor(MapColor.PLANT)
                .strength(0.2F)
                .randomTicks()
                .sound(SoundType.GRASS)
                .noOcclusion()
                .isSuffocating((state, level, pos) -> false)
                .isViewBlocking((state, level, pos) -> false)
                .ignitedByLava()
                .pushReaction(PushReaction.DESTROY)
                .isRedstoneConductor((state, level, pos) -> false);
    }

    /** Свойства саженца — по образцу ванильных саженцев. */
    private static java.util.function.UnaryOperator<BlockBehaviour.Properties> fruitSaplingProperties() {
        return p -> p.mapColor(MapColor.PLANT)
                .noCollision()
                .randomTicks()
                .instabreak()
                .sound(SoundType.GRASS)
                .pushReaction(PushReaction.DESTROY);
    }

    // --- Строительные материалы (этап 7) ---

    /** Саманный кирпич — глиняно-соломенная кладка кубанской хаты. */
    public static final DeferredBlock<Block> ADOBE_BRICKS =
            BLOCKS.registerSimpleBlock("adobe_bricks", adobeProperties());

    /** Саманная ступенька. */
    public static final DeferredBlock<net.minecraft.world.level.block.StairBlock> ADOBE_BRICK_STAIRS =
            BLOCKS.registerBlock("adobe_brick_stairs",
                    p -> new net.minecraft.world.level.block.StairBlock(
                            ADOBE_BRICKS.get().defaultBlockState(), p),
                    adobeProperties());

    /** Саманная плита. */
    public static final DeferredBlock<net.minecraft.world.level.block.SlabBlock> ADOBE_BRICK_SLAB =
            BLOCKS.registerBlock("adobe_brick_slab",
                    net.minecraft.world.level.block.SlabBlock::new,
                    adobeProperties());

    /** Саманная стенка. */
    public static final DeferredBlock<net.minecraft.world.level.block.WallBlock> ADOBE_BRICK_WALL =
            BLOCKS.registerBlock("adobe_brick_wall",
                    net.minecraft.world.level.block.WallBlock::new,
                    solidOn(adobeProperties()));

    /** Ракушечник — пористый известняк азовского побережья. */
    public static final DeferredBlock<Block> SHELL_ROCK =
            BLOCKS.registerSimpleBlock("shell_rock", shellRockProperties());

    /** Ступенька из ракушечника. */
    public static final DeferredBlock<net.minecraft.world.level.block.StairBlock> SHELL_ROCK_STAIRS =
            BLOCKS.registerBlock("shell_rock_stairs",
                    p -> new net.minecraft.world.level.block.StairBlock(
                            SHELL_ROCK.get().defaultBlockState(), p),
                    shellRockProperties());

    /** Плита из ракушечника. */
    public static final DeferredBlock<net.minecraft.world.level.block.SlabBlock> SHELL_ROCK_SLAB =
            BLOCKS.registerBlock("shell_rock_slab",
                    net.minecraft.world.level.block.SlabBlock::new,
                    shellRockProperties());

    /** Стенка из ракушечника. */
    public static final DeferredBlock<net.minecraft.world.level.block.WallBlock> SHELL_ROCK_WALL =
            BLOCKS.registerBlock("shell_rock_wall",
                    net.minecraft.world.level.block.WallBlock::new,
                    solidOn(shellRockProperties()));

    /** Белёная штукатурка кубанской хаты. */
    public static final DeferredBlock<Block> WHITEWASHED_PLASTER =
            BLOCKS.registerSimpleBlock("whitewashed_plaster", plasterProperties());

    /** Ступенька из белёной штукатурки. */
    public static final DeferredBlock<net.minecraft.world.level.block.StairBlock> WHITEWASHED_PLASTER_STAIRS =
            BLOCKS.registerBlock("whitewashed_plaster_stairs",
                    p -> new net.minecraft.world.level.block.StairBlock(
                            WHITEWASHED_PLASTER.get().defaultBlockState(), p),
                    plasterProperties());

    /** Плита из белёной штукатурки. */
    public static final DeferredBlock<net.minecraft.world.level.block.SlabBlock> WHITEWASHED_PLASTER_SLAB =
            BLOCKS.registerBlock("whitewashed_plaster_slab",
                    net.minecraft.world.level.block.SlabBlock::new,
                    plasterProperties());

    /** Черепица — обожжённая глиняная кровля кубанского дома. */
    public static final DeferredBlock<Block> ROOF_TILES =
            BLOCKS.registerSimpleBlock("roof_tiles", ceramicProperties());

    /** Ступеньки из черепицы для скатной кровли. */
    public static final DeferredBlock<net.minecraft.world.level.block.StairBlock> ROOF_TILE_STAIRS =
            BLOCKS.registerBlock("roof_tile_stairs",
                    p -> new net.minecraft.world.level.block.StairBlock(
                            ROOF_TILES.get().defaultBlockState(), p),
                    ceramicProperties());

    /** Плита из черепицы для пологой кровли. */
    public static final DeferredBlock<net.minecraft.world.level.block.SlabBlock> ROOF_TILE_SLAB =
            BLOCKS.registerBlock("roof_tile_slab",
                    net.minecraft.world.level.block.SlabBlock::new,
                    ceramicProperties());

    /** Декоративная расписная керамика. */
    public static final DeferredBlock<Block> DECORATIVE_CERAMIC =
            BLOCKS.registerSimpleBlock("decorative_ceramic", ceramicProperties());

    /** Резной оконный наличник. */
    public static final DeferredBlock<CarvedWindowCasingBlock> CARVED_WINDOW_CASING =
            BLOCKS.registerBlock("carved_window_casing", CarvedWindowCasingBlock::new,
                    p -> plasterProperties().apply(p).noOcclusion());

    /** Плетень — лозовая ограда двора. */
    public static final DeferredBlock<net.minecraft.world.level.block.FenceBlock> WATTLE =
            BLOCKS.registerBlock("wattle",
                    net.minecraft.world.level.block.FenceBlock::new,
                    wattleProperties());

    /** Калитка плетня. */
    public static final DeferredBlock<net.minecraft.world.level.block.FenceGateBlock> WATTLE_GATE =
            BLOCKS.registerBlock("wattle_gate",
                    p -> new net.minecraft.world.level.block.FenceGateBlock(p,
                            net.minecraft.sounds.SoundEvents.FENCE_GATE_OPEN,
                            net.minecraft.sounds.SoundEvents.FENCE_GATE_CLOSE),
                    solidOn(wattleProperties()));

    /**
     * Укрытие для манула — дрова, сено и камень у сарая.
     *
     * <p>Смешанный материал, поэтому и звук смешанный: сено глушит шаги
     * ({@link SoundType#GRASS}), а прочность взята по дровам, не по камню —
     * укрытие должно разбираться топором за пару секунд, иначе игрок не
     * станет переставлять его, подбирая место для манула.</p>
     *
     * <p>{@code noOcclusion} обязателен: у блока есть лаз, и без него
     * освещение соседних блоков считалось бы как за глухим кубом.</p>
     */
    public static final DeferredBlock<dev.romankrukovsky.kubanhorizons.block.ManulShelterBlock> MANUL_SHELTER =
            BLOCKS.registerBlock("manul_shelter",
                    dev.romankrukovsky.kubanhorizons.block.ManulShelterBlock::new,
                    p -> p.mapColor(MapColor.WOOD)
                            .strength(2.0F)
                            .sound(SoundType.GRASS)
                            .ignitedByLava()
                            .noOcclusion());

    // --- Почвенный ярус: чернозём (GAME_DESIGN.md §4, TECH_SPEC.md §3) ---

    /**
     * Чернозём — высший ярус почвы, находится в степи и переносится руками.
     *
     * <p>Свойства сняты с ванильной земли, кроме двух вещей. Во-первых,
     * {@code strength(0.6F)} вместо 0.5: чернозём плотнее рыхлой земли, и
     * лишняя десятая секунды на блок ощутимо превращает вывоз грядки 9×9 в
     * работу, а не в мгновение — поиск должен иметь цену.</p>
     *
     * <p>Во-вторых, {@link SoundType#GRAVEL} вместо ванильного глухого звука
     * земли: комковатая тучная почва звучит суше и «зернистее», и это
     * единственный звуковой признак, по которому ярус слышен на слух.</p>
     */
    public static final DeferredBlock<dev.romankrukovsky.kubanhorizons.soil.ChernozemBlock> CHERNOZEM =
            BLOCKS.registerBlock("chernozem",
                    dev.romankrukovsky.kubanhorizons.soil.ChernozemBlock::new,
                    p -> p.mapColor(MapColor.TERRACOTTA_BROWN)
                            .instrument(net.minecraft.world.level.block.state.properties.NoteBlockInstrument.BASEDRUM)
                            .strength(0.6F)
                            .sound(SoundType.GRAVEL));

    /**
     * Вспаханный чернозём — грядка высшего яруса.
     *
     * <p>Свойства ровно ванильной грядки ({@code Blocks.FARMLAND}):
     * {@code randomTicks} обязателен — на них держится и подсыхание, и
     * возврат в чернозём; {@code isViewBlocking}/{@code isSuffocating}
     * повторяют ваниль, потому что грядка на пиксель ниже полного куба, и без
     * этих флагов освещение и удушение считались бы неверно.</p>
     */
    public static final DeferredBlock<dev.romankrukovsky.kubanhorizons.soil.ChernozemFarmlandBlock> CHERNOZEM_FARMLAND =
            BLOCKS.registerBlock("chernozem_farmland",
                    dev.romankrukovsky.kubanhorizons.soil.ChernozemFarmlandBlock::new,
                    p -> p.mapColor(MapColor.DIRT)
                    .randomTicks()
                    .strength(0.6F)
                    .sound(SoundType.GRAVEL)
                    .isViewBlocking((state, level, pos) -> true)
                    .isSuffocating((state, level, pos) -> true));

    // --- Сосуды джиннии ---

    /** Кубанский кувшин-сосуд — призывает и привязывает джиннию. */
    public static final DeferredBlock<KubanJugBlock> KUBAN_JUG =
            BLOCKS.registerBlock("kuban_jug", KubanJugBlock::new,
                    p -> p.mapColor(MapColor.COLOR_BROWN)
                            .strength(2.0F, 6.0F)
                            .noOcclusion());

    /** Свойства самана — по образцу ванильного грязевого кирпича. */
    private static java.util.function.UnaryOperator<BlockBehaviour.Properties> adobeProperties() {
        return p -> p.mapColor(MapColor.TERRACOTTA_YELLOW)
                .instrument(net.minecraft.world.level.block.state.properties.NoteBlockInstrument.BASEDRUM)
                .requiresCorrectToolForDrops()
                .strength(1.5F, 3.0F)
                .sound(SoundType.MUD_BRICKS);
    }

    /** Свойства ракушечника — мягкий пористый камень (по образцу кальцита). */
    private static java.util.function.UnaryOperator<BlockBehaviour.Properties> shellRockProperties() {
        return p -> p.mapColor(MapColor.SAND)
                .instrument(net.minecraft.world.level.block.state.properties.NoteBlockInstrument.BASEDRUM)
                .requiresCorrectToolForDrops()
                .strength(0.9F, 2.5F)
                .sound(SoundType.CALCITE);
    }

    /** Свойства штукатурки — мягкая известковая отделка поверх кладки. */
    private static java.util.function.UnaryOperator<BlockBehaviour.Properties> plasterProperties() {
        return p -> p.mapColor(MapColor.QUARTZ)
                .instrument(net.minecraft.world.level.block.state.properties.NoteBlockInstrument.BASEDRUM)
                .requiresCorrectToolForDrops()
                .strength(0.8F, 2.0F)
                .sound(SoundType.CALCITE);
    }

    /** Свойства обожжённой черепицы и керамики. */
    private static java.util.function.UnaryOperator<BlockBehaviour.Properties> ceramicProperties() {
        return p -> p.mapColor(MapColor.TERRACOTTA_RED)
                .instrument(net.minecraft.world.level.block.state.properties.NoteBlockInstrument.BASEDRUM)
                .requiresCorrectToolForDrops()
                .strength(1.25F, 4.2F)
                .sound(SoundType.DECORATED_POT);
    }

    /** Свойства плетня — лоза: как дерево, но легче и горит. */
    private static java.util.function.UnaryOperator<BlockBehaviour.Properties> wattleProperties() {
        return p -> p.mapColor(MapColor.WOOD)
                .strength(1.5F, 2.0F)
                .sound(SoundType.SCAFFOLDING)
                .ignitedByLava();
    }

    /**
     * Стенки и калитки должны считаться сплошными снизу (ванильное
     * {@code forceSolidOn}), иначе на них нельзя ставить блоки-опоры.
     */
    private static java.util.function.UnaryOperator<BlockBehaviour.Properties> solidOn(
            java.util.function.UnaryOperator<BlockBehaviour.Properties> base) {
        return p -> base.apply(p).forceSolidOn();
    }

    private KHBlocks() {
    }

    public static void register(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
    }
}