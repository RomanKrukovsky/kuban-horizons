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
    }

    /** Определение одного голоса птицы: файл entity/<вид>/<событие>.ogg. */
    private void bird(String species, net.minecraft.sounds.SoundEvent event,
                      String action) {
        add(event, definition()
                .subtitle("subtitles.kubanhorizons." + species + "." + action)
                .with(sound(KHIds.of("entity/" + species + "/" + action))));
    }
}
