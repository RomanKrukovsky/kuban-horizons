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
        compostables.add(KHItems.CORN_KERNELS.get().builtInRegistryHolder(), new Compostable(0.3F), false);
        compostables.add(KHItems.CORN_COB.get().builtInRegistryHolder(), new Compostable(0.5F), false);
        compostables.add(KHItems.TEA_LEAVES.get().builtInRegistryHolder(), new Compostable(0.3F), false);
        compostables.add(KHItems.TEA_SAPLING.get().builtInRegistryHolder(), new Compostable(0.3F), false);
        compostables.add(KHItems.RICE_SEEDLINGS.get().builtInRegistryHolder(), new Compostable(0.3F), false);
        compostables.add(KHItems.RICE_PANICLE.get().builtInRegistryHolder(), new Compostable(0.5F), false);
        compostables.add(KHItems.RICE.get().builtInRegistryHolder(), new Compostable(0.3F), false);
        compostables.add(KHItems.GRAPE_CUTTING.get().builtInRegistryHolder(), new Compostable(0.3F), false);
        compostables.add(KHItems.GRAPES.get().builtInRegistryHolder(), new Compostable(0.5F), false);
        compostables.add(KHItems.TOMATO_SEEDS.get().builtInRegistryHolder(), new Compostable(0.3F), false);
        compostables.add(KHItems.TOMATO.get().builtInRegistryHolder(), new Compostable(0.5F), false);
        compostables.add(KHItems.PEACH.get().builtInRegistryHolder(), new Compostable(0.65F), false);
        compostables.add(KHItems.APRICOT.get().builtInRegistryHolder(), new Compostable(0.65F), false);
        compostables.add(KHItems.PLUM.get().builtInRegistryHolder(), new Compostable(0.65F), false);
        compostables.add(KHItems.WALNUT.get().builtInRegistryHolder(), new Compostable(0.65F), false);
        compostables.add(KHItems.PEACH_SAPLING.get().builtInRegistryHolder(), new Compostable(0.3F), false);
        compostables.add(KHItems.APRICOT_SAPLING.get().builtInRegistryHolder(), new Compostable(0.3F), false);
        compostables.add(KHItems.PLUM_SAPLING.get().builtInRegistryHolder(), new Compostable(0.3F), false);
        compostables.add(KHItems.WALNUT_SAPLING.get().builtInRegistryHolder(), new Compostable(0.3F), false);
    }
}
