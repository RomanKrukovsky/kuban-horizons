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


def powder_pile(c_main, c_hi, c_dark):
    im = img()
    # Горка порошка
    for y, w in ((12, 6), (11, 5), (10, 4), (9, 3), (8, 2), (7, 1)):
        rect(im, 8 - w, y, 7 + w, y, c_main)
    px(im, 5, 9, c_hi)
    px(im, 6, 8, c_hi)
    px(im, 7, 7, c_hi)
    hline(im, 12, 2, 13, c_dark)
    px(im, 10, 10, c_dark)
    return im


def bowl_dish(content, content_hi, garnish=None):
    """Миска с содержимым."""
    im = img()
    # Миска
    for y in range(9, 14):
        w = 6 - (y - 9)
        rect(im, 8 - w, y, 7 + w, y, PAL["wood"])
    hline(im, 13, 6, 9, PAL["wood_darker"])
    px(im, 2, 9, PAL["wood_hi"])
    px(im, 2, 10, PAL["wood_hi"])
    # Содержимое
    rect(im, 3, 7, 12, 8, content)
    hline(im, 7, 3, 12, content_hi)
    if garnish:
        for x, y, c in garnish:
            px(im, x, y, c)
    return im


def homemade_bread_item():
    im = img()
    # Каравай: округлый, с надрезами
    for y, x0, x1 in ((5, 5, 10), (6, 4, 11), (7, 3, 12), (8, 3, 12),
                      (9, 3, 12), (10, 4, 11)):
        rect(im, x0, y, x1, y, (196, 148, 78, 255))
    hline(im, 5, 5, 10, (222, 178, 106, 255))
    hline(im, 6, 4, 11, (212, 166, 94, 255))
    hline(im, 10, 4, 11, (156, 112, 56, 255))
    # Надрезы
    px(im, 6, 6, (156, 112, 56, 255))
    px(im, 9, 6, (156, 112, 56, 255))
    outline_soft(im, (128, 92, 46, 255))
    return im


def tea_cup_item():
    im = img()
    # Стакан янтарного чая
    rect(im, 5, 5, 10, 12, (178, 108, 38, 255))
    rect(im, 5, 5, 10, 6, (212, 142, 58, 255))
    vline(im, 5, 5, 12, (212, 142, 58, 255))
    # Стеклянные стенки
    vline(im, 4, 4, 13, PAL["glass"])
    vline(im, 11, 4, 13, PAL["glass"])
    hline(im, 13, 4, 11, PAL["glass_hi"])
    # Пар
    px(im, 7, 2, (232, 232, 232, 140))
    px(im, 8, 1, (232, 232, 232, 100))
    outline_soft(im, (90, 100, 108, 255))
    return im


def raw_bird_item(c_main, c_hi, c_dark, bone=True):
    """Сырая тушка птицы: округлое тело, кость слева, свет сверху-слева.

    Форма отличается от плода тем, что тело вытянуто по горизонтали и имеет
    выступающую косточку — иначе иконка мяса читается как ягода.
    """
    im = img()
    rect(im, 5, 7, 11, 11, c_main)
    rect(im, 6, 6, 10, 6, c_main)
    px(im, 6, 7, c_hi)
    px(im, 7, 6, c_hi)
    for x, y in ((5, 11), (11, 11), (6, 12), (10, 12)):
        px(im, x, y, c_dark)
    hline(im, 12, 7, 9, c_dark)
    if bone:
        px(im, 4, 6, PAL["paper"])
        px(im, 4, 5, PAL["paper"])
        px(im, 3, 5, PAL["paper_dark"])
        px(im, 5, 6, PAL["paper_dark"])
    return im


def spawn_egg_item(base, spots):
    """Яйцо-спавнер в ванильном силуэте: овал с пятнами вида."""
    im = img()
    for y in range(3, 13):
        half = 2 if y in (3, 12) else (3 if y in (4, 11) else 4)
        rect(im, 8 - half, y, 7 + half, y, base)
    px(im, 6, 5, (255, 255, 255, 90))
    for x, y in ((5, 7), (9, 6), (7, 9), (10, 10), (6, 11)):
        px(im, x, y, spots)
    return im


