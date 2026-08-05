package dev.romankrukovsky.kubanhorizons.datagen;

import dev.romankrukovsky.kubanhorizons.KubanHorizons;
import dev.romankrukovsky.kubanhorizons.registry.KHItems;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.TagsProvider;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.data.BlockTagCopyingItemTagProvider;

import java.util.concurrent.CompletableFuture;

/**
 * Теги предметов мода.
 */
public final class KHItemTagsProvider extends BlockTagCopyingItemTagProvider {
    public KHItemTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider,
            CompletableFuture<TagsProvider.TagLookup<Block>> blockTags) {
        super(output, lookupProvider, blockTags, KubanHorizons.MOD_ID);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        tag(ItemTags.VILLAGER_PLANTABLE_SEEDS).add(KHItems.SUNFLOWER_SEEDS.getKey());
        // Куры и попугаи едят семечки как ванильные семена.
        tag(ItemTags.CHICKEN_FOOD).add(KHItems.SUNFLOWER_SEEDS.getKey());
        tag(ItemTags.PARROT_FOOD).add(KHItems.SUNFLOWER_SEEDS.getKey());
        // Жмых — корм для свиней.
        tag(ItemTags.PIG_FOOD).add(KHItems.OIL_CAKE.getKey());
    }
}
