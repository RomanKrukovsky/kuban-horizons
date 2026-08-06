package dev.romankrukovsky.kubanhorizons.datagen;

import dev.romankrukovsky.kubanhorizons.registry.KHItems;
import dev.romankrukovsky.kubanhorizons.util.KHIds;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementType;
import net.minecraft.advancements.triggers.InventoryChangeTrigger;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.advancements.AdvancementProvider;
import net.minecraft.data.advancements.AdvancementSubProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Дерево достижений. Ветка подсолнечника — первая ветвь корня «Кубанские
 * горизонты», далее ветки будут расширяться по CONTENT_BIBLE.md §9.
 */
public final class KHAdvancementProvider extends AdvancementProvider {
    public KHAdvancementProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries, List.of(new KubanAdvancements()));
    }

    static final class KubanAdvancements implements AdvancementSubProvider {
        @Override
        public void generate(HolderLookup.Provider registries, Consumer<AdvancementHolder> output) {
            AdvancementHolder root = Advancement.Builder.advancement()
                    .display(KHItems.SUNFLOWER_SEEDS.get(),
                            title("root"), description("root"),
                            Identifier.withDefaultNamespace("gui/advancements/backgrounds/husbandry"),
                            AdvancementType.TASK, false, false, false)
                    .addCriterion("has_seeds",
                            InventoryChangeTrigger.TriggerInstance.hasItems(KHItems.SUNFLOWER_SEEDS.get()))
                    .save(output, KHIds.of("root").toString());

            AdvancementHolder seeds = Advancement.Builder.advancement()
                    .parent(root)
                    .display(KHItems.SUNFLOWER_SEEDS.get(),
                            title("sunflower_seeds"), description("sunflower_seeds"),
                            null, AdvancementType.TASK, true, true, false)
                    .addCriterion("has_seeds",
                            InventoryChangeTrigger.TriggerInstance.hasItems(KHItems.SUNFLOWER_SEEDS.get()))
                    .save(output, KHIds.of("farming/sunflower_seeds").toString());

            AdvancementHolder head = Advancement.Builder.advancement()
                    .parent(seeds)
                    .display(KHItems.SUNFLOWER_HEAD.get(),
                            title("sunflower_head"), description("sunflower_head"),
                            null, AdvancementType.TASK, true, true, false)
                    .addCriterion("has_head",
                            InventoryChangeTrigger.TriggerInstance.hasItems(KHItems.SUNFLOWER_HEAD.get()))
                    .save(output, KHIds.of("farming/sunflower_head").toString());

            AdvancementHolder oil = Advancement.Builder.advancement()
                    .parent(head)
                    .display(KHItems.SUNFLOWER_OIL.get(),
                            title("sunflower_oil"), description("sunflower_oil"),
                            null, AdvancementType.GOAL, true, true, false)
                    .addCriterion("has_oil",
                            InventoryChangeTrigger.TriggerInstance.hasItems(KHItems.SUNFLOWER_OIL.get()))
                    .save(output, KHIds.of("farming/sunflower_oil").toString());

            Advancement.Builder.advancement()
                    .parent(seeds)
                    .display(KHItems.ROASTED_SUNFLOWER_SEEDS.get(),
                            title("roasted_seeds"), description("roasted_seeds"),
                            null, AdvancementType.TASK, true, true, false)
                    .addCriterion("has_roasted",
                            InventoryChangeTrigger.TriggerInstance.hasItems(KHItems.ROASTED_SUNFLOWER_SEEDS.get()))
                    .save(output, KHIds.of("farming/roasted_seeds").toString());

            // --- Ветка кухни ---
            AdvancementHolder kitchen = Advancement.Builder.advancement()
                    .parent(oil)
                    .display(KHItems.HOMEMADE_BREAD.get(),
                            title("kitchen"), description("kitchen"),
                            null, AdvancementType.TASK, true, true, false)
                    .addCriterion("has_bread",
                            InventoryChangeTrigger.TriggerInstance.hasItems(KHItems.HOMEMADE_BREAD.get()))
                    .save(output, KHIds.of("kitchen/homemade_bread").toString());

            Advancement.Builder.advancement()
                    .parent(kitchen)
                    .display(KHItems.BORSCHT.get(),
                            title("borscht"), description("borscht"),
                            null, AdvancementType.GOAL, true, true, false)
                    .addCriterion("has_borscht",
                            InventoryChangeTrigger.TriggerInstance.hasItems(KHItems.BORSCHT.get()))
                    .save(output, KHIds.of("kitchen/borscht").toString());

            Advancement.Builder.advancement()
                    .parent(kitchen)
                    .display(KHItems.TEA_CUP.get(),
                            title("tea_cup"), description("tea_cup"),
                            null, AdvancementType.TASK, true, true, false)
                    .addCriterion("has_tea",
                            InventoryChangeTrigger.TriggerInstance.hasItems(KHItems.TEA_CUP.get()))
                    .save(output, KHIds.of("kitchen/tea_cup").toString());

            // Дегустатор: попробовать все блюда кухни.
            Advancement.Builder.advancement()
                    .parent(kitchen)
                    .display(KHItems.HONEY_WALNUTS.get(),
                            title("taster"), description("taster"),
                            null, AdvancementType.CHALLENGE, true, true, false)
                    .addCriterion("has_borscht",
                            InventoryChangeTrigger.TriggerInstance.hasItems(KHItems.BORSCHT.get()))
                    .addCriterion("has_mamalyga",
                            InventoryChangeTrigger.TriggerInstance.hasItems(KHItems.MAMALYGA.get()))
                    .addCriterion("has_honey_walnuts",
                            InventoryChangeTrigger.TriggerInstance.hasItems(KHItems.HONEY_WALNUTS.get()))
                    .addCriterion("has_spread",
                            InventoryChangeTrigger.TriggerInstance.hasItems(KHItems.VEGETABLE_SPREAD.get()))
                    .save(output, KHIds.of("kitchen/taster").toString());
        }

        private static Component title(String key) {
            return Component.translatable("advancement.kubanhorizons." + key + ".title");
        }

        private static Component description(String key) {
            return Component.translatable("advancement.kubanhorizons." + key + ".description");
        }
    }
}
