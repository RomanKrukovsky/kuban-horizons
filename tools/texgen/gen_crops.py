"""Текстуры культур Kuban Horizons. Детерминированная генерация."""
import os
from texlib import (PAL, img, px, rect, vline, hline, stem, leaf, blob,
                    checker_noise)

OUT = os.path.join(os.path.dirname(__file__), "..", "..",
                   "src/main/resources/assets/kubanhorizons/textures/block")


def save(im, name):
    os.makedirs(OUT, exist_ok=True)
    im.save(os.path.join(OUT, name + ".png"))
    print("block/" + name)


# ---------- Подсолнечник ----------

def sunflower_stage0():
    im = img()
    stem(im, 7, 12, 15, PAL["stem"], PAL["stem_dark"])
    leaf(im, 7, 13, -1, PAL["leaf"], PAL["leaf_hi"], PAL["leaf_dark"], 2)
    leaf(im, 8, 12, 1, PAL["leaf"], PAL["leaf_hi"], PAL["leaf_dark"], 2)
    return im


def sunflower_stage1():
    im = img()
    stem(im, 7, 8, 15, PAL["stem"], PAL["stem_dark"])
    leaf(im, 7, 12, -1, PAL["leaf"], PAL["leaf_hi"], PAL["leaf_dark"], 3)
    leaf(im, 8, 10, 1, PAL["leaf"], PAL["leaf_hi"], PAL["leaf_dark"], 3)
    leaf(im, 7, 9, -1, PAL["leaf"], PAL["leaf_hi"], PAL["leaf_dark"], 2)
    return im


def sunflower_stage2():
    im = img()
    stem(im, 7, 4, 15, PAL["stem"], PAL["stem_dark"])
    for y, d, s in ((13, -1, 3), (11, 1, 4), (9, -1, 4), (7, 1, 3), (5, -1, 2)):
        leaf(im, 7 + (d > 0), y, d, PAL["leaf"], PAL["leaf_hi"], PAL["leaf_dark"], s)
    return im


def sunflower_bottom(stage):
    im = img()
    thick = stage >= 4
    stem(im, 7, 0, 15, PAL["stem"], PAL["stem_dark"])
    if thick:
        vline(im, 6, 0, 15, PAL["stem_dark"])
    for y, d, s in ((13, -1, 4), (11, 1, 4), (8, -1, 4), (6, 1, 4), (3, -1, 3), (2, 1, 3)):
        leaf(im, 7 + (d > 0), y, d, PAL["leaf"], PAL["leaf_hi"], PAL["leaf_dark"], s)
    return im


def sunflower_top3():
    im = img()
    stem(im, 7, 8, 15, PAL["stem"], PAL["stem_dark"])
    # Закрытый бутон: зелёная «луковица» с жёлтым проблеском
    blob(im, 7, 5, 3, PAL["leaf"], PAL["leaf_hi"], PAL["leaf_dark"])
    px(im, 7, 3, PAL["sun_petal"])
    px(im, 6, 4, PAL["sun_petal"])
    leaf(im, 7, 10, -1, PAL["leaf"], PAL["leaf_hi"], PAL["leaf_dark"], 3)
    leaf(im, 8, 9, 1, PAL["leaf"], PAL["leaf_hi"], PAL["leaf_dark"], 3)
    return im


def sunflower_top4():
    im = img()
    stem(im, 7, 10, 15, PAL["stem"], PAL["stem_dark"])
    cx, cy = 7, 5
    # Лепестки: крест + диагонали, двойной слой
    for dx, dy in ((0, -4), (0, 4), (-4, 0), (4, 0),
                   (-3, -3), (3, -3), (-3, 3), (3, 3)):
        px(im, cx + dx, cy + dy, PAL["sun_petal"])
    for dx, dy in ((0, -3), (0, 3), (-3, 0), (3, 0),
                   (-2, -2), (2, -2), (-2, 2), (2, 2),
                   (-1, -3), (1, -3), (-3, -1), (3, -1),
                   (-3, 1), (3, 1), (-1, 3), (1, 3)):
        px(im, cx + dx, cy + dy, PAL["sun_petal_hi"] if dy < 0 or dx < 0 else PAL["sun_petal"])
    # Сердцевина с сетчатым узором семян
    checker_noise(im, cx - 2, cy - 2, cx + 2, cy + 2, PAL["sun_core"], PAL["sun_core_dark"])
    px(im, cx - 2, cy - 2, PAL["sun_core_dark"])
    leaf(im, 7, 12, -1, PAL["leaf"], PAL["leaf_hi"], PAL["leaf_dark"], 3)
    leaf(im, 8, 11, 1, PAL["leaf"], PAL["leaf_hi"], PAL["leaf_dark"], 3)
    return im


