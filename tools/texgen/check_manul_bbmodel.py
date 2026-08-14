"""Validate the edited manul project without requiring Blockbench internals."""

import base64
import io
import json
from pathlib import Path

from PIL import Image


ROOT = Path(__file__).resolve().parents[2]
MODEL = ROOT / "manul.bbmodel"


def collect_children(nodes):
    element_ids = []
    group_ids = []
    for node in nodes:
        if isinstance(node, str):
            element_ids.append(node)
        else:
            group_ids.append(node["uuid"])
            nested_elements, nested_groups = collect_children(node.get("children", []))
            element_ids.extend(nested_elements)
            group_ids.extend(nested_groups)
    return element_ids, group_ids


def main():
    model = json.loads(MODEL.read_text(encoding="utf-8"))
    assert model["meta"]["model_format"] == "free"
    assert len(model["textures"]) == 1

    texture_sizes = []
    for texture in model["textures"]:
        if path := texture.get("path"):
            assert not Path(path).is_absolute()
            texture_path = ROOT / path
            assert texture_path.exists(), texture_path
            source = texture_path
        else:
            prefix, encoded = texture["source"].split(",", 1)
            assert prefix == "data:image/png;base64"
            source = io.BytesIO(base64.b64decode(encoded))
        with Image.open(source) as image:
            texture_sizes.append(image.size)

    elements = model["elements"]
    element_ids = {element["uuid"] for element in elements}
    group_ids = {group["uuid"] for group in model["groups"]}
    outlined_elements, outlined_groups = collect_children(model["outliner"])
    assert set(outlined_elements) == element_ids
    assert set(outlined_groups) == group_ids
    assert len(outlined_elements) == len(set(outlined_elements))
    assert len(outlined_groups) == len(set(outlined_groups))

    for element in elements:
        assert all(a < b for a, b in zip(element["from"], element["to"])), element["name"]
        for face in element["faces"].values():
            texture_index = face["texture"]
            assert 0 <= texture_index < len(texture_sizes), element["name"]
            width, height = texture_sizes[texture_index]
            u0, v0, u1, v1 = face["uv"]
            assert 0 <= min(u0, u1) <= max(u0, u1) <= width, element["name"]
            assert 0 <= min(v0, v1) <= max(v0, v1) <= height, element["name"]

    required = {
        "root", "body", "body_fur", "head", "head_fur", "jaw", "left_ear", "right_ear",
        "front_left_leg", "front_right_leg", "rear_left_leg", "rear_right_leg",
        "tail_1", "tail_2", "tail_3", "tail_4",
    }
    assert required == {group["name"] for group in model["groups"]}
    assert {animation["name"] for animation in model["animations"]} == {
        "animation.manul.idle", "animation.manul.walk", "animation.manul.attack"
    }
    print(f"ok: {len(elements)} cuboids, {len(group_ids)} groups, textures={texture_sizes}")


if __name__ == "__main__":
    main()