def slab_meat_item(c_main, c_hi, c_dark, marbled=False):
    """Кусок мяса без косточки: кабанина рубится куском, а не тушкой.

    Форма намеренно угловатее, чем у raw_bird_item: птичья тушка округлая с
    косточкой, а кабаний окорок — плотный ломоть. Иначе две иконки мяса в
    инвентаре не отличить.
    """
    im = img()
    rect(im, 3, 5, 12, 11, c_main)
    hline(im, 5, 4, 11, c_hi)
    vline(im, 3, 6, 10, c_hi)
    hline(im, 11, 4, 11, c_dark)
    vline(im, 12, 6, 10, c_dark)
    for x, y in ((3, 5), (12, 5), (3, 11), (12, 11)):
        px(im, x, y, (0, 0, 0, 0))
    if marbled:
        # Прожилки сала: две светлые линии, признак сырого мяса.
        for x in range(5, 11, 3):
            px(im, x, 7, c_hi)
            px(im, x + 1, 9, c_hi)
    return im


def smoked_glaze(im, c_glaze):
    """Копчёный лоск: редкие светлые точки по верхней кромке силуэта.

    Копчёность отличается от жареного не только тоном, но и блеском корочки.
    Точки ставятся только там, где уже есть пиксель, — глазурь не выходит за
    силуэт (ART_BIBLE §3: контраст силуэта важнее внутренней детализации).
    """
    for x, y in ((5, 6), (8, 5), (10, 7), (6, 8), (9, 9)):
        if im.getpixel((x, y))[3] != 0:
            px(im, x, y, c_glaze)
    return im


def smoked_fish_item():
    """Копчёный осётр: тёмно-янтарная тушка с лоском.

    Форма та же, что у сырого и жареного осетра (тот же зверь), но тон
    заметно темнее жареного — копчение идёт дольше и даёт корочку. Гребень
    жучков сохранён (cooked=False): при копчении тушку не чистят так, как
    перед жаркой, и это отличает иконку от cooked_sturgeon.
    """
    im = fish_item((132, 84, 44, 255), (186, 138, 84, 255),
                   (92, 56, 30, 255))
    return smoked_glaze(im, (214, 168, 106, 255))


def smoked_meat_item():
    """Копчёное мясо: ломоть глубокого коричневого тона с корочкой.

    Без прожилок сала (marbled=False): при копчении жир вытапливается, и
    светлых прожилок сырого мяса не остаётся — так копчёность не путается
    с raw_boar в инвентаре.
    """
    im = slab_meat_item((116, 68, 40, 255), (158, 100, 60, 255),
                        (80, 46, 26, 255))
    return smoked_glaze(im, (192, 130, 78, 255))


def fish_item(c_back, c_belly, c_dark, cooked=False):
    """Рыба в профиль: вытянутое тело, рострум и раздвоенный хвост.

    Осетра от ванильной рыбы отличает длинный нос — он и вынесен вперёд,
    иначе иконка читается как лосось.
    """
    im = img()
    # Тело.
    rect(im, 5, 6, 11, 10, c_back)
    rect(im, 5, 9, 11, 10, c_belly)
    hline(im, 6, 6, 10, c_back)
    # Рострум — заострённый нос слева.
    px(im, 4, 8, c_dark)
    px(im, 3, 8, c_dark)
    px(im, 2, 8, c_dark)
    # Хвост-вилка справа.
    px(im, 12, 7, c_dark)
    px(im, 13, 6, c_dark)
    px(im, 12, 9, c_dark)
    px(im, 13, 10, c_dark)
    # Спинной гребень жучков.
    for x in range(6, 11, 2):
        px(im, x, 5, c_dark if not cooked else c_back)
    px(im, 6, 8, (24, 20, 18, 255))  # глаз
    return im


def pelt_item(c_fur, c_fur_hi, c_dark):
    """Растянутая шкурка: трапеция с лапами по углам.

    Читается как шкура, а не как мясо, именно за счёт углов-лап и светлой
    изнанки по нижнему краю.
    """
    im = img()
    for i, y in enumerate(range(4, 12)):
        half = 3 + i // 3
        rect(im, 8 - half, y, 7 + half, y, c_fur)
    hline(im, 4, 5, 10, c_fur_hi)
    # Лапы: четыре угловых выступа.
    for x, y in ((3, 6), (12, 6), (2, 10), (13, 10)):
        px(im, x, y, c_fur)
        px(im, x, y + 1, c_dark)
    hline(im, 11, 4, 11, c_dark)
    return im