# ---------- Кукуруза ----------

def corn_stage(stage):
    """Одноблочные стадии 0-1."""
    im = img()
    h = (12, 7)[stage]
    stem(im, 7, h, 15, PAL["stem"], PAL["stem_dark"])
    for i, (y, d) in enumerate(((14, -1), (12, 1), (10, -1), (8, 1))):
        if y >= h:
            leaf(im, 7 + (d > 0), y, d, PAL["leaf"], PAL["leaf_hi"],
                 PAL["leaf_dark"], 2 + (i % 2))
    return im


def corn_bottom(stage):
    im = img()
    stem(im, 7, 0, 15, PAL["stem"], PAL["stem_dark"])
    for y, d, s in ((13, -1, 4), (10, 1, 4), (7, -1, 4), (4, 1, 3), (2, -1, 3)):
        leaf(im, 7 + (d > 0), y, d, PAL["leaf"], PAL["leaf_hi"], PAL["leaf_dark"], s)
    if stage >= 3:
        # Початок на стебле
        rect(im, 9, 8, 10, 12, PAL["corn_yellow"])
        px(im, 9, 8, PAL["corn_hi"])
        vline(im, 10, 8, 12, PAL["sun_core"])
        px(im, 9, 13, PAL["leaf_dark"])
    if stage >= 4:
        rect(im, 4, 6, 5, 10, PAL["corn_yellow"])
        px(im, 4, 6, PAL["corn_hi"])
        vline(im, 5, 6, 10, PAL["sun_core"])
        px(im, 4, 11, PAL["leaf_dark"])
    return im


def corn_top(stage):
    im = img()
    top = (9, 6, 3)[stage - 2]
    stem(im, 7, top, 15, PAL["stem"], PAL["stem_dark"])
    for y, d in ((14, -1), (12, 1), (10, -1), (8, 1)):
        if y > top:
            leaf(im, 7 + (d > 0), y, d, PAL["leaf"], PAL["leaf_hi"], PAL["leaf_dark"], 3)
    if stage >= 3:
        # Метёлка
        px(im, 7, top - 1, PAL["dry_grass"])
        px(im, 6, top, PAL["dry_grass"])
        px(im, 8, top, PAL["dry_grass"])
    return im


# ---------- Чайный куст ----------

