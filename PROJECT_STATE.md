# PROJECT_STATE.md — состояние разработки

> Обновляется после каждого законченного этапа. При возобновлении сессии
> читать после AGENTS.md.

## Текущая версия
`0.1.0` (пре-релизная разработка, этап 1 «Фундамент»)

## Платформа (зафиксирована, AD-001/002)
- Minecraft **26.2**, NeoForge **26.2.0.48-beta**, Java **25** (toolchain),
  Gradle 9.2.1, ModDevGradle 2.0.143.
- Локальный JDK для запуска Gradle: `~/jdks/jdk-21.0.12+8/Contents/Home`
  (`JAVA_HOME=... ./gradlew ...`); toolchain 25 качается foojay-resolver.

## Активная ветка
`main` → https://github.com/RomanKrukovsky/kuban-horizons

## Последний успешный commit
- `90ebe96` fix(conflicts): resolve git conflict markers in tests, models, and network packets

## Завершено
- [x] Разведка версий: MDK-26.2-ModDevGradle, NeoForge 26.2.0.48-beta.
- [x] Каркас проекта; `./gradlew build` — успех; GitHub-репозиторий + CI.
- [x] Документация (полный набор) + шпаргалки API 26.2 в docs/dev/api-notes.
- [x] Модульные регистрации: KHBlocks/KHItems/KHBlockEntities/KHMenus/
      KHRecipes/KHSounds/KHCreativeTabs/KHFoods.
- [x] Конфиг: KHServerConfig (SERVER), KHClientConfig (CLIENT), валидация.
- [x] **Вертикальный контур (код)**: SunflowerCropBlock (двухблочная
      культура AGE 0–4), OilPressBlockEntity (4 слота, ручной+пассивный
      режим, анти-дюп, SchemaVersion), OilPressingRecipe (свой тип),
      OilPressMenu + OilPressScreen (26.2 GuiGraphicsExtractor).
- [x] Datagen: модели/blockstates, рецепты (вкл. кастомный тип), loot,
      теги, ru+en из единого KHTranslations, достижения (ветка
      подсолнечника), sounds.json, компост data maps. 49 файлов.
- [x] GameTest: 9 тестов контура (registry-based по схеме 26.2); актуальный
      общий счёт — в разделе «Выполненные тесты».
- [x] Звуки маслопресса: собственный синтез (tools/soundgen), 3 OGG.

- [x] **Плодородие**: ChunkFertilityData (chunk attachment,
      ValueIOSerializable, SchemaVersion), SoilFertility API (истощение/
      севооборот/компост/ленивое восстановление), интеграция с
      подсолнечником, почвенный щуп с сообщениями ru/en.
- [x] **Орошение**: IrrigationChannelBlock (DISTANCE 0..12, событийная
      волна scheduled ticks, FluidState-гидратация ванильных грядок),
      WaterIntakeBlock (ACTIVE), ручные модели, рецепты, лут, теги.
- [x] **Кукуруза**: DoubleCropBlock (общий базовый класс) + CornCropBlock,
      зёрна/початок/печёная кукуруза, жарка 3 способами.
- [x] **Чайный куст**: TeaBushBlock — многолетний, сбор ПКМ (1–2 листа,
      откат к стадии 1), саженец при разрушении.
- [x] **Рис**: RiceCropBlock — waterlogged-культура затопленного чека,
      рассада/метёлка/крупа/отварной рис.
- [x] **Виноград**: GrapeTrellisBlock — шпалера + прививка черенка,
      многолетний сбор гроздьев.
- [x] **Томат**: TomatoBushBlock — многосборный куст.
- [x] **Плодородие ↔ ваниль**: CropGrowEvent.Pre модулирует рост ванильных
      культур и стеблей арбуза/тыквы; BreakBlockEvent пишет севооборот.
      Для `melon_kuban` сохранены ванильные семена, стебель, плод и лут;
      сбор плода истощает именно грядку направленного к нему привязанного
      стебля, а не грунт под арбузом.
- [x] **GLM-источники семян**: все культуры достижимы в ванильном мире
      (трава/тростник/ягоды/азалия/подсолнух).
- [x] **Плодовые деревья**: FruitLeavesBlock (AGE 0..2, сбор ПКМ),
      FruitSaplingBlock (программная постройка дерева) — персик, абрикос,
      слива, грецкий орех.
- [x] **Дикие культуры в мире**: configured/placed features + biome
      modifiers (чай в джунглях, томат на равнинах, виноград в саваннах).
