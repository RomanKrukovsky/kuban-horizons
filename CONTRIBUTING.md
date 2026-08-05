# CONTRIBUTING.md

Спасибо за интерес к Kuban Horizons!

## Быстрый старт

1. Форкните репозиторий, клонируйте.
2. Требуется JDK для запуска Gradle (toolchain Java 25 скачается сам).
3. `./gradlew build` — сборка; `./gradlew runClient` — клиент.
4. Импорт в IntelliJ IDEA: открыть каталог как Gradle-проект.

## Правила

- Прочитайте ARCHITECTURE.md (архитектурные решения AD-xxx обязательны)
  и ART_BIBLE.md (для ассетов).
- Код: идентификаторы английские, комментарии/Javadoc — русские, UTF-8.
- JSON-данные — только через datagen (`./gradlew runData`), сгенерированное
  коммитится.
- Ассеты — только собственные или с совместимой лицензией + запись в
  THIRD_PARTY_NOTICES.md.
- Каждая механика — с GameTest.

## Коммиты

Conventional Commits, английский:

```
feat: add sunflower crop lifecycle
fix: prevent press recipe duplication
test: cover rice growth conditions
docs: describe soil fertility system
perf: cache irrigation graph updates
```

## Pull Request

1. Ветка от `main`: `feat/<область>` или `fix/<область>`.
2. `./gradlew build` и `./gradlew runGameTestServer` — зелёные.
3. `./gradlew runData` — без незакоммиченного диффа.
4. Обновите CHANGELOG.md (раздел Unreleased) и CONTENT_BIBLE.md (статусы).
5. CI должен пройти до слияния.

## Лицензии

Отправляя PR, вы соглашаетесь лицензировать код под GPL-3.0-or-later,
а ассеты — под CC BY-SA 4.0.
