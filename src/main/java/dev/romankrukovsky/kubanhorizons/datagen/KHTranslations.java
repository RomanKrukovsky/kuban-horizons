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
        add("biome.kubanhorizons.foothill_forest", "Foothill Forest", "Предгорный лес");
        add("biome.kubanhorizons.mountain_forest", "Mountain Forest", "Горный лес");
        add("biome.kubanhorizons.azov_coast", "Azov Coast", "Азовское побережье");
        add("biome.kubanhorizons.black_sea_coast", "Black Sea Coast", "Черноморское побережье");
        add("biome.kubanhorizons.vineyard_hills", "Vineyard Hills", "Виноградные холмы");
        add("biome.kubanhorizons.tea_slopes", "Tea Slopes", "Чайные склоны");
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
        add("block.kubanhorizons.smokehouse", "Smokehouse", "Коптильня");
        add("block.kubanhorizons.grape_press", "Grape Press", "Виноградный пресс");
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
        add("block.kubanhorizons.manul_shelter", "Manul Shelter", "Укрытие для манула");

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
        add("item.kubanhorizons.grape_juice", "Grape Juice", "Виноградный сок");
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
        add("entity.kubanhorizons.caucasian_shepherd", "Caucasian Shepherd",
                "Кавказская овчарка");
        add("entity.kubanhorizons.sturgeon", "Sturgeon", "Осётр");
        add("entity.kubanhorizons.gull", "Gull", "Чайка");
        add("entity.kubanhorizons.heron", "Heron", "Цапля");
        add("entity.kubanhorizons.manul", "Kuban Manul", "Кубанский манул");
        add("manul.personality.cautious", "Cautious", "Осторожный");
        add("manul.personality.lazy", "Lazy", "Ленивый");
        add("manul.personality.curious", "Curious", "Любопытный");
        add("manul.personality.grumpy", "Grumpy", "Ворчливый");
        add("manul.personality.brave", "Brave", "Храбрый");
        add("manul.personality.greedy", "Greedy", "Прожорливый");
        add("entity.kubanhorizons.kuban_genie", "Kuban Genie", "Кубанская джинния");

        add("item.kubanhorizons.raw_boar", "Raw Boar", "Сырая кабанина");
        add("item.kubanhorizons.cooked_boar", "Cooked Boar", "Жареная кабанина");
        add("item.kubanhorizons.raw_sturgeon", "Raw Sturgeon", "Сырой осётр");
        add("item.kubanhorizons.cooked_sturgeon", "Cooked Sturgeon", "Запечённый осётр");
        add("item.kubanhorizons.nutria_pelt", "Nutria Pelt", "Шкура нутрии");
        add("item.kubanhorizons.sturgeon_bucket", "Bucket of Sturgeon", "Ведро с осётром");
        add("item.kubanhorizons.smoked_fish", "Smoked Fish", "Копчёная рыба");
        add("item.kubanhorizons.smoked_meat", "Smoked Meat", "Копчёное мясо");

        add("item.kubanhorizons.wild_boar_spawn_egg", "Wild Boar Spawn Egg",
                "Яйцо появления дикого кабана");
        add("item.kubanhorizons.nutria_spawn_egg", "Nutria Spawn Egg",
                "Яйцо появления нутрии");
        add("item.kubanhorizons.locust_spawn_egg", "Locust Spawn Egg",
                "Яйцо появления саранчи");
        add("item.kubanhorizons.caucasian_shepherd_spawn_egg", "Caucasian Shepherd Spawn Egg",
                "Яйцо появления кавказской овчарки");
        add("item.kubanhorizons.sturgeon_spawn_egg", "Sturgeon Spawn Egg",
                "Яйцо появления осетра");
        add("item.kubanhorizons.manul_spawn_egg", "Manul Spawn Egg",
                "Яйцо появления манула");
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
        add("message.kubanhorizons.grape_press.level",
                "Juice in the vat: %s of %s",
                "Сока в чане: %s из %s");
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
        add("message.kubanhorizons.genie.wish.festival",
                "The village celebrates the Annual Genie Festival!",
                "Деревня празднует Ежегодный праздник Джиннии!");
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
        add("key.category.kubanhorizons.genie", "Kuban Genie", "Кубанская джинния");
        add("key.kubanhorizons.open_genie_dialog", "Open genie dialogue", "Открыть диалог с джиннией");
        add("screen.kubanhorizons.genie.title", "Dialogue with %s", "Разговор с %s");
        add("screen.kubanhorizons.genie.greeting", "What shall reality become?",
                "Какой должна стать реальность?");
        add("screen.kubanhorizons.genie.input", "State your wish", "Сформулируйте желание");
        add("screen.kubanhorizons.genie.send", "Send", "Сказать");
        add("screen.kubanhorizons.genie.orders", "Companion orders", "Приказы спутнице");
        add("screen.kubanhorizons.genie.mode", "Current order: %s", "Текущий приказ: %s");
        add("screen.kubanhorizons.genie.hint", "Enter — send, right click — quick orders",
                "Enter — сказать, ПКМ — быстрые приказы");
        add("screen.kubanhorizons.genie.waiting", "The genie considers your wording...",
                "Джинния обдумывает формулировку...");
        add("screen.kubanhorizons.genie.invalid_wish", "The wish is empty or too long.",
                "Желание пустое или слишком длинное.");
        add("screen.kubanhorizons.genie.confirm", "Confirm change", "Подтвердить изменение");
        add("screen.kubanhorizons.genie.cancel", "Reject", "Отказаться");
        add("screen.kubanhorizons.genie.policy_preview",
                "Global rule %s: %s → %s. Confirm this change?",
                "Глобальное правило %s: %s → %s. Подтвердить изменение?");
        add("screen.kubanhorizons.genie.policy_applied",
                "The rule is active. Undo transaction: %s.",
                "Правило действует. Транзакция отмены: %s.");
        add("screen.kubanhorizons.genie.policy_cancelled", "The rule was left unchanged.",
                "Правило осталось без изменений.");
        add("screen.kubanhorizons.genie.no_policy_preview",
                "That preview is no longer available.", "Это предложение уже недоступно.");
        add("screen.kubanhorizons.genie.no_policy_to_undo",
                "There is no recent rule to undo.", "Нет недавнего правила для отмены.");
        add("screen.kubanhorizons.genie.policy_undone",
                "The last global rule was undone.", "Последнее глобальное правило отменено.");
        add("screen.kubanhorizons.pocket.title", "Pocket scene preview",
                "Предпросмотр карманной сцены");
        add("screen.kubanhorizons.pocket.confirm", "Create scene", "Создать сцену");
        add("screen.kubanhorizons.pocket.cancel", "Leave the world unchanged",
                "Не менять мир");
        add("screen.kubanhorizons.pocket.blocks", "Blocks changed: %s",
                "Изменится блоков: %s");
        add("screen.kubanhorizons.pocket.duration", "Duration: %s seconds",
                "Длительность: %s секунд");
        add("screen.kubanhorizons.pocket.risk", "Risk: %s", "Риск: %s");
        add("screen.kubanhorizons.pocket.applied",
                "The scene is active for %s seconds, then the world will return.",
                "Сцена действует %s секунд, затем мир вернётся.");
        add("screen.kubanhorizons.pocket.restored", "The pocket scene dissolved; the world returned.",
                "Карманная сцена растворилась; мир вернулся.");
        add("screen.kubanhorizons.pocket.cancelled", "The world was left unchanged.",
                "Мир остался без изменений.");
        add("screen.kubanhorizons.pocket.preview_expired", "The scene preview is no longer available.",
                "Предпросмотр сцены уже недоступен.");
        add("screen.kubanhorizons.pocket.already_active", "Your previous pocket scene is still active.",
                "Ваша предыдущая карманная сцена ещё действует.");
        add("message.kubanhorizons.genie.law.use_forfeit",
                "The vessel will not simply release you. Use /genie forfeit.",
                "Сосуд не отпустит просто так. Используйте /genie forfeit.");
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
                "My distant mind is unavailable. The server has no language provider key configured.",
                "Мой дальний разум недоступен. На сервере не задан ключ ни одного провайдера речи.");
        add("message.kubanhorizons.genie.ai.thinking",
                "The genie considers more possibilities than she cares to name...",
                "Джинния перебирает больше возможностей, чем считает нужным назвать...");
        add("message.kubanhorizons.genie.ai.failed",
                "The distant mirror is silent. My local mind remains awake.",
                "Дальнее зеркало молчит. Мой местный разум всё ещё бодрствует.");
        // --- Единственность джиннии ---
        add("message.kubanhorizons.genie.anchor.absent",
                "No genie is bound to this world yet.",
                "К этому миру ещё не привязана джинния.");
        add("message.kubanhorizons.genie.anchor.status",
                "This world's genie is %s, last seen in %s at %s.",
                "Джинния этого мира — %s, последний раз в %s на %s.");
        add("message.kubanhorizons.genie.anchor.reset",
                "The world's binding to its genie has been released.",
                "Привязка мира к джиннии снята.");
        // --- Предметы Джиннии ---
        add("item.kubanhorizons.wooden_spoon", "Wooden Spoon", "Деревянная ложка");
        add("item.kubanhorizons.sonic_boom_item", "Frozen Sonic Wave", "Застывший звуковой вал");
        add("item.kubanhorizons.magic_mirror", "Magic Mirror", "Магическое зеркало");
        add("item.kubanhorizons.miniature_world", "Miniature Pocket World", "Сжатый карманный мир");
        add("item.kubanhorizons.wish_contract", "Wish Contract", "Контракт желания");
        add("message.kubanhorizons.genie.lamp.unbound",
                "This lamp is not bound to a Genie identity.",
                "Эта лампа не связана с личностью джинна.");

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
                "Джинния переводит шёпот колокола: я помню 40 лет праздников и 3 набега...");
        add("message.kubanhorizons.genie.whisper.portal",
                "Genie whispers: This portal remembers a thousand travelers...",
                "Джинния шепчет: «Этот портал помнит путешествия тысячи странников...»");
        add("message.kubanhorizons.genie.whisper.ancient",
                "Genie reads the stones: These patterns were carved long before our time.",
                "Джинния читает камни: «Эти узоры высечены старой цивилизацией задолго до нас.»");
        add("wish.kubanhorizons.whisper.empty",
                "Genie: this block keeps its silence.",
                "Джинния: этот блок хранит молчание.");
        add("message.kubanhorizons.genie.flying_house",
                "Genie: Your house now floats gently in the sky!",
                "Джинния: «Твой дом теперь плавно парит в небесах!»");
        add("message.kubanhorizons.genie.wealthy_village",
                "Genie: The village glows with newfound prosperity!",
                "Джинния: «Деревня сияет новым богатством!»");
        add("message.kubanhorizons.genie.dream.vision",
                "In your sleep, you see visions of forgotten ancient temples and sister genies...",
                "Во сне вам открываются видения забытых древних храмов и сестёр-джинний...");
        add("message.kubanhorizons.genie.dream.reminder",
                "The genie whispers in your dream: your wish «%s» is still unfulfilled.",
                "Джинния шепчет во сне: ваше желание «%s» всё ещё не исполнено.");
        add("message.kubanhorizons.genie.pocket_scene",
                "A temporary 1-minute %s scene materializes in a swirl of magic smoke!",
                "В клубах магического дыма создана 1-минутная карманная сцена: %s!");
        add("message.kubanhorizons.genie.theater_reenactment",
                "Spectral phantoms begin reenacting the ancient history of this place...",
                "Призрачные фантомы начинают разыгрывать историю этого места...");
        add("message.kubanhorizons.genie.theater_empty",
                "The theater is silent: nothing remembered happened here.",
                "Театр молчит: здесь не запомнено ни одного события.");
        add("message.kubanhorizons.genie.theater_wish",
                "A past wish shimmers back into view, word by word.",
                "Былое желание проявляется в воздухе слово за словом.");
        add("message.kubanhorizons.genie.theater_rescue",
                "The rescue replays: a life pulled from danger, once more.",
                "Спасение разыгрывается вновь: жизнь, вырванная из беды.");
        add("message.kubanhorizons.genie.theater_village",
                "The village prosperity blooms again in ghostly color.",
                "Процветание поселения расцветает призрачными красками.");
        add("message.kubanhorizons.genie.living_painting",
                "You step through the canvas into a living painted dimension!",
                "Вы шагаете через холст прямо в живое нарисованное измерение!");
        add("message.kubanhorizons.genie.mirror_world",
                "The mirror surface ripples as you step into the mirror world.",
                "Поверхность зеркала идёт волнами, открывая проход в зазеркалье.");
        add("message.kubanhorizons.genie.magic_realm.left",
                "The portal returns you to the exact place you entered from.",
                "Портал возвращает вас точно к месту входа.");
        add("message.kubanhorizons.genie.magic_realm.recursive",
                "Pocket worlds cannot be opened from inside another pocket world.",
                "Нельзя открыть карманный мир изнутри другого карманного мира.");
        add("message.kubanhorizons.genie.magic_realm.missing",
                "The magical realm is unavailable in this world.",
                "Магический мир недоступен в этом сохранении.");
        add("message.kubanhorizons.genie.magic_realm.no_sleep",
                "Dreams cannot nest inside a painted reality.",
                "Сны нельзя вкладывать внутрь нарисованной реальности.");
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
        add("item.kubanhorizons.genie_lamp", "Genie's Lamp", "Лампа джиннии");
        add("item.kubanhorizons.player_genie_lamp", "%s's Genie Lamp", "Лампа джиннии %s");
        add("message.kubanhorizons.genie.lamp.bound",
                "The lamp now remembers %s — and its true owner.",
                "Лампа запомнила %s — и своего настоящего владельца.");
        add("message.kubanhorizons.genie.lamp.not_owner",
                "The lamp is silent. Stealing a vessel does not transfer its bond.",
                "Лампа молчит. Кража сосуда не передаёт связь.");
        add("message.kubanhorizons.genie.lamp.unavailable",
                "The bond is real, but the genie cannot answer it now.",
                "Связь существует, но джинния сейчас не может ответить.");
        add("message.kubanhorizons.genie.lamp.summoned",
                "The lamp answers. Your genie is beside you.",
                "Лампа откликнулась. Джинния снова рядом.");
        add("message.kubanhorizons.genie.lamp.required",
                "Bind a genie lamp first. A palace without a vessel has no door.",
                "Сначала привяжите лампу. У дворца без сосуда нет двери.");
        add("message.kubanhorizons.genie.conditional.added",
                "The rule is alive and saved: %s",
                "Правило действует и сохранено: %s");
        add("message.kubanhorizons.genie.conditional.removed",
                "The conditional rule has been removed.",
                "Условное правило удалено.");
        add("message.kubanhorizons.genie.conditional.not_found",
                "There is no matching rule to remove.",
                "Подходящего правила для удаления нет.");
        add("message.kubanhorizons.genie.conditional.limit",
                "You may keep no more than 16 conditional rules.",
                "Можно хранить не больше 16 условных правил.");
        add("message.kubanhorizons.genie.conditional.list",
                "Saved conditional rules: %s",
                "Сохранённые условные правила: %s");
        add("message.kubanhorizons.genie.conditional.rule.raining.grow_steppe",
                "When it rains → strengthen plant growth in the Kuban lands",
                "Когда идёт дождь → усиливать рост растений на кубанской земле");
        add("message.kubanhorizons.genie.conditional.rule.night.soul_light",
                "When night falls → kindle soul lights near the owner",
                "Когда наступает ночь → зажигать огни душ рядом с хозяином");
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

        // --- Клиентский UX трансформации игрока ---
        add("screen.kubanhorizons.transformation.title",
                "Transformation of the Player",
                "Трансформация игрока");
        add("stage.kubanhorizons.transformation.human",
                "Human",
                "Человек");
        add("stage.kubanhorizons.transformation.human.desc",
                "You are still mortal. The wish has not yet touched your nature.",
                "Вы ещё смертны. Желание ещё не коснулось вашей природы.");
        add("stage.kubanhorizons.transformation.awakening",
                "Awakening",
                "Пробуждение");
        add("stage.kubanhorizons.transformation.awakening.desc",
                "Mortality is removed. The body is being rewritten; flight is becoming natural.",
                "Смертность снята. Тело переписывается; полёт становится естественным.");
        add("stage.kubanhorizons.transformation.half_genie",
                "Half-Genie",
                "Полуджинния");
        add("stage.kubanhorizons.transformation.half_genie.desc",
                "The avatar takes shape. Physical damage is ignored, but the form is not yet complete.",
                "Аватар обретает форму. Физический урон игнорируется, но форма ещё не завершена.");
        add("stage.kubanhorizons.transformation.genie",
                "Genie",
                "Джинния");
        add("stage.kubanhorizons.transformation.genie.desc",
                "Transformation complete. The vessel lamp is materialized; omnipotence has its price.",
                "Трансформация завершена. Лампа-сосуд материализована; всемогущество имеет свою цену.");
        add("screen.kubanhorizons.transformation.progress",
                "Progress: %s%%",
                "Прогресс: %s%%");
        add("screen.kubanhorizons.transformation.close",
                "Close",
                "Закрыть");

        add("message.kubanhorizons.genie.vessel.summon_countdown",
                "SOMEONE IS SUMMONING YOU (3... 2... 1...)",
                "КТО-ТО ВЫЗЫВАЕТ ВАС (3... 2... 1...)");
        // Заключение в сосуд и выход. «Кто-то» вместо имени — сознательно:
        // анонимность работает на напряжение, имя открывается на первом желании.
        add("message.kubanhorizons.genie.vessel.confined",
                "The vessel has taken you in. Outside, the world goes on without you.",
                "Сосуд принял вас внутрь. Снаружи мир идёт дальше без вас.");
        add("message.kubanhorizons.genie.vessel.released",
                "You are outside again. The vessel remains yours — and findable.",
                "Вы снова снаружи. Сосуд по-прежнему ваш — и его по-прежнему можно найти.");
        add("message.kubanhorizons.genie.vessel.palace_entered",
                "The lamp opens inward. You enter the Eternal Kuban palace.",
                "Лампа открылась внутрь. Вы вошли во дворец Вечной Кубани.");
        add("message.kubanhorizons.genie.vessel.palace_left",
                "The lamp returns you to the exact place you entered from.",
                "Лампа вернула вас точно туда, откуда вы вошли.");
        add("message.kubanhorizons.genie.vessel.dimension_missing",
                "The vessel has no inside: its dimension is not loaded.",
                "У сосуда нет внутреннего пространства: его измерение не загружено.");
        // Четыре стадии натяжения хвоста — интерфейс вместо полоски и цифр.
        add("message.kubanhorizons.genie.leash.slack",
                "Your tail hangs loose. The vessel is near.",
                "Хвост висит свободно. Сосуд рядом.");
        add("message.kubanhorizons.genie.leash.turned",
                "The tip of your tail turns towards the vessel of its own accord.",
                "Кончик хвоста сам разворачивается в сторону сосуда.");
        add("message.kubanhorizons.genie.leash.stretched",
                "Your tail stretches out. The slack is gone.",
                "Хвост вытягивается. Провис исчез.");
        add("message.kubanhorizons.genie.leash.taut",
                "Your tail is drawn taut as a string. Moving is hard.",
                "Хвост натянут как струна. Двигаться тяжело.");
        // Предупреждение перед затягиванием — или намёк, по темпераменту.
        // DANGEROUS не говорит ничего: у молчания есть мотив, а не баг.
        add("message.kubanhorizons.genie.pull.warned",
                "\"The vessel is calling you back. It always does — I should have said so sooner.\"",
                "«Сосуд зовёт тебя назад. Он всегда зовёт — надо было сказать раньше.»");
        add("message.kubanhorizons.genie.pull.hinted",
                "\"Feel that? Something of yours has grown impatient.\"",
                "«Чувствуешь? Кое-что твоё заждалось.»");
        // Закон сосуда: цена выхода падает с тишиной, поэтому в сингле команда
        // со временем становится бесплатной сама.
        add("message.kubanhorizons.genie.law.not_confined",
                "You are not inside a vessel.",
                "Вы не внутри сосуда.");
        add("message.kubanhorizons.genie.law.forfeited",
                "You forced your way out. It cost you %s%% of your omnipotence.",
                "Вы вышли сами. Это стоило %s%% всемогущества.");
        add("message.kubanhorizons.genie.law.forfeited_free",
                "The vessel had forgotten you. You walked out and it cost nothing.",
                "Сосуд успел вас забыть. Вы просто вышли, и это ничего не стоило.");
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

        // --- Квестовые ступени желаний мобов (снег → золотое яблоко → загон и т.д.) ---
        add("message.kubanhorizons.genie.mob_wish.cow.golden_apple",
                "The cow lows in delight as a golden apple materializes before her.",
                "Корова радостно мычит: перед ней материализуется золотое яблоко.");
        add("message.kubanhorizons.genie.mob_wish.cow.pen",
                "A sturdy oak pen rises around the cow — now she has a home.",
                "Вокруг коровы вырастает крепкий дубовый загон — теперь у неё есть дом.");
        add("message.kubanhorizons.genie.mob_wish.wolf.bone",
                "The wolf takes the bone gratefully and looks at you with devotion.",
                "Волк с благодарностью принимает кость и преданно глядит на вас.");
        add("message.kubanhorizons.genie.mob_wish.wolf.guard",
                "The wolf straightens up — blessed by the genie, it will guard its master.",
                "Волк выпрямляется — с благословением джиннии он станет верным стражем хозяина.");
        add("message.kubanhorizons.genie.mob_wish.golem.iron",
                "The iron golem accepts a fresh ingot and nods in thanks.",
                "Железный голем принимает свежий слиток и благодарно кивает.");
        add("message.kubanhorizons.genie.mob_wish.golem.heal",
                "The iron golem's cracks mend as the genie's warmth flows through it.",
                "Трещины железного голема затягиваются — тёплый свет джиннии течёт сквозь него.");
        add("message.kubanhorizons.genie.mob_wish.creeper.firework",
                "The creeper bursts into bright fireworks instead of exploding.",
                "Крипер рассыпается яркими салютами вместо взрыва.");
        add("message.kubanhorizons.genie.mob_wish.creeper.gift",
                "The creeper accepts the golden apple and bows, promising peace.",
                "Крипер принимает золотое яблоко и кланяется, обещая мир.");
        add("message.kubanhorizons.genie.mob_wish.pending",
                "This creature's wish is already being fulfilled. Be patient.",
                "Желание этого существа уже принято. Наберитесь терпения.");
        add("message.kubanhorizons.genie.mob_wish.follow_up",
                "The genie recalls: this one was already granted \"%s\".",
                "Джинния вспоминает: этому уже исполнялось \"%s\".");

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
        add("message.kubanhorizons.genie.wish.policy_confirmation_required",
                "A global rule must be previewed and confirmed in the genie dialogue.",
                "Глобальное правило нужно сначала просмотреть и подтвердить в диалоге с джиннией.");
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
        add("message.kubanhorizons.genie.runtime.snapshot_created",
                "Snapshot '%s' published: %s blocks in %s chunks.",
                "Снимок «%s» опубликован: %s блоков в %s чанках.");
        add("message.kubanhorizons.genie.runtime.snapshot_list_header",
                "Your snapshots: %s.", "Ваши снимки: %s.");
        add("message.kubanhorizons.genie.runtime.snapshot_list_entry",
                "- %s: %s blocks, %s chunks, %s.",
                "- %s: %s блоков, %s чанков, %s.");
        add("message.kubanhorizons.genie.runtime.snapshot_inspect",
                "Snapshot %s (%s): %s blocks, %s chunks, %s, captured %s.",
                "Снимок %s (%s): %s блоков, %s чанков, %s, создан %s.");
        add("message.kubanhorizons.genie.runtime.snapshot_deleted",
                "Snapshot '%s' deleted.", "Снимок «%s» удалён.");
        add("message.kubanhorizons.genie.runtime.miniaturize_preview",
                "Miniaturize: %s non-air blocks, %s block entities, %s entities. Risk: %s.",
                "Миниатюризация: %s непустых блоков, %s блочных сущностей, %s сущностей. Риск: %s.");
        add("message.kubanhorizons.genie.runtime.undo_list_header",
                "Available retained undo entries: %s.", "Доступные отмены: %s.");
        add("message.kubanhorizons.genie.runtime.undo_list_entry",
                "- %s: %s blocks in %s, expires %s.",
                "- %s: %s блоков в %s, истекает %s.");
        add("message.kubanhorizons.genie.runtime.scene_preview",
                "Pocket scene: %s changed blocks for %s ticks. Risk: %s.",
                "Карманная сцена: %s изменённых блоков на %s тиков. Риск: %s.");
        add("message.kubanhorizons.genie.runtime.move_preview",
                "Structure move: %s blocks will change.",
                "Перенос структуры: изменится %s блоков.");
        add("message.kubanhorizons.genie.runtime.selection_first",
                "First corner selected at %s, %s, %s. Use the mirror on the opposite corner.",
                "Первая точка выбрана: %s, %s, %s. Примените зеркало ко второй точке.");
        add("message.kubanhorizons.genie.runtime.selection_complete",
                "Selection ready: %s blocks in %s chunks. Create a named snapshot with /genie snapshot create <name>.",
                "Область готова: %s блоков в %s чанках. Создайте снимок: /genie snapshot create <имя>.");
        add("message.kubanhorizons.genie.miniature.compressed",
                "The selected region now exists inside the miniature world.",
                "Выбранная область теперь существует внутри миниатюрного мира.");
        add("message.kubanhorizons.genie.miniature.blocked",
                "The miniature needs an empty region of the same size.",
                "Для разворачивания миниатюры нужна пустая область того же размера.");
        add("message.kubanhorizons.genie.runtime.preview",
                "Restore '%s': %s blocks and %s block entities will change. Risk: %s. Use /genie snapshot confirm.",
                "Восстановление «%s»: изменятся %s блоков и %s блочных сущностей. Риск: %s. Введите /genie snapshot confirm.");
        add("message.kubanhorizons.genie.runtime.confirmed",
                "Consent recorded. Use /genie snapshot execute before the preview expires.",
                "Согласие записано. Введите /genie snapshot execute до истечения предпросмотра.");
        add("message.kubanhorizons.genie.runtime.no_preview",
                "Create a restore preview first.", "Сначала создайте предпросмотр восстановления.");
        add("message.kubanhorizons.genie.runtime.no_confirmation",
                "This restore has no valid confirmation.", "Для этого восстановления нет действующего подтверждения.");
        add("message.kubanhorizons.genie.runtime.outcome",
                "Strong wish result: %s; changed blocks: %s; transaction: %s.",
                "Результат сильного желания: %s; изменено блоков: %s; транзакция: %s.");
        add("message.kubanhorizons.genie.runtime.status",
                "Strong-wish runtime ready: %s (%s).", "Готовность сильных желаний: %s (%s).");
        add("message.kubanhorizons.genie.runtime.failed",
                "Strong wish rejected: %s", "Сильное желание отклонено: %s");

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
        add("subtitles.kubanhorizons.wild_boar.ambient", "Boar grunts", "Кабан хрюкает");
        add("subtitles.kubanhorizons.wild_boar.hurt", "Boar hurts", "Кабану больно");
        add("subtitles.kubanhorizons.wild_boar.death", "Boar dies", "Кабан умирает");
        add("subtitles.kubanhorizons.nutria.ambient", "Nutria squeaks", "Нутрия пищит");
        add("subtitles.kubanhorizons.nutria.hurt", "Nutria hurts", "Нутрии больно");
        add("subtitles.kubanhorizons.nutria.death", "Nutria dies", "Нутрия умирает");
        add("subtitles.kubanhorizons.locust.ambient", "Locust rasps", "Саранча стрекочет");
        add("subtitles.kubanhorizons.locust.hurt", "Locust crunches", "Саранча хрустит");
        add("subtitles.kubanhorizons.caucasian_shepherd.ambient", "Shepherd barks",
                "Овчарка лает");
        add("subtitles.kubanhorizons.caucasian_shepherd.hurt", "Shepherd yelps",
                "Овчарка взвизгивает");
        add("subtitles.kubanhorizons.caucasian_shepherd.death", "Shepherd dies",
                "Овчарка умирает");
        add("subtitles.kubanhorizons.sturgeon.flop", "Sturgeon flops", "Осётр бьётся");
        add("subtitles.kubanhorizons.gull.ambient", "Gull cries", "Чайка кричит");
        add("subtitles.kubanhorizons.gull.hurt", "Gull hurts", "Чайке больно");
        add("subtitles.kubanhorizons.heron.ambient", "Heron croaks", "Цапля скрипит");
        add("subtitles.kubanhorizons.heron.hurt", "Heron hurts", "Цапле больно");
        add("subtitles.kubanhorizons.manul.ambient", "Manul grumbles", "Манул ворчит");
        add("subtitles.kubanhorizons.manul.hiss", "Manul hisses", "Манул шипит");
        add("subtitles.kubanhorizons.manul.purr", "Manul purrs", "Манул урчит");
        add("subtitles.kubanhorizons.manul.hurt", "Manul hurts", "Манулу больно");
        add("subtitles.kubanhorizons.manul.death", "Manul dies", "Манул умирает");
        add("subtitles.kubanhorizons.weather.dry_wind", "Dry wind blows", "Суховей дует");
        add("subtitles.kubanhorizons.entity.genie.snap", "Genie snaps fingers",
                "Джинн щёлкает пальцами");

        // --- Атмосфера биомов ---
        // Субтитр есть у каждой петли и у каждого вкрапления: со включёнными
        // субтитрами игрок должен видеть, что именно звучит, иначе атмосфера
        // существует только для слышащих. Формулировки короткие и именные,
        // как в ванили («Eerie noise»), а не предложениями.
        add("subtitles.kubanhorizons.ambient.steppe.loop",
                "Steppe wind", "Степной ветер");
        add("subtitles.kubanhorizons.ambient.steppe.additions",
                "Wind whistles in the weeds", "Ветер посвистывает в бурьяне");
        add("subtitles.kubanhorizons.ambient.floodplain.loop",
                "Floodplain rustles", "Пойма шелестит");
        add("subtitles.kubanhorizons.ambient.floodplain.additions",
                "Water laps the bank", "Вода плещет у берега");
        add("subtitles.kubanhorizons.ambient.plavni.loop",
                "Reeds whisper", "Тростник шепчет");
        add("subtitles.kubanhorizons.ambient.plavni.additions",
                "Wings beat in the reeds", "Крылья хлопают в камыше");
        add("subtitles.kubanhorizons.ambient.liman.loop",
                "Estuary breathes", "Лиман дышит");
        add("subtitles.kubanhorizons.ambient.liman.additions",
                "A distant cry over the shallows", "Далёкий крик над отмелью");

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

        // --- Достижения: рыболовство ---
        add("advancement.kubanhorizons.first_sturgeon.title", "Kuban Sturgeon", "Кубанский осётр");
        add("advancement.kubanhorizons.first_sturgeon.description",
                "Catch a sturgeon in the floodplain or a liman",
                "Поймайте осетра в пойме или лимане");
        add("advancement.kubanhorizons.cooked_sturgeon.title", "Fish on the Fire", "Рыба на огне");
        add("advancement.kubanhorizons.cooked_sturgeon.description",
                "Cook a sturgeon", "Пожарьте осетра");
        add("advancement.kubanhorizons.smoked_fish.title", "Smokehouse Keeps", "Коптильня хранит");
        add("advancement.kubanhorizons.smoked_fish.description",
                "Smoke fish in a smokehouse — it keeps far longer than fried",
                "Закопти рыбу в коптильне: так она хранится дольше жареной");
        add("advancement.kubanhorizons.sturgeon_bucket.title", "Live Fish", "Живая рыба");
        add("advancement.kubanhorizons.sturgeon_bucket.description",
                "Carry a living sturgeon in a bucket",
                "Перенесите живого осетра в ведре");

        // --- Достижения: ремесло ---
        add("advancement.kubanhorizons.adobe.title", "Clay and Straw", "Глина и солома");
        add("advancement.kubanhorizons.adobe.description",
                "Make adobe bricks — the cheapest wall in the steppe",
                "Сделайте саманный кирпич — самую дешёвую стену в степи");
        add("advancement.kubanhorizons.whitewash.title", "White Hut", "Белая хата");
        add("advancement.kubanhorizons.whitewash.description",
                "Whitewash a wall with plaster", "Побелите стену штукатуркой");
        add("advancement.kubanhorizons.homestead.title", "Kuban Homestead", "Кубанская усадьба");
        add("advancement.kubanhorizons.homestead.description",
                "Gather roof tiles, shell rock, wattle and a carved window casing",
                "Соберите черепицу, ракушечник, плетень и резной наличник");

        // --- Достижения: манул ---
        add("advancement.kubanhorizons.manul_observed.title", "Don't Touch the Cat", "Не трогай кота");
        add("advancement.kubanhorizons.manul_observed.description",
                "Watch a wild manul for a long while without coming closer",
                "Долго наблюдайте за диким манулом, не приближаясь к нему");
        add("advancement.kubanhorizons.manul_trusted.title", "The Manul Tolerates You",
                "Манул тебя терпит");
        add("advancement.kubanhorizons.manul_trusted.description",
                "Earn a manul's full trust with patience and offerings",
                "Заслужите полное доверие манула терпением и подношениями");
        add("advancement.kubanhorizons.manul_settled.title", "Pillar of the Stanitsa",
                "Опора станицы");
        add("advancement.kubanhorizons.manul_settled.description",
                "A manul settles in a shelter beside your homestead",
                "Манул поселился в укрытии рядом с вашей усадьбой");
        add("advancement.kubanhorizons.manul_silver.title", "The Kuban One", "Кубанский");
        add("advancement.kubanhorizons.manul_silver.description",
                "Meet the rarest of them all — the silver manul",
                "Встретьте самого редкого — серебристого манула");

        // --- Манул: репутация и легенды станицы ---
        add("message.kubanhorizons.manul.killed_witnessed",
                "The villagers saw you kill the manul",
                "Жители видели, как вы убили манула");
        add("message.kubanhorizons.manul.legend.1",
                "Villager: While the manul keeps the yard, the mice keep away from the grain.",
                "Житель: «Пока манул при дворе — мыши от зерна подальше».");
        add("message.kubanhorizons.manul.legend.2",
                "Villager: They say a manul on the roof means the house will stand a hundred years.",
                "Житель: «Говорят, манул на крыше — дом сто лет простоит».");
        add("message.kubanhorizons.manul.legend.3",
                "Villager: Meeting a manul is good luck. Just don't reach out to pet it.",
                "Житель: «Встретить манула — к удаче. Только гладить не тянись».");
        add("message.kubanhorizons.manul.legend.4",
                "Villager: My grandfather never chased one off the hay. He said the steppe would take offence.",
                "Житель: «Дед мой манула с сена никогда не гонял. Говорил — степь обидится».");
        // --- Конфигурация (переводы ключей) ---
        add("config.kubanhorizons.crops.growthSpeed", "Crop growth speed", "Скорость роста культур");
        add("config.kubanhorizons.fertility.enabled", "Soil fertility system", "Система плодородия почвы");
        add("config.kubanhorizons.irrigation.enabled", "Irrigation system", "Система орошения");

        // --- Отладочный оверлей (debug.overlay) ---
        // Показываются только те числа, которые клиент действительно знает:
        // плодородие на клиент не синхронизируется, и строки про него не
        // существует намеренно — см. KHDebugOverlay.
        add("debug.kubanhorizons.biome", "Biome: %s", "Биом: %s");
        add("debug.kubanhorizons.moisture", "Farmland moisture: %s/%s",
                "Влажность грядки: %s/%s");
        add("debug.kubanhorizons.channel.distance", "Channel: water at distance %s",
                "Жёлоб: вода на удалении %s");
        add("debug.kubanhorizons.channel.dry", "Channel: dry", "Жёлоб: сухой");

        // --- Игровые подсказки на предметах (tooltips.detailed) ---
        // Только там, где применение не следует из названия и модели.
        add("tooltip.kubanhorizons.grape_cutting",
                "Graft onto a trellis, not into farmland",
                "Прививается в шпалеру, не в грядку");
        add("tooltip.kubanhorizons.rice_seedlings",
                "Plant in shallow water: a flooded paddy",
                "Сажают в мелководье — затопленный чек");
        add("tooltip.kubanhorizons.tea_sapling",
                "A perennial bush: pick leaves by hand, it keeps growing",
                "Многолетний куст: лист собирают рукой, куст остаётся");
        add("tooltip.kubanhorizons.tea_leaves",
                "Dry on a drying rack before brewing",
                "Перед заваркой сушат на сушильной раме");
        add("tooltip.kubanhorizons.sunflower_head",
                "Thresh for seeds, or press for oil",
                "Обмолачивают на семечки или отжимают на масло");
        add("tooltip.kubanhorizons.oil_cake",
                "Left over from pressing: livestock feed and compost",
                "Остаётся после отжима: корм скоту и в компост");
        add("tooltip.kubanhorizons.soil_probe",
                "Right-click farmland to read its fertility and moisture",
                "ПКМ по грядке — покажет плодородие и влажность");
        add("tooltip.kubanhorizons.irrigation_channel",
                "Carries water from an intake; wooden channels are gnawed by nutria",
                "Ведёт воду от водозабора; деревянный портят нутрии");
        add("tooltip.kubanhorizons.stone_irrigation_channel",
                "Same as the wooden channel, but nutria cannot gnaw it",
                "То же, что деревянный желоб, но нутрии его не прогрызут");
        add("tooltip.kubanhorizons.water_intake",
                "Place against water: the source for a channel network",
                "Ставят к воде — источник для сети желобов");
        add("tooltip.kubanhorizons.drying_rack",
                "Dries tea leaves and fruit in the open air",
                "Сушит чайный лист и фрукты на открытом воздухе");
        add("tooltip.kubanhorizons.manul_shelter",
                "A manul may settle here — if it trusts you and likes the spot",
                "Здесь может поселиться манул — если доверяет и место по нему");

        // Смерть владельца / выбор судьбы
        add("screen.kubanhorizons.genie.owner_death.title",
                "Owner's Death — Choose Your Fate",
                "Смерть владельца — Выберите судьбу");
        add("screen.kubanhorizons.genie.owner_death.subtitle",
                "The genie offers you a choice:",
                "Джинния предлагает вам выбор:");
        add("choice.kubanhorizons.genie.owner_death.resurrect",
                "Resurrect owner",
                "Воскресить владельца");
        add("choice.kubanhorizons.genie.owner_death.save_soul",
                "Preserve soul (60s)",
                "Сохранить душу (60 сек)");
        add("choice.kubanhorizons.genie.owner_death.rollback",
                "Roll back last wish",
                "Откатить последнее желание");
        add("choice.kubanhorizons.genie.owner_death.respawn_free",
                "Release genie and respawn",
                "Освободить джиннию");
        add("message.kubanhorizons.genie.rescue_success",
                "You have been rescued at the place of death.",
                "Вы воскрешены на месте смерти.");
        add("message.kubanhorizons.genie.soul_saved",
                "Your soul has been preserved for 60 seconds.",
                "Ваша душа сохранена на 60 секунд.");
        add("message.kubanhorizons.genie.rollback_success",
                "Last wish has been rolled back.",
                "Последнее желание откачено.");
        add("message.kubanhorizons.genie.respawn_free",
                "Genie is released. You are mortal again.",
                "Джинния освобождена. Вы снова обычный человек.");
        add("message.kubanhorizons.genie.lamp.no_sleep",
                "You cannot sleep inside the genie palace.",
                "Во дворце джиннии нельзя спать.");

        // --- Экран состояния Wishborne ---
        add("screen.kubanhorizons.genie.wishborne.title",
                "Wishborne State",
                "Состояние Wishborne");
        add("screen.kubanhorizons.genie.wishborne.close",
                "Close",
                "Закрыть");
        add("screen.kubanhorizons.genie.wishborne.anchoring",
                "Reality anchoring: %s/100",
                "Якорение реальности: %s/100");
        add("state.kubanhorizons.genie.wishborne.manifested",
                "Manifested",
                "Проявлена");
        add("state.kubanhorizons.genie.wishborne.manifested.desc",
                "The avatar is fully present and free to act.",
                "Аватар полностью присутствует и свободно действует.");
        add("state.kubanhorizons.genie.wishborne.dispersed",
                "Dispersed",
                "Рассеяна");
        add("state.kubanhorizons.genie.wishborne.dispersed.desc",
                "The physical form is dispersed but can reform.",
                "Физическая форма рассеяна, но может восстановиться.");
        add("state.kubanhorizons.genie.wishborne.sealed",
                "Sealed",
                "Запечатана");
        add("state.kubanhorizons.genie.wishborne.sealed.desc",
                "Trapped by seals; anchoring has reached its limit.",
                "Запечатана рунами; якорение достигло предела.");
        add("state.kubanhorizons.genie.wishborne.banished",
                "Banished",
                "Изгнана");
        add("state.kubanhorizons.genie.wishborne.banished.desc",
                "Exiled from this realm of reality.",
                "Изгнана из этого слоя реальности.");

        // General wish executor keys
        add("message.kubanhorizons.genie.wish.gave_diamonds",
                "Here are your diamonds!",
                "Вот твои алмазы!");
        add("message.kubanhorizons.genie.wish.gave_gold",
                "Pure Kuban gold for you!",
                "Чистое кубанское золото!");
        add("message.kubanhorizons.genie.wish.gave_food",
                "A feast for the road!",
                "Угощение в дорогу!");
        add("message.kubanhorizons.genie.wish.gave_sword",
                "A sharp diamond blade!",
                "Острый алмазный клинок!");
        add("message.kubanhorizons.genie.wish.gave_pickaxe",
                "A sturdy pickaxe!",
                "Крепкая кирка!");
        add("message.kubanhorizons.genie.wish.spawned_chickens",
                "Chickens gathered from all around!",
                "Куры со всей округи сбежались!");
        add("message.kubanhorizons.genie.wish.spawned_cows",
                "Cows are now grazing nearby!",
                "Коровы пасутся рядом!");
        add("message.kubanhorizons.genie.wish.spawned_wolves",
                "Steppe wolves heed your call!",
                "Степные волки откликнулись!");
        add("message.kubanhorizons.genie.wish.spawned_horses",
                "Noble horses for your journey!",
                "Благородные кони для пути!");
        add("message.kubanhorizons.genie.wish.spawned_cats",
                "Cats appeared to keep you company!",
                "Коты пришли согреть теплом!");
        add("message.kubanhorizons.genie.wish.rain_started",
                "Rain clouds gather over the steppe!",
                "Дождевые тучи сгущаются над степью!");
        add("message.kubanhorizons.genie.wish.weather_cleared",
                "The steppe sky is clear and sunny!",
                "Небо над степью чистое и ясное!");
        add("message.kubanhorizons.genie.wish.night_set",
                "Steppe night blankets the world.",
                "Степная ночь опустилась на мир.");
        add("message.kubanhorizons.genie.wish.day_set",
                "The warm Kuban sun rises.",
                "Тёплое кубанское солнце взошло.");
        add("message.kubanhorizons.genie.wish.placed_tree",
                "A young oak springs forth from the soil!",
                "Молодой дуб вырос из земли!");
        add("message.kubanhorizons.genie.wish.placed_house",
                "A humble cozy cottage has been built!",
                "Уютная хата возведена!");
        add("message.kubanhorizons.genie.wish.firework",
                "Magic bursts across the sky!",
                "Магический салют озарил небо!");
        add("message.kubanhorizons.genie.wish.general_fulfilled",
                "The genie weaved magic to fulfill your wish!",
                "Джинния соткала магию и исполнила желание!");
        add("message.kubanhorizons.genie.wish.failed",
                "The wish could not take form.",
                "Желание не смогло обрести форму.");

        // --- История: «А что если?» — альтернативные версии мира ---
        add("wish.kubanhorizons.whatif.result",
                "Genie: '%s' — %s. If the wish had stayed, the world would differ in %s blocks. %s",
                "Джинния: «%s» — %s. Останься это желание, мир отличался бы на %s блоков. %s");
        add("wish.kubanhorizons.whatif.empty",
                "Genie: no undone wish to compare. Reality remembers nothing to replay.",
                "Джинния: нечего сравнивать. Реальность не помнит ни одного отменённого желания.");

        // --- Слова, рисунки и переписывание биома (GENIE_VISION) ---
        add("wish.kubanhorizons.word.written",
                "The word materializes in the air, block by block.",
                "Слово материализуется в воздухе, блок за блоком.");
        add("wish.kubanhorizons.word.bad",
                "Genie: say a short word to materialize (up to 12 letters).",
                "Джинния: назовите короткое слово для материализации (до 12 букв).");
        add("wish.kubanhorizons.drawing.drawn",
                "The drawing traces itself across the ground.",
                "Рисунок вычерчивается по земле.");
        add("wish.kubanhorizons.photo.captured",
                "The scene freezes into a magical photograph.",
                "Сцена застывает в магической фотографии.");
        add("wish.kubanhorizons.painting.entered",
                "You step through the canvas into the mirror world.",
                "Вы шагаете через холст в зеркальный мир.");
        add("wish.kubanhorizons.painting.missing",
                "Genie: the mirror world is not available here.",
                "Джинния: зеркальный мир сейчас недоступен.");
        add("wish.kubanhorizons.doppelganger.created",
                "A magical double of you steps out of the smoke.",
                "Из дыма выходит ваш магический двойник.");
        add("wish.kubanhorizons.bridge.built",
                "A wooden bridge of %s blocks rises across the gap.",
                "Деревянный мост из %s блоков поднимается над пропастью.");
        add("wish.kubanhorizons.bridge.none",
                "Genie: there is no gap to bridge here.",
                "Джинния: здесь нечего перекрывать мостом.");
        add("wish.kubanhorizons.ground.raised",
                "The earth rises %s blocks before you.",
                "Земля поднимается на %s блоков перед вами.");
        add("wish.kubanhorizons.ground.none",
                "Genie: there is nowhere to raise the ground.",
                "Джинния: землю здесь некуда поднимать.");
        add("wish.kubanhorizons.scale.small",
                "The world suddenly seems huge around you.",
                "Мир вокруг вдруг кажется огромным.");
        add("wish.kubanhorizons.scale.giant",
                "The world shrinks beneath your feet.",
                "Мир сжимается под вашими ногами.");
        add("wish.kubanhorizons.army.summoned",
                "%s iron guardians step out to protect you.",
                "%s железных стражей выступают вас защищать.");
        add("wish.kubanhorizons.biome.rewritten",
                "The steppe rewrites the land around you.",
                "Степь переписывает землю вокруг вас.");
        add("wish.kubanhorizons.npc.modified",
                "Genie: the nearby creature now feels %s.",
                "Джинния: ближайшее существо теперь чувствует себя %s.");
        add("wish.kubanhorizons.npc.none",
                "Genie: no creature nearby to influence.",
                "Джинния: рядом нет существа, на которое можно повлиять.");

        // --- Магическая музыка и танец (GENIE_VISION) ---
        add("wish.kubanhorizons.music.rain",
                "The Rain Song begins. Clouds gather over the steppe.",
                "Звучит Песня дождя. Над степью сгущаются облака.");
        add("wish.kubanhorizons.music.growth",
                "The Growth Melody makes the fields hurry to ripen.",
                "Мелодия Роста торопит поля созреть.");
        add("wish.kubanhorizons.music.peace",
                "The Peace Lullaby calms the hostility around you.",
                "Колыбельная Покоя гасит враждебность вокруг вас.");
        add("wish.kubanhorizons.music.fire",
                "The Dance of Fire wraps allies in fireproof light.",
                "Танец Огня окутывает союзников огнеупорным светом.");
        add("wish.kubanhorizons.dance.triggered",
                "Your dance becomes a song.",
                "Ваш танец становится песней.");
        add("message.kubanhorizons.music_box.mood",
                "The music box plays: %s",
                "Шкатулка играет: %s");
        add("mood.kubanhorizons.music.calm",
                "Calm",
                "Покой");
        add("mood.kubanhorizons.music.joy",
                "Joy",
                "Радость");
        add("mood.kubanhorizons.music.sadness",
                "Sadness",
                "Грусть");
        add("mood.kubanhorizons.music.awe",
                "Awe",
                "Благоговение");
    }

    private KHTranslations() {
    }
}
