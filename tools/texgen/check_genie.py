"""Проверка геометрии Кубанской джиннии.

Ловит именно те дефекты, которые не видны в JSON глазами и всплывают только
в игре: z-fighting от пересечения кубоидов разных костей, выход box-UV за
границу атласа, наложение развёрток друг на друга, потеря силуэта
«песочные часы» и разрыв цепочки хвоста.

Запуск: ``python3 tools/texgen/check_genie.py``
Код возврата 1, если есть хотя бы одна ошибка (для CI).
"""
import itertools
import json
import os
import sys

import genie_parts as gp

ROOT = os.path.normpath(os.path.join(os.path.dirname(__file__), "..", ".."))
GEO = os.path.join(ROOT, "src/main/resources/assets/kubanhorizons"
                         "/geckolib/models/kuban_genie.geo.json")
TEX = os.path.join(ROOT, "src/main/resources/assets/kubanhorizons"
                         "/textures/entity/kuban_genie.png")
GLOW = os.path.join(ROOT, "src/main/resources/assets/kubanhorizons"
                          "/textures/entity/kuban_genie_glowmask.png")
ANIM = os.path.join(ROOT, "src/main/resources/assets/kubanhorizons"
                          "/geckolib/animations/kuban_genie.animation.json")

# Кости, которым пересекаться друг с другом можно и нужно: одежда сидит на
# теле, волосы лежат на голове. Пересечение внутри одной кости — тоже норма.
ALLOWED_PAIRS = {
    frozenset({"torso", "head"}),
    frozenset({"torso", "hair_mass"}),
    frozenset({"head", "hair_mass"}),
    frozenset({"head", "earring_l"}),
    frozenset({"head", "earring_r"}),
    frozenset({"hair_mass", "hair_tips"}),
    frozenset({"torso", "hips"}),
    frozenset({"hips", "belt"}),
    frozenset({"hips", "tail1"}),
    frozenset({"belt", "tail1"}),
    frozenset({"belt", "pendant_l"}),
    frozenset({"belt", "pendant_c"}),
    frozenset({"belt", "pendant_r"}),
    frozenset({"belt", "rushnyk"}),
    frozenset({"rushnyk", "rushnyk_mid"}),
    frozenset({"rushnyk_mid", "rushnyk_tip"}),
    frozenset({"torso", "arm_l"}),  # рукав лифа — только по касательной
    frozenset({"torso", "arm_r"}),
}
# Сегменты хвоста заходят друг в друга на 1 px намеренно.
for _i in range(1, 7):
    ALLOWED_PAIRS.add(frozenset({"tail%d" % _i, "tail%d" % (_i + 1)}))

EPS = 0.05  # допуск: касание грань-в-грань пересечением не считается


def overlap_volume(a, b):
    v = 1.0
    for k in range(3):
        d = min(a.hi()[k], b.hi()[k]) - max(a.lo()[k], b.lo()[k])
        if d <= EPS:
            return 0.0
        v *= d
    return v


def check_intersections(bones, errors, warnings):
    owner = []
    for bone in bones:
        for cube in bone.cubes:
            owner.append((bone.name, cube))
    for (bn_a, ca), (bn_b, cb) in itertools.combinations(owner, 2):
        if bn_a == bn_b:
            continue
        if frozenset({bn_a, bn_b}) in ALLOWED_PAIRS:
            continue
        v = overlap_volume(ca, cb)
        if v > 0.5:
            errors.append(
                "пересечение костей: %s/%s и %s/%s, объём %.2f px³"
                % (bn_a, ca.name, bn_b, cb.name, v))
        elif v > 0:
            warnings.append(
                "касание с перекрытием: %s/%s и %s/%s (%.2f px³)"
                % (bn_a, ca.name, bn_b, cb.name, v))


