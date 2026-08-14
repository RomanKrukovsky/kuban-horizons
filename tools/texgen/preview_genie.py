"""Ортографический рендер джиннии в ASCII — проверка силуэта без Blockbench.

Нужен потому, что «песочные часы» и S-образность хвоста нельзя подтвердить
чтением JSON: числа выглядят правильно, а фигура при этом может быть
бочкой.

Рендер честный: сэмплируются поверхности кубоидов, точки прогоняются через
цепочку поворотов костей и попадают в z-буфер. Заливка bounding box здесь
не годится — повёрнутый сегмент хвоста давал бы силуэт вдвое шире реального.

Проекции ровно как в Minecraft: FRONT смотрит в −Z, поэтому экранный
«вправо» — это +X.

Запуск: ``python3 tools/texgen/preview_genie.py [--views front,side,top]``
"""
import argparse
import math

import genie_parts as gp

# Символы по группам частей: силуэт должен читаться, а не быть мешаниной.
GLYPH = [
    ("hair_cap", "h"), ("hair_fringe", "h"), ("hair", "H"),
    ("head", "O"), ("glasses", "="), ("tiara_gem", "*"), ("tiara", "T"),
    ("earring", "o"),
    ("chest", "B"), ("bodice", "#"), ("ribs", "b"), ("waist", "|"),
    ("necklace", "c"), ("navel", "*"),
    ("arm", "A"), ("sleeve", "S"), ("bangles", "g"), ("sunflower", "@"),
    ("hips", "P"), ("belt_gem", "*"), ("belt", "="), ("pendant", "'"),
    ("rushnyk", "R"),
    ("tail1", "1"), ("tail2", "2"), ("tail3", "3"), ("tail4", "4"),
    ("tail5", "5"), ("tail6", "6"), ("tail7", "7"),
]

STEP = 0.34  # шаг сэмплирования поверхности в пикселях модели


def glyph_for(name):
    for key, ch in GLYPH:
        if name.startswith(key):
            return ch
    for key, ch in GLYPH:
        if key in name:
            return ch
    return "?"


def rotate(p, deg):
    """Поворот вокруг Z, затем Y, затем X — порядок как в GeckoLib."""
    x, y, z = p
    rz, ry, rx = (math.radians(deg[2]), math.radians(deg[1]),
                  math.radians(deg[0]))
    if rz:
        x, y = (x * math.cos(rz) - y * math.sin(rz),
                x * math.sin(rz) + y * math.cos(rz))
    if ry:
        x, z = (x * math.cos(ry) + z * math.sin(ry),
                -x * math.sin(ry) + z * math.cos(ry))
    if rx:
        y, z = (y * math.cos(rx) - z * math.sin(rx),
                y * math.sin(rx) + z * math.cos(rx))
    return [x, y, z]


def chain(bones):
    return {b.name: b for b in bones}


def to_world(p, bone, by):
    """Прогоняет точку через повороты кости и всех её родителей."""
    node = bone
    while node is not None:
        if node.rotation and any(node.rotation):
            piv = node.pivot
            local = [p[i] - piv[i] for i in range(3)]
            rot = rotate(local, node.rotation)
            p = [rot[i] + piv[i] for i in range(3)]
        node = by.get(node.parent) if node.parent else None
    return p


def surface_samples(cube):
    """Точки на шести гранях кубоида с шагом STEP."""
    lo, hi = cube.lo(), cube.hi()

    def span(a, b):
        n = max(2, int(math.ceil((b - a) / STEP)) + 1)
        return [a + (b - a) * i / (n - 1) for i in range(n)]

    xs, ys, zs = span(lo[0], hi[0]), span(lo[1], hi[1]), span(lo[2], hi[2])
    pts = []
    for x in xs:
        for y in ys:
            pts.append((x, y, lo[2]))
            pts.append((x, y, hi[2]))
    for x in xs:
        for z in zs:
            pts.append((x, lo[1], z))
            pts.append((x, hi[1], z))
    for y in ys:
        for z in zs:
            pts.append((lo[0], y, z))
            pts.append((hi[0], y, z))
    return pts


def render(bones, axes, depth, title, hscale=2):
    """Z-буферный ортографический рендер в ASCII."""
    ha, va = axes
    daxis, dsign = depth
    by = chain(bones)
    zbuf = {}
    for bone in bones:
        for cube in bone.cubes:
            ch = glyph_for(cube.name)
            for p in surface_samples(cube):
                w = to_world(list(p), bone, by)
                col = int(math.floor(w[ha] * hscale))
                row = int(math.floor(w[va]))
                key = (col, row)
                d = dsign * w[daxis]
                prev = zbuf.get(key)
                if prev is None or d > prev[0]:
                    zbuf[key] = (d, ch)
    if not zbuf:
        return ""
    cols = [k[0] for k in zbuf]
    rows = [k[1] for k in zbuf]
    out = ["--- %s ---" % title]
    for row in range(max(rows), min(rows) - 1, -1):
        line = "".join(
            zbuf.get((col, row), (0, " "))[1]
            for col in range(min(cols), max(cols) + 1))
        out.append("%4d |%s" % (row, line))
    return "\n".join(out)


