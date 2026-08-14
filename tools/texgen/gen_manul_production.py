"""Build the reference-matched editable Blockbench and GeckoLib manul assets."""

from __future__ import annotations

import base64
import json
import uuid
from dataclasses import dataclass, field
from pathlib import Path

try:
    from PIL import Image, ImageDraw
except ModuleNotFoundError:
    # Blender only consumes the geometry declarations and does not bundle Pillow.
    Image = None
    ImageDraw = None


ROOT = Path(__file__).resolve().parents[2]
MODEL_PATH = ROOT / "manul.bbmodel"
TEXTURE_PATH = ROOT / "manul_texture.png"
GEO_PATH = ROOT / "src/main/resources/assets/kubanhorizons/geckolib/models/manul.geo.json"
ANIMATION_PATH = ROOT / "src/main/resources/assets/kubanhorizons/geckolib/animations/manul.animation.json"
TEXTURE_DIR = ROOT / "src/main/resources/assets/kubanhorizons/textures/entity"
NS = uuid.UUID("89d0ccf8-496c-4ee1-b8a5-6b15c76a7e36")
ATLAS = 256
TILE = 16

COLORS = {
    "fur_gray": (126, 130, 132, 255),
    "fur_mid": (151, 154, 154, 255),
    "fur_light": (185, 187, 184, 255),
    "fur_white": (224, 220, 210, 255),
    "fur_cream": (205, 198, 184, 255),
    "fur_shadow": (83, 88, 91, 255),
    "mark_dark": (36, 38, 40, 255),
    "eye_gold": (239, 177, 30, 255),
    "pupil": (20, 18, 15, 255),
    "nose_brown": (121, 68, 44, 255),
    "mouth_dark": (45, 20, 22, 255),
    "tongue_red": (168, 53, 54, 255),
    "tooth": (246, 239, 215, 255),
    "claw": (39, 37, 35, 255),
    "tail_black": (25, 27, 29, 255),
    "ear_inner": (132, 99, 93, 255),
}

PARENTS = {
    "body": "root",
    "body_fur": "body",
    "head": "root",
    "head_fur": "head",
    "jaw": "head",
    "left_ear": "head",
    "right_ear": "head",
    "front_left_leg": "root",
    "front_right_leg": "root",
    "rear_left_leg": "root",
    "rear_right_leg": "root",
    "tail_1": "root",
    "tail_2": "tail_1",
    "tail_3": "tail_2",
    "tail_4": "tail_3",
}

PIVOTS = {
    "root": (0, 0, 0),
    "body": (0, 17, 4),
    "body_fur": (0, 17, 4),
    "head": (0, 24, -9),
    "head_fur": (0, 24, -9),
    "jaw": (0, 18.8, -17.2),
    "left_ear": (-10.7, 29.0, -12.0),
    "right_ear": (10.7, 29.0, -12.0),
    "front_left_leg": (-7.8, 14.0, -8.5),
    "front_right_leg": (7.8, 14.0, -8.5),
    "rear_left_leg": (-8.7, 14.0, 12.5),
    "rear_right_leg": (8.7, 14.0, 12.5),
    "tail_1": (5.0, 12.5, 17.0),
    "tail_2": (6.5, 10.8, 25.0),
    "tail_3": (8.3, 9.0, 31.5),
    "tail_4": (9.5, 7.8, 37.0),
}


@dataclass
class Part:
    name: str
    group: str
    center: tuple[float, float, float]
    size: tuple[float, float, float]
    material: str
    rotation: tuple[float, float, float] = (0, 0, 0)
    pivot: tuple[float, float, float] | None = None
    uuid: str = field(init=False)

    def __post_init__(self) -> None:
        self.uuid = str(uuid.uuid5(NS, f"part:{self.name}"))


parts: list[Part] = []


def add(name: str, group: str, center: tuple[float, float, float],
        size: tuple[float, float, float], material: str = "fur_gray",
        rotation: tuple[float, float, float] = (0, 0, 0),
        pivot: tuple[float, float, float] | None = None) -> None:
    parts.append(Part(name, group, center, size, material, rotation, pivot))


