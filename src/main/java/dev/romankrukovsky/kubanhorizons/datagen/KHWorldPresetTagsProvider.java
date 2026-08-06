package dev.romankrukovsky.kubanhorizons.datagen;

import dev.romankrukovsky.kubanhorizons.KubanHorizons;
import dev.romankrukovsky.kubanhorizons.worldgen.KHWorldPresets;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.WorldPresetTagsProvider;
import net.minecraft.tags.WorldPresetTags;

import java.util.concurrent.CompletableFuture;

/** Делает мир Kuban Horizons доступным в стандартном экране создания мира. */
final class KHWorldPresetTagsProvider extends WorldPresetTagsProvider {
    KHWorldPresetTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries, KubanHorizons.MOD_ID);
    }

    @Override
    protected void addTags(HolderLookup.Provider registries) {
        tag(WorldPresetTags.NORMAL).add(KHWorldPresets.KUBAN_HORIZONS);
        tag(WorldPresetTags.EXTENDED).add(KHWorldPresets.KUBAN_HORIZONS);
    }
}
