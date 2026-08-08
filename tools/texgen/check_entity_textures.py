"""Аудит текстур сущностей: контраст, палитра, заполнение, ASCII-рендер.

Проверяет то, что нельзя увидеть глазом в редакторе вплотную (ART_BIBLE §7.3 —
решения по контрасту принимаются на дистанции):

  * число оттенков — не более ~10 на текстуру (ART_BIBLE §2);
  * разброс яркости — узкий диапазон читается как плоская заливка;
  * доля заполнения — сколько развёртки реально занято;
  * ASCII-рендер — силуэт и внутренняя структура на глаз.

Запуск:
    python3 check_entity_textures.py                 # текущее состояние
    python3 check_entity_textures.py --before DIR    # сравнение с бэкапом
"""
import argparse
import os
import sys

from PIL import Image

ENTITY_DIR = os.path.join(os.path.dirname(__file__), "..", "..",
                          "src/main/resources/assets/kubanhorizons/textures/entity")

# Текстуры мода (глиняный глечик и джинния рисуются вручную — не наши).
TARGETS = ["locust", "caucasian_bee", "gull", "heron", "sturgeon",
           "wild_boar", "caucasian_shepherd", "nutria", "pheasant", "quail"]

# ART_BIBLE §2 говорит про 8-10 оттенков. Глаз, блик и клюв — точечные акценты
# по 1-2 px, они формально раздувают счётчик: у фазана, принятого за эталон, их
# 14. Поэтому проверяем «структурные» оттенки — те, что занимают заметную долю
# площади и реально формируют объём, а общее число держим на уровне фазана.
MAX_STRUCTURAL = 10      # оттенки с долей >= ACCENT_CUTOFF
MAX_TOTAL = 15           # общий предел с учётом точечных акцентов
ACCENT_CUTOFF = 1.5      # % от заполненной площади: ниже — точечный акцент
DOMINANT_CUTOFF = 3.0    # % — тона, определяющие читаемость на дистанции
RAMP = " .:-=+*#%@"      # яркость -> символ


def luma(c):
    return 0.299 * c[0] + 0.587 * c[1] + 0.114 * c[2]


def stats(path):
    im = Image.open(path).convert("RGBA")
    w, h = im.size
    solid = [p for p in im.get_flattened_data() if p[3] > 0]
    shades = {}
    for p in solid:
        shades[p] = shades.get(p, 0) + 1
    n = len(solid) or 1
    lumas = [luma(p) for p in solid] or [0]

    structural = {c: k for c, k in shades.items()
                  if 100.0 * k / n >= ACCENT_CUTOFF}
    dominant = [luma(c) for c, k in shades.items()
                if 100.0 * k / n >= DOMINANT_CUTOFF] or [0]

    return {
        "size": (w, h),
        "filled": len(solid),
        "total": w * h,
        "fill_pct": 100.0 * len(solid) / (w * h),
        "shades": len(shades),
        "structural": len(structural),
        "hist": shades,
        "lmin": min(lumas),
        "lmax": max(lumas),
        "spread": max(lumas) - min(lumas),
        # Главная метрика: разброс тонов, которые действительно видно.
        "dom_min": min(dominant),
        "dom_max": max(dominant),
        "dom_spread": max(dominant) - min(dominant),
        "image": im,
    }


def ascii_render(im, scale=1):
    """Яркость -> символы. Прозрачное — точка-разделитель, чтобы виден силуэт."""
    w, h = im.size
    out = []
    for y in range(0, h, scale):
        row = []
        for x in range(0, w, scale):
            p = im.getpixel((x, y))
            if p[3] == 0:
                row.append("·")
            else:
                idx = int(luma(p) / 256.0 * len(RAMP))
                row.append(RAMP[min(idx, len(RAMP) - 1)])
        out.append("".join(row))
    return out


