package dev.romankrukovsky.kubanhorizons.worldgen;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;
import net.minecraft.world.level.biome.MultiNoiseBiomeSourceParameterList;

import java.util.stream.Stream;

/**
 * Компактная обёртка над ванильным multi-noise preset.
 * Подменяет выбранные ванильные аналоги на региональные биомы.
 */
public final class KubanBiomeSource extends BiomeSource {
    public static final MapCodec<KubanBiomeSource> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            MultiNoiseBiomeSourceParameterList.CODEC.fieldOf("preset")
                    .forGetter(source -> source.preset),
            Biome.CODEC.fieldOf("steppe")
                    .forGetter(source -> source.steppe),
            Biome.CODEC.fieldOf("plavni")
                    .forGetter(source -> source.plavni),
            Biome.CODEC.fieldOf("liman")
                    .forGetter(source -> source.liman)
    ).apply(instance, KubanBiomeSource::new));

    private final Holder<MultiNoiseBiomeSourceParameterList> preset;
    private final Holder<Biome> steppe;
    private final Holder<Biome> plavni;
    private final Holder<Biome> liman;
    private final MultiNoiseBiomeSource delegate;

    public KubanBiomeSource(Holder<MultiNoiseBiomeSourceParameterList> preset, Holder<Biome> steppe,
            Holder<Biome> plavni, Holder<Biome> liman) {
        this.preset = preset;
        this.steppe = steppe;
        this.plavni = plavni;
        this.liman = liman;
        this.delegate = MultiNoiseBiomeSource.createFromPreset(preset);
    }

    @Override
    protected Stream<Holder<Biome>> collectPossibleBiomes() {
        return Stream.concat(delegate.possibleBiomes().stream(), Stream.of(steppe, plavni, liman)).distinct();
    }

    @Override
    protected MapCodec<? extends BiomeSource> codec() {
        return KHBiomeSources.KUBAN.get();
    }

    @Override
    public Holder<Biome> getNoiseBiome(int x, int y, int z, Climate.Sampler sampler) {
        Holder<Biome> selected = delegate.getNoiseBiome(x, y, z, sampler);
        if (selected.is(net.minecraft.world.level.biome.Biomes.SUNFLOWER_PLAINS)) {
            return steppe;
        }
        if (selected.is(net.minecraft.world.level.biome.Biomes.SWAMP)) {
            return plavni;
        }
        if (selected.is(net.minecraft.world.level.biome.Biomes.MANGROVE_SWAMP)) {
            return liman;
        }
        return selected;
    }
}
