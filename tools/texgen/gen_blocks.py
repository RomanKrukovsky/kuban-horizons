"""Текстуры блоков: маслопресс, желоб, водозабор, листва, саженцы,
строительные материалы (саман, ракушечник, плетень)."""
import os
from texlib import (PAL, img, px, rect, vline, hline, blob, leaf, stem,
                    checker_noise, plank_texture)

# Производные оттенки строительных материалов (ART_BIBLE §2: не более
# 8–10 тонов на текстуру, обводка тёмным тоном собственного цвета).
ADOBE_HI = (196, 170, 126, 255)
ADOBE_DARK = (148, 122, 82, 255)
ADOBE_MORTAR = (124, 100, 68, 255)

SHELL_HI = (232, 221, 192, 255)
SHELL_DARK = (188, 174, 140, 255)
SHELL_PORE = (160, 146, 116, 255)

PLASTER_HI = (245, 240, 226, 255)
PLASTER_MID = PAL["white_wall"]
PLASTER_DARK = (201, 193, 174, 255)
PLASTER_CRACK = (174, 164, 146, 255)
CASING_ACCENT = (106, 153, 178, 255)
CASING_ACCENT_DARK = (70, 112, 138, 255)
ROOF_HI = (190, 104, 67, 255)
ROOF_MID = (165, 80, 46, 255)
ROOF_DARK = (118, 53, 34, 255)
CERAMIC_WHITE = (232, 224, 208, 255)
CERAMIC_BLUE = (66, 111, 151, 255)
CERAMIC_BLUE_DARK = (44, 76, 112, 255)

OUT = os.path.join(os.path.dirname(__file__), "..", "..",
                   "src/main/resources/assets/kubanhorizons/textures/block")


def save(im, name):
    os.makedirs(OUT, exist_ok=True)
    im.save(os.path.join(OUT, name + ".png"))
    print("block/" + name)


# ---------- Маслопресс ----------

def oil_press_side():
    im = plank_texture(PAL["wood"], PAL["wood_hi"], PAL["wood_dark"], PAL["wood_darker"])
    # Вертикальные стойки станины
    for x in (1, 2, 13, 14):
        vline(im, x, 0, 15, PAL["wood_dark"] if x in (2, 14) else PAL["wood"])
        px(im, x, 0, PAL["wood_hi"])
    # Металлический обруч
    hline(im, 5, 0, 15, PAL["iron"])
    hline(im, 6, 0, 15, PAL["iron_dark"])
    hline(im, 11, 0, 15, PAL["iron"])
    hline(im, 12, 0, 15, PAL["iron_dark"])
    return im


def oil_press_front():
    im = oil_press_side()
    # Жёлоб стока масла по центру
    rect(im, 6, 8, 9, 15, PAL["wood_darker"])
    vline(im, 7, 9, 15, PAL["oil"])
    vline(im, 8, 9, 15, PAL["oil_hi"])
    px(im, 7, 15, PAL["oil_hi"])
    return im


def oil_press_top():
    im = plank_texture(PAL["wood"], PAL["wood_hi"], PAL["wood_dark"], PAL["wood_darker"])
    # Рама по периметру
    for i in range(16):
        px(im, i, 0, PAL["wood_hi"])
        px(im, i, 15, PAL["wood_darker"])
        px(im, 0, i, PAL["wood_hi"])
        px(im, 15, i, PAL["wood_darker"])
    # Отверстие с винтом
    rect(im, 5, 5, 10, 10, PAL["wood_darker"])
    rect(im, 6, 6, 9, 9, PAL["chernozem_dark"])
    # Винт: спиральные витки
    checker_noise(im, 6, 6, 9, 9, PAL["wood_dark"], PAL["wood_darker"])
    px(im, 7, 7, PAL["wood"])
    px(im, 8, 8, PAL["wood"])
    return im


def oil_press_bottom():
    im = plank_texture(PAL["wood_dark"], PAL["wood"], PAL["wood_darker"], PAL["wood_darker"])
    return im


def oil_press_screw():
    im = img()
    # Деревянный винт: вертикаль с резьбой
    rect(im, 6, 0, 9, 15, PAL["wood"])
    vline(im, 9, 0, 15, PAL["wood_darker"])
    vline(im, 6, 0, 15, PAL["wood_hi"])
    for y in range(0, 16, 3):
        hline(im, y, 6, 9, PAL["wood_dark"])
    return im


