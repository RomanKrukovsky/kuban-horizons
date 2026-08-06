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

/** Noise settings ванильного Overworld с поверхностями плавней и лимана. */
public final class KHNoiseSettings {
    public static final ResourceKey<NoiseGeneratorSettings> OVERWORLD =
            ResourceKey.create(Registries.NOISE_SETTINGS, KHIds.of("overworld"));

    private KHNoiseSettings() {
    }

    public static void bootstrap(BootstrapContext<NoiseGeneratorSettings> context) {
        NoiseGeneratorSettings vanilla = NoiseGeneratorSettings.overworld(context, false, false);
        HolderGetter<Biome> biomes = context.lookup(Registries.BIOME);

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

        context.register(OVERWORLD, new NoiseGeneratorSettings(
                vanilla.noiseSettings(),
                vanilla.defaultBlock(),
                vanilla.defaultFluid(),
                vanilla.noiseRouter(),
                SurfaceRules.sequence(plavniWater, limanMud, SurfaceRuleData.overworld(biomes)),
                vanilla.spawnTarget(),
                vanilla.seaLevel(),
                vanilla.disableMobGeneration(),
                vanilla.aquifersEnabled(),
                vanilla.oreVeinsEnabled(),
                vanilla.useLegacyRandomSource()));
    }
}
