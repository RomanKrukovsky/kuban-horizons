package dev.romankrukovsky.kubanhorizons.worldgen;

import com.mojang.datafixers.util.Pair;
import dev.romankrukovsky.kubanhorizons.util.KHIds;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderSet;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.worldgen.Pools;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.heightproviders.ConstantHeight;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.structures.JigsawStructure;

import java.util.List;

/** Небольшие ориентиры поймы и плавней, доступные только в их родных биомах. */
public final class KHStructures {
    public static final ResourceKey<StructureTemplatePool> FLOODPLAIN_FISHING_CAMP_POOL =
            ResourceKey.create(Registries.TEMPLATE_POOL, KHIds.of("floodplain_fishing_camp"));
    public static final ResourceKey<StructureTemplatePool> PLAVNI_REED_SHELTER_POOL =
            ResourceKey.create(Registries.TEMPLATE_POOL, KHIds.of("plavni_reed_shelter"));

    public static final ResourceKey<Structure> FLOODPLAIN_FISHING_CAMP =
            ResourceKey.create(Registries.STRUCTURE, KHIds.of("floodplain_fishing_camp"));
    public static final ResourceKey<Structure> PLAVNI_REED_SHELTER =
            ResourceKey.create(Registries.STRUCTURE, KHIds.of("plavni_reed_shelter"));

    private KHStructures() {
    }

    /** Пулы содержат по одной законченной детали: разрастание jigsaw здесь не требуется. */
    public static void bootstrapTemplatePools(BootstrapContext<StructureTemplatePool> context) {
        var empty = context.lookup(Registries.TEMPLATE_POOL).getOrThrow(Pools.EMPTY);
        context.register(FLOODPLAIN_FISHING_CAMP_POOL, new StructureTemplatePool(
                empty,
                List.of(Pair.of(StructurePoolElement.single(
                        "kubanhorizons:floodplain_fishing_camp"), 1)),
                StructureTemplatePool.Projection.RIGID));
        context.register(PLAVNI_REED_SHELTER_POOL, new StructureTemplatePool(
                empty,
                List.of(Pair.of(StructurePoolElement.single(
                        "kubanhorizons:plavni_reed_shelter"), 1)),
                StructureTemplatePool.Projection.RIGID));
    }

    public static void bootstrap(BootstrapContext<Structure> context) {
        HolderGetter<Biome> biomes = context.lookup(Registries.BIOME);
        HolderGetter<StructureTemplatePool> pools = context.lookup(Registries.TEMPLATE_POOL);

        context.register(FLOODPLAIN_FISHING_CAMP, landmark(
                new Structure.StructureSettings(HolderSet.direct(
                        biomes.getOrThrow(KHBiomes.RIVER_FLOODPLAIN))),
                pools.getOrThrow(FLOODPLAIN_FISHING_CAMP_POOL)));
        context.register(PLAVNI_REED_SHELTER, landmark(
                new Structure.StructureSettings(HolderSet.direct(
                        biomes.getOrThrow(KHBiomes.PLAVNI))),
                pools.getOrThrow(PLAVNI_REED_SHELTER_POOL)));

        // Дворец джиннии живёт в своём пакете, но регистрируется здесь:
        // RegistrySetBuilder принимает один bootstrap на реестр.
        dev.romankrukovsky.kubanhorizons.worldgen.palace.KHPalaceWorldgen
                .bootstrapStructure(context);
    }

    private static JigsawStructure landmark(Structure.StructureSettings settings,
            net.minecraft.core.Holder<StructureTemplatePool> pool) {
        return new JigsawStructure(
                settings,
                pool,
                1,
                ConstantHeight.of(VerticalAnchor.absolute(0)),
                false,
                Heightmap.Types.WORLD_SURFACE_WG);
    }
}
