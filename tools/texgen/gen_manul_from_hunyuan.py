"""Convert the multi-view Hunyuan reconstruction into an editable Blockbench model."""

from __future__ import annotations

import base64
import json
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import gen_manul_blockbench as bb
import gen_manul_from_concepts as concept


ROOT = Path(__file__).resolve().parents[2]
MESH_PATH = ROOT / "manul_hunyuan_mv.glb"
CACHE = ROOT / "tools/texgen/manul_hunyuan_cache.json"
PITCH = 1.15


def projected_material(rgb, palette):
    import numpy as np

    delta = palette.astype(np.int32) - np.asarray(rgb, dtype=np.int32)
    index = int(np.sum(delta * delta, axis=1).argmin())
    return f"concept_{index:02}"


def build_model():
    import numpy as np
    import trimesh

    front_image, front_mask = concept.grab_mask(ROOT / "MANUL_4.png", (130, 100, 1020, 1030))
    side_image, side_mask = concept.grab_mask(ROOT / "MANUL_2.png", (40, 150, 1120, 890))
    back_image, back_mask = concept.grab_mask(ROOT / "MANUL_3.png", (250, 130, 730, 980))
    front, fm = concept.crop_grid(front_image, front_mask, (250, 115, 1010, 1045), 256, 256)
    side, sm = concept.crop_grid(side_image, side_mask, (70, 165, 1040, 1020), 256, 256)
    back, bm = concept.crop_grid(back_image, back_mask, (270, 145, 985, 1050), 256, 256)
    palette = concept.build_palette([front[fm], side[sm], back[bm]])

    loaded = trimesh.load(MESH_PATH, force="scene")
    mesh = trimesh.util.concatenate(tuple(loaded.geometry.values()))
    mesh.apply_scale((36.0, 38.3, 31.7))
    points = mesh.voxelized(PITCH).points
    minimum = points.min(axis=0)
    maximum = points.max(axis=0)
    span = maximum - minimum

    def key_x_vertical(point):
        return round(point[0] / PITCH), round(point[1] / PITCH)

    def key_depth_vertical(point):
        return round(point[2] / PITCH), round(point[1] / PITCH)

    front_y = {}
    back_y = {}
    left_x = {}
    right_x = {}
    for point in points:
        x_vertical = key_x_vertical(point)
        depth_vertical = key_depth_vertical(point)
        front_y[x_vertical] = max(front_y.get(x_vertical, point[2]), point[2])
        back_y[x_vertical] = min(back_y.get(x_vertical, point[2]), point[2])
        left_x[depth_vertical] = min(left_x.get(depth_vertical, point[0]), point[0])
        right_x[depth_vertical] = max(right_x.get(depth_vertical, point[0]), point[0])

    def sample(image, u, v):
        px = min(255, max(0, int(round(u * 255))))
        py = min(255, max(0, int(round(v * 255))))
        return image[py, px]

    bb.parts.clear()
    for index, point in enumerate(points):
        x, vertical, depth = (float(value) for value in point)
        u = (x - minimum[0]) / span[0]
        d = (maximum[2] - depth) / span[2]
        v = 1.0 - (vertical - minimum[1]) / span[1]
        x_vertical = key_x_vertical(point)
        depth_vertical = key_depth_vertical(point)
        is_front = abs(depth - front_y[x_vertical]) < PITCH * 0.6
        is_back = abs(depth - back_y[x_vertical]) < PITCH * 0.6
        is_side = (
            abs(x - left_x[depth_vertical]) < PITCH * 0.6
            or abs(x - right_x[depth_vertical]) < PITCH * 0.6
        )

        if v < 0.09:
            rgb = sample(side, d, 0.16)
        elif is_front:
            rgb = sample(front, u, v)
        elif is_back:
            rgb = sample(back, 1.0 - u, v)
        elif is_side:
            rgb = sample(side, d, v)
        else:
            rgb = sample(side, d, v)
        material = projected_material(rgb, palette)
        red_pixel = int(rgb[0]) > int(rgb[1]) * 1.25 and int(rgb[0]) > int(rgb[2]) * 1.3
        if red_pixel:
            material = ("fur_base", "fur_light", "fur_base", "fur_warm", "fur_shadow")[index % 5]

        if d > 0.80 and v > 0.55:
            group = "tail_root" if d < 0.87 else ("tail_mid" if d < 0.94 else "tail_tip")
        elif v > 0.78:
            side_name = "L" if u < 0.5 else "R"
            group = f"front_leg_{side_name}" if d < 0.52 else f"rear_leg_{side_name}"
        elif is_front and v < 0.48:
            group = concept.group_for_front(int(u * concept.GRID_W), int(v * concept.GRID_H), rgb)
        elif is_front and v < 0.73:
            group = "chest_cloth" if red_pixel else "chest_fur"
        elif d < 0.36 and v < 0.58:
            group = "head"
        else:
            group = "body"
        if group == "chest_cloth":
            material = ("fur_base", "fur_light", "fur_base", "fur_warm", "fur_shadow")[index % 5]
        if group == "left_ear" and u < 0.13 and 0.09 < v < 0.22:
            material = "fur_dark"
        if group == "right_ear" and u > 0.87 and 0.09 < v < 0.22:
            material = "fur_dark"

        bb.add(
            f"hunyuan_{group}_{index:05}",
            group,
            (x, vertical - minimum[1], -depth),
            (PITCH * 1.03, PITCH * 1.03, PITCH * 1.03),
            material,
        )

    height = float(span[1])
    front_depth = float(-maximum[2] - PITCH * 0.65)
    for side_name, sign in (("left", -1), ("right", 1)):
        eye_group = f"{side_name}_eye"
        x = sign * 5.9
        eye_y = height * 0.70
        bb.add(f"{side_name}_eye_cream", eye_group, (x, eye_y, front_depth), (5.8, 4.1, .4), "fur_cream")
        bb.add(f"{side_name}_eye_outline", eye_group, (x, eye_y, front_depth - .3), (5.2, 3.6, .3), "fur_dark")
        bb.add(f"{side_name}_iris", eye_group, (x, eye_y, front_depth - .55), (4.2, 3.2, .25), "eye")
        bb.add(f"{side_name}_pupil", eye_group, (x, eye_y, front_depth - .75), (.68, 3.0, .18), "pupil")
        bb.add(f"{side_name}_lid", "forehead_fur", (x - sign * .25, eye_y + 1.9, front_depth - .72),
               (6.3, .9, .2), "fur_dark", rotation=(0, 0, sign * 9))

    nose_y = height * 0.60
    bb.add("hunyuan_nose", "nose", (0, nose_y, front_depth - .8), (2.8, 1.8, .55), "nose",
           pivot=(0, nose_y + .5, front_depth))
    bb.add("hunyuan_nose_drop", "nose", (0, nose_y - 1.05, front_depth - .9), (.85, 1.1, .4), "nose")

    cloth_y = height * .34
    bb.add("hunyuan_cloth_border", "chest_cloth", (0, cloth_y, front_depth - .3), (11.5, 10.8, .4),
           "cloth_dark", pivot=(0, cloth_y + 5, front_depth), texture=1, uv=(0, 0, 64, 64))
    for index, (x, y, width, ornament_height, angle) in enumerate((
        (0, 0, .8, 6.6, 0), (0, 0, 6.2, .8, 0),
        (-2.3, 2.3, 3.3, .7, 32), (2.3, 2.3, 3.3, .7, -32),
        (-2.3, -2.3, 3.3, .7, -32), (2.3, -2.3, 3.3, .7, 32),
        (-3.8, 0, .7, 4.7, 0), (3.8, 0, .7, 4.7, 0),
    )):
        bb.add(f"hunyuan_cloth_ornament_{index}", "chest_cloth",
               (x, cloth_y + y, front_depth - .56), (width, ornament_height, .18),
               "cloth_cream", rotation=(0, 0, angle))
    for index, x in enumerate((-4.5, -3.0, -1.5, 0, 1.5, 3.0, 4.5)):
        bb.add(f"hunyuan_fringe_{index}", "cloth_fringe", (x, cloth_y - 6.5 - index % 2 * .2, front_depth - .25),
               (.7, 2.5 + index % 2 * .4, .45), "cloth_red", pivot=(x, cloth_y - 5, front_depth))

    for side_name, sign in (("left", -1), ("right", 1)):
        for index in range(7):
            bb.add(f"{side_name}_hunyuan_whisker_{index}", f"{side_name}_whiskers",
                   (sign * (11.5 + index * .7), height * .49 - index * .65, front_depth - 1.1),
                   (9.5 + index * .5, .18, .18), "whisker",
                   rotation=(0, 0, sign * (-5 - index * 2)),
                   pivot=(sign * 7.5, height * .51, front_depth))


