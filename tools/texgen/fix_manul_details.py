"""Patch whiskers, cloth, nose, and paws without rebuilding the manul body."""

from __future__ import annotations

import base64
import json
import shutil
from pathlib import Path

from PIL import Image, ImageDraw

import gen_manul_blockbench as bb


ROOT = Path(__file__).resolve().parents[2]
MODEL = ROOT / "manul.bbmodel"
MODEL_BACKUP = ROOT / "manul.before_detail_fix.bbmodel"
CACHE = ROOT / "tools/texgen/manul_hunyuan_cache.json"
CACHE_BACKUP = ROOT / "tools/texgen/manul_hunyuan_cache.before_detail_fix.json"
PREFIX = "detailfix_"
REPLACED_PREFIXES = ("hunyuan_cloth_ornament_", "hunyuan_fringe_")


EDITED_PARTS = {
    "hunyuan_nose": {
        "center": (0, 29.85, -33.58),
        "size": (3.6, 1.35, .72),
        "rotation": (0, 0, 0),
        "pivot": (0, 30.2, -32.0),
    },
    "hunyuan_nose_drop": {
        "center": (0, 28.85, -33.78),
        "size": (1.45, 1.15, .78),
        "rotation": (0, 0, 0),
        "pivot": None,
    },
    "hunyuan_cloth_border": {
        "center": (0, 17.1, -32.12),
        "size": (12.0, 11.6, .5),
        "rotation": (0, 0, 0),
        "pivot": (0, 22.4, -31.8),
    },
}


NEW_PARTS = (
    ("detailfix_left_nostril", "nose", (-.9, 29.72, -34.0), (.55, .3, .18), "mouth", (0, 0, -5)),
    ("detailfix_right_nostril", "nose", (.9, 29.72, -34.0), (.55, .3, .18), "mouth", (0, 0, 5)),
    ("detailfix_cloth_field", "chest_cloth", (0, 17.15, -32.52), (9.9, 8.8, .2), "cloth_cream", (0, 0, 0)),
    ("detailfix_cloth_border_top", "chest_cloth", (0, 21.15, -32.67), (10.0, .65, .18), "cloth_red", (0, 0, 0)),
    ("detailfix_cloth_border_bottom", "chest_cloth", (0, 13.15, -32.67), (10.0, .65, .18), "cloth_red", (0, 0, 0)),
    ("detailfix_cloth_border_left", "chest_cloth", (-4.62, 17.15, -32.67), (.65, 8.65, .18), "cloth_red", (0, 0, 0)),
    ("detailfix_cloth_border_right", "chest_cloth", (4.62, 17.15, -32.67), (.65, 8.65, .18), "cloth_red", (0, 0, 0)),
    ("detailfix_cloth_star_diag_a", "chest_cloth", (0, 17.2, -32.83), (5.0, .62, .18), "cloth_red", (0, 0, 45)),
    ("detailfix_cloth_star_diag_b", "chest_cloth", (0, 17.2, -32.83), (5.0, .62, .18), "cloth_red", (0, 0, -45)),
    ("detailfix_cloth_star_center", "chest_cloth", (0, 17.2, -32.85), (1.15, 1.15, .18), "cloth_red", (0, 0, 45)),
    ("detailfix_cloth_left_flower_a", "chest_cloth", (-3.25, 19.45, -32.82), (2.2, .5, .18), "cloth_red", (0, 0, 45)),
    ("detailfix_cloth_left_flower_b", "chest_cloth", (-3.25, 19.45, -32.83), (2.2, .5, .18), "cloth_red", (0, 0, -45)),
    ("detailfix_cloth_right_flower_a", "chest_cloth", (3.25, 19.45, -32.82), (2.2, .5, .18), "cloth_red", (0, 0, 45)),
    ("detailfix_cloth_right_flower_b", "chest_cloth", (3.25, 19.45, -32.83), (2.2, .5, .18), "cloth_red", (0, 0, -45)),
    ("detailfix_cloth_left_leaf_a", "chest_cloth", (-3.25, 15.0, -32.82), (2.4, .5, .18), "cloth_red", (0, 0, 32)),
    ("detailfix_cloth_left_leaf_b", "chest_cloth", (-3.25, 15.0, -32.83), (2.4, .5, .18), "cloth_red", (0, 0, -32)),
    ("detailfix_cloth_right_leaf_a", "chest_cloth", (3.25, 15.0, -32.82), (2.4, .5, .18), "cloth_red", (0, 0, 32)),
    ("detailfix_cloth_right_leaf_b", "chest_cloth", (3.25, 15.0, -32.83), (2.4, .5, .18), "cloth_red", (0, 0, -32)),
    ("detailfix_left_paw_cap", "front_leg_L", (-9.0, 2.25, -20.35), (10.2, 4.5, 2.8), "fur_warm", (0, 0, 0)),
    ("detailfix_right_paw_cap", "front_leg_R", (9.0, 2.25, -20.35), (10.2, 4.5, 2.8), "fur_warm", (0, 0, 0)),
)


