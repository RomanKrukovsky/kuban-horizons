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
- `fd11ffb` feat: obtain mod seeds from vanilla world via GLM

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
- [x] GameTest: 9 тестов (registry-based по схеме 26.2) — **все проходят**.
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
- [x] **GLM-источники семян**: все культуры достижимы в ванильном мире
      (трава/тростник/ягоды/азалия/подсолнух).
- [🔨] **Плодовые деревья** (персик/абрикос/слива/орех) — фоновый агент.
- [🔨] **Текстуры** цепочки подсолнечника — фоновый агент.

## Выполненные тесты
- `./gradlew build`, `runData` — успех.
- `runGameTestServer`: **20/20** (подсолнечник 5, пресс 3, плодородие 3,
  орошение 3, кукуруза 1, чай 1, рис 2, реестры 1, сериализация 1).
- Dedicated server: чистый старт, мир создан, Done (1.2s), ошибок мода
  нет. Клиент: старт до окна, инициализация мода видна, 0 ERROR.

## Известные ошибки
- GitHub Actions не запускается: «account is locked due to a billing
  issue» (ограничение аккаунта GitHub, не кода). Все проверки CI
  выполняются локально теми же командами; workflow готов и заработает
  после разблокировки аккаунта.

## Незавершённые изменения / в работе
- Текстуры цепочки подсолнечника: фоновый агент генерирует
  (tools/texgen + assets/…/textures). До их появления модели ссылаются
  на отсутствующие текстуры — клиент покажет missing texture.
- GUI-текстура oil_press.png — тем же агентом.
- Проверка запуска dedicated server и клиента — в процессе.

## Следующий конкретный шаг
1. Дождаться текстур, проверить их качество, закоммитить.
2. `runServer` (eula=true уже в run/) — проверить чистый старт без
   client-классов; затем `runClient` — smoke.
3. Обновить CHANGELOG; пометить этап 2 в ROADMAP как завершённый.
4. Этап 3: плодородие (ChunkFertilityData attachment) и орошение.

## Команды для продолжения
```bash
cd /Users/romanmolodyko/Documents/kuban-horizon
export JAVA_HOME=~/jdks/jdk-21.0.12+8/Contents/Home
./gradlew build            # проверка
./gradlew runData          # datagen (после появления провайдеров)
```

## Важные архитектурные решения
- AD-001: MC 26.2 (конфликт «1.26.2 vs 1.21.1» в спецификации решён в
  пользу 26.2 — современная нумерация Mojang).
- AD-002: Java 25 (требование NeoForge 26.2; вместо Java 21 из спецификации).
- AD-003: ModDevGradle. AD-004: русские комментарии/док, английские коммиты.
- AD-005: datagen — источник всех JSON. AD-006: версии схем сохранений.
