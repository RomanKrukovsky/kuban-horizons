"""Load the edited Blockbench manul as the geometry source of truth."""

from __future__ import annotations

import json
from dataclasses import dataclass
from pathlib import Path

from gen_manul_production import ATLAS, COLORS, TILE


ROOT = Path(__file__).resolve().parents[2]
MODEL_PATH = ROOT / "manul.bbmodel"


@dataclass(frozen=True)
class Part:
    name: str
    group: str
    center: tuple[float, float, float]
    size: tuple[float, float, float]
    material: str
    rotation: tuple[float, float, float]
    pivot: tuple[float, float, float]
    from_: tuple[float, float, float]
    faces: dict[str, dict]


def _model_hierarchy(model: dict) -> tuple[dict[str, str], dict[str, str]]:
    groups_by_id = {group["uuid"]: group["name"] for group in model["groups"]}
    element_groups: dict[str, str] = {}
    parents: dict[str, str] = {}

    def visit(node: str | dict, parent: str | None = None) -> None:
        if isinstance(node, str):
            if parent is None:
                raise ValueError(f"Element {node} is outside a group")
            element_groups[node] = parent
            return

        name = groups_by_id[node["uuid"]]
        if parent is not None:
            parents[name] = parent
        for child in node.get("children", []):
            visit(child, name)

    for node in model["outliner"]:
        visit(node)
    return element_groups, parents


def _material_name(element: dict) -> str:
    face = next(iter(element["faces"].values()))
    u, v = face["uv"][:2]
    index = int(v // TILE) * 8 + int(u // TILE)
    materials = tuple(COLORS)
    if index >= len(materials):
        raise ValueError(f"Unknown material UV on {element['name']}: {face['uv']}")
    return materials[index]


def load_model(path: Path = MODEL_PATH) -> tuple[list[Part], dict[str, tuple[float, float, float]], dict[str, str]]:
    model = json.loads(path.read_text(encoding="utf-8"))
    element_groups, parents = _model_hierarchy(model)
    pivots = {group["name"]: tuple(group.get("origin", (0, 0, 0))) for group in model["groups"]}
    parts = []
    for element in model["elements"]:
        from_ = tuple(element["from"])
        to = tuple(element["to"])
        center = tuple((start + end) / 2 for start, end in zip(from_, to))
        size = tuple(end - start for start, end in zip(from_, to))
        parts.append(Part(
            name=element["name"],
            group=element_groups[element["uuid"]],
            center=center,
            size=size,
            material=_material_name(element),
            rotation=tuple(element.get("rotation", (0, 0, 0))),
            pivot=tuple(element.get("origin", center)),
            from_=from_,
            faces=element["faces"],
        ))
    return parts, pivots, parents


parts, PIVOTS, PARENTS = load_model()