- [x] **Профессия «маслодел»**: POI = маслопресс, data-driven сделки
      2 уровней (datapack-реестры villager_trade/trade_set).
- [x] **Текстуры**: полный набор 84 PNG (tools/texgen, детерминированная
      генерация по палитре ART_BIBLE): все стадии культур, маслопресс,
      орошение, листва деревьев, предметы, GUI пресса. Клиентский атлас
      собирается без missing textures.
- [x] Чистый JAR: kubanhorizons-0.1.0.jar (без .cache и bbmodel).
- [x] **Сушилка**: DryingRack (4 слота, без GUI, день/небо/дождь),
      сушёный чай + сушёные фрукты.
- [x] **Ручная мельница**: HandMill (обороты ПКМ), мука/крупа/рис.
- [x] **Кухня**: хлеб, борщ (регенерация), мамалыга, чашка чая
      (скорость), мёд с орехами (поглощение), овощная закуска;
      ветка достижений кухни + челлендж «Дегустатор».
- [x] **Путеводитель по Кубани**: локализованная письменная книга из
      8 страниц выдаётся один раз при первом входе; сериализуемый player
      attachment с SchemaVersion предотвращает повторную выдачу и
      переносится после смерти.
- [x] **Биом «кубанская степь»**: datapack-биом с равнинной экосистемой,
      полями подсолнухов и структурами равнин; compact custom BiomeSource
      сохраняет ванильную multi-noise географию и рельеф, но opt-in Overworld
      публикует и генерирует только четыре биома мода. Обычный preset,
      существующие чанки, Nether и End не меняются.
- [x] **Плавни и лиман**: влажные datapack-биомы проецируют ванильные swamp
      и mangrove_swamp только в opt-in preset; плавни получают мелкие водные
      окна, лиман — грязевые берега через собственные noise settings поверх
      ванильных surface rules.
- [x] **Пойма реки**: заливной луг проецирует ванильную `river` в opt-in
      preset; остальные результаты, включая замёрзшие реки, становятся
      кубанской степью. Растительность следует
      cycle-safe порядку ванильной незамёрзшей реки: прибрежные деревья и
      кусты, цветы, трава, грибы и речная растительность; речная фауна плюс
      скот заливных пастбищ; речной ил (грязь/глина под водой, супесь на
      берегу) через surface rules; дикий рис на мелководье — второй источник
      культуры. Ежевика, высокая трава и явная кувшинка исключены из-за
      глобальных ограничений порядка shared placed features.
- [x] **Достижения по культурам**: ветки рисоводства (рассада → метёлка →
      отварной рис), виноградарства (черенок → шпалера → гроздь), чая
      (саженец → лист → купаж → чашка) и садоводства (саженец → первый плод
      → сушёные фрукты + челлендж «Кубанский сад»); 13 достижений, у каждого
      локализация ru+en, дерево проверяется GameTest'ом.
- [x] **Строительные материалы Кубани**: саман и ракушечник — полные
      семейства `BlockFamily` (блок, ступеньки, плита, стенка) с рецептами
      камнереза; плетень и калитка плетня стыкуются с ванильными
      деревянными оградами через теги FENCES/WOODEN_FENCES/FENCE_GATES.
      6 текстур (tools/texgen), 10 предметов, ru+en, лут (двойная плита
      отдаёт две), 4 GameTest'а.
- [x] **Ориентиры поймы и плавней**: одноэлементные jigsaw-структуры
      `floodplain_fishing_camp` и `plavni_reed_shelter` с ручными NBT-шаблонами,
      прямой привязкой строго к `river_floodplain`/`plavni` и отдельными
      разреженными random-spread наборами (48/16 и 44/14 чанков).
- [x] **Белёная штукатурка и наличник**: семейство `BlockFamily` из блока,
      ступенек и плиты, рецепты крафта и камнереза; отдельный горизонтально
      ориентируемый резной наличник с полой четырёхреечной геометрией,
      открытым центром и `noOcclusion`. Детерминированные 16×16 текстуры,
      ru+en, лут, теги и GameTest-регрессии.
- [x] **Черепица и декоративная керамика**: семейство `BlockFamily` из блока,
      ступенек и плиты, отдельный расписной керамический блок, крафты и явно
      namespaced-рецепты камнереза, ru+en, лут, shape tags и две новые
      детерминированные 16×16 текстуры.
- [x] **Кубанская джинния — техническая интеграция**: сущность с хитбоксом
      0.8×2.4, парящей навигацией и GeckoLib 5.5.3; geo/animation-ресурсы,
      бинарно-дизеренный cutout-хвост, glowmask и 8 анимаций.