def bucket_with_fish_item(c_fish):
    """Ведро с рыбой: ванильный силуэт ведра плюс хвост наружу."""
    im = img()
    # Ведро.
    rect(im, 4, 7, 11, 13, PAL["iron"])
    hline(im, 7, 4, 11, PAL["iron"])
    vline(im, 4, 8, 12, (168, 168, 176, 255))
    vline(im, 11, 8, 12, PAL["iron_dark"])
    hline(im, 13, 5, 10, PAL["iron_dark"])
    # Дужка.
    px(im, 3, 6, PAL["iron_dark"])
    px(im, 12, 6, PAL["iron_dark"])
    hline(im, 5, 5, 10, PAL["iron"])
    # Рыба торчит хвостом вверх — так ванильные вёдра и читаются.
    rect(im, 7, 3, 8, 6, c_fish)
    px(im, 6, 3, c_fish)
    px(im, 9, 3, c_fish)
    return im


# --- Предметы системы желаний (джинния) ---------------------------------

def wooden_spoon_item():
    """Деревянная ложка: черпак в правом верхнем углу, черенок вниз-влево.

    Модель handheld, поэтому раскладка ванильная (как у soil_probe и
    ванильных инструментов): рабочая часть сверху-справа, рукоять уходит
    в нижний левый угол — именно за него предмет и «держат».
    Ложку от лопатки отличает округлый черпак с тёмной впадиной внутри:
    впадина и есть то, что читается как «ложка» в размере 16×16.
    """
    im = img()
    # Черпак: овал 5×4 в правом верхнем углу.
    for y, x0, x1 in ((2, 10, 12), (3, 9, 13), (4, 9, 13), (5, 10, 12)):
        rect(im, x0, y, x1, y, PAL["wood"])
    # Впадина — тёмное «нутро» ложки, главный признак силуэта.
    rect(im, 10, 3, 12, 4, PAL["wood_darker"])
    px(im, 10, 3, PAL["wood_dark"])
    # Блик по верхне-левой кромке черпака (свет сверху-слева).
    px(im, 10, 2, PAL["wood_hi"])
    px(im, 11, 2, PAL["wood_hi"])
    px(im, 9, 3, PAL["wood_hi"])
    # Тень по нижне-правой кромке черпака.
    px(im, 13, 4, PAL["wood_dark"])
    px(im, 12, 5, PAL["wood_dark"])
    # Черенок: из шейки черпака в нижний левый угол, тень справа.
    for i in range(8):
        x, y = 9 - i, 6 + i
        px(im, x, y, PAL["wood"])
        px(im, x + 1, y, PAL["wood_dark"])
    px(im, 9, 6, PAL["wood_hi"])  # шейка светлее — стык с черпаком
    px(im, 2, 13, PAL["wood_darker"])  # торец рукояти
    outline_soft(im, (66, 48, 30, 255))
    return im