def build_body() -> None:
    # Overlapping longitudinal masses produce a heavy barrel without a flat rear wall.
    add("body_belly", "body", (0, 12.0, 2.0), (21.5, 8.0, 25.0), "fur_light")
    add("shoulder_mass", "body", (0, 19.5, -7.0), (27.5, 16.0, 14.0), "fur_mid")
    add("torso_mid", "body", (0, 19.0, 4.5), (25.5, 15.5, 13.0), "fur_gray")
    for side, sign, material in (("left", -1, "fur_gray"), ("right", 1, "fur_mid")):
        add(f"{side}_haunch_upper", "body", (sign * 5.7, 23.0, 14.0),
            (11.0, 7.0, 13.0), material)
        add(f"{side}_haunch_mid", "body", (sign * 6.3, 18.0, 14.5),
            (13.0, 8.0, 14.0), material)
        add(f"{side}_haunch_lower", "body", (sign * 5.5, 12.5, 13.5),
            (10.0, 5.5, 11.5), "fur_light")
    add("rump_center", "body", (0, 18.0, 13.0), (7.0, 13.5, 12.0), "fur_gray")
    add("spine_mass", "body", (0, 26.0, 2.5), (20.5, 5.5, 25.0), "fur_mid")
    add("tail_root_saddle", "body", (5.0, 14.5, 18.5), (11.5, 10.0, 7.0), "fur_gray")
    add("chest_white", "body", (0, 13.5, -13.2), (17.5, 12.0, 3.0), "fur_light")

    fur_palette = ("fur_gray", "fur_mid", "fur_light", "fur_shadow")
    ridge_stations = ((-8.5, 2.3), (-1.0, 3.0), (7.0, 2.5), (14.5, 2.0))
    for row, (z, height) in enumerate(ridge_stations):
        for column, x in enumerate((-6.0, 0.0, 6.0)):
            add(f"back_cluster_{row}_{column}", "body_fur",
                (x, 28.4 + height / 2 + (0.35 if column == 1 else 0), z),
                (5.2, height, 4.8), fur_palette[(row + column) % 3])

    top_flank = ((-8.5, 24.0), (-1.5, 23.0), (5.5, 22.0), (12.0, 21.5), (17.0, 20.0))
    middle_flank = ((-7.5, 17.5), (0.5, 16.5), (8.0, 16.0), (15.0, 15.5))
    lower_fringe = ((-8.0, 4.8), (-1.0, 5.3), (6.0, 4.6), (12.5, 5.4), (17.0, 4.2))
    for side, sign in (("left", -1), ("right", 1)):
        for index, (z, y) in enumerate(top_flank):
            depth = 2.0 + (index % 2) * 0.45
            add(f"{side}_upper_flank_{index}", "body_fur",
                (sign * (13.0 + depth / 2), y, z),
                (depth, 4.0 + (index % 3) * 0.5, 4.8),
                fur_palette[(index + (1 if sign > 0 else 0)) % 3],
                rotation=(0, 0, sign * (4 + index * 1.5)))
        for index, (z, y) in enumerate(middle_flank):
            add(f"{side}_middle_flank_{index}", "body_fur",
                (sign * 13.45, y, z), (1.8, 4.5, 5.3),
                fur_palette[(index + 1) % len(fur_palette)],
                rotation=(0, 0, sign * (-3 + index * 2)))
        for index, (z, height) in enumerate(lower_fringe):
            add(f"{side}_belly_fringe_{index}", "body_fur",
                (sign * (9.0 + (index % 2) * 0.6), 8.0, z),
                (4.3, height, 4.8), "fur_light" if index % 2 else "fur_cream",
                rotation=(0, 0, sign * (2 if index % 2 else -3)))

        for index, (y, z) in enumerate(((23, -5), (20, 1), (17, 8), (14, 14))):
            add(f"{side}_body_mark_{index}", "body_fur", (sign * 14.15, y, z),
                (0.55, 2.1, 3.4), "fur_shadow", rotation=(0, 0, sign * (9 + index * 2)))

        for index, (x_offset, y, z) in enumerate(((11.5, 23.0, 16.0),
                                                   (12.2, 18.0, 19.0),
                                                   (10.8, 12.5, 19.8))):
            add(f"{side}_croup_tuft_{index}", "body_fur", (sign * x_offset, y, z),
                (3.2, 4.2 + index * 0.4, 4.0),
                ("fur_mid", "fur_gray", "fur_light")[index])

    for index, x in enumerate((-8, -4, 0, 4, 8)):
        add(f"chest_tuft_{index}", "body_fur", (x, 10.3 + (index % 2) * 0.7, -14.3),
            (4.2, 5.5 + (index % 3) * 0.8, 2.2), "fur_light" if index % 2 else "fur_white")


