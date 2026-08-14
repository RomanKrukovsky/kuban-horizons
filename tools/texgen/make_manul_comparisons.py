"""Create 300 px side-by-side reference comparisons for visual QA."""

from pathlib import Path

from PIL import Image, ImageDraw


ROOT = Path(__file__).resolve().parents[2]
PAIRS = {
    "comparison_front.png": ("MANUL_4.png", "preview_front.png"),
    "comparison_3q.png": ("MANUL_1.png", "preview_3q.png"),
    "comparison_side.png": ("MANUL_2.png", "preview_side.png"),
    "comparison_back.png": ("MANUL_3.png", "preview_back.png"),
}


def fit(image, size=300):
    result = Image.new("RGB", (size, size), (230, 230, 230))
    copy = image.convert("RGB")
    copy.thumbnail((size, size), Image.Resampling.LANCZOS)
    result.paste(copy, ((size - copy.width) // 2, (size - copy.height) // 2))
    return result


def main():
    for output, (reference, preview) in PAIRS.items():
        canvas = Image.new("RGB", (604, 326), (34, 34, 34))
        canvas.paste(fit(Image.open(ROOT / reference)), (0, 26))
        canvas.paste(fit(Image.open(ROOT / preview)), (304, 26))
        draw = ImageDraw.Draw(canvas)
        draw.text((8, 6), "REFERENCE", fill=(245, 245, 245))
        draw.text((312, 6), "MODEL", fill=(245, 245, 245))
        canvas.save(ROOT / output)
        print("wrote", output)


if __name__ == "__main__":
    main()
