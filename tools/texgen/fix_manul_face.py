"""Patch the face of the existing manul model without rebuilding its body."""

from __future__ import annotations

import json
import shutil
from pathlib import Path

import gen_manul_blockbench as bb


ROOT = Path(__file__).resolve().parents[2]
MODEL = ROOT / "manul.bbmodel"
MODEL_BACKUP = ROOT / "manul.before_face_fix.bbmodel"
CACHE = ROOT / "tools/texgen/manul_hunyuan_cache.json"
CACHE_BACKUP = ROOT / "tools/texgen/manul_hunyuan_cache.before_face_fix.json"
PREFIX = "facefix_"


EXISTING_BOXES = {
    "left_eye_cream": ((-6.0, 34.7, -32.05), (6.2, 4.0, .45), (0, 0, 0)),
    "left_eye_outline": ((-6.0, 34.7, -32.35), (5.5, 3.5, .35), (0, 0, 0)),
    "left_iris": ((-6.0, 34.65, -32.62), (4.4, 3.0, .3), (0, 0, 0)),
    "left_pupil": ((-6.0, 34.65, -32.82), (.75, 2.8, .2), (0, 0, 0)),
    "left_lid": ((-6.25, 36.75, -32.88), (6.8, 1.0, .25), (0, 0, -12)),
    "right_eye_cream": ((6.0, 34.7, -32.05), (6.2, 4.0, .45), (0, 0, 0)),
    "right_eye_outline": ((6.0, 34.7, -32.35), (5.5, 3.5, .35), (0, 0, 0)),
    "right_iris": ((6.0, 34.65, -32.62), (4.4, 3.0, .3), (0, 0, 0)),
    "right_pupil": ((6.0, 34.65, -32.82), (.75, 2.8, .2), (0, 0, 0)),
    "right_lid": ((6.25, 36.75, -32.88), (6.8, 1.0, .25), (0, 0, 12)),
    "hunyuan_nose": ((0, 29.9, -33.15), (3.25, 2.15, 1.0), (0, 0, 0)),
    "hunyuan_nose_drop": ((0, 28.55, -33.5), (1.0, 1.5, .65), (0, 0, 0)),
}


NEW_PARTS = (
    ("facefix_nose_bridge", "muzzle", (0, 31.75, -32.5), (2.7, 3.2, .7), "fur_warm", (0, 0, 0)),
    ("facefix_left_upper_brow", "forehead_fur", (-6.25, 37.55, -32.75), (5.6, .7, .28), "fur_dark", (0, 0, -12)),
    ("facefix_right_upper_brow", "forehead_fur", (6.25, 37.55, -32.75), (5.6, .7, .28), "fur_dark", (0, 0, 12)),
    ("facefix_left_cheek_tuft", "left_cheek", (-9.2, 29.2, -32.35), (3.6, 5.0, .65), "fur_light", (0, 0, -5)),
    ("facefix_right_cheek_tuft", "right_cheek", (9.2, 29.2, -32.35), (3.6, 5.0, .65), "fur_light", (0, 0, 5)),
    ("facefix_left_pad_inner", "muzzle", (-2.25, 28.2, -32.9), (3.8, 2.3, .85), "fur_cream", (0, 0, -2)),
    ("facefix_right_pad_inner", "muzzle", (2.25, 28.2, -32.9), (3.8, 2.3, .85), "fur_cream", (0, 0, 2)),
    ("facefix_left_pad_outer", "muzzle", (-5.0, 27.8, -32.85), (3.7, 2.8, .75), "fur_light", (0, 0, -4)),
    ("facefix_right_pad_outer", "muzzle", (5.0, 27.8, -32.85), (3.7, 2.8, .75), "fur_light", (0, 0, 4)),
    ("facefix_left_pad_lower", "muzzle", (-3.55, 26.65, -32.95), (4.7, 1.7, .8), "fur_cream", (0, 0, -3)),
    ("facefix_right_pad_lower", "muzzle", (3.55, 26.65, -32.95), (4.7, 1.7, .8), "fur_cream", (0, 0, 3)),
    ("facefix_mouth_drop", "muzzle", (0, 26.25, -33.45), (.55, 2.2, .25), "mouth", (0, 0, 0)),
    ("facefix_mouth_left", "muzzle", (-1.35, 25.7, -33.42), (2.7, .4, .22), "mouth", (0, 0, -8)),
    ("facefix_mouth_right", "muzzle", (1.35, 25.7, -33.42), (2.7, .4, .22), "mouth", (0, 0, 8)),
    ("facefix_chin", "chin", (0, 24.45, -32.9), (6.2, 2.2, .8), "fur_cream", (0, 0, 0)),
    ("facefix_chin_shadow", "chin", (0, 25.15, -33.35), (4.8, .4, .22), "fur_shadow", (0, 0, 0)),
)


def set_box(item, center, size, rotation):
    item["from"] = [round(center[i] - size[i] / 2, 3) for i in range(3)]
    item["to"] = [round(center[i] + size[i] / 2, 3) for i in range(3)]
    item["origin"] = list(center)
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


def patch_model():
    model = json.loads(MODEL.read_text(encoding="utf-8"))
    removed = {element["uuid"] for element in model["elements"] if element["name"].startswith(PREFIX)}
    model["elements"] = [element for element in model["elements"] if element["uuid"] not in removed]
    clean_outliner(model["outliner"], removed)

    elements_by_name = {element["name"]: element for element in model["elements"]}
    for name, (center, size, rotation) in EXISTING_BOXES.items():
        set_box(elements_by_name[name], center, size, rotation)

    group_ids = {group["name"]: group["uuid"] for group in model["groups"]}
    for name, group, center, size, material, rotation in NEW_PARTS:
        part = bb.Part(name, group, center, size, material, rotation)
        model["elements"].append(bb.cube_json(part))
        target = find_group(model["outliner"], group_ids[group])
        if target is None:
            raise RuntimeError(f"Could not find outliner group: {group}")
        target["children"].append(part.uuid)

    MODEL.write_text(json.dumps(model, separators=(",", ":")), encoding="utf-8")


def patch_cache():
    cache = json.loads(CACHE.read_text(encoding="utf-8"))
    cache["parts"] = [part for part in cache["parts"] if not part["name"].startswith(PREFIX)]
    parts_by_name = {part["name"]: part for part in cache["parts"]}
    for name, (center, size, rotation) in EXISTING_BOXES.items():
        part = parts_by_name[name]
        part["xyz"] = center
        part["size"] = size
        part["rotation"] = rotation
    for name, group, center, size, material, rotation in NEW_PARTS:
        cache["parts"].append(part_record(bb.Part(name, group, center, size, material, rotation)))
    CACHE.write_text(json.dumps(cache, separators=(",", ":")), encoding="utf-8")


def main():
    if not MODEL_BACKUP.exists():
        shutil.copy2(MODEL, MODEL_BACKUP)
    if not CACHE_BACKUP.exists():
        shutil.copy2(CACHE, CACHE_BACKUP)
    patch_model()
    patch_cache()
    print(f"patched {MODEL.name}: existing face edited, {len(NEW_PARTS)} face cuboids added")


if __name__ == "__main__":
    main()
