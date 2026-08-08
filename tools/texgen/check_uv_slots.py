"""Проверка UV-развёрток четвероногих: краска не должна выходить за слоты.

Ловит два промаха, невидимых в редакторе (и ART_BIBLE §7.3, и здравый смысл
требуют проверять это не глазом):

  * пиксель вне всех слотов модели — краска на шве;
  * столкновение ИСПОЛЬЗУЕМЫХ граней двух частей — рисунок одной части
    окажется на другой.

Прямоугольник развёртки шире суммы граней: в углах box-UV остаются пустые
карманы, и модели намеренно селят туда мелкие части (ухо нутрии в углу
корпуса). Поэтому пересечение прямоугольников — не ошибка, а вот пересечение
самих граней — ошибка.

Слоты обязаны совпадать с texOffs в KubanQuadrupedModel и NutriaModel.
"""
import os
import sys

from PIL import Image

from gen_quadrupeds import faces

ENTITY_DIR = os.path.join(os.path.dirname(__file__), "..", "..",
                          "src/main/resources/assets/kubanhorizons/textures/entity")

# (имя, u, v, w, h, d) — ровно как texOffs/addBox в моделях.
# Развёртка манула одна на все четыре окраса, поэтому вынесена в константу:
# четыре копии списка разъехались бы при первой же правке модели.
MANUL_PARTS = [
    ("head", 0, 0, 8, 8, 8), ("body", 28, 8, 10, 16, 8),
    ("leg", 0, 16, 4, 6, 4), ("right_ear", 0, 32, 3, 2, 1),
    ("left_ear", 8, 32, 3, 2, 1), ("ruff", 16, 32, 10, 4, 2),
    ("tail", 42, 32, 3, 10, 3),
]

MODELS = {
    "wild_boar": [
        ("head", 0, 0, 8, 8, 8), ("body", 28, 8, 10, 16, 8),
        ("leg", 0, 16, 4, 7, 4), ("withers", 0, 32, 8, 4, 7),
        ("snout", 23, 32, 4, 3, 3), ("tail", 37, 32, 2, 6, 2),
    ],
    "caucasian_shepherd": [
        ("head", 0, 0, 8, 8, 8), ("body", 28, 8, 10, 16, 8),
        ("leg", 0, 16, 4, 12, 4), ("right_ear", 0, 32, 3, 3, 1),
        ("left_ear", 8, 32, 3, 3, 1), ("muzzle", 16, 32, 4, 3, 3),
        ("tail", 30, 32, 3, 7, 3),
    ],
    "nutria": [
        ("body", 0, 0, 6, 8, 7), ("head", 26, 0, 4, 4, 4),
        ("snout", 19, 0, 3, 2, 2), ("right_ear", 0, 0, 2, 2, 1),
        ("left_ear", 38, 0, 2, 2, 1), ("incisors", 58, 0, 2, 1, 1),
        ("tail", 44, 0, 2, 9, 2), ("right_hind", 52, 0, 2, 3, 2),
        ("left_hind", 52, 5, 2, 3, 2), ("right_front", 26, 8, 2, 3, 2),
        ("left_front", 34, 8, 2, 3, 2),
    ],
    # Манул: ванильная развёртка четвероногого плюс свои части ниже y=32.
    # Оболочка «шуба» намеренно переиспользует слот корпуса (28,8) — это та же
    # геометрия, раздутая CubeDeformation, поэтому она обязана делить UV с
    # корпусом и в список не входит: иначе проверка сочла бы это конфликтом.
    "manul_steppe": MANUL_PARTS,
    "manul_sand": MANUL_PARTS,
    "manul_mountain": MANUL_PARTS,
    "manul_silver": MANUL_PARTS,
}


def used_pixels(u, v, w, h, d):
    """Пиксели, которые реально занимают шесть граней куба."""
    out = set()
    for x0, y0, x1, y1 in faces(u, v, w, h, d).values():
        for y in range(y0, y1 + 1):
            for x in range(x0, x1 + 1):
                out.add((x, y))
    return out


def main():
    failures = []
    for name, parts in MODELS.items():
        path = os.path.join(ENTITY_DIR, name + ".png")
        if not os.path.exists(path):
            failures.append(f"{name}: файл не найден")
            continue
        im = Image.open(path).convert("RGBA")

        slots = {n: used_pixels(u, v, w, h, d) for n, u, v, w, h, d in parts}
        allowed = set()
        for s in slots.values():
            allowed |= s

        stray = [(x, y) for y in range(im.height) for x in range(im.width)
                 if im.getpixel((x, y))[3] > 0 and (x, y) not in allowed]

        names = list(slots)
        clashes = []
        for i in range(len(names)):
            for j in range(i + 1, len(names)):
                overlap = slots[names[i]] & slots[names[j]]
                if overlap:
                    clashes.append((names[i], names[j], len(overlap)))

        print(f"{name:<22} вне слотов: {len(stray):<5} "
              f"конфликтов граней: {len(clashes)}")
        if stray:
            failures.append(f"{name}: {len(stray)} px вне развёртки, "
                            f"первые {stray[:6]}")
        for a, b, n in clashes:
            failures.append(f"{name}: грани {a} и {b} налегают на {n} px")

    if failures:
        print("\nНАРУШЕНИЯ:")
        for f in failures:
            print("  ! " + f)
        return 1
    print("\nUV чистый: краска внутри слотов, грани не конфликтуют.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