- [x] **Кубанская джинния — первый gameplay-срез**: Wishborne отменяет
      физический урон; первая взаимодействующая персона становится хозяином;
      джинния следует за хозяином и телепортируется при отставании. Семь
      параметров отношений сохраняются в сущности и выводят характер.
- [x] **Адаптивный разум джиннии**: персистентный `GenieBrain` хранит приказ
      и память о спасениях, отражённых угрозах, перехваченных снарядах и
      желаниях. Доступны режимы follow/stay/guard/scout.
- [x] **Предиктивный utility-планировщик**: вместо жёсткой цепочки реакций
      мозг оценивает полезность всех доступных действий.
- [x] **Инвентаризация полной магической линии**: `GENIE_VISION.md` фиксирует
      весь пользовательский каталог механик.
- [x] **Свободный текст связан с локальным wish runtime**: `/genie` и экран диалога.
- [x] **Нефизическая модель поражения Wishborne**: `WishborneState` отделяет
      истинную сущность от аватара.
- [x] **Safe Strong-Wish Runtime**: именованный снимок, preview, confirmation,
      durable recovery journal, rollback и retained undo.
- [x] **Рабочий диалог джиннии**: ПКМ пустой рукой и клавиша G открывают
      экран свободного текста; каждый пакет проверяет владельца, UUID сущности
      и дистанцию на сервере.
- [x] **Диалог джиннии — визуальный и accessibility pass**: комплексная проработка UI (фокусные состояния, клавиатурная навигация, ARIA-метки, контраст, responsive-раскладка).
- [x] **Лампа NPC-джиннии и дворец**: крафтовая лампа привязывается к UUID
      единственной джиннии и настоящего хозяина. Вечная Кубань, зеркальный мир и мир картины.

## Выполненные тесты
- `./gradlew compileJava`, `runData`, `build` — успешно.
- Полный `runGameTestServer` мода: **165 тестов проходят** (`All 165 required tests passed`).
- Все JSON-ресурсы сгенерированы через datagen (AD-005).

## Известные ошибки
- GitHub Actions не запускается: «account is locked due to a billing
  issue» (ограничение аккаунта GitHub, не кода). Все проверки CI
  выполняются локально теми же командами.

## Незавершённые изменения / в работе
- Магическая линия имеет широкий набор компилируемых прототипов (ауры, память,
  мета-правила, условные желания, пространственные и сюжетные классы).
- Stage 5: cycle-safe порядок растительности поймы и структуры проверены;
  продолжается визуальная шлифовка.

## Механики джиннии: интеграция (этап «механики из MD»)
- Починена сборка после регрессии: восстановлены ~40 регистраций KHBlocks,
  объединён KHDataComponents (VESSEL_BOND/VESSEL_TYPE/REGION_PAYLOAD/SOUL_OWNER),
  vessel-система переведена на MC 26.2 API (Identifier, GuiGraphicsExtractor,
  InteractionResult, ValueInput/ValueOutput, SavedDataType, KeyMapping.Category),
  созданы недостающие wish-типы (ParsedWish, WishPlanResult, WishResult,
  WishOperation, BudgetCalculator, BudgetResult).
- Карманное измерение джиннии: PocketDimension (тип+стебель, flat void),
  вход/выход по TeleportTransition, запрет рекурсии, таймер сцены через конфиг.
- Клиентский UX трансформации игрока: PlayerTransformationScreen,
  TransformationHudOverlay, S2CTransformationSync, TransformationClientState.
- Экран Wishborne-состояния (4 состояния + шкала якорения), методы WishborneState
  (increaseAnchoring/isBanished/setCurrentState).
- Provenance-журнал предметов/блоков (SavedDataType + Codec), запрос «откуда
  предмет?» через WishExecutor.
- Буквальный режим: лимиты сущностей через KHServerConfig (max/чанк), тест.
- Условные желания: категория PROVENANCE, ConditionalWishEngine, тесты.
- Экран смерти владельца (OwnerDeathChoiceScreen) дополнен серверной логикой,
  SOUL_SHARD предмет + SOUL_OWNER компонент, KUBAN_JUG (блок+item+текстура).
- Исправлены регрессии: instant-smelt policy (read возвращал "false"),
  перенос структуры с сущностями, безопасные triggerAnim/пакеты для тестовых
  клиентов.

