"""Текстуры почвенного яруса: чернозём и вспаханный чернозём.

ART_BIBLE.md §2 открывается двумя цветами чернозёма — это первое, что
библия называет о регионе. Здесь они и используются как есть:
`chernozem_dark` (#2b2016) и `chernozem` (#3d2e1e).

Задача текстуры — чтобы чернозём читался как чернозём с трёх метров, то
есть заметно темнее ванильной земли, но не как чёрный провал: иначе на
грядке не видно ни борозд, ни всходов.
"""
import os

from texlib import PAL, img, px, rect, vline, hline

# Оттенки строятся от двух опорных тонов библии. Всего восемь — предел
# ART_BIBLE §2 (8–10) соблюдён с запасом.
SOIL_BASE = PAL["chernozem"]              # #3d2e1e — основной тон почвы
SOIL_DARK = PAL["chernozem_dark"]         # #2b2016 — влажная земля, тени
SOIL_HI = (78, 60, 40, 255)               # блик сверху-слева
SOIL_MID = (52, 39, 26, 255)              # переход между базой и тенью
SOIL_CRUMB = (90, 71, 48, 255)            # комок земли, пойманный светом
# Перегной: чернозём тем и ценен, поэтому в текстуре есть редкие
# зернистые вкрапления — они читаются как структура, а не как шум.
SOIL_HUMUS = (36, 27, 19, 255)
# Борозда вспаханного слоя: темнее базы, потому что в глубине борозды
# тень, и чуть теплее — там влажно.
FURROW = (46, 34, 22, 255)
FURROW_DARK = (33, 24, 17, 255)

OUT = os.path.join(os.path.dirname(__file__), "..", "..",
                   "src/main/resources/assets/kubanhorizons/textures/block")


def save(im, name):
    os.makedirs(OUT, exist_ok=True)
    im.save(os.path.join(OUT, name + ".png"))
    print("block/" + name)


def _humus_grain(im):
    """Зерно перегноя: фиксированный узор, а не случайный шум.

    Позиции подобраны так, чтобы при тайлинге 16×16 не возникало видимых
    строк и диагоналей — иначе поле чернозёма пошло бы «полосами».
    """
    for x, y in ((1, 3), (6, 1), (11, 4), (14, 2),
                 (3, 7), (8, 6), (13, 9),
                 (2, 11), (7, 13), (10, 10), (15, 14),
                 (5, 15), (12, 12)):
        px(im, x, y, SOIL_HUMUS)


def chernozem():
    """Чернозём: плотная тёмная земля с комковатой структурой."""
    im = img()
    rect(im, 0, 0, 15, 15, SOIL_BASE)

    # Комковатость — пятна двух тонов вокруг опорной базы. Земля не
    # однородна, но и не пестрит: пятна крупные, по 2–3 пикселя.
    for x, y in ((2, 1), (9, 2), (5, 5), (12, 6), (1, 9), (7, 10),
                 (13, 13), (4, 14)):
        rect(im, x, y, min(15, x + 1), min(15, y + 1), SOIL_MID)
    for x, y in ((6, 3), (11, 8), (3, 12), (14, 4)):
        rect(im, x, y, min(15, x + 1), min(15, y + 1), SOIL_DARK)

    # Свет сверху-слева (ART_BIBLE §3): верхняя и левая кромки светлее,
    # нижняя и правая — обводка тёмным тоном собственного цвета, не чёрным.
    hline(im, 0, 0, 15, SOIL_HI)
    vline(im, 0, 0, 15, SOIL_HI)
    hline(im, 15, 0, 15, SOIL_DARK)
    vline(im, 15, 0, 15, SOIL_DARK)

    # Отдельные комки, поймавшие свет: дают объём без детализации.
    for x, y in ((4, 2), (10, 5), (2, 7), (8, 12), (13, 10)):
        px(im, x, y, SOIL_CRUMB)

    _humus_grain(im)
    return im


def chernozem_farmland(moist=False):
    """Вспаханный чернозём: борозды поперёк грядки.

    Влажный вариант темнее и насыщеннее — ванильная грядка так же
    отличает `farmland` от `farmland_moist`, и игрок читает влажность
    по цвету, не открывая щуп.
    """
    im = img()
    base = SOIL_DARK if moist else SOIL_BASE
    furrow = FURROW_DARK if moist else FURROW
    rect(im, 0, 0, 15, 15, base)

    # Четыре борозды: пахота видна сверху как правильный ритм гребней.
    # Ритм регулярный намеренно — это след плуга, а не природная фактура.
    for y in (2, 6, 10, 14):
        hline(im, y, 0, 15, furrow)
        hline(im, y - 1, 0, 15, SOIL_HI if not moist else SOIL_MID)

    # Гребни между борозд: светлая верхняя кромка гребня — свет сверху.
    for y in (0, 4, 8, 12):
        for x in range(0, 16, 3):
            px(im, x, y, SOIL_CRUMB if not moist else SOIL_BASE)

    # Обводка: тёмный тон собственного цвета.
    hline(im, 15, 0, 15, FURROW_DARK)
    vline(im, 15, 0, 15, FURROW_DARK)

    if not moist:
        _humus_grain(im)
    return im


def main():
    save(chernozem(), "chernozem")
    save(chernozem_farmland(moist=False), "chernozem_farmland")
    save(chernozem_farmland(moist=True), "chernozem_farmland_moist")


if __name__ == "__main__":
    main()