def magic_mirror_item():
    """Магическое зеркало — «смартфон» джиннии: золотая оправа, лиловая гладь.

    По рецепту это золото + аметист + жемчужина Края, поэтому оправа
    золотая, а отражение — аметистово-лиловое с бликом Края. Ручка снизу
    делает силуэт зеркалом, а не монетой или самоцветом.
    """
    im = img()
    gold = (222, 176, 60, 255)
    gold_hi = (245, 214, 118, 255)
    gold_dark = (156, 116, 34, 255)
    glass_deep = (58, 40, 84, 255)
    glass_mid = (96, 66, 132, 255)
    glass_hi = (168, 138, 208, 255)
    # Оправа: круг радиусом 5 (диаметр 11) в верхней части иконки.
    for y, x0, x1 in ((1, 5, 9), (2, 3, 11), (3, 2, 12), (4, 2, 12),
                      (5, 2, 12), (6, 2, 12), (7, 2, 12), (8, 3, 11),
                      (9, 5, 9)):
        rect(im, x0, y, x1, y, gold)
    # Гладь внутри оправы.
    for y, x0, x1 in ((3, 5, 9), (4, 4, 10), (5, 4, 10), (6, 4, 10),
                      (7, 5, 9)):
        rect(im, x0, y, x1, y, glass_mid)
    # Глубина отражения снизу-справа, свет — сверху-слева.
    rect(im, 7, 6, 10, 7, glass_deep)
    px(im, 5, 4, glass_hi)
    px(im, 6, 3, glass_hi)
    px(im, 4, 5, glass_hi)
    # Искра Края в центре — зеркало смотрит «сквозь» мир.
    px(im, 7, 5, (214, 236, 230, 255))
    px(im, 8, 5, glass_hi)
    # Блик и тень оправы.
    px(im, 4, 2, gold_hi)
    px(im, 5, 1, gold_hi)
    px(im, 3, 3, gold_hi)
    hline(im, 9, 5, 9, gold_dark)
    px(im, 11, 8, gold_dark)
    px(im, 12, 7, gold_dark)
    # Ручка: короткий золотой хвостовик вниз.
    rect(im, 7, 10, 8, 14, gold)
    vline(im, 7, 10, 14, gold_hi)
    vline(im, 9, 11, 13, gold_dark)
    px(im, 8, 14, gold_dark)
    # Перехват ручки аметистом — отсылка к рецепту.
    px(im, 7, 12, (142, 104, 190, 255))
    px(im, 8, 12, (108, 76, 150, 255))
    outline_soft(im, (74, 52, 18, 255))
    return im


def miniature_world_item():
    """Сжатый карманный мир: стеклянная сфера с живым куском Кубани внутри.

    Внутри сферы читается срез мира — небо, степная трава и чернозём, —
    поэтому предмет не путается с обычной жемчужиной. Блик сверху-слева
    даёт стекло, тёмный обод снизу — объём.
    """
    im = img()
    sky = (128, 176, 206, 255)
    sky_hi = (176, 212, 232, 255)
    soil = PAL["chernozem"]
    soil_dark = PAL["chernozem_dark"]
    rim = (86, 104, 122, 255)
    # Сфера радиусом 6 (диаметр 13), чуть ниже центра иконки.
    rows = ((2, 6, 9), (3, 4, 11), (4, 3, 12), (5, 2, 13), (6, 2, 13),
            (7, 2, 13), (8, 2, 13), (9, 2, 13), (10, 3, 12), (11, 4, 11),
            (12, 6, 9))
    for y, x0, x1 in rows:
        rect(im, x0, y, x1, y, sky)
    # Горизонт: трава поверх чернозёма — «мир на ладони».
    for y, x0, x1 in rows:
        if y == 8:
            rect(im, x0, y, x1, y, PAL["steppe_grass"])
        elif y == 9:
            rect(im, x0, y, x1, y, soil)
        elif y in (10, 11, 12):
            rect(im, x0, y, x1, y, soil_dark)
    # Пригорок на горизонте, чтобы срез не был линейкой.
    rect(im, 5, 7, 7, 7, PAL["steppe_grass"])
    px(im, 5, 7, PAL["dry_grass"])
    px(im, 10, 8, PAL["dry_grass"])
    # Солнце-точка в небе сферы.
    px(im, 10, 4, PAL["sun_petal_hi"])
    # Стеклянный блик сверху-слева (два пикселя, без градиента).
    px(im, 5, 3, (238, 248, 252, 255))
    px(im, 4, 4, (238, 248, 252, 255))
    px(im, 6, 3, sky_hi)
    # Тёмный обод по нижне-правой дуге — объём стекла.
    for x, y in ((12, 10), (11, 11), (13, 8), (13, 9), (9, 12), (10, 11)):
        px(im, x, y, rim)
    outline_soft(im, (52, 62, 78, 255))
    return im