def tea_stage(stage):
    im = img()
    w = (3, 4, 6, 7)[stage]
    h = (4, 6, 9, 10)[stage]
    cx = 7
    # Куполообразная крона
    for yy in range(h):
        y = 15 - yy
        half = max(1, int(w * (1 - (yy / h) ** 2) ** 0.5))
        for x in range(cx - half, cx + half + 1):
            shade = PAL["tea_leaf_hi"] if (x < cx and yy > h // 2) else PAL["tea_leaf"]
            if (x + y) % 3 == 0:
                shade = PAL["leaf_dark"]
            px(im, x, y, shade)
    # Свежие флеши на зрелом кусте
    if stage == 3:
        for x, y in ((4, 7), (7, 5), (10, 7), (6, 8), (9, 9)):
            px(im, x, y, PAL["tea_leaf_hi"])
            px(im, x, y - 1, PAL["leaf_hi"])
    # Стволик
    px(im, cx, 15, PAL["wood_dark"])
    return im


# ---------- Рис ----------

def rice_stage(stage):
    im = img()
    heights = (5, 8, 11, 13)
    h = heights[stage]
    for i, x in enumerate((4, 7, 10, 12)):
        top = 15 - h + (i % 2)
        c = PAL["rice_green"] if stage < 3 else PAL["rice_pale"]
        vline(im, x, top, 15, c)
        px(im, x + 1, top + 2, PAL["stem_dark"])
        if stage >= 2:
            # Поникающая метёлка
            px(im, x + 1, top, PAL["rice_pale"] if stage == 3 else PAL["dry_grass"])
            px(im, x + 2, top + 1, PAL["rice_pale"] if stage == 3 else PAL["dry_grass"])
    return im


# ---------- Томат ----------

def tomato_stage(stage):
    im = img()
    h = (4, 7, 10, 11)[stage]
    stem(im, 7, 15 - h, 15, PAL["stem"], PAL["stem_dark"])
    for y, d in ((14, -1), (12, 1), (10, -1), (8, 1), (6, -1)):
        if y >= 15 - h:
            leaf(im, 7 + (d > 0), y, d, PAL["leaf"], PAL["leaf_hi"], PAL["leaf_dark"], 3)
    if stage == 2:
        # Зелёные завязи
        blob(im, 5, 11, 1, PAL["leaf_hi"], PAL["leaf_hi"], PAL["leaf_dark"])
        blob(im, 10, 9, 1, PAL["leaf_hi"], PAL["leaf_hi"], PAL["leaf_dark"])
    if stage == 3:
        blob(im, 5, 11, 1, PAL["tomato_red"], PAL["tomato_hi"], PAL["chernozem_dark"])
        blob(im, 10, 9, 1, PAL["tomato_red"], PAL["tomato_hi"], PAL["chernozem_dark"])
        blob(im, 8, 12, 1, PAL["tomato_red"], PAL["tomato_hi"], PAL["chernozem_dark"])
    return im


# ---------- Виноградная лоза (на шпалере) ----------

def grape_vine(stage):
    im = img()
    # Извилистая лоза
    path = [(7, 15), (7, 14), (8, 13), (8, 12), (7, 11), (7, 10), (8, 9),
            (8, 8), (7, 7), (7, 6), (8, 5), (8, 4), (7, 3), (7, 2)]
    grow = (6, 10, 14, 14)[stage - 1]
    for i, (x, y) in enumerate(path[:grow]):
        px(im, x, y, PAL["wood"] if stage >= 3 else PAL["stem"])
    # Листья
    n_leaves = (2, 4, 6, 6)[stage - 1]
    spots = ((5, 12), (10, 11), (4, 8), (11, 7), (5, 4), (10, 3))
    for i in range(n_leaves):
        x, y = spots[i]
        rect(im, x, y, x + 1, y + 1, PAL["leaf"])
        px(im, x, y, PAL["leaf_hi"])
        px(im, x + 1, y + 1, PAL["leaf_dark"])
    # Гроздья на зрелой лозе
    if stage == 4:
        for cx, cy in ((5, 9), (10, 6), (7, 12)):
            blob(im, cx, cy, 1, PAL["grape_dark"], PAL["grape_hi"], PAL["chernozem_dark"])
            px(im, cx, cy + 1, PAL["grape_dark"])
            px(im, cx + 1, cy + 1, PAL["grape_dark"])
    return im


def main():
    save(sunflower_stage0(), "sunflower_crop_stage0")
    save(sunflower_stage1(), "sunflower_crop_stage1")
    save(sunflower_stage2(), "sunflower_crop_stage2")
    save(sunflower_bottom(3), "sunflower_crop_bottom_stage3")
    save(sunflower_bottom(4), "sunflower_crop_bottom_stage4")
    save(sunflower_top3(), "sunflower_crop_top_stage3")
    save(sunflower_top4(), "sunflower_crop_top_stage4")

    save(corn_stage(0), "corn_crop_stage0")
    save(corn_stage(1), "corn_crop_stage1")
    for s in (2, 3, 4):
        save(corn_bottom(s), f"corn_crop_bottom_stage{s}")
        save(corn_top(s), f"corn_crop_top_stage{s}")

    for s in range(4):
        save(tea_stage(s), f"tea_bush_stage{s}")
        save(rice_stage(s), f"rice_crop_stage{s}")
        save(tomato_stage(s), f"tomato_bush_stage{s}")

    for s in (1, 2, 3, 4):
        save(grape_vine(s), f"grape_vine_stage{s}")


if __name__ == "__main__":
    main()