def build_head() -> None:
    add("head_back", "head", (0, 23.0, -12.8), (22.5, 16.0, 12.0), "fur_gray")
    add("head_face", "head", (0, 22.5, -18.2), (20.5, 13.5, 5.0), "fur_mid")
    add("head_crown", "head", (0, 30.0, -14.0), (21.0, 5.0, 12.0), "fur_mid")
    add("left_temple", "head", (-10.0, 24.0, -16.8), (6.5, 11.5, 7.0), "fur_gray")
    add("right_temple", "head", (10.0, 24.0, -16.8), (6.5, 11.5, 7.0), "fur_gray")
    add("left_cheek_mass", "head", (-8.5, 18.8, -18.9), (11.5, 8.5, 5.0), "fur_light")
    add("right_cheek_mass", "head", (8.5, 18.8, -18.9), (11.5, 8.5, 5.0), "fur_light")
    add("chin_mass", "head", (0, 15.5, -20.2), (11.0, 3.0, 5.0), "fur_cream")
    add("forehead_brow_mass", "head", (0, 27.7, -19.2), (14.0, 4.0, 3.0), "fur_mid")

    # Small, wide-set ears, partially buried by the crown coat.
    for side, sign in (("left", -1), ("right", 1)):
        group = f"{side}_ear"
        add(f"{side}_ear_outer", group, (sign * 10.9, 29.3, -17.8), (5.2, 5.2, 3.2),
            "fur_shadow", rotation=(0, 0, sign * 12), pivot=PIVOTS[group])
        add(f"{side}_ear_inner", group, (sign * 11.0, 29.1, -19.6), (3.0, 3.0, 0.6),
            "ear_inner", rotation=(0, 0, sign * 12), pivot=PIVOTS[group])
        add(f"{side}_ear_cap", "head_fur", (sign * 9.5, 31.0, -17.1), (5.2, 3.0, 4.2), "fur_light")

    # Eyes and brow planes reproduce the aggressive downward angle from the video.
    for side, sign in (("left", -1), ("right", 1)):
        x = sign * 4.9
        add(f"{side}_eye_outline", "head", (x, 23.8, -21.15), (4.5, 3.2, 0.8), "mark_dark")
        add(f"{side}_eye_gold", "head", (x, 23.7, -21.65), (3.5, 2.35, 0.55), "eye_gold")
        add(f"{side}_pupil", "head", (x, 23.7, -22.02), (0.7, 2.25, 0.35), "pupil")
        add(f"{side}_brow", "head_fur", (x - sign * 0.35, 25.65, -21.95), (4.3, 0.72, 0.55),
            "mark_dark", rotation=(0, 0, sign * 8))
        add(f"{side}_brow_fur", "head_fur", (x - sign * 0.55, 26.55, -21.55), (4.0, 0.9, 0.7),
            "fur_white", rotation=(0, 0, sign * 9))

    add("nose_bridge", "head", (0, 22.5, -20.5), (3.6, 5.0, 2.0), "fur_mid")
    add("nose", "head", (0, 20.2, -23.5), (3.2, 2.1, 1.4), "nose_brown")
    add("nose_drop", "head", (0, 18.85, -23.45), (0.9, 1.2, 0.8), "nose_brown")
    add("left_muzzle", "head", (-3.2, 18.5, -21.7), (5.5, 4.2, 3.4), "fur_white")
    add("right_muzzle", "head", (3.2, 18.5, -21.7), (5.5, 4.2, 3.4), "fur_white")
    add("left_muzzle_outer", "head", (-6.2, 18.9, -21.4), (2.5, 3.4, 2.7), "fur_cream")
    add("right_muzzle_outer", "head", (6.2, 18.9, -21.4), (2.5, 3.4, 2.7), "fur_cream")
    add("mouth_center", "head", (0, 17.0, -23.75), (4.2, 0.6, 0.45), "mouth_dark")
    add("mouth_inner_upper", "head", (0, 15.9, -21.0), (7.0, 1.5, 0.8), "mouth_dark")
    add("upper_fang_left", "head", (-2.7, 15.8, -21.5), (0.8, 1.8, 0.65), "tooth")
    add("upper_fang_right", "head", (2.7, 15.8, -21.5), (0.8, 1.8, 0.65), "tooth")

    # Separate lower jaw, tongue and lower canines support the attack pose.
    add("jaw_outer", "jaw", (0, 14.6, -20.2), (9.0, 3.4, 4.6), "fur_cream")
    add("jaw_inner", "jaw", (0, 15.8, -22.0), (6.6, 1.8, 1.0), "mouth_dark")
    add("tongue", "jaw", (0, 14.8, -21.8), (4.8, 0.9, 1.0), "tongue_red")
    add("lower_fang_left", "jaw", (-3.0, 16.5, -22.45), (0.8, 1.8, 0.65), "tooth")
    add("lower_fang_right", "jaw", (3.0, 16.5, -22.45), (0.8, 1.8, 0.65), "tooth")

    # Forehead spots and cheek stripes are modeled planes, not baked shading.
    forehead_marks = ((-5.4, 29.2), (-3.0, 30.5), (-0.8, 29.0), (1.6, 30.6), (4.2, 29.4),
                      (-4.2, 27.7), (-1.8, 27.4), (0.6, 28.2), (3.1, 27.5))
    for index, (x, y) in enumerate(forehead_marks):
        add(f"forehead_spot_{index}", "head_fur", (x, y, -21.1),
            (1.1 + (index % 2) * 0.45, 1.0, 0.45), "mark_dark")

    for row, y in enumerate((28.0, 30.4)):
        for column, x in enumerate((-8.5, -6, -3.5, 0, 3.5, 6, 8.5)):
            add(f"forehead_fur_{row}_{column}", "head_fur", (x, y, -20.75),
                (2.25, 1.8, 0.7), ("fur_gray", "fur_light", "fur_mid")[(row + column) % 3])

    face_pelt = ((-9, 26.0), (9, 26.0), (-9.5, 23.2), (9.5, 23.2),
                 (-9.5, 20.4), (9.5, 20.4), (-7.8, 28.2), (7.8, 28.2),
                 (-6.8, 21.7), (6.8, 21.7), (-7.2, 18.0), (7.2, 18.0))
    for index, (x, y) in enumerate(face_pelt):
        add(f"face_pelt_{index}", "head_fur", (x, y, -21.15),
            (2.1, 2.2, 0.75), ("fur_gray", "fur_light", "fur_mid")[index % 3])

    for side, sign in (("left", -1), ("right", 1)):
        for index in range(3):
            add(f"{side}_cheek_stripe_{index}", "head_fur",
                 (sign * (8.0 + index * 1.0), 21.6 - index * 1.75, -22.25),
                 (4.7 - index * 0.35, 1.0, 0.6), "mark_dark",
                 rotation=(0, 0, sign * -(23 + index * 4)))
        for row, y in enumerate((27.0, 24.0, 21.0, 18.0)):
            for column, x_offset in enumerate((8.8, 11.1)):
                add(f"{side}_cheek_tuft_{row}_{column}", "head_fur",
                    (sign * x_offset, y + (column * 0.45), -21.0 - column * 0.4),
                    (2.3, 2.4, 1.5), "fur_white" if (row + column) % 2 else "fur_light",
                    rotation=(0, 0, sign * (5 + row * 3)))
        for index in range(5):
            length = 7.0 + index * 0.65
            add(f"{side}_whisker_{index}", "head_fur",
                 (sign * (5.0 + length / 2), 19.0 - index * 0.6, -23.0),
                 (length, 0.16, 0.16), "tooth", rotation=(0, 0, sign * (-4 - index * 2)))

    for row, (y, z) in enumerate(((32.0, -12.5), (30.5, -16.0), (28.8, -19.0))):
        for column, x in enumerate((-9, -6, -3, 0, 3, 6, 9)):
            add(f"crown_tuft_{row}_{column}", "head_fur", (x, y, z),
                (2.6, 2.5 + ((row + column) % 2) * 0.5, 2.6),
                ("fur_mid", "fur_light", "fur_gray")[(row + column) % 3])


