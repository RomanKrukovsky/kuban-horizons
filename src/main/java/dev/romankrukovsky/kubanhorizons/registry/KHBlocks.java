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

    private KHBlocks() {
    }

    public static void register(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
    }
}
