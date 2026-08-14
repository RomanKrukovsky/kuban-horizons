"""Generate the reference-specific editable Blockbench manul and preview renders.

Run with system Python to write the bbmodel/textures. Blender calls the same file
with ``--render`` to render the exact cuboid layout from four fixed cameras.
"""

from __future__ import annotations

import base64
import json
import math
import random
import sys
import uuid
from dataclasses import dataclass, field
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
TEXTURE_PATH = ROOT / "manul_texture.png"
CLOTH_PATH = ROOT / "manul_cloth.png"
MODEL_PATH = ROOT / "manul.bbmodel"
NS = uuid.UUID("f362efcf-dcad-4a13-b257-6bc7a261a557")

COLORS = {
    "fur_base": (137, 124, 104, 255),
    "fur_warm": (171, 151, 119, 255),
    "fur_light": (206, 194, 169, 255),
    "fur_cream": (239, 230, 207, 255),
    "fur_shadow": (91, 84, 72, 255),
    "fur_dark": (49, 46, 40, 255),
    "eye": (174, 177, 55, 255),
    "pupil": (31, 30, 24, 255),
    "nose": (154, 91, 74, 255),
    "mouth": (48, 43, 38, 255),
    "cloth_red": (151, 34, 27, 255),
    "cloth_dark": (91, 27, 25, 255),
    "cloth_cream": (224, 215, 190, 255),
    "whisker": (231, 224, 207, 255),
}


@dataclass
class Part:
    name: str
    group: str
    xyz: tuple[float, float, float]
    size: tuple[float, float, float]
    material: str = "fur_base"
    rotation: tuple[float, float, float] = (0, 0, 0)
    pivot: tuple[float, float, float] | None = None
    texture: int = 0
    uv: tuple[int, int, int, int] | None = None
    uuid: str = field(init=False)

    def __post_init__(self) -> None:
        self.uuid = str(uuid.uuid5(NS, self.name))


parts: list[Part] = []


def add(name, group, xyz, size, material="fur_base", rotation=(0, 0, 0), pivot=None,
        texture=0, uv=None):
    parts.append(Part(name, group, xyz, size, material, rotation, pivot, texture, uv))


def add_voxel_volume(prefix, group, center, radii, step=1.5, power=3.0, seed=0,
                     materials=("fur_base", "fur_warm", "fur_light", "fur_shadow")):
    """Build a dense stepped volume from editable cuboid rows.

    Each Y/Z voxel row is merged into one X cuboid. This gives the rounded,
    densely stepped reference silhouette without creating thousands of loose cubes.
    """
    cx, cy, cz = center
    rx, ry, rz = radii
    rng = random.Random(seed)
    row = 0
    y = cy - ry + step / 2
    while y < cy + ry:
        z = cz - rz + step / 2
        while z < cz + rz:
            normalized = abs((y - cy) / ry) ** power + abs((z - cz) / rz) ** power
            if normalized < 1:
                half_width = rx * (1 - normalized) ** (1 / power)
                width = max(step, math.floor((half_width * 2) / step) * step)
                # Split visible front/back rows into compact cells. Long merged rows
                # remain inside the volume, where they reduce element count without
                # causing the striped cardboard surface seen in early previews.
                if abs((z - cz) / rz) > .58:
                    cell = step * 2
                    x = cx - width / 2 + min(cell, width) / 2
                    cell_index = 0
                    while x < cx + width / 2:
                        cell_width = min(cell, cx + width / 2 - (x - cell / 2))
                        material = rng.choices(materials, (7, 5, 3, 2)[:len(materials)])[0]
                        add(f"{prefix}_surface_{row:03}_{cell_index:02}", group,
                            (x, y, z), (cell_width, step, step), material)
                        x += cell
                        cell_index += 1
                    row += 1
                else:
                    material = rng.choices(materials, (7, 5, 3, 2)[:len(materials)])[0]
                    add(f"{prefix}_row_{row:03}", group, (cx, y, z), (width, step, step), material)
                    row += 1
            z += step
        y += step


