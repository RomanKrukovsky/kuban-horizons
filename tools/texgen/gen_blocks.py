"""Текстуры блоков-устройств: маслопресс, желоб, водозабор, листва, саженцы."""
import os
from texlib import (PAL, img, px, rect, vline, hline, blob, leaf, stem,
                    checker_noise, plank_texture)

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

    save(water_intake_side(), "water_intake_side")
    save(water_intake_top(), "water_intake_top")
    save(water_intake_top_active(), "water_intake_top_active")
    save(water_intake_side_base(), "water_intake")

    save(grape_trellis(), "grape_trellis")

    for kind in LEAF_SETS:
        for stage in range(3):
            save(fruit_leaves(kind, stage), f"{kind}_leaves_stage{stage}")
        save(fruit_sapling(kind), f"{kind}_sapling")


if __name__ == "__main__":
    main()
