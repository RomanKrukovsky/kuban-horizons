package dev.romankrukovsky.kubanhorizons.worldgen;

import dev.romankrukovsky.kubanhorizons.util.KHIds;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.worldgen.biome.OverworldBiomes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.carver.ConfiguredWorldCarver;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

/** Datapack-биомы Kuban Horizons. */
public final class KHBiomes {
    public static final ResourceKey<Biome> KUBAN_STEPPE =
            ResourceKey.create(Registries.BIOME, KHIds.of("kuban_steppe"));

    private KHBiomes() {
    }

    public static void bootstrap(BootstrapContext<Biome> context) {
        HolderGetter<PlacedFeature> features = context.lookup(Registries.PLACED_FEATURE);
        HolderGetter<ConfiguredWorldCarver<?>> carvers = context.lookup(Registries.CONFIGURED_CARVER);

        // Солнечная степь наследует полный безопасный набор генерации равнин:
        // руды, озёра, траву, цветы, животных и редкие поля подсолнухов.
        context.register(KUBAN_STEPPE, OverworldBiomes.plains(features, carvers, true, false, false));
    }
}
