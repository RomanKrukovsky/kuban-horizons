package dev.romankrukovsky.kubanhorizons.worldgen;

import dev.romankrukovsky.kubanhorizons.util.KHIds;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;

/** Публичные категории биомов для интеграции с контентом поймы и плавней. */
public final class KHBiomeTags {
    public static final TagKey<Biome> HAS_FLOODPLAIN_FISHING_CAMP = TagKey.create(
            Registries.BIOME, KHIds.of("has_structure/floodplain_fishing_camp"));
    public static final TagKey<Biome> HAS_PLAVNI_REED_SHELTER = TagKey.create(
            Registries.BIOME, KHIds.of("has_structure/plavni_reed_shelter"));

    private KHBiomeTags() {
    }
}
