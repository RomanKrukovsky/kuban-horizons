"""Текстура Кубанской джиннии: атлас 128×128 + glowmask.

Рисует по box-UV раскладке, которую посчитал ``gen_genie_model``, поэтому
узор гарантированно попадает на нужную грань нужного кубоида. Ни одной
координаты «на глаз»: каждая грань запрашивается через ``Face``.

Box-UV кубоида W×H×D (проверено на существующем атласе, не по документации):

    (D, 0)      up      W×D
    (D+W, 0)    down    W×D
    (0, D)      east    D×H     ← экранно левая сторона
    (D, D)      north   W×H     ← лицо, смотрит в -Z
    (D+W, D)    west    D×H
    (2D+W, D)   south   W×H     ← спина

Вышивка: красный ВСЕГДА с чёрным контуром. Чистый красный по белому на
16 px размывается фильтрацией в розовое — контур держит рисунок.
"""
import os

from PIL import Image

import genie_parts as gp
import gen_genie_model
from genie_parts import (
    SKIN, SKIN_HI, SKIN_SH, SKIN_SH2, HAIR, HAIR_HI, HAIR_SH,
    LINEN, LINEN_SH, LINEN_SH2, RED, RED_DK, BLACK,
    GOLD, GOLD_HI, GOLD_DK, SAPPHIRE, SAPPHIRE_HI,
    CORAL, CORAL_HI, CORAL_DK, BELT_RED, BELT_RED_DK,
    EYE_WHITE, EYE_BROWN, EYE_BROWN_HI, MOUTH, BLUSH,
    SUN_PETAL, SUN_PETAL_HI, SUN_CORE, LENS,
    TAIL_COLORS, TAIL_SH, TAIL_HI,
)

ATLAS = gen_genie_model.ATLAS
ROOT = os.path.normpath(os.path.join(os.path.dirname(__file__), "..", ".."))
OUT_DIR = os.path.join(ROOT, "src/main/resources/assets/kubanhorizons"
                             "/textures/entity")
CLEAR = (0, 0, 0, 0)


class Face:
    """Прямоугольник грани в координатах атласа с локальной адресацией.

    Локальные координаты идут от левого верхнего угла грани, поэтому узор
    описывается независимо от того, куда упаковщик положил развёртку.
    """

    def __init__(self, canvas, x, y, w, h):
        self.c = canvas
        self.x = int(x)
        self.y = int(y)
        self.w = int(w)
        self.h = int(h)

    def px(self, lx, ly, col):
        if 0 <= lx < self.w and 0 <= ly < self.h:
            self.c.putpixel((self.x + lx, self.y + ly), col)

    def fill(self, col):
        self.rect(0, 0, self.w - 1, self.h - 1, col)

    def rect(self, x0, y0, x1, y1, col):
        for ly in range(min(y0, y1), max(y0, y1) + 1):
            for lx in range(min(x0, x1), max(x0, x1) + 1):
                self.px(lx, ly, col)

    def hline(self, ly, x0, x1, col):
        self.rect(x0, ly, x1, ly, col)

    def vline(self, lx, y0, y1, col):
        self.rect(lx, y0, lx, y1, col)

    def border(self, col):
        self.hline(0, 0, self.w - 1, col)
        self.hline(self.h - 1, 0, self.w - 1, col)
        self.vline(0, 0, self.h - 1, col)
        self.vline(self.w - 1, 0, self.h - 1, col)

    def get(self, lx, ly):
        if 0 <= lx < self.w and 0 <= ly < self.h:
            return self.c.getpixel((self.x + lx, self.y + ly))
        return CLEAR


