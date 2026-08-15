package dev.romankrukovsky.kubanhorizons.registry;

import com.mojang.serialization.Codec;
import dev.romankrukovsky.kubanhorizons.KubanHorizons;
import dev.romankrukovsky.kubanhorizons.vessel.VesselBond;
import dev.romankrukovsky.kubanhorizons.vessel.VesselType;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.UUID;
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
            .networkSynchronized(ByteBufCodecs.STRING_UTF8.map(VesselType::fromId, VesselType::getId))
        );

    /**
     * Захваченный регион мира внутри предмета (сжатие мира джиннией).
     *
     * <p>Хранит результат {@code RegionSnapshot.toTag()}: предмет физически
     * несёт в себе состояние блоков, покинувших мир.</p>
     */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<CompoundTag>> REGION_PAYLOAD =
        register("region_payload", builder -> builder
            .persistent(CompoundTag.CODEC)
        );

    /**
     * UUID владельца, душу которого хранит осколок души.
     *
     * <p>Создаётся вариантом «Сохранить душу» протокола смерти владельца:
     * предмет фиксирует связь, пока игрок не вернётся в мир.</p>
     */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<UUID>> SOUL_OWNER =
        register("soul_owner", builder -> builder
            .persistent(UUIDUtil.CODEC)
            .networkSynchronized(UUIDUtil.STREAM_CODEC)
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
