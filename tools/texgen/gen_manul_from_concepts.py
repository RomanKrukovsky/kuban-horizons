"""Reconstruct an editable voxel manul directly from the four concept images."""

from __future__ import annotations

import sys
import json
import base64
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import gen_manul_blockbench as bb


ROOT = Path(__file__).resolve().parents[2]
GRID_W = 40
GRID_H = 50
GRID_D = 48
CELL = 1.0
CACHE = ROOT / "tools/texgen/manul_concept_cache.json"


def grab_mask(path: Path, rect: tuple[int, int, int, int]):
    import cv2
    import numpy as np

    image = cv2.imread(str(path), cv2.IMREAD_COLOR)
    mask = np.zeros(image.shape[:2], np.uint8)
    background = np.zeros((1, 65), np.float64)
    foreground = np.zeros((1, 65), np.float64)
    cv2.grabCut(image, mask, rect, background, foreground, 8, cv2.GC_INIT_WITH_RECT)
    result = np.where((mask == cv2.GC_FGD) | (mask == cv2.GC_PR_FGD), 255, 0).astype(np.uint8)
    result = cv2.morphologyEx(result, cv2.MORPH_CLOSE, np.ones((11, 11), np.uint8))
    return cv2.cvtColor(image, cv2.COLOR_BGR2RGB), result


def crop_grid(image, mask, crop, width, height):
    import cv2

    x0, y0, x1, y1 = crop
    color = cv2.resize(image[y0:y1, x0:x1], (width, height), interpolation=cv2.INTER_AREA)
    binary = cv2.resize(mask[y0:y1, x0:x1], (width, height), interpolation=cv2.INTER_NEAREST) > 0
    return color, binary


def build_palette(images, count=44):
    import numpy as np
    from PIL import Image

    pixels = np.concatenate([image.reshape(-1, 3) for image in images], axis=0)
    sample = Image.fromarray(pixels.reshape(-1, 1, 3).astype(np.uint8))
    quantized = sample.quantize(colors=count, method=Image.Quantize.MEDIANCUT)
    palette = quantized.getpalette()[:count * 3]
    colors = [tuple(palette[i:i + 3]) + (255,) for i in range(0, len(palette), 3)]
    for index, color in enumerate(colors):
        bb.COLORS[f"concept_{index:02}"] = color
    return np.array([color[:3] for color in colors], dtype=np.int16)


def nearest_material(rgb, palette):
    import numpy as np

    index = int(np.square(palette - np.array(rgb, dtype=np.int16)).sum(axis=1).argmin())
    return f"concept_{index:02}"


def group_for_front(ix, iy, rgb):
    r, g, b = (int(value) for value in rgb)
    if r > g * 1.25 and r > b * 1.35 and iy > 27:
        return "chest_cloth"
    if iy < 9:
        return "forehead_fur"
    if iy < 22:
        if ix < 8:
            return "left_ear"
        if ix > GRID_W - 9:
            return "right_ear"
        if 8 < ix < 18:
            return "left_eye"
        if GRID_W - 18 < ix < GRID_W - 8:
            return "right_eye"
        return "head"
    if iy < 31:
        if ix < GRID_W // 2 - 2:
            return "left_cheek"
        if ix > GRID_W // 2 + 2:
            return "right_cheek"
        return "muzzle"
    if iy < 37:
        return "chin"
    if iy > 42 and ix < 15:
        return "front_leg_L"
    if iy > 42 and ix > GRID_W - 16:
        return "front_leg_R"
    return "chest_fur"


