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
| `assets/kubanhorizons/sounds/entity/wild_boar/ambient.ogg` | собственный синтез (tools/soundgen) | CC BY-SA 4.0 |
| `assets/kubanhorizons/sounds/entity/wild_boar/hurt.ogg` | собственный синтез (tools/soundgen) | CC BY-SA 4.0 |
| `assets/kubanhorizons/sounds/entity/wild_boar/death.ogg` | собственный синтез (tools/soundgen) | CC BY-SA 4.0 |
| `assets/kubanhorizons/sounds/entity/nutria/ambient.ogg` | собственный синтез (tools/soundgen) | CC BY-SA 4.0 |
| `assets/kubanhorizons/sounds/entity/nutria/hurt.ogg` | собственный синтез (tools/soundgen) | CC BY-SA 4.0 |
| `assets/kubanhorizons/sounds/entity/nutria/death.ogg` | собственный синтез (tools/soundgen) | CC BY-SA 4.0 |
| `assets/kubanhorizons/sounds/entity/locust/ambient.ogg` | собственный синтез (tools/soundgen) | CC BY-SA 4.0 |
| `assets/kubanhorizons/sounds/entity/locust/hurt.ogg` | собственный синтез (tools/soundgen) | CC BY-SA 4.0 |
| `assets/kubanhorizons/sounds/entity/caucasian_shepherd/ambient.ogg` | собственный синтез (tools/soundgen) | CC BY-SA 4.0 |
| `assets/kubanhorizons/sounds/entity/caucasian_shepherd/hurt.ogg` | собственный синтез (tools/soundgen) | CC BY-SA 4.0 |
| `assets/kubanhorizons/sounds/entity/caucasian_shepherd/death.ogg` | собственный синтез (tools/soundgen) | CC BY-SA 4.0 |
| `assets/kubanhorizons/sounds/entity/sturgeon/flop.ogg` | собственный синтез (tools/soundgen) | CC BY-SA 4.0 |
| `assets/kubanhorizons/sounds/entity/gull/ambient.ogg` | собственный синтез (tools/soundgen) | CC BY-SA 4.0 |
| `assets/kubanhorizons/sounds/entity/gull/hurt.ogg` | собственный синтез (tools/soundgen) | CC BY-SA 4.0 |
| `assets/kubanhorizons/sounds/entity/heron/ambient.ogg` | собственный синтез (tools/soundgen) | CC BY-SA 4.0 |
| `assets/kubanhorizons/sounds/entity/heron/hurt.ogg` | собственный синтез (tools/soundgen) | CC BY-SA 4.0 |
| `assets/kubanhorizons/sounds/entity/manul/ambient.ogg` | собственный синтез (tools/soundgen) | CC BY-SA 4.0 |
| `assets/kubanhorizons/sounds/entity/manul/hiss.ogg` | собственный синтез (tools/soundgen) | CC BY-SA 4.0 |
| `assets/kubanhorizons/sounds/entity/manul/purr.ogg` | собственный синтез (tools/soundgen) | CC BY-SA 4.0 |
| `assets/kubanhorizons/sounds/entity/manul/hurt.ogg` | собственный синтез (tools/soundgen) | CC BY-SA 4.0 |
| `assets/kubanhorizons/sounds/entity/manul/death.ogg` | собственный синтез (tools/soundgen) | CC BY-SA 4.0 |
| `assets/kubanhorizons/sounds/entity/genie/snap.ogg` | собственный синтез (tools/soundgen) | CC BY-SA 4.0 |
| `assets/kubanhorizons/sounds/weather/dry_wind.ogg` | собственный синтез (tools/soundgen) | CC BY-SA 4.0 |

Голоса фауны давления, погодный суховей и щелчок джинна сгенерированы
детерминированным скриптом `tools/soundgen/generate_fauna_sounds.py`
(Python 3 + numpy, без сторонних сэмплов). Целостность связки
«событие → файл → субтитр» проверяется `tools/soundgen/check_sounds.py`.

Звуки маслопресса сгенерированы детерминированным скриптом
`tools/soundgen/generate_press_sounds.py` (Python 3 + numpy, без сторонних
сэмплов); кодирование в OGG Vorbis — python-модуль `soundfile` (libsndfile).

Голос кубанского манула (ambient, hiss, purr, hurt, death) сгенерирован
детерминированным скриптом `tools/soundgen/generate_manul_sounds.py`
(Python 3 + numpy, без сторонних сэмплов). Шипение синтезировано как
широкополосная турбулентность без тонального ядра, мурлыканье — как
амплитудная модуляция низкого шума частотой работы гортани (~25 Гц).
Скрипт дополнительно канонизирует serial number потока Ogg: libvorbis
выбирает его случайно, из-за чего одинаковый звук давал разные байты и
детерминизм нельзя было проверить контрольными суммами.

## Текстуры и модели

Все текстуры и модели — собственные (CC BY-SA 4.0, см. LICENSE-ASSETS).
Текстуры генерируются детерминированными скриптами `tools/texgen/`
(один запуск `python3 tools/texgen/generate_all.py` пересоздаёт все PNG).
Сторонние текстуры и модели не используются.

Четыре окраса кубанского манула (`textures/entity/manul_steppe.png`,
`manul_sand.png`, `manul_mountain.png`, `manul_silver.png`) генерируются
скриптом `tools/texgen/gen_manul.py`: одна развёртка, разные палитры.
Палитра — собственная, построена от опорной таблицы ART_BIBLE §2; фотографии
и чужие текстуры при этом не трассировались. Контраст и заполнение
проверяются `tools/texgen/check_entity_textures.py`.

## Шрифты

Используются только стандартные шрифты Minecraft. Собственные шрифты
не добавлялись.
