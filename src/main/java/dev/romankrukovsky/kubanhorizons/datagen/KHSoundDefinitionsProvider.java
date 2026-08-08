package dev.romankrukovsky.kubanhorizons.datagen;

import dev.romankrukovsky.kubanhorizons.KubanHorizons;
import dev.romankrukovsky.kubanhorizons.registry.KHSounds;
import dev.romankrukovsky.kubanhorizons.util.KHIds;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.SoundDefinitionsProvider;

/**
 * Генерация sounds.json.
 */
public final class KHSoundDefinitionsProvider extends SoundDefinitionsProvider {
    public KHSoundDefinitionsProvider(PackOutput output) {
        super(output, KubanHorizons.MOD_ID);
    }

    @Override
    public void registerSounds() {
        add(KHSounds.OIL_PRESS_CREAK.get(), definition()
                .subtitle("subtitles.kubanhorizons.oil_press.creak")
                .with(sound(KHIds.of("block/oil_press/creak"))));
        add(KHSounds.OIL_PRESS_WORK.get(), definition()
                .subtitle("subtitles.kubanhorizons.oil_press.work")
                .with(sound(KHIds.of("block/oil_press/work"))));
        add(KHSounds.OIL_PRESS_FINISH.get(), definition()
                .subtitle("subtitles.kubanhorizons.oil_press.finish")
                .with(sound(KHIds.of("block/oil_press/finish"))));
        add(KHSounds.GENIE_SNAP.get(), definition()
                .subtitle("subtitles.kubanhorizons.entity.genie.snap")
                .with(sound(KHIds.of("entity/genie/snap"))));

        // Фауна. Без этих определений зарегистрированные события звучат как
        // тишина: игрок видит птицу и не слышит её.
        bird("pheasant", KHSounds.PHEASANT_AMBIENT.get(), "ambient");
        bird("pheasant", KHSounds.PHEASANT_HURT.get(), "hurt");
        bird("pheasant", KHSounds.PHEASANT_DEATH.get(), "death");
        bird("pheasant", KHSounds.PHEASANT_FLUSH.get(), "flush");
        bird("quail", KHSounds.QUAIL_AMBIENT.get(), "ambient");
        bird("quail", KHSounds.QUAIL_HURT.get(), "hurt");
        bird("quail", KHSounds.QUAIL_DEATH.get(), "death");
        bird("quail", KHSounds.QUAIL_FLUSH.get(), "flush");

        // Фауна давления и водная живность. Та же схема: файл лежит в
        // entity/<вид>/<событие>.ogg, поэтому хелпер один на всех.
        bird("wild_boar", KHSounds.WILD_BOAR_AMBIENT.get(), "ambient");
        bird("wild_boar", KHSounds.WILD_BOAR_HURT.get(), "hurt");
        bird("wild_boar", KHSounds.WILD_BOAR_DEATH.get(), "death");
        bird("nutria", KHSounds.NUTRIA_AMBIENT.get(), "ambient");
        bird("nutria", KHSounds.NUTRIA_HURT.get(), "hurt");
        bird("nutria", KHSounds.NUTRIA_DEATH.get(), "death");
        bird("locust", KHSounds.LOCUST_AMBIENT.get(), "ambient");
        bird("locust", KHSounds.LOCUST_HURT.get(), "hurt");
        bird("caucasian_shepherd", KHSounds.CAUCASIAN_SHEPHERD_AMBIENT.get(), "ambient");
        bird("caucasian_shepherd", KHSounds.CAUCASIAN_SHEPHERD_HURT.get(), "hurt");
        bird("caucasian_shepherd", KHSounds.CAUCASIAN_SHEPHERD_DEATH.get(), "death");
        bird("sturgeon", KHSounds.STURGEON_FLOP.get(), "flop");
        bird("gull", KHSounds.GULL_AMBIENT.get(), "ambient");
        bird("gull", KHSounds.GULL_HURT.get(), "hurt");
        bird("heron", KHSounds.HERON_AMBIENT.get(), "ambient");
        bird("heron", KHSounds.HERON_HURT.get(), "hurt");

        // Погода: суховей играется как событие над полем, а не как реплика,
        // поэтому лежит вне entity/ и заводится отдельной строкой.
        add(KHSounds.DRY_WIND.get(), definition()
                .subtitle("subtitles.kubanhorizons.weather.dry_wind")
                .with(sound(KHIds.of("weather/dry_wind"))));
    }

    /** Определение одного голоса птицы: файл entity/<вид>/<событие>.ogg. */
    private void bird(String species, net.minecraft.sounds.SoundEvent event,
                      String action) {
        add(event, definition()
                .subtitle("subtitles.kubanhorizons." + species + "." + action)
                .with(sound(KHIds.of("entity/" + species + "/" + action))));
    }
}
