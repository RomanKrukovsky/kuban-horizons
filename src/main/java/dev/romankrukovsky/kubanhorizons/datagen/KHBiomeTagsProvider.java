package dev.romankrukovsky.kubanhorizons.datagen;

import dev.romankrukovsky.kubanhorizons.KubanHorizons;
import dev.romankrukovsky.kubanhorizons.worldgen.KHBiomes;
import dev.romankrukovsky.kubanhorizons.worldgen.KHBiomeTags;
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
                .add(KHBiomes.LIMAN)
                .add(KHBiomes.RIVER_FLOODPLAIN);
        // Теги публикуют смысловые категории для datapack-интеграций. Сами
        // структуры намеренно используют прямые holder'ы и не расширяются тегами.
        tag(KHBiomeTags.HAS_FLOODPLAIN_FISHING_CAMP).add(KHBiomes.RIVER_FLOODPLAIN);
        tag(KHBiomeTags.HAS_PLAVNI_REED_SHELTER).add(KHBiomes.PLAVNI);
        tag(net.neoforged.neoforge.common.Tags.Biomes.IS_PLAINS).add(KHBiomes.KUBAN_STEPPE);
        tag(net.neoforged.neoforge.common.Tags.Biomes.IS_TEMPERATE)
                .add(KHBiomes.KUBAN_STEPPE)
                .add(KHBiomes.PLAVNI)
                .add(KHBiomes.LIMAN)
                .add(KHBiomes.RIVER_FLOODPLAIN);
        tag(net.neoforged.neoforge.common.Tags.Biomes.IS_DRY).add(KHBiomes.KUBAN_STEPPE);
        tag(net.neoforged.neoforge.common.Tags.Biomes.IS_WET)
                .add(KHBiomes.PLAVNI)
                .add(KHBiomes.LIMAN)
                .add(KHBiomes.RIVER_FLOODPLAIN);
        tag(net.neoforged.neoforge.common.Tags.Biomes.IS_SWAMP)
                .add(KHBiomes.PLAVNI)
                .add(KHBiomes.LIMAN);
        tag(net.neoforged.neoforge.common.Tags.Biomes.IS_AQUATIC).add(KHBiomes.LIMAN);
        tag(net.neoforged.neoforge.common.Tags.Biomes.IS_LUSH).add(KHBiomes.RIVER_FLOODPLAIN);
        // Пойма — речной биом: наследует все ванильные категории рек.
        tag(BiomeTags.IS_RIVER).add(KHBiomes.RIVER_FLOODPLAIN);
        tag(BiomeTags.WATER_ON_MAP_OUTLINES).add(KHBiomes.RIVER_FLOODPLAIN);
        tag(BiomeTags.REDUCED_WATER_AMBIENT_SPAWNS).add(KHBiomes.RIVER_FLOODPLAIN);
        tag(BiomeTags.MORE_FREQUENT_DROWNED_SPAWNS).add(KHBiomes.RIVER_FLOODPLAIN);
        tag(BiomeTags.HAS_VILLAGE_PLAINS).add(KHBiomes.KUBAN_STEPPE);
        tag(BiomeTags.HAS_MINESHAFT)
                .add(KHBiomes.KUBAN_STEPPE)
                .add(KHBiomes.PLAVNI)
                .add(KHBiomes.LIMAN)
                .add(KHBiomes.RIVER_FLOODPLAIN);
        tag(BiomeTags.HAS_RUINED_PORTAL_STANDARD)
                .add(KHBiomes.KUBAN_STEPPE)
                .add(KHBiomes.LIMAN)
                .add(KHBiomes.RIVER_FLOODPLAIN);
        tag(BiomeTags.HAS_RUINED_PORTAL_SWAMP).add(KHBiomes.PLAVNI);
        tag(BiomeTags.HAS_SWAMP_HUT).add(KHBiomes.PLAVNI);
        tag(BiomeTags.HAS_PILLAGER_OUTPOST).add(KHBiomes.KUBAN_STEPPE);
        tag(BiomeTags.HAS_STRONGHOLD)
                .add(KHBiomes.KUBAN_STEPPE)
                .add(KHBiomes.PLAVNI)
                .add(KHBiomes.LIMAN)
                .add(KHBiomes.RIVER_FLOODPLAIN);
        tag(BiomeTags.HAS_TRIAL_CHAMBERS)
                .add(KHBiomes.KUBAN_STEPPE)
                .add(KHBiomes.PLAVNI)
                .add(KHBiomes.LIMAN)
                .add(KHBiomes.RIVER_FLOODPLAIN);
    }
}
