"""Generate a fresh four-view Hunyuan3D guide mesh for the manul."""

from __future__ import annotations

import argparse
import sys
from pathlib import Path

import torch
from PIL import Image, ImageChops


ROOT = Path(__file__).resolve().parents[2]
HUNYUAN_ROOT = Path("/Users/romanmolodyko/Documents/Hunyuan3D")
sys.path.insert(0, str(HUNYUAN_ROOT / "repo"))

from hy3dgen.shapegen import Hunyuan3DDiTFlowMatchingPipeline


VIEWS = {
    "front": ("MANUL_1.png", (245, 165, 1035, 1025)),
    "left": ("MANUL_2.png", (30, 285, 1230, 875)),
    "back": ("MANUL_3.png", (295, 135, 940, 1150)),
    "right": ("MANUL_4.png", (25, 305, 1225, 860)),
}


def prepare_view(source: Path, crop: tuple[int, int, int, int]) -> Image.Image:
    image = Image.open(source).convert("RGBA").crop(crop)
    white = Image.new("RGBA", image.size, "white")
    difference = ImageChops.difference(image, white).convert("L")
    alpha = difference.point(lambda value: 0 if value < 18 else min(255, value * 5))
    bounds = alpha.getbbox()
    if bounds is None:
        raise ValueError(f"No subject found in {source}")
    image = image.crop(bounds)
    alpha = alpha.crop(bounds)
    image.putalpha(alpha)

    side = max(image.size)
    margin = max(24, round(side * 0.08))
    canvas = Image.new("RGBA", (side + margin * 2, side + margin * 2), (255, 255, 255, 0))
    canvas.alpha_composite(image, ((canvas.width - image.width) // 2, (canvas.height - image.height) // 2))
    return canvas.resize((768, 768), Image.Resampling.LANCZOS)


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--steps", type=int, default=35)
    parser.add_argument("--resolution", type=int, default=320)
    parser.add_argument("--seed", type=int, default=1946)
    parser.add_argument("--device", default="mps")
    parser.add_argument("--output", type=Path, default=ROOT / "manul_hunyuan_mv_v2.glb")
    args = parser.parse_args()

    prepared_dir = ROOT / "reference" / "hunyuan_mv_v2"
    prepared_dir.mkdir(parents=True, exist_ok=True)
    images: dict[str, Image.Image] = {}
    for name, (filename, crop) in VIEWS.items():
        image = prepare_view(ROOT / filename, crop)
        image.save(prepared_dir / f"{name}.png")
        images[name] = image

    model_path = HUNYUAN_ROOT / "models" / "Hunyuan3D-2mv"
    pipeline = Hunyuan3DDiTFlowMatchingPipeline.from_pretrained(
        str(model_path),
        subfolder="hunyuan3d-dit-v2-mv",
        variant="fp16",
        device=args.device,
        dtype=torch.float16,
    )
    mesh = pipeline(
        image=images,
        num_inference_steps=args.steps,
        octree_resolution=args.resolution,
        num_chunks=12000,
        generator=torch.manual_seed(args.seed),
        output_type="trimesh",
    )[0]
    mesh.export(args.output)
    print(f"wrote {args.output}")


if __name__ == "__main__":
    main()
