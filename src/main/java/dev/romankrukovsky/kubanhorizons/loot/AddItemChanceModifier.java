package dev.romankrukovsky.kubanhorizons.loot;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.common.loot.LootModifier;

/**
 * Глобальный loot-модификатор: с шансом добавляет предмет к дропу.
 *
 * <p>Используется для получения семян культур мода из ванильных
 * источников (трава, sniffer-раскопки) до появления собственных биомов
 * и структур. Данные — {@code data/kubanhorizons/loot_modifiers/}.</p>
 *
 * @param conditionsIn условия применения (обычно совпадение loot table)
 * @param item         добавляемый предмет
 * @param chance       шанс добавления 0..1
 * @param count        количество при срабатывании
 */
public final class AddItemChanceModifier extends LootModifier {
    public static final MapCodec<AddItemChanceModifier> CODEC =
            RecordCodecBuilder.mapCodec(inst -> codecStart(inst).and(inst.group(
                    BuiltInRegistries.ITEM.holderByNameCodec().fieldOf("item").forGetter(m -> m.item),
                    com.mojang.serialization.Codec.floatRange(0.0F, 1.0F).fieldOf("chance").forGetter(m -> m.chance),
                    com.mojang.serialization.Codec.intRange(1, 64).optionalFieldOf("count", 1).forGetter(m -> m.count)
            )).apply(inst, AddItemChanceModifier::new));

    private final Holder<Item> item;
    private final float chance;
    private final int count;

    public AddItemChanceModifier(LootItemCondition[] conditionsIn, int priority,
            Holder<Item> item, float chance, int count) {
        super(conditionsIn, priority);
        this.item = item;
        this.chance = chance;
        this.count = count;
    }

    @Override
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
        if (context.getRandom().nextFloat() < this.chance) {
            generatedLoot.add(new ItemStack(this.item.value(), this.count));
        }
        return generatedLoot;
    }

    @Override
    public MapCodec<? extends IGlobalLootModifier> codec() {
        return CODEC;
    }
}
