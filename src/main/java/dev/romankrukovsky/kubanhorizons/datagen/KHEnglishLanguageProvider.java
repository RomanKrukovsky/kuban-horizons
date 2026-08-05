package dev.romankrukovsky.kubanhorizons.datagen;

import dev.romankrukovsky.kubanhorizons.KubanHorizons;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.LanguageProvider;

/**
 * Генератор английской локализации из единого реестра {@link KHTranslations}.
 */
public final class KHEnglishLanguageProvider extends LanguageProvider {
    public KHEnglishLanguageProvider(PackOutput output) {
        super(output, KubanHorizons.MOD_ID, "en_us");
    }

    @Override
    protected void addTranslations() {
        KHTranslations.ALL.forEach((key, entry) -> add(key, entry.english()));
    }
}
