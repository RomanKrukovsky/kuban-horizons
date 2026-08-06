package dev.romankrukovsky.kubanhorizons.datagen;

import dev.romankrukovsky.kubanhorizons.registry.KHItems;
import dev.romankrukovsky.kubanhorizons.util.KHIds;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementRequirements;
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
 * Дерево достижений. Корень «Кубанские горизонты» ветвится по CONTENT_BIBLE.md
 * §9: подсолнечник и кухня, рисоводство, виноградарство, чай, садоводство.
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

            // --- Ветка рисоводства ---
            AdvancementHolder riceSeedlings = Advancement.Builder.advancement()
                    .parent(root)
                    .display(KHItems.RICE_SEEDLINGS.get(),
                            title("rice_seedlings"), description("rice_seedlings"),
                            null, AdvancementType.TASK, true, true, false)
                    .addCriterion("has_seedlings",
                            InventoryChangeTrigger.TriggerInstance.hasItems(KHItems.RICE_SEEDLINGS.get()))
                    .save(output, KHIds.of("rice/rice_seedlings").toString());

            AdvancementHolder ricePanicle = Advancement.Builder.advancement()
                    .parent(riceSeedlings)
                    .display(KHItems.RICE_PANICLE.get(),
                            title("rice_panicle"), description("rice_panicle"),
                            null, AdvancementType.TASK, true, true, false)
                    .addCriterion("has_panicle",
                            InventoryChangeTrigger.TriggerInstance.hasItems(KHItems.RICE_PANICLE.get()))
                    .save(output, KHIds.of("rice/rice_panicle").toString());

            Advancement.Builder.advancement()
                    .parent(ricePanicle)
                    .display(KHItems.COOKED_RICE.get(),
                            title("cooked_rice"), description("cooked_rice"),
                            null, AdvancementType.GOAL, true, true, false)
                    .addCriterion("has_cooked_rice",
                            InventoryChangeTrigger.TriggerInstance.hasItems(KHItems.COOKED_RICE.get()))
                    .save(output, KHIds.of("rice/cooked_rice").toString());

            // --- Ветка виноградарства ---
            AdvancementHolder grapeCutting = Advancement.Builder.advancement()
                    .parent(root)
                    .display(KHItems.GRAPE_CUTTING.get(),
                            title("grape_cutting"), description("grape_cutting"),
                            null, AdvancementType.TASK, true, true, false)
                    .addCriterion("has_cutting",
                            InventoryChangeTrigger.TriggerInstance.hasItems(KHItems.GRAPE_CUTTING.get()))
                    .save(output, KHIds.of("vineyard/grape_cutting").toString());

            AdvancementHolder trellis = Advancement.Builder.advancement()
                    .parent(grapeCutting)
                    .display(KHItems.GRAPE_TRELLIS.get(),
                            title("grape_trellis"), description("grape_trellis"),
                            null, AdvancementType.TASK, true, true, false)
                    .addCriterion("has_trellis",
                            InventoryChangeTrigger.TriggerInstance.hasItems(KHItems.GRAPE_TRELLIS.get()))
                    .save(output, KHIds.of("vineyard/grape_trellis").toString());

            Advancement.Builder.advancement()
                    .parent(trellis)
                    .display(KHItems.GRAPES.get(),
                            title("grapes"), description("grapes"),
                            null, AdvancementType.GOAL, true, true, false)
                    .addCriterion("has_grapes",
                            InventoryChangeTrigger.TriggerInstance.hasItems(KHItems.GRAPES.get()))
                    .save(output, KHIds.of("vineyard/grapes").toString());

            // --- Ветка чая ---
            AdvancementHolder teaSapling = Advancement.Builder.advancement()
                    .parent(root)
                    .display(KHItems.TEA_SAPLING.get(),
                            title("tea_sapling"), description("tea_sapling"),
                            null, AdvancementType.TASK, true, true, false)
                    .addCriterion("has_sapling",
                            InventoryChangeTrigger.TriggerInstance.hasItems(KHItems.TEA_SAPLING.get()))
                    .save(output, KHIds.of("tea/tea_sapling").toString());

            AdvancementHolder teaLeaves = Advancement.Builder.advancement()
                    .parent(teaSapling)
                    .display(KHItems.TEA_LEAVES.get(),
                            title("tea_leaves"), description("tea_leaves"),
                            null, AdvancementType.TASK, true, true, false)
                    .addCriterion("has_leaves",
                            InventoryChangeTrigger.TriggerInstance.hasItems(KHItems.TEA_LEAVES.get()))
                    .save(output, KHIds.of("tea/tea_leaves").toString());

            AdvancementHolder driedTea = Advancement.Builder.advancement()
                    .parent(teaLeaves)
                    .display(KHItems.DRIED_TEA.get(),
                            title("dried_tea"), description("dried_tea"),
                            null, AdvancementType.TASK, true, true, false)
                    .addCriterion("has_dried_tea",
                            InventoryChangeTrigger.TriggerInstance.hasItems(KHItems.DRIED_TEA.get()))
                    .save(output, KHIds.of("tea/dried_tea").toString());

            // Чашка чая завершает чайную ветку, но сохраняет исторический id (AD-006).
            Advancement.Builder.advancement()
                    .parent(driedTea)
                    .display(KHItems.TEA_CUP.get(),
                            title("tea_cup"), description("tea_cup"),
                            null, AdvancementType.GOAL, true, true, false)
                    .addCriterion("has_tea",
                            InventoryChangeTrigger.TriggerInstance.hasItems(KHItems.TEA_CUP.get()))
                    .save(output, KHIds.of("kitchen/tea_cup").toString());

            // --- Ветка садоводства ---
            AdvancementHolder orchard = Advancement.Builder.advancement()
                    .parent(root)
                    .display(KHItems.PEACH_SAPLING.get(),
                            title("orchard"), description("orchard"),
                            null, AdvancementType.TASK, true, true, false)
                    .requirements(AdvancementRequirements.Strategy.OR)
                    .addCriterion("has_peach_sapling",
                            InventoryChangeTrigger.TriggerInstance.hasItems(KHItems.PEACH_SAPLING.get()))
                    .addCriterion("has_apricot_sapling",
                            InventoryChangeTrigger.TriggerInstance.hasItems(KHItems.APRICOT_SAPLING.get()))
                    .addCriterion("has_plum_sapling",
                            InventoryChangeTrigger.TriggerInstance.hasItems(KHItems.PLUM_SAPLING.get()))
                    .addCriterion("has_walnut_sapling",
                            InventoryChangeTrigger.TriggerInstance.hasItems(KHItems.WALNUT_SAPLING.get()))
                    .save(output, KHIds.of("orchard/sapling").toString());

            AdvancementHolder firstFruit = Advancement.Builder.advancement()
                    .parent(orchard)
                    .display(KHItems.PEACH.get(),
                            title("first_fruit"), description("first_fruit"),
                            null, AdvancementType.TASK, true, true, false)
                    .requirements(AdvancementRequirements.Strategy.OR)
                    .addCriterion("has_peach",
                            InventoryChangeTrigger.TriggerInstance.hasItems(KHItems.PEACH.get()))
                    .addCriterion("has_apricot",
                            InventoryChangeTrigger.TriggerInstance.hasItems(KHItems.APRICOT.get()))
                    .addCriterion("has_plum",
                            InventoryChangeTrigger.TriggerInstance.hasItems(KHItems.PLUM.get()))
                    .addCriterion("has_walnut",
                            InventoryChangeTrigger.TriggerInstance.hasItems(KHItems.WALNUT.get()))
                    .save(output, KHIds.of("orchard/first_fruit").toString());

            Advancement.Builder.advancement()
                    .parent(firstFruit)
                    .display(KHItems.DRIED_FRUIT.get(),
                            title("dried_fruit"), description("dried_fruit"),
                            null, AdvancementType.TASK, true, true, false)
                    .addCriterion("has_dried_fruit",
                            InventoryChangeTrigger.TriggerInstance.hasItems(KHItems.DRIED_FRUIT.get()))
                    .save(output, KHIds.of("orchard/dried_fruit").toString());

            // Кубанский сад: собрать плоды всех четырёх деревьев.
            Advancement.Builder.advancement()
                    .parent(firstFruit)
                    .display(KHItems.WALNUT.get(),
                            title("kuban_orchard"), description("kuban_orchard"),
                            null, AdvancementType.CHALLENGE, true, true, false)
                    .addCriterion("has_peach",
                            InventoryChangeTrigger.TriggerInstance.hasItems(KHItems.PEACH.get()))
                    .addCriterion("has_apricot",
                            InventoryChangeTrigger.TriggerInstance.hasItems(KHItems.APRICOT.get()))
                    .addCriterion("has_plum",
                            InventoryChangeTrigger.TriggerInstance.hasItems(KHItems.PLUM.get()))
                    .addCriterion("has_walnut",
                            InventoryChangeTrigger.TriggerInstance.hasItems(KHItems.WALNUT.get()))
                    .save(output, KHIds.of("orchard/kuban_orchard").toString());
        }

        private static Component title(String key) {
            return Component.translatable("advancement.kubanhorizons." + key + ".title");
        }

        private static Component description(String key) {
            return Component.translatable("advancement.kubanhorizons." + key + ".description");
        }
    }
}