def whisker_edits():
    for side_name, sign in (("left", -1), ("right", 1)):
        for index in range(7):
            length = 10.4 + index * .45
            yield f"{side_name}_hunyuan_whisker_{index}", {
                "center": (sign * (6.4 + length / 2), 28.9 - index * .48, -33.92),
                "size": (length, .16, .16),
                "rotation": (0, 0, sign * (-3.5 - index * 2.2)),
                "pivot": (sign * 6.35, 29.1, -33.15),
            }


def generated_parts():
    yield from NEW_PARTS
    for side_name, sign, group in (("left", -1, "front_leg_L"), ("right", 1, "front_leg_R")):
        for index, offset in enumerate((-2.8, 0, 2.8)):
            yield (
                f"detailfix_{side_name}_toe_crease_{index}",
                group,
                (sign * 9.0 + offset, 1.75, -21.85),
                (.42, 2.65, .24),
                "fur_dark",
                (0, 0, 0),
            )
    for index, x in enumerate((-5.0, -4.0, -3.0, -2.0, -1.0, 0, 1.0, 2.0, 3.0, 4.0, 5.0)):
        height = 2.55 + (index % 2) * .45
        yield (
            f"detailfix_fringe_{index:02}",
            "cloth_fringe",
            (x, 10.15 - (index % 2) * .2, -32.4),
            (.55, height, .42),
            "cloth_red",
            (0, 0, 0),
        )


def set_box(item, center, size, rotation, pivot):
    item["from"] = [round(center[i] - size[i] / 2, 3) for i in range(3)]
    item["to"] = [round(center[i] + size[i] / 2, 3) for i in range(3)]
    item["origin"] = list(pivot or center)
    item["rotation"] = list(rotation)


def clean_outliner(nodes, removed):
    for node in nodes:
        if not isinstance(node, dict):
            continue
        node["children"] = [
            child for child in node.get("children", [])
            if not (isinstance(child, str) and child in removed)
        ]
        clean_outliner(node["children"], removed)


def is_ground_voxel(element):
    center_y = (element["from"][1] + element["to"][1]) / 2
    return element["name"].startswith("hunyuan_") and center_y <= 1.151


def find_group(nodes, group_uuid):
    for node in nodes:
        if not isinstance(node, dict):
            continue
        if node.get("uuid") == group_uuid:
            return node
        found = find_group(node.get("children", []), group_uuid)
        if found:
            return found
    return None


def part_record(part):
    return {
        "name": part.name,
        "group": part.group,
        "xyz": part.xyz,
        "size": part.size,
        "material": part.material,
        "rotation": part.rotation,
        "pivot": part.pivot,
        "texture": part.texture,
        "uv": part.uv,
    }


