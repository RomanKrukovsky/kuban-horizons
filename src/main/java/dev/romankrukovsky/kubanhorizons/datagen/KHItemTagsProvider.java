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
 *
 * <p>Теги формы (ступеньки, плиты, стенки, ограды, листва, саженцы) существуют
 * и для блока, и для предмета. Дублировать список вручную нельзя — расхождение
 * рано или поздно случится, — поэтому предметная сторона копируется из
 * блочного провайдера через {@link #copy}. Само копирование в
 * {@code BlockTagCopyingItemTagProvider} не автоматическое: каждую пару нужно
 * объявить явно.</p>
 */
public final class KHItemTagsProvider extends BlockTagCopyingItemTagProvider {
    public KHItemTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider,
            CompletableFuture<TagsProvider.TagLookup<Block>> blockTags) {
        super(output, lookupProvider, blockTags, KubanHorizons.MOD_ID);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        tag(ItemTags.VILLAGER_PLANTABLE_SEEDS)
                .add(KHItems.SUNFLOWER_SEEDS.getKey())
                .add(KHItems.CORN_KERNELS.getKey());
        // Куры и попугаи едят семечки и зёрна как ванильные семена.
        tag(ItemTags.CHICKEN_FOOD)
                .add(KHItems.SUNFLOWER_SEEDS.getKey())
                .add(KHItems.CORN_KERNELS.getKey());
        tag(ItemTags.PARROT_FOOD)
                .add(KHItems.SUNFLOWER_SEEDS.getKey())
                .add(KHItems.CORN_KERNELS.getKey());
        // Жмых и початки — корм для свиней.
        tag(ItemTags.PIG_FOOD)
                .add(KHItems.OIL_CAKE.getKey())
                .add(KHItems.CORN_COB.getKey());

        // Формы и материалы: предметный тег повторяет блочный.
        // Листва не копируется — у плодовой листвы нет предмета (при
        // разрушении падает саженец), и minecraft:leaves не смог бы
        // разрешить ссылки.
        copyShape(net.minecraft.tags.BlockItemTags.STAIRS);
        copyShape(net.minecraft.tags.BlockItemTags.SLABS);
        copyShape(net.minecraft.tags.BlockItemTags.WALLS);
        copyShape(net.minecraft.tags.BlockItemTags.FENCES);
        copyShape(net.minecraft.tags.BlockItemTags.WOODEN_FENCES);
        copyShape(net.minecraft.tags.BlockItemTags.FENCE_GATES);
        copyShape(net.minecraft.tags.BlockItemTags.SAPLINGS);
    }

    /** Копирует блочную половину парного тега в предметную. */
    private void copyShape(net.minecraft.tags.BlockItemTagId tag) {
        copy(tag.block(), tag.item());
    }
}
