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
# Диапазон намеренно широкий: прежняя палитра держалась в яркости 44..104, и
# зверь читался плоской бурой заливкой. Ванильная корова для сравнения даёт
# разброс основных тонов ~134. Тёмный хребет против светлого бока — это то, что
# лепит объём на дистанции (ART_BIBLE §3: контраст силуэта важнее детализации).
BO_SPINE = (34, 28, 22, 255)         # хребет — самый тёмный тон
BO_BODY_DARK = (58, 48, 38, 255)
BO_BODY = (88, 72, 56, 255)
BO_BODY_HI = (122, 100, 76, 255)
BO_FLANK = (152, 128, 96, 255)       # низ бока выгорел на солнце
BO_HEAD = (74, 60, 48, 255)
BO_WITHERS = (110, 92, 70, 255)      # седина на горбе — примета взрослого кабана
# Самый светлый тон шкуры: и седина на горбе, и верх брюха. Один оттенок на две
# роли намеренно — в бюджете ART_BIBLE §2 нет места двум почти равным светлым.
BO_WITHERS_HI = (180, 158, 124, 255)
BO_SNOUT = (104, 78, 70, 255)        # рыло мясистое, теплее шкуры
BO_TUSK = (238, 232, 214, 255)
BO_HOOF = (28, 24, 20, 255)

# --- Кавказская овчарка: палевая, с тёмным «седлом» ---
# Разброс основных тонов был 118 — уже неплохо, но окрас читался тремя плоскими
# плашками. Добавлены переходный тон подшёрстка и светлая грудь.
SH_BODY = (188, 170, 138, 255)
SH_BODY_HI = (216, 202, 174, 255)
SH_BODY_DARK = (146, 130, 104, 255)
SH_UNDERCOAT = (112, 100, 82, 255)  # переход от седла к боку, иначе край резкий
SH_SADDLE = (84, 74, 62, 255)       # тёмная спина
SH_HEAD = (196, 180, 150, 255)
SH_HEAD_HI = (222, 210, 186, 255)
SH_MUZZLE = (68, 60, 54, 255)       # тёмная маска на морде
SH_EAR = (116, 102, 84, 255)
SH_PAW = (208, 196, 170, 255)

EYE = (24, 20, 18, 255)
EYE_GLINT = (238, 238, 232, 255)

# --- Нутрия: буро-рыжий болотный грызун ---
# Разброс основных тонов был 65 — самый узкий в моде, зверь читался бурым
# пятном. Спина затемнена, брюхо и щёки высветлены: у нутрии брюхо реально
# намного светлее спины, так что контраст здесь ещё и биологически верен.
NU_SPINE = (58, 42, 28, 255)         # мокрая тёмная спина
NU_FUR_DARK = (86, 64, 42, 255)
NU_FUR = (118, 88, 58, 255)
NU_FUR_HI = (152, 118, 80, 255)
NU_BELLY = (186, 158, 118, 255)      # брюхо заметно светлее — примета вида
NU_TAIL = (92, 76, 66, 255)          # хвост голый, чешуйчатый — не мех
NU_TAIL_HI = (124, 106, 92, 255)
NU_TAIL_DARK = (62, 52, 44, 255)
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


def faces(u, v, w, h, d):
    """Прямоугольники шести граней куба в box-UV: (x0, y0, x1, y1).

    Возвращает то же, что заливает cube(), но адресуемо по имени. Нужен, чтобы
    рисовать рисунок окраса строго внутри одной грани: пересечение границы даёт
    шерсть корпуса на морде, а такие промахи в редакторе не видны.

    ВАЖНО про корпус четвероногого. QuadrupedModel поворачивает корпус на pi/2
    вокруг X, поэтому имена граней box-UV не совпадают с анатомией:

        UV back   -> спина (хребет)      UV front  -> брюхо
        UV top    -> зад (крестец)       UV bottom -> грудь
        UV left/right -> бока

    Проверено по ванильным текстурам: вымя коровы (247,175,175) лежит именно в
    UV front, а спина (UV back) там СВЕТЛЕЕ брюха (110 против 85 по яркости) —
    в ваниле верхний свет запечён в саму текстуру. Для головы и остальных кубов
    поворота нет, и имена граней читаются буквально.
    """
    return {
        "top": (u + d, v, u + d + w - 1, v + d - 1),
        "bottom": (u + d + w, v, u + d + w + w - 1, v + d - 1),
        "right": (u, v + d, u + d - 1, v + d + h - 1),
        "front": (u + d, v + d, u + d + w - 1, v + d + h - 1),
        "left": (u + d + w, v + d, u + d + w + d - 1, v + d + h - 1),
        "back": (u + d + w + d, v + d, u + d + w + d + w - 1, v + d + h - 1),
    }