def sonic_boom_item():
    """Застывший звуковой вал: материализованная волна Вардена.

    Плотное ядро слева и три сплошные дуги, расходящиеся вправо, — силуэт
    читается как звук, а не как рог или кристалл. Дуги цельные и толщиной
    в два пикселя: рваный пунктир на 16×16 превращается в шум, что
    запрещено ART_BIBLE §1. Палитра бирюзово-чернильная, как у ванильного
    sonic_boom; дуги гаснут к краям, ближняя светлее дальней.
    """
    im = img()
    core = (222, 244, 248, 255)
    wave_hi = (126, 214, 224, 255)
    wave = (66, 158, 182, 255)
    wave_dark = (36, 96, 124, 255)
    # Ядро волны — плотный сгусток у левого края, свет сверху-слева.
    rect(im, 1, 6, 4, 9, wave)
    rect(im, 1, 6, 3, 8, wave_hi)
    px(im, 2, 7, core)
    px(im, 1, 7, core)
    hline(im, 9, 2, 4, wave_dark)
    px(im, 4, 8, wave_dark)
    # Три дуги: (x-основание, полувысота, основной тон, тон концов).
    # Дуга задаётся выпуклостью вправо — x растёт к середине по высоте.
    for base, span, c_main, c_tip in ((6, 5, wave_hi, wave),
                                      (9, 6, wave, wave_dark),
                                      (12, 4, wave_dark, wave_dark)):
        for dy in range(-span, span + 1):
            # Выпуклость: середина дуги выдаётся вправо на 1 пиксель.
            bulge = 1 if abs(dy) <= span // 2 else 0
            x, y = base + bulge, 8 + dy
            c = c_main if abs(dy) <= span - 1 else c_tip
            px(im, x, y, c)
            # Вторая колонка — толщина дуги, тень с внешней стороны.
            px(im, x + 1, y, c_tip)
    return im


