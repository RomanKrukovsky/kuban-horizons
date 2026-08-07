"""Текстуры четвероногих Kuban Horizons: кабан и кавказская овчарка (64×64).

Развёртка совпадает с KubanQuadrupedModel, которая строится на ванильной
QuadrupedModel.createBodyMesh:

    head  (0,0)   8x8x8      body (28,8)  10x16x8
    leg   (0,16)  4xLEGHx4   ...плюс свои части ниже y=32:

    кабан:   withers (0,32) 8x4x7   snout (23,32) 4x3x3   tail (37,32) 2x6x2
    овчарка: right_ear (0,32) 3x3x1  left_ear (8,32) 3x3x1
             muzzle (16,32) 4x3x3    tail (30,32) 3x7x3

Слоты ниже y=32 выбраны расчётом по раскладке граней: ванильная зона занята
целиком, и пересечение с ней дало бы шерсть корпуса на морде.

Палитра по ART_BIBLE §2: кабан — чернозёмные буро-серые тона степного зверя,
овчарка — палево-серая с тёмной спиной. Свет сверху-слева, не более 8-10
оттенков на текстуру.
"""
import os

from PIL import Image

from texlib import px, rect, hline

OUT = os.path.join(os.path.dirname(__file__), "..", "..",
                   "src/main/resources/assets/kubanhorizons/textures/entity")

# --- Кабан: щетинистый, тёмный, с седым загривком ---
BO_BODY = (74, 60, 48, 255)
BO_BODY_HI = (98, 80, 64, 255)
BO_BODY_DARK = (52, 42, 34, 255)
BO_HEAD = (66, 54, 44, 255)
BO_HEAD_HI = (88, 72, 58, 255)
BO_WITHERS = (92, 78, 62, 255)      # седина на горбе — примета взрослого кабана
BO_WITHERS_HI = (118, 102, 84, 255)
BO_SNOUT = (58, 46, 40, 255)
BO_TUSK = (232, 226, 206, 255)
BO_HOOF = (38, 32, 28, 255)

# --- Кавказская овчарка: палевая, с тёмным «седлом» ---
SH_BODY = (188, 170, 138, 255)
SH_BODY_HI = (212, 198, 170, 255)
SH_BODY_DARK = (146, 130, 104, 255)
SH_SADDLE = (98, 86, 72, 255)       # тёмная спина
SH_HEAD = (196, 180, 150, 255)
SH_HEAD_HI = (218, 206, 180, 255)
SH_MUZZLE = (74, 66, 58, 255)       # тёмная маска на морде
SH_EAR = (120, 106, 88, 255)
SH_PAW = (206, 194, 168, 255)

EYE = (24, 20, 18, 255)
EYE_GLINT = (238, 238, 232, 255)

# --- Нутрия: буро-рыжий болотный грызун ---
NU_FUR = (118, 88, 58, 255)
NU_FUR_HI = (146, 112, 76, 255)
NU_FUR_DARK = (86, 64, 42, 255)
NU_BELLY = (150, 124, 92, 255)
NU_TAIL = (92, 76, 66, 255)          # хвост голый, чешуйчатый — не мех
NU_TAIL_HI = (114, 96, 84, 255)
NU_INCISOR = (226, 146, 46, 255)     # оранжевые резцы — примета вида
NU_EAR = (74, 56, 44, 255)


def save(im, name):
    os.makedirs(OUT, exist_ok=True)
    im.save(os.path.join(OUT, name + ".png"))
    print("entity/" + name)


def bristle(im, x0, y0, x1, y1, colour, period=3, offset=0):
    """Щетина/подшёрсток: короткие штрихи по решётке. Детерминированно."""
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            if (x * 2 + y + offset) % period == 0:
                px(im, x, y, colour)


def cube(im, u, v, w, h, d, base, hi, dark):
    """Заливает шесть граней куба по ванильной раскладке box-UV.

    Раскладка: top (u+d, v, w, d), bottom (u+d+w, v, w, d),
    right (u, v+d, d, h), front (u+d, v+d, w, h),
    left (u+d+w, v+d, d, h), back (u+d+w+d, v+d, w, h).
    Свет сверху-слева: верх светлее, низ и правая грань темнее.
    """
    rect(im, u + d, v, u + d + w - 1, v + d - 1, hi)                       # top
    rect(im, u + d + w, v, u + d + w + w - 1, v + d - 1, dark)             # bottom
    rect(im, u, v + d, u + d - 1, v + d + h - 1, base)                     # right
    rect(im, u + d, v + d, u + d + w - 1, v + d + h - 1, base)             # front
    rect(im, u + d + w, v + d, u + d + w + d - 1, v + d + h - 1, dark)     # left
    rect(im, u + d + w + d, v + d, u + d + w + d + w - 1, v + d + h - 1,
         base)                                                            # back


def quadruped_texture(body, body_hi, body_dark, head, head_hi,
                      leg_hi, leg_h, saddle=None, bristled=False):
    """Общая часть: голова, корпус, лапа."""
    im = Image.new("RGBA", (64, 64), (0, 0, 0, 0))

    # --- Голова: 8x8x8 в (0,0) ---
    cube(im, 0, 0, 8, 8, 8, head, head_hi, body_dark)
    # Глаза на передней грани (front начинается в (8,8)).
    px(im, 10, 11, EYE)
    px(im, 11, 10, EYE_GLINT)
    px(im, 13, 11, EYE)
    px(im, 14, 10, EYE_GLINT)

    # --- Корпус: 10x16x8 в (28,8) ---
    cube(im, 28, 8, 10, 16, 8, body, body_hi, body_dark)
    if saddle is not None:
        # Тёмное «седло» по верхней грани — рисунок окраса овчарки.
        rect(im, 36, 8, 45, 15, saddle)
    if bristled:
        bristle(im, 36, 16, 45, 31, body_dark, period=4)
        bristle(im, 36, 16, 45, 31, body_hi, period=7, offset=3)

    # --- Лапа: 4x{leg_h}x4 в (0,16) ---
    cube(im, 0, 16, 4, leg_h, 4, body, leg_hi, body_dark)
    return im


