"""Render orthographic checks for the fresh Hunyuan3D guide mesh in Blender."""

from __future__ import annotations

import math
from pathlib import Path

import bpy
from mathutils import Vector


ROOT = Path(__file__).resolve().parents[2]
SOURCE = ROOT / "manul_hunyuan_mv_v2.glb"
OUTPUT = ROOT / "hunyuan_comparisons_v2"
OUTPUT.mkdir(exist_ok=True)

bpy.ops.object.select_all(action="SELECT")
bpy.ops.object.delete(use_global=False)
bpy.ops.import_scene.gltf(filepath=str(SOURCE))

meshes = [obj for obj in bpy.context.scene.objects if obj.type == "MESH"]
minimum = Vector((math.inf, math.inf, math.inf))
maximum = Vector((-math.inf, -math.inf, -math.inf))
for obj in meshes:
    for corner in obj.bound_box:
        world = obj.matrix_world @ Vector(corner)
        minimum.x = min(minimum.x, world.x)
        minimum.y = min(minimum.y, world.y)
        minimum.z = min(minimum.z, world.z)
        maximum.x = max(maximum.x, world.x)
        maximum.y = max(maximum.y, world.y)
        maximum.z = max(maximum.z, world.z)

center = (minimum + maximum) / 2
for obj in meshes:
    obj.location -= center

material = bpy.data.materials.new("Hunyuan guide")
material.diffuse_color = (0.58, 0.61, 0.64, 1)
material.roughness = 0.92
for obj in meshes:
    obj.data.materials.clear()
    obj.data.materials.append(material)

bpy.context.scene.world.color = (1, 1, 1)
bpy.ops.object.light_add(type="AREA", location=(-3, -4, 6))
bpy.context.object.data.energy = 800
bpy.context.object.data.shape = "DISK"
bpy.context.object.data.size = 5
bpy.ops.object.light_add(type="AREA", location=(4, 2, 3))
bpy.context.object.data.energy = 450
bpy.context.object.data.size = 4

bpy.ops.object.camera_add()
camera = bpy.context.object
camera.data.type = "ORTHO"
camera.data.ortho_scale = 2.65
bpy.context.scene.camera = camera

def aim(location: tuple[float, float, float]) -> None:
    camera.location = location
    camera.rotation_euler = (-camera.location).to_track_quat("-Z", "Y").to_euler()

scene = bpy.context.scene
scene.render.engine = "BLENDER_EEVEE"
scene.render.resolution_x = 900
scene.render.resolution_y = 900
scene.render.resolution_percentage = 100
scene.render.image_settings.file_format = "PNG"
scene.render.film_transparent = False
scene.view_settings.look = "AgX - Medium High Contrast"

views = {
    "front.png": (0, -4, 0),
    "left.png": (-4, 0, 0),
    "back.png": (0, 4, 0),
    "right.png": (4, 0, 0),
    "three_quarter.png": (3.2, -3.2, 2.2),
}
for filename, location in views.items():
    aim(location)
    scene.render.filepath = str(OUTPUT / filename)
    bpy.ops.render.render(write_still=True)

bpy.ops.wm.save_as_mainfile(filepath=str(ROOT / "manul_hunyuan_mv_v2.blend"))