## Механики джиннии: вторая волна (12 субагентов)
- **Поворот области**: RegionRotateService (90/180/270°, блоки + block entity NBT),
  StructureRotatePreview/ConfirmedStructureRotate, WishRuntime.previewSelectedStructureRotate/
  confirmStructureRotate/executeStructureRotate, тест genie_runtime_region_rotate.
- **Движущиеся структуры**: FlyingStructureController (persistent SavedData,
  полёт со скоростью и длительностью, посадка через RegionRestorer),
  тик-хук в GenieEvents, желание «летающий дом» (GeneralWishEngine),
  конфиг genie.flyingHouseDurationTicks, тест genie_runtime_flying_structure.
- **Гибридная экология**: Genome (Codec+StreamCodec, менделевское скрещивание +
  мутации, поколения), PopulationControl (лимит на чанк, SavedData),
  интеграция HybridSpeciesEngine.tryReproduce + applyTraits,
  конфиг genie.hybridPopulationCapPerChunk, тест genie_ecology_genome_inheritance.
- **Музыка и танец**: MusicSpell (4 песни: дождь/рост/покой/огонь),
  DanceEngine (распознавание фигур движений), хуки LivingJumpEvent/onLevelTick,
  желание MUSIC_SPELL, тест genie_music_rain_song.
- **Условные желания**: ConditionalRule (7 триггеров, Codec) + ConditionalRuleStore
  (SavedData, tick с проверкой триггеров), переписан ConditionalWishEngine,
  тик-хук в GenieEvents, тест genie_conditional_rule_store.
- **Желания мобов**: MobWishMemory (SavedData, 3-уровневая эскалация квестов),
  интеграция MobWishHandler, тест genie_mob_wish_memory.
- **Комната невыполненных желаний**: UnfulfilledWishRoom (SavedData,
  материализация стеллажа с книгой), хук в WishExecutor, тест genie_unfulfilled_wish_room.
- **«А что если?»**: AlternativeCausalityEngine (сравнение снимков отменённых
  транзакций, без изменения мира), желание WHAT_IF, тест genie_alternative_causality.
- **Гигантизм**: GiantPieBuilder (пирог 5×2×5, кровать 4×1×6), BIG_PIE/BIG_BED
  в GigantismScaleEngine, тест genie_gigantism_pie.
- **Социум**: SocietySimulator (репутация + слухи, SavedData SocietyData),
  GenieMythSystem (мифы + ежегодный праздник), желание GENIE_FESTIVAL,
  тест genie_society_reputation.
- **Деформация хвоста**: tailIntensity в KubanGenie (SynchedEntityData, растёт при
  касте, гаснет при DISPERSED), TailPose API в CartoonAnatomyEngine.
- **Музыкальная шкатулка**: MusicBoxSchool (4 настроения-ауры: покой/радость/
  грусть/благоговение), регистрация 5 сосудов в KHItems (vessel_lamp/mirror/ring/
  jug/music_box) + текстуры, тест genie_music_box_school.
- **Полные сосуды (5 школ)**: WishExecutionSchool (лампа — желание с бумаги в
  offhand), IllusionSchool (зеркало — мираж/невидимость/успокоение),
  PersonalMagicSchool (кольцо — стойкость/стремительность/могущество),
  CreatureCreationSchool (кувшин — волк/эллай/овца-спутник) поверх уже готовой
  MusicBoxSchool; тест genie_vessel_schools.
- **Театр реальности**: VisualReenactmentEngine читает WorldGenieMemory и
  воспроизводит ближайшее событие («покажи, что здесь было» / "theater") —
  частицы по типу события (wish/rescue/village), без изменения мира;
  wish-таргет THEATER_REENACTMENT + локализация ru/en; тест genie_theater_reenactment.
- **Сны джиннии**: GenieDreamEngine при пробуждении игрока (PlayerWakeUpEvent)
  напоминает о невыполненных желаниях из UnfulfilledWishRoom или делится тёплым
  видением; локализация dream.reminder ru/en; тест genie_dream_reminder.
- **Слова, рисунки и биом через wish**: WordMaterializer/MagicDrawingHandler/
  BiomeRewriterEngine были подключены только к WishRuntime; теперь доступны
  через диалог/бумагу — таргеты WORD_MATERIALIZATION («напиши слово X»),
  DRAWING («нарисуй»), BIOME_REWRITE («перепиши биом в степь») в WishParser/
  WishExecutor; локализация ru/en; тест genie_wish_word_materialization.
