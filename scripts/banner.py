"""Render the Android TV banner (320x180 dp, drawn at xhdpi = 640x360 px).

    make banner

Text can't live in a vector drawable without converting it to paths, so the banner is a PNG.
It reuses the launcher icon's geometry (ic_launcher_foreground.xml): the house inside the
draining ring, with the amber "moment we leave" dot. Drawn at 4x and downsampled for clean edges.
"""
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / "app/src/main/res/drawable-xhdpi/tv_banner.png"
FONT = "/usr/share/fonts/opentype/inter/Inter-Light.otf"
FONT_SMALL = "/usr/share/fonts/opentype/inter/Inter-Regular.otf"

NIGHT, DUSK = (14, 26, 43), (36, 31, 53)
MIST, AMBER = (207, 216, 227), (232, 176, 74)
TRACK = (60, 70, 88)

W, H, S = 640, 360, 4  # final size and supersampling factor


def main() -> None:
    img = Image.new("RGB", (W * S, H * S))
    px = img.load()
    # Diagonal night -> dusk gradient, like the countdown screen draining.
    for y in range(H * S):
        for x in range(0, W * S, 1):
            t = (x / (W * S) * 0.6 + y / (H * S) * 0.4)
            px[x, y] = tuple(round(a + (b - a) * t) for a, b in zip(NIGHT, DUSK))
    d = ImageDraw.Draw(img)

    # Icon geometry (108-unit viewport) scaled so the ring radius is 78 px, centred at (130, 180).
    k = 78 / 26 * S
    cx, cy = 130 * S, 180 * S

    def p(x: float, y: float) -> tuple[float, float]:
        return cx + (x - 54) * k, cy + (y - 54) * k

    r, w = 26 * k, round(4.5 * k)
    box = [cx - r, cy - r, cx + r, cy + r]
    d.arc(box, 0, 360, fill=TRACK, width=w)
    d.arc(box, -90, 180, fill=MIST, width=w)  # 270 degrees clockwise from 12 o'clock
    for ang_end in ((cx, cy - r),):  # round cap at the top
        d.ellipse([ang_end[0] - w / 2, ang_end[1] - w / 2, ang_end[0] + w / 2, ang_end[1] + w / 2], fill=MIST)
    dot = 4 * k
    lx, ly = cx - r, cy
    d.ellipse([lx - dot, ly - dot, lx + dot, ly + dot], fill=AMBER)
    d.polygon([p(54, 40.5), p(67, 52), p(63, 52), p(63, 66), p(45, 66), p(45, 52), p(41, 52)], fill=MIST)
    d.rectangle([p(51, 57), p(57, 66)], fill=AMBER)

    # Largest title size that fits between the ring and a 32 px right margin.
    tx, room = 240 * S, (W - 240 - 32) * S
    size = 56
    while ImageFont.truetype(FONT, size * S).getlength("COUNT-US-DOWN") > room:
        size -= 1
    title = ImageFont.truetype(FONT, size * S)
    small = ImageFont.truetype(FONT_SMALL, 22 * S)
    d.text((tx, 172 * S), "COUNT-US-DOWN", font=title, fill=MIST, anchor="ls")
    d.text((tx + 2 * S, 214 * S), "time to leave, calmly", font=small, fill=(150, 162, 180), anchor="ls")

    OUT.parent.mkdir(parents=True, exist_ok=True)
    img.resize((W, H), Image.LANCZOS).save(OUT, optimize=True)
    print(OUT)


if __name__ == "__main__":
    main()
