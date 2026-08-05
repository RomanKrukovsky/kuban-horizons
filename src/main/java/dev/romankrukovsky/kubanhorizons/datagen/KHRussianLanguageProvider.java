package dev.romankrukovsky.kubanhorizons.datagen;

import dev.romankrukovsky.kubanhorizons.KubanHorizons;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.LanguageProvider;

/**
 * Генератор русской локализации из единого реестра {@link KHTranslations}.
 */
public final class KHRussianLanguageProvider extends LanguageProvider {
    public KHRussianLanguageProvider(PackOutput output) {
        super(output, KubanHorizons.MOD_ID, "ru_ru");
    }

    @Override
    protected void addTranslations() {
        KHTranslations.ALL.forEach((key, entry) -> add(key, entry.russian()));
    }
}