def fill(im, box, colour):
    rect(im, box[0], box[1], box[2], box[3], colour)


def band(im, box, colour, rows):
    """Полоса поперёк грани: rows — доли высоты грани (from, to) в пикселях
    от её верхнего края. Обрезается границей грани, за неё не выходит."""
    x0, y0, x1, y1 = box
    for i in rows:
        y = y0 + i
        if y0 <= y <= y1:
            hline(im, y, x0, x1, colour)


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
                      leg_hi, leg_h, saddle=None, bristled=False,
                      spine=None, flank=None, flank_hi=None,
                      bristle_dark=None, undercoat=None):
    """Общая часть: голова, корпус, лапа.

    spine и flank задают вертикальный градиент шкуры: хребет тёмный, низ бока
    светлый. Без него корпус — одна заливка: у кабана разброс основных тонов
    был 60 при ~134 у ванильной коровы, и зверь читался силуэтом без объёма.
    """
    im = Image.new("RGBA", (64, 64), (0, 0, 0, 0))

    # --- Голова: 8x8x8 в (0,0) ---
    # Голова не повёрнута: top — это действительно верх черепа.
    cube(im, 0, 0, 8, 8, 8, head, head_hi, body_dark)
    hf = faces(0, 0, 8, 8, 8)
    if spine is not None:
        # Тёмная шапка по верху головы: замыкает линию хребта.
        band(im, hf["top"], spine, (0, 1, 2))
        # Лоб темнее морды — глаза читаются на тёмном.
        band(im, hf["front"], body_dark, (0, 1))
    # Глаза на передней грани (front начинается в (8,8)).
    px(im, 10, 11, EYE)
    px(im, 11, 10, EYE_GLINT)
    px(im, 13, 11, EYE)
    px(im, 14, 10, EYE_GLINT)

    # --- Корпус: 10x16x8 в (28,8) ---
    # Корпус повёрнут на pi/2: спина — это грань back, брюхо — front (см. faces).
    cube(im, 28, 8, 10, 16, 8, body, body_hi, body_dark)
    bf = faces(28, 8, 10, 16, 8)
    if flank is not None:
        # Бока: тёмный верх (у хребта) -> светлый низ (у брюха). Свет
        # сверху-слева, поэтому левая грань на ступень темнее правой.
        for box, top_c, mid_c, low_c in (
                (bf["right"], body_dark, body_hi, flank),
                (bf["left"], spine or body_dark, body, body_hi)):
            fill(im, box, mid_c)
            band(im, box, top_c, (0, 1, 2))
            band(im, box, low_c, (12, 13, 14, 15))
        # Брюхо (front): самое светлое место — выгоревшая на солнце шкура.
        fill(im, bf["front"], flank)
        band(im, bf["front"], flank_hi or flank, (0, 1, 2))
        # Грудь (bottom) и крестец (top) — промежуточный тон.
        fill(im, bf["bottom"], body_hi)
        fill(im, bf["top"], body)
    if spine is not None:
        # Спина (back): тёмный хребет по центру, к бокам светлеет. В ваниле в
        # спину запечён верхний свет, поэтому кромки светлые, а не тёмные.
        fill(im, bf["back"], body)
        x0, y0, x1, y1 = bf["back"]
        rect(im, x0 + 3, y0, x1 - 3, y1, spine)          # хребет
        rect(im, x0, y0, x0 + 1, y1, body_hi)            # освещённая кромка
        rect(im, x1 - 1, y0, x1, y1, body_dark)          # теневая кромка
    if saddle is not None:
        # Тёмное «седло» по спине (грань back) — рисунок окраса овчарки.
        # Края растушёваны подшёрстком: резкая плашка читается как наклейка.
        fill(im, bf["back"], saddle)
        x0, y0, x1, y1 = bf["back"]
        rect(im, x0, y0, x0 + 1, y1, undercoat or body_dark)
        rect(im, x1 - 1, y0, x1, y1, undercoat or body_dark)
        band(im, bf["back"], undercoat or body_dark, (0, y1 - y0))
        # Бока: подшёрсток у хребта, палевый низ, светлая грудь.
        for box in (bf["right"], bf["left"]):
            band(im, box, undercoat or body_dark, (0, 1))
        fill(im, bf["front"], body_hi)          # брюхо и грудь светлые
        band(im, bf["front"], body, (0, 1, 2))
    if bristled:
        # Щетина по спине и верху боков: там, где она реально топорщится.
        bristle(im, bf["back"][0] + 2, bf["back"][1], bf["back"][2] - 2,
                bf["back"][3], bristle_dark or body_dark, period=3)
        bristle(im, bf["right"][0], bf["right"][1], bf["right"][2],
                bf["right"][1] + 6, spine or body_dark, period=4)
        bristle(im, bf["left"][0], bf["left"][1], bf["left"][2],
                bf["left"][1] + 6, spine or body_dark, period=4, offset=2)

    # --- Лапа: 4x{leg_h}x4 в (0,16) ---
    cube(im, 0, 16, 4, leg_h, 4, body, leg_hi, body_dark)
    lf = faces(0, 16, 4, leg_h, 4)
    # Копыто/лапа: тёмный низ отделяет ногу от земли.
    for side in ("right", "front", "left", "back"):
        band(im, lf[side], body_dark, (leg_h - 2, leg_h - 1))
    return im


