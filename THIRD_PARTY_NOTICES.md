# Сторонние компоненты и источники / Third-Party Notices

Этот файл фиксирует все сторонние компоненты, использованные в проекте
Kuban Horizons, и их лицензии. Любой новый сторонний ресурс (код, текстура,
модель, звук, шрифт) обязан быть внесён сюда **до** попадания в репозиторий.

## Код и инструменты сборки

| Компонент | Назначение | Лицензия | Источник |
|-----------|------------|----------|----------|
| NeoForge | Mod loader и API | LGPL-2.1 | https://github.com/neoforged/NeoForge |
| ModDevGradle | Gradle-плагин разработки модов | Apache-2.0 | https://github.com/neoforged/ModDevGradle |
| Gradle Wrapper | Система сборки | Apache-2.0 | https://gradle.org |
| MDK-26.2-ModDevGradle (шаблон) | Исходный шаблон структуры проекта | CC0-1.0 (см. TEMPLATE_LICENSE) | https://github.com/NeoForgeMDKs/MDK-26.2-ModDevGradle |

Minecraft принадлежит Mojang AB / Microsoft. Проект не распространяет
ресурсы Minecraft и требует легальной копии игры.

## Звуки

Каждый звук записывается здесь с указанием: файл, источник, автор,
лицензия, изменения.

| Файл | Источник | Лицензия |
|------|----------|----------|
| `assets/kubanhorizons/sounds/block/oil_press/creak.ogg` | собственный синтез (tools/soundgen) | CC BY-SA 4.0 |
| `assets/kubanhorizons/sounds/block/oil_press/work.ogg` | собственный синтез (tools/soundgen) | CC BY-SA 4.0 |
| `assets/kubanhorizons/sounds/block/oil_press/finish.ogg` | собственный синтез (tools/soundgen) | CC BY-SA 4.0 |

Звуки маслопресса сгенерированы детерминированным скриптом
`tools/soundgen/generate_press_sounds.py` (Python 3 + numpy, без сторонних
сэмплов); кодирование в OGG Vorbis — python-модуль `soundfile` (libsndfile).

## Текстуры и модели

Все текстуры и модели — собственные (CC BY-SA 4.0, см. LICENSE-ASSETS).
Текстуры генерируются детерминированными скриптами `tools/texgen/`
(один запуск `python3 tools/texgen/generate_all.py` пересоздаёт все PNG).
Сторонние текстуры и модели не используются.

## Шрифты

Используются только стандартные шрифты Minecraft. Собственные шрифты
не добавлялись.
