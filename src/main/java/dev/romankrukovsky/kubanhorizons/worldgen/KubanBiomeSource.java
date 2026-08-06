package dev.romankrukovsky.kubanhorizons.worldgen;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;
import net.minecraft.world.level.biome.MultiNoiseBiomeSourceParameterList;

import java.util.stream.Stream;

/**
 * Проецирует ванильную multi-noise географию в четыре региональных биома.
 * Ванильный источник остаётся климатическим классификатором, но его biome ID наружу не выходят.
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
                    .forGetter(source -> source.liman),
            Biome.CODEC.fieldOf("floodplain")
                    .forGetter(source -> source.floodplain)
    ).apply(instance, KubanBiomeSource::new));

    private final Holder<MultiNoiseBiomeSourceParameterList> preset;
    private final Holder<Biome> steppe;
    private final Holder<Biome> plavni;
    private final Holder<Biome> liman;
    private final Holder<Biome> floodplain;
    private final MultiNoiseBiomeSource delegate;

    public KubanBiomeSource(Holder<MultiNoiseBiomeSourceParameterList> preset, Holder<Biome> steppe,
            Holder<Biome> plavni, Holder<Biome> liman, Holder<Biome> floodplain) {
        this.preset = preset;
        this.steppe = steppe;
        this.plavni = plavni;
        this.liman = liman;
        this.floodplain = floodplain;
        this.delegate = MultiNoiseBiomeSource.createFromPreset(preset);
    }

    @Override
    protected Stream<Holder<Biome>> collectPossibleBiomes() {
        return Stream.of(steppe, plavni, liman, floodplain);
    }

    @Override
    protected MapCodec<? extends BiomeSource> codec() {
        return KHBiomeSources.KUBAN.get();
    }

    @Override
    public Holder<Biome> getNoiseBiome(int x, int y, int z, Climate.Sampler sampler) {
        Holder<Biome> selected = delegate.getNoiseBiome(x, y, z, sampler);
        if (selected.is(Biomes.SWAMP)) {
            return plavni;
        }
        if (selected.is(Biomes.MANGROVE_SWAMP)) {
            return liman;
        }
        if (selected.is(Biomes.RIVER)) {
            return floodplain;
        }
        return steppe;
    }
}
