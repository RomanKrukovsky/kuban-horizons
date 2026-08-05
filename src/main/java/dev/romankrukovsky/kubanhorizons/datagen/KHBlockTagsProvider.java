package dev.romankrukovsky.kubanhorizons.datagen;

import dev.romankrukovsky.kubanhorizons.KubanHorizons;
import dev.romankrukovsky.kubanhorizons.registry.KHBlocks;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.tags.BlockTags;
import net.neoforged.neoforge.common.data.BlockTagsProvider;

import java.util.concurrent.CompletableFuture;

/**
 * Теги блоков мода.
 */
public final class KHBlockTagsProvider extends BlockTagsProvider {
    public KHBlockTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider) {
        super(output, lookupProvider, KubanHorizons.MOD_ID);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        // Маслопресс и желоб рубятся топором.
        tag(BlockTags.MINEABLE_WITH_AXE).add(KHBlocks.OIL_PRESS.getKey());
        tag(BlockTags.MINEABLE_WITH_AXE).add(KHBlocks.IRRIGATION_CHANNEL.getKey());
        tag(BlockTags.MINEABLE_WITH_AXE).add(KHBlocks.GRAPE_TRELLIS.getKey());
        // Водозабор добывается киркой.
        tag(BlockTags.MINEABLE_WITH_PICKAXE).add(KHBlocks.WATER_INTAKE.getKey());
        // Культуры (для механик, ломающих/обходящих культуры).
        tag(BlockTags.CROPS)
                .add(KHBlocks.SUNFLOWER_CROP.getKey())
                .add(KHBlocks.CORN_CROP.getKey());
        // Чайный куст растёт на земле, как ягодный куст.
        tag(BlockTags.SWORD_EFFICIENT).add(KHBlocks.TEA_BUSH.getKey());
    }
}
