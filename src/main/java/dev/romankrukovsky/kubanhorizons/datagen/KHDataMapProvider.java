package dev.romankrukovsky.kubanhorizons.datagen;

import dev.romankrukovsky.kubanhorizons.registry.KHItems;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.DataMapProvider;
import net.neoforged.neoforge.registries.datamaps.builtin.Compostable;
import net.neoforged.neoforge.registries.datamaps.builtin.NeoForgeDataMaps;

import java.util.concurrent.CompletableFuture;

/**
 * Data maps: компостируемость предметов мода.
 */
public final class KHDataMapProvider extends DataMapProvider {
    public KHDataMapProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider) {
        super(output, lookupProvider);
    }

    @Override
    protected void gather(HolderLookup.Provider provider) {
        var compostables = builder(NeoForgeDataMaps.COMPOSTABLES);
        compostables.add(KHItems.SUNFLOWER_SEEDS.get().builtInRegistryHolder(), new Compostable(0.3F), false);
        compostables.add(KHItems.SUNFLOWER_HEAD.get().builtInRegistryHolder(), new Compostable(0.65F), false);
        compostables.add(KHItems.OIL_CAKE.get().builtInRegistryHolder(), new Compostable(0.5F), false);
    }
}
