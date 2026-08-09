package dev.romankrukovsky.kubanhorizons.worldgen;

import dev.romankrukovsky.kubanhorizons.crop.FruitLeavesBlock;
import dev.romankrukovsky.kubanhorizons.crop.GrapeTrellisBlock;
import dev.romankrukovsky.kubanhorizons.crop.RiceCropBlock;
import dev.romankrukovsky.kubanhorizons.crop.TeaBushBlock;
import dev.romankrukovsky.kubanhorizons.crop.TomatoBushBlock;
import dev.romankrukovsky.kubanhorizons.registry.KHBlocks;
import dev.romankrukovsky.kubanhorizons.util.KHIds;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.SimpleBlockConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration;
import net.minecraft.world.level.levelgen.feature.featuresize.TwoLayersFeatureSize;
import net.minecraft.world.level.levelgen.feature.foliageplacers.BlobFoliagePlacer;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.minecraft.world.level.levelgen.feature.stateproviders.RandomizedIntStateProvider;
import net.minecraft.world.level.levelgen.feature.trunkplacers.StraightTrunkPlacer;

/**
 * Configured features мода: дикие культуры.
 *
 * <p>Дикие растения — второй (помимо loot-модификаторов) способ найти
 * культуры мода: одиночные дикие кусты в подходящих ванильных биомах,
 * по образцу ванильного sweet berry bush (SIMPLE_BLOCK + модификаторы
 * размещения). Регистрируются через datapack-реестры в datagen.</p>
 */
public final class KHConfiguredFeatures {
    public static final ResourceKey<ConfiguredFeature<?, ?>> WILD_TEA = createKey("wild_tea");
    public static final ResourceKey<ConfiguredFeature<?, ?>> WILD_TOMATO = createKey("wild_tomato");
    public static final ResourceKey<ConfiguredFeature<?, ?>> WILD_GRAPE = createKey("wild_grape");
    public static final ResourceKey<ConfiguredFeature<?, ?>> WILD_RICE = createKey("wild_rice");
    public static final ResourceKey<ConfiguredFeature<?, ?>> WILD_PEACH_TREE = createKey("wild_peach_tree");
    public static final ResourceKey<ConfiguredFeature<?, ?>> WILD_APRICOT_TREE = createKey("wild_apricot_tree");
    public static final ResourceKey<ConfiguredFeature<?, ?>> WILD_PLUM_TREE = createKey("wild_plum_tree");
    public static final ResourceKey<ConfiguredFeature<?, ?>> WILD_WALNUT_TREE = createKey("wild_walnut_tree");

    private KHConfiguredFeatures() {
    }

    private static ResourceKey<ConfiguredFeature<?, ?>> createKey(String name) {
        return ResourceKey.create(Registries.CONFIGURED_FEATURE, KHIds.of(name));
    }

