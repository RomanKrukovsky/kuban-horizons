#!/usr/bin/env python3
"""Модели коптильни: холодная и топящаяся.

Геометрия описана кодом, а не рисуется руками, — ART_BIBLE §7 допускает
программную генерацию корректного Minecraft JSON, и так модель остаётся
воспроизводимой и ревьюабельной.

Силуэт (ART_BIBLE §4: устройство узнаваемо силуэтом): кирпичная топка снизу,
дощатая камера копчения сверху, труба на крыше. Отличие от сушильной рамы
читается мгновенно — рама открыта и пуста, коптильня закрыта и с трубой.
Различие двух состояний — только текстура топки и заслонки: холодная тёмная,
горячая светящаяся. Геометрия одна, чтобы блок не «дёргался» при поджиге.
"""
import json
import os

OUT = os.path.join(os.path.dirname(__file__), "..", "..",
                   "src/main/resources/assets/kubanhorizons/models/block")

ALL = ("down", "up", "north", "south", "west", "east")


def cube(comment, frm, to, tex, overrides=None, cullfaces=()):
    """Один куб: одна текстура на все грани плюс точечные переопределения."""
    faces = {}
    for face in ALL:
        entry = {"texture": tex}
        if face in cullfaces:
            entry["cullface"] = face
        faces[face] = entry
    for face, override in (overrides or {}).items():
        faces[face] = dict(faces[face], texture=override)
    return {"__comment": comment, "from": list(frm), "to": list(to), "faces": faces}


def smokehouse(lit):
    """Модель коптильни. lit=True — топка горит."""
    firebox = "#firebox_lit" if lit else "#firebox"
    textures = {
        "particle": "kubanhorizons:block/smokehouse_side",
        "side": "kubanhorizons:block/smokehouse_side",
        "top": "kubanhorizons:block/smokehouse_top",
        "brick": "kubanhorizons:block/smokehouse_brick",
        "firebox": "kubanhorizons:block/smokehouse_firebox",
        "firebox_lit": "kubanhorizons:block/smokehouse_firebox_lit",
    }
    elements = [
        # Кирпичная топка: низ устройства, где горят дрова. Заслонка смотрит
        # вперёд (north в модели — лицо блока до поворота blockstate).
        cube("Кирпичное основание — топка", (1, 0, 1), (15, 6, 15), "#brick",
             overrides={"north": firebox}, cullfaces=("down",)),
        # Камера копчения: дощатый сруб, где висит продукт.
        cube("Дощатая камера копчения", (2, 6, 2), (14, 14, 14), "#side",
             overrides={"up": "#top", "down": "#top"}),
        # Труба: главная деталь силуэта, отличающая коптильню от рамы.
        cube("Труба", (6, 14, 6), (10, 16, 10), "#brick",
             overrides={"up": "#brick"}),
    ]
    return {
        "parent": "minecraft:block/block",
        "textures": textures,
        "elements": elements,
    }


def save(model, name):
    os.makedirs(OUT, exist_ok=True)
    path = os.path.join(OUT, name + ".json")
    with open(path, "w", encoding="utf-8") as handle:
        json.dump(model, handle, ensure_ascii=False, indent=2)
        handle.write("\n")
    print("block/" + name)


def main():
    save(smokehouse(lit=False), "smokehouse")
    save(smokehouse(lit=True), "smokehouse_lit")


if __name__ == "__main__":
    main()
