package dev.romankrukovsky.kubanhorizons.registry;

import dev.romankrukovsky.kubanhorizons.KubanHorizons;
import dev.romankrukovsky.kubanhorizons.util.KHIds;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Регистрация звуковых событий мода.
 * Файлы — собственный синтез, см. tools/soundgen и THIRD_PARTY_NOTICES.md.
 */
public final class KHSounds {
    private static final DeferredRegister<SoundEvent> SOUNDS =
            DeferredRegister.create(Registries.SOUND_EVENT, KubanHorizons.MOD_ID);

    /** Скрип винта маслопресса при ручном такте. */
    public static final DeferredHolder<SoundEvent, SoundEvent> OIL_PRESS_CREAK =
            register("block.oil_press.creak");

    /** Рабочий цикл маслопресса (пассивный режим). */
    public static final DeferredHolder<SoundEvent, SoundEvent> OIL_PRESS_WORK =
            register("block.oil_press.work");

    /** Завершение отжима: стук и капли масла. */
    public static final DeferredHolder<SoundEvent, SoundEvent> OIL_PRESS_FINISH =
            register("block.oil_press.finish");

    private static DeferredHolder<SoundEvent, SoundEvent> register(String name) {
        return SOUNDS.register(name.replace('.', '_'),
                () -> SoundEvent.createVariableRangeEvent(KHIds.of(name.replace('.', '_'))));
    }

    private KHSounds() {
    }

    public static void register(IEventBus modEventBus) {
        SOUNDS.register(modEventBus);
    }
}
