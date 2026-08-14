"""Кубанский глечик: геометрия и текстура.

Ключевое требование — горло действительно открытое. В прежней версии
«внутренность» была плоской пластиной 4×1×4 НАД венчиком: сверху читалась
как крышка, а не как отверстие. Здесь вместо неё вертикальная шахта из
четырёх стенок и дна: сверху видно тёмное нутро на реальной глубине.

Атлас 64×64, box UV. Тот же порядок сборки, что и у джиннии: геометрия
задаёт UV, текстура рисует по ним.
"""
import json
import os

from PIL import Image

import gen_genie_model
from genie_parts import Bone, BLACK

ATLAS = 64
ROOT = os.path.normpath(os.path.join(os.path.dirname(__file__), "..", ".."))
GEO = os.path.join(ROOT, "src/main/resources/assets/kubanhorizons"
                         "/geckolib/models/kuban_jug.geo.json")
TEX = os.path.join(ROOT, "src/main/resources/assets/kubanhorizons"
                         "/textures/entity/kuban_jug.png")

# Терракота по ART_BIBLE §2 + тёмное нутро.
CLAY = (181, 86, 31, 255)
CLAY_HI = (206, 112, 52, 255)
CLAY_SH = (138, 63, 22, 255)
CLAY_SH2 = (104, 46, 16, 255)
INNER = (28, 16, 10, 255)      # нутро: почти чёрное, но не чистый чёрный

# Профиль: горло, венчик, плечи, тулово, поддон. Стенки шахты — 4 отдельных
# кубоида, поэтому отверстие настоящее, а не нарисованное.
NECK_R = 2.5      # внешний радиус горла
BORE_R = 1.5      # радиус отверстия
Y_RIM = 12.0      # верх венчика
Y_NECK = 8.0      # низ горла
Y_BORE_FLOOR = 5.0  # дно шахты: видно как тёмная глубина


def build():
    bones = []
    jug = Bone("jug", None, [0, 0, 0])
    bones.append(jug)

    # Тулово и плечи — кольцами, а не сплошными кубоидами: иначе они сами
    # становятся дном шахты, и горло снова выглядит закрытым. Толщина стенки
    # WALL, отверстие радиусом BORE_R проходит сквозь них до Y_BORE_FLOOR.
    def ring(prefix, y0, y1, half, y_from_center=None):
        """Четыре стенки квадратного кольца с отверстием BORE_R в центре."""
        h = y1 - y0
        jug.add(prefix + "_n", [-half, y0, -half], [half * 2, h, half - BORE_R])
        jug.add(prefix + "_s", [-half, y0, BORE_R], [half * 2, h, half - BORE_R])
        jug.add(prefix + "_e", [-half, y0, -BORE_R], [half - BORE_R, h,
                                                      BORE_R * 2])
        jug.add(prefix + "_w", [BORE_R, y0, -BORE_R], [half - BORE_R, h,
                                                       BORE_R * 2])

    # Ниже дна шахты тулово сплошное — там отверстия уже нет.
    jug.add("base", [-4, 0, -4], [8, Y_BORE_FLOOR - 0.4, 8])
    # Выше дна — кольцами.
    ring("body", Y_BORE_FLOOR - 0.4, 8.0, 4.0)
    ring("shoulder", 2.0, 7.0, 5.0)
    # Горло: обечайка тоже кольцом.
    ring("neck", Y_NECK, Y_RIM - 1.0, NECK_R)
    # Венчик — кольцо: сплошная плита и была причиной «закрытого» горла.
    ring("rim", Y_RIM - 1.0, Y_RIM, 3.0)
    # Дно шахты: видно как тёмная глубина.
    jug.add("bore_floor", [-BORE_R, Y_BORE_FLOOR - 0.4, -BORE_R],
            [BORE_R * 2, 0.4, BORE_R * 2])
    # Одна ручка.
    jug.add("handle", [-5, 4, -1], [1, 5, 2])
    return bones


