package dev.romankrukovsky.kubanhorizons.worldgen;

import dev.romankrukovsky.kubanhorizons.util.KHIds;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadType;

/** Разреженное размещение локальных ориентиров без превращения их в декорационный шум. */
public final class KHStructureSets {
    public static final ResourceKey<StructureSet> FLOODPLAIN_FISHING_CAMPS =
            ResourceKey.create(Registries.STRUCTURE_SET, KHIds.of("floodplain_fishing_camps"));
    public static final ResourceKey<StructureSet> PLAVNI_REED_SHELTERS =
            ResourceKey.create(Registries.STRUCTURE_SET, KHIds.of("plavni_reed_shelters"));

    private KHStructureSets() {
    }

    public static void bootstrap(BootstrapContext<StructureSet> context) {
        HolderGetter<Structure> structures = context.lookup(Registries.STRUCTURE);
        context.register(FLOODPLAIN_FISHING_CAMPS, new StructureSet(
                structures.getOrThrow(KHStructures.FLOODPLAIN_FISHING_CAMP),
                new RandomSpreadStructurePlacement(48, 16, RandomSpreadType.TRIANGULAR, 17420351)));
        context.register(PLAVNI_REED_SHELTERS, new StructureSet(
                structures.getOrThrow(KHStructures.PLAVNI_REED_SHELTER),
                new RandomSpreadStructurePlacement(44, 14, RandomSpreadType.TRIANGULAR, 17420352)));

        dev.romankrukovsky.kubanhorizons.worldgen.palace.KHPalaceWorldgen
                .bootstrapSet(context);
    }
}