def generate_cloth_texture():
    dark = bb.COLORS["cloth_dark"]
    red = bb.COLORS["cloth_red"]
    cream = bb.COLORS["cloth_cream"]
    image = Image.new("RGBA", (64, 64), dark)
    draw = ImageDraw.Draw(image)
    draw.rectangle((4, 3, 59, 59), fill=red)
    draw.rectangle((8, 7, 55, 54), fill=cream)
    draw.rectangle((10, 9, 53, 52), outline=red, width=2)

    for cx in (16, 24, 40, 48):
        draw.polygon(((cx, 11), (cx + 3, 14), (cx, 17), (cx - 3, 14)), fill=red)
        draw.polygon(((cx, 44), (cx + 3, 47), (cx, 50), (cx - 3, 47)), fill=red)

    draw.rectangle((30, 19, 33, 42), fill=red)
    draw.rectangle((20, 29, 43, 32), fill=red)
    draw.polygon(((32, 20), (43, 30), (32, 41), (21, 30)), outline=red, width=3)
    draw.polygon(((32, 24), (38, 30), (32, 36), (26, 30)), fill=red)
    for x, y in ((17, 22), (47, 22), (17, 39), (47, 39)):
        draw.polygon(((x, y - 4), (x + 4, y), (x, y + 4), (x - 4, y)), fill=red)

    image.save(bb.CLOTH_PATH)


def patch_model():
    model = json.loads(MODEL.read_text(encoding="utf-8"))
    removed = {
        element["uuid"]
        for element in model["elements"]
        if element["name"].startswith((PREFIX, *REPLACED_PREFIXES)) or is_ground_voxel(element)
    }
    model["elements"] = [element for element in model["elements"] if element["uuid"] not in removed]
    clean_outliner(model["outliner"], removed)

    elements_by_name = {element["name"]: element for element in model["elements"]}
    edits = {**EDITED_PARTS, **dict(whisker_edits())}
    for name, edit in edits.items():
        set_box(elements_by_name[name], edit["center"], edit["size"], edit["rotation"], edit["pivot"])

    group_ids = {group["name"]: group["uuid"] for group in model["groups"]}
    for name, group, center, size, material, rotation in generated_parts():
        part = bb.Part(name, group, center, size, material, rotation)
        model["elements"].append(bb.cube_json(part))
        target = find_group(model["outliner"], group_ids[group])
        if target is None:
            raise RuntimeError(f"Could not find outliner group: {group}")
        target["children"].append(part.uuid)

    cloth = next(texture for texture in model["textures"] if texture["id"] == "1")
    cloth["source"] = "data:image/png;base64," + base64.b64encode(bb.CLOTH_PATH.read_bytes()).decode("ascii")
    cloth["path"] = bb.CLOTH_PATH.name
    MODEL.write_text(json.dumps(model, separators=(",", ":")), encoding="utf-8")


def patch_cache():
    cache = json.loads(CACHE.read_text(encoding="utf-8"))
    cache["parts"] = [
        part for part in cache["parts"]
        if not part["name"].startswith((PREFIX, *REPLACED_PREFIXES))
        and not (part["name"].startswith("hunyuan_") and part["xyz"][1] <= 1.151)
    ]
    parts_by_name = {part["name"]: part for part in cache["parts"]}
    edits = {**EDITED_PARTS, **dict(whisker_edits())}
    for name, edit in edits.items():
        part = parts_by_name[name]
        part["xyz"] = edit["center"]
        part["size"] = edit["size"]
        part["rotation"] = edit["rotation"]
        part["pivot"] = edit["pivot"]
    for name, group, center, size, material, rotation in generated_parts():
        cache["parts"].append(part_record(bb.Part(name, group, center, size, material, rotation)))
    CACHE.write_text(json.dumps(cache, separators=(",", ":")), encoding="utf-8")


def main():
    if not MODEL_BACKUP.exists():
        shutil.copy2(MODEL, MODEL_BACKUP)
    if not CACHE_BACKUP.exists():
        shutil.copy2(CACHE, CACHE_BACKUP)
    generate_cloth_texture()
    patch_model()
    patch_cache()
    print(f"patched {MODEL.name}: whiskers, cloth, nose, and front paws refined")


if __name__ == "__main__":
    main()
