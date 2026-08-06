package dev.romankrukovsky.kubanhorizons.datagen;

import dev.romankrukovsky.kubanhorizons.KubanHorizons;
import dev.romankrukovsky.kubanhorizons.worldgen.KHBiomes;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.BiomeTagsProvider;
import net.minecraft.tags.BiomeTags;

import java.util.concurrent.CompletableFuture;

/** Ванильные категории для биомов мода. */
final class KHBiomeTagsProvider extends BiomeTagsProvider {
    KHBiomeTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries, KubanHorizons.MOD_ID);
    }

    @Override
    protected void addTags(HolderLookup.Provider registries) {
        tag(BiomeTags.IS_OVERWORLD)
                .add(KHBiomes.KUBAN_STEPPE)
                .add(KHBiomes.PLAVNI)
                .add(KHBiomes.LIMAN);
        tag(net.neoforged.neoforge.common.Tags.Biomes.IS_PLAINS).add(KHBiomes.KUBAN_STEPPE);
        tag(net.neoforged.neoforge.common.Tags.Biomes.IS_TEMPERATE)
                .add(KHBiomes.KUBAN_STEPPE)
                .add(KHBiomes.PLAVNI)
                .add(KHBiomes.LIMAN);
        tag(net.neoforged.neoforge.common.Tags.Biomes.IS_DRY).add(KHBiomes.KUBAN_STEPPE);
        tag(net.neoforged.neoforge.common.Tags.Biomes.IS_WET)
                .add(KHBiomes.PLAVNI)
                .add(KHBiomes.LIMAN);
        tag(net.neoforged.neoforge.common.Tags.Biomes.IS_SWAMP)
                .add(KHBiomes.PLAVNI)
                .add(KHBiomes.LIMAN);
        tag(net.neoforged.neoforge.common.Tags.Biomes.IS_AQUATIC).add(KHBiomes.LIMAN);
        tag(BiomeTags.HAS_VILLAGE_PLAINS).add(KHBiomes.KUBAN_STEPPE);
        tag(BiomeTags.HAS_MINESHAFT)
                .add(KHBiomes.KUBAN_STEPPE)
                .add(KHBiomes.PLAVNI)
                .add(KHBiomes.LIMAN);
        tag(BiomeTags.HAS_RUINED_PORTAL_STANDARD)
                .add(KHBiomes.KUBAN_STEPPE)
                .add(KHBiomes.LIMAN);
        tag(BiomeTags.HAS_RUINED_PORTAL_SWAMP).add(KHBiomes.PLAVNI);
        tag(BiomeTags.HAS_SWAMP_HUT).add(KHBiomes.PLAVNI);
        tag(BiomeTags.HAS_PILLAGER_OUTPOST).add(KHBiomes.KUBAN_STEPPE);
        tag(BiomeTags.HAS_STRONGHOLD)
                .add(KHBiomes.KUBAN_STEPPE)
                .add(KHBiomes.PLAVNI)
                .add(KHBiomes.LIMAN);
        tag(BiomeTags.HAS_TRIAL_CHAMBERS)
                .add(KHBiomes.KUBAN_STEPPE)
                .add(KHBiomes.PLAVNI)
                .add(KHBiomes.LIMAN);
    }
}