def build_legs() -> None:
    specs = (
        ("front_left_leg", -7.8, -8.5, 7.8),
        ("front_right_leg", 7.8, -8.5, 7.8),
        ("rear_left_leg", -8.7, 12.5, 7.2),
        ("rear_right_leg", 8.7, 12.5, 7.2),
    )
    for group, x, z, width in specs:
        front = group.startswith("front")
        sign = -1 if x < 0 else 1
        add(f"{group}_hip", group, (x + sign * 0.8, 13.0, z + 0.4),
            (width + 1.5, 5.0, 8.0), "fur_mid" if front else "fur_gray", pivot=PIVOTS[group])
        add(f"{group}_upper", group, (x, 10.0, z), (width, 8.5, 7.0),
            "fur_light" if front else "fur_mid", pivot=PIVOTS[group])
        add(f"{group}_shin", group, (x, 6.5, z - 0.8),
            (6.4 if front else 6.0, 7.5, 6.2), "fur_cream", pivot=PIVOTS[group])
        add(f"{group}_paw", group, (x, 2.8, z - 2.2), (8.5 if front else 7.8, 4.6, 8.6),
            "fur_white" if front else "fur_cream", pivot=PIVOTS[group])
        toe_specs = ((-2.6, 0.75, -0.1), (0, 0.9, -0.3), (2.5, 0.7, 0.1))
        for index, (offset, toe_width, z_offset) in enumerate(toe_specs):
            add(f"{group}_toe_{index}", group, (x + offset * 0.82, 2.35, z - 6.55 + z_offset),
                (toe_width, 1.8, 0.55), "claw", pivot=PIVOTS[group])
        for index in range(2):
            add(f"{group}_mark_{index}", group, (x + (-1.4 if index == 0 else 1.4), 7.0, z - 3.95),
                (1.2, 2.2, 0.45), "fur_shadow", pivot=PIVOTS[group])
        for index, y in enumerate((7.0, 10.5)):
            add(f"{group}_fur_{index}", group, (x + (-1 if index else 1), y, z - 3.8),
                (3.2, 3.0, 1.0), "fur_light" if index else "fur_mid", pivot=PIVOTS[group])
        if not front:
            add(f"{group}_rear_plane", group, (x, 7.4, z + 3.45),
                (4.4, 5.0, 0.55), "fur_shadow", pivot=PIVOTS[group])


