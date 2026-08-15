package dev.romankrukovsky.kubanhorizons.genie.ecology;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.RandomSource;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Геном гибридного существа: набор наследуемых признаков и номер поколения.
 *
 * <p>Поколение потомка всегда равно {@code max(родители) + 1}; признаки
 * наследуются по менделевской схеме с мутациями (см. {@link #combine}).</p>
 */
public record Genome(Set<Trait> traits, int generation) {

    public static final Codec<Genome> CODEC = RecordCodecBuilder.create(
            instance -> instance.group(
                    Codec.list(Trait.CODEC).fieldOf("traits").forGetter(Genome::traitsAsList),
                    Codec.INT.fieldOf("generation").forGetter(Genome::generation)
            ).apply(instance, Genome::decode));

    private static final StreamCodec<ByteBuf, Trait> TRAIT_STREAM_CODEC =
            ByteBufCodecs.VAR_INT.map(Trait::byId, Trait::id);

    private static final StreamCodec<ByteBuf, Set<Trait>> TRAIT_SET_STREAM_CODEC =
            ByteBufCodecs.collection(HashSet::new, TRAIT_STREAM_CODEC)
                    .map(set -> set, HashSet::new);

    public static final StreamCodec<RegistryFriendlyByteBuf, Genome> STREAM_CODEC =
            StreamCodec.composite(
                    TRAIT_SET_STREAM_CODEC,
                    Genome::traits,
                    ByteBufCodecs.VAR_INT,
                    Genome::generation,
                    Genome::new);

    public static Genome of(Trait... traits) {
        EnumSet<Trait> set = EnumSet.noneOf(Trait.class);
        set.addAll(Arrays.asList(traits));
        return new Genome(set, 0);
    }

    /**
     * Менделевское скрещивание двух геномов.
     *
     * <p>Признак, присутствующий у обоих родителей, наследуется в 90% случаев и
     * мутирует в остальные 10%; признак одного родителя достаётся половине
     * потомков; признак, отсутствующий у обоих, может появиться спонтанно в 2%
     * случаев. Поколение потомка — {@code max(a, b) + 1}.</p>
     */
    public static Genome combine(Genome a, Genome b, RandomSource random) {
        EnumSet<Trait> result = EnumSet.noneOf(Trait.class);
        for (Trait trait : Trait.values()) {
            boolean inA = a.traits.contains(trait);
            boolean inB = b.traits.contains(trait);
            if (inA && inB) {
                if (random.nextDouble() < 0.90D) {
                    result.add(trait);
                } else {
                    result.add(Trait.weightedRandom(random));
                }
            } else if (inA || inB) {
                if (random.nextDouble() < 0.50D) {
                    result.add(trait);
                }
            } else if (random.nextDouble() < 0.02D) {
                result.add(Trait.weightedRandom(random));
            }
        }
        return new Genome(result, Math.max(a.generation, b.generation) + 1);
    }

    public boolean has(Trait trait) {
        return traits.contains(trait);
    }

    private static List<Trait> traitsAsList(Genome genome) {
        return genome.traits.stream().toList();
    }

    private static Genome decode(List<Trait> traits, int generation) {
        EnumSet<Trait> set = EnumSet.noneOf(Trait.class);
        set.addAll(traits);
        return new Genome(set, generation);
    }
}
