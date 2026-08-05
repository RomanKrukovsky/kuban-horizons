package dev.romankrukovsky.kubanhorizons.crop;

import com.mojang.serialization.MapCodec;
import dev.romankrukovsky.kubanhorizons.registry.KHItems;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Культурный подсолнечник — двухблочная культура.
 *
 * <p>Стадии {@code AGE 0..4}: до стадии 2 включительно растение занимает
 * один блок, со стадии 3 появляется верхняя половина (бутон), на стадии 4
 * раскрывается шляпка. Урожай — шляпка подсолнечника и семена
 * (loot table {@code blocks/sunflower_crop}).</p>
 */
public class SunflowerCropBlock extends DoubleCropBlock {
    public static final MapCodec<SunflowerCropBlock> CODEC = simpleCodec(SunflowerCropBlock::new);

    public static final int MAX_AGE = 4;

    public SunflowerCropBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public MapCodec<SunflowerCropBlock> codec() {
        return CODEC;
    }

    @Override
    protected ItemStack seedStack() {
        return new ItemStack(KHItems.SUNFLOWER_SEEDS.get());
    }

    /** Статический аналог для loot-условий. */
    public static boolean isMature(BlockState state) {
        return state.getValue(AGE) >= MAX_AGE;
    }
}
