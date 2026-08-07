"""Текстуры мелкой фауны Kuban Horizons: насекомые, водные птицы, осётр.

Развёртки совпадают с моделями InsectModel, GullModel, HeronModel,
SturgeonModel. UV-слоты посчитаны по раскладке граней куба (top/bottom/right/
front/left/back), поэтому части не перекрываются — иначе крыло получило бы
рисунок брюшка.

Палитра по ART_BIBLE §2: саранча в тонах выгоревшей травы (#b7a95c), пчела
серо-жёлтая (кавказская порода тусклее итальянской), чайка белая с тёмной
спиной, цапля пепельно-серая, осётр — тёмная спина и светлое брюхо.
"""
import os

from PIL import Image

from texlib import px, rect, hline

OUT = os.path.join(os.path.dirname(__file__), "..", "..",
                   "src/main/resources/assets/kubanhorizons/textures/entity")

# --- Саранча: сухая трава, чтобы читалась на выгоревшем поле ---
LO_BODY = (162, 148, 82, 255)
LO_BODY_HI = (192, 180, 112, 255)
LO_BODY_DARK = (120, 108, 58, 255)
LO_HEAD = (146, 132, 74, 255)
LO_WING = (176, 168, 122, 200)       # полупрозрачное крыло насекомого
LO_WING_HI = (206, 200, 158, 210)

# --- Кавказская пчела: серее и тусклее итальянской ---
BE_BODY = (196, 158, 66, 255)
BE_BODY_HI = (222, 190, 104, 255)
BE_BODY_DARK = (86, 72, 52, 255)     # тёмные полосы
BE_HEAD = (74, 64, 50, 255)
BE_WING = (206, 214, 220, 190)
BE_WING_HI = (232, 240, 246, 200)

# --- Чайка: белая с тёмно-серой спиной и жёлтым клювом ---
GU_BODY = (236, 238, 240, 255)
GU_BODY_HI = (250, 251, 252, 255)
GU_BODY_DARK = (196, 202, 208, 255)
GU_MANTLE = (112, 126, 138, 255)     # тёмная спина и верх крыла
GU_MANTLE_HI = (140, 154, 166, 255)
GU_WINGTIP = (44, 48, 52, 255)       # чёрные концы крыльев
GU_BEAK = (226, 176, 44, 255)
GU_LEG = (232, 168, 84, 255)

# --- Цапля: пепельно-серая, с чёрной косицей и жёлтым клювом ---
HE_BODY = (168, 172, 176, 255)
HE_BODY_HI = (196, 200, 204, 255)
HE_BODY_DARK = (128, 132, 138, 255)
HE_NECK = (204, 202, 194, 255)       # шея светлее корпуса
HE_CREST = (40, 42, 46, 255)         # чёрная косица на затылке
HE_BEAK = (222, 194, 78, 255)
HE_LEG = (146, 138, 108, 255)

# --- Осётр: тёмная спина, светлое брюхо, костяные жучки ---
ST_BODY = (104, 108, 96, 255)
ST_BODY_HI = (132, 138, 124, 255)
ST_BODY_DARK = (74, 78, 70, 255)
ST_BELLY = (208, 202, 180, 255)
ST_SCUTE = (168, 172, 156, 255)      # жучки светлее спины
ST_FIN = (92, 96, 88, 255)

EYE = (24, 20, 18, 255)
EYE_GLINT = (238, 238, 232, 255)


def save(im, name):
    os.makedirs(OUT, exist_ok=True)
    im.save(os.path.join(OUT, name + ".png"))
    print("entity/" + name)


def cube(im, u, v, w, h, d, base, hi, dark):
    """Шесть граней куба по ванильной box-UV раскладке. Свет сверху-слева."""
    rect(im, u + d, v, u + d + w - 1, v + d - 1, hi)                       # top
    rect(im, u + d + w, v, u + d + w + w - 1, v + d - 1, dark)             # bottom
    rect(im, u, v + d, u + d - 1, v + d + h - 1, base)                     # right
    rect(im, u + d, v + d, u + d + w - 1, v + d + h - 1, base)             # front
    rect(im, u + d + w, v + d, u + d + w + d - 1, v + d + h - 1, dark)     # left
    rect(im, u + d + w + d, v + d, u + d + w + d + w - 1, v + d + h - 1,
         base)                                                            # back


def insect(body, body_hi, body_dark, head, wing, wing_hi,
           body_len, leg_len, striped=False):
    """Развёртка насекомого: body (0,0), head (14,0), крылья (26,0)/(44,0),
    ноги (0,0)/(26,0), усик (40,0)."""
    im = Image.new("RGBA", (64, 32), (0, 0, 0, 0))

    cube(im, 0, 0, 4, 3, body_len, body, body_hi, body_dark)
    if striped:
        # Поперечные полосы пчелы — по передней грани брюшка.
        for y in range(body_len, body_len + 3):
            if (y - body_len) % 2 == 0:
                hline(im, y, body_len, body_len + 3, body_dark)

    cube(im, 14, 0, 3, 3, 3, head, body_hi, body_dark)
    px(im, 18, 4, EYE)
    px(im, 20, 4, EYE)

    # Крылья: полупрозрачные, с жилкой по передней кромке.
    cube(im, 26, 0, 5, 1, 4, wing, wing_hi, wing)
    cube(im, 44, 0, 5, 1, 4, wing, wing_hi, wing)
    hline(im, 4, 30, 34, wing_hi)
    hline(im, 4, 48, 52, wing_hi)

    cube(im, 0, 0, 1, leg_len, 1, body_dark, body, body_dark)
    cube(im, 26, 0, 1, leg_len, 1, body_dark, body, body_dark)
    cube(im, 40, 0, 1, 2, 1, body_dark, body, body_dark)
    return im


