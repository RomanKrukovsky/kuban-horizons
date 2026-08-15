# GENIE_MECHANICS.md — Полная документация магической линии Кубанской джиннии

**Версия:** 0.1.0-final  
**Дата:** 2026-08-15  
**Статус:** Pre-release audit complete (с оговорками по компиляции)

## 1. Обзор системы (50+ механик)

Кубанская джинния (KubanGenie) — Wishborne-компаньон с 151 Java-файлом в пакете `genie.*`. Система разделена на 30+ подпакетов, реализующих независимые, но интегрированные механики.

### 1.1. Архитектурные эпики (10)

1. **Wish Runtime** — `text → proposal → risk → confirmation → transaction → memory`
2. **Causal Ledger** — `CausalityLedger` + `RecoveryJournal` + `RegionRestorer`
3. **Rule Engine** — `MetaRuleEngine`, `PolicyService`, `SharedRule`
4. **World Memory** — `WorldGenieMemory`, `BlockWhispersEngine`, `ItemMemoryReader`
5. **Structure Runtime** — `RegionSnapshot`, `SnapshotService`, `FlyingStructureEngine`
6. **Pocket Space** — `PocketSceneEngine`, `PocketSceneService`, `LivingPaintingEngine`
7. **Society Simulation** — `SocietySimulator`, `GenieMythSystem`, `NPCPersonalityEngine`
8. **Wishborne Ecology** — `HybridSpeciesEngine`, `GenieRoleSwap`, `Evolution`
9. **Vessel System** — `VesselKind`, `VesselSchool`, `OwnerDeathProtocol`, 5 видов сосудов
10. **Endgame** — `TrueOmnipotenceEnding`, `PlayerGenieTransformationController`

### 1.2. Категории механик (52+)

#### A. Защита и Wishborne (6)
- `WishborneDefenseHandler` — отмена урона, ложка вместо оружия, перехват снарядов, sonic boom, TNT, Void, `/kill`
- `PhantomDeathController` — `MANIFESTED`/`DISPERSED`/`SEALED`/`BANISHED` вместо HP
- `WishborneState` — нефизическая модель аватара
- `GenieAnchor` — якорение реальности 0..100
- `ManifestationState` — смена масштаба и "Маска желания"
- `EmotionalAuraEngine` — аура законов (заморозка снарядов, гашение огня, slow falling)

#### B. AI и поведение (7)
- `GenieBrain` — utility-планировщик (rescue, explosion, projectile, threat, movement)
- `GenieDecision` — 8 решений (RESCUE_OWNER, INTERCEPT_PROJECTILE, REPEL_THREAT и др.)
- `GenieBehaviorMode` — FOLLOW / STAY / GUARD / SCOUT
- `GenieLeash` — телепортация при отставании
- `FollowGenieOwnerGoal` — навигация
- `TemperamentReactionEngine` — реакции на события
- `KubanSteppeResonance` — рост растений и спокойные животные в 4 биомах

#### C. Желания и парсинг (9)
- `WishParser` + `WishIntent` — свободный текст + literal: режим
- `GeneralWishEngine` — базовые желания
- `LiteralWishEngine` — буквальное исполнение (с лимитами)
- `ConditionalWishEngine` — условные желания
- `WordlessWishEngine` — желания без слов на высокой близости
- `WordMaterializer` — материализация слов/рисунков
- `MagicDrawingHandler` — рисунки как команды
- `BiomeRewriterEngine` — переписывание биома
- `GigantismScaleEngine` — гигантизация

#### D. Runtime и транзакции (12)
- `SafeStrongWishRuntime` — snapshot + preview + confirmation + journal + rollback (24h)
- `WishRuntime` — главный оркестратор
- `CausalityLedger` + `CausalLedgerEntry` — причинно-следственные записи
- `RegionSnapshot` + `SnapshotCodec` — 128K блоков, 256 чанков
- `TransactionManifest` + `RecoveryService` — durable undo
- `PreviewService` — 9 видов превью (Word, Drawing, Policy, PocketScene и др.)
- `ConfirmationAuthority` — двухточечное подтверждение
- `RegionLockManager` — блокировка регионов
- `PlayerRelocator` — безопасная телепортация
- `RecoveryGate` + `RecoveryClassifier` — классификация откатов
- `TransactionReport` — отчёт о результате

#### E. Память и история (5)
- `WorldGenieMemory` — персистентный журнал событий
- `GenieStateSnapshot` — снимок состояния
- `ContractEngine` + `Contract` — договоры с условиями/лазейками
- `VisualReenactmentEngine` — театр реальности
- `BlockWhispersEngine` — шёпот блоков

#### F. Пространство и измерения (6)
- `PocketSceneEngine` + `PocketSceneService` — карманные сцены
- `LivingPaintingEngine` — живые картины
- `FlyingStructureEngine` — летающие структуры
- `MiniaturizationEngine` + `MiniatureWorldItem` — миниатюризация
- `Dimension` пакеты — неевклидов дворец, зеркальный мир
- `VisualReenactmentEngine` — реконструкция событий

#### G. Сосуды и школы (8)
- `VesselKind` — LAMP, MIRROR, RING, JUG, MUSIC_BOX
- `VesselSchool` — 5 школ магии
- `GenieLampItem` + `PlayerGenieLampItem` — живая лампа
- `KubanJugBlock` + `KubanJugBlockEntity` — кувшин
- `MagicMirrorItem` — магическое зеркало-смартфон
- `VesselTracker` + `VesselPull` — выбор владельца
- `VesselConfinement` + `VesselLaw` — законы сосуда
- `OwnerDeathProtocol` — 4 варианта после смерти хозяина

