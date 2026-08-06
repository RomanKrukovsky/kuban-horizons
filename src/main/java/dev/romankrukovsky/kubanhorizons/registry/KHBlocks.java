package dev.romankrukovsky.kubanhorizons.registry;

import dev.romankrukovsky.kubanhorizons.KubanHorizons;
import dev.romankrukovsky.kubanhorizons.crop.SunflowerCropBlock;
import dev.romankrukovsky.kubanhorizons.processing.OilPressBlock;
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
    private static final DeferredRegister.Blocks BLOCKS =
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

    /** Маслопресс — первое перерабатывающее устройство мода. */
    public static final DeferredBlock<OilPressBlock> OIL_PRESS =
            BLOCKS.registerBlock("oil_press", OilPressBlock::new,
                    p -> p.mapColor(MapColor.WOOD)
                            .strength(2.5F)
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
    public static final DeferredBlock<net.minecraft.world.level.block.Block> ADOBE_BRICKS =
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
    public static final DeferredBlock<net.minecraft.world.level.block.Block> SHELL_ROCK =
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
