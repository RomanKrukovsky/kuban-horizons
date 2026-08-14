"""Convert the edited manul Blockbench project into GeckoLib game assets."""

from __future__ import annotations

import json
import re
from collections import defaultdict
from pathlib import Path

from PIL import Image, ImageDraw


ROOT = Path(__file__).resolve().parents[2]
MODEL_PATH = ROOT / "manul.bbmodel"
CACHE_PATH = ROOT / "tools/texgen/manul_hunyuan_cache.json"
GEO_PATH = ROOT / "src/main/resources/assets/kubanhorizons/geckolib/models/manul.geo.json"
ANIMATION_PATH = ROOT / "src/main/resources/assets/kubanhorizons/geckolib/animations/manul.animation.json"
TEXTURE_DIR = ROOT / "src/main/resources/assets/kubanhorizons/textures/entity"
PITCH = 1.15
TILE = 8
ATLAS = 64
VOXEL_NAME = re.compile(r"^hunyuan_.+_\d{5}$")
FACES = ("north", "east", "south", "west", "up", "down")
COATS = ("steppe", "mountain", "sand", "silver")


PARENTS = {
    "body": "root",
    "chest_fur": "root",
    "head": "root",
    "front_leg_L": "root",
    "front_leg_R": "root",
    "rear_leg_L": "root",
    "rear_leg_R": "root",
    "tail_root": "root",
    "chest_cloth": "root",
    "forehead_fur": "head",
    "left_cheek": "head",
    "right_cheek": "head",
    "muzzle": "head",
    "nose": "head",
    "chin": "head",
    "left_ear": "head",
    "right_ear": "head",
    "left_eye": "head",
    "right_eye": "head",
    "left_whiskers": "head",
    "right_whiskers": "head",
    "tail_mid": "tail_root",
    "tail_tip": "tail_mid",
    "cloth_fringe": "chest_cloth",
}


def number(value):
    rounded = round(float(value), 4)
    return int(rounded) if rounded.is_integer() else rounded


def merge_voxels(parts):
    voxel_sets = defaultdict(dict)
    authored = []
    for part in parts:
        if not VOXEL_NAME.match(part["name"]):
            authored.append(part)
            continue
        key = tuple(round(value / PITCH) for value in part["xyz"])
        voxel_sets[(part["group"], part["material"])][key] = part

    merged = []
    for (group, material), cells in sorted(voxel_sets.items()):
        remaining = set(cells)
        while remaining:
            x0, y0, z0 = min(remaining, key=lambda key: (key[1], key[2], key[0]))
            x1 = x0
            while (x1 + 1, y0, z0) in remaining:
                x1 += 1

            z1 = z0
            while all((x, y0, z1 + 1) in remaining for x in range(x0, x1 + 1)):
                z1 += 1

            y1 = y0
            while all(
                (x, y1 + 1, z) in remaining
                for x in range(x0, x1 + 1)
                for z in range(z0, z1 + 1)
            ):
                y1 += 1

            for x in range(x0, x1 + 1):
                for y in range(y0, y1 + 1):
                    for z in range(z0, z1 + 1):
                        remaining.remove((x, y, z))

            sample = cells[(x0, y0, z0)]
            cell_size = sample["size"]
            merged.append({
                "name": f"merged_{group}_{len(merged):05}",
                "group": group,
                "xyz": (
                    (x0 + x1) * PITCH / 2,
                    (y0 + y1) * PITCH / 2,
                    (z0 + z1) * PITCH / 2,
                ),
                "size": (
                    (x1 - x0) * PITCH + cell_size[0],
                    (y1 - y0) * PITCH + cell_size[1],
                    (z1 - z0) * PITCH + cell_size[2],
                ),
                "material": material,
                "rotation": (0, 0, 0),
                "pivot": None,
            })
    return authored + merged


def transformed_color(name, rgba, coat):
    r, g, b, a = rgba
    if not (name.startswith("fur_") or name.startswith("concept_")):
        return tuple(rgba)
    if coat == "mountain":
        r, g, b = r * .9 + 20, g * .94 + 20, b * 1.04 + 20
    elif coat == "sand":
        r, g, b = r * 1.08 + 8, g * 1.01 + 5, b * .86
    elif coat == "silver":
        gray = r * .28 + g * .55 + b * .17
        r, g, b = gray * .96 + 18, gray + 20, gray * 1.04 + 23
    return tuple(max(0, min(255, round(value))) for value in (r, g, b)) + (a,)


