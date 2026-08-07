"""Текстуры сущностей Kuban Horizons (развёртки 64×32).

Птицы мода рендерятся собственной моделью GroundBirdModel, а модель без
текстуры даёт чёрно-фиолетовый шахматный куб. UV-раскладка здесь обязана
совпадать с texOffs в GroundBirdModel — иначе оперение окажется на клюве.

Раскладка (совпадает с моделью):
    head   (0,0)   3x5x3     beak  (14,0)  2x1x2
    body   (0,9)   5x8x6     tail  (22,20) 3x1x9
    leg    (26,0)  2x5x2     wing  (24,13) 1x3x6

Палитра по ART_BIBLE §2: фазан — тёмно-рыжий с зелёной головой и красной
маской, перепел — охристый в тёмных пестринах. Свет сверху-слева.
"""
import os

from PIL import Image

from texlib import px, rect, hline

OUT = os.path.join(os.path.dirname(__file__), "..", "..",
                   "src/main/resources/assets/kubanhorizons/textures/entity")

# --- Фазан: самец, узнаваемый по зелёной голове и рыжему корпусу ---
PH_BODY = (150, 74, 38, 255)
PH_BODY_HI = (186, 106, 58, 255)
PH_BODY_DARK = (104, 48, 24, 255)
PH_HEAD = (36, 74, 58, 255)
PH_HEAD_HI = (54, 102, 78, 255)
PH_MASK = (168, 42, 36, 255)
PH_TAIL = (140, 112, 62, 255)
PH_TAIL_DARK = (98, 76, 40, 255)
PH_BEAK = (206, 190, 140, 255)
PH_LEG = (150, 132, 96, 255)

# --- Перепел: мелкий, охристый, в пестринах ---
QU_BODY = (172, 146, 100, 255)
QU_BODY_HI = (200, 178, 132, 255)
QU_BODY_DARK = (126, 104, 68, 255)
QU_HEAD = (150, 126, 86, 255)
QU_HEAD_HI = (178, 154, 112, 255)
QU_BROW = (238, 232, 214, 255)
QU_TAIL = (132, 112, 76, 255)
QU_BEAK = (108, 96, 74, 255)
QU_LEG = (166, 150, 118, 255)


def save(im, name):
    os.makedirs(OUT, exist_ok=True)
    im.save(os.path.join(OUT, name + ".png"))
    print("entity/" + name)


def speckle(im, x0, y0, x1, y1, colour, period=3, offset=0):
    """Пестрины: разреженные пиксели по решётке. Детерминированно, без rng."""
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            if (x + y * 2 + offset) % period == 0:
                px(im, x, y, colour)


def bird_texture(body, body_hi, body_dark, head, head_hi, accent,
                 tail, tail_dark, beak, leg, speckled=False, brow=None):
    """Общая развёртка наземной птицы 64×32."""
    # Ровно 64×32, как объявлено в LayerDefinition.create(mesh, 64, 32):
    # texlib.img() делает квадрат, а квадратная развёртка сдвинет все UV.
    im = Image.new("RGBA", (64, 32), (0, 0, 0, 0))

    # --- Голова: куб 3x5x3, развёртка начинается в (0,0) ---
    # Верх и бока головы.
    rect(im, 0, 0, 11, 4, head)
    # Освещённая левая сторона.
    rect(im, 0, 0, 2, 4, head_hi)
    # Красная маска вокруг глаза (у фазана) либо светлая бровь (у перепела).
    if brow is not None:
        hline(im, 2, 3, 8, brow)
    else:
        rect(im, 4, 1, 7, 2, accent)
    # Глаз: тёмная точка с блеском.
    px(im, 5, 2, (24, 20, 18, 255))
    px(im, 6, 1, (238, 238, 232, 255))

    # --- Клюв: (14,0), 2x1x2 ---
    rect(im, 14, 0, 19, 2, beak)
    px(im, 14, 0, (232, 220, 176, 255))

    # --- Корпус: (0,9), 5x8x6 ---
    rect(im, 0, 9, 21, 19, body)
    # Свет сверху-слева, тень снизу-справа.
    rect(im, 0, 9, 5, 19, body_hi)
    rect(im, 16, 9, 21, 19, body_dark)
    if speckled:
        speckle(im, 0, 9, 21, 19, body_dark, period=4)
        speckle(im, 0, 9, 21, 19, body_hi, period=7, offset=2)
    else:
        # Чешуйчатый рисунок пера: короткие тёмные штрихи.
        for y in range(10, 19, 2):
            for x in range(2, 20, 3):
                px(im, x, y, body_dark)

    # --- Хвост: (22,20), 3x1x9 ---
    rect(im, 22, 20, 43, 30, tail)
    # Поперечные полосы — подпись хвоста фазана.
    for y in range(20, 31, 2):
        hline(im, y, 22, 43, tail_dark)

    # --- Ноги: (26,0), 2x5x2 ---
    rect(im, 26, 0, 33, 6, leg)
    rect(im, 26, 0, 27, 6, (188, 174, 140, 255))

    # --- Крылья: (24,13), 1x3x6 ---
    rect(im, 24, 13, 37, 18, body)
    rect(im, 24, 13, 37, 13, body_hi)
    hline(im, 18, 24, 37, body_dark)
    # Кроющие перья: два ряда штрихов.
    for x in range(25, 37, 2):
        px(im, x, 15, body_dark)
        px(im, x + 1, 17, body_dark)
    return im


def pheasant():
    return bird_texture(PH_BODY, PH_BODY_HI, PH_BODY_DARK, PH_HEAD, PH_HEAD_HI,
                        PH_MASK, PH_TAIL, PH_TAIL_DARK, PH_BEAK, PH_LEG)


def quail():
    return bird_texture(QU_BODY, QU_BODY_HI, QU_BODY_DARK, QU_HEAD, QU_HEAD_HI,
                        QU_BROW, QU_TAIL, QU_BODY_DARK, QU_BEAK, QU_LEG,
                        speckled=True, brow=QU_BROW)


def main():
    save(pheasant(), "pheasant")
    save(quail(), "quail")


if __name__ == "__main__":
    main()
