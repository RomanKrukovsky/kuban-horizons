package dev.romankrukovsky.kubanhorizons.worldgen;

import dev.romankrukovsky.kubanhorizons.util.KHIds;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.worldgen.SurfaceRuleData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.Noises;
import net.minecraft.world.level.levelgen.SurfaceRules;
import net.minecraft.world.level.levelgen.VerticalAnchor;

/** Noise settings ванильного Overworld с поверхностями плавней, лимана и поймы. */
public final class KHNoiseSettings {
    public static final ResourceKey<NoiseGeneratorSettings> OVERWORLD =
            ResourceKey.create(Registries.NOISE_SETTINGS, KHIds.of("overworld"));

    private KHNoiseSettings() {
    }

    public static void bootstrap(BootstrapContext<NoiseGeneratorSettings> context) {
        NoiseGeneratorSettings vanilla = NoiseGeneratorSettings.overworld(context, false, false);
        HolderGetter<Biome> biomes = context.lookup(Registries.BIOME);

        // Ванильные идиомы проверки воды (см. SurfaceRuleData).
        SurfaceRules.ConditionSource aboveWater = SurfaceRules.waterBlockCheck(0, 0);

        SurfaceRules.RuleSource plavniWater = SurfaceRules.ifTrue(
                SurfaceRules.isBiome(biomes, KHBiomes.PLAVNI),
                SurfaceRules.ifTrue(
                        SurfaceRules.ON_FLOOR,
                        SurfaceRules.ifTrue(
                                SurfaceRules.yBlockCheck(VerticalAnchor.absolute(62), 0),
                                SurfaceRules.ifTrue(
                                        SurfaceRules.noiseCondition2d(Noises.SWAMP, 0.0),
                                        SurfaceRules.state(Blocks.WATER.defaultBlockState())))));
        SurfaceRules.RuleSource limanMud = SurfaceRules.ifTrue(
                SurfaceRules.isBiome(biomes, KHBiomes.LIMAN),
                SurfaceRules.ifTrue(
                        SurfaceRules.ON_FLOOR,
                        SurfaceRules.state(Blocks.MUD.defaultBlockState())));

        // Пойма: под водой — речной ил (грязь и глина полосами), на суше —
        // редкие песчано-глинистые наносы поверх обычного дёрна.
        SurfaceRules.RuleSource floodplainSilt = SurfaceRules.ifTrue(
                SurfaceRules.isBiome(biomes, KHBiomes.RIVER_FLOODPLAIN),
                SurfaceRules.ifTrue(
                        SurfaceRules.ON_FLOOR,
                        SurfaceRules.sequence(
                                SurfaceRules.ifTrue(
                                        SurfaceRules.not(aboveWater),
                                        SurfaceRules.sequence(
                                                SurfaceRules.ifTrue(
                                                        SurfaceRules.noiseCondition2d(Noises.SURFACE, -0.35, 0.15),
                                                        SurfaceRules.state(Blocks.MUD.defaultBlockState())),
                                                SurfaceRules.ifTrue(
                                                        SurfaceRules.noiseCondition2d(Noises.SURFACE, 0.55, 0.95),
                                                        SurfaceRules.state(Blocks.CLAY.defaultBlockState())))),
                                SurfaceRules.ifTrue(
                                        aboveWater,
                                        SurfaceRules.sequence(
                                                SurfaceRules.ifTrue(
                                                        SurfaceRules.noiseCondition2d(Noises.SURFACE, 0.75, 1.0),
                                                        SurfaceRules.state(Blocks.COARSE_DIRT.defaultBlockState())),
                                                SurfaceRules.ifTrue(
                                                        SurfaceRules.noiseCondition2d(Noises.SURFACE, -1.0, -0.85),
                                                        SurfaceRules.state(Blocks.CLAY.defaultBlockState())))))));

        context.register(OVERWORLD, new NoiseGeneratorSettings(
                vanilla.noiseSettings(),
                vanilla.defaultBlock(),
                vanilla.defaultFluid(),
                vanilla.noiseRouter(),
                SurfaceRules.sequence(plavniWater, limanMud, floodplainSilt,
                        SurfaceRuleData.overworld(biomes)),
                vanilla.spawnTarget(),
                vanilla.seaLevel(),
                vanilla.disableMobGeneration(),
                vanilla.aquifersEnabled(),
                vanilla.oreVeinsEnabled(),
                vanilla.useLegacyRandomSource()));

        // Форма измерения лампы живёт в своём классе, но регистрируется здесь:
        // RegistrySetBuilder принимает один bootstrap на реестр.
        dev.romankrukovsky.kubanhorizons.worldgen.dimension.KHEternalKubanNoise.bootstrap(context);
    }
}
