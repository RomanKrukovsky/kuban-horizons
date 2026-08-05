"""Библиотека примитивов пиксель-арта Kuban Horizons.

Правила ART_BIBLE.md: свет сверху-слева, ограниченная палитра,
обводка тёмным тоном собственного цвета, без случайного шума.
Все функции детерминированы (random с фиксированным seed вызывающего).
"""
from PIL import Image

# Палитра проекта (ART_BIBLE.md §2)
PAL = {
    "chernozem_dark": (43, 32, 22, 255),
    "chernozem": (61, 46, 30, 255),
    "steppe_grass": (138, 167, 74, 255),
    "dry_grass": (183, 169, 92, 255),
    "sun_petal": (232, 184, 32, 255),
    "sun_petal_hi": (245, 208, 84, 255),
    "sun_core": (92, 67, 34, 255),
    "sun_core_dark": (66, 47, 24, 255),
    "white_wall": (232, 224, 208, 255),
    "adobe": (176, 148, 104, 255),
    "tile": (165, 80, 46, 255),
    "shell_rock": (216, 203, 168, 255),
    "sea_black": (30, 110, 140, 255),
    "liman": (94, 138, 106, 255),
    "grape_dark": (74, 42, 82, 255),
    "grape_hi": (108, 66, 118, 255),
    "tea_leaf": (62, 107, 52, 255),
    "tea_leaf_hi": (88, 138, 72, 255),
    # Производные
    "leaf": (74, 122, 50, 255),
    "leaf_hi": (104, 156, 70, 255),
    "leaf_dark": (52, 88, 38, 255),
    "stem": (86, 128, 56, 255),
    "stem_dark": (60, 92, 42, 255),
    "wood": (146, 112, 70, 255),
    "wood_hi": (172, 136, 88, 255),
    "wood_dark": (108, 82, 52, 255),
    "wood_darker": (84, 62, 40, 255),
    "iron": (140, 140, 148, 255),
    "iron_dark": (100, 100, 108, 255),
    "water": (52, 118, 188, 255),
    "water_hi": (88, 150, 210, 255),
    "stone": (128, 128, 128, 255),
    "stone_hi": (152, 152, 152, 255),
    "stone_dark": (100, 100, 100, 255),
    "oil": (214, 168, 44, 255),
    "oil_hi": (236, 198, 88, 255),
    "glass": (200, 220, 228, 160),
    "glass_hi": (235, 245, 248, 190),
    "paper": (206, 178, 132, 255),
    "paper_dark": (170, 142, 100, 255),
    "seed_black": (48, 42, 38, 255),
    "seed_white": (214, 208, 196, 255),
    "tomato_red": (198, 62, 48, 255),
    "tomato_hi": (226, 104, 84, 255),
    "peach": (240, 158, 96, 255),
    "peach_hi": (248, 190, 128, 255),
    "apricot": (238, 172, 66, 255),
    "apricot_hi": (247, 202, 112, 255),
    "plum": (110, 62, 128, 255),
    "plum_hi": (142, 92, 158, 255),
    "walnut": (150, 112, 62, 255),
    "walnut_hi": (178, 140, 84, 255),
    "rice_green": (130, 158, 82, 255),
    "rice_pale": (198, 196, 136, 255),
    "corn_yellow": (230, 190, 70, 255),
    "corn_hi": (243, 214, 110, 255),
    "blossom": (246, 232, 238, 255),
    "blossom_hi": (255, 248, 252, 255),
}


def img(size=16):
    return Image.new("RGBA", (size, size), (0, 0, 0, 0))


def px(im, x, y, c):
    if 0 <= x < im.width and 0 <= y < im.height:
        im.putpixel((x, y), c)


def rect(im, x0, y0, x1, y1, c):
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            px(im, x, y, c)


def vline(im, x, y0, y1, c):
    rect(im, x, y0, x, y1, c)


def hline(im, y, x0, x1, c):
    rect(im, x0, y, x1, y, c)


def stem(im, x, y_top, y_bottom, c_main, c_dark, bend=0):
    """Стебель с тёмной правой кромкой (свет слева)."""
    for y in range(y_top, y_bottom + 1):
        xx = x + (bend if y < (y_top + y_bottom) // 2 else 0)
        px(im, xx, y, c_main)
        px(im, xx + 1, y, c_dark)


def leaf(im, x, y, direction, c_main, c_hi, c_dark, size=3):
    """Лист: направление -1 влево, +1 вправо."""
    for i in range(size):
        px(im, x + direction * (i + 1), y - (i > 1), c_main if i else c_hi)
    px(im, x + direction * size, y, c_dark)


def blob(im, cx, cy, r, c_main, c_hi, c_dark):
    """Круглый плод/ягода с бликом сверху-слева и тенью снизу-справа."""
    for y in range(cy - r, cy + r + 1):
        for x in range(cx - r, cx + r + 1):
            dx, dy = x - cx, y - cy
            if dx * dx + dy * dy <= r * r + (r > 1):
                px(im, x, y, c_main)
    px(im, cx - max(1, r - 1), cy - max(1, r - 1), c_hi)
    # Тень по нижне-правому краю
    for y in range(cy, cy + r + 1):
        for x in range(cx, cx + r + 1):
            dx, dy = x - cx, y - cy
            d2 = dx * dx + dy * dy
            if r * r - r < d2 <= r * r + (r > 1):
                px(im, x, y, c_dark)


def outline_soft(im, c_dark):
    """Мягкая обводка: тёмный тон вокруг непрозрачных пикселей не ставим
    автоматически (ручной контроль в спрайтах); функция для плоских иконок."""
    w, h = im.size
    src = im.copy()
    for y in range(h):
        for x in range(w):
            if src.getpixel((x, y))[3] == 0:
                # сосед непрозрачен?
                for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                    nx, ny = x + dx, y + dy
                    if 0 <= nx < w and 0 <= ny < h and src.getpixel((nx, ny))[3] > 200:
                        px(im, x, y, c_dark)
                        break


def checker_noise(im, x0, y0, x1, y1, c_a, c_b, period=2):
    """Осмысленное «зерно»: шахматное чередование двух тонов."""
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            px(im, x, y, c_a if (x + y) % period == 0 else c_b)


def plank_texture(base, hi, dark, darker, size=16):
    """Доска: горизонтальные ряды с фаской."""
    im = img(size)
    row_h = 4
    for row in range(size // row_h):
        y0 = row * row_h
        rect(im, 0, y0, size - 1, y0 + row_h - 1, base)
        hline(im, y0, 0, size - 1, hi)
        hline(im, y0 + row_h - 1, 0, size - 1, darker)
        # редкие «сучки»-штрихи
        px(im, (row * 5 + 3) % size, y0 + 2, dark)
        px(im, (row * 9 + 8) % size, y0 + 1, dark)
    return im
