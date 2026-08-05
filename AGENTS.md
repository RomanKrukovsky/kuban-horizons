# AGENTS.md — регламент автономной разработки Kuban Horizons

Этот файл читается первым при возобновлении любой сессии разработки.

## Порядок возобновления работы

1. Прочитать `AGENTS.md` (этот файл).
2. Прочитать `PROJECT_STATE.md` — текущее состояние, следующий шаг.
3. Прочитать `ARCHITECTURE.md` — зафиксированные решения (AD-xxx).
4. Просмотреть открытые GitHub Issues (если репозиторий подключён).
5. Просмотреть последние коммиты: `git log --oneline -15`.
6. Проверить результаты CI (если доступны).
7. Продолжить с первого незавершённого шага из PROJECT_STATE.md.
   **Не начинать проект заново. Не переписывать рабочую архитектуру.**

## Роли агентов

| Роль | Зона ответственности | Основные файлы/пакеты |
|------|---------------------|----------------------|
| Lead Architect | архитектура, зависимости, интеграция результатов | ARCHITECTURE.md, build.gradle, registry |
| Gameplay Engineer | механики, блоки, предметы, производство | block, blockentity, item, crop, soil, irrigation, processing, food |
| World Generation Engineer | биомы, растения, структуры, worldgen | worldgen, biome, structure, datagen (worldgen-часть) |
| Data Engineer | datagen, рецепты, loot, теги, локализация | datagen, ресурсы lang |
| Art Director | ART_BIBLE, модели, текстуры, анимации | assets, ART_BIBLE.md |
| QA Engineer | GameTest, интеграционные тесты, сервер | gametest, TEST_PLAN.md |
| Performance Engineer | профилирование, память, сеть | PERFORMANCE.md, оптимизации |
| Release Engineer | CI, сборки, changelog, публикация | .github, CHANGELOG.md, релизы |

Правила параллельной работы:

- Два агента не изменяют один файл одновременно. Разбиение работ — по
  пакетам/каталогам из таблицы выше.
- Общие точки (registry-классы, локализация, datagen-провайдеры)
  изменяет только один агент за итерацию; интеграцию выполняет
  Lead Architect.
- Каждый агент обязан прогнать `./gradlew compileJava` (минимум) на своей
  зоне перед завершением задачи.

## Технические инварианты (нарушать нельзя)

- Minecraft 26.2, NeoForge 26.2.x, Java 25 — см. AD-001/AD-002.
- Никаких client-only классов в common/server-коде.
- Никаких заглушек, пустых методов и фиктивных тестов в `main`.
- Все JSON-данные — через datagen (AD-005), сгенерированное коммитится.
- Registry ID не переименовываются после публикации.
- Сторонние ассеты — только с записью в THIRD_PARTY_NOTICES.md.
- Сообщения коммитов — Conventional Commits на английском.

## Чек-лист перед коммитом

1. `git diff` просмотрен, отладочный мусор удалён.
2. Связанные тесты запущены.
3. Секреты/локальные конфиги не попали в индекс.
4. `build/`, `run/`, миры — не в индексе.
5. Обновлён PROJECT_STATE.md (после законченного этапа).

## Чек-лист «функция готова»

Функция считается готовой только при наличии: кода, моделей, текстур,
локализации ru+en, рецептов/loot/тегов (если применимо), звуков (если
применимо), GameTest, работоспособности на dedicated server.

## Команды

```bash
JAVA_HOME=<jdk21+> ./gradlew build              # сборка
./gradlew runData                               # datagen
./gradlew runClient                             # клиент
./gradlew runServer                             # dedicated server
./gradlew runGameTestServer                     # полный GameTest
git log --oneline -15                           # последние коммиты
```

Для запуска Gradle требуется установленный JDK (любой ≥ 17 для запуска
самого Gradle; toolchain Java 25 скачивается автоматически через
foojay-resolver). Локально: `JAVA_HOME=~/jdks/jdk-21.0.12+8/Contents/Home`.