    public static void bootstrap(BootstrapContext<ConfiguredFeature<?, ?>> context) {
        // Дикий чай: почти зрелый куст (стадия 2).
        context.register(WILD_TEA, new ConfiguredFeature<>(Feature.SIMPLE_BLOCK,
                new SimpleBlockConfiguration(BlockStateProvider.simple(
                        KHBlocks.TEA_BUSH.get().defaultBlockState()
                                .setValue(TeaBushBlock.AGE, 2)))));

        // Дикий томат.
        context.register(WILD_TOMATO, new ConfiguredFeature<>(Feature.SIMPLE_BLOCK,
                new SimpleBlockConfiguration(BlockStateProvider.simple(
                        KHBlocks.TOMATO_BUSH.get().defaultBlockState()
                                .setValue(TomatoBushBlock.AGE, 2)))));

        // Одичавшая виноградная шпалера с лозой (след старых виноградников).
        context.register(WILD_GRAPE, new ConfiguredFeature<>(Feature.SIMPLE_BLOCK,
                new SimpleBlockConfiguration(BlockStateProvider.simple(
                        KHBlocks.GRAPE_TRELLIS.get().defaultBlockState()
                                .setValue(GrapeTrellisBlock.AGE, 3)))));

        // Дикий рис поймы: зрелая метёлка в затопленном мелководье.
        // Состояние waterlogged обязательно — блок сохраняет воду вокруг себя.
        context.register(WILD_RICE, new ConfiguredFeature<>(Feature.SIMPLE_BLOCK,
                new SimpleBlockConfiguration(BlockStateProvider.simple(
                        KHBlocks.RICE_CROP.get().defaultBlockState()
                                .setValue(RiceCropBlock.AGE, RiceCropBlock.MAX_AGE)
                                .setValue(RiceCropBlock.WATERLOGGED, true)))));

        // Дикие плодовые деревья — единственный вход в садовый контур.
        //
        // Без них ветка была замкнута сама на себя: саженец выпадает только
        // из плодовой листвы, а листва бывает только у дерева, выросшего из
        // саженца. Ни рецепта, ни торговли, ни лута сундуков на саженцы не
        // было, поэтому четыре культуры (персик, абрикос, слива, орех) вместе
        // с достижением «сад» физически нельзя было получить в выживании.
        // Одичавшее дерево в биоме и есть тот первый саженец.
        HolderGetter<Biome> biomes = context.lookup(Registries.BIOME);
        BlockStateProvider belowTrunk = TreeConfiguration.defaultPlaceBelowTreeTrunkProvider(biomes);

        fruitTree(context, WILD_PEACH_TREE, KHBlocks.PEACH_LEAVES.get(), belowTrunk);
        fruitTree(context, WILD_APRICOT_TREE, KHBlocks.APRICOT_LEAVES.get(), belowTrunk);
        fruitTree(context, WILD_PLUM_TREE, KHBlocks.PLUM_LEAVES.get(), belowTrunk);
        fruitTree(context, WILD_WALNUT_TREE, KHBlocks.WALNUT_LEAVES.get(), belowTrunk);
    }

    /**
     * Одичавшее плодовое дерево: дубовый ствол + крона плодовой листвы.
     *
     * <p>Форма — ванильный дуб ({@code createStraightBlobTree}), чтобы дикое
     * дерево читалось как дерево, а не как куст: ствол 4–6 брёвен и шаровая
     * крона радиусом 2. Ствол дубовый, потому что у мода нет своих брёвен, и
     * ровно из таких же брёвен строит дерево {@code FruitSaplingBlock} —
     * выросшее и найденное дерево выглядят одинаково.</p>
     *
     * <p>Стадия плодоношения задаётся случайно ({@code AGE 0..2}), иначе все
     * дикие деревья стояли бы разом либо пустыми, либо в плодах. Листва
     * ставится {@code PERSISTENT}, чтобы крона не осыпалась от дубового
     * ствола: {@code DISTANCE} у чужой листвы не считается родным деревом.</p>
     */
    private static void fruitTree(BootstrapContext<ConfiguredFeature<?, ?>> context,
            ResourceKey<ConfiguredFeature<?, ?>> key, Block leaves,
            BlockStateProvider belowTrunk) {
        BlockStateProvider foliage = new RandomizedIntStateProvider(
                BlockStateProvider.simple(leaves.defaultBlockState()
                        .setValue(LeavesBlock.PERSISTENT, true)),
                FruitLeavesBlock.AGE, UniformInt.of(0, FruitLeavesBlock.MAX_AGE));

        context.register(key, new ConfiguredFeature<>(Feature.TREE,
                new TreeConfiguration.TreeConfigurationBuilder(
                        BlockStateProvider.simple(Blocks.OAK_LOG),
                        new StraightTrunkPlacer(4, 2, 0),
                        foliage,
                        new BlobFoliagePlacer(ConstantInt.of(2), ConstantInt.of(0), 3),
                        new TwoLayersFeatureSize(1, 0, 1),
                        belowTrunk)
                        .ignoreVines()
                        .build()));
    }
}