def build_visual_hull():
    import numpy as np

    front_image, front_mask = grab_mask(ROOT / "MANUL_4.png", (130, 100, 1020, 1030))
    side_image, side_mask = grab_mask(ROOT / "MANUL_2.png", (40, 150, 1120, 890))
    back_image, back_mask = grab_mask(ROOT / "MANUL_3.png", (250, 130, 730, 980))

    front, fm = crop_grid(front_image, front_mask, (250, 115, 1010, 1045), GRID_W, GRID_H)
    side, sm = crop_grid(side_image, side_mask, (70, 165, 1040, 1020), GRID_D, GRID_H)
    back, bm = crop_grid(back_image, back_mask, (270, 145, 985, 1050), GRID_W, GRID_H)
    palette = build_palette([front[fm], side[sm], back[bm]])

    bb.parts.clear()
    for iy in range(GRID_H):
        valid_x = np.flatnonzero(fm[iy])
        valid_z = np.flatnonzero(sm[iy])
        if not len(valid_x) or not len(valid_z):
            continue
        min_x, max_x = int(valid_x.min()), int(valid_x.max())
        min_z, max_z = int(valid_z.min()), int(valid_z.max())
        for ix in valid_x:
            for iz in valid_z:
                is_front = iz == min_z
                is_back = iz == max_z
                is_side = ix in (min_x, max_x)
                is_step = (ix + iy + iz) % 7 == 0 and (iz <= min_z + 1 or iz >= max_z - 1)
                if not (is_front or is_back or is_side or is_step):
                    continue

                if is_front:
                    rgb = front[iy, ix]
                    group = group_for_front(int(ix), iy, rgb)
                elif is_back:
                    bx = min(GRID_W - 1, int(round(ix / max(1, GRID_W - 1) * (GRID_W - 1))))
                    rgb = back[iy, GRID_W - 1 - bx]
                    group = "body"
                else:
                    rgb = side[iy, iz]
                    group = "body" if iy < 42 else ("rear_leg_L" if ix < GRID_W // 2 else "rear_leg_R")

                # Suppress the detached tail from the front projection. The tail is
                # reconstructed by the side/back projections where its attachment is measurable.
                if is_front and iy > 31 and ix < 7:
                    continue
                x = (float(ix) - (GRID_W - 1) / 2) * CELL
                y = (GRID_H - 1 - iy) * CELL
                z = (float(iz) - (GRID_D - 1) / 2) * CELL
                name = f"concept_{group}_{iy:02}_{int(ix):02}_{int(iz):02}"
                bb.add(name, group, (x, y, z), (1.05, 1.05, 1.05), nearest_material(rgb, palette))

    # Restore small high-contrast facial features lost by the coarse visual-hull grid.
    for side_name, sign in (("left", -1), ("right", 1)):
        eye_group = f"{side_name}_eye"
        x = sign * 4.8
        bb.add(f"{side_name}_concept_eye_cream", eye_group, (x, 34.7, -23.85), (4.5, 3.3, .35), "fur_cream")
        bb.add(f"{side_name}_concept_eye_outline", eye_group, (x, 34.7, -24.12), (3.9, 2.8, .3), "fur_dark")
        bb.add(f"{side_name}_concept_iris", eye_group, (x, 34.6, -24.35), (2.9, 2.35, .25), "eye")
        bb.add(f"{side_name}_concept_pupil", eye_group, (x, 34.6, -24.52), (.55, 2.25, .18), "pupil")
        bb.add(f"{side_name}_concept_lid", "forehead_fur", (x - sign * .25, 36.5, -24.55),
               (5.2, .9, .25), "fur_dark", rotation=(0, 0, sign * 9))
    bb.add("concept_nose", "nose", (0, 30.4, -24.45), (2.6, 1.6, .5), "nose", pivot=(0, 31, -24))
    bb.add("concept_nose_drop", "nose", (0, 29.35, -24.55), (.8, 1.1, .4), "nose")

    # Crisp separate textile geometry restores the ornament lost by palette quantization.
    bb.add("concept_cloth_border", "chest_cloth", (0, 16.7, -24.2), (11.0, 10.5, .4), "cloth_dark",
           pivot=(0, 22, -24), texture=1, uv=(0, 0, 64, 64))
    for index, (x, y, w, h, angle) in enumerate((
        (0, 17.0, .8, 6.6, 0), (0, 17.0, 6.2, .8, 0),
        (-2.3, 19.3, 3.3, .7, 32), (2.3, 19.3, 3.3, .7, -32),
        (-2.3, 14.7, 3.3, .7, -32), (2.3, 14.7, 3.3, .7, 32),
        (-3.8, 17.0, .7, 4.7, 0), (3.8, 17.0, .7, 4.7, 0))):
        bb.add(f"concept_cloth_ornament_{index}", "chest_cloth", (x, y, -24.65), (w, h, .18),
               "cloth_cream", rotation=(0, 0, angle))
    for index, x in enumerate((-4.4, -3.0, -1.5, 0, 1.5, 3.0, 4.4)):
        bb.add(f"concept_fringe_{index}", "cloth_fringe", (x, 10.5 - index % 2 * .25, -24.3),
               (.65, 2.4 + index % 2 * .5, .45), "cloth_red", pivot=(x, 12, -24))

    # Secondary diagonal stitches turn the central cross into the reference's
    # dense Slavic/Kuban floral geometry instead of a generic symbol.
    for index, (x, y, angle) in enumerate((
        (-2.2, 17.2, 45), (2.2, 17.2, -45), (-2.2, 14.6, -45), (2.2, 14.6, 45),
        (-3.6, 20.0, -35), (3.6, 20.0, 35), (-3.6, 13.2, 35), (3.6, 13.2, -35))):
        bb.add(f"concept_cloth_stitch_{index}", "chest_cloth", (x, y, -24.7),
               (2.8, .55, .16), "cloth_cream", rotation=(0, 0, angle))

    for part in bb.parts:
        if part.name.startswith(("concept_cloth_", "concept_fringe_")):
            x, y, z = part.xyz
            part.xyz = (x, y, z + 1.8)
            if part.pivot:
                px, py, pz = part.pivot
                part.pivot = (px, py, pz + 1.8)

    # Explicit animated pivots and thin whiskers remain true geometry.
    for side_name, sign in (("left", -1), ("right", 1)):
        for index in range(7):
            bb.add(f"{side_name}_concept_whisker_{index}", f"{side_name}_whiskers",
                   (sign * (12 + index * .75), 22 - index * .7, -25.0),
                   (10 + index * .5, .18, .18), "whisker", rotation=(0, 0, sign * (-5 - index * 2)),
                   pivot=(sign * 8, 23, -24))

    # The tail changes screen position radically between views, so orthographic
    # silhouette intersection underestimates it. Build its measured rearward curve
    # along Z (not X), keeping the tip offset left in the hero/front camera.
    tail_path = (
        (7.0, 8.2, 18.5, 5.1), (5.0, 7.5, 22.3, 5.2),
        (2.2, 6.7, 26.1, 5.1), (-.8, 6.0, 29.9, 4.9),
        (-3.8, 5.5, 33.5, 4.6), (-6.5, 5.2, 36.7, 4.2),
    )
    tail_step = 1.3
    x_values = np.arange(-12.0, 13.0, tail_step)
    y_values = np.arange(.5, 14.5, tail_step)
    z_values = np.arange(13.0, 42.0, tail_step)

    def tail_inside(x, y, z):
        return any((x - cx) ** 2 + (y - cy) ** 2 + (z - cz) ** 2 <= radius ** 2
                   for cx, cy, cz, radius in tail_path)

    tail_index = 0
    for x in x_values:
        for y in y_values:
            for z in z_values:
                if not tail_inside(x, y, z):
                    continue
                if all(tail_inside(x + dx, y + dy, z + dz) for dx, dy, dz in (
                    (tail_step, 0, 0), (-tail_step, 0, 0), (0, tail_step, 0),
                    (0, -tail_step, 0), (0, 0, tail_step), (0, 0, -tail_step))):
                    continue
                nearest = min(range(len(tail_path)), key=lambda i: (z - tail_path[i][2]) ** 2)
                material = ("fur_base", "fur_light", "fur_dark", "fur_light", "fur_dark", "fur_dark")[nearest]
                group = "tail_root" if nearest < 2 else ("tail_mid" if nearest < 5 else "tail_tip")
                bb.add(f"concept_tail_voxel_{tail_index:04}", group, (float(x), float(y), float(z)),
                       (tail_step, tail_step, tail_step), material,
                       pivot=tail_path[max(0, nearest - 1)][:3])
                tail_index += 1


def save_cache():
    CACHE.write_text(json.dumps({
        "colors": bb.COLORS,
        "parts": [{
            "name": part.name, "group": part.group, "xyz": part.xyz, "size": part.size,
            "material": part.material, "rotation": part.rotation, "pivot": part.pivot,
            "texture": part.texture, "uv": part.uv,
        } for part in bb.parts],
    }, separators=(",", ":")), encoding="utf-8")


def load_cache():
    data = json.loads(CACHE.read_text(encoding="utf-8"))
    bb.COLORS.clear()
    bb.COLORS.update({name: tuple(rgba) for name, rgba in data["colors"].items()})
    bb.parts.clear()
    for part in data["parts"]:
        bb.add(part["name"], part["group"], tuple(part["xyz"]), tuple(part["size"]), part["material"],
               tuple(part["rotation"]), tuple(part["pivot"]) if part["pivot"] else None,
               part["texture"], tuple(part["uv"]) if part["uv"] else None)


def generate_concept_cloth():
    import numpy as np
    from PIL import Image

    source = Image.open(ROOT / "MANUL_4.png").convert("RGB")
    array = np.asarray(source)
    red = (array[:, :, 0] > array[:, :, 1] * 1.35) & (array[:, :, 0] > array[:, :, 2] * 1.25)
    yy, xx = np.indices(red.shape)
    red &= (yy > source.height * .43) & (yy < source.height * .72)
    red &= (xx > source.width * .35) & (xx < source.width * .65)
    ys, xs = np.where(red)
    if not len(xs):
        raise RuntimeError("Could not locate the red textile in MANUL_4.png")
    padding = 12
    bounds = (max(0, int(xs.min()) - padding), max(0, int(ys.min()) - padding),
              min(source.width, int(xs.max()) + padding), min(source.height, int(ys.max()) + padding))
    crop = source.crop(bounds).resize((32, 32), Image.Resampling.LANCZOS)
    pixel = crop.quantize(colors=12, method=Image.Quantize.MEDIANCUT).convert("RGBA")
    pixel.resize((64, 64), Image.Resampling.NEAREST).save(bb.CLOTH_PATH)

    model = json.loads(bb.MODEL_PATH.read_text(encoding="utf-8"))
    cloth = next(texture for texture in model["textures"] if texture["id"] == "1")
    cloth["source"] = "data:image/png;base64," + base64.b64encode(bb.CLOTH_PATH.read_bytes()).decode("ascii")
    cloth["path"] = bb.CLOTH_PATH.name
    model["textures"][model["textures"].index(cloth)] = cloth
    bb.MODEL_PATH.write_text(json.dumps(model, separators=(",", ":")), encoding="utf-8")


def main():
    if "--render" in sys.argv:
        load_cache()
        bb.render()
    else:
        build_visual_hull()
        save_cache()
        bb.make_model()
        generate_concept_cloth()


if __name__ == "__main__":
    main()
