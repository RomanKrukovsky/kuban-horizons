"""Текстуры предметов Kuban Horizons (16×16 иконки)."""
import os
from texlib import (PAL, img, px, rect, vline, hline, blob, leaf, stem,
                    checker_noise, outline_soft)

OUT = os.path.join(os.path.dirname(__file__), "..", "..",
                   "src/main/resources/assets/kubanhorizons/textures/item")


def save(im, name):
    os.makedirs(OUT, exist_ok=True)
    im.save(os.path.join(OUT, name + ".png"))
    print("item/" + name)


def seed(im, x, y, flip=False):
    """Одна полосатая семечка (2×3)."""
    px(im, x, y, PAL["seed_white"])
    px(im, x, y + 1, PAL["seed_black"])
    px(im, x, y + 2, PAL["seed_black"])
    px(im, x + 1, y + 1, PAL["seed_white"] if flip else PAL["seed_black"])


def sunflower_seeds():
    im = img()
    for x, y, f in ((4, 4, 0), (8, 3, 1), (11, 5, 0), (3, 8, 1), (7, 8, 0),
                    (10, 9, 1), (5, 11, 0), (9, 12, 1)):
        seed(im, x, y, f)
    return im


def sunflower_head():
    im = img()
    cx, cy = 8, 8
    # Внешний круг лепестков
    for dx, dy in ((0, -6), (0, 6), (-6, 0), (6, 0), (-4, -4), (4, -4),
                   (-4, 4), (4, 4), (-2, -5), (2, -5), (-5, -2), (5, -2),
                   (-5, 2), (5, 2), (-2, 5), (2, 5)):
        c = PAL["sun_petal_hi"] if dy < 0 or dx < 0 else PAL["sun_petal"]
        px(im, cx + dx, cy + dy, c)
        # Второй слой ближе к центру
        px(im, cx + dx * 3 // 4, cy + dy * 3 // 4, PAL["sun_petal"])
    # Сердцевина с сеткой семян
    for y in range(cy - 3, cy + 4):
        for x in range(cx - 3, cx + 4):
            if abs(x - cx) + abs(y - cy) <= 4:
                px(im, x, y, PAL["sun_core"] if (x + y) % 2 else PAL["sun_core_dark"])
    return im


def bottle(liquid, liquid_hi):
    im = img()
    # Горлышко с пробкой
    rect(im, 7, 1, 8, 2, PAL["wood"])
    px(im, 7, 1, PAL["wood_hi"])
    rect(im, 6, 3, 9, 4, PAL["glass"])
    # Корпус
    rect(im, 4, 5, 11, 13, PAL["glass"])
    px(im, 4, 5, PAL["glass_hi"])
    px(im, 5, 5, PAL["glass_hi"])
    # Жидкость
    rect(im, 5, 7, 10, 12, liquid)
    px(im, 5, 7, liquid_hi)
    px(im, 6, 7, liquid_hi)
    vline(im, 5, 8, 11, liquid_hi)
    # Дно и контур
    hline(im, 13, 4, 11, PAL["glass_hi"])
    outline_soft(im, (70, 82, 90, 255))
    return im


def roasted_seeds():
    im = img()
    # Бумажный кулёк
    for y in range(5, 15):
        w = (y - 3) // 2
        rect(im, 8 - w, y, 8 + w, y, PAL["paper"])
        px(im, 8 - w, y, PAL["paper_dark"])
    hline(im, 14, 4, 12, PAL["paper_dark"])
    # Семечки сверху
    for x, y, f in ((6, 3, 0), (8, 2, 1), (10, 3, 0)):
        seed(im, x, y, f)
    return im


def oil_cake():
    im = img()
    # Прессованный диск
    for y in range(4, 12):
        w = 6 if 5 <= y <= 10 else 5
        rect(im, 8 - w, y, 7 + w, y, PAL["adobe"])
    hline(im, 4, 4, 11, PAL["paper"])
    hline(im, 11, 3, 12, PAL["paper_dark"])
    checker_noise(im, 4, 6, 11, 9, PAL["adobe"], PAL["paper_dark"], 3)
    outline_soft(im, (120, 98, 66, 255))
    return im


def soil_probe():
    im = img()
    # Медный щуп с Т-рукоятью (диагональ, как инструмент)
    for i in range(9):
        px(im, 4 + i, 13 - i, (184, 115, 51, 255))
        px(im, 5 + i, 13 - i, (140, 84, 38, 255))
    # Рукоять
    rect(im, 11, 2, 14, 3, PAL["wood"])
    px(im, 11, 2, PAL["wood_hi"])
    px(im, 14, 3, PAL["wood_darker"])
    # Остриё
    px(im, 3, 14, (216, 148, 80, 255))
    return im


def corn_kernels():
    im = img()
    for x, y in ((4, 4), (8, 3), (11, 5), (3, 8), (7, 7), (10, 9), (5, 11), (9, 12)):
        px(im, x, y, PAL["corn_yellow"])
        px(im, x + 1, y, PAL["corn_hi"])
        px(im, x, y + 1, PAL["sun_core"])
    return im


def corn_cob():
    im = img()
    # Початок по диагонали с листьями обёртки
    for i in range(8):
        rect(im, 4 + i, 10 - i, 6 + i, 11 - i, PAL["corn_yellow"])
    checker_noise(im, 5, 4, 11, 10, PAL["corn_yellow"], PAL["corn_hi"], 2)
    # Обёртка снизу
    for i in range(4):
        px(im, 3 + i, 13 - i, PAL["leaf"])
        px(im, 4 + i, 13 - i, PAL["leaf_hi"])
        px(im, 3 + i, 14 - i, PAL["leaf_dark"])
    outline_soft(im, PAL["sun_core"])
    return im


def grilled_corn():
    im = img()
    for i in range(8):
        rect(im, 4 + i, 10 - i, 6 + i, 11 - i, (206, 152, 46, 255))
    checker_noise(im, 5, 4, 11, 10, (206, 152, 46, 255), (176, 116, 38, 255), 2)
    # Подпал
    px(im, 6, 8, PAL["wood_darker"])
    px(im, 9, 6, PAL["wood_darker"])
    # Палочка
    for i in range(3):
        px(im, 2 + i, 15 - i, PAL["wood"])
    outline_soft(im, (120, 82, 30, 255))
    return im


def tea_sapling_item():
    im = img()
    stem(im, 7, 9, 14, PAL["stem"], PAL["stem_dark"])
    for x, y in ((6, 6), (7, 5), (8, 6), (5, 8), (9, 8), (7, 7)):
        px(im, x, y, PAL["tea_leaf"])
    px(im, 7, 5, PAL["tea_leaf_hi"])
    px(im, 9, 8, PAL["leaf_dark"])
    # Комок земли
    rect(im, 6, 14, 9, 15, PAL["chernozem"])
    px(im, 6, 14, PAL["chernozem_dark"])
    return im


def tea_leaves_item():
    im = img()
    # Три листа «два листика и почка»
    for cx, cy, d in ((5, 9, -1), (10, 8, 1), (8, 4, 0)):
        px(im, cx, cy, PAL["tea_leaf_hi"])
        px(im, cx + 1, cy + 1, PAL["tea_leaf"])
        px(im, cx - 1, cy + 1, PAL["tea_leaf"])
        px(im, cx, cy + 2, PAL["leaf_dark"])
        px(im, cx + d, cy + 3, PAL["stem_dark"])
    return im


def rice_seedlings_item():
    im = img()
    for x in (5, 8, 11):
        vline(im, x, 5, 12, PAL["rice_green"])
        px(im, x + 1, 7, PAL["stem_dark"])
        px(im, x, 4, PAL["leaf_hi"])
    # Связка
    hline(im, 11, 4, 12, PAL["dry_grass"])
    rect(im, 4, 13, 12, 14, PAL["chernozem"])
    return im


def rice_panicle_item():
    im = img()
    # Поникающая метёлка
    for i in range(7):
        px(im, 5 + i, 12 - i, PAL["dry_grass"])
    for x, y in ((10, 4), (11, 5), (12, 5), (11, 6), (12, 7), (13, 7)):
        px(im, x, y, PAL["rice_pale"])
        px(im, x, y + 1, PAL["dry_grass"])
    return im


def rice_item():
    im = img()
    for x, y in ((5, 5), (8, 4), (11, 6), (4, 8), (7, 8), (10, 9), (6, 11), (9, 12)):
        px(im, x, y, PAL["rice_pale"])
        px(im, x + 1, y, (255, 252, 240, 255))
    return im


def cooked_rice_item():
    im = img()
    # Миска
    for y in range(9, 14):
        w = 6 - (y - 9)
        rect(im, 8 - w, y, 7 + w, y, PAL["wood"])
    hline(im, 13, 6, 9, PAL["wood_darker"])
    vline(im, 2, 9, 11, PAL["wood_hi"])
    # Горка риса
    for y, w in ((8, 5), (7, 4), (6, 3), (5, 1)):
        rect(im, 8 - w, y, 7 + w, y, (240, 238, 226, 255))
    px(im, 5, 6, (255, 255, 248, 255))
    px(im, 7, 5, (255, 255, 248, 255))
    px(im, 10, 7, PAL["rice_pale"])
    return im


def grape_cutting_item():
    im = img()
    # Черенок с почкой и листиком
    for i in range(9):
        px(im, 4 + i, 13 - i, PAL["wood"])
        px(im, 5 + i, 13 - i, PAL["wood_dark"])
    px(im, 10, 6, PAL["leaf"])
    px(im, 11, 5, PAL["leaf_hi"])
    px(im, 11, 6, PAL["leaf_dark"])
    px(im, 6, 10, PAL["stem"])
    return im


def grapes_item():
    im = img()
    # Гроздь: треугольник ягод
    rows = ((5, 4, 11), (6, 5, 10), (8, 5, 10), (10, 6, 9), (12, 7, 8))
    for y, x0, x1 in rows:
        for x in range(x0, x1 + 1, 2):
            blob(im, x, y, 1, PAL["grape_dark"], PAL["grape_hi"], PAL["chernozem_dark"])
    # Веточка и лист
    px(im, 8, 2, PAL["wood_dark"])
    px(im, 8, 3, PAL["wood_dark"])
    px(im, 9, 2, PAL["leaf"])
    px(im, 10, 1, PAL["leaf_hi"])
    return im


def tomato_seeds_item():
    im = img()
    for x, y in ((5, 5), (9, 4), (12, 6), (4, 9), (8, 8), (11, 10), (6, 12)):
        px(im, x, y, (224, 214, 176, 255))
        px(im, x + 1, y, (198, 186, 148, 255))
    return im


def tomato_item():
    im = img()
    blob(im, 8, 9, 4, PAL["tomato_red"], PAL["tomato_hi"], (150, 40, 30, 255))
    # Чашелистик
    px(im, 8, 4, PAL["stem_dark"])
    for dx in (-2, -1, 1, 2):
        px(im, 8 + dx, 5, PAL["leaf"])
    px(im, 8, 5, PAL["leaf_dark"])
    return im


def fruit_item(c_main, c_hi, c_dark, leaf_offset=(2, -1)):
    im = img()
    blob(im, 8, 9, 4, c_main, c_hi, c_dark)
    px(im, 8, 4, PAL["wood_dark"])
    px(im, 8 + leaf_offset[0], 4 + leaf_offset[1], PAL["leaf"])
    px(im, 8 + leaf_offset[0] + 1, 4 + leaf_offset[1], PAL["leaf_hi"])
    return im


def peach_item():
    im = fruit_item(PAL["peach"], PAL["peach_hi"], (196, 112, 58, 255))
    # Бороздка персика
    vline(im, 8, 6, 12, (216, 128, 72, 255))
    return im


def walnut_item():
    im = img()
    blob(im, 8, 9, 3, PAL["walnut"], PAL["walnut_hi"], PAL["wood_darker"])
    # Бороздки скорлупы
    vline(im, 8, 7, 11, PAL["wood_darker"])
    px(im, 6, 8, PAL["wood_darker"])
    px(im, 10, 9, PAL["wood_darker"])
    return im


def dried_tea_item():
    im = img()
    # Кучка скрученного сухого листа
    for x, y in ((5, 8), (7, 7), (9, 8), (6, 10), (8, 10), (10, 10), (7, 12), (9, 12), (5, 11)):
        px(im, x, y, (74, 88, 46, 255))
        px(im, x + 1, y, (56, 68, 36, 255))
    px(im, 7, 7, (96, 112, 60, 255))
    px(im, 5, 8, (96, 112, 60, 255))
    return im


def dried_fruit_item():
    im = img()
    # Сморщенные дольки трёх оттенков
    for cx, cy, c, hi in ((5, 6, PAL["apricot"], PAL["apricot_hi"]),
                          (10, 7, PAL["plum"], PAL["plum_hi"]),
                          (7, 11, (176, 108, 60, 255), (204, 140, 88, 255))):
        rect(im, cx - 1, cy, cx + 1, cy + 1, c)
        px(im, cx - 1, cy, hi)
        px(im, cx + 1, cy + 1, PAL["chernozem_dark"])
        px(im, cx, cy - 1, c)
    return im


def main():
    save(dried_tea_item(), "dried_tea")
    save(dried_fruit_item(), "dried_fruit")
    save(sunflower_seeds(), "sunflower_seeds")
    save(sunflower_head(), "sunflower_head")
    save(bottle(PAL["oil"], PAL["oil_hi"]), "sunflower_oil")
    save(roasted_seeds(), "roasted_sunflower_seeds")
    save(oil_cake(), "oil_cake")
    save(soil_probe(), "soil_probe")

    save(corn_kernels(), "corn_kernels")
    save(corn_cob(), "corn_cob")
    save(grilled_corn(), "grilled_corn")

    save(tea_sapling_item(), "tea_sapling")
    save(tea_leaves_item(), "tea_leaves")

    save(rice_seedlings_item(), "rice_seedlings")
    save(rice_panicle_item(), "rice_panicle")
    save(rice_item(), "rice")
    save(cooked_rice_item(), "cooked_rice")

    save(grape_cutting_item(), "grape_cutting")
    save(grapes_item(), "grapes")

    save(tomato_seeds_item(), "tomato_seeds")
    save(tomato_item(), "tomato")

    save(peach_item(), "peach")
    save(fruit_item(PAL["apricot"], PAL["apricot_hi"], (196, 132, 40, 255)), "apricot")
    save(fruit_item(PAL["plum"], PAL["plum_hi"], (80, 42, 96, 255)), "plum")
    save(walnut_item(), "walnut")


if __name__ == "__main__":
    main()
