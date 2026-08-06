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
        add("biome.kubanhorizons.kuban_steppe", "Kuban Steppe", "Кубанская степь");
        add("biome.kubanhorizons.plavni", "Kuban Wetlands", "Плавни");
        add("biome.kubanhorizons.liman", "Liman", "Лиман");
        add("biome.kubanhorizons.river_floodplain", "River Floodplain", "Пойма реки");
        add("generator.kubanhorizons.kuban_horizons", "Kuban Horizons", "Кубанские горизонты");

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
        add("block.kubanhorizons.drying_rack", "Drying Rack", "Сушильная рама");
        add("block.kubanhorizons.hand_mill", "Hand Mill", "Ручная мельница");
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
        add("item.kubanhorizons.dried_tea", "Dried Tea", "Сушёный чай");
        add("item.kubanhorizons.dried_fruit", "Dried Fruit", "Сушёные фрукты");
        add("item.kubanhorizons.flour", "Flour", "Мука");
        add("item.kubanhorizons.cornmeal", "Cornmeal", "Кукурузная крупа");
        add("item.kubanhorizons.homemade_bread", "Homemade Bread", "Домашний хлеб");
        add("item.kubanhorizons.borscht", "Kuban Borscht", "Кубанский борщ");
        add("item.kubanhorizons.mamalyga", "Mamalyga", "Мамалыга");
        add("item.kubanhorizons.tea_cup", "Cup of Tea", "Чашка чая");
        add("item.kubanhorizons.honey_walnuts", "Honey with Walnuts", "Мёд с орехами");
        add("item.kubanhorizons.vegetable_spread", "Vegetable Spread", "Овощная закуска");
        add("item.kubanhorizons.peach_sapling", "Peach Sapling", "Саженец персика");
        add("item.kubanhorizons.apricot_sapling", "Apricot Sapling", "Саженец абрикоса");
        add("item.kubanhorizons.plum_sapling", "Plum Sapling", "Саженец сливы");
        add("item.kubanhorizons.walnut_sapling", "Walnut Sapling", "Саженец грецкого ореха");
        add("item.kubanhorizons.peach", "Peach", "Персик");
        add("item.kubanhorizons.apricot", "Apricot", "Абрикос");
        add("item.kubanhorizons.plum", "Plum", "Слива");
        add("item.kubanhorizons.walnut", "Walnut", "Грецкий орех");

        // --- Путеводитель ---
        add("guide.kubanhorizons.title", "Guide to Kuban", "Путеводитель по Кубани");
        add("guide.kubanhorizons.page1",
                "KUBAN HORIZONS\n\nWelcome to the fertile lands between the black-earth steppe and the warm sea. This guide will help you begin.",
                "КУБАНСКИЕ ГОРИЗОНТЫ\n\nДобро пожаловать на плодородные земли между чернозёмной степью и тёплым морем. Этот путеводитель поможет начать.");
        add("guide.kubanhorizons.page2",
                "FERTILE SOIL\n\nCrops exhaust farmland. Use the soil probe to inspect fertility. Compost exhausted soil and rotate crops between harvests.",
                "ПЛОДОРОДНАЯ ПОЧВА\n\nКультуры истощают грядки. Проверяйте плодородие почвенным щупом. Удобряйте истощённую почву и чередуйте культуры.");
        add("guide.kubanhorizons.page3",
                "FIELD CROPS\n\nGrow sunflower and corn on farmland. Rice needs a flooded field. Tomatoes can be harvested repeatedly after ripening.",
                "ПОЛЕВЫЕ КУЛЬТУРЫ\n\nВыращивайте подсолнечник и кукурузу на грядках. Рису нужен затопленный чек. Созревшие томаты можно собирать многократно.");
        add("guide.kubanhorizons.page4",
                "TEA AND GRAPES\n\nTea grows on soil and gives new leaves after each harvest. Build a trellis, then graft a grape cutting onto it.",
                "ЧАЙ И ВИНОГРАД\n\nЧай растёт на земле и вновь даёт листья после сбора. Постройте шпалеру и привейте к ней черенок винограда.");
        add("guide.kubanhorizons.page5",
                "IRRIGATION\n\nPlace a water intake beside water, then lead irrigation channels to your fields. Wet channels hydrate nearby farmland.",
                "ОРОШЕНИЕ\n\nПоставьте водозабор у воды и проведите от него оросительные желоба к полям. Заполненные желоба увлажняют грядки рядом.");
        add("guide.kubanhorizons.page6",
                "THE ORCHARD\n\nPlant peach, apricot, plum, and walnut saplings. Ripe fruit appears in the crown and can be picked without cutting the tree.",
                "КУБАНСКИЙ САД\n\nСажайте персики, абрикосы, сливы и грецкие орехи. Спелые плоды появляются в кроне и собираются без рубки дерева.");
        add("guide.kubanhorizons.page7",
                "CRAFT AND PROCESS\n\nThe oil press turns sunflower seeds into oil. Dry tea and fruit under open skies. Turn the hand mill to grind grain.",
                "РЕМЕСЛО\n\nМаслопресс выжимает масло из семечек. Сушите чай и фрукты под открытым небом. Мелите зерно ручной мельницей.");
        add("guide.kubanhorizons.page8",
                "KUBAN KITCHEN\n\nBake bread, brew tea, and cook local dishes from your harvest. Follow advancements to discover every branch of the homestead.",
                "КУБАНСКАЯ КУХНЯ\n\nПеките хлеб, заваривайте чай и готовьте местные блюда из своего урожая. Достижения подскажут все пути развития хозяйства.");

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
        add("advancement.kubanhorizons.kitchen.title", "Kuban Kitchen", "Кубанская кухня");
        add("advancement.kubanhorizons.kitchen.description",
                "Bake homemade bread from flour", "Испеките домашний хлеб из муки");
        add("advancement.kubanhorizons.borscht.title", "The Real Borscht", "Тот самый борщ");
        add("advancement.kubanhorizons.borscht.description",
                "Cook Kuban borscht", "Сварите кубанский борщ");
        add("advancement.kubanhorizons.tea_cup.title", "Tea Time", "Чаепитие");
        add("advancement.kubanhorizons.tea_cup.description",
                "Brew a cup of Krasnodar tea", "Заварите чашку краснодарского чая");
        add("advancement.kubanhorizons.taster.title", "Taster", "Дегустатор");
        add("advancement.kubanhorizons.taster.description",
                "Collect every Kuban dish", "Соберите все кубанские блюда");

        // --- Конфигурация (переводы ключей) ---
        add("config.kubanhorizons.crops.growthSpeed", "Crop growth speed", "Скорость роста культур");
        add("config.kubanhorizons.fertility.enabled", "Soil fertility system", "Система плодородия почвы");
        add("config.kubanhorizons.irrigation.enabled", "Irrigation system", "Система орошения");
    }

    private KHTranslations() {
    }
}