class Sheet:
    """Атлас с доступом к граням кубоидов по имени."""

    def __init__(self, bones, size=ATLAS):
        self.im = Image.new("RGBA", (size, size), CLEAR)
        self.cubes = {}
        for bone in bones:
            for cube in bone.cubes:
                self.cubes[cube.name] = cube

    def faces(self, name):
        """Возвращает dict граней кубоида: up/down/east/north/west/south."""
        c = self.cubes[name]
        w, h, d = (int(round(v)) for v in c.size)
        # Дробные размеры округляем вверх — так же, как упаковщик.
        w = max(1, int(-(-c.size[0] // 1)))
        h = max(1, int(-(-c.size[1] // 1)))
        d = max(1, int(-(-c.size[2] // 1)))
        ux, uy = int(c.uv[0]), int(c.uv[1])
        f = lambda x, y, fw, fh: Face(self.im, x, y, fw, fh)
        return {
            "up": f(ux + d, uy, w, d),
            "down": f(ux + d + w, uy, w, d),
            "east": f(ux, uy + d, d, h),
            "north": f(ux + d, uy + d, w, h),
            "west": f(ux + d + w, uy + d, d, h),
            "south": f(ux + 2 * d + w, uy + d, w, h),
        }

    def solid(self, name, base, top=None, bottom=None, side=None):
        """Заливает все грани кубоида с лёгким разделением по свету.

        Свет сверху-слева (ART_BIBLE §2): верх светлее, низ темнее.
        """
        fs = self.faces(name)
        for key, face in fs.items():
            if key == "up":
                face.fill(top or base)
            elif key == "down":
                face.fill(bottom or _dark(base))
            elif key in ("east", "west"):
                face.fill(side or base)
            else:
                face.fill(base)
        return fs


def _dark(c, k=0.82):
    r, g, b, a = c
    return (int(r * k), int(g * k), int(b * k), a)


def _lite(c, k=1.12):
    r, g, b, a = c
    f = lambda v: min(255, int(v * k))
    return (f(r), f(g), f(b), a)


# --- Кубанская вышивка -------------------------------------------------------
def embroidery_band(face, ly, x0, x1, motif="diamond"):
    """Рисует полосу геометрического орнамента с чёрным контуром.

    Мотивы строго народные: ромбы, косые кресты, восьмиконечные звёзды.
    Мягкие цветочные завитки не используются — они читаются как модерн, а не
    как кубанская вышивка.
    """
    w = x1 - x0 + 1
    if motif == "diamond":
        # Ромб 3×3 с шагом 4: контур чёрный, ядро красное.
        for cx in range(x0 + 1, x1, 4):
            face.px(cx, ly, RED)
            face.px(cx - 1, ly, BLACK)
            face.px(cx + 1, ly, BLACK)
            face.px(cx, ly - 1, BLACK)
            face.px(cx, ly + 1, BLACK)
    elif motif == "cross":
        # Косой крест 3×3, шаг 4.
        for cx in range(x0 + 1, x1, 4):
            face.px(cx, ly, RED)
            for dx, dy in ((-1, -1), (1, -1), (-1, 1), (1, 1)):
                face.px(cx + dx, ly + dy, RED_DK)
            face.px(cx, ly - 1, BLACK)
            face.px(cx, ly + 1, BLACK)
    elif motif == "star8":
        # Восьмиконечная звезда: центральный пиксель + четыре луча.
        for cx in range(x0 + 2, x1 - 1, 6):
            face.px(cx, ly, RED)
            for dx, dy in ((0, -1), (0, 1), (-1, 0), (1, 0)):
                face.px(cx + dx, ly + dy, RED)
            for dx, dy in ((-1, -1), (1, -1), (-1, 1), (1, 1)):
                face.px(cx + dx, ly + dy, BLACK)
    elif motif == "line":
        face.hline(ly, x0, x1, RED)
        face.hline(ly - 1, x0, x1, BLACK)
        face.hline(ly + 1, x0, x1, BLACK)


def embroidered_panel(face, motifs=("diamond", "cross"), gold_edge=True):
    """Белое полотно с золотой окантовкой и полосами орнамента."""
    face.fill(LINEN)
    # Тень по нижней и правой границе: полотно не должно быть плоским.
    face.hline(face.h - 1, 0, face.w - 1, LINEN_SH)
    face.vline(face.w - 1, 0, face.h - 1, LINEN_SH)
    if gold_edge:
        face.hline(0, 0, face.w - 1, GOLD)
        if face.h > 3:
            face.hline(face.h - 1, 0, face.w - 1, GOLD_DK)
    rows = [y for y in range(2, face.h - 2, 3)]
    for i, ly in enumerate(rows):
        embroidery_band(face, ly, 1, face.w - 2, motifs[i % len(motifs)])


# --- Части персонажа ---------------------------------------------------------
def paint_head(sh):
    fs = sh.solid("head", SKIN, top=SKIN_HI, bottom=SKIN_SH, side=SKIN)
    face = fs["north"]
    w, h = face.w, face.h
    # Линия волос занимает верхние 2–3 px лица: сама причёска стоит выше
    # Y_FACE_TOP, поэтому лоб рисуется здесь.
    face.rect(0, 0, w - 1, 1, HAIR)
    for lx in range(0, w, 2):
        face.px(lx, 2, HAIR)
    # Глаза: большие выразительные карие. На 8 px решают три пикселя.
    ey = 4
    for ex in (1, w - 4):
        face.rect(ex, ey, ex + 2, ey + 1, EYE_WHITE)
        face.px(ex + 1, ey, EYE_BROWN_HI)
        face.px(ex + 1, ey + 1, EYE_BROWN)
        for dx in range(3):
            face.px(ex + dx, ey - 1, HAIR)   # ресница
    # Лёгкий румянец под глазами.
    face.px(0, ey + 2, BLUSH)
    face.px(w - 1, ey + 2, BLUSH)
    # Аккуратная улыбка с приподнятыми уголками.
    face.px(3, h - 1, MOUTH)
    face.px(4, h - 1, MOUTH)
    face.px(2, h - 2, _dark(MOUTH))
    face.px(5, h - 2, _dark(MOUTH))
    # Затылок и верх головы скрыты волосами, но кожа не должна светиться
    # сквозь стык, поэтому там тень, а не базовый тон.
    fs["south"].fill(SKIN_SH)
    fs["up"].fill(SKIN_SH)

    # Волосы на голове: отдельные кубоиды сверху, по бокам и на затылке.
    # Лицо не перекрывается геометрией, поэтому прозрачных «окон» в текстуре
    # больше не требуется — волосы просто не стоят перед лицом.
    for name in ("hair_top", "hair_side_l", "hair_side_r", "hair_nape",
                 "hair_fringe"):
        fs = sh.solid(name, HAIR, top=HAIR_HI, bottom=HAIR_SH, side=HAIR)
        for key in ("north", "south", "east", "west"):
            f = fs[key]
            # Пряди: вертикальные штрихи чуть светлее основы, чтобы масса
            # не выглядела плоской заливкой.
            for lx in range(0, f.w, 3):
                f.vline(lx, 0, f.h - 1, HAIR_HI)
            for lx in range(2, f.w, 5):
                f.vline(lx, f.h // 2, f.h - 1, HAIR_SH)
    # Золотая лента на затылке маскирует стык волос головы с задней массой.
    nape = sh.faces("hair_nape")
    nape["down"].fill(GOLD)
    nape["south"].hline(nape["south"].h - 1, 0, nape["south"].w - 1, GOLD)

    # Очки: тонкая чёрная прямоугольная оправа, линзы полупрозрачные по тону.
    g = sh.faces("glasses")
    for key in ("north", "south", "up", "down", "east", "west"):
        g[key].fill(CLEAR)
    gf = g["north"]
    gf.border(BLACK)
    gf.rect(1, 1, 2, gf.h - 2, LENS)
    gf.rect(gf.w - 3, 1, gf.w - 2, gf.h - 2, LENS)
    gf.px(1, 1, _lite(LENS, 1.6))          # блик на левой линзе
    gf.px(gf.w - 3, 1, _lite(LENS, 1.4))
    gf.vline(gf.w // 2, 1, 1, BLACK)       # переносица
    g["up"].hline(0, 0, g["up"].w - 1, BLACK)

    # Диадема с крупным центральным сапфиром.
    t = sh.solid("tiara", GOLD, top=GOLD_HI, bottom=GOLD_DK, side=GOLD)
    tn = t["north"]
    for lx in range(0, tn.w, 2):
        tn.px(lx, tn.h - 1, GOLD_DK)
    # Торцы диадемы: заливаем целиком, иначе на виде сбоку видна щель.
    for key in ("east", "west"):
        t[key].fill(GOLD)
    for name, hi in (("tiara_gem_c", True), ("tiara_gem_l", False),
                     ("tiara_gem_r", False)):
        gem = sh.solid(name, SAPPHIRE, top=SAPPHIRE_HI,
                       bottom=_dark(SAPPHIRE, 0.7), side=SAPPHIRE)
        if hi:
            gem["north"].px(0, 0, SAPPHIRE_HI)

    for name in ("earring_l", "earring_r"):
        e = sh.solid(name, GOLD, top=GOLD_HI, bottom=GOLD_DK, side=GOLD)
        # Синий камень в нижней части серьги.
        for key in ("north", "south", "east", "west"):
            e[key].px(0, e[key].h - 1, SAPPHIRE)


def paint_hair(sh):
    for name in ("hair_mass", "hair_tips", "hair_lock_l", "hair_lock_r"):
        fs = sh.solid(name, HAIR, top=HAIR_HI, bottom=HAIR_SH, side=HAIR)
        for key in ("north", "south", "east", "west"):
            f = fs[key]
            # Пряди с переменным шагом: ровная гребёнка выглядит как текстиль.
            for lx in range(0, f.w, 3):
                f.vline(lx, 0, f.h - 1, HAIR_HI)
            for lx in range(1, f.w, 5):
                f.vline(lx, f.h // 2, f.h - 1, HAIR_SH)


def paint_torso(sh):
    # Открытый живот: кожа с лёгкой тенью по бокам талии.
    for name in ("ribs", "waist"):
        fs = sh.solid(name, SKIN, top=SKIN_HI, bottom=SKIN_SH, side=SKIN_SH)
        f = fs["north"]
        # Тень по бокам подчёркивает вогнутую дугу талии.
        f.vline(0, 0, f.h - 1, SKIN_SH)
        f.vline(f.w - 1, 0, f.h - 1, SKIN_SH)

    chest = sh.solid("chest", SKIN, top=SKIN_HI, bottom=SKIN_SH, side=SKIN)
    # Тень непосредственно под грудью — читается объём, а не плоская доска.
    chest["north"].hline(chest["north"].h - 1, 0, chest["north"].w - 1, SKIN_SH)
    chest["north"].hline(chest["north"].h - 2, 1, chest["north"].w - 2, SKIN_SH2)

    # Лиф: белый, вышитый, с золотой окантовкой.
    b = sh.faces("bodice")
    for key in ("up", "down", "east", "west", "north", "south"):
        b[key].fill(LINEN)
    embroidered_panel(b["north"], motifs=("star8", "diamond"))
    embroidered_panel(b["south"], motifs=("diamond", "cross"))
    for key in ("east", "west"):
        f = b[key]
        f.fill(LINEN)
        f.hline(0, 0, f.w - 1, GOLD)
        embroidery_band(f, f.h // 2, 1, f.w - 2, "diamond")
    b["up"].fill(LINEN_SH)
    b["down"].fill(LINEN_SH2)
    # Золотая центральная линия лифа.
    bn = b["north"]
    bn.vline(bn.w // 2, 1, bn.h - 2, GOLD_DK)

    # Коралловые бусы: три нити крупных красных бусин.
    n = sh.faces("necklace")
    for key in n:
        n[key].fill(CLEAR)
    nf = n["north"]
    for row in range(min(3, nf.h)):
        for lx in range(row % 2, nf.w, 2):
            nf.px(lx, row, CORAL if row != 1 else CORAL_DK)
        for lx in range(row % 2, nf.w, 4):
            nf.px(lx, row, CORAL_HI)
    n["up"].fill(CORAL_DK)

    # Украшение пупка: сапфир и короткая золотая цепочка вверх к лифу.
    ng = sh.faces("navel_gem")
    for key in ng:
        ng[key].fill(CLEAR)
    gf = ng["north"]
    gf.fill(SAPPHIRE)
    gf.px(0, 0, SAPPHIRE_HI)
    ng["up"].fill(GOLD)


def paint_arm_cubes(sh, bones):
    """Красит руки через кости: кубоид `arm` есть и в arm_l, и в arm_r."""
    by = gp.bones_by_name(bones)
    for bone_name in ("arm_l", "arm_r"):
        for cube in by[bone_name].cubes:
            fs = _faces_of(sh, cube)
            if cube.name == "arm":
                for key, f in fs.items():
                    f.fill(SKIN if key not in ("up", "down") else
                           (SKIN_HI if key == "up" else SKIN_SH))
                # Кисть — сплошная, лишь тень у нижней кромки.
                for key in ("north", "south", "east", "west"):
                    f = fs[key]
                    f.hline(f.h - 1, 0, f.w - 1, SKIN_SH)
                    f.hline(f.h - 2, 0, f.w - 1, SKIN_SH2)
            elif cube.name == "sleeve":
                for key in ("north", "south", "east", "west"):
                    embroidered_panel(fs[key], motifs=("diamond", "cross"))
                fs["up"].fill(LINEN_SH)
                fs["down"].fill(LINEN_SH2)
            elif cube.name == "bangles":
                for key, f in fs.items():
                    f.fill(GOLD)
                    # Тонкая тёмная нижняя кромка для контраста.
                    f.hline(f.h - 1, 0, f.w - 1, GOLD_DK)
                    for lx in range(0, f.w, 2):
                        f.px(lx, 0, GOLD_HI)
            elif cube.name == "sunflower":
                for key, f in fs.items():
                    f.fill(SUN_PETAL)
                    for lx in range(0, f.w, 2):
                        f.vline(lx, 0, f.h - 1, SUN_PETAL_HI)
                # Тёмно-коричневая сердцевина на лицевой грани.
                f = fs["north"]
                f.rect(f.w // 2 - 1, f.h // 2 - 1, f.w // 2, f.h // 2, SUN_CORE)


def _faces_of(sh, cube):
    w = max(1, int(-(-cube.size[0] // 1)))
    h = max(1, int(-(-cube.size[1] // 1)))
    d = max(1, int(-(-cube.size[2] // 1)))
    ux, uy = int(cube.uv[0]), int(cube.uv[1])
    f = lambda x, y, fw, fh: Face(sh.im, x, y, fw, fh)
    return {
        "up": f(ux + d, uy, w, d),
        "down": f(ux + d + w, uy, w, d),
        "east": f(ux, uy + d, d, h),
        "north": f(ux + d, uy + d, w, h),
        "west": f(ux + d + w, uy + d, d, h),
        "south": f(ux + 2 * d + w, uy + d, w, h),
    }


def paint_hips(sh):
    for name in ("hips", "hips_low"):
        fs = sh.solid(name, SKIN, top=SKIN_HI, bottom=SKIN_SH, side=SKIN)
        f = fs["north"]
        f.vline(0, 0, f.h - 1, SKIN_SH)
        f.vline(f.w - 1, 0, f.h - 1, SKIN_SH)


def paint_belt(sh):
    fs = sh.faces("belt")
    for key, f in fs.items():
        f.fill(BELT_RED)
        # Золотая окантовка сверху и снизу.
        f.hline(0, 0, f.w - 1, GOLD)
        f.hline(f.h - 1, 0, f.w - 1, GOLD_DK)
    for key in ("north", "south", "east", "west"):
        f = fs[key]
        embroidery_band(f, f.h // 2, 1, f.w - 2, "diamond")
        for lx in range(0, f.w, 4):
            f.px(lx, 1, BELT_RED_DK)
    fs["up"].fill(_dark(BELT_RED, 0.9))
    fs["down"].fill(BELT_RED_DK)

    for name in ("belt_gem_c", "belt_gem_l", "belt_gem_r"):
        g = sh.solid(name, SAPPHIRE, top=SAPPHIRE_HI,
                     bottom=_dark(SAPPHIRE, 0.7), side=SAPPHIRE)
        g["north"].px(0, 0, SAPPHIRE_HI)

    # Подвески: короткие золотые цепочки.
    for name in ("pendant_l", "pendant_c", "pendant_r"):
        p = sh.solid(name, GOLD, top=GOLD_HI, bottom=GOLD_DK, side=GOLD)
        for key in ("north", "south", "east", "west"):
            f = p[key]
            for ly in range(0, f.h, 2):
                f.hline(ly, 0, f.w - 1, GOLD_DK)
            f.px(0, f.h - 1, SAPPHIRE)


def paint_rushnyk(sh):
    """Рушник: белая основа, красно-чёрная вышивка, красная бахрома снизу."""
    parts = ("rushnyk", "rushnyk_mid", "rushnyk_tip")
    for i, name in enumerate(parts):
        fs = sh.faces(name)
        for key in ("north", "south"):
            f = fs[key]
            f.fill(LINEN)
            f.vline(0, 0, f.h - 1, LINEN_SH)
            f.vline(f.w - 1, 0, f.h - 1, LINEN_SH)
            motifs = ("diamond", "cross", "star8")
            for j, ly in enumerate(range(1, f.h - 1, 3)):
                embroidery_band(f, ly, 1, f.w - 2, motifs[j % 3])
        for key in ("east", "west"):
            fs[key].fill(LINEN_SH)
        fs["up"].fill(LINEN_SH)
        fs["down"].fill(LINEN_SH2)
        # Бахрома только на последнем звене.
        if name == "rushnyk_tip":
            for key in ("north", "south"):
                f = fs[key]
                f.hline(f.h - 1, 0, f.w - 1, RED)
                for lx in range(0, f.w, 2):
                    f.px(lx, f.h - 2, RED_DK)
            fs["down"].fill(RED)


def paint_tail(sh):
    """Хвост: заданный градиент сине-фиолетовый, дизеринг на последних звеньях.

    Растворение делается шахматкой полностью прозрачных пикселей, а не
    альфой: translucent-сущности в Minecraft сортируются неверно и хвост
    просвечивал бы сам сквозь себя на изгибе.
    """
    for i in range(7):
        name = "tail%d" % (i + 1)
        base, sh_c, hi = TAIL_COLORS[i], TAIL_SH[i], TAIL_HI[i]
        fs = sh.solid(name, base, top=hi, bottom=sh_c, side=base)
        for key in ("north", "south", "east", "west"):
            f = fs[key]
            # Струи дыма редкие: базовый цвет обязан остаться доминирующим,
            # иначе заданный градиент читается как другой оттенок.
            for lx in range(1, f.w, 4):
                f.vline(lx, 0, f.h - 1, hi)
            for lx in range(3, f.w, 6):
                f.vline(lx, f.h // 2, f.h - 1, sh_c)
        # Дизеринг: tail6 — 25% прозрачных, tail7 — 50%.
        drop = {5: 4, 6: 2}.get(i)
        if drop:
            for key, f in fs.items():
                for ly in range(f.h):
                    for lx in range(f.w):
                        if drop == 2:
                            if (lx + ly) % 2 == 0:
                                f.px(lx, ly, CLEAR)
                        elif (lx % 2 == 0) and (ly % 2 == 0):
                            f.px(lx, ly, CLEAR)


def build_glowmask(sh, bones):
    """Emissive-слой: только камни, подвески, серьги и два конца хвоста.

    Всё остальное должно остаться прозрачным, иначе персонаж будет светиться
    целиком и потеряет читаемость в темноте.
    """
    glow = Image.new("RGBA", sh.im.size, CLEAR)
    names = {"tiara_gem_c", "tiara_gem_l", "tiara_gem_r",
             "earring_l", "earring_r", "navel_gem",
             "belt_gem_c", "belt_gem_l", "belt_gem_r",
             "pendant_l", "pendant_c", "pendant_r",
             "tail6", "tail7"}
    for bone in bones:
        for cube in bone.cubes:
            if cube.name not in names:
                continue
            for f in _faces_of(sh, cube).values():
                for ly in range(f.h):
                    for lx in range(f.w):
                        src = f.get(lx, ly)
                        if src[3] > 8:
                            glow.putpixel((f.x + lx, f.y + ly), src)
    return glow


def main():
    bones = gp.build()
    gen_genie_model.pack(bones)
    sh = Sheet(bones)

    paint_head(sh)
    paint_hair(sh)
    paint_torso(sh)
    paint_arm_cubes(sh, bones)
    paint_hips(sh)
    paint_belt(sh)
    paint_rushnyk(sh)
    paint_tail(sh)

    glow = build_glowmask(sh, bones)

    tex_path = os.path.join(OUT_DIR, "kuban_genie.png")
    glow_path = os.path.join(OUT_DIR, "kuban_genie_glowmask.png")
    sh.im.save(tex_path)
    glow.save(glow_path)

    pixels = list(sh.im.get_flattened_data())
    opaque = sum(1 for p in pixels if p[3] > 8)
    colors = len({p for p in pixels if p[3] > 8})
    print("текстура: %s" % os.path.relpath(tex_path, ROOT))
    print("  заполнено %d px (%.1f%%), цветов %d"
          % (opaque, 100.0 * opaque / (ATLAS * ATLAS), colors))
    print("glowmask: %s" % os.path.relpath(glow_path, ROOT))
    print("  светится %d px"
          % sum(1 for p in glow.get_flattened_data() if p[3] > 8))


if __name__ == "__main__":
    main()