def region_ascii(im, x0, y0, x1, y1):
    return ascii_render(im.crop((x0, y0, x1 + 1, y1 + 1)))


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--before", help="каталог с текстурами до правок")
    ap.add_argument("--only", nargs="*", help="ограничить список текстур")
    ap.add_argument("--ascii", action="store_true", help="печатать ASCII-рендер")
    ap.add_argument("--regions", nargs="*", default=[],
                    help="крупный план: name:x0,y0,x1,y1")
    args = ap.parse_args()

    names = args.only or TARGETS
    failures = []

    print("=" * 90)
    print(f"{'текстура':<22}{'размер':>9}{'оттенк':>8}{'структ':>8}"
          f"{'заполн':>9}{'осн.тона':>12}{'разброс':>9}")
    print("=" * 90)

    for name in names:
        cur = os.path.join(ENTITY_DIR, name + ".png")
        if not os.path.exists(cur):
            print(f"{name:<22} ОТСУТСТВУЕТ")
            failures.append(f"{name}: файл не найден")
            continue
        s = stats(cur)
        print(f"{name:<22}{s['size'][0]}x{s['size'][1]:<5}"
              f"{s['shades']:>8}{s['structural']:>8}{s['fill_pct']:>8.1f}%"
              f"{int(s['dom_min']):>8}..{int(s['dom_max']):<4}"
              f"{int(s['dom_spread']):>7}")

        if args.before:
            b = os.path.join(args.before, name + ".png")
            if os.path.exists(b):
                sb = stats(b)
                print(f"{'  было':<22}{sb['size'][0]}x{sb['size'][1]:<5}"
                      f"{sb['shades']:>8}{sb['structural']:>8}"
                      f"{sb['fill_pct']:>8.1f}%"
                      f"{int(sb['dom_min']):>8}..{int(sb['dom_max']):<4}"
                      f"{int(sb['dom_spread']):>7}")
                print(f"{'  дельта':<22}{'':>14}{s['shades'] - sb['shades']:>+8}"
                      f"{s['structural'] - sb['structural']:>+8}"
                      f"{s['fill_pct'] - sb['fill_pct']:>+8.1f}%{'':>14}"
                      f"{s['dom_spread'] - sb['dom_spread']:>+7}")
                if s["size"] != sb["size"]:
                    failures.append(f"{name}: размер развёртки изменился "
                                    f"{sb['size']} -> {s['size']}")

        if s["structural"] > MAX_STRUCTURAL:
            failures.append(f"{name}: {s['structural']} структурных оттенков > "
                            f"{MAX_STRUCTURAL} (ART_BIBLE §2)")
        if s["shades"] > MAX_TOTAL:
            failures.append(f"{name}: {s['shades']} оттенков всего > {MAX_TOTAL}")
        for c in s["hist"]:
            if c[3] > 0 and (c[:3] == (0, 0, 0) or c[:3] == (255, 255, 255)):
                failures.append(f"{name}: чистый чёрный/белый {c[:3]} "
                                f"(ART_BIBLE §8)")

    print("=" * 90)

    if args.ascii:
        for name in names:
            cur = os.path.join(ENTITY_DIR, name + ".png")
            if not os.path.exists(cur):
                continue
            s = stats(cur)
            print(f"\n--- {name} ({s['size'][0]}x{s['size'][1]}, "
                  f"{s['shades']} оттенков, разброс {int(s['spread'])}) ---")
            for row in ascii_render(s["image"]):
                print(row)

    for spec in args.regions:
        name, box = spec.split(":")
        x0, y0, x1, y1 = (int(v) for v in box.split(","))
        im = Image.open(os.path.join(ENTITY_DIR, name + ".png")).convert("RGBA")
        print(f"\n--- {name} крупный план [{x0},{y0}..{x1},{y1}] ---")
        for row in region_ascii(im, x0, y0, x1, y1):
            print(row)

    if failures:
        print("\nНАРУШЕНИЯ:")
        for f in failures:
            print("  ! " + f)
        return 1
    print("\nВсе проверки пройдены.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