def locust():
    return insect(LO_BODY, LO_BODY_HI, LO_BODY_DARK, LO_HEAD, LO_WING,
                  LO_WING_HI, 6, 3)


def caucasian_bee():
    return insect(BE_BODY, BE_BODY_HI, BE_BODY_DARK, BE_HEAD, BE_WING,
                  BE_WING_HI, 4, 2, striped=True)


def gull():
    """Развёртка чайки: body (0,0), head (15,0), beak (27,0),
    крылья (30,0)/(17,6), tail (47,0), лапы (0,0)/(24,0)."""
    im = Image.new("RGBA", (64, 32), (0, 0, 0, 0))

    cube(im, 0, 0, 4, 4, 7, GU_BODY, GU_BODY_HI, GU_BODY_DARK)
    # Тёмная мантия по верхней грани корпуса.
    rect(im, 7, 0, 10, 6, GU_MANTLE)

    cube(im, 15, 0, 3, 3, 3, GU_BODY, GU_BODY_HI, GU_BODY_DARK)
    px(im, 19, 4, EYE)
    px(im, 20, 3, EYE_GLINT)
    px(im, 22, 4, EYE)

    cube(im, 27, 0, 1, 1, 3, GU_BEAK, (240, 198, 76, 255), (188, 140, 28, 255))

    # Крылья: верх тёмный (мантия), концы чёрные — примета чайки.
    for u, v in ((30, 0), (17, 6)):
        cube(im, u, v, 6, 1, 5, GU_BODY, GU_MANTLE_HI, GU_BODY_DARK)
        rect(im, u + 5, v, u + 10, v + 4, GU_MANTLE)
        rect(im, u + 5, v, u + 6, v + 4, GU_WINGTIP)

    cube(im, 47, 0, 3, 1, 4, GU_BODY, GU_BODY_HI, GU_BODY_DARK)
    cube(im, 0, 0, 1, 2, 1, GU_LEG, (244, 190, 112, 255), (196, 132, 56, 255))
    cube(im, 24, 0, 1, 2, 1, GU_LEG, (244, 190, 112, 255), (196, 132, 56, 255))
    return im


def heron():
    """Развёртка цапли: body (0,0), neck (24,0), head (32,0), beak (39,0),
    крылья (25,6)/(0,13), ноги (51,0)/(55,0)."""
    im = Image.new("RGBA", (64, 32), (0, 0, 0, 0))

    cube(im, 0, 0, 4, 5, 8, HE_BODY, HE_BODY_HI, HE_BODY_DARK)
    cube(im, 24, 0, 2, 7, 2, HE_NECK, (222, 220, 214, 255), HE_BODY_DARK)

    cube(im, 32, 0, 3, 2, 3, HE_BODY_HI, (210, 214, 218, 255), HE_BODY_DARK)
    # Чёрная косица на затылке — подпись серой цапли.
    hline(im, 3, 35, 37, HE_CREST)
    px(im, 36, 4, EYE)
    px(im, 38, 4, EYE)

    cube(im, 39, 0, 1, 1, 5, HE_BEAK, (238, 214, 108, 255), (186, 158, 48, 255))

    for u, v in ((25, 6), (0, 13)):
        cube(im, u, v, 5, 1, 7, HE_BODY, HE_BODY_HI, HE_BODY_DARK)
        # Тёмные маховые по задней кромке.
        rect(im, u + 7, v + 7, u + 11, v + 7, HE_BODY_DARK)

    cube(im, 51, 0, 1, 8, 1, HE_LEG, (170, 160, 128, 255), (116, 108, 84, 255))
    cube(im, 55, 0, 1, 8, 1, HE_LEG, (170, 160, 128, 255), (116, 108, 84, 255))
    return im


def sturgeon():
    """Развёртка осетра: body (0,0), tail_body (18,0), rostrum (30,0),
    tail_fin (0,0), плавники (38,0)/(48,0), dorsal (37,3)."""
    im = Image.new("RGBA", (64, 32), (0, 0, 0, 0))

    cube(im, 0, 0, 4, 4, 10, ST_BODY, ST_BODY_HI, ST_BODY_DARK)
    # Светлое брюхо по нижней грани.
    rect(im, 14, 0, 17, 9, ST_BELLY)
    # Ряд костяных жучков по боку — вторая примета после рострума.
    for z in range(11, 20, 2):
        px(im, 11, z, ST_SCUTE)
        px(im, 12, z, ST_SCUTE)

    cube(im, 18, 0, 3, 3, 6, ST_BODY, ST_BODY_HI, ST_BODY_DARK)
    cube(im, 30, 0, 2, 1, 4, ST_BODY_DARK, ST_BODY, ST_BODY_DARK)
    cube(im, 0, 0, 1, 5, 4, ST_FIN, ST_BODY_HI, ST_BODY_DARK)
    cube(im, 38, 0, 3, 1, 2, ST_FIN, ST_BODY_HI, ST_BODY_DARK)
    cube(im, 48, 0, 3, 1, 2, ST_FIN, ST_BODY_HI, ST_BODY_DARK)
    cube(im, 37, 3, 1, 3, 5, ST_SCUTE, (188, 192, 176, 255), ST_BODY_DARK)
    return im


def main():
    save(locust(), "locust")
    save(caucasian_bee(), "caucasian_bee")
    save(gull(), "gull")
    save(heron(), "heron")
    save(sturgeon(), "sturgeon")


if __name__ == "__main__":
    main()