- **Желания без слов**: WordlessWishEngine был orphan-движком; теперь вызывается
  из mobInteract при ПКМ с пустой рукой (доверенная джинния чинит грядку/камень
  по взгляду); добавлены setTrust/setAffection в GeniePersonality;
  тест genie_wordless_wish.
- **Шёпот блоков**: BlockWhispersEngine был orphan-движком; теперь доступен через
  wish-таргет BLOCK_WHISPER («о чём говорит блок» / "whisper") — джинния читает
  колокол/портал/древние блоки по взгляду; локализация whisper.empty;
  тест genie_block_whisper.
- **Склонности NPC**: NPCPersonalityEngine был orphan-движком; теперь доступен
  через wish-таргет NPC_PERSONALITY («сделай моба спокойным/деятельным») —
  джинния меняет скорость/агрессию ближайшего моба; локализация npc.modified/
  npc.none; тест genie_npc_personality.
- **Эффекты манифестации**: GenieManifestationEffects переведён на
  WishborneState.Presence и подключён в тик джиннии — рассеянная/запечатанная/
  изгнанная джинния окружена характерными частицами.
- **Память предмета**: ItemMemoryReader был orphan-движком; теперь доступен через
  wish-таргет ITEM_MEMORY («что помнит предмет») — джинния читает чары/повреждение
  предмета в руке; тест genie_item_memory.
- **Магическая фотография**: MagicPhotoEngine («сфотографируй это») сохраняет
  вид сцены (блоки+существа) в предмет MAGIC_PHOTO с описанием; предмет
  зарегистрирован + текстура + item model; тест genie_magic_photo.
- **Живые картины**: LivingPaintingEngine был доступен только в тесте; теперь
  wish-таргет LIVING_PAINTING («войди в живую картину» / "living painting")
  переводит игрока в зеркальный мир и обратно; локализация painting.entered/
  painting.missing; тест genie_living_painting_wish.
- **Летающий дом**: GeneralWishEngine.flyingHouse существовал, но wish-таргет
  FLYING_HOUSE не создавался парсером; теперь «подними мой дом в небо» /
  «летающий дом» доступны через wish; тест genie_flying_house_wish.
- **Магический двойник**: MagicDoppelgangerEntity был зарегистрирован, но не
  вызывался; wish-таргет MAGIC_DOPPELGANGER («создай моего двойника») спавнит
  копию игрока; локализация doppelganger.created; тест genie_doppelganger_wish.
- **Материализация намерения «мост»**: BridgeMaterializerEngine («построй мост»)
  поднимает дощатый мост над пропастью в направлении взгляда (до 16 блоков);
  локализация bridge.built/none; тест genie_bridge_wish.
- **165 GameTest проходят**: `All 165 required tests passed :)`.
- Фикс стабильности: testUnfulfilledWishRoom больше не ждёт count()==1 (общий
  SavedData с параллельными тестами).
- Исправлен флак: ConditionalRuleStore-тест возвращал время мира, ломая
  параллельные тесты (манул).

## Следующий конкретный шаг
1. Провести визуальный smoke-тест диалога, лампы, трансформации и pocket-измерения
   в клиенте (runClient).
2. Визуальная шлифовка Stage 5 (пойма/плавни).

## Verified: dedicated server smoke
- `runServer -PkhServerWorld=fresh-smoke` — запуск за 1.5s, без ошибок и крашей.
- Новое измерение `kubanhorizons:pocket` загружается и корректно сохраняется;
  `kubanhorizons:eternal_kuban` тоже. Все 5 измерений (overworld, eternal_kuban,
  pocket, nether, end) сохраняются штатно.
- Только ожидаемые warnings (offline mode, command ambiguity).

## Команды для продолжения
```bash
cd /Users/romanmolodyko/Documents/kuban-horizon
export JAVA_HOME=~/jdks/jdk-21.0.12+8/Contents/Home
./gradlew build            # проверка
./gradlew runData          # datagen
./gradlew runServer -PkhServerWorld=fresh-smoke  # отдельный серверный мир
```

## Важные архитектурные решения
- AD-001: MC 26.2.
- AD-002: Java 25 (NeoForge 26.2).
- AD-003: ModDevGradle. AD-004: русские комментарии/док, английские коммиты.
- AD-005: datagen — источник всех JSON. AD-006: версии схем сохранений.
- AD-007: новые биомы доступны через opt-in world preset.
- AD-008: LLM не исполняет мир напрямую (валидация на сервере).
- AD-009: сильные желания только как транзакции с causal ledger/rollback.
