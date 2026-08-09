package dev.romankrukovsky.kubanhorizons.datagen;

import dev.romankrukovsky.kubanhorizons.entity.ManulCriteria;
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

            generateManul(output, root);
            generateFishing(output, root);
            generateCrafts(output, root);
        }

        /**
         * Ветка рыболовства: от первой рыбы до копчёностей.
         *
         * <p>Сырьё, снасти и коптильня были в моде давно, а вести цепочку
         * им было некуда: ветка стояла в спеке как план, хотя всё нужное
         * лежало готовым. Достижение здесь не награда за труд, а указатель:
         * без него игрок мог годами не узнать, что рыбу можно копить не
         * только жаркой.</p>
         *
         * <p>Вход — сырой осётр, а не ведро: ведро требует ведра, а рыбу
         * можно добыть удочкой сразу. Ветка ведёт к копчению, потому что
         * это единственная переработка, дающая долгий запас — то есть
         * настоящая причина ловить больше, чем съешь сегодня.</p>
         */
        private static void generateFishing(Consumer<AdvancementHolder> output,
                                         AdvancementHolder root) {
            AdvancementHolder firstCatch = Advancement.Builder.advancement()
                    .parent(root)
                    .display(KHItems.RAW_STURGEON.get(),
                            title("first_sturgeon"), description("first_sturgeon"),
                            null, AdvancementType.TASK, true, true, false)
                    .addCriterion("has_raw_sturgeon",
                            InventoryChangeTrigger.TriggerInstance.hasItems(
                                    KHItems.RAW_STURGEON.get()))
                    .save(output, KHIds.of("fishing/first_sturgeon").toString());

            // Жарка и копчение — две ветви от одной рыбы, а не цепочка:
            // это разные решения игрока, и ни одно не предваряет другое.
            Advancement.Builder.advancement()
                    .parent(firstCatch)
                    .display(KHItems.COOKED_STURGEON.get(),
                            title("cooked_sturgeon"), description("cooked_sturgeon"),
                            null, AdvancementType.TASK, true, true, false)
                    .addCriterion("has_cooked_sturgeon",
                            InventoryChangeTrigger.TriggerInstance.hasItems(
                                    KHItems.COOKED_STURGEON.get()))
                    .save(output, KHIds.of("fishing/cooked_sturgeon").toString());

            AdvancementHolder smoked = Advancement.Builder.advancement()
                    .parent(firstCatch)
                    .display(KHItems.SMOKED_FISH.get(),
                            title("smoked_fish"), description("smoked_fish"),
                            null, AdvancementType.GOAL, true, true, false)
                    .addCriterion("has_smoked_fish",
                            InventoryChangeTrigger.TriggerInstance.hasItems(
                                    KHItems.SMOKED_FISH.get()))
                    .save(output, KHIds.of("fishing/smoked_fish").toString());

            // Живая рыба в ведре — отдельная затея: её носят не ради еды,
            // а чтобы завести своё, поэтому узел висит на копчении как
            // признак хозяйского подхода к промыслу.
            Advancement.Builder.advancement()
                    .parent(smoked)
                    .display(KHItems.STURGEON_BUCKET.get(),
                            title("sturgeon_bucket"), description("sturgeon_bucket"),
                            null, AdvancementType.CHALLENGE, true, true, false)
                    .addCriterion("has_sturgeon_bucket",
                            InventoryChangeTrigger.TriggerInstance.hasItems(
                                    KHItems.STURGEON_BUCKET.get()))
                    .save(output, KHIds.of("fishing/sturgeon_bucket").toString());
        }

        /**
         * Ветка ремесла: региональные строительные материалы.
         *
         * <p>Тридцать два предмета — саман, ракушечник, побелка, черепица,
         * плетень, резные наличники — существовали без единого достижения.
         * Игрок мог собрать всю усадьбу и не получить ни одного знака, что
         * прошёл заметный путь.</p>
         *
         * <p>Вход через саман: это первый и самый дешёвый материал набора.
         * Итог — челлендж на четыре материала сразу (OR здесь был бы
         * бессмысленным: «дом из чего-нибудь одного» — не усадьба).</p>
         */
        private static void generateCrafts(Consumer<AdvancementHolder> output,
                                         AdvancementHolder root) {
            AdvancementHolder adobe = Advancement.Builder.advancement()
                    .parent(root)
                    .display(KHItems.ADOBE_BRICKS.get(),
                            title("adobe"), description("adobe"),
                            null, AdvancementType.TASK, true, true, false)
                    .addCriterion("has_adobe",
                            InventoryChangeTrigger.TriggerInstance.hasItems(
                                    KHItems.ADOBE_BRICKS.get()))
                    .save(output, KHIds.of("crafts/adobe").toString());

            AdvancementHolder whitewash = Advancement.Builder.advancement()
                    .parent(adobe)
                    .display(KHItems.WHITEWASHED_PLASTER.get(),
                            title("whitewash"), description("whitewash"),
                            null, AdvancementType.TASK, true, true, false)
                    .addCriterion("has_plaster",
                            InventoryChangeTrigger.TriggerInstance.hasItems(
                                    KHItems.WHITEWASHED_PLASTER.get()))
                    .save(output, KHIds.of("crafts/whitewash").toString());

            // Усадьба: все четыре материала разом. Strategy по умолчанию —
            // AND, то есть нужны все критерии, и это здесь принципиально.
            Advancement.Builder.advancement()
                    .parent(whitewash)
                    .display(KHItems.ROOF_TILES.get(),
                            title("homestead"), description("homestead"),
                            null, AdvancementType.CHALLENGE, true, true, false)
                    .addCriterion("has_roof_tiles",
                            InventoryChangeTrigger.TriggerInstance.hasItems(
                                    KHItems.ROOF_TILES.get()))
                    .addCriterion("has_shell_rock",
                            InventoryChangeTrigger.TriggerInstance.hasItems(
                                    KHItems.SHELL_ROCK.get()))
                    .addCriterion("has_wattle",
                            InventoryChangeTrigger.TriggerInstance.hasItems(
                                    KHItems.WATTLE.get()))
                    .addCriterion("has_casing",
                            InventoryChangeTrigger.TriggerInstance.hasItems(
                                    KHItems.CARVED_WINDOW_CASING.get()))
                    .save(output, KHIds.of("crafts/homestead").toString());
        }

        /**
         * Ветка манула: от случайной встречи до постоянного жителя двора.
         *
         * <p>Корень ветки — наблюдение, а не предмет: манула нельзя
         * «раздобыть», и любое условие через инвентарь тут было бы ложью.
         * Поэтому ветка растёт от «Не трогай кота» — единственного, что
         * игрок может сделать со зверем, ничего не имея: посмотреть на него
         * и не подойти.</p>
         *
         * <p>Порядок узлов повторяет порядок в игре: наблюдение → доверие →
         * расселение. Секретное «Кубанский» висит отдельной веткой от того
         * же корня, потому что редкий окрас можно встретить в любой момент,
         * в том числе до всякого доверия.</p>
         */
        private static void generateManul(Consumer<AdvancementHolder> output,
                                         AdvancementHolder root) {
            // «Не трогай кота» — вход в ветку: выдержка вместо предмета.
            AdvancementHolder observed = Advancement.Builder.advancement()
                    .parent(root)
                    .display(KHItems.MANUL_SPAWN_EGG.get(),
                            title("manul_observed"), description("manul_observed"),
                            null, AdvancementType.TASK, true, true, false)
                    .addCriterion("observed_wild_manul",
                            ManulCriteria.criterion(ManulCriteria.MANUL_OBSERVED))
                    .save(output, KHIds.of("manul/observed").toString());

            // «Манул тебя терпит» — максимум доверия: цель, а не задача.
            AdvancementHolder trusted = Advancement.Builder.advancement()
                    .parent(observed)
                    .display(KHItems.MANUL_SHELTER.get(),
                            title("manul_trusted"), description("manul_trusted"),
                            null, AdvancementType.GOAL, true, true, false)
                    .addCriterion("max_trust",
                            ManulCriteria.criterion(ManulCriteria.MANUL_TRUSTED))
                    .save(output, KHIds.of("manul/trusted").toString());

            // «Опора станицы» — зверь поселился во дворе: завершение ветки.
            Advancement.Builder.advancement()
                    .parent(trusted)
                    .display(KHItems.MANUL_SHELTER.get(),
                            title("manul_settled"), description("manul_settled"),
                            null, AdvancementType.CHALLENGE, true, true, false)
                    .addCriterion("settled_near_homestead",
                            ManulCriteria.criterion(ManulCriteria.MANUL_SETTLED))
                    .save(output, KHIds.of("manul/settled").toString());

            // Секретное «Кубанский»: hidden=true — узел не виден в дереве,
            // пока не выполнен, поэтому редкий окрас остаётся находкой.
            Advancement.Builder.advancement()
                    .parent(observed)
                    .display(KHItems.MANUL_SPAWN_EGG.get(),
                            title("manul_silver"), description("manul_silver"),
                            null, AdvancementType.CHALLENGE, true, true, true)
                    .addCriterion("met_silver_manul",
                            ManulCriteria.criterion(ManulCriteria.MANUL_SILVER))
                    .save(output, KHIds.of("manul/silver").toString());
        }

        private static Component title(String key) {
            return Component.translatable("advancement.kubanhorizons." + key + ".title");
        }

        private static Component description(String key) {
            return Component.translatable("advancement.kubanhorizons." + key + ".description");
        }
    }
}
