package dev.romankrukovsky.kubanhorizons.datagen;

import dev.romankrukovsky.kubanhorizons.KubanHorizons;
import dev.romankrukovsky.kubanhorizons.crop.SunflowerCropBlock;
import dev.romankrukovsky.kubanhorizons.registry.KHBlocks;
import dev.romankrukovsky.kubanhorizons.registry.KHItems;
import net.minecraft.advancements.predicates.StatePropertiesPredicate;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.predicates.LootItemBlockStatePropertyCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * Loot tables мода.
 */
public final class KHLootTableProvider extends LootTableProvider {
    public KHLootTableProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, Set.of(),
                List.of(new SubProviderEntry(KHBlockLoot::new, LootContextParamSets.BLOCK)),
                registries);
    }

    static final class KHBlockLoot extends BlockLootSubProvider {
        KHBlockLoot(HolderLookup.Provider registries) {
            super(Set.of(), FeatureFlags.REGISTRY.allFlags(), registries);
        }

        @Override
        protected void generate() {
            dropSelf(KHBlocks.OIL_PRESS.get());

            // Подсолнечник: дроп только с нижней зрелой половины —
            // верхняя половина не дропает ничего (защита от двойного лута).
            LootItemCondition.Builder ripeLower = LootItemBlockStatePropertyCondition
                    .hasBlockStateProperties(KHBlocks.SUNFLOWER_CROP.get())
                    .setProperties(StatePropertiesPredicate.Builder.properties()
                            .hasProperty(SunflowerCropBlock.AGE, SunflowerCropBlock.MAX_AGE)
                            .hasProperty(SunflowerCropBlock.HALF, DoubleBlockHalf.LOWER));
            LootTable.Builder builder = this.createCropDrops(
                    KHBlocks.SUNFLOWER_CROP.get(),
                    KHItems.SUNFLOWER_HEAD.get(),
                    KHItems.SUNFLOWER_SEEDS.get(),
                    ripeLower);
            add(KHBlocks.SUNFLOWER_CROP.get(), builder);
        }

        @Override
        protected Iterable<Block> getKnownBlocks() {
            return BuiltInRegistries.BLOCK.entrySet().stream()
                    .filter(e -> e.getKey().identifier().getNamespace().equals(KubanHorizons.MOD_ID))
                    .map(Map.Entry::getValue)
                    .toList();
        }
    }
}
