"""Render and export the authored production manul with Blender."""

from __future__ import annotations

import math
import sys
from pathlib import Path

import bpy
from mathutils import Euler, Matrix, Vector


ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(Path(__file__).resolve().parent))
import manul_blockbench_model as manul


OUTPUT = ROOT / "manul_checks"
OUTPUT.mkdir(exist_ok=True)
GLB_PATH = ROOT / "manul_game.glb"
CORNERS = ((-1, -1, -1), (1, -1, -1), (1, 1, -1), (-1, 1, -1),
           (-1, -1, 1), (1, -1, 1), (1, 1, 1), (-1, 1, 1))
FACES = ((0, 1, 2, 3), (4, 7, 6, 5), (0, 4, 5, 1),
         (1, 5, 6, 2), (2, 6, 7, 3), (4, 0, 3, 7))


def rotation_matrix(rotation: tuple[float, float, float]) -> Matrix:
    return Euler(tuple(math.radians(value) for value in rotation), "XYZ").to_matrix().to_4x4()


def group_matrix(group: str, pose: dict[str, dict], cache: dict[str, Matrix]) -> Matrix:
    if group in cache:
        return cache[group]
    data = pose.get(group, {})
    pivot = Vector(manul.PIVOTS[group])
    rotation = rotation_matrix(data.get("rotation", (0, 0, 0)))
    translation = Matrix.Translation(Vector(data.get("position", (0, 0, 0))))
    local = translation @ Matrix.Translation(pivot) @ rotation @ Matrix.Translation(-pivot)
    parent = manul.PARENTS.get(group)
    total = group_matrix(parent, pose, cache) @ local if parent else local
    cache[group] = total
    return total


def part_vertices(part: manul.Part, pose: dict[str, dict]) -> list[tuple[float, float, float]]:
    cache: dict[str, Matrix] = {}
    group_transform = group_matrix(part.group, pose, cache)
    pivot = Vector(part.pivot or part.center)
    part_transform = Matrix.Translation(pivot) @ rotation_matrix(part.rotation) @ Matrix.Translation(-pivot)
    center = Vector(part.center)
    sx, sy, sz = part.size
    vertices = []
    for cx, cy, cz in CORNERS:
        point = center + Vector((cx * sx / 2, cy * sy / 2, cz * sz / 2))
        point = group_transform @ (part_transform @ point)
        vertices.append((point.x, -point.z, point.y))
    return vertices


def material_set() -> dict[str, bpy.types.Material]:
    result = {}
    for name, rgba in manul.COLORS.items():
        material = bpy.data.materials.new(name)
        color = tuple(channel / 255 for channel in rgba)
        material.diffuse_color = color
        material.roughness = 0.9
        material.use_nodes = True
        material.node_tree.nodes["Principled BSDF"].inputs["Base Color"].default_value = color
        material.node_tree.nodes["Principled BSDF"].inputs["Roughness"].default_value = 0.9
        result[name] = material
    return result


def create_model(pose: dict[str, dict] | None = None, hierarchy: bool = False) -> list[bpy.types.Object]:
    pose = pose or {}
    materials = material_set()
    group_objects = {}
    if hierarchy:
        for name in ("root", *manul.PARENTS.keys()):
            empty = bpy.data.objects.new(name, None)
            empty.empty_display_type = "PLAIN_AXES"
            empty.location = (manul.PIVOTS[name][0], -manul.PIVOTS[name][2], manul.PIVOTS[name][1])
            bpy.context.collection.objects.link(empty)
            group_objects[name] = empty
        for name, parent in manul.PARENTS.items():
            group_objects[name].parent = group_objects[parent]
            group_objects[name].matrix_parent_inverse = group_objects[parent].matrix_world.inverted()

    objects = []
    for part in manul.parts:
        mesh = bpy.data.meshes.new(part.name)
        mesh.from_pydata(part_vertices(part, pose), [], FACES)
        mesh.materials.append(materials[part.material])
        obj = bpy.data.objects.new(part.name, mesh)
        bpy.context.collection.objects.link(obj)
        if hierarchy:
            obj.parent = group_objects[part.group]
            obj.matrix_parent_inverse = group_objects[part.group].matrix_world.inverted()
        objects.append(obj)
    return objects


def clear_scene() -> None:
    bpy.ops.object.select_all(action="SELECT")
    bpy.ops.object.delete(use_global=False)
    for mesh in list(bpy.data.meshes):
        if mesh.users == 0:
            bpy.data.meshes.remove(mesh)
    for collection in (bpy.data.materials, bpy.data.cameras, bpy.data.lights):
        for datablock in list(collection):
            if datablock.users == 0:
                collection.remove(datablock)


def aim(camera: bpy.types.Object, location: tuple[float, float, float],
        target: tuple[float, float, float] = (0, 0, 15)) -> None:
    camera.location = location
    direction = Vector(target) - camera.location
    camera.rotation_euler = direction.to_track_quat("-Z", "Y").to_euler()


