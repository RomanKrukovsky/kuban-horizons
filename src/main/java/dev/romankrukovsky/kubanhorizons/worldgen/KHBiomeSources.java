package dev.romankrukovsky.kubanhorizons.worldgen;

import com.mojang.serialization.MapCodec;
import dev.romankrukovsky.kubanhorizons.KubanHorizons;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.biome.BiomeSource;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Статические codecs источников биомов мода. */
public final class KHBiomeSources {
    private static final DeferredRegister<MapCodec<? extends BiomeSource>> SOURCES =
            DeferredRegister.create(Registries.BIOME_SOURCE, KubanHorizons.MOD_ID);

    public static final DeferredHolder<MapCodec<? extends BiomeSource>, MapCodec<KubanBiomeSource>> KUBAN =
            SOURCES.register("kuban", () -> KubanBiomeSource.CODEC);

    private KHBiomeSources() {
    }

    public static void register(IEventBus modEventBus) {
        SOURCES.register(modEventBus);
    }
}
