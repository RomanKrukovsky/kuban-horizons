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
        tag(BlockTags.MINEABLE_WITH_AXE).add(KHBlocks.DRYING_RACK.getKey());
        // Водозабор и мельница добываются киркой.
        tag(BlockTags.MINEABLE_WITH_PICKAXE).add(KHBlocks.WATER_INTAKE.getKey());
        tag(BlockTags.MINEABLE_WITH_PICKAXE).add(KHBlocks.HAND_MILL.getKey());
        // Культуры (для механик, ломающих/обходящих культуры).
        tag(BlockTags.CROPS)
                .add(KHBlocks.SUNFLOWER_CROP.getKey())
                .add(KHBlocks.CORN_CROP.getKey());
        // Чайный куст растёт на земле, как ягодный куст.
        tag(BlockTags.SWORD_EFFICIENT).add(KHBlocks.TEA_BUSH.getKey());
        // Плодовая листва: рубится мотыгой и ведёт себя как листва
        // (decay контролируется PERSISTENT — саженец ставит true).
        tag(BlockTags.MINEABLE_WITH_HOE)
                .add(KHBlocks.PEACH_LEAVES.getKey())
                .add(KHBlocks.APRICOT_LEAVES.getKey())
                .add(KHBlocks.PLUM_LEAVES.getKey())
                .add(KHBlocks.WALNUT_LEAVES.getKey());
        tag(BlockTags.LEAVES)
                .add(KHBlocks.PEACH_LEAVES.getKey())
                .add(KHBlocks.APRICOT_LEAVES.getKey())
                .add(KHBlocks.PLUM_LEAVES.getKey())
                .add(KHBlocks.WALNUT_LEAVES.getKey());
        tag(net.minecraft.tags.BlockItemTags.SAPLINGS.block())
                .add(KHBlocks.PEACH_SAPLING.getKey())
                .add(KHBlocks.APRICOT_SAPLING.getKey())
                .add(KHBlocks.PLUM_SAPLING.getKey())
                .add(KHBlocks.WALNUT_SAPLING.getKey());

        // --- Строительные материалы (этап 7) ---
        // Саман и ракушечник — каменная кладка: кирка, ярус не нужен
        // (как ванильный грязевой кирпич — хватает деревянной).
        tag(BlockTags.MINEABLE_WITH_PICKAXE)
                .add(KHBlocks.ADOBE_BRICKS.getKey())
                .add(KHBlocks.ADOBE_BRICK_STAIRS.getKey())
                .add(KHBlocks.ADOBE_BRICK_SLAB.getKey())
                .add(KHBlocks.ADOBE_BRICK_WALL.getKey())
                .add(KHBlocks.SHELL_ROCK.getKey())
                .add(KHBlocks.SHELL_ROCK_STAIRS.getKey())
                .add(KHBlocks.SHELL_ROCK_SLAB.getKey())
                .add(KHBlocks.SHELL_ROCK_WALL.getKey())
                .add(KHBlocks.WHITEWASHED_PLASTER.getKey())
                .add(KHBlocks.WHITEWASHED_PLASTER_STAIRS.getKey())
                .add(KHBlocks.WHITEWASHED_PLASTER_SLAB.getKey())
                .add(KHBlocks.CARVED_WINDOW_CASING.getKey());
        // Формы вариантов: без этих тегов стенки не стыкуются с ванильными,
        // а плиты и ступеньки не видны механикам, работающим по форме.
        tag(BlockTags.STAIRS)
                .add(KHBlocks.ADOBE_BRICK_STAIRS.getKey())
                .add(KHBlocks.SHELL_ROCK_STAIRS.getKey())
                .add(KHBlocks.WHITEWASHED_PLASTER_STAIRS.getKey());
        tag(BlockTags.SLABS)
                .add(KHBlocks.ADOBE_BRICK_SLAB.getKey())
                .add(KHBlocks.SHELL_ROCK_SLAB.getKey())
                .add(KHBlocks.WHITEWASHED_PLASTER_SLAB.getKey());
        tag(BlockTags.WALLS)
                .add(KHBlocks.ADOBE_BRICK_WALL.getKey())
                .add(KHBlocks.SHELL_ROCK_WALL.getKey());
        // Плетень — деревянная ограда: топор, стыковка с ванильными оградами
        // и калитками (WOODEN_FENCES нужен, чтобы калитка вставала в линию).
        tag(BlockTags.MINEABLE_WITH_AXE)
                .add(KHBlocks.WATTLE.getKey())
                .add(KHBlocks.WATTLE_GATE.getKey());
        tag(BlockTags.FENCES).add(KHBlocks.WATTLE.getKey());
        tag(BlockTags.WOODEN_FENCES).add(KHBlocks.WATTLE.getKey());
        tag(BlockTags.FENCE_GATES).add(KHBlocks.WATTLE_GATE.getKey());
    }
}