def write_textures(colors):
    materials = list(colors)
    if len(materials) > (ATLAS // TILE) ** 2:
        raise RuntimeError(f"Too many materials for {ATLAS}x{ATLAS} atlas: {len(materials)}")
    uv_by_material = {}
    for index, material in enumerate(materials):
        uv_by_material[material] = ((index % 8) * TILE, (index // 8) * TILE)

    for coat in COATS:
        image = Image.new("RGBA", (ATLAS, ATLAS), (0, 0, 0, 0))
        draw = ImageDraw.Draw(image)
        for material, (u, v) in uv_by_material.items():
            color = transformed_color(material, colors[material], coat)
            draw.rectangle((u, v, u + TILE - 1, v + TILE - 1), fill=color)
            accent = tuple(max(0, min(255, channel + 8)) for channel in color[:3]) + (color[3],)
            draw.point((u + 2, v + 2), fill=accent)
            draw.point((u + 5, v + 5), fill=accent)
        image.save(TEXTURE_DIR / f"manul_{coat}.png")
    return uv_by_material


def cube_entry(part, uv_by_material):
    center = part["xyz"]
    size = part["size"]
    u, v = uv_by_material[part["material"]]
    face_uv = {face: {"uv": [u, v], "uv_size": [TILE, TILE]} for face in FACES}
    cube = {
        "origin": [number(center[index] - size[index] / 2) for index in range(3)],
        "size": [number(value) for value in size],
        "uv": face_uv,
    }
    rotation = part.get("rotation") or (0, 0, 0)
    if any(abs(value) > .0001 for value in rotation):
        cube["pivot"] = [number(value) for value in (part.get("pivot") or center)]
        cube["rotation"] = [number(value) for value in rotation]
    return cube


def write_geometry(model, parts, uv_by_material):
    parts_by_group = defaultdict(list)
    for part in parts:
        parts_by_group[part["group"]].append(part)

    bones = []
    for group in model["groups"]:
        name = group["name"]
        bone = {
            "name": name,
            "pivot": [number(value) for value in group.get("origin", (0, 0, 0))],
        }
        parent = PARENTS.get(name)
        if parent:
            bone["parent"] = parent
        cubes = parts_by_group.get(name)
        if cubes:
            bone["cubes"] = [
                cube_entry(part, uv_by_material)
                for part in sorted(cubes, key=lambda part: part["name"])
            ]
        bones.append(bone)

    geometry = {
        "format_version": "1.12.0",
        "minecraft:geometry": [{
            "description": {
                "identifier": "geometry.manul",
                "texture_width": ATLAS,
                "texture_height": ATLAS,
                "visible_bounds_width": 4,
                "visible_bounds_height": 4,
                "visible_bounds_offset": [0, 1.5, 0],
            },
            "bones": bones,
        }],
    }
    GEO_PATH.parent.mkdir(parents=True, exist_ok=True)
    GEO_PATH.write_text(json.dumps(geometry, separators=(",", ":")) + "\n", encoding="utf-8")


def write_animations():
    animations = {
        "format_version": "1.8.0",
        "animations": {
            "animation.manul.idle": {
                "loop": True,
                "animation_length": 4,
                "bones": {
                    "body": {"position": {"0.0": [0, 0, 0], "2.0": [0, .35, 0], "4.0": [0, 0, 0]}},
                    "tail_mid": {"rotation": {"0.0": [0, -3, 0], "2.0": [0, 3, 0], "4.0": [0, -3, 0]}},
                },
            },
            "animation.manul.walk": {
                "loop": True,
                "animation_length": 1,
                "bones": {
                    "front_leg_L": {"rotation": {"0.0": [18, 0, 0], ".5": [-18, 0, 0], "1.0": [18, 0, 0]}},
                    "front_leg_R": {"rotation": {"0.0": [-18, 0, 0], ".5": [18, 0, 0], "1.0": [-18, 0, 0]}},
                    "rear_leg_L": {"rotation": {"0.0": [-15, 0, 0], ".5": [15, 0, 0], "1.0": [-15, 0, 0]}},
                    "rear_leg_R": {"rotation": {"0.0": [15, 0, 0], ".5": [-15, 0, 0], "1.0": [15, 0, 0]}},
                    "body": {"position": {"0.0": [0, 0, 0], ".25": [0, .5, 0], ".5": [0, 0, 0], ".75": [0, .5, 0], "1.0": [0, 0, 0]}},
                },
            },
            "animation.manul.sit": {
                "loop": True,
                "animation_length": 3,
                "bones": {
                    "body": {"rotation": [8, 0, 0], "position": [0, -1.2, 1]},
                    "tail_root": {"rotation": [0, 18, 12]},
                    "head": {"position": [0, -1, -1]},
                },
            },
            "animation.manul.sleep": {
                "loop": True,
                "animation_length": 4,
                "bones": {
                    "body": {"rotation": [0, 0, 5], "position": [0, -2, 1]},
                    "head": {"rotation": [18, 25, 8], "position": [0, -3, 2]},
                    "tail_root": {"rotation": [0, 38, 18]},
                },
            },
            "animation.manul.hiss": {
                "loop": True,
                "animation_length": .5,
                "bones": {
                    "left_ear": {"rotation": [0, 0, -28]},
                    "right_ear": {"rotation": [0, 0, 28]},
                    "head": {"position": {"0.0": [0, 0, 0], ".25": [0, 0, -.5], ".5": [0, 0, 0]}},
                },
            },
        },
    }
    ANIMATION_PATH.parent.mkdir(parents=True, exist_ok=True)
    ANIMATION_PATH.write_text(json.dumps(animations, indent=2) + "\n", encoding="utf-8")


def main():
    model = json.loads(MODEL_PATH.read_text(encoding="utf-8"))
    cache = json.loads(CACHE_PATH.read_text(encoding="utf-8"))
    merged_parts = merge_voxels(cache["parts"])
    uv_by_material = write_textures(cache["colors"])
    write_geometry(model, merged_parts, uv_by_material)
    write_animations()
    print(
        f"wrote game manul: {len(cache['parts'])} source cuboids -> "
        f"{len(merged_parts)} rendered cuboids, {len(model['groups'])} bones"
    )


if __name__ == "__main__":
    main()
