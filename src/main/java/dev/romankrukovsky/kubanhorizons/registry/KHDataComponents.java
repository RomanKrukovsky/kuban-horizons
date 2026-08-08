package dev.romankrukovsky.kubanhorizons.registry;

import dev.romankrukovsky.kubanhorizons.KubanHorizons;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Регистрация data components мода.
 */
public final class KHDataComponents {
    private static final DeferredRegister<DataComponentType<?>> COMPONENTS =
            DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, KubanHorizons.MOD_ID);

    /**
     * Захваченный регион мира внутри предмета (сжатие мира джиннией).
     *
     * <p>Хранит результат {@code RegionSnapshot.toTag()}: предмет физически
     * несёт в себе состояние блоков, покинувших мир.</p>
     */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<CompoundTag>> REGION_PAYLOAD =
            COMPONENTS.register("region_payload",
                    () -> DataComponentType.<CompoundTag>builder()
                            .persistent(CompoundTag.CODEC)
                            .build());

    private KHDataComponents() {
    }

    public static void register(IEventBus modEventBus) {
        COMPONENTS.register(modEventBus);
    }
}
