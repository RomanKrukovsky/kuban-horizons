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
    }
}
