package dev.romankrukovsky.kubanhorizons.datagen;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Единый реестр строк локализации.
 *
 * <p>Каждая запись содержит и русский, и английский перевод — язык не может
 * «отстать»: оба файла генерируются из одного источника, и пропуск перевода
 * невозможен по построению.</p>
 */
final class KHTranslations {
    record Entry(String english, String russian) {
    }

    static final Map<String, Entry> ALL = new LinkedHashMap<>();

    private static void add(String key, String english, String russian) {
        if (ALL.put(key, new Entry(english, russian)) != null) {
            throw new IllegalStateException("Дублирующийся ключ локализации: " + key);
        }
    }

    static {
        // --- Общее ---
        add("itemGroup.kubanhorizons", "Kuban Horizons", "Кубанские горизонты");

        // --- Блоки ---
        add("block.kubanhorizons.sunflower_crop", "Sunflower Crop", "Подсолнечник");
        add("block.kubanhorizons.oil_press", "Oil Press", "Маслопресс");

        // --- Предметы ---
        add("item.kubanhorizons.sunflower_seeds", "Sunflower Seeds", "Семечки подсолнечника");
        add("item.kubanhorizons.sunflower_head", "Sunflower Head", "Шляпка подсолнечника");
        add("item.kubanhorizons.sunflower_oil", "Bottle of Sunflower Oil", "Бутылка подсолнечного масла");
        add("item.kubanhorizons.oil_cake", "Oil Cake", "Жмых");
        add("item.kubanhorizons.roasted_sunflower_seeds", "Roasted Sunflower Seeds", "Жареные семечки");

        // --- Контейнеры ---
        add("container.kubanhorizons.oil_press", "Oil Press", "Маслопресс");

        // --- Субтитры звуков ---
        add("subtitles.kubanhorizons.oil_press.creak", "Oil press creaks", "Маслопресс скрипит");
        add("subtitles.kubanhorizons.oil_press.work", "Oil press works", "Маслопресс работает");
        add("subtitles.kubanhorizons.oil_press.finish", "Oil drips", "Капает масло");

        // --- Достижения ---
        add("advancement.kubanhorizons.root.title", "Kuban Horizons", "Кубанские горизонты");
        add("advancement.kubanhorizons.root.description",
                "From the black-earth steppe to the Black Sea",
                "От чернозёмной степи до Чёрного моря");
        add("advancement.kubanhorizons.sunflower_seeds.title", "Sunflower Seeds!", "Семечки!");
        add("advancement.kubanhorizons.sunflower_seeds.description",
                "Obtain sunflower seeds", "Раздобудьте семечки подсолнечника");
        add("advancement.kubanhorizons.sunflower_head.title", "First Harvest", "Первый урожай");
        add("advancement.kubanhorizons.sunflower_head.description",
                "Harvest a ripe sunflower", "Соберите созревший подсолнечник");
        add("advancement.kubanhorizons.sunflower_oil.title", "Gold of the Steppe", "Золото степи");
        add("advancement.kubanhorizons.sunflower_oil.description",
                "Press a bottle of sunflower oil", "Выжмите бутылку подсолнечного масла");
        add("advancement.kubanhorizons.roasted_seeds.title", "Crack, Crack", "Щёлк-щёлк");
        add("advancement.kubanhorizons.roasted_seeds.description",
                "Roast sunflower seeds", "Пожарьте семечки");

        // --- Конфигурация (переводы ключей) ---
        add("config.kubanhorizons.crops.growthSpeed", "Crop growth speed", "Скорость роста культур");
        add("config.kubanhorizons.fertility.enabled", "Soil fertility system", "Система плодородия почвы");
        add("config.kubanhorizons.irrigation.enabled", "Irrigation system", "Система орошения");
    }

    private KHTranslations() {
    }
}
