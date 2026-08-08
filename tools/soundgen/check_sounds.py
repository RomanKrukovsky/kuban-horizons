#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Проверка целостности звуков: каждое событие в sounds.json имеет файл на диске.

Существует потому, что этот класс ошибок в моде уже случался дважды и оба раза
не ловился глазами: девятнадцать голосов фауны были зарегистрированы в
KHSounds без .ogg, а щелчок джинна был объявлен в sounds.json и вызывался из
кода при полном отсутствии файла. Оба раза сборка проходила, датаген проходил,
тесты проходили — и только запущенная игра писала в лог «Missing sound for
event». Скрипт превращает эту тишину в падающую проверку.

Проверяет три вещи:
  1. каждый путь из sounds.json существует как .ogg на диске;
  2. каждое событие, зарегистрированное в KHSounds.java, объявлено в
     sounds.json (иначе файл может быть, а игра его не найдёт);
  3. у каждого события есть ключ субтитра в KHTranslations.java.

Запуск:  python3 tools/soundgen/check_sounds.py
Код возврата 1 при любом несоответствии — годится для CI и гейтов.
"""

import json
import os
import re
import sys

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
SOUNDS_JSON = os.path.join(
    ROOT, "src", "generated", "resources", "assets", "kubanhorizons",
    "sounds.json",
)
SOUNDS_DIR = os.path.join(
    ROOT, "src", "main", "resources", "assets", "kubanhorizons", "sounds",
)
KH_SOUNDS = os.path.join(
    ROOT, "src", "main", "java", "dev", "romankrukovsky", "kubanhorizons",
    "registry", "KHSounds.java",
)
KH_TRANSLATIONS = os.path.join(
    ROOT, "src", "main", "java", "dev", "romankrukovsky", "kubanhorizons",
    "datagen", "KHTranslations.java",
)


def main() -> int:
    problems = []

    with open(SOUNDS_JSON, encoding="utf-8") as fh:
        declared = json.load(fh)

    # 1. Файлы на диске.
    subtitles = {}
    for event, body in declared.items():
        if "subtitle" in body:
            subtitles[event] = body["subtitle"]
        for entry in body.get("sounds", []):
            name = entry if isinstance(entry, str) else entry.get("name", "")
            rel = name.split(":", 1)[-1]
            path = os.path.join(SOUNDS_DIR, rel + ".ogg")
            if not os.path.exists(path):
                problems.append(f"нет файла: {rel}.ogg (событие {event})")
            elif os.path.getsize(path) == 0:
                problems.append(f"пустой файл: {rel}.ogg (событие {event})")

    # 2. Все зарегистрированные события объявлены.
    with open(KH_SOUNDS, encoding="utf-8") as fh:
        registered = {m.replace(".", "_")
                      for m in re.findall(r'register\("([a-z_.]+)"\)', fh.read())}
    for event in sorted(registered - set(declared)):
        problems.append(f"событие {event} зарегистрировано, но нет в sounds.json")

    # 3. У каждого объявленного события есть субтитр в реестре строк.
    with open(KH_TRANSLATIONS, encoding="utf-8") as fh:
        translations = fh.read()
    for event, key in sorted(subtitles.items()):
        if f'"{key}"' not in translations:
            problems.append(f"нет субтитра {key} (событие {event})")

    print(f"события в sounds.json: {len(declared)}")
    print(f"события в KHSounds.java: {len(registered)}")
    print(f"файлов .ogg на диске: "
          f"{sum(len(f) for _, _, f in os.walk(SOUNDS_DIR))}")

    if problems:
        print(f"\nПРОБЛЕМ: {len(problems)}")
        for p in problems:
            print("  -", p)
        return 1
    print("\nOK: каждое событие объявлено, озвучено файлом и подписано.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
