package dev.romankrukovsky.kubanhorizons.worldgen;

import dev.romankrukovsky.kubanhorizons.crop.GrapeTrellisBlock;
import dev.romankrukovsky.kubanhorizons.crop.RiceCropBlock;
import dev.romankrukovsky.kubanhorizons.crop.TeaBushBlock;
import dev.romankrukovsky.kubanhorizons.crop.TomatoBushBlock;
import dev.romankrukovsky.kubanhorizons.registry.KHBlocks;
import dev.romankrukovsky.kubanhorizons.util.KHIds;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.SimpleBlockConfiguration;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;

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
    }
}