def main():
    save(homemade_bread_item(), "homemade_bread")
    save(bowl_dish((176, 44, 52, 255), (204, 74, 70, 255),
                   [(5, 7, (238, 238, 238, 255)), (9, 7, PAL["leaf_hi"])]), "borscht")
    save(bowl_dish(PAL["corn_yellow"], PAL["corn_hi"]), "mamalyga")
    save(tea_cup_item(), "tea_cup")
    save(bowl_dish(PAL["oil"], PAL["oil_hi"],
                   [(5, 7, PAL["walnut"]), (8, 7, PAL["walnut"]), (10, 7, PAL["walnut_hi"])]), "honey_walnuts")
    save(bowl_dish((188, 96, 40, 255), (212, 124, 58, 255),
                   [(6, 7, PAL["tomato_red"]), (9, 7, PAL["leaf"])]), "vegetable_spread")
    save(powder_pile((238, 232, 216, 255), (250, 246, 236, 255), (206, 196, 172, 255)), "flour")
    save(powder_pile(PAL["corn_yellow"], PAL["corn_hi"], (188, 148, 48, 255)), "cornmeal")
    save(dried_tea_item(), "dried_tea")
    save(dried_fruit_item(), "dried_fruit")
    save(sunflower_seeds(), "sunflower_seeds")
    save(sunflower_head(), "sunflower_head")
    save(bottle(PAL["oil"], PAL["oil_hi"]), "sunflower_oil")
    # Виноградный сок: та же тара, что у масла (обе бутылки — продукт
    # переработки и возвращают стекло), но заливка — виноград тёмный
    # #4a2a52 из палитры региона (ART_BIBLE §2). Силуэт общий намеренно:
    # игрок должен читать «бутылка продукта», а сорт различать цветом.
    save(bottle(PAL["grape_dark"], PAL["grape_hi"]), "grape_juice")
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

    # Фауна: фазан и перепел. Сырое мясо холоднее и краснее, готовое — темнее
    # и теплее; перепел мельче фазана, поэтому его тушка на пиксель компактнее.
    save(raw_bird_item((196, 106, 104, 255), (222, 142, 138, 255),
                       (150, 68, 70, 255)), "raw_pheasant")
    save(raw_bird_item((186, 122, 96, 255), (214, 156, 128, 255),
                       (142, 84, 62, 255)), "raw_quail")
    save(raw_bird_item((150, 96, 52, 255), (182, 128, 74, 255),
                       (104, 62, 32, 255)), "cooked_pheasant")
    save(raw_bird_item((160, 112, 64, 255), (192, 144, 92, 255),
                       (112, 72, 40, 255)), "cooked_quail")
    # Яйца-спавнеры: базовый тон — оперение вида, пятна — акцент.
    save(spawn_egg_item((122, 84, 48, 255), (196, 64, 44, 255)),
         "pheasant_spawn_egg")
    save(spawn_egg_item((176, 156, 112, 255), (92, 72, 46, 255)),
         "quail_spawn_egg")

    # Кабан: мясо ломтем, а не тушкой — зверь крупный.
    save(slab_meat_item((176, 82, 84, 255), (206, 116, 116, 255),
                        (130, 54, 58, 255), marbled=True), "raw_boar")
    save(slab_meat_item((138, 86, 50, 255), (172, 116, 72, 255),
                        (98, 58, 32, 255)), "cooked_boar")
    # Осётр: тёмная спина, светлое брюхо; готовый теплее и без гребня.
    save(fish_item((104, 108, 96, 255), (208, 202, 180, 255),
                   (74, 78, 70, 255)), "raw_sturgeon")
    save(fish_item((166, 126, 78, 255), (216, 190, 146, 255),
                   (122, 88, 50, 255), cooked=True), "cooked_sturgeon")
    save(bucket_with_fish_item((104, 108, 96, 255)), "sturgeon_bucket")
    # Копчёности: следующая ступень после жарки, тон темнее и с лоском.
    save(smoked_fish_item(), "smoked_fish")
    save(smoked_meat_item(), "smoked_meat")
    # Шкурка нутрии — товарный продукт плавней.
    save(pelt_item((118, 88, 58, 255), (146, 112, 76, 255),
                   (86, 64, 42, 255)), "nutria_pelt")

    # Яйца-спавнеры остальной фауны: базовый тон — окрас вида, пятна — акцент.
    save(spawn_egg_item((74, 60, 48, 255), (92, 78, 62, 255)),
         "wild_boar_spawn_egg")
    save(spawn_egg_item((188, 170, 138, 255), (98, 86, 72, 255)),
         "caucasian_shepherd_spawn_egg")
    save(spawn_egg_item((118, 88, 58, 255), (226, 146, 46, 255)),
         "nutria_spawn_egg")
    save(spawn_egg_item((162, 148, 82, 255), (120, 108, 58, 255)),
         "locust_spawn_egg")
    # Манул: серо-песочная основа с янтарными крапинами — по окрасу зверя,
    # чтобы яйцо читалось как «тот самый кот», а не как ещё одно серое яйцо.
    save(spawn_egg_item((168, 156, 136, 255), (214, 158, 62, 255)),
         "manul_spawn_egg")
    save(spawn_egg_item((236, 238, 240, 255), (112, 126, 138, 255)),
         "gull_spawn_egg")
    save(spawn_egg_item((168, 172, 176, 255), (40, 42, 46, 255)),
         "heron_spawn_egg")
    save(spawn_egg_item((104, 108, 96, 255), (208, 202, 180, 255)),
         "sturgeon_spawn_egg")
    save(bottle((212, 172, 60, 255), (255, 220, 100, 255)), "genie_lamp")
    save(bottle((212, 172, 60, 255), (255, 220, 100, 255)), "player_genie_lamp")

    # Предметы системы желаний: ложка подмены, зеркало-«смартфон»,
    # сжатый карманный мир и застывшая волна Вардена.
    save(wooden_spoon_item(), "wooden_spoon")
    save(magic_mirror_item(), "magic_mirror")
    save(miniature_world_item(), "miniature_world")
    save(sonic_boom_item(), "sonic_boom_item")
    save(soul_shard_item(), "soul_shard")
    save(magic_photo_item(), "magic_photo")
    save(bottle((212, 172, 60, 255), (255, 220, 100, 255)), "vessel_lamp")
    save(magic_mirror_item(), "vessel_mirror")
    save(vessel_ring_item(), "vessel_ring")
    save(vessel_jug_item(), "vessel_jug")
    save(vessel_music_box_item(), "vessel_music_box")