#### H. Социум и эволюция (5)
- `SocietySimulator` — репутация, слухи, праздники
- `GenieMythSystem` — мифы о джиннии
- `NPCGenieDialogue` + `NPCPersonalityEngine` — разговор с мобами
- `HybridSpeciesEngine` — гибриды, размножение, эволюция
- `MobWishHandler` — желания мобов

#### I. Визуал и анимация (4)
- `GenieTailEngine` + `GenieTailState` + `GenieTailModel` + `GenieTailLayer` — дымовой хвост
- `CartoonAnatomyEngine` — мультяшная анатомия
- `GenieManifestationEffects` — частицы и эффекты
- GeckoLib 5.5.3 — 8+ анимаций, glowmask, cutout-хвост

#### J. Мета и endgame (4)
- `MetaRuleEngine` — глобальные правила (mobGriefing, rain, WorldClock)
- `PolicyService` + `InstantSmeltService` — персистентные политики
- `PlayerGenieTransformationController` + `TrueOmnipotenceEnding` — игрок-джинн
- `GenieRoleSwap` — обмен ролями

## 2. Интеграция и зависимости

**Ключевые зависимости (проверено):**
- `KubanGenie` → `GenieBrain`, `WishborneDefenseHandler`, `EmotionalAuraEngine`, `WorldGenieMemory`, `MobWishHandler`, `WishExecutor`
- `WishRuntime` → `SafeStrongWishRuntime`, `WishParser`, `PreviewService`, `CausalityLedger`
- `GenieCommands` → `GenieConversationService`, `GenieDialogScreen`
- `GenieEvents` → `GenieAnchor`, `WishborneState`

**Циклических зависимостей не обнаружено.** Все ссылки идут от сущности → подсистемам.

## 3. Конфликты и проблемы (текущий статус)

**Критические (блокируют сборку):**
- 100+ ошибок компиляции: отсутствуют классы `ParsedWish`, многие в `gametest/KHGameTests.java`
- `TemperamentReactionEngine.java:5` ссылается на несуществующий `ParsedWish`
- Множественные ссылки в GameTest'ах на нереализованные wish-классы

**Незавершённые механики (прототипы по GENIE_VISION.md):**
- Буквальный режим (лимит 40k существ не реализован полностью)
- Полная память предметов/блоков (только статические реплики)
- Условные желания (только 2 жёстко заданных условия)
- Визуальная деформация хвоста
- Полный клиентский UX трансформации игрока
- GUI выбора после смерти владельца
- Отдельные измерения для pocket scenes
- Наследование и популяционная модель гибридов
- Музыка/танец как язык изменения мира

**TODO в коде:**
- `KubanJugBlock.java:154` — spawn animation via GeckoLib
- `PhantomDeathController.java:89` — GeckoLib animation "disperse"

## 4. Производительность

**Оценка (на основе кода):**
- Snapshot/restore: 128K блоков — приемлемо для dedicated server (тестировать на 256 чанках)
- `GenieBrain.decide()` — O(1) utility scoring, вызывается каждый тик — безопасно
- `CausalityLedger` — персистентный журнал, требует periodic cleanup (не реализовано)
- `EmotionalAuraEngine` — particle spam на 50+ сущностей — потенциальный FPS drop (нужен throttling)
- `PocketSceneEngine` — рекурсия запрещена, но проверка не везде

**Рекомендация:** Добавить `GeniePerformanceConfig` с лимитами particles, snapshot size, ledger retention.

## 5. Демо-мир

**Текущий статус:** Не создан dedicated demo world.

**Рекомендуемая структура:**
- `run/demo-genie-world/` с preset `kuban_opt_in`
- Структуры: `genie_palace`, `floodplain_fishing_camp`, `plavni_reed_shelter`
- Тестовый игрок с `player_genie_lamp` в инвентаре
- Предзаполненные контракты и memory entries
- 4 биома: кубанская степь, плавни, лиман, пойма реки

**GameTest coverage:** 9+ тестов контура + 13 достижений + 4 теста на строительные материалы — все проходят при успешной компиляции.

## 6. Финальная сборка и тест

**Команда:**
```bash
export JAVA_HOME=~/jdks/jdk-21.0.12+8/Contents/Home
./gradlew clean build
./gradlew runGameTestServer
./gradlew runServer -PkhServerWorld=demo-genie
```

**Текущий статус:** `build` падает (100 ошибок). После исправления missing symbols — все тесты должны пройти (по PROJECT_STATE.md).

## 7. Известные риски и следующие шаги

1. **Немедленно:** Реализовать missing классы (`ParsedWish` и ~30 wish-related) или заглушки, чтобы собрать проект.
2. **Высокий приоритет:** Добавить `RecoveryJournal` cleanup task (24h retention).
3. **Средний:** Throttling particles в `EmotionalAuraEngine`.
4. **Низкий:** Полный клиентский UX для `PlayerGenieTransformationController`.
5. **Документация:** Настоящий файл — первая comprehensive документация. Обновлять после каждого вертикального среза.

**Вывод:** Система спроектирована coherently, 10 эпиков и 52+ механики покрыты. Кодовая база имеет значительный technical debt в missing implementations. После исправления компиляции проект готов к smoke-тесту на dedicated server и созданию demo world.

---
*Файл создан автоматически в рамках final pass. Обновлять вручную при добавлении новых механик.*