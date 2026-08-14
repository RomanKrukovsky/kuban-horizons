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

        // --- Второй пояс биомов ---
        //
        // Категории тут не украшение. Биом, который источник может вернуть,
        // но который не попал в #is_overworld и ванильные наборы структур,
        // становится дырой в мире: в нём не строятся деревни, не находятся
        // крепости, не работают datapack'и, рассчитывающие на категорию.
        // Раньше степь забирала себе все эти регионы и приносила свои теги
        // с собой — теперь каждый пояс обязан объявить их сам.
        newBiomes(BiomeTags.IS_OVERWORLD);
        // Ванильный портал и шахты уместны везде на поверхности.
        newBiomes(BiomeTags.HAS_MINESHAFT);
        newBiomes(BiomeTags.HAS_RUINED_PORTAL_STANDARD);
        // Крепость обязана быть достижима из любого биома генератора:
        // иначе игрок может родиться в мире, где до Края не добраться.
        // Тест kuban_stronghold_biomes проверяет это по всему списку.
        newBiomes(BiomeTags.HAS_STRONGHOLD);
        newBiomes(BiomeTags.HAS_TRIAL_CHAMBERS);

        // Лес: деревни лесного типа и лесные категории NeoForge.
        tag(BiomeTags.HAS_VILLAGE_TAIGA).add(KHBiomes.FOOTHILL_FOREST);
        tag(net.neoforged.neoforge.common.Tags.Biomes.IS_FOREST)
                .add(KHBiomes.FOOTHILL_FOREST)
                .add(KHBiomes.MOUNTAIN_FOREST)
                .add(KHBiomes.TEA_SLOPES);
        tag(net.neoforged.neoforge.common.Tags.Biomes.IS_DECIDUOUS_TREE)
                .add(KHBiomes.FOOTHILL_FOREST);

        // Горы: аванпосты и горные категории.
        tag(BiomeTags.HAS_PILLAGER_OUTPOST).add(KHBiomes.MOUNTAIN_FOREST);
        tag(net.neoforged.neoforge.common.Tags.Biomes.IS_MOUNTAIN)
                .add(KHBiomes.MOUNTAIN_FOREST);
        tag(net.neoforged.neoforge.common.Tags.Biomes.IS_MOUNTAIN_SLOPE)
                .add(KHBiomes.MOUNTAIN_FOREST);
        tag(net.neoforged.neoforge.common.Tags.Biomes.IS_WINDSWEPT)
                .add(KHBiomes.MOUNTAIN_FOREST);

        // Берега: ванильные категории пляжа — от них зависят обломки
        // кораблей и морские структуры, иначе побережье остаётся пустым.
        tag(BiomeTags.IS_BEACH)
                .add(KHBiomes.AZOV_COAST)
                .add(KHBiomes.BLACK_SEA_COAST);
        tag(BiomeTags.HAS_SHIPWRECK_BEACHED)
                .add(KHBiomes.AZOV_COAST)
                .add(KHBiomes.BLACK_SEA_COAST);
        tag(BiomeTags.HAS_BURIED_TREASURE)
                .add(KHBiomes.AZOV_COAST)
                .add(KHBiomes.BLACK_SEA_COAST);
        tag(net.neoforged.neoforge.common.Tags.Biomes.IS_BEACH)
                .add(KHBiomes.AZOV_COAST)
                .add(KHBiomes.BLACK_SEA_COAST);
        // Черноморский берег взят из каменистого берега ванили — сохраняем
        // и эту категорию, она про гальку и скалу у воды.
        tag(net.neoforged.neoforge.common.Tags.Biomes.IS_STONY_SHORES)
                .add(KHBiomes.BLACK_SEA_COAST);
        tag(net.neoforged.neoforge.common.Tags.Biomes.IS_SANDY)
                .add(KHBiomes.AZOV_COAST);

        // Виноградный пояс — сухой и жаркий, как саванна, откуда и взят.
        tag(net.neoforged.neoforge.common.Tags.Biomes.IS_DRY)
                .add(KHBiomes.VINEYARD_HILLS)
                .add(KHBiomes.AZOV_COAST);
        tag(net.neoforged.neoforge.common.Tags.Biomes.IS_HOT)
                .add(KHBiomes.VINEYARD_HILLS)
                .add(KHBiomes.TEA_SLOPES);
        tag(BiomeTags.HAS_VILLAGE_SAVANNA).add(KHBiomes.VINEYARD_HILLS);

        // Чайные склоны — влажные субтропики: джунглевые категории.
        tag(net.neoforged.neoforge.common.Tags.Biomes.IS_JUNGLE).add(KHBiomes.TEA_SLOPES);
        tag(net.neoforged.neoforge.common.Tags.Biomes.IS_WET).add(KHBiomes.TEA_SLOPES);
        tag(net.neoforged.neoforge.common.Tags.Biomes.IS_LUSH).add(KHBiomes.TEA_SLOPES);
        tag(BiomeTags.HAS_JUNGLE_TEMPLE).add(KHBiomes.TEA_SLOPES);

        // Умеренный климат: всё, кроме жаркого виноградного и чайного пояса.
        tag(net.neoforged.neoforge.common.Tags.Biomes.IS_TEMPERATE)
                .add(KHBiomes.FOOTHILL_FOREST)
                .add(KHBiomes.MOUNTAIN_FOREST)
                .add(KHBiomes.BLACK_SEA_COAST);
        // Горный лес поднимается выше границы леса — там холодно.
        tag(net.neoforged.neoforge.common.Tags.Biomes.IS_COLD)
                .add(KHBiomes.MOUNTAIN_FOREST);
    }

    /** Добавляет все шесть биомов второго пояса в общий для них тег. */
    private void newBiomes(net.minecraft.tags.TagKey<net.minecraft.world.level.biome.Biome> key) {
        tag(key)
                .add(KHBiomes.FOOTHILL_FOREST)
                .add(KHBiomes.MOUNTAIN_FOREST)
                .add(KHBiomes.AZOV_COAST)
                .add(KHBiomes.BLACK_SEA_COAST)
                .add(KHBiomes.VINEYARD_HILLS)
                .add(KHBiomes.TEA_SLOPES);
    }
}