def soul_shard_item():
    """Осколок души: фиолетовый кристалл с магической подписью джиннии."""
    im = img()
    SHARD_DARK = (86, 52, 128, 255)
    SHARD_MID = (128, 82, 178, 255)
    SHARD_HI = (178, 134, 222, 255)
    # Ромбический осколок: тёмная обводка, светлая грань слева.
    for dy, row in enumerate((8, 7, 6, 7, 6, 7, 8)):
        start = (15 - row) // 2
        for dx in range(row):
            px(im, start + dx, 3 + dy, SHARD_DARK)
    for dx, dy in ((6, 3), (7, 3), (8, 3), (9, 3), (5, 4), (10, 4),
                   (6, 4), (9, 4), (7, 5), (8, 5), (6, 6), (9, 6),
                   (7, 7), (8, 7)):
        px(im, dx, dy, SHARD_MID)
    for dx, dy in ((6, 3), (7, 3), (5, 4), (6, 4), (6, 5), (7, 5), (7, 7)):
        px(im, dx, dy, SHARD_HI)
    outline_soft(im, SHARD_DARK)
    return im


def vessel_ring_item():
    """Кольцо-сосуд: золотой обод с лиловым камнем магии."""
    im = img()
    gold = (222, 176, 60, 255)
    gold_hi = (245, 214, 118, 255)
    gold_dark = (156, 116, 34, 255)
    gem = (138, 96, 186, 255)
    gem_hi = (196, 158, 236, 255)
    rect(im, 3, 9, 12, 12, gold)
    rect(im, 5, 8, 10, 13, gold)
    px(im, 4, 9, gold_hi)
    px(im, 11, 9, gold_hi)
    for y, x0, x1 in ((4, 5, 10), (5, 4, 11), (6, 4, 11), (7, 5, 10)):
        rect(im, x0, y, x1, y, gold)
    px(im, 6, 3, gem_hi)
    px(im, 7, 4, gem)
    px(im, 8, 4, gem)
    px(im, 9, 5, gold_dark)
    px(im, 7, 5, gem_hi)
    return im


def vessel_jug_item():
    """Кувшин-сосуд (предмет): глиняная форма с синим орнаментом."""
    im = img()
    clay = (140, 98, 66, 255)
    clay_dark = (96, 64, 44, 255)
    blue = (66, 111, 151, 255)
    rect(im, 7, 2, 8, 4, clay_dark)
    rect(im, 5, 5, 10, 12, clay)
    rect(im, 6, 6, 9, 11, clay_dark)
    for x, y in ((5, 8), (6, 8), (9, 8), (10, 8), (5, 10), (10, 10)):
        px(im, x, y, blue)
    px(im, 7, 3, (196, 150, 110, 255))
    return im


def vessel_music_box_item():
    """Музыкальная шкатулка: тёмное дерево с золотой инкрустацией."""
    im = img()
    wood = (120, 82, 58, 255)
    wood_dark = (78, 50, 34, 255)
    gold = (222, 176, 60, 255)
    rect(im, 3, 6, 12, 12, wood)
    rect(im, 4, 5, 11, 6, wood_dark)
    for x, y in ((4, 9), (5, 9), (6, 9), (9, 9), (10, 9), (11, 9),
                 (5, 11), (6, 11), (9, 11), (10, 11)):
        px(im, x, y, gold)
    px(im, 7, 9, gold)
    px(im, 8, 9, gold)
    px(im, 7, 8, gold)
    px(im, 8, 8, gold)
    return im


def magic_photo_item():
    """Магическая фотография: лиловая рамка с сине-голубым снимком сцены."""
    im = img()
    frame = (88, 58, 120, 255)
    frame_hi = (150, 112, 180, 255)
    sky = (120, 176, 220, 255)
    land = (90, 130, 90, 255)
    rect(im, 2, 3, 13, 12, frame)
    rect(im, 3, 4, 12, 11, sky)
    rect(im, 3, 9, 12, 11, land)
    for x, y in ((4, 6), (5, 6), (8, 5), (9, 5), (6, 7), (7, 7), (11, 6), (4, 8), (11, 8)):
        px(im, x, y, (240, 244, 250, 255))
    px(im, 2, 3, frame_hi)
    px(im, 3, 3, frame_hi)
    px(im, 2, 4, frame_hi)
    return im


if __name__ == "__main__":
    main()
