"""Export the edited Blockbench manul geometry to GeckoLib without rebuilding it."""

from __future__ import annotations

import json
from collections import defaultdict
from pathlib import Path

import manul_blockbench_model as manul


ROOT = Path(__file__).resolve().parents[2]
GEO_PATH = ROOT / "src/main/resources/assets/kubanhorizons/geckolib/models/manul.geo.json"


def number(value: float) -> int | float:
    rounded = round(float(value), 4)
    return int(rounded) if rounded.is_integer() else rounded


def cube_entry(part: manul.Part) -> dict:
    cube = {
        "origin": [number(value) for value in part.from_],
        "size": [number(value) for value in part.size],
        "uv": {},
    }
    for name, face in part.faces.items():
        u0, v0, u1, v1 = face["uv"]
        cube["uv"][name] = {
            "uv": [number(u0), number(v0)],
            "uv_size": [number(u1 - u0), number(v1 - v0)],
        }
    if any(abs(value) > 0.001 for value in part.rotation):
        cube["pivot"] = [number(value) for value in part.pivot]
        cube["rotation"] = [number(value) for value in part.rotation]
    return cube


def main() -> None:
    parts_by_group = defaultdict(list)
    for part in manul.parts:
        parts_by_group[part.group].append(part)

    bones = []
    for name, pivot in manul.PIVOTS.items():
        bone = {"name": name, "pivot": [number(value) for value in pivot]}
        if name in manul.PARENTS:
            bone["parent"] = manul.PARENTS[name]
        cubes = parts_by_group.get(name)
        if cubes:
            bone["cubes"] = [cube_entry(part) for part in cubes]
        bones.append(bone)

    geometry = {
        "format_version": "1.12.0",
        "minecraft:geometry": [{
            "description": {
                "identifier": "geometry.manul",
                "texture_width": manul.ATLAS,
                "texture_height": manul.ATLAS,
                "visible_bounds_width": 4,
                "visible_bounds_height": 3,
                "visible_bounds_offset": [0, 1.2, 0],
            },
            "bones": bones,
        }],
    }
    GEO_PATH.write_text(json.dumps(geometry, separators=(",", ":")) + "\n", encoding="utf-8")
    print(f"wrote {GEO_PATH.name}: {len(manul.parts)} cuboids, {len(bones)} bones")


if __name__ == "__main__":
    main()
