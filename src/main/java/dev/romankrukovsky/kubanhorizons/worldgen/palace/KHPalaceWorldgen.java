package dev.romankrukovsky.kubanhorizons.worldgen.palace;

import dev.romankrukovsky.kubanhorizons.util.KHIds;
import dev.romankrukovsky.kubanhorizons.worldgen.KHBiomes;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadType;

/**
 * Datapack-регистрация дворца.
 *
 * <p>Дворец нужен в единственном экземпляре в начале координат. Размещение
 * задано огромным шагом сетки, поэтому во всём разумном диапазоне координат
 * существует только один чанк-кандидат, а
 * {@link GeniePalaceStructure#findGenerationPoint} дополнительно отклоняет
 * всё, кроме чанка (0, 0). Двух дворцов не появится даже теоретически.</p>
 */
public final class KHPalaceWorldgen {
    public static final ResourceKey<Structure> GENIE_PALACE =
            ResourceKey.create(Registries.STRUCTURE, KHIds.of("genie_palace"));

    public static final ResourceKey<StructureSet> GENIE_PALACES =
            ResourceKey.create(Registries.STRUCTURE_SET, KHIds.of("genie_palaces"));

    private KHPalaceWorldgen() {
    }

    public static void bootstrapStructure(BootstrapContext<Structure> context) {
        HolderGetter<Biome> biomes = context.lookup(Registries.BIOME);

        // Биом ограничения не даёт: внутри лампы биом всегда один, а сам
        // findGenerationPoint жёстко привязывает дворец к началу координат.
        context.register(GENIE_PALACE, new GeniePalaceStructure(
                new Structure.StructureSettings(HolderSet.direct(
                        biomes.getOrThrow(KHBiomes.KUBAN_STEPPE)))));
    }

    public static void bootstrapSet(BootstrapContext<StructureSet> context) {
        HolderGetter<Structure> structures = context.lookup(Registries.STRUCTURE);

        // spacing 4096 и separation 0: единственный кандидат на весь мир.
        context.register(GENIE_PALACES, new StructureSet(
                structures.getOrThrow(GENIE_PALACE),
                new RandomSpreadStructurePlacement(4096, 0, RandomSpreadType.LINEAR, 17420360)));
    }
}
