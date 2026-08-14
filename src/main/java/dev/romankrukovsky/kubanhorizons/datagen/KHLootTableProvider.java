package dev.romankrukovsky.kubanhorizons.datagen;

import dev.romankrukovsky.kubanhorizons.KubanHorizons;
import dev.romankrukovsky.kubanhorizons.crop.SunflowerCropBlock;
import dev.romankrukovsky.kubanhorizons.registry.KHBlocks;
import dev.romankrukovsky.kubanhorizons.registry.KHItems;
import net.minecraft.advancements.predicates.StatePropertiesPredicate;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.predicates.LootItemBlockStatePropertyCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * Loot tables мода.
 */
public final class KHLootTableProvider extends LootTableProvider {
    public KHLootTableProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, Set.of(),
                List.of(new SubProviderEntry(KHBlockLoot::new, LootContextParamSets.BLOCK)),
                registries);
    }

    static final class KHBlockLoot extends BlockLootSubProvider {
        KHBlockLoot(HolderLookup.Provider registries) {
            super(Set.of(), FeatureFlags.REGISTRY.allFlags(), registries);
        }

        @Override
        protected void generate() {
            dropSelf(KHBlocks.OIL_PRESS.get());
            dropSelf(KHBlocks.IRRIGATION_CHANNEL.get());
            dropSelf(KHBlocks.WATER_INTAKE.get());
            dropSelf(KHBlocks.DRYING_RACK.get());
            dropSelf(KHBlocks.HAND_MILL.get());
<<<<<<< Updated upstream
=======
            dropSelf(KHBlocks.CUTTING_BOARD.get());
            // Коптильня разбирается целиком. Дрова внутри при этом сгорают
            // безвозвратно — иначе переустановка блока была бы способом
            // «сохранить» топливо и механика снабжения потеряла бы вес.
            dropSelf(KHBlocks.SMOKEHOUSE.get());
            // Виноградный чан возвращается целиком, но недолитый сок при
            // разборе пропадает: он хранится числом, а не предметом, и
            // «переносить жидкость без тары», переставляя блок, нельзя.
            dropSelf(KHBlocks.GRAPE_PRESS.get());
            // Укрытие манула разбирается целиком: игрок должен свободно
            // переставлять его, подбирая место, где зверь согласится жить.
            dropSelf(KHBlocks.MANUL_SHELTER.get());
>>>>>>> Stashed changes

            // Чернозём. Выпадает собой без всяких условий: весь смысл яруса —
            // что найденную почву можно вывезти на свою ферму. Требование
            // шёлкового касания или особого инструмента убило бы механику.
            dropSelf(KHBlocks.CHERNOZEM.get());
            // Вспаханный чернозём отдаёт нетронутый чернозём, а не себя: у
            // грядки нет предмета, и без этой строки блок выпадал бы ничем.
            add(KHBlocks.CHERNOZEM_FARMLAND.get(),
                    block -> createSingleItemTable(KHBlocks.CHERNOZEM.get()));

            // Строительные блоки (этап 7). Плиты дают два предмета — иначе
            // разбор двойной плиты терял бы половину материала.
            dropSelf(KHBlocks.ADOBE_BRICKS.get());
            dropSelf(KHBlocks.ADOBE_BRICK_STAIRS.get());
            add(KHBlocks.ADOBE_BRICK_SLAB.get(),
                    createSlabItemTable(KHBlocks.ADOBE_BRICK_SLAB.get()));
            dropSelf(KHBlocks.ADOBE_BRICK_WALL.get());
            dropSelf(KHBlocks.SHELL_ROCK.get());
            dropSelf(KHBlocks.SHELL_ROCK_STAIRS.get());
            add(KHBlocks.SHELL_ROCK_SLAB.get(),
                    createSlabItemTable(KHBlocks.SHELL_ROCK_SLAB.get()));
            dropSelf(KHBlocks.SHELL_ROCK_WALL.get());
            dropSelf(KHBlocks.WHITEWASHED_PLASTER.get());
            dropSelf(KHBlocks.WHITEWASHED_PLASTER_STAIRS.get());
            add(KHBlocks.WHITEWASHED_PLASTER_SLAB.get(),
                    createSlabItemTable(KHBlocks.WHITEWASHED_PLASTER_SLAB.get()));
            dropSelf(KHBlocks.ROOF_TILES.get());
            dropSelf(KHBlocks.ROOF_TILE_STAIRS.get());
            add(KHBlocks.ROOF_TILE_SLAB.get(),
                    createSlabItemTable(KHBlocks.ROOF_TILE_SLAB.get()));
            dropSelf(KHBlocks.DECORATIVE_CERAMIC.get());
            dropSelf(KHBlocks.CARVED_WINDOW_CASING.get());
            dropSelf(KHBlocks.WATTLE.get());
            dropSelf(KHBlocks.WATTLE_GATE.get());

            // Двухблочные культуры: дроп только с нижней зрелой половины —
            // верхняя не дропает ничего (защита от двойного лута).
            addDoubleCropDrops(KHBlocks.SUNFLOWER_CROP.get(),
                    KHItems.SUNFLOWER_HEAD.get(), KHItems.SUNFLOWER_SEEDS.get(),
                    SunflowerCropBlock.MAX_AGE);
            addDoubleCropDrops(KHBlocks.CORN_CROP.get(),
                    KHItems.CORN_COB.get(), KHItems.CORN_KERNELS.get(),
                    dev.romankrukovsky.kubanhorizons.crop.CornCropBlock.MAX_AGE);

            // Чайный куст: при разрушении — саженец (лист собирается ПКМ).
            add(KHBlocks.TEA_BUSH.get(),
                    createSingleItemTable(KHItems.TEA_SAPLING.get()));

            // Томатный куст: при разрушении — семена (томаты собираются ПКМ).
            add(KHBlocks.TOMATO_BUSH.get(),
                    createSingleItemTable(KHItems.TOMATO_SEEDS.get()));

            // Шпалера: всегда сама шпалера; с лозой (AGE != 0) — черенок.
            add(KHBlocks.GRAPE_TRELLIS.get(), LootTable.lootTable()
                    .withPool(this.applyExplosionCondition(KHBlocks.GRAPE_TRELLIS.get(),
                            net.minecraft.world.level.storage.loot.LootPool.lootPool()
                                    .add(net.minecraft.world.level.storage.loot.entries.LootItem
                                            .lootTableItem(KHItems.GRAPE_TRELLIS.get()))))
                    .withPool(this.applyExplosionCondition(KHBlocks.GRAPE_TRELLIS.get(),
                            net.minecraft.world.level.storage.loot.LootPool.lootPool()
                                    .when(net.minecraft.world.level.storage.loot.predicates.InvertedLootItemCondition.invert(
                                            LootItemBlockStatePropertyCondition
                                                    .hasBlockStateProperties(KHBlocks.GRAPE_TRELLIS.get())
                                                    .setProperties(StatePropertiesPredicate.Builder.properties()
                                                            .hasProperty(dev.romankrukovsky.kubanhorizons.crop.GrapeTrellisBlock.AGE, 0))))
                                    .add(net.minecraft.world.level.storage.loot.entries.LootItem
                                            .lootTableItem(KHItems.GRAPE_CUTTING.get())))));

            // Плодовые деревья: листва — как ванильная (шанс саженца),
            // саженцы — сами себя.
            addFruitTree(KHBlocks.PEACH_LEAVES.get(), KHBlocks.PEACH_SAPLING.get());
            addFruitTree(KHBlocks.APRICOT_LEAVES.get(), KHBlocks.APRICOT_SAPLING.get());
            addFruitTree(KHBlocks.PLUM_LEAVES.get(), KHBlocks.PLUM_SAPLING.get());
            addFruitTree(KHBlocks.WALNUT_LEAVES.get(), KHBlocks.WALNUT_SAPLING.get());

            // Рис: зрелый — метёлки + рассада; незрелый — рассада.
            LootItemCondition.Builder ripeRice = LootItemBlockStatePropertyCondition
                    .hasBlockStateProperties(KHBlocks.RICE_CROP.get())
                    .setProperties(StatePropertiesPredicate.Builder.properties()
                            .hasProperty(dev.romankrukovsky.kubanhorizons.crop.RiceCropBlock.AGE,
                                    dev.romankrukovsky.kubanhorizons.crop.RiceCropBlock.MAX_AGE));
            add(KHBlocks.RICE_CROP.get(), this.createCropDrops(
                    KHBlocks.RICE_CROP.get(),
                    KHItems.RICE_PANICLE.get(),
                    KHItems.RICE_SEEDLINGS.get(),
                    ripeRice));
        }

        /**
         * Листва без блок-предмета: {@code createLeavesDrops} не подходит
         * (он дропает саму листву при shears/silk touch, а её предмет — air).
         * Дроп: шанс саженца + палки, как у ванильной листвы без инструмента.
         */
        private void addFruitTree(Block leaves, Block sapling) {
            var enchantments = this.registries.lookupOrThrow(
                    net.minecraft.core.registries.Registries.ENCHANTMENT);
            var fortune = enchantments.getOrThrow(
                    net.minecraft.world.item.enchantment.Enchantments.FORTUNE);

            var saplingEntry = ((net.minecraft.world.level.storage.loot.entries.LootPoolSingletonContainer.Builder<?>)
                    this.applyExplosionCondition(leaves,
                            net.minecraft.world.level.storage.loot.entries.LootItem.lootTableItem(sapling)))
                    .when(net.minecraft.world.level.storage.loot.predicates.BonusLevelTableCondition
                            .bonusLevelFlatChance(fortune, NORMAL_LEAVES_SAPLING_CHANCES));
            var stickEntry = ((net.minecraft.world.level.storage.loot.entries.LootPoolSingletonContainer.Builder<?>)
                    this.applyExplosionDecay(leaves,
                            net.minecraft.world.level.storage.loot.entries.LootItem
                                    .lootTableItem(net.minecraft.world.item.Items.STICK)
                                    .apply(net.minecraft.world.level.storage.loot.functions.SetItemCountFunction
                                            .setCount(net.minecraft.world.level.storage.loot.providers.number.UniformGenerator
                                                    .between(1.0F, 2.0F)))))
                    .when(net.minecraft.world.level.storage.loot.predicates.BonusLevelTableCondition
                            .bonusLevelFlatChance(fortune, 0.02F, 0.022222223F, 0.025F, 0.033333335F, 0.1F));

            add(leaves, LootTable.lootTable()
                    .withPool(net.minecraft.world.level.storage.loot.LootPool.lootPool().add(saplingEntry))
                    .withPool(net.minecraft.world.level.storage.loot.LootPool.lootPool().add(stickEntry)));
            dropSelf(sapling);
        }

        private void addDoubleCropDrops(Block crop, net.minecraft.world.item.Item product,
                net.minecraft.world.item.Item seeds, int maxAge) {
            LootItemCondition.Builder ripeLower = LootItemBlockStatePropertyCondition
                    .hasBlockStateProperties(crop)
                    .setProperties(StatePropertiesPredicate.Builder.properties()
                            .hasProperty(dev.romankrukovsky.kubanhorizons.crop.DoubleCropBlock.AGE, maxAge)
                            .hasProperty(dev.romankrukovsky.kubanhorizons.crop.DoubleCropBlock.HALF,
                                    DoubleBlockHalf.LOWER));
            add(crop, this.createCropDrops(crop, product, seeds, ripeLower));
        }

        @Override
        protected Iterable<Block> getKnownBlocks() {
            return BuiltInRegistries.BLOCK.entrySet().stream()
                    .filter(e -> e.getKey().identifier().getNamespace().equals(KubanHorizons.MOD_ID))
                    .map(Map.Entry::getValue)
                    .toList();
        }
    }
}
