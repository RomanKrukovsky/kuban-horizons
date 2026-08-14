#!/usr/bin/env python3
"""
Генератор спавн-яиц Kuban Horizons (16×16, ванильный стиль Minecraft).

Создаёт PNG с двумя тонами (верх/низ) + лёгким силуэтом животного.
Цвета берутся из ART_BIBLE и региональной палитры.
"""

from pathlib import Path
from PIL import Image
import struct
import zlib

# === Цвета спавн-яиц (верх, низ) — региональная палитра ===
SPAWN_EGG_COLORS = {
    "manul":          ((0x8B, 0x5C, 0xF6), (0x6B, 0x46, 0xC1)),  # фиолетовый манул
    "wild_boar":      ((0x5D, 0x4E, 0x37), (0x3D, 0x2E, 0x1E)),  # кабан — тёмно-коричневый
    "caucasian_shepherd": ((0xD2, 0x69, 0x1E), (0x8B, 0x45, 0x13)),  # овчарка — рыжий
    "quail":          ((0x8B, 0x73, 0x55), (0x6B, 0x53, 0x35)),  # перепел — серо-бурый
    "pheasant":       ((0x8B, 0x00, 0x00), (0x65, 0x00, 0x00)),  # фазан — тёмно-красный
    "locust":         ((0x55, 0x6B, 0x2F), (0x3D, 0x4F, 0x1F)),  # саранча — зелёная
    "nutria":         ((0x70, 0x80, 0x90), (0x50, 0x60, 0x70)),  # нутрия — серо-голубая
    "gull":           ((0xF5, 0xF5, 0xF5), (0xD3, 0xD3, 0xD3)),  # чайка — белая
    "heron":          ((0x70, 0x80, 0x90), (0x4A, 0x63, 0xD8)),  # цапля — серо-синяя
    "sturgeon":       ((0x2F, 0x4F, 0x4F), (0x1C, 0x3A, 0x3A)),  # осётр — тёмно-серый
}

def create_spawn_egg(name: str, top_rgb, bottom_rgb, out_path: Path):
    """Создаёт 16×16 PNG спавн-яйца с градиентом и яйцевидной формой."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    pixels = img.load()

    # Яйцевидная маска (овал, чуть суженный к верху)
    for y in range(16):
        for x in range(16):
            # Нормализованные координаты (-1..1)
            nx = (x - 7.5) / 7.5
            ny = (y - 7.5) / 7.5
            # Эллипс + лёгкое сужение верха
            if nx*nx + ny*ny*1.15 < 1.0:
                # Верхняя половина — top_rgb, нижняя — bottom_rgb
                t = (y / 15.0)
                r = int(top_rgb[0] * (1-t) + bottom_rgb[0] * t)
                g = int(top_rgb[1] * (1-t) + bottom_rgb[1] * t)
                b = int(top_rgb[2] * (1-t) + bottom_rgb[2] * t)
                # Лёгкая обводка (тёмнее на 20%)
                if nx*nx + ny*ny > 0.85:
                    r = int(r * 0.7)
                    g = int(g * 0.7)
                    b = int(b * 0.7)
                pixels[x, y] = (r, g, b, 255)

    img.save(out_path, "PNG")
    print(f"  ✓ {name}_spawn_egg.png")

def main():
    out_dir = Path(__file__).parent.parent.parent / "src" / "main" / "resources" / "assets" / "kubanhorizons" / "textures" / "item"
    out_dir.mkdir(parents=True, exist_ok=True)

    print("Генерация спавн-яиц Kuban Horizons (16×16)...")
    for name, (top, bottom) in SPAWN_EGG_COLORS.items():
        create_spawn_egg(name, top, bottom, out_dir / f"{name}_spawn_egg.png")

    print("Готово. Запустите datagen, чтобы обновить item models.")

if __name__ == "__main__":
    main()