def paint(bones):
    import gen_genie_texture as gt
    im = Image.new("RGBA", (ATLAS, ATLAS), (0, 0, 0, 0))
    stub = type("S", (), {"im": im})()

    def faces(name):
        cube = next(c for b in bones for c in b.cubes if c.name == name)
        return gt._faces_of(stub, cube)

    def solid(name, base, top=None, bottom=None, side=None):
        fs = faces(name)
        for key, f in fs.items():
            if key == "up":
                f.fill(top or base)
            elif key == "down":
                f.fill(bottom or CLAY_SH2)
            elif key in ("east", "west"):
                f.fill(side or base)
            else:
                f.fill(base)
        return fs

    # Тулово с красно-чёрной геометрической полосой вокруг корпуса.
    solid("base", CLAY, top=CLAY, bottom=CLAY_SH2, side=CLAY)
    for side in ("n", "s", "e", "w"):
        fs = solid("body_" + side, CLAY, top=CLAY_HI, bottom=CLAY_SH2,
                   side=CLAY)
        for key in ("north", "south", "east", "west"):
            f = fs[key]
            if f.h < 5:
                continue
            band = f.h // 2
            gt.embroidery_band(f, band, 1, f.w - 2, "diamond")
            f.hline(band - 2, 0, f.w - 1, BLACK)
            f.hline(band + 2, 0, f.w - 1, BLACK)

    for side in ("n", "s", "e", "w"):
        fs = solid("shoulder_" + side, CLAY_HI, top=CLAY_HI, bottom=CLAY_SH,
                   side=CLAY_HI)
        for key in ("north", "south", "east", "west"):
            f = fs[key]
            if f.h >= 3:
                gt.embroidery_band(f, f.h // 2, 1, f.w - 2, "cross")

    for side in ("n", "s", "e", "w"):
        fs = solid("neck_" + side, CLAY, top=CLAY_SH, bottom=CLAY_SH, side=CLAY)
        fs["up"].fill(CLAY_SH)
    for side in ("n", "s", "e", "w"):
        fs = solid("rim_" + side, CLAY_HI, top=CLAY_HI, bottom=CLAY_SH,
                   side=CLAY_HI)
        # Внутренние торцы венчика — уже нутро, а не глина.
        fs["down"].fill(INNER)

    # Дно шахты: тёмное, свет сверху до него не достаёт.
    solid("bore_floor", INNER, top=INNER, bottom=INNER, side=INNER)

    handle = solid("handle", CLAY, top=CLAY_HI, bottom=CLAY_SH, side=CLAY)
    for key in ("north", "south"):
        handle[key].vline(0, 0, handle[key].h - 1, CLAY_SH)
    return im


def to_geo(bones):
    def num(v):
        return int(v) if float(v).is_integer() else round(float(v), 3)

    out = []
    for b in bones:
        entry = {"name": b.name, "pivot": [num(v) for v in b.pivot]}
        if b.parent:
            entry["parent"] = b.parent
        entry["cubes"] = [{
            "origin": [num(v) for v in c.pos],
            "size": [num(v) for v in c.size],
            "uv": [num(v) for v in c.uv],
        } for c in b.cubes]
        out.append(entry)
    return {
        "format_version": "1.12.0",
        "minecraft:geometry": [{
            "description": {
                "identifier": "geometry.kuban_jug",
                "texture_width": ATLAS,
                "texture_height": ATLAS,
                "visible_bounds_width": 2,
                "visible_bounds_height": 2,
                "visible_bounds_offset": [0, 0.5, 0],
            },
            "bones": out,
        }],
    }


def main():
    bones = build()
    gen_genie_model.pack(bones, atlas=ATLAS)
    problems = verify(bones)
    if problems:
        for p in problems:
            print("  ✗ %s" % p)
        raise SystemExit(1)
    with open(GEO, "w", encoding="utf-8") as fh:
        json.dump(to_geo(bones), fh, indent=2, ensure_ascii=False)
        fh.write("\n")
    im = paint(bones)
    im.save(TEX)
    print("глечик: %s" % os.path.relpath(GEO, ROOT))
    print("  кубоидов %d, горло открыто на %.1f px в глубину"
          % (sum(len(b.cubes) for b in bones), Y_RIM - 1.0 - Y_BORE_FLOOR))
    print("текстура: %s" % os.path.relpath(TEX, ROOT))


def verify(bones):
    """Проверяет, что горло действительно открыто, а UV не пересекаются.

    Луч сверху по центру обязан дойти до дна шахты: если он упирается в
    кубоид выше, значит горло снова «закрыто крышкой», как в прежней версии.
    """
    cubes = [c for b in bones for c in b.cubes]
    blockers = [c for c in cubes
                if c.lo()[0] <= 0 <= c.hi()[0]
                and c.lo()[2] <= 0 <= c.hi()[2]
                and c.hi()[1] > Y_BORE_FLOOR]
    problems = []
    if blockers:
        problems.append("горло перекрыто сверху: %s"
                        % ", ".join(c.name for c in blockers))
    depth = (Y_RIM - 1.0) - Y_BORE_FLOOR
    if depth < 3.0:
        problems.append("горло слишком мелкое (%.1f px), нутро не читается"
                        % depth)
    mask = {}
    for c in cubes:
        w, h = c.footprint()
        x, y = int(c.uv[0]), int(c.uv[1])
        if x + w > ATLAS or y + h > ATLAS:
            problems.append("UV за границей атласа: %s" % c.name)
        for yy in range(y, y + int(h)):
            for xx in range(x, x + int(w)):
                if (xx, yy) in mask:
                    problems.append("UV-наложение %s и %s"
                                    % (mask[(xx, yy)], c.name))
                    return problems
                mask[(xx, yy)] = c.name
    return problems


if __name__ == "__main__":
    main()
