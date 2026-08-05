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
        add("block.kubanhorizons.irrigation_channel", "Irrigation Channel", "Оросительный желоб");
        add("block.kubanhorizons.water_intake", "Water Intake", "Водозабор");
        add("block.kubanhorizons.corn_crop", "Corn", "Кукуруза");
        add("block.kubanhorizons.tea_bush", "Tea Bush", "Чайный куст");
        add("block.kubanhorizons.rice_crop", "Rice", "Рис");
        add("block.kubanhorizons.grape_trellis", "Grape Trellis", "Виноградная шпалера");
        add("block.kubanhorizons.tomato_bush", "Tomato Bush", "Томатный куст");
        add("block.kubanhorizons.peach_leaves", "Peach Leaves", "Персиковая листва");
        add("block.kubanhorizons.apricot_leaves", "Apricot Leaves", "Абрикосовая листва");
        add("block.kubanhorizons.plum_leaves", "Plum Leaves", "Сливовая листва");
        add("block.kubanhorizons.walnut_leaves", "Walnut Leaves", "Листва грецкого ореха");
        add("block.kubanhorizons.peach_sapling", "Peach Sapling", "Саженец персика");
        add("block.kubanhorizons.apricot_sapling", "Apricot Sapling", "Саженец абрикоса");
        add("block.kubanhorizons.plum_sapling", "Plum Sapling", "Саженец сливы");
        add("block.kubanhorizons.walnut_sapling", "Walnut Sapling", "Саженец грецкого ореха");

        // --- Предметы ---
        add("item.kubanhorizons.sunflower_seeds", "Sunflower Seeds", "Семечки подсолнечника");
        add("item.kubanhorizons.sunflower_head", "Sunflower Head", "Шляпка подсолнечника");
        add("item.kubanhorizons.sunflower_oil", "Bottle of Sunflower Oil", "Бутылка подсолнечного масла");
        add("item.kubanhorizons.oil_cake", "Oil Cake", "Жмых");
        add("item.kubanhorizons.roasted_sunflower_seeds", "Roasted Sunflower Seeds", "Жареные семечки");
        add("item.kubanhorizons.soil_probe", "Soil Probe", "Почвенный щуп");
        add("item.kubanhorizons.corn_kernels", "Corn Kernels", "Зёрна кукурузы");
        add("item.kubanhorizons.corn_cob", "Corn Cob", "Початок кукурузы");
        add("item.kubanhorizons.grilled_corn", "Grilled Corn", "Печёная кукуруза");
        add("item.kubanhorizons.tea_sapling", "Tea Sapling", "Саженец чая");
        add("item.kubanhorizons.tea_leaves", "Tea Leaves", "Чайный лист");
        add("item.kubanhorizons.rice_seedlings", "Rice Seedlings", "Рассада риса");
        add("item.kubanhorizons.rice_panicle", "Rice Panicle", "Рисовая метёлка");
        add("item.kubanhorizons.rice", "Rice", "Рис");
        add("item.kubanhorizons.cooked_rice", "Bowl of Cooked Rice", "Миска отварного риса");
        add("item.kubanhorizons.grape_cutting", "Grape Cutting", "Черенок винограда");
        add("item.kubanhorizons.grapes", "Grapes", "Виноград");
        add("item.kubanhorizons.tomato_seeds", "Tomato Seeds", "Семена томата");
        add("item.kubanhorizons.tomato", "Tomato", "Томат");
        add("item.kubanhorizons.peach_sapling", "Peach Sapling", "Саженец персика");
        add("item.kubanhorizons.apricot_sapling", "Apricot Sapling", "Саженец абрикоса");
        add("item.kubanhorizons.plum_sapling", "Plum Sapling", "Саженец сливы");
        add("item.kubanhorizons.walnut_sapling", "Walnut Sapling", "Саженец грецкого ореха");
        add("item.kubanhorizons.peach", "Peach", "Персик");
        add("item.kubanhorizons.apricot", "Apricot", "Абрикос");
        add("item.kubanhorizons.plum", "Plum", "Слива");
        add("item.kubanhorizons.walnut", "Walnut", "Грецкий орех");

        // --- Сообщения ---
        add("message.kubanhorizons.soil_probe.result",
                "Fertility: %s (%s), moisture: %s/7",
                "Плодородие: %s (%s), влажность: %s/7");
        add("message.kubanhorizons.soil_probe.rich", "rich", "богатая");
        add("message.kubanhorizons.soil_probe.normal", "normal", "обычная");
        add("message.kubanhorizons.soil_probe.poor", "poor", "истощённая");
        add("message.kubanhorizons.soil_probe.not_farmland",
                "Till the soil first to probe it",
                "Сначала вспашите землю, чтобы взять пробу");
        add("message.kubanhorizons.soil_probe.advice",
                "Tip: compost the soil or rotate crops to restore fertility",
                "Совет: внесите компост или смените культуру, чтобы восстановить плодородие");

        // --- Профессии ---
        add("entity.minecraft.villager.kubanhorizons.oil_presser", "Oil Presser", "Маслодел");

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
