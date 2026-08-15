package dev.romankrukovsky.kubanhorizons.genie.ecology;

import com.mojang.serialization.Codec;
import net.minecraft.util.ByIdMap;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;

import java.util.Arrays;
import java.util.function.IntFunction;

/**
 * Наследуемый признак гибридного вида (Wishborne Ecology).
 *
 * <p>Каждый признак имеет вес (редкость проявления при спонтанной мутации) и
 * мутабельность (шанс измениться при наследовании). {@code weight} — не
 * нормированная вероятность: сумма весов используется как знаменатель при
 * выборе случайного признака.</p>
 */
public enum Trait implements StringRepresentable {
    FLIGHT(0, "flight", 6, 0.10D),
    GLOWING(1, "glowing", 10, 0.05D),
    SIZE_LARGE(2, "size_large", 4, 0.08D),
    FIRE_RESISTANT(3, "fire_resistant", 3, 0.06D),
    FAST(4, "fast", 8, 0.12D),
    STRONG(5, "strong", 5, 0.10D),
    AQUATIC(6, "aquatic", 4, 0.09D);

    public static final Codec<Trait> CODEC = StringRepresentable.fromEnum(() -> Trait.values());
    private static final Trait[] VALUES = values();
    private static final int TOTAL_WEIGHT = Arrays.stream(VALUES).mapToInt(Trait::weight).sum();

    private final int id;
    private final String name;
    private final int weight;
    private final double mutability;

    Trait(int id, String name, int weight, double mutability) {
        this.id = id;
        this.name = name;
        this.weight = weight;
        this.mutability = mutability;
    }

    public int id() {
        return id;
    }

    public int weight() {
        return weight;
    }

    public double mutability() {
        return mutability;
    }

    @Override
    public String getSerializedName() {
        return name;
    }

    public static Trait byId(int id) {
        IntFunction<Trait> lookup = ByIdMap.continuous(
                Trait::id, VALUES, ByIdMap.OutOfBoundsStrategy.ZERO);
        return lookup.apply(id);
    }

    /** Случайный признак, взвешенный по {@link #weight}. */
    public static Trait weightedRandom(RandomSource random) {
        int roll = random.nextInt(TOTAL_WEIGHT);
        int acc = 0;
        for (Trait trait : VALUES) {
            acc += trait.weight;
            if (roll < acc) {
                return trait;
            }
        }
        return VALUES[VALUES.length - 1];
    }
}