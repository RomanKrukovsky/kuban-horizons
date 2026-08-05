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

    /** Маслопресс — первое перерабатывающее устройство мода. */
    public static final DeferredBlock<OilPressBlock> OIL_PRESS =
            BLOCKS.registerBlock("oil_press", OilPressBlock::new,
                    p -> p.mapColor(MapColor.WOOD)
                            .strength(2.5F)
                            .sound(SoundType.WOOD)
                            .noOcclusion());

    private KHBlocks() {
    }

    public static void register(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
    }
}
