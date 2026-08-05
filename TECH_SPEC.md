# TECH_SPEC.md — технические спецификации подсистем

Детализация реализации. Общие принципы — ARCHITECTURE.md.

## 1. Регистрации

- По одному классу на реестр в `registry`: `KHBlocks`, `KHItems`,
  `KHBlockEntities`, `KHMenus`, `KHRecipes`, `KHSounds`, `KHParticles`,
  `KHCreativeTabs`, `KHDataComponents`, `KHEntities`.
- Каждый класс: приватный `DeferredRegister` + статический метод
  `register(IEventBus)`; вызовы собраны в конструкторе `KubanHorizons`.
- Хелперы регистрации «блок+item» — в `KHBlocks`, чтобы блок и его item
  создавались одним вызовом и не расходились.

## 2. Вертикальный контур «подсолнечник» (эталон качества)

### 2.1 Культура `sunflower_crop`
- Двухблочная культура (`DoubleCropBlock`): свойства `AGE 0..4` (низ),
  верхняя половина появляется с AGE≥3; на AGE=4 верх несёт шляпку.
- Сажается `sunflower_seeds` на farmland (обычную и региональные почвы).
- `randomTick`: шанс роста = базовый × множитель почвы/плодородия ×
  конфиг `growthSpeed`.
- Костная мука работает (стандартный `BonemealableBlock`).
- Урожай: разрушение зрелого растения → 1 `sunflower_head` + 0–2
  `sunflower_seeds` (loot table, fortune учитывается).
- Компостируемость: seeds 0.3, head 0.65.

### 2.2 Маслопресс `oil_press`
- Блок с горизонтальной ориентацией (`FACING`), непрозрачная модель
  ~30 кубов «станина + винт».
- BlockEntity: 3 слота (вход / бутылки / выход) + прогресс.
  Рецепт-тип `oil_pressing`: вход N предметов → выход (масло, жмых).
- Работа требует взаимодействия игрока (прокрутка винта ПКМ — 1 такт)
  ИЛИ пассивно медленно, если в конфиге включён авторежим. Базовый рецепт:
  8 семян + стеклянная бутылка → 1 бутылка масла + 1 жмых.
- Синхронизация: `ContainerData` для прогресса; `setChanged` + block
  update только при фактическом изменении.
- Анти-дюп: слоты валидируются на серверной стороне; результат создаётся
  только в `craft()` на сервере; тест на двойное извлечение.
- Звук: скрип пресса (свой, синтезированный); частицы: капли масла.

### 2.3 Продукты
- `sunflower_oil` (bottle): не еда сама по себе, ингредиент. Возвращает
  пустую бутылку при готовке (crafting remainder — у рецептов).
- `roasted_sunflower_seeds`: еда 3/0.4, быстрое поедание (fast food).
- `oil_cake`: корм для свиней/кур (ускоряет размножение), компост 0.5.

### 2.4 Достижения ветки
- `kuban_root` → «Семечки!» (получить семена) → «Первый урожай» (собрать
  шляпку) → «Золото степи» (выжать масло) → «На своём масле» (приготовить
  блюдо на масле).

## 3. Плодородие (soil)

- Хранение: `ChunkFertilityData` — attachment на LevelChunk;
  `Long2ByteMap` позиций farmland → плодородие 0..100. Пустая карта не
  сериализуется. `schemaVersion` в NBT.
- Значение по умолчанию выводится из типа блока (не хранится): farmland 40,
  степной суглинок 60, чернозём 85.
- События изменения: сбор урожая (−N за ту же культуру подряд, история
  последней культуры — байт-код культуры), компост (+N), простой под паром
  (ленивое восстановление: при чтении учитывать прошедшее время),
  орошение (множитель, не хранится).
- Влияние: множитель шанса randomTick-роста 0.6..1.6 и модификатор
  количества урожая (при 80+ шанс бонусного дропа).
- Никаких per-block тикеров. Чтение — O(1) по карте чанка.

## 4. Орошение (irrigation)

- Сеть = граф блоков (водозабор → желоба/шлюзы). Индекс сетей на уровень:
  `IrrigationNetworkManager` (SavedData), перестройка при
  `onPlace/onRemove` блоков сети, поиск ограничен радиусом 64 и бюджетом
  512 блоков на перестройку.
- Желоб имеет `LEVEL 0..3` (визуальная наполненность), обновляется
  волной от водозабора не чаще раза в 20 тиков и только при изменении.
- Эффект: farmland в радиусе 4 от заполненного желоба считается влажным
  и получает бонус плодородия-множителя.
- Конфиг: дальность, производительность, отключение системы.

## 5. Сеть (network)

- Custom payloads только для: анимация щупа (S2C), состояние страницы
  путеводителя (C2S — запрос, S2C — данные). Версия протокола в ID.
- Всё остальное — стандартные механизмы (BE update tags, ContainerData).

## 6. Конфигурация

- `KHServerConfig` (SERVER): `fertility.enabled`, `fertility.depletionRate`,
  `fertility.recoveryRate`, `irrigation.enabled`, `irrigation.range`,
  `crops.growthSpeed`, `trade.enabled`, `worldgen.biomeWeight`,
  `worldgen.structureDensity`, `automation.oilPressAuto`,
  `wine.fermentationEnabled`.
- `KHClientConfig` (CLIENT): `particles.density`, `ambience.volume`,
  `visual.extraEffects`, `tooltips.detailed`, `debug.overlay`.
- Все параметры — с диапазонами; чтение только через геттеры с валидацией.

## 7. Datagen

Провайдеры в `datagen`: BlockStates+Models, ItemModels, Recipes, LootTables,
BlockTags/ItemTags/BiomeTags, RuLang/EnLang (оба генерируются из одного
реестра строк `KHTranslations` — гарантия полноты обоих языков),
Advancements, DamageTypes, Worldgen (configured/placed features, биомы).
Событие: `GatherDataEvent.Client` (единый клиентский datagen NeoForge 26.x).

## 8. GameTest

- Пакет `gametest`, шаблоны структур — `data/kubanhorizons/structure/gametest/`.
- Каждый тест — сценарий: подготовка → действие → `succeedWhen`.
- Обязательные наборы — TEST_PLAN.md. Namespace включён в run-конфигурациях
  и CI (`neoforge.enabledGameTestNamespaces=kubanhorizons`).

## 9. Звук

- Формат: OGG Vorbis, моно, 44.1 кГц, нормализация −16 LUFS ориентировочно.
- Синтез собственных звуков программно (Python/DSP) либо запись с
  совместимой лицензией + запись в THIRD_PARTY_NOTICES.md.
- `sounds.json` — через datagen-провайдер SoundDefinitions.

## 10. Производительность (бюджеты)

- Серверный tick мода на тестовом мире (ферма 64×64, 2 сети орошения,
  6 устройств, 10 жителей): **< 1.5 мс** в среднем.
- Перестройка сети орошения: **< 0.5 мс**, амортизировано.
- Никаких аллокаций в horячих путях randomTick.
- Замеры — PERFORMANCE.md (этап 9).
