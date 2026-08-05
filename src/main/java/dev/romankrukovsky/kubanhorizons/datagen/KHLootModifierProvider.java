package dev.romankrukovsky.kubanhorizons.datagen;

import dev.romankrukovsky.kubanhorizons.loot.AddItemChanceModifier;
import dev.romankrukovsky.kubanhorizons.KubanHorizons;
import dev.romankrukovsky.kubanhorizons.registry.KHItems;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.neoforge.common.data.GlobalLootModifierProvider;
import net.neoforged.neoforge.common.loot.LootTableIdCondition;

import java.util.concurrent.CompletableFuture;

/**
 * Глобальные loot-модификаторы: способы получения семян культур мода
 * из ванильного мира (до появления собственных биомов и структур).
 */
public final class KHLootModifierProvider extends GlobalLootModifierProvider {
    public KHLootModifierProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries, KubanHorizons.MOD_ID);
    }

    @Override
    protected void start() {
        // Семечки подсолнечника — из ванильного подсолнуха.
        add("sunflower_seeds_from_sunflower", new AddItemChanceModifier(
                new LootItemCondition[]{lootTable("blocks/sunflower")},
                0, KHItems.SUNFLOWER_SEEDS.get().builtInRegistryHolder(), 1.0F, 2));

        // Зёрна кукурузы и семена томата — редкий дроп из короткой травы.
        add("corn_kernels_from_grass", new AddItemChanceModifier(
                new LootItemCondition[]{lootTable("blocks/short_grass")},
                0, KHItems.CORN_KERNELS.get().builtInRegistryHolder(), 0.05F, 1));
        add("tomato_seeds_from_grass", new AddItemChanceModifier(
                new LootItemCondition[]{lootTable("blocks/short_grass")},
                0, KHItems.TOMATO_SEEDS.get().builtInRegistryHolder(), 0.05F, 1));

        // Рассада риса — из тростника у воды.
        add("rice_seedlings_from_sugar_cane", new AddItemChanceModifier(
                new LootItemCondition[]{lootTable("blocks/sugar_cane")},
                0, KHItems.RICE_SEEDLINGS.get().builtInRegistryHolder(), 0.15F, 1));

        // Черенок винограда — из сладких ягод (аналог лианы).
        add("grape_cutting_from_sweet_berries", new AddItemChanceModifier(
                new LootItemCondition[]{lootTable("blocks/sweet_berry_bush")},
                0, KHItems.GRAPE_CUTTING.get().builtInRegistryHolder(), 0.1F, 1));

        // Саженец чая — из азалии (влажные предгорья).
        add("tea_sapling_from_azalea", new AddItemChanceModifier(
                new LootItemCondition[]{lootTable("blocks/azalea")},
                0, KHItems.TEA_SAPLING.get().builtInRegistryHolder(), 0.25F, 1));
    }

    private static LootItemCondition lootTable(String path) {
        return LootTableIdCondition.builder(Identifier.withDefaultNamespace(path)).build();
    }
}