def wild_boar():
    im = quadruped_texture(BO_BODY, BO_BODY_HI, BO_BODY_DARK, BO_HEAD,
                           BO_HEAD_HI, BO_BODY_HI, 7, bristled=True)
    # Загривок: 8x4x7 в (0,32) — седой горб.
    cube(im, 0, 32, 8, 4, 7, BO_WITHERS, BO_WITHERS_HI, BO_BODY_DARK)
    # Жёсткая грива по хребту: тёмная полоса на верхней грани загривка.
    hline(im, 33, 8, 15, BO_BODY_DARK)
    # Рыло: 4x3x3 в (23,32).
    cube(im, 23, 32, 4, 3, 3, BO_SNOUT, BO_BODY, BO_BODY_DARK)
    # Клыки — две светлые точки по краям передней грани рыла.
    px(im, 26, 36, BO_TUSK)
    px(im, 29, 36, BO_TUSK)
    # Хвост: 2x6x2 в (37,32).
    cube(im, 37, 32, 2, 6, 2, BO_BODY_DARK, BO_BODY, BO_HOOF)
    return im


def caucasian_shepherd():
    im = quadruped_texture(SH_BODY, SH_BODY_HI, SH_BODY_DARK, SH_HEAD,
                           SH_HEAD_HI, SH_PAW, 8, saddle=SH_SADDLE)
    # Уши: 3x3x1 в (0,32) и (8,32) — тёмные, короткие.
    cube(im, 0, 32, 3, 3, 1, SH_EAR, SH_BODY_DARK, SH_SADDLE)
    cube(im, 8, 32, 3, 3, 1, SH_EAR, SH_BODY_DARK, SH_SADDLE)
    # Морда: 4x3x3 в (16,32) — тёмная маска.
    cube(im, 16, 32, 4, 3, 3, SH_MUZZLE, SH_BODY_DARK, SH_SADDLE)
    # Нос: тёмная точка по центру передней грани.
    px(im, 21, 36, (32, 28, 26, 255))
    px(im, 22, 36, (32, 28, 26, 255))
    # Хвост: 3x7x3 в (30,32) — пушистый, светлее спины.
    cube(im, 30, 32, 3, 7, 3, SH_BODY, SH_BODY_HI, SH_BODY_DARK)
    return im


def nutria():
    """Нутрия 64×32. Развёртка совпадает с NutriaModel.

        body (0,0) 6x8x7      head (26,0) 4x4x4    snout (19,0) 3x2x2
        r_ear (0,0) 2x2x1     l_ear (38,0) 2x2x1   incisors (58,0) 2x1x1
        tail (44,0) 2x9x2     задние лапы (52,0) и (52,5), передние (26,8) и (34,8)

    Ухо в (0,0) не конфликтует с корпусом: развёртка куба 6x8x7 занимает верхнюю
    полосу правее x=7, а ухо 2x2x1 укладывается в свободный левый угол.
    """
    im = Image.new("RGBA", (64, 32), (0, 0, 0, 0))

    # Корпус: горбатая спина светлее, брюхо ощутимо светлее боков.
    cube(im, 0, 0, 6, 8, 7, NU_FUR, NU_FUR_HI, NU_FUR_DARK)
    rect(im, 13, 0, 18, 6, NU_BELLY)          # нижняя грань = брюхо
    bristle(im, 7, 7, 12, 14, NU_FUR_DARK, period=4)

    # Голова.
    cube(im, 26, 0, 4, 4, 4, NU_FUR, NU_FUR_HI, NU_FUR_DARK)
    px(im, 31, 5, EYE)
    px(im, 32, 4, EYE_GLINT)
    px(im, 34, 5, EYE)
    px(im, 35, 4, EYE_GLINT)

    cube(im, 19, 0, 3, 2, 2, NU_FUR_DARK, NU_FUR, NU_TAIL)
    cube(im, 58, 0, 2, 1, 1, NU_INCISOR, (240, 178, 82, 255), (188, 116, 32, 255))
    cube(im, 0, 0, 2, 2, 1, NU_EAR, NU_FUR_DARK, (56, 42, 34, 255))
    cube(im, 38, 0, 2, 2, 1, NU_EAR, NU_FUR_DARK, (56, 42, 34, 255))

    # Хвост: голый, с поперечными чешуйками — читается даже мелким.
    cube(im, 44, 0, 2, 9, 2, NU_TAIL, NU_TAIL_HI, (72, 60, 52, 255))
    for y in range(2, 11, 2):
        hline(im, y, 46, 47, (72, 60, 52, 255))

    for u, v in ((52, 0), (52, 5), (26, 8), (34, 8)):
        cube(im, u, v, 2, 3, 2, NU_FUR, NU_FUR_HI, NU_FUR_DARK)
    return im


def main():
    save(wild_boar(), "wild_boar")
    save(caucasian_shepherd(), "caucasian_shepherd")
    save(nutria(), "nutria")


if __name__ == "__main__":
    main()
