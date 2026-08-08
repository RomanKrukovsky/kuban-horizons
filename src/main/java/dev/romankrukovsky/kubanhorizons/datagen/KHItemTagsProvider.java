package dev.romankrukovsky.kubanhorizons.datagen;

import dev.romankrukovsky.kubanhorizons.KubanHorizons;
import dev.romankrukovsky.kubanhorizons.registry.KHItems;
import dev.romankrukovsky.kubanhorizons.util.KHIds;
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

        // Корма фауны мода. Теги объявлены в самих сущностях
        // (AbstractGroundBird.FOOD_TAG и т.д.), но не заполнялись, поэтому ни
        // одно существо нельзя было ни приманить, ни развести: TemptGoal и
        // BreedGoal проверяют isFood, а тот всегда возвращал false.
        // Корм выбран по тому, чем животное питается в природе, а не «любой
        // предмет» — иначе выбор корма перестаёт быть игровым решением.
        tag(ItemTags.create(KHIds.of("ground_bird_foods")))
                .add(KHItems.SUNFLOWER_SEEDS.getKey())
                .add(KHItems.CORN_KERNELS.getKey())
                .add(KHItems.TOMATO_SEEDS.getKey());
        // Кабан всеяден: клубни, зерно, падалица.
        tag(ItemTags.create(KHIds.of("wild_boar_foods")))
                .add(KHItems.CORN_COB.getKey())
                .add(KHItems.OIL_CAKE.getKey())
                .add(KHItems.WALNUT.getKey())
                .add(KHItems.PLUM.getKey());
        // Нутрия — грызун-фитофаг: сочные стебли и корнеплоды.
        tag(ItemTags.create(KHIds.of("nutria_foods")))
                .add(KHItems.RICE_SEEDLINGS.getKey())
                .add(KHItems.RICE_PANICLE.getKey())
                .add(KHItems.TOMATO.getKey());
        // Пчела: нектароносы — цветущие части растений.
        // Чайка — падальщик побережья: рыба и любые объедки.
        tag(ItemTags.create(KHIds.of("gull_foods")))
                .add(KHItems.RAW_STURGEON.getKey())
                .add(KHItems.HOMEMADE_BREAD.getKey());
        // Цапля ест только рыбу — она рыбоядный хищник.
        tag(ItemTags.create(KHIds.of("heron_foods")))
                .add(KHItems.RAW_STURGEON.getKey());
        // Овчарка: мясо. Приручение — сырым мясом, разведение — готовым,
        // чтобы приручить было проще, чем развести.
        tag(ItemTags.create(KHIds.of("caucasian_shepherd_taming")))
                .add(KHItems.RAW_BOAR.getKey())
                .add(KHItems.RAW_PHEASANT.getKey());
        tag(ItemTags.create(KHIds.of("caucasian_shepherd_foods")))
                .add(KHItems.COOKED_BOAR.getKey())
                .add(KHItems.COOKED_PHEASANT.getKey())
                .add(KHItems.COOKED_QUAIL.getKey());

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