def measure(bones):
    """Числовая сводка: то, что должно совпасть во всех видах."""
    by = chain(bones)
    core_skip = ("hair", "arm", "sleeve", "bangles", "rushnyk", "sunflower",
                 "earring")
    rows = {}
    all_x = all_y = all_z = None
    tail_axis = {}
    for bone in bones:
        for cube in bone.cubes:
            pts = [to_world(list(p), bone, by) for p in surface_samples(cube)]
            xs = [p[0] for p in pts]
            ys = [p[1] for p in pts]
            zs = [p[2] for p in pts]
            lo = (min(xs), min(ys), min(zs))
            hi = (max(xs), max(ys), max(zs))
            all_x = (min(lo[0], all_x[0]), max(hi[0], all_x[1])) if all_x else (lo[0], hi[0])
            all_y = (min(lo[1], all_y[0]), max(hi[1], all_y[1])) if all_y else (lo[1], hi[1])
            all_z = (min(lo[2], all_z[0]), max(hi[2], all_z[1])) if all_z else (lo[2], hi[2])
            if cube.name.startswith("tail"):
                tail_axis[cube.name] = ((lo[0] + hi[0]) / 2,
                                        (lo[1] + hi[1]) / 2,
                                        (lo[2] + hi[2]) / 2)
            if any(cube.name.startswith(s) for s in core_skip):
                continue
            half = max(abs(lo[0]), abs(hi[0]))
            for y in range(int(math.floor(lo[1])), int(math.ceil(hi[1]))):
                if y not in rows or half > rows[y][0]:
                    rows[y] = (half, cube.name)

    print("\n--- профиль корпуса (без рук, волос, рушника) ---")
    print(" Y   ширина  деталь")
    for y in sorted(rows, reverse=True):
        half, name = rows[y]
        print("%4d  %5.1f  %-11s %s" % (y, half * 2, name, "#" * int(half * 2)))

    print("\nвысота: %.1f px (%.2f блока), от Y=%.1f до Y=%.1f"
          % (all_y[1] - all_y[0], (all_y[1] - all_y[0]) / 16.0,
             all_y[0], all_y[1]))
    print("габарит X: %.1f..%.1f (%.1f px)  Z: %.1f..%.1f (%.1f px)"
          % (all_x[0], all_x[1], all_x[1] - all_x[0],
             all_z[0], all_z[1], all_z[1] - all_z[0]))
    need = max(abs(all_x[0]), abs(all_x[1])) * 2
    # Визуальные габариты в Minecraft штатно шире hitbox: у ванильного игрока
    # 16 px размаха при hitbox 9.6 px (×1.67). Тревожно только заметно больше.
    ratio = need / 12.8
    print("hitbox 0.8 блока = 12.8 px; размах модели %.1f px (×%.2f, "
          "у игрока ×1.67) → %s"
          % (need, ratio, "шире обычного" if ratio > 2.0 else "в норме"))

    print("\n--- ось хвоста (центры сегментов) ---")
    print("  сегмент     X      Y      Z")
    for i in range(1, 8):
        n = "tail%d" % i
        if n in tail_axis:
            x, y, z = tail_axis[n]
            print("  %-8s %6.2f %6.2f %6.2f" % (n, x, y, z))
    xs = [tail_axis["tail%d" % i][0] for i in range(1, 8) if "tail%d" % i in tail_axis]
    zs = [tail_axis["tail%d" % i][2] for i in range(1, 8) if "tail%d" % i in tail_axis]
    print("  разброс по X %.2f px, по Z %.2f px" % (max(xs) - min(xs),
                                                    max(zs) - min(zs)))


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--views", default="front,side,top")
    ap.add_argument("--hscale", type=int, default=2,
                    help="растяжение по горизонтали: символ уже, чем строка")
    args = ap.parse_args()

    bones = gp.build()
    views = {
        # (горизонталь, вертикаль), (ось глубины, знак «к наблюдателю»)
        "front": ((0, 1), (2, -1), "FRONT (взгляд в −Z, +X вправо)"),
        "side": ((2, 1), (0, 1), "SIDE (взгляд по +X, +Z вправо)"),
        "top": ((0, 2), (1, 1), "TOP (взгляд вниз, +X вправо, +Z вниз)"),
    }
    for key in args.views.split(","):
        key = key.strip()
        if key in views:
            axes, depth, title = views[key]
            print(render(bones, axes, depth, title, args.hscale))
            print()
    measure(bones)


if __name__ == "__main__":
    main()
