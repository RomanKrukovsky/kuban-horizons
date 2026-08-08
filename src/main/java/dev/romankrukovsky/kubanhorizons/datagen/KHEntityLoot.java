package dev.romankrukovsky.kubanhorizons.datagen;

import dev.romankrukovsky.kubanhorizons.registry.KHEntities;
import dev.romankrukovsky.kubanhorizons.registry.KHItems;
import java.util.stream.Stream;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.loot.EntityLootSubProvider;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.EnchantedCountIncreaseFunction;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.functions.SmeltItemFunction;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;

/**
 * Loot tables региональной фауны.
 *
 * <p>Без этого класса убитое животное не даёт ничего, а его мясо остаётся
 * недостижимым предметом: зарегистрированным, но не выпадающим ниоткуда. Поэтому
 * добавление существа без loot — молчаливый сбой, а не отсутствие фичи.</p>
 *
 * <p>{@link #getKnownEntityTypes()} сужен до сущностей мода: базовый провайдер
 * иначе требует таблицу для каждого ванильного типа и падает на первом же.</p>
 */
final class KHEntityLoot extends EntityLootSubProvider {
    KHEntityLoot(HolderLookup.Provider registries) {
        super(FeatureFlags.REGISTRY.allFlags(), registries);
    }

    @Override
    protected Stream<EntityType<?>> getKnownEntityTypes() {
        return Stream.of(
                KHEntities.PHEASANT.get(),
                KHEntities.QUAIL.get(),
                KHEntities.WILD_BOAR.get(),
                KHEntities.NUTRIA.get(),
                KHEntities.LOCUST.get(),
                KHEntities.CAUCASIAN_SHEPHERD.get(),
                KHEntities.STURGEON.get(),
                KHEntities.GULL.get(),
                KHEntities.HERON.get(),
                KHEntities.MANUL.get());
    }

    @Override
    public void generate() {
        // Дичь: мясо с шансом поджарки от огня, как у ванильных животных.
        addMeat(KHEntities.PHEASANT.get(), KHItems.RAW_PHEASANT.get(), 1.0F, 2.0F);
        addMeat(KHEntities.QUAIL.get(), KHItems.RAW_QUAIL.get(), 1.0F, 1.0F);
        addMeat(KHEntities.WILD_BOAR.get(), KHItems.RAW_BOAR.get(), 2.0F, 4.0F);
        addMeat(KHEntities.STURGEON.get(), KHItems.RAW_STURGEON.get(), 1.0F, 2.0F);
        addMeat(KHEntities.HERON.get(), KHItems.RAW_PHEASANT.get(), 1.0F, 2.0F);
        addMeat(KHEntities.GULL.get(), KHItems.RAW_QUAIL.get(), 0.0F, 1.0F);

        // Нутрия ценна шкурой, а не мясом — ремесленное сырьё.
        add(KHEntities.NUTRIA.get(), LootTable.lootTable()
                .withPool(LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1.0F))
                        .add(LootItem.lootTableItem(KHItems.NUTRIA_PELT.get())
                                .apply(SetItemCountFunction.setCount(
                                        UniformGenerator.between(1.0F, 2.0F)))
                                .apply(EnchantedCountIncreaseFunction.lootingMultiplier(
                                        this.registries, UniformGenerator.between(0.0F, 1.0F))))));

        // Пустые таблицы там, где добыча была бы бессмысленной: саранча слишком
        // мелка, пчела и овчарка — не добыча. Таблица всё равно нужна: без неё
        // датаген падает, а сущность считается недоделанной.
        // Манул: убийство намеренно ничего не даёт. Талисман мода не должен
        // быть источником ресурсов, иначе его начнут "фармить".
        add(KHEntities.MANUL.get(), LootTable.lootTable());
        add(KHEntities.LOCUST.get(), LootTable.lootTable());
        add(KHEntities.CAUCASIAN_SHEPHERD.get(), LootTable.lootTable());
    }

    /** Мясная таблица: N..M единиц, поджаривается при смерти в огне. */
    private void addMeat(EntityType<?> type, net.minecraft.world.item.Item meat,
            float min, float max) {
        add(type, LootTable.lootTable()
                .withPool(LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1.0F))
                        .add(LootItem.lootTableItem(meat)
                                .apply(SetItemCountFunction.setCount(
                                        UniformGenerator.between(min, max)))
                                .apply(SmeltItemFunction.smelted().when(this.shouldSmeltLoot()))
                                .apply(EnchantedCountIncreaseFunction.lootingMultiplier(
                                        this.registries, UniformGenerator.between(0.0F, 1.0F))))));
    }
}
