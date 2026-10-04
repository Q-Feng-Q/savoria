"""Render two notebook calendar markers from one heart outline."""

from pathlib import Path

from PIL import Image, ImageDraw


SIZE = 128
SUPERSAMPLE = 4
CANVAS = SIZE * SUPERSAMPLE
HORIZONTAL_SCALE = 0.925
ASSET_DIR = Path(__file__).resolve().parents[1] / "assets" / "ui"
SEGMENTS = [
    ((43, 13), (35, 10), (27, 10)),
    ((14, 10), (8, 20), (8, 31)),
    ((8, 45), (21, 58), (50, 85)),
    ((79, 58), (92, 45), (92, 31)),
    ((92, 20), (86, 10), (73, 10)),
    ((65, 10), (57, 13), (50, 23)),
]
EVENT_TONES = ("#e43f5a", "#2f7ef7", "#1bae75", "#8957e5", "#f59e0b")


def heart_points():
    points = [(50, 23)]
    start = points[0]
    for control_a, control_b, end in SEGMENTS:
        for step in range(1, 33):
            t = step / 32
            inverse = 1 - t
            points.append((
                inverse ** 3 * start[0] + 3 * inverse ** 2 * t * control_a[0]
                + 3 * inverse * t ** 2 * control_b[0] + t ** 3 * end[0],
                inverse ** 3 * start[1] + 3 * inverse ** 2 * t * control_a[1]
                + 3 * inverse * t ** 2 * control_b[1] + t ** 3 * end[1],
            ))
        start = end
    return [(round((50 + (x - 50) * HORIZONTAL_SCALE) * CANVAS / 100),
             round(y * CANVAS / 100)) for x, y in points]


def render(kind, color, suffix=""):
    points = heart_points()
    mask = Image.new("L", (CANVAS, CANVAS), 0)
    draw = ImageDraw.Draw(mask)
    if kind == "filled":
        draw.polygon(points, fill=255)
    draw.line(points, fill=255, width=round(CANVAS * 0.05), joint="curve")
    mask = mask.resize((SIZE, SIZE), Image.Resampling.LANCZOS)
    image = Image.new("RGBA", (SIZE, SIZE), color)
    image.putalpha(mask)
    image.save(ASSET_DIR / f"notebook-heart-{kind}{suffix}.png")


if __name__ == "__main__":
    render("outline", "#ce3f45")
    render("filled", "#df7739")
    for index, color in enumerate(EVENT_TONES, start=1):
        render("outline", color, f"-tone-{index}")
        render("filled", color, f"-tone-{index}")