def wild_boar():
    im = quadruped_texture(BO_BODY, BO_BODY_HI, BO_BODY_DARK, BO_HEAD,
                           BO_BODY_HI, BO_BODY_HI, 7, bristled=True,
                           spine=BO_SPINE, flank=BO_FLANK,
                           flank_hi=BO_WITHERS_HI, bristle_dark=BO_HOOF)
    # Загривок: 8x4x7 в (0,32) — седой горб, самое светлое место шкуры.
    cube(im, 0, 32, 8, 4, 7, BO_WITHERS, BO_WITHERS_HI, BO_BODY_DARK)
    wf = faces(0, 32, 8, 4, 7)
    # Жёсткая грива по хребту: тёмная полоса продолжает линию спины корпуса.
    band(im, wf["top"], BO_SPINE, (0, 1))
    bristle(im, wf["top"][0], wf["top"][1] + 2, wf["top"][2], wf["top"][3],
            BO_SPINE, period=3)
    # Седина на боках горба — короткие светлые штрихи по тёмному.
    bristle(im, wf["front"][0], wf["front"][1], wf["front"][2], wf["front"][3],
            BO_WITHERS_HI, period=3, offset=1)

    # Рыло: 4x3x3 в (23,32).
    cube(im, 23, 32, 4, 3, 3, BO_SNOUT, BO_BODY, BO_BODY_DARK)
    sf = faces(23, 32, 4, 3, 3)
    # Пятак: светлый диск на передней грани с тёмными ноздрями.
    fill(im, sf["front"], BO_SNOUT)
    band(im, sf["front"], BO_BODY_DARK, (2,))
    px(im, sf["front"][0] + 1, sf["front"][1] + 1, BO_SPINE)
    px(im, sf["front"][0] + 2, sf["front"][1] + 1, BO_SPINE)
    # Клыки: по два пикселя вверх от углов рыла — снизу вверх, как у кабана.
    for dx in (0, 3):
        px(im, sf["front"][0] + dx, sf["front"][1] + 2, BO_TUSK)
        px(im, sf["front"][0] + dx, sf["front"][1] + 1, BO_TUSK)

    # Хвост: 2x6x2 в (37,32).
    cube(im, 37, 32, 2, 6, 2, BO_BODY_DARK, BO_BODY, BO_HOOF)
    tf = faces(37, 32, 2, 6, 2)
    # Кисточка на конце — тёмная, чтобы хвост не выглядел обрубком.
    band(im, tf["front"], BO_SPINE, (4, 5))
    band(im, tf["back"], BO_SPINE, (4, 5))
    return im