def build_tail() -> None:
    segments = (
        ("tail_1", (5.5, 11.8, 21.0), (10.5, 9.2, 9.0), "fur_gray", (0, 5, 0)),
        ("tail_2", (7.5, 10.0, 27.5), (9.5, 8.4, 8.2), "fur_light", (0, 4, 0)),
        ("tail_3", (9.0, 8.4, 33.5), (8.3, 7.6, 7.4), "fur_gray", (0, -4, 0)),
        ("tail_4", (10.0, 7.2, 39.0), (7.2, 6.8, 6.8), "tail_black", (0, -7, 0)),
    )
    for group, center, size, material, rotation in segments:
        add(f"{group}_core", group, center, size, material, rotation=rotation, pivot=PIVOTS[group])
        if group != "tail_4":
            add(f"{group}_ring", group, (center[0], center[1], center[2] + size[2] * 0.28),
                (size[0] + 0.25, size[1] + 0.25, 1.6), "fur_shadow", rotation=rotation, pivot=PIVOTS[group])
        for tuft, offset in enumerate((-0.28, 0, 0.28)):
            add(f"{group}_tuft_{tuft}", group,
                 (center[0] + offset * size[0], center[1] + size[1] / 2 + 0.55,
                  center[2] + (0.5 if tuft == 1 else -0.35)),
                 (2.1, 1.3 + (tuft % 2) * 0.4, 3.0), material,
                 rotation=rotation, pivot=PIVOTS[group])


def build_parts() -> None:
    if parts:
        return
    build_body()
    build_head()
    build_legs()
    build_tail()


def group_uuid(name: str) -> str:
    return str(uuid.uuid5(NS, f"group:{name}"))