def build_parts() -> None:
    if parts:
        return

    # Dense barrel torso. The superellipse keeps the mass heavy while stepping the contour.
    add_voxel_volume("body", "body", (0, 20.5, 4.5), (15.8, 17.0, 17.0),
                     step=1.55, power=3.2, seed=1946)
    add_voxel_volume("belly", "body", (0, 11.0, 1.0), (13.5, 7.0, 13.5),
                     step=1.55, power=3.0, seed=1947,
                     materials=("fur_light", "fur_base", "fur_cream", "fur_warm"))

    # Chest curtain hides the neck and makes the head flow directly into the body.
    add("chest_mass", "chest_fur", (0, 20, -10.2), (22, 18, 5), "fur_light")
    for i, (x, y, w, h, mat) in enumerate((
        (-9, 14, 5, 8, "fur_light"), (-4, 13, 5, 10, "fur_cream"),
        (1, 13, 5, 11, "fur_light"), (6, 14, 5, 8, "fur_cream"))):
        add(f"chest_tuft_{i:02}", "chest_fur", (x, y, -13), (w, h, 2.2), mat)

    # Broad superellipsoid head: flat top, short depth, almost body-width.
    add_voxel_volume("head", "head", (0, 36.0, -8.5), (17.0, 12.0, 8.5),
                     step=1.25, power=4.2, seed=1950,
                     materials=("fur_warm", "fur_base", "fur_light", "fur_shadow"))
    add("head_top", "forehead_fur", (0, 46.9, -8.2), (27.5, 2.0, 12), "fur_base")
    add("brow_bridge", "forehead_fur", (0, 40.4, -16.0), (7.0, 2.0, 2.0), "fur_warm")
    for i, (x, y, w, mat) in enumerate((
        (-10, 44.8, 5, "fur_shadow"), (-5.5, 45.4, 4, "fur_warm"),
        (-1, 45.0, 5, "fur_dark"), (4, 45.2, 4, "fur_shadow"),
        (9, 44.7, 5, "fur_warm"))):
        add(f"forehead_tuft_{i:02}", "forehead_fur", (x, y, -14.2), (w, 2.2, 2), mat)
    for i, (x, y, w, h) in enumerate((
        (-10.5, 43.8, 3.0, 2.3), (-6.5, 45.1, 2.4, 2.6), (-2.5, 43.6, 3.2, 2.5),
        (2.0, 45.0, 2.5, 2.6), (6.0, 43.4, 3.0, 2.3), (10.0, 44.5, 2.8, 2.4),
        (-8.0, 40.9, 2.4, 2.0), (0.0, 42.0, 2.8, 2.0), (8.0, 40.8, 2.5, 2.0))):
        add(f"forehead_mark_{i:02}", "forehead_fur", (x, y, -18.0), (w, h, .9),
            "fur_dark" if i % 3 else "fur_shadow")

    # Tiny, low, side-set ears, mostly buried in pale ear fringe.
    add("left_ear_core", "left_ear", (-16.2, 42.3, -9.8), (4.2, 6.0, 5.5), "fur_dark",
        rotation=(0, 0, -7), pivot=(-15, 40, -8))
    add("left_ear_inner", "left_ear", (-16.4, 42.2, -13.0), (2.2, 3.2, 1.2), "fur_shadow")
    add("right_ear_core", "right_ear", (16.2, 42.3, -9.8), (4.2, 6.0, 5.5), "fur_dark",
        rotation=(0, 0, 7), pivot=(15, 40, -8))
    add("right_ear_inner", "right_ear", (16.4, 42.2, -13.0), (2.2, 3.2, 1.2), "fur_shadow")
    for side, sx in (("left", -1), ("right", 1)):
        for i in range(4):
            add(f"{side}_ear_fringe_{i}", f"{side}_ear",
                (sx * (14.7 + i * .55), 42.5 - i * .35, -14.2),
                (1.0, 3.7 - i * .4, 1.8), "fur_cream", rotation=(0, 0, sx * (10 + i * 5)))

    # Deep-set eyes under separate overhanging lids.
    for side, sx in (("left", -1), ("right", 1)):
        eye_group = f"{side}_eye"
        ex = sx * 6.15
        add(f"{side}_eye_socket", eye_group, (ex, 36.9, -16.55), (6.4, 5.2, 1.2), "fur_cream")
        add(f"{side}_eye_outline", eye_group, (ex, 36.9, -17.18), (6.0, 4.8, .75), "fur_dark")
        add(f"{side}_iris", eye_group, (ex, 36.75, -17.65), (5.35, 4.35, .55), "eye")
        add(f"{side}_pupil", eye_group, (ex, 36.75, -18.0), (.78, 3.95, .45), "pupil")
        add(f"{side}_eye_glint", eye_group, (ex - .75, 38.0, -18.28), (.55, .55, .25), "fur_cream")
        add(f"{side}_eye_lower_rim", eye_group, (ex, 34.9, -17.28), (5.6, .9, .85), "fur_light")
        add(f"{side}_upper_lid", "forehead_fur", (ex - sx * .25, 39.45, -18.0), (7.6, 1.4, 1.15), "fur_dark",
            rotation=(0, 0, sx * 10))
        add(f"{side}_upper_lid_fur", "forehead_fur", (ex - sx * .55, 40.6, -17.35), (6.4, 1.65, 1.25), "fur_light",
            rotation=(0, 0, sx * 12))

    # Multi-level muzzle: bridge, nose, whisker pads, mouth and low chin.
    add("nose_bridge", "muzzle", (0, 35.2, -17.2), (4.2, 5.8, 2.9), "fur_warm")
    add("nose", "nose", (0, 33.2, -19.5), (3.4, 2.0, 2.2), "nose", pivot=(0, 34, -18))
    add("nose_lower", "nose", (0, 31.9, -19.6), (1.2, 1.4, 1.5), "nose")
    add("left_whisker_pad", "muzzle", (-4.5, 30.4, -19.0), (9.0, 5.4, 3.8), "fur_cream")
    add("right_whisker_pad", "muzzle", (4.5, 30.4, -19.0), (9.0, 5.4, 3.8), "fur_cream")
    add("muzzle_center_shadow", "muzzle", (0, 29.4, -21.0), (1.1, 3.4, .7), "fur_shadow")
    add("left_muzzle_outer", "muzzle", (-7.5, 29.5, -17.7), (4.0, 5.2, 2.4), "fur_light")
    add("right_muzzle_outer", "muzzle", (7.5, 29.5, -17.7), (4.0, 5.2, 2.4), "fur_light")
    add("mouth_line", "muzzle", (0, 27.75, -19.75), (7.4, .62, .7), "mouth")
    add("mouth_left_corner", "muzzle", (-4.0, 27.15, -19.65), (1.4, 1.45, .7), "mouth")
    add("mouth_right_corner", "muzzle", (4.0, 27.15, -19.65), (1.4, 1.45, .7), "mouth")
    add("chin_core", "chin", (0, 24.9, -18.0), (16.0, 6.2, 3.8), "fur_cream", pivot=(0, 28, -16))
    for i, x in enumerate((-5.0, -3.2, -1.4, .5, 2.4, 4.4)):
        add(f"chin_tuft_{i:02}", "chin", (x, 23.7 - (i % 2) * .5, -19.2),
            (2.1, 3.5 + (i % 3) * .6, 1.8), "fur_cream" if i % 2 else "fur_light",
            rotation=(0, 0, (-1 if x < 0 else 1) * (3 + i)))

    # Massive cheek slabs and directional tufts. Dark stripes run down/out from eyes.
    for side, sx in (("left", -1), ("right", 1)):
        group = f"{side}_cheek"
        add(f"{side}_cheek_core", group, (sx * 11.4, 32.0, -14.9), (11.0, 14.5, 7.0), "fur_light")
        for i in range(7):
            add(f"{side}_cheek_tuft_{i:02}", group,
                (sx * (14.2 + (i % 3) * .75), 36.0 - i * 1.6, -16.0 - (i % 2) * .6),
                (3.3 - (i % 2) * .5, 3.8, 2.5), "fur_cream" if i % 3 else "fur_light",
                rotation=(0, 0, sx * (5 + i * 2)))
        for i in range(3):
            add(f"{side}_face_stripe_{i}", group,
                (sx * (9.0 + i * 2.15), 33.8 - i * 3.1, -18.65),
                (7.2 - i * .55, 1.55, 1.0), "fur_dark" if i != 1 else "fur_shadow",
                rotation=(0, 0, sx * -(28 + i * 3)))
        add(f"{side}_cheek_pale_wedge", group, (sx * 10.8, 30.0, -18.25),
            (6.0, 2.0, .8), "fur_cream", rotation=(0, 0, sx * 12))
        add(f"{side}_muzzle_fan_upper", group, (sx * 9.1, 29.4, -19.15),
            (8.3, 2.4, 1.35), "fur_cream", rotation=(0, 0, sx * 20))
        add(f"{side}_muzzle_fan_lower", group, (sx * 8.7, 26.4, -19.05),
            (8.0, 2.5, 1.25), "fur_light", rotation=(0, 0, sx * -15))
        add(f"{side}_eye_mask_upper", group, (sx * 9.2, 33.6, -18.7),
            (7.8, 1.8, 1.0), "fur_dark", rotation=(0, 0, sx * -31))
        add(f"{side}_eye_mask_lower", group, (sx * 11.0, 29.8, -18.7),
            (7.3, 1.6, 1.0), "fur_dark", rotation=(0, 0, sx * -34))

    # Small front-facing tufts break the facial planes into the reference's shaggy contour.
    for i in range(34):
        side = -1 if i % 2 == 0 else 1
        band = i // 2
        x = side * (7.5 + (band % 5) * 1.7)
        y = 43.0 - (band // 5) * 3.0 - (band % 2) * .6
        z = -18.0 - (band % 3) * .25
        mat = ("fur_light", "fur_cream", "fur_warm", "fur_shadow")[(i + band) % 4]
        add(f"face_fur_{i:03}", "forehead_fur" if y > 39 else ("left_cheek" if side < 0 else "right_cheek"),
            (x, y, z), (1.5 + (i % 3) * .35, 2.0 + (band % 2) * .6, 1.1), mat,
            rotation=(0, 0, side * (4 + band % 7)))

    # Thin cuboid whiskers, fanning laterally and slightly downward.
    for side, sx in (("left", -1), ("right", 1)):
        for i in range(6):
            length = 9.0 + i * .55
            add(f"{side}_whisker_{i:02}", f"{side}_whiskers",
                (sx * (6.8 + length / 2), 31.8 - i * .75, -20.0),
                (length, .22, .22), "whisker", rotation=(0, 0, sx * (-4 - i * 2)),
                pivot=(sx * 6.4, 32, -19))

    # Short column legs and broad feet with explicit toe divisions.
    leg_specs = (("front_leg_L", -9.2, -7.0), ("front_leg_R", 9.2, -7.0),
                 ("rear_leg_L", -9.2, 10.0), ("rear_leg_R", 9.2, 10.0))
    for group, x, z in leg_specs:
        add(f"{group}_upper", group, (x, 10.0, z), (8.2, 13.0, 9.2), "fur_light", pivot=(x, 15, z))
        add(f"{group}_foot", group, (x, 4.0, z - 1.2), (9.3, 5.0, 11.0), "fur_warm")
        for toe in (-2.6, 0, 2.6):
            add(f"{group}_toe_{toe:+.1f}", group, (x + toe, 3.7, z - 6.8), (1.0, 3.6, .55), "fur_dark")

    # Thick segmented tail exits rear-left and bends around the body.
    add("tail_root_core", "tail_root", (11.4, 10.5, 13.2), (8.5, 8.0, 8.0), "fur_shadow",
        rotation=(5, -18, -18), pivot=(10, 15, 12))
    add("tail_root_lower", "tail_root", (14.0, 8.1, 12.5), (8.2, 7.8, 8.5), "fur_light",
        rotation=(0, -15, -22))
    add("tail_mid_core", "tail_mid", (17.0, 5.8, 10.8), (8.6, 7.4, 8.3), "fur_light",
        rotation=(0, -12, -10), pivot=(16.5, 8.0, 13))
    add("tail_mid_outer", "tail_mid", (20.0, 4.9, 8.8), (7.7, 7.2, 8.0), "fur_base",
        rotation=(0, -8, -4))
    add("tail_ring_1", "tail_mid", (17.2, 5.9, 10.9), (1.8, 7.8, 8.6), "fur_dark", rotation=(0, -12, -10))
    add("tail_ring_2", "tail_mid", (20.3, 5.0, 8.9), (1.8, 7.6, 8.3), "fur_shadow", rotation=(0, -8, -4))
    add("tail_tip_core", "tail_tip", (23.4, 4.5, 6.8), (6.2, 7.0, 7.5), "fur_dark",
        rotation=(0, -4, 0), pivot=(21.5, 5.2, 8))

    # The chest textile is a separate hanging object, not a scarf.
    add("cloth_backing", "chest_cloth", (0, 19.2, -15.2), (12.5, 14.2, 1.0), "cloth_dark",
        pivot=(0, 25.5, -14.5), texture=1, uv=(0, 0, 64, 64))
    add("cloth_field", "chest_cloth", (0, 19.5, -15.85), (10.8, 12.3, .7), "cloth_red")
    # Embroidery: central eight-point star and mirrored ram-horn geometry.
    for i, (x, y, w, h) in enumerate((
        (0, 20.1, 1.2, 8.0), (0, 20.1, 8.0, 1.2), (-2.8, 22.9, 3.0, 1.1),
        (2.8, 22.9, 3.0, 1.1), (-2.8, 17.3, 3.0, 1.1), (2.8, 17.3, 3.0, 1.1),
        (-4.1, 24.0, 1.1, 3.5), (4.1, 24.0, 1.1, 3.5),
        (-4.1, 16.2, 1.1, 3.5), (4.1, 16.2, 1.1, 3.5),
        (-2.0, 25.0, 1.0, 3.0), (2.0, 25.0, 1.0, 3.0),
        (-2.0, 15.2, 1.0, 3.0), (2.0, 15.2, 1.0, 3.0))):
        add(f"cloth_embroidery_{i:02}", "chest_cloth", (x, y, -16.35), (w, h, .32), "cloth_cream")
    for side, sx in (("left", -1), ("right", 1)):
        for i, (x, y, w, h, angle) in enumerate(((4.1, 21.5, 3.0, .75, 32), (4.5, 19.0, 2.7, .75, -32),
                                                   (4.2, 17.0, 2.4, .75, 32))):
            add(f"cloth_{side}_branch_{i}", "chest_cloth", (sx * x, y, -16.36), (w, h, .32),
                "cloth_cream", rotation=(0, 0, sx * angle))
    for i, x in enumerate((-4.8, -3.2, -1.6, 0, 1.6, 3.2, 4.8)):
        add(f"fringe_{i:02}", "cloth_fringe", (x, 11.6 - (i % 2) * .3, -15.8),
            (.75, 3.2 + (i % 2) * .6, .8), "cloth_red", pivot=(x, 13, -15))

    # Structured body fur clusters: sparse at the back, dense on shoulder/chest contours.
    rng = random.Random(1946)
    for i in range(62):
        side = -1 if i % 2 == 0 else 1
        angle = rng.uniform(-1.1, 1.1)
        x = side * rng.uniform(11.8, 14.6)
        y = rng.uniform(13.0, 33.0)
        z = rng.uniform(-5.5, 13.0)
        scale = rng.choice((1.4, 1.8, 2.2, 2.7))
        mat = rng.choices(("fur_base", "fur_warm", "fur_light", "fur_shadow"), (4, 3, 3, 2))[0]
        add(f"body_fur_{i:03}", "body", (x, y, z), (scale, scale * 1.35, scale), mat,
            rotation=(0, math.degrees(angle) * .15, side * rng.uniform(-8, 8)))

    # Front-facing fur breaks up the large chest plane without becoming random noise.
    for i in range(28):
        row = i // 7
        col = i % 7
        x = (col - 3) * 3.0 + (row % 2) * .7
        y = 24.0 - row * 3.7
        z = -13.1 - (i % 3) * .25
        mat = ("fur_light", "fur_base", "fur_cream", "fur_shadow")[(i * 5 + row) % 4]
        add(f"chest_front_fur_{i:03}", "chest_fur", (x, y, z),
            (2.4 + (i % 2) * .5, 3.4 + (i % 3) * .4, 1.4), mat,
            rotation=(0, 0, (col - 3) * 2.5))

    # Authored coat patches follow shoulder and flank volumes rather than a noise field.
    for i, (x, y, z, w, h, mat) in enumerate((
        (-11.8, 29, -4, 3.5, 5.0, "fur_shadow"), (-12.5, 24, 1, 2.8, 4.0, "fur_dark"),
        (-13.0, 19, 7, 3.0, 5.5, "fur_warm"), (12.0, 28, -2, 3.0, 4.0, "fur_shadow"),
        (13.0, 22, 4, 2.6, 5.0, "fur_dark"), (12.5, 17, 10, 3.5, 4.5, "fur_warm"),
        (-8.0, 33, 13, 5.0, 2.4, "fur_dark"), (2.0, 33.5, 14, 4.0, 2.2, "fur_shadow"),
        (9.0, 31.5, 13.5, 3.5, 2.4, "fur_dark"))):
        add(f"coat_patch_{i:02}", "body", (x, y, z), (w, h, 1.0), mat)

    # The reference head is wider and lower than a conventional cat head.
    head_groups = {
        "head", "forehead_fur", "left_cheek", "right_cheek", "muzzle", "nose", "chin",
        "left_ear", "right_ear", "left_eye", "right_eye", "left_whiskers", "right_whiskers",
    }
    for part in parts:
        if part.group in head_groups:
            x, y, z = part.xyz
            sx, sy, sz = part.size
            part.xyz = (x * 1.08, y - 2.4, z)
            part.size = (sx * 1.08, sy, sz)
            if part.pivot:
                px, py, pz = part.pivot
                part.pivot = (px * 1.08, py - 2.4, pz)


def uuid_for(name: str) -> str:
    return str(uuid.uuid5(NS, "group:" + name))


GROUP_TREE = {
    "root": ["body", "chest_fur", "head", "front_leg_L", "front_leg_R", "rear_leg_L", "rear_leg_R",
             "tail_root", "chest_cloth"],
    "head": ["forehead_fur", "left_cheek", "right_cheek", "muzzle", "nose", "chin", "left_ear",
             "right_ear", "left_eye", "right_eye", "left_whiskers", "right_whiskers"],
    "tail_root": ["tail_mid"],
    "tail_mid": ["tail_tip"],
    "chest_cloth": ["cloth_fringe"],
}


def make_texture() -> None:
    from PIL import Image, ImageDraw

    image = Image.new("RGBA", (256, 256), COLORS["fur_base"])
    draw = ImageDraw.Draw(image)
    names = list(COLORS)
    for index, name in enumerate(names):
        x = (index % 8) * 32
        y = (index // 8) * 32
        draw.rectangle((x, y, x + 31, y + 31), fill=COLORS[name])
        lighter = tuple(min(255, c + 12) for c in COLORS[name][:3]) + (255,)
        for p in range(4, 32, 8):
            draw.rectangle((x + p, y + (p * 3) % 27, x + p + 2, y + (p * 3) % 27 + 2), fill=lighter)
    # Irregular coat tiles used by any manually re-UVed future additions.
    rng = random.Random(1946)
    for y in range(96, 256, 8):
        for x in range(0, 256, 8):
            name = rng.choices(("fur_base", "fur_warm", "fur_light", "fur_shadow", "fur_dark"), (5, 4, 3, 2, 1))[0]
            draw.rectangle((x, y, x + 7, y + 7), fill=COLORS[name])
    image.save(TEXTURE_PATH)

    cloth = Image.new("RGBA", (64, 64), COLORS["cloth_dark"])
    cd = ImageDraw.Draw(cloth)
    cd.rectangle((5, 4, 58, 57), fill=COLORS["cloth_red"])
    cd.rectangle((8, 7, 55, 54), outline=COLORS["cloth_cream"], width=2)
    cream = COLORS["cloth_cream"]
    # Pixel-only eight-point flower and mirrored angular branches.
    for box in ((30, 15, 33, 47), (16, 30, 47, 33), (22, 22, 41, 25), (22, 38, 41, 41),
                (22, 22, 25, 41), (38, 22, 41, 41), (12, 14, 16, 28), (47, 14, 51, 28),
                (12, 36, 16, 50), (47, 36, 51, 50)):
        cd.rectangle(box, fill=cream)
    cloth.save(CLOTH_PATH)


def swatch_uv(material: str) -> list[int]:
    index = list(COLORS).index(material)
    x = (index % 8) * 32 + 4
    y = (index // 8) * 32 + 4
    return [x, y, x + 20, y + 20]


def cube_json(part: Part) -> dict:
    x, y, z = part.xyz
    sx, sy, sz = part.size
    uv = list(part.uv) if part.uv else swatch_uv(part.material)
    faces = {face: {"uv": uv, "texture": part.texture} for face in ("north", "east", "south", "west", "up", "down")}
    return {
        "name": part.name, "box_uv": False, "render_order": "default", "locked": False,
        "export": True, "scope": 0, "allow_mirror_modeling": True,
        "from": [round(x - sx / 2, 3), round(y - sy / 2, 3), round(z - sz / 2, 3)],
        "to": [round(x + sx / 2, 3), round(y + sy / 2, 3), round(z + sz / 2, 3)],
        "autouv": 0, "color": list(COLORS).index(part.material) % 8,
        "origin": list(part.pivot or part.xyz), "rotation": list(part.rotation),
        "faces": faces, "type": "cube", "uuid": part.uuid,
    }


def build_outliner(group: str) -> dict:
    child_groups = GROUP_TREE.get(group, [])
    direct_parts = [p.uuid for p in parts if p.group == group]
    return {
        "uuid": uuid_for(group), "isOpen": group in ("root", "head"),
        "children": direct_parts + [build_outliner(child) for child in child_groups],
    }


def make_model() -> None:
    build_parts()
    make_texture()
    groups = []
    all_groups = ["root"] + [g for children in GROUP_TREE.values() for g in children]
    pivots = {"head": (0, 32, -4), "front_leg_L": (-8.4, 15, -7), "front_leg_R": (8.4, 15, -7),
              "rear_leg_L": (-9.2, 15, 10), "rear_leg_R": (9.2, 15, 10), "tail_root": (10, 17, 11),
              "tail_mid": (14, 12, 13), "tail_tip": (22, 8, 11), "chest_cloth": (0, 25.5, -14.5)}
    for name in dict.fromkeys(all_groups):
        groups.append({
            "name": name, "uuid": uuid_for(name), "export": True, "locked": False, "scope": 0,
            "selected": False, "origin": list(pivots.get(name, (0, 0, 0))), "rotation": [0, 0, 0],
            "color": 0, "children": [], "reset": False, "shade": True, "mirror_uv": False,
            "visibility": True, "autouv": 0, "isOpen": name in ("root", "head"), "primary_selected": False,
        })

    def texture_entry(path: Path, texture_id: str) -> dict:
        source = "data:image/png;base64," + base64.b64encode(path.read_bytes()).decode("ascii")
        return {
            "name": path.stem, "path": path.name, "folder": "", "namespace": "", "id": texture_id,
            "group": "", "scope": 0, "width": 256 if texture_id == "0" else 64,
            "height": 256 if texture_id == "0" else 64, "uv_width": 256 if texture_id == "0" else 64,
            "uv_height": 256 if texture_id == "0" else 64, "particle": False, "use_as_default": texture_id == "0",
            "layers_enabled": False, "sync_to_project": "", "file_format": "png", "render_mode": "default",
            "render_sides": "auto", "wrap_mode": "limited", "pbr_channel": "color", "fps": 1,
            "frame_time": 1, "frame_order_type": "loop", "frame_order": "", "frame_interpolate": False,
            "visible": True, "internal": True, "saved": True,
            "uuid": str(uuid.uuid5(NS, "texture:" + path.name)), "source": source,
        }

    model = {
        "meta": {"format_version": "5.0", "model_format": "free", "box_uv": False},
        "name": "manul", "model_identifier": "manul", "visible_box": [3, 3, 0],
        "variable_placeholders": "", "multi_file_ruleset": "", "variable_placeholder_buttons": [],
        "timeline_setups": [], "unhandled_root_fields": {}, "resolution": {"width": 256, "height": 256},
        "elements": [cube_json(part) for part in parts], "groups": groups,
        "outliner": [build_outliner("root")],
        "textures": [texture_entry(TEXTURE_PATH, "0"), texture_entry(CLOTH_PATH, "1")],
    }
    MODEL_PATH.write_text(json.dumps(model, separators=(",", ":")), encoding="utf-8")
    print(f"wrote {MODEL_PATH.name}: {len(parts)} editable cuboids, {len(groups)} groups")


def render() -> None:
    import bpy
    from mathutils import Matrix, Vector

    build_parts()
    bpy.ops.object.select_all(action="SELECT")
    bpy.ops.object.delete(use_global=False)

    materials = {}
    for name, rgba in COLORS.items():
        mat = bpy.data.materials.new(name)
        color = tuple(c / 255 for c in rgba)
        mat.diffuse_color = color
        mat.roughness = .92
        mat.use_nodes = True
        mat.node_tree.nodes["Principled BSDF"].inputs["Base Color"].default_value = color
        mat.node_tree.nodes["Principled BSDF"].inputs["Roughness"].default_value = .92
        materials[name] = mat

    # Batch preview geometry by material. The bbmodel keeps every cuboid separate;
    # only the verification renderer merges them to avoid thousands of Blender objects.
    batches = {}
    corners = ((-1, -1, -1), (1, -1, -1), (1, 1, -1), (-1, 1, -1),
               (-1, -1, 1), (1, -1, 1), (1, 1, 1), (-1, 1, 1))
    cube_faces = ((0, 1, 2, 3), (4, 7, 6, 5), (0, 4, 5, 1),
                  (1, 5, 6, 2), (2, 6, 7, 3), (4, 0, 3, 7))
    for part in parts:
        vertices, faces = batches.setdefault(part.material, ([], []))
        x, y, z = part.xyz
        sx, sy, sz = part.size
        rotation = Matrix.Rotation(math.radians(part.rotation[0]), 4, "X")
        rotation @= Matrix.Rotation(math.radians(part.rotation[1]), 4, "Z")
        rotation @= Matrix.Rotation(math.radians(-part.rotation[2]), 4, "Y")
        base = len(vertices)
        for cx, cy, cz in corners:
            local = Vector((cx * sx / 2, -cz * sz / 2, cy * sy / 2))
            transformed = rotation @ local + Vector((x, -z, y))
            vertices.append(tuple(transformed))
        faces.extend(tuple(base + index for index in face) for face in cube_faces)
    for material_name, (vertices, faces) in batches.items():
        mesh = bpy.data.meshes.new(f"manul_{material_name}")
        mesh.from_pydata(vertices, [], faces)
        mesh.materials.append(materials[material_name])
        bpy.data.objects.new(f"manul_{material_name}", mesh)
        bpy.context.collection.objects.link(bpy.data.objects[f"manul_{material_name}"])

    world = bpy.context.scene.world
    world.use_nodes = True
    world.node_tree.nodes["Background"].inputs["Color"].default_value = (.56, .56, .56, 1)
    world.node_tree.nodes["Background"].inputs["Strength"].default_value = .55
    bpy.ops.object.light_add(type="AREA", location=(-35, 45, 65))
    bpy.context.object.data.energy = 1300
    bpy.context.object.data.shape = "DISK"
    bpy.context.object.data.size = 35
    bpy.ops.object.light_add(type="AREA", location=(35, 20, 42))
    bpy.context.object.data.energy = 700
    bpy.context.object.data.size = 25

    bpy.ops.object.camera_add()
    camera = bpy.context.object
    camera.data.type = "ORTHO"
    camera.data.ortho_scale = 72
    bpy.context.scene.camera = camera

    def point_camera(location, target=(0, 23, 0)):
        camera.location = (location[0], -location[2], location[1])
        mapped_target = Vector((target[0], -target[2], target[1]))
        camera.rotation_euler = (mapped_target - camera.location).to_track_quat("-Z", "Y").to_euler()

    settings = bpy.context.scene.render
    settings.engine = "BLENDER_EEVEE"
    settings.resolution_x = 900
    settings.resolution_y = 900
    settings.resolution_percentage = 100
    settings.image_settings.file_format = "PNG"
    settings.film_transparent = False
    settings.image_settings.color_mode = "RGBA"
    bpy.context.scene.view_settings.look = "AgX - High Contrast"

    views = {
        "preview_front.png": ((0, 27, -115), (0, 23, -2)),
        "preview_3q.png": ((72, 36, -96), (0, 23, 0)),
        "preview_side.png": ((-115, 28, 0), (0, 22, 0)),
        "preview_back.png": ((0, 28, 115), (0, 22, 3)),
    }
    for filename, (location, target) in views.items():
        point_camera(location, target)
        settings.filepath = str(ROOT / filename)
        bpy.ops.render.render(write_still=True)
        print("rendered", filename)


if __name__ == "__main__":
    if "--render" in sys.argv:
        render()
    else:
        make_model()
