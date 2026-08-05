package dev.romankrukovsky.kubanhorizons.crop;

import com.mojang.serialization.MapCodec;
import dev.romankrukovsky.kubanhorizons.registry.KHItems;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Кукуруза — высокая двухблочная культура.
 *
 * <p>Стадии {@code AGE 0..4}; выше подсолнечника, двухблочная уже со
 * стадии 2. Урожай — початки (loot table {@code blocks/corn_crop}).</p>
 */
public class CornCropBlock extends DoubleCropBlock {
    public static final MapCodec<CornCropBlock> CODEC = simpleCodec(CornCropBlock::new);

    public static final int MAX_AGE = 4;

    public CornCropBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public MapCodec<CornCropBlock> codec() {
        return CODEC;
    }

    @Override
    protected int doubleAge() {
        return 2;
    }

    @Override
    protected int[] stageHeights() {
        return new int[]{4, 10, 20, 26, 32};
    }

    @Override
    protected ItemStack seedStack() {
        return new ItemStack(KHItems.CORN_KERNELS.get());
    }

    public static boolean isMature(BlockState state) {
        return state.getValue(AGE) >= MAX_AGE;
    }
}