# ---------- Желоб ----------

def irrigation_channel_wood():
    return plank_texture(PAL["wood"], PAL["wood_hi"], PAL["wood_dark"], PAL["wood_darker"])


def irrigation_channel_inner():
    im = plank_texture(PAL["wood_dark"], PAL["wood"], PAL["wood_darker"], PAL["wood_darker"])
    # Потёки от воды
    for x in (3, 8, 12):
        vline(im, x, 2, 13, PAL["wood_darker"])
    return im


def irrigation_channel_water():
    im = img()
    rect(im, 0, 0, 15, 15, PAL["water"])
    # Блики волн (регулярный узор, не шум)
    for y in range(1, 16, 4):
        for x in range((y // 4) % 2 * 2, 16, 6):
            px(im, x, y, PAL["water_hi"])
            px(im, x + 1, y, PAL["water_hi"])
    return im


<<<<<<< HEAD
=======
<<<<<<< Updated upstream
=======
>>>>>>> 6e2806cd7fd1eec95181e4e48e492da66ebf50a8

# ---------- Каменный желоб и разделочный стол ----------

def stone_irrigation_channel():
    """Каменный желоб: тёсаный камень, суше и холоднее деревянного.

    Отличается от ракушечника отсутствием каверн: это обработанный камень,
    а не природный известняк, и апгрейд сети должен читаться как рукотворный.
    """
    im = img()
    rect(im, 0, 0, 15, 15, PAL["stone"])
    checker_noise(im, 0, 0, 15, 15, PAL["stone"], PAL["stone_dark"], 4)
    # Тёска: горизонтальные борозды инструмента.
    for y in (3, 8, 12):
        hline(im, y, 1, 14, PAL["stone_dark"])
    hline(im, 0, 0, 15, PAL["stone_hi"])
    vline(im, 0, 0, 15, PAL["stone_hi"])
    hline(im, 15, 0, 15, PAL["stone_dark"])
    vline(im, 15, 0, 15, PAL["stone_dark"])
    return im


def stone_irrigation_channel_inner():
    """Внутренняя поверхность: темнее и с потёками, как у деревянного."""
    im = img()
    rect(im, 0, 0, 15, 15, PAL["stone_dark"])
    checker_noise(im, 0, 0, 15, 15, PAL["stone_dark"], PAL["stone"], 5)
    for x in (3, 8, 12):
        vline(im, x, 2, 13, PAL["chernozem_dark"])
    return im


def cutting_board_top():
    """Столешница: доска со следами ножа вдоль волокна."""
    im = plank_texture(PAL["wood_hi"], PAL["paper"], PAL["wood"], PAL["wood_dark"])
    # Порезы — подпись именно разделочного стола, а не просто доски.
    for x, y0, y1 in ((4, 2, 9), (7, 5, 13), (11, 3, 11), (13, 7, 14)):
        vline(im, x, y0, y1, PAL["wood_dark"])
    for y, x0, x1 in ((6, 2, 6), (11, 8, 14)):
        hline(im, y, x0, x1, PAL["wood_dark"])
    return im


def cutting_board_side():
    """Бок: чистая доска, торец волокна."""
    return plank_texture(PAL["wood"], PAL["wood_hi"], PAL["wood_dark"],
                         PAL["wood_darker"])


<<<<<<< HEAD
=======
# ---------- Коптильня ----------
#
# Копчёная древесина темнее обычной: устройство должно читаться как
# «прокопчённое», а не как свежий сруб рамы. Оттенки взяты от wood_dark
# в сторону чернозёма — палитра региона (ART_BIBLE §2), не произвольный
# коричневый. Всего в текстуре 5–6 тонов, потолок §2 не тронут.

SMOKE_WOOD = (104, 78, 50, 255)
SMOKE_WOOD_HI = (126, 96, 62, 255)
SMOKE_WOOD_DARK = (78, 58, 38, 255)
SMOKE_WOOD_DARKER = (56, 42, 28, 255)
SMOKE_SOOT = (46, 38, 32, 255)

# Топка: кирпич региона (черепичный тон §2) и жар в устье.
FIRE_BRICK = (150, 74, 44, 255)
FIRE_BRICK_HI = (176, 96, 60, 255)
FIRE_BRICK_DARK = (108, 52, 32, 255)
EMBER = (238, 148, 44, 255)
EMBER_HI = (252, 202, 96, 255)
EMBER_DARK = (186, 92, 28, 255)


def smokehouse_side():
    """Бок сруба: прокопчённая доска с потёками сажи сверху.

    Сажа идёт от верха вниз — дым поднимается, и след остаётся под крышей.
    Это отличает бок коптильни от бока разделочного стола, хотя оба дощатые.
    """
    im = plank_texture(SMOKE_WOOD, SMOKE_WOOD_HI, SMOKE_WOOD_DARK,
                       SMOKE_WOOD_DARKER)
    # Потёки сажи: короткие вертикальные мазки от верхней кромки.
    for x, depth in ((2, 4), (5, 2), (8, 5), (11, 3), (14, 4)):
        vline(im, x, 0, depth, SMOKE_SOOT)
    hline(im, 0, 0, 15, SMOKE_SOOT)
    return im


def smokehouse_top():
    """Крыша камеры: доски поперёк с закопчённой серединой у трубы."""
    im = plank_texture(SMOKE_WOOD_HI, SMOKE_WOOD, SMOKE_WOOD_DARK,
                       SMOKE_WOOD_DARKER)
    # Кольцо сажи вокруг основания трубы (труба 4×4 по центру).
    for x in range(5, 11):
        px(im, x, 5, SMOKE_SOOT)
        px(im, x, 10, SMOKE_SOOT)
    for y in range(5, 11):
        px(im, 5, y, SMOKE_SOOT)
        px(im, 10, y, SMOKE_SOOT)
    return im


def smokehouse_brick():
    """Кирпичная кладка топки: смещённые ряды, свет сверху-слева."""
    im = img()
    rect(im, 0, 0, 15, 15, FIRE_BRICK)
    # Горизонтальные швы.
    for y in (3, 7, 11, 15):
        hline(im, y, 0, 15, FIRE_BRICK_DARK)
    # Вертикальные швы со сдвигом через ряд — иначе читается как сетка.
    for row, y0 in enumerate((0, 4, 8, 12)):
        offset = 0 if row % 2 == 0 else 4
        for x in range(offset, 16, 8):
            vline(im, x, y0, y0 + 2, FIRE_BRICK_DARK)
    # Блик по верхней кромке каждого ряда (свет сверху).
    for y0 in (0, 4, 8, 12):
        hline(im, y0, 0, 15, FIRE_BRICK_HI)
    return im


def smokehouse_firebox(lit=False):
    """Устье топки: заслонка в кирпичной рамке.

    Холодная — чёрный провал с сажей; горячая — угли и пламя. Форма устья
    одна и та же, поэтому состояние различается не только цветом (ART_BIBLE
    §5: различимо не только цветом — светятся именно угли внутри рамки).
    """
    im = smokehouse_brick()
    # Устье 8×6 по центру-низу.
    x0, y0, x1, y1 = 4, 7, 11, 13
    rect(im, x0, y0, x1, y1, SMOKE_SOOT)
    # Рамка устья — кирпич темнее, обводка своим тоном (§3).
    hline(im, y0 - 1, x0 - 1, x1 + 1, FIRE_BRICK_DARK)
    hline(im, y1 + 1, x0 - 1, x1 + 1, FIRE_BRICK_DARK)
    vline(im, x0 - 1, y0 - 1, y1 + 1, FIRE_BRICK_DARK)
    vline(im, x1 + 1, y0 - 1, y1 + 1, FIRE_BRICK_DARK)
    if lit:
        # Поленья углями: два ряда жара, ярче снизу — там горит.
        for x in range(x0 + 1, x1):
            px(im, x, y1 - 1, EMBER_DARK)
            px(im, x, y1, EMBER)
        for x in range(x0 + 2, x1 - 1, 2):
            px(im, x, y1 - 2, EMBER_HI)
        # Язычки пламени вверх, силуэт важнее детали (§3).
        px(im, x0 + 3, y0 + 1, EMBER)
        px(im, x0 + 4, y0, EMBER_HI)
        px(im, x1 - 2, y0 + 1, EMBER_DARK)
    else:
        # Холодная топка: остывшие дрова тёмным по тёмному, силуэт поленьев.
        for x in range(x0 + 1, x1, 3):
            px(im, x, y1 - 1, SMOKE_WOOD_DARKER)
            px(im, x + 1, y1, SMOKE_WOOD_DARK)
    return im


>>>>>>> Stashed changes
>>>>>>> 6e2806cd7fd1eec95181e4e48e492da66ebf50a8
# ---------- Водозабор ----------

def water_intake_side():
    im = img()
    rect(im, 0, 0, 15, 15, PAL["stone"])
    # Каменная кладка
    for y in (0, 5, 10, 15):
        hline(im, y, 0, 15, PAL["stone_dark"])
    for row, off in ((1, 5), (6, 10), (11, 3)):
        vline(im, off, row, row + 3, PAL["stone_dark"])
        vline(im, (off + 8) % 16, row, row + 3, PAL["stone_dark"])
    # Свет сверху
    hline(im, 1, 0, 15, PAL["stone_hi"])
    # Решётка входа
    rect(im, 5, 6, 10, 12, PAL["chernozem_dark"])
    for x in (6, 8, 10):
        vline(im, x, 6, 12, PAL["iron_dark"])
    hline(im, 6, 5, 10, PAL["iron"])
    return im


def water_intake_top():
    im = water_intake_side_base()
    rect(im, 3, 3, 12, 12, PAL["chernozem_dark"])
    for x in (4, 6, 8, 10, 12):
        vline(im, x, 3, 12, PAL["iron_dark"])
    hline(im, 3, 3, 12, PAL["iron"])
    return im


def water_intake_side_base():
    im = img()
    rect(im, 0, 0, 15, 15, PAL["stone"])
    hline(im, 0, 0, 15, PAL["stone_hi"])
    hline(im, 15, 0, 15, PAL["stone_dark"])
    vline(im, 0, 0, 15, PAL["stone_hi"])
    vline(im, 15, 0, 15, PAL["stone_dark"])
    return im


def water_intake_top_active():
    im = water_intake_side_base()
    rect(im, 3, 3, 12, 12, PAL["water"])
    for y in (5, 9):
        for x in (4, 8):
            px(im, x + (y // 5) % 2, y, PAL["water_hi"])
    for x in (4, 6, 8, 10, 12):
        vline(im, x, 3, 12, PAL["iron_dark"])
    hline(im, 3, 3, 12, PAL["iron"])
    return im


# ---------- Шпалера ----------

def grape_trellis():
    im = plank_texture(PAL["wood"], PAL["wood_hi"], PAL["wood_dark"], PAL["wood_darker"])
    return im


# ---------- Плодовая листва ----------

LEAF_SETS = {
    "peach": (PAL["peach"], PAL["peach_hi"]),
    "apricot": (PAL["apricot"], PAL["apricot_hi"]),
    "plum": (PAL["plum"], PAL["plum_hi"]),
    "walnut": (PAL["walnut"], PAL["walnut_hi"]),
}


def fruit_leaves(kind, stage):
    im = img()
    # Плотная листва с регулярным рисунком
    for y in range(16):
        for x in range(16):
            m = (x * 3 + y * 5) % 7
            if m == 0:
                c = PAL["leaf_dark"]
            elif m in (1, 2):
                c = PAL["leaf"]
            elif m == 3:
                c = PAL["leaf_hi"]
            else:
                c = PAL["leaf"]
            px(im, x, y, c)
    if stage == 1:
        # Цветение: бело-розовые соцветия (у ореха — серёжки dry_grass)
        bloom = PAL["blossom"] if kind != "walnut" else PAL["dry_grass"]
        bloom_hi = PAL["blossom_hi"] if kind != "walnut" else PAL["rice_pale"]
        for x, y in ((2, 3), (7, 1), (12, 4), (4, 8), (10, 9), (14, 12),
                     (1, 12), (6, 13), (11, 14)):
            px(im, x, y, bloom)
            px(im, x + 1, y, bloom_hi)
            px(im, x, y + 1, bloom_hi)
    elif stage == 2:
        c_main, c_hi = LEAF_SETS[kind]
        for x, y in ((2, 3), (7, 2), (12, 4), (4, 9), (10, 10), (14, 13), (6, 13)):
            blob(im, x, y, 1, c_main, c_hi, PAL["chernozem_dark"])
    return im


def fruit_sapling(kind):
    im = img()
    stem(im, 7, 8, 15, PAL["wood_dark"], PAL["wood_darker"])
    # Маленькая крона
    for x, y in ((6, 5), (7, 4), (8, 5), (7, 6), (5, 7), (9, 7), (6, 8), (8, 8), (7, 7)):
        px(im, x, y, PAL["leaf"])
    px(im, 6, 5, PAL["leaf_hi"])
    px(im, 7, 4, PAL["leaf_hi"])
    px(im, 9, 7, PAL["leaf_dark"])
    px(im, 8, 8, PAL["leaf_dark"])
    # Намёк на вид: цветная точка
    c_main, _ = LEAF_SETS[kind]
    px(im, 7, 5, c_main)
    return im


def drying_rack_side():
    im = plank_texture(PAL["wood"], PAL["wood_hi"], PAL["wood_dark"], PAL["wood_darker"])
    return im


def drying_rack_top():
    im = img()
    # Решётка из прутьев с просветами
    for x in range(16):
        for y in range(16):
            if y % 3 != 2:
                c = PAL["wood"] if y % 3 == 0 else PAL["wood_dark"]
                px(im, x, y, c)
    hline(im, 0, 0, 15, PAL["wood_hi"])
    return im


def hand_mill_side():
    im = img()
    rect(im, 0, 0, 15, 15, PAL["stone"])
    hline(im, 0, 0, 15, PAL["stone_hi"])
    hline(im, 15, 0, 15, PAL["stone_dark"])
    # Каменные насечки жёрнова
    for x in range(2, 15, 4):
        vline(im, x, 3, 12, PAL["stone_dark"])
    return im


def hand_mill_top():
    im = img()
    rect(im, 0, 0, 15, 15, PAL["stone"])
    # Радиальные борозды (упрощённо — крест и диагонали)
    for i in range(16):
        px(im, i, 7, PAL["stone_dark"])
        px(im, 7, i, PAL["stone_dark"])
        if 0 <= i < 16:
            px(im, i, i, PAL["stone_dark"])
            px(im, i, 15 - i, PAL["stone_dark"])
    # Отверстие в центре
    rect(im, 6, 6, 9, 9, PAL["chernozem_dark"])
    hline(im, 0, 0, 15, PAL["stone_hi"])
    return im


# ---------- Виноградный пресс (давильный чан) ----------
#
# Силуэт чана должен читаться иначе, чем станина маслопресса: у пресса
# вертикальные стойки и железный обруч по горизонтали, у чана — клёпки
# бочарной клепки и два широких обруча, то есть рисунок «бочка», а не
# «рама». Тон древесины взят светлее маслопресса (свежая клепка), чтобы
# два деревянных устройства не путались в инвентаре.
#
# Сок — виноград тёмный #4a2a52 из палитры региона (ART_BIBLE §2) плюс
# два производных оттенка на блик и тень. Всего в текстуре 7–8 тонов,
# потолок §2 не тронут.

VAT_WOOD = (158, 124, 78, 255)
VAT_WOOD_HI = (184, 150, 100, 255)
VAT_WOOD_DARK = (120, 92, 58, 255)
VAT_HOOP = (128, 126, 118, 255)
VAT_HOOP_DARK = (94, 92, 86, 255)

JUICE = PAL["grape_dark"]
JUICE_HI = (96, 58, 106, 255)
JUICE_DARK = (52, 28, 58, 255)


def grape_press_side():
    """Бок чана: бочарная клепка с двумя обручами.

    Вертикальные швы между клёпками — главное отличие от маслопресса, у
    которого бок читается горизонтальной доской с одной парой обручей.
    """
    im = img()
    rect(im, 0, 0, 15, 15, VAT_WOOD)
    # Клёпки: вертикальные полосы со светлой левой и тёмной правой кромкой
    # (свет сверху-слева, ART_BIBLE §3).
    for x in range(0, 16, 4):
        vline(im, x, 0, 15, VAT_WOOD_HI)
        vline(im, x + 3, 0, 15, VAT_WOOD_DARK)
    # Два обруча: широкие, у верха и у низа — «бочка», а не «рама».
    for y in (2, 12):
        hline(im, y, 0, 15, VAT_HOOP)
        hline(im, y + 1, 0, 15, VAT_HOOP_DARK)
    return im


def grape_press_top(level=0):
    """Вид сверху: обод чана и зеркало сока.

    Уровень 0 — сухое дно (видна клепка), 1..4 — сок поднимается. Именно
    эта текстура делает наполнение видимым без GUI.
    """
    im = img()
    # Обод по периметру
    rect(im, 0, 0, 15, 15, VAT_WOOD)
    for i in range(16):
        px(im, i, 0, VAT_WOOD_HI)
        px(im, 0, i, VAT_WOOD_HI)
        px(im, i, 15, VAT_WOOD_DARK)
        px(im, 15, i, VAT_WOOD_DARK)
    hline(im, 1, 1, 14, VAT_HOOP)
    vline(im, 1, 1, 14, VAT_HOOP)
    hline(im, 14, 1, 14, VAT_HOOP_DARK)
    vline(im, 14, 1, 14, VAT_HOOP_DARK)
    # Дно: доски настила
    rect(im, 2, 2, 13, 13, VAT_WOOD_DARK)
    for y in range(3, 13, 3):
        hline(im, y, 2, 13, VAT_WOOD)
    if level <= 0:
        return im
    # Зеркало сока: площадь растёт с уровнем, поэтому «мало сока» и «полный
    # чан» отличаются на глаз, а не только числом в подсказке.
    inset = {1: 4, 2: 3, 3: 2, 4: 2}[level]
    rect(im, inset, inset, 15 - inset, 15 - inset, JUICE)
    # Блик сверху-слева и тень снизу-справа — по ART_BIBLE §3.
    hline(im, inset, inset, 15 - inset, JUICE_HI)
    vline(im, inset, inset, 15 - inset, JUICE_HI)
    hline(im, 15 - inset, inset, 15 - inset, JUICE_DARK)
    vline(im, 15 - inset, inset, 15 - inset, JUICE_DARK)
    if level == 4:
        # Полный чан: пена/ягодная кожица пятнами, осмысленное «зерно» §2.
        for x, y in ((5, 6), (9, 5), (7, 9), (11, 10), (6, 11)):
            px(im, x, y, JUICE_HI)
    return im


def grape_press_bottom():
    """Дно чана: тёмная клепка без обручей."""
    im = img()
    rect(im, 0, 0, 15, 15, VAT_WOOD_DARK)
    for x in range(0, 16, 4):
        vline(im, x, 0, 15, VAT_WOOD)
    return im


# ---------- Строительные материалы ----------

def adobe_bricks():
    """Саманный кирпич: глиняно-соломенные блоки с широкими швами."""
    im = img()
    rect(im, 0, 0, 15, 15, PAL["adobe"])
    # Горизонтальные швы (четыре ряда по 4 пикселя)
    for y in (0, 4, 8, 12):
        hline(im, y, 0, 15, ADOBE_MORTAR)
        hline(im, y + 1, 0, 15, ADOBE_HI)
        hline(im, y + 3, 0, 15, ADOBE_DARK)
    # Вертикальные швы со сдвигом через ряд
    for row, off in ((0, 7), (1, 3), (2, 11), (3, 3)):
        y0 = row * 4 + 1
        vline(im, off, y0, y0 + 2, ADOBE_MORTAR)
    # Соломенные вкрапления самана: короткие светлые штрихи
    for x, y in ((2, 2), (10, 2), (5, 6), (13, 6), (3, 10), (9, 10), (6, 14),
                 (12, 14)):
        px(im, x, y, PAL["dry_grass"])
    return im


def shell_rock():
    """Ракушечник: пористый светлый известняк с раковинными вкраплениями."""
    im = img()
    rect(im, 0, 0, 15, 15, PAL["shell_rock"])
    # Поры — осмысленное «зерно» камня, а не случайный шум
    checker_noise(im, 0, 0, 15, 15, PAL["shell_rock"], SHELL_DARK, 3)
    # Свет сверху-слева, тень снизу-справа
    hline(im, 0, 0, 15, SHELL_HI)
    vline(im, 0, 0, 15, SHELL_HI)
    hline(im, 15, 0, 15, SHELL_DARK)
    vline(im, 15, 0, 15, SHELL_DARK)
    # Крупные раковинные каверны
    for x, y in ((4, 3), (11, 5), (6, 10), (13, 12), (2, 13)):
        px(im, x, y, SHELL_PORE)
        px(im, x + 1, y, SHELL_PORE)
        px(im, x, y + 1, SHELL_PORE)
        px(im, x + 1, y - 1, SHELL_HI)
    return im


def whitewashed_plaster():
    """Белёная штукатурка: спокойная известковая поверхность с мазками."""
    im = img()
    rect(im, 0, 0, 15, 15, PLASTER_MID)
    # Нерегулярность создаётся фиксированными мазками, без случайного шума.
    for x, y, length in ((1, 2, 5), (9, 1, 4), (4, 6, 7), (0, 10, 4),
                         (8, 12, 6), (3, 15, 5)):
        hline(im, y, x, min(15, x + length), PLASTER_HI)
    for x, y, length in ((6, 3, 3), (1, 8, 5), (11, 7, 3), (5, 13, 4)):
        hline(im, y, x, min(15, x + length), PLASTER_DARK)
    # Две тонкие усадочные трещины, читаемые, но не превращающие стену в руину.
    for x, y in ((13, 2), (13, 3), (12, 4), (12, 5), (3, 11), (4, 12), (4, 13)):
        px(im, x, y, PLASTER_CRACK)
    return im


def roof_tiles():
    """Черепица: повторяющиеся выпуклые ряды обожжённой глины."""
    im = img()
    rect(im, 0, 0, 15, 15, ROOF_MID)
    for y in (0, 4, 8, 12):
        hline(im, y, 0, 15, ROOF_HI)
        hline(im, min(15, y + 3), 0, 15, ROOF_DARK)
        offset = 0 if (y // 4) % 2 == 0 else 4
        for x in range(offset, 16, 8):
            vline(im, x, y, min(15, y + 3), ROOF_DARK)
            if x + 1 < 16:
                vline(im, x + 1, y, min(15, y + 2), ROOF_HI)
    return im


def decorative_ceramic():
    """Расписная керамика: белая глазурь с синим ромбическим орнаментом."""
    im = img()
    rect(im, 0, 0, 15, 15, CERAMIC_WHITE)
    hline(im, 0, 0, 15, (245, 238, 222, 255))
    hline(im, 15, 0, 15, (194, 181, 160, 255))
    for cx, cy in ((3, 3), (11, 3), (7, 7), (3, 11), (11, 11)):
        for dx, dy in ((0, -2), (-1, -1), (1, -1), (-2, 0), (0, 0), (2, 0),
                       (-1, 1), (1, 1), (0, 2)):
            px(im, cx + dx, cy + dy, CERAMIC_BLUE)
        px(im, cx, cy, CERAMIC_BLUE_DARK)
    return im


def carved_window_casing():
    """Резной наличник: белая основа с кубанским сине-голубым орнаментом."""
    im = whitewashed_plaster()
    # Тёмная внутренняя кромка усиливает рельеф модели.
    for p in range(3, 13):
        px(im, p, 3, PLASTER_DARK)
        px(im, p, 12, PLASTER_DARK)
        px(im, 3, p, PLASTER_DARK)
        px(im, 12, p, PLASTER_DARK)
    # Симметричный геометрический резной мотив по углам и верхней планке.
    for x, y in ((1, 1), (14, 1), (1, 14), (14, 14),
                 (5, 1), (8, 1), (3, 2), (12, 2)):
        px(im, x, y, CASING_ACCENT_DARK)
    for x, y in ((2, 1), (13, 1), (1, 2), (14, 2),
                 (2, 14), (13, 14), (6, 1), (9, 1)):
        px(im, x, y, CASING_ACCENT)
    return im


def wattle():
    """Плетень: вертикальные колья и горизонтальная лозовая оплётка."""
    im = img()
    # Колья
    for x in (1, 6, 11):
        vline(im, x, 0, 15, PAL["wood_dark"])
        vline(im, x + 1, 0, 15, PAL["wood_darker"])
    # Лоза: ряды прутьев, огибающие колья через один
    for row in range(8):
        y = row * 2
        hline(im, y, 0, 15, PAL["wood"])
        hline(im, y + 1, 0, 15, PAL["wood_darker"])
        # Прут уходит за кол — восстанавливаем кол там, где он спереди
        for x in (1, 6, 11):
            if (row + x) % 2 == 0:
                px(im, x, y, PAL["wood_dark"])
                px(im, x + 1, y, PAL["wood_darker"])
                px(im, x, y + 1, PAL["wood_darker"])
                px(im, x + 1, y + 1, PAL["wood_darker"])
            else:
                px(im, x, y, PAL["wood_hi"])
    # Верхняя кромка светлее (свет сверху)
    hline(im, 0, 0, 15, PAL["wood_hi"])
    return im


def wattle_particle():
    """Частицы плетня: усреднённая лозовая фактура без просветов."""
    im = img()
    rect(im, 0, 0, 15, 15, PAL["wood"])
    for y in range(1, 16, 2):
        hline(im, y, 0, 15, PAL["wood_dark"])
    hline(im, 0, 0, 15, PAL["wood_hi"])
    return im


def wattle_gate():
    """Калитка плетня: та же оплётка, но с усиленной рамой."""
    im = wattle()
    hline(im, 0, 0, 15, PAL["wood_hi"])
    hline(im, 1, 0, 15, PAL["wood"])
    hline(im, 14, 0, 15, PAL["wood_dark"])
    hline(im, 15, 0, 15, PAL["wood_darker"])
    return im


def main():
    save(hand_mill_side(), "hand_mill_side")
    save(hand_mill_top(), "hand_mill_top")
    save(drying_rack_side(), "drying_rack_side")
    save(drying_rack_top(), "drying_rack_top")
    save(oil_press_side(), "oil_press_side")
    save(oil_press_front(), "oil_press_front")
    save(oil_press_top(), "oil_press_top")
    save(oil_press_bottom(), "oil_press_bottom")
    save(oil_press_screw(), "oil_press_screw")

    save(irrigation_channel_wood(), "irrigation_channel_wood")
    save(irrigation_channel_inner(), "irrigation_channel_inner")
    save(irrigation_channel_water(), "irrigation_channel_water")

<<<<<<< HEAD
=======
<<<<<<< Updated upstream
=======
>>>>>>> 6e2806cd7fd1eec95181e4e48e492da66ebf50a8
    save(stone_irrigation_channel(), "stone_irrigation_channel")
    save(stone_irrigation_channel_inner(), "stone_irrigation_channel_inner")
    save(cutting_board_top(), "cutting_board_top")
    save(cutting_board_side(), "cutting_board_side")

<<<<<<< HEAD
=======
    save(smokehouse_side(), "smokehouse_side")
    save(smokehouse_top(), "smokehouse_top")
    save(smokehouse_brick(), "smokehouse_brick")
    save(smokehouse_firebox(lit=False), "smokehouse_firebox")
    save(smokehouse_firebox(lit=True), "smokehouse_firebox_lit")

    # Виноградный чан: бок, дно и пять состояний зеркала сока (0..4).
    save(grape_press_side(), "grape_press_side")
    save(grape_press_bottom(), "grape_press_bottom")
    for lvl in range(5):
        save(grape_press_top(lvl), f"grape_press_top{lvl}")

>>>>>>> Stashed changes
>>>>>>> 6e2806cd7fd1eec95181e4e48e492da66ebf50a8
    save(water_intake_side(), "water_intake_side")
    save(water_intake_top(), "water_intake_top")
    save(water_intake_top_active(), "water_intake_top_active")
    save(water_intake_side_base(), "water_intake")

    save(grape_trellis(), "grape_trellis")

    save(adobe_bricks(), "adobe_bricks")
    save(shell_rock(), "shell_rock")
    save(whitewashed_plaster(), "whitewashed_plaster")
    save(roof_tiles(), "roof_tiles")
    save(decorative_ceramic(), "decorative_ceramic")
    save(carved_window_casing(), "carved_window_casing")
    save(wattle(), "wattle")
    save(wattle_particle(), "wattle_particle")
    save(wattle_gate(), "wattle_gate")
    save(wattle_particle(), "wattle_gate_particle")

    for kind in LEAF_SETS:
        for stage in range(3):
            save(fruit_leaves(kind, stage), f"{kind}_leaves_stage{stage}")
        save(fruit_sapling(kind), f"{kind}_sapling")


if __name__ == "__main__":
    main()