def check_uv(bones, errors):
    mask = {}
    for bone in bones:
        for cube in bone.cubes:
            if cube.uv is None:
                errors.append("нет UV: %s/%s" % (bone.name, cube.name))
                continue
            w, h = cube.footprint()
            w = int(-(-w // 1))
            h = int(-(-h // 1))
            x, y = int(cube.uv[0]), int(cube.uv[1])
            if x + w > gp_atlas() or y + h > gp_atlas():
                errors.append(
                    "UV за границей атласа: %s/%s uv=%s footprint=%dx%d"
                    % (bone.name, cube.name, cube.uv, w, h))
                continue
            for yy in range(y, y + h):
                for xx in range(x, x + w):
                    prev = mask.get((xx, yy))
                    if prev:
                        errors.append(
                            "UV-наложение в (%d,%d): %s/%s и %s/%s"
                            % (xx, yy, prev[0], prev[1], bone.name, cube.name))
                        return
                    mask[(xx, yy)] = (bone.name, cube.name)


def gp_atlas():
    import gen_genie_model
    return gen_genie_model.ATLAS


def check_silhouette(bones, errors, warnings):
    """Песочные часы: талия должна быть заметно уже груди и таза."""
    by = gp.bones_by_name(bones)

    def width(bone, cube_name):
        for c in by[bone].cubes:
            if c.name == cube_name:
                return c.size[0] + 2 * c.inflate
        errors.append("не найден кубоид %s/%s" % (bone, cube_name))
        return 0.0

    bust = width("torso", "chest")
    waist = width("torso", "waist")
    hip = width("hips", "hips")
    tail = width("tail1", "tail1")

    if not waist < bust - 2.0:
        errors.append("талия (%.1f) недостаточно уже груди (%.1f)" % (waist, bust))
    if not waist < hip - 2.0:
        errors.append("талия (%.1f) недостаточно уже таза (%.1f)" % (waist, hip))
    if tail >= hip:
        errors.append(
            "основание хвоста (%.1f) не уже таза (%.1f): пояс не перекроет стык"
            % (tail, hip))
    ratio = waist / bust
    if ratio > 0.75:
        warnings.append("силуэт слабо выражен: талия/грудь = %.2f" % ratio)


def check_tail(bones, errors, warnings):
    by = gp.bones_by_name(bones)
    names = ["tail%d" % i for i in range(1, 8)]
    prev_w = prev_d = prev_h = None
    for i, n in enumerate(names):
        if n not in by:
            errors.append("нет кости %s" % n)
            return
        b = by[n]
        c = b.cubes[0]
        w, h, d = c.size
        # Центрирование по X/Z: изгиб задаётся только rotation.
        cx = c.pos[0] + w / 2
        cz = c.pos[2] + d / 2
        if abs(cx) > 0.01:
            errors.append("%s не центрирован по X (центр %.2f)" % (n, cx))
        if abs(cz) > 0.01:
            errors.append("%s не центрирован по Z (центр %.2f)" % (n, cz))
        if abs(b.pivot[0]) > 0.01 or abs(b.pivot[2]) > 0.01:
            errors.append("пивот %s сдвинут по X/Z: %s" % (n, b.pivot))
        if prev_w is not None:
            if w >= prev_w or d >= prev_d:
                errors.append("%s не сужается: %.1f×%.1f после %.1f×%.1f"
                              % (n, w, d, prev_w, prev_d))
            # Заход в родителя: верх сегмента должен быть выше пивота ребёнка.
            gap = by[names[i - 1]].pivot[1] - (by[names[i - 1]].cubes[0].pos[1]
                                              + by[names[i - 1]].cubes[0].size[1])
            if gap > 0.01:
                warnings.append("щель у %s: %.2f px" % (names[i - 1], gap))
        prev_w, prev_d, prev_h = w, d, h

    rots_z = [by[n].rotation[2] for n in names]
    rots_x = [by[n].rotation[0] for n in names]
    # Хвост обязан читаться S-образным и спереди (Z), и сбоку (X). Проверяем
    # накопленный угол: повороты вложенных костей складываются, и знак
    # суммы — это реальный наклон сегмента в мире.
    for axis, rots in (("Z (вид спереди)", rots_z), ("X (вид сбоку)", rots_x)):
        acc, signs = 0.0, []
        for r in rots:
            acc += r
            signs.append(1 if acc > 0 else (-1 if acc < 0 else 0))
        nz = [s for s in signs if s]
        if len(set(nz)) < 2:
            errors.append(
                "хвост не S-образный по оси %s: накопленный угол не меняет знак"
                % axis)
        flips = sum(1 for a, b in zip(nz, nz[1:]) if a != b)
        if flips != 1:
            warnings.append("перегибов по оси %s: %d (ожидался 1)"
                            % (axis, flips))
    if abs(rots_z[-1]) <= abs(rots_z[-2]) and abs(rots_x[-1]) <= abs(rots_x[-2]):
        warnings.append("кончик не загибается крючком: Z %s, X %s"
                        % (rots_z[-2:], rots_x[-2:]))


def check_hair_parent(bones, errors):
    by = gp.bones_by_name(bones)
    if "hair_mass" not in by:
        errors.append("нет кости hair_mass")
        return
    if by["hair_mass"].parent != "torso":
        errors.append(
            "hair_mass висит на %s: при повороте головы волосы прошьют спину"
            % by["hair_mass"].parent)


def check_exported(bones, errors):
    """Файл на диске обязан совпадать с генератором."""
    if not os.path.exists(GEO):
        errors.append("нет файла модели: %s" % GEO)
        return
    with open(GEO, encoding="utf-8") as fh:
        disk = json.load(fh)
    import gen_genie_model
    fresh = gen_genie_model.to_geo(bones)
    if disk != fresh:
        errors.append(
            "kuban_genie.geo.json разошёлся с генератором — "
            "перезапустите gen_genie_model.py")


def check_textures(bones, errors, warnings):
    try:
        from PIL import Image
    except ImportError:
        warnings.append("PIL недоступен, текстуры не проверены")
        return
    for path, label in ((TEX, "текстура"), (GLOW, "glowmask")):
        if not os.path.exists(path):
            errors.append("нет файла: %s (%s)" % (path, label))
            continue
        im = Image.open(path)
        if im.size != (gp_atlas(), gp_atlas()):
            errors.append("%s имеет размер %s, ожидался %dx%d"
                          % (label, im.size, gp_atlas(), gp_atlas()))
    if not os.path.exists(TEX):
        return
    im = Image.open(TEX).convert("RGBA")
    px = im.load()
    # Проверяем ГРАНИ, а не весь footprint: box-UV оставляет по краям
    # развёртки служебные полосы, которые красить не нужно, и измерение
    # прямоугольника целиком давало бы ложные «недокрашено».
    # Часть граней прозрачна намеренно: линзы очков, промежутки между
    # бусинами, дизеринг на растворяющемся кончике хвоста.
    intentional = {"glasses", "necklace", "navel_gem", "tail6", "tail7",
                   "belt_gem_c", "belt_gem_l", "belt_gem_r",
                   "tiara_gem_c", "tiara_gem_l", "tiara_gem_r"}
    for bone in bones:
        for cube in bone.cubes:
            if cube.uv is None or cube.name in intentional:
                continue
            w = max(1, int(-(-cube.size[0] // 1)))
            h = max(1, int(-(-cube.size[1] // 1)))
            d = max(1, int(-(-cube.size[2] // 1)))
            x, y = int(cube.uv[0]), int(cube.uv[1])
            faces = {
                "up": (x + d, y, w, d),
                "down": (x + d + w, y, w, d),
                "east": (x, y + d, d, h),
                "north": (x + d, y + d, w, h),
                "west": (x + d + w, y + d, d, h),
                "south": (x + 2 * d + w, y + d, w, h),
            }
            for key, (fx, fy, fw, fh) in faces.items():
                total = filled = 0
                for yy in range(fy, min(fy + fh, im.size[1])):
                    for xx in range(fx, min(fx + fw, im.size[0])):
                        total += 1
                        if px[xx, yy][3] > 8:
                            filled += 1
                if total and filled / total < 0.9:
                    warnings.append(
                        "грань %s кубоида %s/%s закрашена на %d%%"
                        % (key, bone.name, cube.name,
                           round(100 * filled / total)))


def check_animation_bones(bones, errors):
    """Анимации не должны ссылаться на несуществующие кости."""
    if not os.path.exists(ANIM):
        errors.append("нет файла анимаций: %s" % ANIM)
        return
    with open(ANIM, encoding="utf-8") as fh:
        data = json.load(fh)
    known = {b.name for b in bones}
    for anim_name, anim in data.get("animations", {}).items():
        for bone_name in anim.get("bones", {}):
            if bone_name not in known:
                errors.append("%s крутит несуществующую кость %s"
                              % (anim_name, bone_name))


def check_tail_colors(bones, errors, warnings):
    """Семь цветов хвоста заданы заказчиком и должны совпадать побайтово.

    Проверяется доминирующий цвет лицевой грани: струи дыма и дизеринг не
    имеют права перебить базовый оттенок, иначе градиент читается неверно.
    """
    try:
        from PIL import Image
    except ImportError:
        return
    if not os.path.exists(TEX):
        return
    im = Image.open(TEX).convert("RGBA")
    px = im.load()
    by = gp.bones_by_name(bones)
    for i, want in enumerate(gp.TAIL_COLORS):
        name = "tail%d" % (i + 1)
        if name not in by:
            continue
        cube = by[name].cubes[0]
        w = int(-(-cube.size[0] // 1))
        h = int(-(-cube.size[1] // 1))
        d = int(-(-cube.size[2] // 1))
        x, y = int(cube.uv[0]), int(cube.uv[1])
        counts = {}
        for yy in range(y + d, min(y + d + h, im.size[1])):
            for xx in range(x + d, min(x + d + w, im.size[0])):
                p = px[xx, yy]
                if p[3] > 8:
                    counts[p[:3]] = counts.get(p[:3], 0) + 1
        if not counts:
            errors.append("%s не закрашен на лицевой грани" % name)
            continue
        top = max(counts, key=counts.get)
        if top != want[:3]:
            errors.append(
                "%s: доминирующий цвет #%02x%02x%02x, требуется #%02x%02x%02x"
                % (name, top[0], top[1], top[2], want[0], want[1], want[2]))


def check_palette(bones, errors, warnings):
    """Палитра ограничена: разъезд оттенков ломает единство стиля мода."""
    try:
        from PIL import Image
    except ImportError:
        return
    if not os.path.exists(TEX):
        return
    im = Image.open(TEX).convert("RGBA")
    colors = {p[:3] for p in im.get_flattened_data() if p[3] > 8}
    if len(colors) > 80:
        warnings.append("в текстуре %d цветов — палитра расползается"
                        % len(colors))


def main():
    bones = gp.build()
    import gen_genie_model
    gen_genie_model.pack(bones)

    errors, warnings = [], []
    check_hair_parent(bones, errors)
    check_intersections(bones, errors, warnings)
    check_uv(bones, errors)
    check_silhouette(bones, errors, warnings)
    check_tail(bones, errors, warnings)
    check_exported(bones, errors)
    check_textures(bones, errors, warnings)
    check_tail_colors(bones, errors, warnings)
    check_palette(bones, errors, warnings)
    check_animation_bones(bones, errors)

    for w in warnings:
        print("  ⚠ %s" % w)
    for e in errors:
        print("  ✗ %s" % e)
    print("\nпроверено: костей %d, кубоидов %d"
          % (len(bones), sum(len(b.cubes) for b in bones)))
    print("ошибок %d, предупреждений %d" % (len(errors), len(warnings)))
    return 1 if errors else 0


if __name__ == "__main__":
    sys.exit(main())
