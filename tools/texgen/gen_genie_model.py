"""Экспорт геометрии Кубанской джиннии в GeckoLib .geo.json.

Кубоиды описаны в ``genie_parts``; здесь только упаковка box-UV в атлас
128×128 и запись JSON. Упаковщик детерминирован (сортировка по площади,
затем по имени), поэтому повторный запуск даёт побайтово тот же файл —
иначе каждый прогон плодил бы шум в diff.

Box-UV в Minecraft раскладывает кубоид W×H×D так:

    (D, 0)      up      W×D
    (D+W, 0)    down    W×D
    (0, D)      east    D×H
    (D, D)      north   W×H   ← лицо, смотрит в -Z
    (D+W, D)    west    D×H
    (2D+W, D)   south   W×H

Общий footprint: (2D+2W) × (D+H). Раскладка проверена на существующей
текстуре головы, а не взята из документации.
"""
import json
import os

import genie_parts as gp

ATLAS = 128

OUT_MODEL = os.path.join(
    os.path.dirname(__file__), "..", "..",
    "src/main/resources/assets/kubanhorizons/geckolib/models/kuban_genie.geo.json")


def pack(bones, atlas=ATLAS):
    """Раскладывает кубоиды по атласу и проставляет им ``uv``.

    Простой shelf-упаковщик: крупное вперёд, каждый кубоид кладётся в первую
    свободную позицию по сетке 1 px. Пересечения физически невозможны —
    занятые ячейки помечаются в маске.
    """
    cubes = []
    for b in bones:
        for c in b.cubes:
            w, h = c.footprint()
            # Округляем вверх: дробный кубоид всё равно занимает целый пиксель.
            cubes.append((int(-(-w // 1)), int(-(-h // 1)), c))
    # Детерминированный порядок: сначала площадь, потом высота, потом имя.
    cubes.sort(key=lambda t: (-t[0] * t[1], -t[1], t[2].name))

    used = bytearray(atlas * atlas)

    def fits(x, y, w, h):
        if x + w > atlas or y + h > atlas:
            return False
        for yy in range(y, y + h):
            row = yy * atlas
            for xx in range(x, x + w):
                if used[row + xx]:
                    return False
        return True

    def occupy(x, y, w, h):
        for yy in range(y, y + h):
            row = yy * atlas
            for xx in range(x, x + w):
                used[row + xx] = 1

    for w, h, c in cubes:
        placed = False
        for y in range(atlas - h + 1):
            for x in range(atlas - w + 1):
                if fits(x, y, w, h):
                    occupy(x, y, w, h)
                    c.uv = [x, y]
                    placed = True
                    break
            if placed:
                break
        if not placed:
            raise SystemExit(
                "атлас %d×%d переполнен на кубоиде %s (%d×%d)"
                % (atlas, atlas, c.name, w, h))
    return used


def _num(v):
    """Целые пишем целыми: '8' вместо '8.0' — так geo-файлы читаемее."""
    return int(v) if float(v).is_integer() else round(float(v), 3)


def to_geo(bones):
    out_bones = []
    for b in bones:
        entry = {"name": b.name, "pivot": [_num(v) for v in b.pivot]}
        if b.parent:
            entry["parent"] = b.parent
        if b.rotation and any(b.rotation):
            entry["rotation"] = [_num(v) for v in b.rotation]
        if b.cubes:
            cubes = []
            for c in b.cubes:
                cube = {
                    "origin": [_num(v) for v in c.pos],
                    "size": [_num(v) for v in c.size],
                    "uv": [_num(v) for v in c.uv],
                }
                if c.inflate:
                    cube["inflate"] = _num(c.inflate)
                if c.mirror:
                    cube["mirror"] = True
                cubes.append(cube)
            entry["cubes"] = cubes
        out_bones.append(entry)

    return {
        "format_version": "1.12.0",
        "minecraft:geometry": [{
            "description": {
                "identifier": "geometry.kuban_genie",
                "texture_width": ATLAS,
                "texture_height": ATLAS,
                "visible_bounds_width": 3,
                "visible_bounds_height": 4,
                "visible_bounds_offset": [0, 1, 0],
            },
            "bones": out_bones,
        }],
    }


def main():
    bones = gp.build()
    used = pack(bones)
    geo = to_geo(bones)
    path = os.path.normpath(OUT_MODEL)
    with open(path, "w", encoding="utf-8") as fh:
        json.dump(geo, fh, indent=2, ensure_ascii=False)
        fh.write("\n")
    fill = 100.0 * sum(used) / (ATLAS * ATLAS)
    print("модель: %s" % os.path.relpath(path, os.path.join(
        os.path.dirname(__file__), "..", "..")))
    print("  костей %d, кубоидов %d, атлас занят %.1f%%"
          % (len(bones), sum(len(b.cubes) for b in bones), fill))


if __name__ == "__main__":
    main()