def save_cache():
    CACHE.write_text(json.dumps({
        "colors": bb.COLORS,
        "parts": [{
            "name": part.name,
            "group": part.group,
            "xyz": part.xyz,
            "size": part.size,
            "material": part.material,
            "rotation": part.rotation,
            "pivot": part.pivot,
            "texture": part.texture,
            "uv": part.uv,
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
    bounds = (int(xs.min()) - 12, int(ys.min()) - 12, int(xs.max()) + 12, int(ys.max()) + 12)
    crop = source.crop(bounds).resize((32, 32), Image.Resampling.LANCZOS)
    crop.quantize(colors=12, method=Image.Quantize.MEDIANCUT).convert("RGBA").resize(
        (64, 64), Image.Resampling.NEAREST
    ).save(bb.CLOTH_PATH)

    model = json.loads(bb.MODEL_PATH.read_text(encoding="utf-8"))
    cloth = next(texture for texture in model["textures"] if texture["id"] == "1")
    cloth["source"] = "data:image/png;base64," + base64.b64encode(bb.CLOTH_PATH.read_bytes()).decode("ascii")
    cloth["path"] = bb.CLOTH_PATH.name
    bb.MODEL_PATH.write_text(json.dumps(model, separators=(",", ":")), encoding="utf-8")


def main():
    if "--render" in sys.argv:
        load_cache()
        bb.render()
        return
    build_model()
    save_cache()
    bb.make_model()
    generate_concept_cloth()


if __name__ == "__main__":
    main()
