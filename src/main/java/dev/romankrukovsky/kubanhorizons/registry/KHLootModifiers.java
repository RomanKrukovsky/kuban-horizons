package dev.romankrukovsky.kubanhorizons.registry;

import com.mojang.serialization.MapCodec;
import dev.romankrukovsky.kubanhorizons.KubanHorizons;
import dev.romankrukovsky.kubanhorizons.loot.AddItemChanceModifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

/**
 * Регистрация сериализаторов глобальных loot-модификаторов.
 */
public final class KHLootModifiers {
    private static final DeferredRegister<MapCodec<? extends IGlobalLootModifier>> SERIALIZERS =
            DeferredRegister.create(NeoForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, KubanHorizons.MOD_ID);

    public static final Supplier<MapCodec<AddItemChanceModifier>> ADD_ITEM_CHANCE =
            SERIALIZERS.register("add_item_chance", () -> AddItemChanceModifier.CODEC);

    private KHLootModifiers() {
    }

    public static void register(IEventBus modEventBus) {
        SERIALIZERS.register(modEventBus);
    }
}
