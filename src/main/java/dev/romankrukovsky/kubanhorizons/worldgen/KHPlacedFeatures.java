package dev.romankrukovsky.kubanhorizons.worldgen;

import dev.romankrukovsky.kubanhorizons.util.KHIds;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.worldgen.placement.PlacementUtils;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.blockpredicates.BlockPredicate;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.placement.BiomeFilter;
import net.minecraft.world.level.levelgen.placement.BlockPredicateFilter;
import net.minecraft.world.level.levelgen.placement.CountPlacement;
import net.minecraft.world.level.levelgen.placement.InSquarePlacement;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.placement.RandomOffsetPlacement;
import net.minecraft.world.level.levelgen.placement.RarityFilter;
import net.minecraft.world.level.material.Fluids;

import java.util.List;

/**
 * Placed features мода: правила размещения диких культур
 * (по образцу ванильного patch_berry).
 */
public final class KHPlacedFeatures {
    public static final ResourceKey<PlacedFeature> WILD_TEA_PLACED = createKey("wild_tea");
    public static final ResourceKey<PlacedFeature> WILD_TOMATO_PLACED = createKey("wild_tomato");
    public static final ResourceKey<PlacedFeature> WILD_GRAPE_PLACED = createKey("wild_grape");
    public static final ResourceKey<PlacedFeature> WILD_RICE_PLACED = createKey("wild_rice");

    private KHPlacedFeatures() {
    }

    private static ResourceKey<PlacedFeature> createKey(String name) {
        return ResourceKey.create(Registries.PLACED_FEATURE, KHIds.of(name));
    }

    public static void bootstrap(BootstrapContext<PlacedFeature> context) {
        HolderGetter<ConfiguredFeature<?, ?>> features =
                context.lookup(Registries.CONFIGURED_FEATURE);

        BlockPredicateFilter onGrass = BlockPredicateFilter.forPredicate(BlockPredicate.allOf(
                BlockPredicate.ONLY_IN_AIR_PREDICATE,
                BlockPredicate.matchesBlocks(Direction.DOWN.getUnitVec3i(),
                        Blocks.GRASS_BLOCK, Blocks.DIRT)));

        register(context, WILD_TEA_PLACED, features.getOrThrow(KHConfiguredFeatures.WILD_TEA), 24, 48, onGrass);
        register(context, WILD_TOMATO_PLACED, features.getOrThrow(KHConfiguredFeatures.WILD_TOMATO), 32, 32, onGrass);
        register(context, WILD_GRAPE_PLACED, features.getOrThrow(KHConfiguredFeatures.WILD_GRAPE), 48, 16, onGrass);

        // Дикий рис растёт только в мелководье поймы: сама позиция — вода
        // глубиной один блок над илистым дном (грязь, глина, земля).
        BlockPredicateFilter inShallowWater = BlockPredicateFilter.forPredicate(BlockPredicate.allOf(
                BlockPredicate.matchesFluids(Fluids.WATER),
                BlockPredicate.not(BlockPredicate.matchesFluids(
                        Direction.UP.getUnitVec3i(), Fluids.WATER)),
                BlockPredicate.matchesBlocks(Direction.DOWN.getUnitVec3i(),
                        Blocks.MUD, Blocks.CLAY, Blocks.DIRT)));

        context.register(WILD_RICE_PLACED, new PlacedFeature(
                features.getOrThrow(KHConfiguredFeatures.WILD_RICE),
                List.of(
                        RarityFilter.onAverageOnceEvery(4),
                        CountPlacement.of(24),
                        InSquarePlacement.spread(),
                        // Первая позиция над твёрдым дном — то есть в воде.
                        PlacementUtils.HEIGHTMAP_TOP_SOLID,
                        BiomeFilter.biome(),
                        inShallowWater)));
    }

    private static void register(BootstrapContext<PlacedFeature> context,
            ResourceKey<PlacedFeature> key, Holder<ConfiguredFeature<?, ?>> feature,
            int rarity, int count, BlockPredicateFilter predicate) {
        context.register(key, new PlacedFeature(feature, List.of(
                RarityFilter.onAverageOnceEvery(rarity),
                InSquarePlacement.spread(),
                PlacementUtils.HEIGHTMAP_WORLD_SURFACE,
                BiomeFilter.biome(),
                CountPlacement.of(count),
                RandomOffsetPlacement.ofTriangle(7, 3),
                predicate)));
    }
}