def caucasian_shepherd():
    im = quadruped_texture(SH_BODY, SH_BODY_HI, SH_BODY_DARK, SH_HEAD,
                           SH_HEAD_HI, SH_PAW, 8, saddle=SH_SADDLE,
                           undercoat=SH_UNDERCOAT)
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

    # Корпус повёрнут на pi/2, как у четвероногих: спина — грань back, брюхо —
    # front (см. faces). Раньше брюхо красили по грани bottom, то есть на самом
    # деле по груди, а спина оставалась ровной заливкой.
    cube(im, 0, 0, 6, 8, 7, NU_FUR, NU_FUR_HI, NU_FUR_DARK)
    nf = faces(0, 0, 6, 8, 7)
    # Спина: тёмный хребет по центру, светлые кромки (верхний свет как в ваниле).
    fill(im, nf["back"], NU_FUR_DARK)
    x0, y0, x1, y1 = nf["back"]
    rect(im, x0 + 2, y0, x1 - 2, y1, NU_SPINE)
    rect(im, x0, y0, x0, y1, NU_FUR)
    # Бока: тёмный верх у хребта -> светлый низ у брюха.
    for box in (nf["right"], nf["left"]):
        band(im, box, NU_FUR_DARK, (0, 1))
        band(im, box, NU_FUR_HI, (6, 7))
    # Брюхо светлое, грудь на тон темнее.
    fill(im, nf["front"], NU_BELLY)
    band(im, nf["front"], NU_FUR_HI, (0, 1))
    fill(im, nf["bottom"], NU_FUR_HI)
    # Мех: короткие штрихи по спине, чтобы шкура не была плоской.
    bristle(im, x0 + 2, y0, x1 - 2, y1, NU_FUR_DARK, period=3)

    # Голова.
    cube(im, 26, 0, 4, 4, 4, NU_FUR, NU_FUR_HI, NU_FUR_DARK)
    hf = faces(26, 0, 4, 4, 4)
    # Тёмная шапка на голове продолжает хребет, щёки светлые.
    band(im, hf["top"], NU_SPINE, (0, 1))
    band(im, hf["front"], NU_FUR_HI, (2, 3))
    px(im, 31, 5, EYE)
    px(im, 32, 4, EYE_GLINT)
    px(im, 34, 5, EYE)
    px(im, 35, 4, EYE_GLINT)

    cube(im, 19, 0, 3, 2, 2, NU_FUR_DARK, NU_FUR_HI, NU_SPINE)
    cube(im, 58, 0, 2, 1, 1, NU_INCISOR, (240, 178, 82, 255), (188, 116, 32, 255))
    cube(im, 0, 0, 2, 2, 1, NU_EAR, NU_FUR_DARK, NU_SPINE)
    cube(im, 38, 0, 2, 2, 1, NU_EAR, NU_FUR_DARK, NU_SPINE)

    # Хвост: голый, с поперечными чешуйками — читается даже мелким.
    cube(im, 44, 0, 2, 9, 2, NU_TAIL, NU_TAIL_HI, NU_TAIL_DARK)
    for y in range(2, 11, 2):
        hline(im, y, 46, 47, NU_TAIL_DARK)

    for u, v in ((52, 0), (52, 5), (26, 8), (34, 8)):
        cube(im, u, v, 2, 3, 2, NU_FUR, NU_FUR_HI, NU_FUR_DARK)
    return im


def main():
    save(wild_boar(), "wild_boar")
    save(caucasian_shepherd(), "caucasian_shepherd")
    save(nutria(), "nutria")


if __name__ == "__main__":
    main()
