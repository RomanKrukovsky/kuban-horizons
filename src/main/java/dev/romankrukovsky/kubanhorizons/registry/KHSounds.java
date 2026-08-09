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

    public static final DeferredHolder<SoundEvent, SoundEvent> PHEASANT_AMBIENT = register("entity.pheasant.ambient");
    public static final DeferredHolder<SoundEvent, SoundEvent> PHEASANT_HURT = register("entity.pheasant.hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> PHEASANT_DEATH = register("entity.pheasant.death");
    public static final DeferredHolder<SoundEvent, SoundEvent> PHEASANT_FLUSH = register("entity.pheasant.flush");
    public static final DeferredHolder<SoundEvent, SoundEvent> QUAIL_AMBIENT = register("entity.quail.ambient");
    public static final DeferredHolder<SoundEvent, SoundEvent> QUAIL_HURT = register("entity.quail.hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> QUAIL_DEATH = register("entity.quail.death");
    public static final DeferredHolder<SoundEvent, SoundEvent> QUAIL_FLUSH = register("entity.quail.flush");
    public static final DeferredHolder<SoundEvent, SoundEvent> WILD_BOAR_AMBIENT = register("entity.wild_boar.ambient");
    public static final DeferredHolder<SoundEvent, SoundEvent> WILD_BOAR_HURT = register("entity.wild_boar.hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> WILD_BOAR_DEATH = register("entity.wild_boar.death");
    public static final DeferredHolder<SoundEvent, SoundEvent> NUTRIA_AMBIENT = register("entity.nutria.ambient");
    public static final DeferredHolder<SoundEvent, SoundEvent> NUTRIA_HURT = register("entity.nutria.hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> NUTRIA_DEATH = register("entity.nutria.death");
    public static final DeferredHolder<SoundEvent, SoundEvent> LOCUST_AMBIENT = register("entity.locust.ambient");
    public static final DeferredHolder<SoundEvent, SoundEvent> LOCUST_HURT = register("entity.locust.hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> CAUCASIAN_SHEPHERD_AMBIENT = register("entity.caucasian_shepherd.ambient");
    public static final DeferredHolder<SoundEvent, SoundEvent> CAUCASIAN_SHEPHERD_HURT = register("entity.caucasian_shepherd.hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> CAUCASIAN_SHEPHERD_DEATH = register("entity.caucasian_shepherd.death");
    public static final DeferredHolder<SoundEvent, SoundEvent> STURGEON_FLOP = register("entity.sturgeon.flop");
    public static final DeferredHolder<SoundEvent, SoundEvent> GULL_AMBIENT = register("entity.gull.ambient");
    public static final DeferredHolder<SoundEvent, SoundEvent> GULL_HURT = register("entity.gull.hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> HERON_AMBIENT = register("entity.heron.ambient");
    public static final DeferredHolder<SoundEvent, SoundEvent> HERON_HURT = register("entity.heron.hurt");

    public static final DeferredHolder<SoundEvent, SoundEvent> MANUL_AMBIENT = register("entity.manul.ambient");
    public static final DeferredHolder<SoundEvent, SoundEvent> MANUL_HISS = register("entity.manul.hiss");
    public static final DeferredHolder<SoundEvent, SoundEvent> MANUL_PURR = register("entity.manul.purr");
    public static final DeferredHolder<SoundEvent, SoundEvent> MANUL_HURT = register("entity.manul.hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> MANUL_DEATH = register("entity.manul.death");

    /** Суховей: шум сухого степного ветра над полем. */
    public static final DeferredHolder<SoundEvent, SoundEvent> DRY_WIND = register("weather.dry_wind");

    /** Магический щелчок Кубанской Джиннии при исполнении желаний. */
    public static final DeferredHolder<SoundEvent, SoundEvent> GENIE_SNAP = register("entity.genie.snap");

    // --- Атмосфера биомов ---
    // Четыре подписных биома мода генерировались с одними цветами в effects и
    // на слух не отличались от ванильных равнин. Петля даёт биому голос,
    // additions — редкое вкрапление, чтобы петля не превратилась в фон,
    // который перестают замечать.
    //
    // Событию петли нужен фиксированный радиус: createVariableRangeEvent
    // берёт радиус из громкости конкретного проигрывания, а атмосферная
    // петля играется как relative-звук «вокруг игрока», без источника в
    // мире, и переменный радиус для неё лишён смысла.

    /** Степь: сухой ветер по ковылю (бесшовная петля). */
    public static final DeferredHolder<SoundEvent, SoundEvent> AMBIENT_STEPPE_LOOP =
            register("ambient.steppe.loop");
    /** Степь: далёкий посвист ветра в бурьяне. */
    public static final DeferredHolder<SoundEvent, SoundEvent> AMBIENT_STEPPE_ADDITIONS =
            register("ambient.steppe.additions");

    /** Пойма: шелест листвы и тихий низ текущей воды (бесшовная петля). */
    public static final DeferredHolder<SoundEvent, SoundEvent> AMBIENT_FLOODPLAIN_LOOP =
            register("ambient.floodplain.loop");
    /** Пойма: плеск воды у берега. */
    public static final DeferredHolder<SoundEvent, SoundEvent> AMBIENT_FLOODPLAIN_ADDITIONS =
            register("ambient.floodplain.additions");

    /** Плавни: свист тростниковых стеблей над стоячей водой (петля). */
    public static final DeferredHolder<SoundEvent, SoundEvent> AMBIENT_PLAVNI_LOOP =
            register("ambient.plavni.loop");
    /** Плавни: хлопок крыльев в камыше. */
    public static final DeferredHolder<SoundEvent, SoundEvent> AMBIENT_PLAVNI_ADDITIONS =
            register("ambient.plavni.additions");

    /** Лиман: низкий гул открытой воды (бесшовная петля). */
    public static final DeferredHolder<SoundEvent, SoundEvent> AMBIENT_LIMAN_LOOP =
            register("ambient.liman.loop");
    /** Лиман: далёкий крик над отмелью. */
    public static final DeferredHolder<SoundEvent, SoundEvent> AMBIENT_LIMAN_ADDITIONS =
            register("ambient.liman.additions");

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
