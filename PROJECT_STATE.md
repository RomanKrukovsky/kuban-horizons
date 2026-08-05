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
`main` (репозиторий локальный; GitHub — в плане этапа 1)

## Последний успешный commit
- (заполняется при коммите)

## Завершено
- [x] Разведка версий: MDK-26.2-ModDevGradle, NeoForge 26.2.0.48-beta.
- [x] Каркас проекта: build.gradle, settings, gradle.properties, wrapper.
- [x] `./gradlew build` — **BUILD SUCCESSFUL** (первая сборка ~5 мин).
- [x] Главный класс `KubanHorizons`, mods.toml, lang-каркас ru/en.
- [x] Документация: README, ARCHITECTURE, AGENTS, GAME_DESIGN,
      CONTENT_BIBLE, ART_BIBLE, TECH_SPEC, TEST_PLAN, ROADMAP, CHANGELOG,
      CONTRIBUTING, LICENSE, LICENSE-ASSETS, THIRD_PARTY_NOTICES.

## Выполненные тесты
- `./gradlew build` — успех (без исходников тестов пока).

## Известные ошибки
- Нет.

## Незавершённые изменения
- Первый коммит ещё не создан (следующее действие).

## Следующий конкретный шаг
1. `git add -A && git commit` — первый чистый коммит каркаса.
2. Инфраструктура: registry-классы, конфиг, datagen, GameTest, CI
   (задача #4 в списке задач сессии).
3. Вертикальный контур подсолнечника (задача #5).

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
