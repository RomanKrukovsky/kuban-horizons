package dev.romankrukovsky.kubanhorizons.registry;

import com.mojang.serialization.Codec;
import dev.romankrukovsky.kubanhorizons.KubanHorizons;
import dev.romankrukovsky.kubanhorizons.vessel.VesselBond;
import dev.romankrukovsky.kubanhorizons.vessel.VesselType;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.UnaryOperator;

/**
 * Data Components для хранения состояния сосудов и других модовых данных.
 */
public final class KHDataComponents {
    public static final DeferredRegister<DataComponentType<?>> REGISTRY =
        DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, KubanHorizons.MOD_ID);

    /** Привязка сосуда к владельцу */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<VesselBond>> VESSEL_BOND =
        register("vessel_bond", builder -> builder
            .persistent(VesselBond.CODEC)
            .networkSynchronized(VesselBond.STREAM_CODEC)
            .cacheEncoding()
        );

    /** Тип сосуда */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<VesselType>> VESSEL_TYPE =
        register("vessel_type", builder -> builder
            .persistent(Codec.STRING.xmap(VesselType::fromId, VesselType::getId))
            .networkSynchronized(Codec.STRING.xmap(VesselType::fromId, VesselType::getId))
        );

    private static <T> DeferredHolder<DataComponentType<?>, DataComponentType<T>> register(
            String name,
            UnaryOperator<DataComponentType.Builder<T>> builder) {
        return REGISTRY.register(name, () -> builder.apply(DataComponentType.builder()).build());
    }

    public static void register(IEventBus modEventBus) {
        REGISTRY.register(modEventBus);
    }
}