def texture_uv(material: str) -> list[int]:
    index = list(COLORS).index(material)
    x = (index % 8) * TILE
    y = (index // 8) * TILE
    return [x, y, x + TILE, y + TILE]


def make_texture(path: Path, coat: str = "steppe") -> None:
    if Image is None or ImageDraw is None:
        raise RuntimeError("Pillow is required to generate the texture atlas")
    image = Image.new("RGBA", (ATLAS, ATLAS), (0, 0, 0, 0))
    draw = ImageDraw.Draw(image)
    for index, (name, rgba) in enumerate(COLORS.items()):
        r, g, b, a = rgba
        if name.startswith("fur_"):
            if coat == "mountain":
                r, g, b = min(255, r + 16), min(255, g + 19), min(255, b + 24)
            elif coat == "sand":
                r, g, b = min(255, r + 22), min(255, g + 13), max(0, b - 12)
            elif coat == "silver":
                gray = round(r * 0.25 + g * 0.55 + b * 0.20)
                r, g, b = gray, min(255, gray + 4), min(255, gray + 8)
        x = (index % 8) * TILE
        y = (index // 8) * TILE
        color = (r, g, b, a)
        draw.rectangle((x, y, x + TILE - 1, y + TILE - 1), fill=color)
        accent = (min(255, r + 10), min(255, g + 10), min(255, b + 10), a)
        shade = (max(0, r - 9), max(0, g - 9), max(0, b - 9), a)
        for px, py in ((2, 3), (11, 2), (6, 8), (13, 12)):
            draw.rectangle((x + px, y + py, x + px + 1, y + py + 1), fill=accent)
        for px, py in ((4, 12), (9, 5)):
            draw.point((x + px, y + py), fill=shade)
    image.save(path)


def cube_json(part: Part) -> dict:
    x, y, z = part.center
    sx, sy, sz = part.size
    uv = texture_uv(part.material)
    return {
        "name": part.name,
        "box_uv": False,
        "render_order": "default",
        "locked": False,
        "export": True,
        "scope": 0,
        "allow_mirror_modeling": True,
        "from": [round(x - sx / 2, 3), round(y - sy / 2, 3), round(z - sz / 2, 3)],
        "to": [round(x + sx / 2, 3), round(y + sy / 2, 3), round(z + sz / 2, 3)],
        "autouv": 0,
        "color": list(COLORS).index(part.material) % 8,
        "origin": list(part.pivot or part.center),
        "rotation": list(part.rotation),
        "faces": {face: {"uv": uv, "texture": 0} for face in ("north", "east", "south", "west", "up", "down")},
        "type": "cube",
        "uuid": part.uuid,
    }


def children_for(group: str) -> list[str]:
    return [name for name, parent in PARENTS.items() if parent == group]


def outliner(group: str) -> dict:
    direct = [part.uuid for part in parts if part.group == group]
    return {
        "uuid": group_uuid(group),
        "isOpen": group in ("root", "head"),
        "children": direct + [outliner(child) for child in children_for(group)],
    }


def keyframe(channel: str, time: float, values: tuple[float, float, float], name: str) -> dict:
    return {
        "channel": channel,
        "data_points": [{"x": str(values[0]), "y": str(values[1]), "z": str(values[2])}],
        "uuid": str(uuid.uuid5(NS, f"key:{name}:{channel}:{time}")),
        "time": time,
        "color": -1,
        "interpolation": "linear",
    }


def animation(name: str, length: float, loop: bool,
              tracks: dict[str, list[tuple[str, float, tuple[float, float, float]]]]) -> dict:
    animators = {}
    for bone, values in tracks.items():
        animators[group_uuid(bone)] = {
            "name": bone,
            "type": "bone",
            "rotation_global": False,
            "quaternion_interpolation": False,
            "keyframes": [keyframe(channel, time, vector, f"{name}:{bone}") for channel, time, vector in values],
        }
    return {
        "uuid": str(uuid.uuid5(NS, f"animation:{name}")),
        "name": f"animation.manul.{name}",
        "loop": "loop" if loop else "once",
        "override": False,
        "length": length,
        "snapping": 24,
        "selected": False,
        "group_name": "",
        "scope": 0,
        "anim_time_update": "",
        "blend_weight": "",
        "start_delay": "",
        "loop_delay": "",
        "animators": animators,
    }


def blockbench_animations() -> list[dict]:
    return [
        animation("idle", 3, True, {
            "body": [("position", 0, (0, 0, 0)), ("position", 1.5, (0, 0.25, 0)), ("position", 3, (0, 0, 0))],
            "tail_2": [("rotation", 0, (0, -4, 0)), ("rotation", 1.5, (0, 4, 0)), ("rotation", 3, (0, -4, 0))],
        }),
        animation("walk", 1, True, {
            "front_left_leg": [("rotation", 0, (20, 0, 0)), ("rotation", 0.5, (-20, 0, 0)), ("rotation", 1, (20, 0, 0))],
            "front_right_leg": [("rotation", 0, (-20, 0, 0)), ("rotation", 0.5, (20, 0, 0)), ("rotation", 1, (-20, 0, 0))],
            "rear_left_leg": [("rotation", 0, (-16, 0, 0)), ("rotation", 0.5, (16, 0, 0)), ("rotation", 1, (-16, 0, 0))],
            "rear_right_leg": [("rotation", 0, (16, 0, 0)), ("rotation", 0.5, (-16, 0, 0)), ("rotation", 1, (16, 0, 0))],
        }),
        animation("attack", 0.65, False, {
            "jaw": [("rotation", 0, (0, 0, 0)), ("rotation", 0.22, (-38, 0, 0)), ("rotation", 0.65, (0, 0, 0))],
            "front_right_leg": [("rotation", 0, (0, 0, 0)), ("rotation", 0.22, (-62, 0, 0)), ("rotation", 0.65, (0, 0, 0))],
            "head": [("position", 0, (0, 0, 0)), ("position", 0.22, (0, 1.2, -2.0)), ("position", 0.65, (0, 0, 0))],
        }),
    ]


def make_bbmodel() -> None:
    build_parts()
    make_texture(TEXTURE_PATH)
    texture_source = "data:image/png;base64," + base64.b64encode(TEXTURE_PATH.read_bytes()).decode("ascii")
    groups = []
    for name in ("root", *PARENTS.keys()):
        groups.append({
            "name": name,
            "uuid": group_uuid(name),
            "export": True,
            "locked": False,
            "scope": 0,
            "selected": False,
            "origin": list(PIVOTS[name]),
            "rotation": [0, 0, 0],
            "color": 0,
            "children": [],
            "reset": False,
            "shade": True,
            "mirror_uv": False,
            "visibility": True,
            "autouv": 0,
            "isOpen": name in ("root", "head"),
            "primary_selected": False,
        })
    model = {
        "meta": {"format_version": "5.0", "model_format": "free", "box_uv": False},
        "name": "manul_production",
        "model_identifier": "manul",
        "visible_box": [4, 3, 0],
        "variable_placeholders": "",
        "multi_file_ruleset": "",
        "variable_placeholder_buttons": [],
        "timeline_setups": [],
        "unhandled_root_fields": {},
        "resolution": {"width": ATLAS, "height": ATLAS},
        "elements": [cube_json(part) for part in parts],
        "groups": groups,
        "outliner": [outliner("root")],
        "textures": [{
            "name": TEXTURE_PATH.stem,
            "path": TEXTURE_PATH.name,
            "folder": "",
            "namespace": "kubanhorizons",
            "id": "0",
            "group": "",
            "scope": 0,
            "width": ATLAS,
            "height": ATLAS,
            "uv_width": ATLAS,
            "uv_height": ATLAS,
            "particle": False,
            "use_as_default": True,
            "layers_enabled": False,
            "sync_to_project": "",
            "file_format": "png",
            "render_mode": "default",
            "render_sides": "auto",
            "wrap_mode": "limited",
            "pbr_channel": "color",
            "fps": 1,
            "frame_time": 1,
            "frame_order_type": "loop",
            "frame_order": "",
            "frame_interpolate": False,
            "visible": True,
            "internal": True,
            "saved": True,
            "uuid": str(uuid.uuid5(NS, "texture:manul_texture.png")),
            "source": texture_source,
        }],
        "animations": blockbench_animations(),
    }
    MODEL_PATH.write_text(json.dumps(model, separators=(",", ":")), encoding="utf-8")


def number(value: float) -> int | float:
    rounded = round(float(value), 4)
    return int(rounded) if rounded.is_integer() else rounded


def make_game_assets() -> None:
    build_parts()
    TEXTURE_DIR.mkdir(parents=True, exist_ok=True)
    for coat in ("steppe", "mountain", "sand", "silver"):
        make_texture(TEXTURE_DIR / f"manul_{coat}.png", coat)

    by_group: dict[str, list[Part]] = {}
    for part in parts:
        by_group.setdefault(part.group, []).append(part)
    bones = []
    for name in ("root", *PARENTS.keys()):
        bone = {"name": name, "pivot": [number(value) for value in PIVOTS[name]]}
        if name in PARENTS:
            bone["parent"] = PARENTS[name]
        cubes = []
        for part in by_group.get(name, []):
            x, y, z = part.center
            sx, sy, sz = part.size
            uv = texture_uv(part.material)
            cube = {
                "origin": [number(x - sx / 2), number(y - sy / 2), number(z - sz / 2)],
                "size": [number(sx), number(sy), number(sz)],
                "uv": {face: {"uv": uv[:2], "uv_size": [TILE, TILE]}
                       for face in ("north", "east", "south", "west", "up", "down")},
            }
            if any(abs(value) > 0.001 for value in part.rotation):
                cube["pivot"] = [number(value) for value in (part.pivot or part.center)]
                cube["rotation"] = [number(value) for value in part.rotation]
            cubes.append(cube)
        if cubes:
            bone["cubes"] = cubes
        bones.append(bone)
    geometry = {
        "format_version": "1.12.0",
        "minecraft:geometry": [{
            "description": {
                "identifier": "geometry.manul",
                "texture_width": ATLAS,
                "texture_height": ATLAS,
                "visible_bounds_width": 4,
                "visible_bounds_height": 3,
                "visible_bounds_offset": [0, 1.2, 0],
            },
            "bones": bones,
        }],
    }
    GEO_PATH.write_text(json.dumps(geometry, separators=(",", ":")) + "\n", encoding="utf-8")

    animations = {
        "format_version": "1.8.0",
        "animations": {
            "animation.manul.idle": {
                "loop": True,
                "animation_length": 3,
                "bones": {
                    "body": {"position": {"0.0": [0, 0, 0], "1.5": [0, 0.25, 0], "3.0": [0, 0, 0]}},
                    "tail_2": {"rotation": {"0.0": [0, -4, 0], "1.5": [0, 4, 0], "3.0": [0, -4, 0]}},
                },
            },
            "animation.manul.walk": {
                "loop": True,
                "animation_length": 1,
                "bones": {
                    "front_left_leg": {"rotation": {"0.0": [20, 0, 0], ".5": [-20, 0, 0], "1.0": [20, 0, 0]}},
                    "front_right_leg": {"rotation": {"0.0": [-20, 0, 0], ".5": [20, 0, 0], "1.0": [-20, 0, 0]}},
                    "rear_left_leg": {"rotation": {"0.0": [-16, 0, 0], ".5": [16, 0, 0], "1.0": [-16, 0, 0]}},
                    "rear_right_leg": {"rotation": {"0.0": [16, 0, 0], ".5": [-16, 0, 0], "1.0": [16, 0, 0]}},
                },
            },
            "animation.manul.sit": {
                "loop": True,
                "animation_length": 3,
                "bones": {
                    "body": {"position": [0, -1.8, 1.5], "rotation": [6, 0, 0]},
                    "rear_left_leg": {"rotation": [28, 0, 0]},
                    "rear_right_leg": {"rotation": [28, 0, 0]},
                    "tail_1": {"rotation": [0, 20, 0]},
                },
            },
            "animation.manul.sleep": {
                "loop": True,
                "animation_length": 4,
                "bones": {
                    "body": {"position": [0, -3.2, 0], "rotation": [0, 0, 4]},
                    "head": {"position": [0, -2.0, -1.0], "rotation": [12, 18, 5]},
                    "tail_1": {"rotation": [0, 36, 12]},
                },
            },
            "animation.manul.hiss": {
                "loop": True,
                "animation_length": 0.65,
                "bones": {
                    "jaw": {"rotation": {"0.0": [0, 0, 0], ".2": [-38, 0, 0], ".5": [-34, 0, 0], ".65": [0, 0, 0]}},
                    "head": {"position": {"0.0": [0, 0, 0], ".2": [0, 0.5, -1], ".65": [0, 0, 0]}},
                    "left_ear": {"rotation": [0, 0, -22]},
                    "right_ear": {"rotation": [0, 0, 22]},
                },
            },
            "animation.manul.attack": {
                "loop": False,
                "animation_length": 0.65,
                "bones": {
                    "jaw": {"rotation": {"0.0": [0, 0, 0], ".22": [-38, 0, 0], ".65": [0, 0, 0]}},
                    "front_right_leg": {"rotation": {"0.0": [0, 0, 0], ".22": [-62, 0, 0], ".65": [0, 0, 0]}},
                    "head": {"position": {"0.0": [0, 0, 0], ".22": [0, 1.2, -2], ".65": [0, 0, 0]}},
                },
            },
        },
    }
    ANIMATION_PATH.write_text(json.dumps(animations, indent=2) + "\n", encoding="utf-8")


def main() -> None:
    make_bbmodel()
    make_game_assets()
    print(f"wrote {MODEL_PATH.name}: {len(parts)} cuboids, {len(PARENTS) + 1} bones")


if __name__ == "__main__":
    main()
