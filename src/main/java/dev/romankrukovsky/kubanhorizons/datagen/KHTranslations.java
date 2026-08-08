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
        add("structure.kubanhorizons.floodplain_fishing_camp",
                "Floodplain Fishing Camp", "Рыбацкий стан в пойме");
        add("structure.kubanhorizons.plavni_reed_shelter",
                "Plavni Reed Shelter", "Камышовый навес в плавнях");

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
        add("block.kubanhorizons.cutting_board", "Cutting Board", "Разделочная доска");
        add("block.kubanhorizons.peach_leaves", "Peach Leaves", "Персиковая листва");
        add("block.kubanhorizons.apricot_leaves", "Apricot Leaves", "Абрикосовая листва");
        add("block.kubanhorizons.plum_leaves", "Plum Leaves", "Сливовая листва");
        add("block.kubanhorizons.walnut_leaves", "Walnut Leaves", "Листва грецкого ореха");
        add("block.kubanhorizons.peach_sapling", "Peach Sapling", "Саженец персика");
        add("block.kubanhorizons.apricot_sapling", "Apricot Sapling", "Саженец абрикоса");
        add("block.kubanhorizons.plum_sapling", "Plum Sapling", "Саженец сливы");
        add("block.kubanhorizons.walnut_sapling", "Walnut Sapling", "Саженец грецкого ореха");

        // --- Строительные материалы ---
        add("block.kubanhorizons.adobe_bricks", "Adobe Bricks", "Саманный кирпич");
        add("block.kubanhorizons.adobe_brick_stairs", "Adobe Brick Stairs", "Саманная ступенька");
        add("block.kubanhorizons.adobe_brick_slab", "Adobe Brick Slab", "Саманная плита");
        add("block.kubanhorizons.adobe_brick_wall", "Adobe Brick Wall", "Саманная стенка");
        add("block.kubanhorizons.shell_rock", "Shell Rock", "Ракушечник");
        add("block.kubanhorizons.shell_rock_stairs", "Shell Rock Stairs", "Ступенька из ракушечника");
        add("block.kubanhorizons.shell_rock_slab", "Shell Rock Slab", "Плита из ракушечника");
        add("block.kubanhorizons.shell_rock_wall", "Shell Rock Wall", "Стенка из ракушечника");
        add("block.kubanhorizons.whitewashed_plaster", "Whitewashed Plaster", "Белёная штукатурка");
        add("block.kubanhorizons.whitewashed_plaster_stairs", "Whitewashed Plaster Stairs", "Ступеньки из белёной штукатурки");
        add("block.kubanhorizons.whitewashed_plaster_slab", "Whitewashed Plaster Slab", "Плита из белёной штукатурки");
        add("block.kubanhorizons.roof_tiles", "Roof Tiles", "Черепица");
        add("block.kubanhorizons.roof_tile_stairs", "Roof Tile Stairs", "Ступеньки из черепицы");
        add("block.kubanhorizons.roof_tile_slab", "Roof Tile Slab", "Плита из черепицы");
        add("block.kubanhorizons.decorative_ceramic", "Decorative Ceramic", "Декоративная керамика");
        add("block.kubanhorizons.carved_window_casing", "Carved Window Casing", "Резной оконный наличник");
        add("block.kubanhorizons.wattle", "Wattle Fence", "Плетень");
        add("block.kubanhorizons.wattle_gate", "Wattle Gate", "Калитка плетня");

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

        // --- Фауна: сущности, мясо, яйца-спавнеры ---
        add("entity.kubanhorizons.pheasant", "Pheasant", "Фазан");
        add("entity.kubanhorizons.quail", "Quail", "Перепел");
        add("item.kubanhorizons.raw_pheasant", "Raw Pheasant", "Сырой фазан");
        add("item.kubanhorizons.cooked_pheasant", "Cooked Pheasant", "Жареный фазан");
        add("item.kubanhorizons.raw_quail", "Raw Quail", "Сырой перепел");
        add("item.kubanhorizons.cooked_quail", "Cooked Quail", "Жареный перепел");
        add("item.kubanhorizons.pheasant_spawn_egg", "Pheasant Spawn Egg",
                "Яйцо появления фазана");
        add("item.kubanhorizons.quail_spawn_egg", "Quail Spawn Egg",
                "Яйцо появления перепела");

        // --- Фауна давления: вредители, симбионты и защита ---
        add("entity.kubanhorizons.wild_boar", "Wild Boar", "Дикий кабан");
        add("entity.kubanhorizons.nutria", "Nutria", "Нутрия");
        add("entity.kubanhorizons.locust", "Locust", "Саранча");
        add("entity.kubanhorizons.caucasian_bee", "Caucasian Bee", "Кавказская пчела");
        add("entity.kubanhorizons.caucasian_shepherd", "Caucasian Shepherd",
                "Кавказская овчарка");
        add("entity.kubanhorizons.sturgeon", "Sturgeon", "Осётр");
        add("entity.kubanhorizons.gull", "Gull", "Чайка");
        add("entity.kubanhorizons.heron", "Heron", "Цапля");
        add("entity.kubanhorizons.kuban_genie", "Kuban Genie", "Кубанская джинния");

        add("item.kubanhorizons.raw_boar", "Raw Boar", "Сырая кабанина");
        add("item.kubanhorizons.cooked_boar", "Cooked Boar", "Жареная кабанина");
        add("item.kubanhorizons.raw_sturgeon", "Raw Sturgeon", "Сырой осётр");
        add("item.kubanhorizons.cooked_sturgeon", "Cooked Sturgeon", "Запечённый осётр");
        add("item.kubanhorizons.nutria_pelt", "Nutria Pelt", "Шкура нутрии");
        add("item.kubanhorizons.sturgeon_bucket", "Bucket of Sturgeon", "Ведро с осётром");

        add("item.kubanhorizons.wild_boar_spawn_egg", "Wild Boar Spawn Egg",
                "Яйцо появления дикого кабана");
        add("item.kubanhorizons.nutria_spawn_egg", "Nutria Spawn Egg",
                "Яйцо появления нутрии");
        add("item.kubanhorizons.locust_spawn_egg", "Locust Spawn Egg",
                "Яйцо появления саранчи");
        add("item.kubanhorizons.caucasian_bee_spawn_egg", "Caucasian Bee Spawn Egg",
                "Яйцо появления кавказской пчелы");
        add("item.kubanhorizons.caucasian_shepherd_spawn_egg", "Caucasian Shepherd Spawn Egg",
                "Яйцо появления кавказской овчарки");
        add("item.kubanhorizons.sturgeon_spawn_egg", "Sturgeon Spawn Egg",
                "Яйцо появления осетра");
        add("item.kubanhorizons.gull_spawn_egg", "Gull Spawn Egg",
                "Яйцо появления чайки");
        add("item.kubanhorizons.heron_spawn_egg", "Heron Spawn Egg",
                "Яйцо появления цапли");

        add("block.kubanhorizons.stone_irrigation_channel", "Stone Irrigation Channel",
                "Каменный оросительный желоб");

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
        add("message.kubanhorizons.genie.bound",
                "The genie studies you in silence. The first bond has been made.",
                "Джинния молча изучает вас. Первая связь установлена.");
        add("message.kubanhorizons.genie.not_owner",
                "The genie answers: You do not hold my bond.",
                "Джинния отвечает: «Не с тобой заключена моя связь».");
        add("message.kubanhorizons.genie.status",
                "Temperament: %s | trust %s, respect %s, fear %s, affection %s, freedom %s, power %s, corruption %s. Rename paper with a wish and use it on me.",
                "Характер: %s | доверие %s, уважение %s, страх %s, симпатия %s, свобода %s, могущество %s, искажение %s. Переименуйте бумагу в желание и примените её ко мне.");
        add("message.kubanhorizons.genie.wish.unknown",
                "I do not understand that material wish. Precision: %s.",
                "Я не понимаю это материальное желание. Точность: %s.");
        add("message.kubanhorizons.genie.wish.safe",
                "A precise wish leaves little room for mischief. Precision: %s.",
                "Точное желание почти не оставляет места для уловок. Точность: %s.");
        add("message.kubanhorizons.genie.wish.literal",
                "You asked for diamonds. You did not say where. Precision: %s.",
                "Вы просили алмазы. Где именно — не сказали. Точность: %s.");
        add("message.kubanhorizons.genie.wish.no_space",
                "Reality found no safe place for the chest. Precision: %s.",
                "Реальность не нашла безопасного места для сундука. Точность: %s.");
        add("genie.kubanhorizons.temperament.kind", "kind", "добрая");
        add("genie.kubanhorizons.temperament.sardonic", "sardonic", "ехидная");
        add("genie.kubanhorizons.temperament.proud", "proud", "гордая");
        add("genie.kubanhorizons.temperament.cunning", "cunning", "хитрая");
        add("genie.kubanhorizons.temperament.dangerous", "dangerous", "опасная");
        add("genie.kubanhorizons.temperament.guarded", "guarded", "настороженная");
        add("genie.kubanhorizons.mode.follow", "follow", "следовать");
        add("genie.kubanhorizons.mode.stay", "stay", "оставаться на месте");
        add("genie.kubanhorizons.mode.guard", "guard", "охранять");
        add("genie.kubanhorizons.mode.scout", "scout", "исследовать окрестности");
        add("message.kubanhorizons.genie.ai.mode", "New order: %s.", "Новый приказ: %s.");
        add("message.kubanhorizons.genie.ai.status",
                "Mind: %s | rescues %s, threats repelled %s, projectiles stopped %s, wishes remembered %s. Sneak-use with an empty hand to change my order.",
                "Разум: %s | спасений %s, отражено угроз %s, остановлено снарядов %s, запомнено желаний %s. Крадитесь и нажмите пустой рукой, чтобы сменить приказ.");
        add("message.kubanhorizons.genie.ai.rescue",
                "Not yet, master. Reality can wait.",
                "Ещё не время, хозяин. Реальность подождёт.");
        add("message.kubanhorizons.genie.ai.projectile",
                "That was not meant to reach you.",
                "Этому не суждено было до вас долететь.");
        add("message.kubanhorizons.genie.ai.threat",
                "They should have chosen a safer target.",
                "Им стоило выбрать цель побезопаснее.");
        add("message.kubanhorizons.genie.ai.explosion",
                "I saw the explosion before it happened.",
                "Я увидела взрыв раньше, чем он случился.");
        add("message.kubanhorizons.genie.ai.reasoning",
                "Last conclusion: %s (utility %s).",
                "Последний вывод: %s (полезность %s).");
        add("genie.kubanhorizons.decision.rescue_owner", "prevent your death", "предотвратить вашу гибель");
        add("genie.kubanhorizons.decision.preempt_explosion", "prevent an explosion", "предотвратить взрыв");
        add("genie.kubanhorizons.decision.intercept_projectile", "intercept a predicted impact", "перехватить рассчитанное попадание");
        add("genie.kubanhorizons.decision.repel_threat", "control nearby threats", "контролировать ближайшие угрозы");
        add("genie.kubanhorizons.decision.return_to_owner", "close the distance", "сократить дистанцию");
        add("genie.kubanhorizons.decision.hold_position", "hold the ordered position", "удерживать указанную позицию");
        add("genie.kubanhorizons.decision.scout_area", "survey the route ahead", "разведать путь впереди");
        add("genie.kubanhorizons.decision.observe", "continue observing", "продолжить наблюдение");
        add("message.kubanhorizons.genie.ai.no_companion",
                "No genie bound to you is present in this dimension.",
                "В этом измерении рядом нет связанной с вами джиннии.");
        add("message.kubanhorizons.genie.ai.unavailable",
                "My distant mind is unavailable. The server needs EUROMODELS_API_KEY.",
                "Мой дальний разум недоступен. Серверу нужна переменная EUROMODELS_API_KEY.");
        add("message.kubanhorizons.genie.ai.thinking",
                "The genie considers more possibilities than she cares to name...",
                "Джинния перебирает больше возможностей, чем считает нужным назвать...");
        add("message.kubanhorizons.genie.ai.failed",
                "The distant mirror is silent. My local mind remains awake.",
                "Дальнее зеркало молчит. Мой местный разум всё ещё бодрствует.");
        // --- Предметы Джиннии ---
        add("item.kubanhorizons.wooden_spoon", "Wooden Spoon", "Деревянная ложка");
        add("item.kubanhorizons.sonic_boom_item", "Frozen Sonic Wave", "Застывший звуковой вал");
        add("item.kubanhorizons.magic_mirror", "Magic Mirror", "Магическое зеркало");
        add("item.kubanhorizons.miniature_world", "Miniature Pocket World", "Сжатый карманный мир");

        // --- Желания без слов, память и шёпот блоков ---
        add("message.kubanhorizons.genie.wordless.farmland",
                "Genie (wordless): Till this dry soil? Done!",
                "Джинния (без слов): «Вспахать эту сухую землю? Готово!»");
        add("message.kubanhorizons.genie.wordless.stone",
                "Genie (wordless): Repair cracked masonry? Easy!",
                "Джинния (без слов): «Починить растрескавшуюся кладку? Запросто!»");
        add("message.kubanhorizons.genie.memory.item_read",
                "Memory of %s: enchantments: %s, damage: %s.",
                "Память %s: зачарований: %s, износ: %s.");
        add("message.kubanhorizons.genie.whisper.bell",
                "Genie translates the bell's whispers: I remember 40 years of festivals and 3 raids...",
                "Джинния переводит шёпот колокола: «Я помню 40 лет праздников и 3 набега...»");
        add("message.kubanhorizons.genie.whisper.portal",
                "Genie whispers: This portal remembers a thousand travelers...",
                "Джинния шепчет: «Этот портал помнит путешествия тысячи странников...»");
        add("message.kubanhorizons.genie.whisper.ancient",
                "Genie reads the stones: These patterns were carved long before our time.",
                "Джинния читает камни: «Эти узоры высечены старой цивилизацией задолго до нас.»");
        add("message.kubanhorizons.genie.flying_house",
                "Genie: Your house now floats gently in the sky!",
                "Джинния: «Твой дом теперь плавно парит в небесах!»");
        add("message.kubanhorizons.genie.wealthy_village",
                "Genie: The village glows with newfound prosperity!",
                "Джинния: «Деревня сияет новым богатством!»");
        add("message.kubanhorizons.genie.dream.vision",
                "In your sleep, you see visions of forgotten ancient temples and sister genies...",
                "Во сне вам открываются видения забытых древних храмов и сестёр-джинний...");
        add("message.kubanhorizons.genie.pocket_scene",
                "A temporary 1-minute %s scene materializes in a swirl of magic smoke!",
                "В клубах магического дыма создана 1-минутная карманная сцена: %s!");
        add("message.kubanhorizons.genie.theater_reenactment",
                "Spectral phantoms begin reenacting the ancient history of this place...",
                "Призрачные фантомы начинают разыгрывать историю этого места...");
        add("message.kubanhorizons.genie.living_painting",
                "You step through the canvas into a living painted dimension!",
                "Вы шагаете через холст прямо в живое нарисованное измерение!");
        add("message.kubanhorizons.genie.mirror_world",
                "The mirror surface ripples as you step into the parallel mirror timeline...",
                "Поверхность зеркала идет волнами, открывая проход в параллельный таймлайн...");
        add("message.kubanhorizons.genie.festival_start",
                "Annual Genie Festival has begun in the village with music, decorations and fireworks!",
                "В деревне начался Ежегодный праздник Джиннии с музыкой и салютами!");
        add("message.kubanhorizons.genie.death_choice",
                "Genie offers a choice: Instant Revive, Soul Preservation, or 5-Minute Rollback?",
                "Джинния предлагает выбор: Мгновенное Воскрешение, Сохранение Души или Откат на 5 минут?");
        add("message.kubanhorizons.genie.contract_issued",
                "A formal Magical Contract with fine print and loopholes has been issued!",
                "Выдан формальный Магический Контракт со всеми правилами и лазейками!");
        add("message.kubanhorizons.genie.role_swapped",
                "Role Reversal! You take her place in the lamp, while she becomes a free entity!",
                "Обмен ролями! Вы занимаете место в лампе, а Джинния становится свободной!");
        add("entity.kubanhorizons.magic_doppelganger", "Magic Doppelgänger", "Магический двойник");

        // --- Ироническая защита и ложная смерть ---
        add("message.kubanhorizons.genie.irony.spoon",
                "Did you really think a piece of metal would hurt me? Hold this wooden spoon instead.",
                "Ты действительно думал, что кусок металла мне навредит? Держи пока деревянную ложку.");
        add("message.kubanhorizons.genie.irony.arrow",
                "A sharp point... How adorable.",
                "Острое пёрышко... Как мило.");
        add("message.kubanhorizons.genie.irony.sonic",
                "Sound can be quite tangible if you catch it properly.",
                "Звук становится вполне осязаемым, если его вовремя поймать.");
        add("message.kubanhorizons.genie.irony.tnt",
                "Explosions are so loud... My hair is ruined.",
                "Взрывы такие громкие... Причёску слегка испортил.");
        add("message.kubanhorizons.genie.phantom_death.finished",
                "Finished yet?",
                "Ты закончил?");
        add("message.kubanhorizons.genie.irony.kill",
                "Entity cannot be permanently destroyed.",
                "«Сущность не может быть уничтожена навсегда.»");
        add("message.kubanhorizons.genie.wish.literal_chickens",
                "Genie (literal): 40,000 chickens? As you wish!",
                "Джинния (буквально): «40 тысяч куриц? Изволь получать!»");
        add("message.kubanhorizons.genie.wish.literal_gold",
                "Genie (literal): Pure gold right on your head!",
                "Джинния (буквально): «Чистое золото прямо на голову!»");
        add("message.kubanhorizons.genie.wish.literal_done",
                "Genie (literal): Wish granted word for word.",
                "Джинния (буквально): «Желание исполнено буква в букву.»");
        // --- Искажённые желания и превращение в джиннию ---
        add("item.kubanhorizons.player_genie_lamp", "%s's Genie Lamp", "Лампа джиннии %s");
        add("message.kubanhorizons.genie.wish.omnipotence_warning",
                "Genie: Omnipotent?.. That is a very careless word.",
                "Джинния: «Всемогущим?.. Это очень неосторожное слово.»");
        add("message.kubanhorizons.genie.wish.eternity_warning",
                "Genie: Forever?.. Eternity has a habit of growing cold.",
                "Джинния: «Вечно?.. Вечность имеет свойство остывать.»");
        add("message.kubanhorizons.genie.wish.self_fulfillment_warning",
                "Genie: To grant your own wishes?.. A recursive loop of power.",
                "Джинния: «Исполнять свои желания?.. Рекурсивная петля силы.»");
        add("message.kubanhorizons.genie.wish.mirror_warning",
                "Genie: To be like me?.. You do not know what you ask.",
                "Джинния: «Быть такой же, как я?.. Ты не ведаешь, о чём просишь.»");
        add("message.kubanhorizons.genie.wish.power_transfer_warning",
                "Genie: My power comes with a heavy vessel.",
                "Джинния: «Моя сила приходит вместе с тяжёлым сосудом.»");
        add("message.kubanhorizons.genie.wish.intangibility_warning",
                "Genie: No one can harm you if you cease to be physical.",
                "Джинния: «Никто не причинит тебе вреда, если ты перестанешь быть физическим.»");
        add("message.kubanhorizons.genie.wish.higher_order_warning",
                "Genie: A request of the highest order...",
                "Джинния: «Запрос высшего порядка...»");
        add("message.kubanhorizons.genie.wish.higher_order_banner",
                "HIGHER ORDER WISH: Formulation alters the nature of the wisher. Changes may be irreversible.",
                "ЖЕЛАНИЕ ВЫСШЕГО ПОРЯДКА: Формулировка затрагивает природу желающего. Изменения могут быть необратимы.");
        add("message.kubanhorizons.genie.wish.transformation_started",
                "As you wish. Transformation initiated...",
                "Как пожелаешь. Трансформация начата...");

        add("message.kubanhorizons.genie.transformation.stage1_status",
                "Body condition: UNDEFINED",
                "Состояние тела: НЕОПРЕДЕЛЕНО");
        add("message.kubanhorizons.genie.transformation.mortality_removed",
                "Mortality removed.",
                "Смертность удалена.");
        add("message.kubanhorizons.genie.transformation.stage2_flight",
                "ANATOMICAL LIMITATION REMOVED",
                "АНАТОМИЧЕСКОЕ ОГРАНИЧЕНИЕ СНЯТО");
        add("message.kubanhorizons.genie.transformation.stage2_anatomical",
                "Flight is now a natural state.",
                "Полёт теперь является естественным состоянием.");
        add("message.kubanhorizons.genie.transformation.stage3_form_dialogue",
                "Genie: You asked for omnipotence. You did not specify the form.",
                "Джинния: «Ты попросил всемогущество. Ты не уточнял форму.»");
        add("message.kubanhorizons.genie.transformation.stage4_damage_ignored",
                "Physical damage ignored.",
                "Физическое повреждение проигнорировано.");
        add("message.kubanhorizons.genie.transformation.stage4_genie_quote",
                "Genie: You get used to it.",
                "Джинния: «К этому привыкаешь.»");
        add("message.kubanhorizons.genie.transformation.vessel_created",
                "Your vessel lamp has been materialized.",
                "Материализована ваша собственная лампа-сосуд.");
        add("message.kubanhorizons.genie.transformation.price_dialogue",
                "Genie: You wanted omnipotence. Nobody said omnipotence means freedom.",
                "Джинния: «Ты хотел стать всемогущим. Никто не сказал, что всемогущество означает свободу.»");
        add("message.kubanhorizons.genie.transformation.void_rescue",
                "Spatial shell lost. Creating new presence point...",
                "Пространственная оболочка потеряна. Создание новой точки присутствия...");

        add("message.kubanhorizons.genie.vessel.summon_countdown",
                "SOMEONE IS SUMMONING YOU (3... 2... 1...)",
                "КТО-ТО ВЫЗЫВАЕТ ВАС (3... 2... 1...)");
        add("message.kubanhorizons.genie.vessel.new_rule_master",
                "New rule discovered: Vessel owner has the right to state a wish.",
                "Новое правило обнаружено: Владелец сосуда имеет право произнести желание.");
        add("message.kubanhorizons.genie.vessel.master_acquired",
                "Genie %s has emerged before you!",
                "Джинния %s вылетела из лампы перед вами!");
        add("message.kubanhorizons.genie.vessel.fulfilled", "Wish fulfilled: %s", "Желание исполнено: %s");
        add("message.kubanhorizons.genie.vessel.interpreted", "Wish interpreted: %s", "Желание интерпретировано: %s");
        add("message.kubanhorizons.genie.vessel.warned", "Master warned about wish: %s", "Хозяин предупреждён о желании: %s");
        add("message.kubanhorizons.genie.vessel.refused", "Wish refused: %s", "В исполнении желания отказано: %s");
        add("message.kubanhorizons.genie.vessel.loophole_found", "Loophole found for wish: %s", "Найдена лазейка в желании: %s");
        add("message.kubanhorizons.genie.lamp.no_genie_online",
                "The lamp glows softly, but its bound genie is currently dormant.",
                "Лампа тихо светится, но связанная джинния сейчас дремлет.");

        add("message.kubanhorizons.genie.progression.status", "Wish #1 Progress: %s%%", "Желание №1: %s%%");
        add("message.kubanhorizons.genie.progression.tier_unlocked", "Genie Tier %s Unlocked!", "Разблокирован Уровень силы %s!");
        add("message.kubanhorizons.genie.progression.will_requires_tier5", "Genie Will requires Tier V Omnipotence.", "Воля Джиннии требует V Уровня Всемогущества.");
        add("message.kubanhorizons.genie.progression.will_executed", "Genie Will: %s", "Воля Джиннии: %s");

        add("message.kubanhorizons.genie.ending.wish1_complete", "Wish #1: 'I want to become omnipotent.' - 100% COMPLETE", "Желание №1: «Я хочу стать всемогущим.» — 100% ИСПОЛНЕНО");
        add("message.kubanhorizons.genie.ending.no_interface_needed", "You no longer need an interface to change reality.", "Вам больше не требуется интерфейс, чтобы изменять реальность.");
        add("message.kubanhorizons.genie.ending.supreme_djinni", "You are now a Supreme Order Kuban Genie.", "Вы стали Истинной Кубанской Джиннией высшего порядка.");

        // --- Желания мобов ---
        add("message.kubanhorizons.genie.mob_wish.cow",
                "The cow smiles as gentle snowflakes begin to fall...",
                "Корова радостно мычит, рассматривая первые снежинки...");
        add("message.kubanhorizons.genie.mob_wish.wolf",
                "The wolf feels a warm bond with its true master.",
                "Волк ощутил тёплую связь со своим истинным хозяином.");
        add("message.kubanhorizons.genie.mob_wish.golem",
                "The iron golem receives a flower and understands its protective purpose.",
                "Железный голем принимает цветок и обретает смысл защиты.");
        add("message.kubanhorizons.genie.mob_wish.creeper",
                "The creeper sparkles with harmless fireworks instead of exploding.",
                "Крипер рассыпается безопасными салютами вместо взрыва.");

        // --- Результаты желаний гигантизма, мета-правил и деревень ---
        add("message.kubanhorizons.genie.wish.big_chicken",
                "A chicken height of 30 blocks steps into reality!",
                "Курица высотой 30 блоков шагает в вашу реальность!");
        add("message.kubanhorizons.genie.wish.big_pie",
                "A house-sized cake materializes before you!",
                "Пирог размером с дом материализуется перед вами!");
        add("message.kubanhorizons.genie.wish.big_bed",
                "A giant bed fit for ten players appears!",
                "Огромная кровать для десяти игроков появляется перед вами!");
        add("message.kubanhorizons.genie.wish.meta_longer_night",
                "The genie stretches the fabric of time. Night shall linger.",
                "Джинния растягивает ткань времени. Ночь продлится дольше.");
        add("message.kubanhorizons.genie.wish.meta_no_creeper_damage",
                "Creepers will no longer shatter the earth.",
                "Криперы больше не смогут разрушать блоки земляной тверди.");
        add("message.kubanhorizons.genie.wish.meta_instant_smelt",
                "The fires of reality flare to instant heat.",
                "Пламя реальности вспыхивает мгновенным жаром.");
        add("message.kubanhorizons.genie.wish.village_wealth",
                "A chest of emeralds arrives for the settlement.",
                "Сундук изумрудов прибывает на благо поселения.");

        // --- Статистика памяти ---
        add("message.kubanhorizons.genie.memory.status",
                "World Memory: %s wishes granted, %s rescues performed, %s villages protected.",
                "Память мира: исполнено желаний %s, совершено спасений %s, защищено поселений %s.");
        add("message.kubanhorizons.genie.memory.first_discovery",
                "First bond forged in the wild land.",
                "Первая связь сформирована на вольной земле.");
        add("message.kubanhorizons.genie.memory.rescue",
                "Owner saved from lethal threat.",
                "Хозяин спасён от смертельной угрозы.");
        add("message.kubanhorizons.genie.memory.village",
                "Village economy transformed by magic.",
                "Экономика поселения преобразована магией.");

        add("message.kubanhorizons.genie.ai.reply", "Genie: %s", "Джинния: %s");

        // --- Профессии ---
        add("entity.minecraft.villager.kubanhorizons.oil_presser", "Oil Presser", "Маслодел");

        // --- Контейнеры ---
        add("container.kubanhorizons.oil_press", "Oil Press", "Маслопресс");

        // --- Субтитры звуков ---
        add("subtitles.kubanhorizons.oil_press.creak", "Oil press creaks", "Маслопресс скрипит");
        add("subtitles.kubanhorizons.oil_press.work", "Oil press works", "Маслопресс работает");
        add("subtitles.kubanhorizons.oil_press.finish", "Oil drips", "Капает масло");
        add("subtitles.kubanhorizons.pheasant.ambient", "Pheasant calls", "Фазан кричит");
        add("subtitles.kubanhorizons.pheasant.hurt", "Pheasant hurts", "Фазану больно");
        add("subtitles.kubanhorizons.pheasant.death", "Pheasant dies", "Фазан умирает");
        add("subtitles.kubanhorizons.pheasant.flush", "Pheasant flushes",
                "Фазан взлетает");
        add("subtitles.kubanhorizons.quail.ambient", "Quail whistles", "Перепел посвистывает");
        add("subtitles.kubanhorizons.quail.hurt", "Quail hurts", "Перепелу больно");
        add("subtitles.kubanhorizons.quail.death", "Quail dies", "Перепел умирает");
        add("subtitles.kubanhorizons.quail.flush", "Quail flushes", "Перепел взлетает");

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
        add("advancement.kubanhorizons.rice_seedlings.title", "Flooded Paddy", "Затопленный чек");
        add("advancement.kubanhorizons.rice_seedlings.description",
                "Obtain rice seedlings", "Раздобудьте рисовую рассаду");
        add("advancement.kubanhorizons.rice_panicle.title", "Kuban Rice", "Кубанский рис");
        add("advancement.kubanhorizons.rice_panicle.description",
                "Harvest a ripe rice panicle", "Соберите созревшую метёлку риса");
        add("advancement.kubanhorizons.cooked_rice.title", "Steaming Bowl", "Пар над казаном");
        add("advancement.kubanhorizons.cooked_rice.description",
                "Cook a portion of rice", "Отварите порцию риса");
        add("advancement.kubanhorizons.grape_cutting.title", "A Cutting for Grafting",
                "Черенок на прививку");
        add("advancement.kubanhorizons.grape_cutting.description",
                "Obtain a grape cutting", "Раздобудьте виноградный черенок");
        add("advancement.kubanhorizons.grape_trellis.title", "Trellis Row", "Шпалерный ряд");
        add("advancement.kubanhorizons.grape_trellis.description",
                "Build a grape trellis", "Соберите виноградную шпалеру");
        add("advancement.kubanhorizons.grapes.title", "Cluster by Cluster", "Гроздь за гроздью");
        add("advancement.kubanhorizons.grapes.description",
                "Harvest a cluster of grapes", "Соберите гроздь винограда");
        add("advancement.kubanhorizons.tea_sapling.title", "Tea Slopes", "Чайные склоны");
        add("advancement.kubanhorizons.tea_sapling.description",
                "Obtain a tea sapling", "Раздобудьте чайный саженец");
        add("advancement.kubanhorizons.tea_leaves.title", "Top Two Leaves and a Bud",
                "Два листа и почка");
        add("advancement.kubanhorizons.tea_leaves.description",
                "Pick tea leaves from a bush", "Соберите листья с чайного куста");
        add("advancement.kubanhorizons.dried_tea.title", "Krasnodar Blend", "Краснодарский купаж");
        add("advancement.kubanhorizons.dried_tea.description",
                "Dry tea leaves on a drying rack", "Высушите чайный лист на сушилке");
        add("advancement.kubanhorizons.orchard.title", "Orchard Begins", "Сад начинается");
        add("advancement.kubanhorizons.orchard.description",
                "Obtain a fruit tree sapling", "Раздобудьте саженец плодового дерева");
        add("advancement.kubanhorizons.first_fruit.title", "First Fruit", "Первый плод");
        add("advancement.kubanhorizons.first_fruit.description",
                "Pick fruit from your own tree", "Сорвите плод с собственного дерева");
        add("advancement.kubanhorizons.dried_fruit.title", "Winter Stores", "Запасы на зиму");
        add("advancement.kubanhorizons.dried_fruit.description",
                "Dry fruit on a drying rack", "Высушите фрукты на сушилке");
        add("advancement.kubanhorizons.kuban_orchard.title", "Kuban Orchard", "Кубанский сад");
        add("advancement.kubanhorizons.kuban_orchard.description",
                "Gather peach, apricot, plum and walnut",
                "Соберите персик, абрикос, сливу и грецкий орех");

        // --- Конфигурация (переводы ключей) ---
        add("config.kubanhorizons.crops.growthSpeed", "Crop growth speed", "Скорость роста культур");
        add("config.kubanhorizons.fertility.enabled", "Soil fertility system", "Система плодородия почвы");
        add("config.kubanhorizons.irrigation.enabled", "Irrigation system", "Система орошения");
    }

    private KHTranslations() {
    }
}