def setup_stage() -> bpy.types.Object:
    world = bpy.context.scene.world
    world.use_nodes = True
    world.node_tree.nodes["Background"].inputs["Color"].default_value = (1, 1, 1, 1)
    world.node_tree.nodes["Background"].inputs["Strength"].default_value = 0.45

    bpy.ops.mesh.primitive_plane_add(size=180, location=(0, 0, -0.05))
    ground = bpy.context.object
    ground.name = "neutral_white_ground"
    material = bpy.data.materials.new("neutral_white")
    material.diffuse_color = (1, 1, 1, 1)
    material.roughness = 1
    ground.data.materials.append(material)

    bpy.ops.object.light_add(type="AREA", location=(-32, -38, 58))
    bpy.context.object.data.energy = 800
    bpy.context.object.data.size = 34
    bpy.ops.object.light_add(type="AREA", location=(35, 20, 38))
    bpy.context.object.data.energy = 400
    bpy.context.object.data.size = 26

    bpy.ops.object.camera_add()
    camera = bpy.context.object
    camera.data.type = "ORTHO"
    camera.data.ortho_scale = 64
    bpy.context.scene.camera = camera

    scene = bpy.context.scene
    scene.render.engine = "BLENDER_EEVEE"
    scene.render.resolution_x = 1024
    scene.render.resolution_y = 1024
    scene.render.resolution_percentage = 100
    scene.render.image_settings.file_format = "PNG"
    scene.render.film_transparent = False
    scene.view_settings.look = "AgX - Medium High Contrast"
    scene.view_settings.exposure = 0.0
    return camera


def render_pose(filename: str, pose: dict[str, dict], camera_location: tuple[float, float, float],
                target: tuple[float, float, float] = (0, 0, 15), ortho_scale: float = 64) -> None:
    clear_scene()
    create_model(pose)
    camera = setup_stage()
    camera.data.ortho_scale = ortho_scale
    aim(camera, camera_location, target)
    bpy.context.scene.render.filepath = str(OUTPUT / filename)
    bpy.ops.render.render(write_still=True)


def export_neutral_glb() -> None:
    clear_scene()
    objects = create_model(hierarchy=True)
    bpy.ops.object.select_all(action="DESELECT")
    for obj in objects:
        obj.select_set(True)
    for obj in bpy.context.scene.objects:
        if obj.type == "EMPTY":
            obj.select_set(True)
    bpy.ops.export_scene.gltf(filepath=str(GLB_PATH), export_format="GLB", use_selection=True)


def main() -> None:
    export_neutral_glb()
    neutral = {}
    views = {
        "FRONT.png": ((0, 86, 17), (0, 3, 16), 52),
        "LEFT.png": ((-86, 0, 17), (0, 0, 16), 64),
        "BACK.png": ((0, -86, 17), (0, 0, 16), 52),
        "RIGHT.png": ((86, 0, 17), (0, 0, 16), 64),
        "TOP.png": ((0, 0, 92), (0, 0, 12), 68),
        "PERSPECTIVE.png": ((-52, 67, 40), (0, 0, 15), 60),
    }
    for filename, (location, target, ortho_scale) in views.items():
        render_pose(filename, neutral, location, target, ortho_scale)

    crouch = {
        "body": {"position": (0, -2.2, 0)},
        "head": {"position": (0, -2.0, -1.0)},
        "front_left_leg": {"rotation": (13, 0, -4)},
        "front_right_leg": {"rotation": (13, 0, 4)},
        "rear_left_leg": {"rotation": (-18, 0, -5)},
        "rear_right_leg": {"rotation": (-18, 0, 5)},
    }
    run = {
        "body": {"position": (0, 1.0, 0)},
        "front_left_leg": {"rotation": (28, 0, 0)},
        "front_right_leg": {"rotation": (-26, 0, 0)},
        "rear_left_leg": {"rotation": (-24, 0, 0)},
        "rear_right_leg": {"rotation": (22, 0, 0)},
        "tail_1": {"rotation": (-10, 0, 0)},
    }
    attack = {
        "body": {"position": (0, 3.0, -1.5), "rotation": (-8, 0, 0)},
        "head": {"position": (0, 2.0, -2.0), "rotation": (-7, 0, 0)},
        "jaw": {"position": (0, -1.0, 0), "rotation": (-52, 0, 0)},
        "front_left_leg": {"rotation": (-58, 0, -7)},
        "front_right_leg": {"rotation": (-50, 0, 7)},
        "rear_left_leg": {"rotation": (18, 0, 0)},
        "rear_right_leg": {"rotation": (18, 0, 0)},
        "tail_1": {"rotation": (-22, 0, 0)},
        "tail_2": {"rotation": (-14, 0, 0)},
    }
    render_pose("POSE_CROUCH.png", crouch, (-52, 67, 35))
    render_pose("POSE_RUN.png", run, (72, 44, 28))
    render_pose("POSE_ATTACK.png", attack, (-30, 58, 25), (0, 3, 18))
    clear_scene()
    create_model(hierarchy=True)
    bpy.ops.wm.save_as_mainfile(filepath=str(ROOT / "manul_production.blend"))


if __name__ == "__main__":
    main()
