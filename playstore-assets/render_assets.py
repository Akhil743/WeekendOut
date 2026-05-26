"""
Generate Play Store assets from scratch using Pillow:
  - 512x512 hi-res app icon (icon-512.png)
  - 1024x500 feature graphic (feature-graphic-1024x500.png)

Same WeekendOut brand: sun + two mountains over off-white, forest-green primary
plus warm-orange accent. Design mirrors app/src/main/res/drawable/ic_launcher_foreground.xml
but is rendered at full bleed (no adaptive icon safe area) for Play Store.
"""
import os
from PIL import Image, ImageDraw, ImageFont

OUT_DIR = os.path.dirname(os.path.abspath(__file__))

# Brand colors
BG       = (252, 251, 247)   # #FCFBF7 — warm off-white
SUN      = (224, 122, 62)    # #E07A3E
BACK_MTN = (21, 80, 52)      # #155034 — deep forest
FRONT_MTN = (46, 125, 92)    # #2E7D5C — lighter forest
INK      = (26, 28, 25)      # #1A1C19


# ── 512×512 icon ──────────────────────────────────────────────────────────────
def render_icon(size=512):
    img = Image.new("RGB", (size, size), BG)
    d = ImageDraw.Draw(img)
    # Simple rounded-square hint isn't required (Play Store masks automatically)
    # Sun
    cx, cy, r = size * 0.62, size * 0.30, size * 0.13
    d.ellipse([cx - r, cy - r, cx + r, cy + r], fill=SUN)
    # Back (taller, deeper) mountain — drawn first
    back = [
        (size * 0.05, size * 0.85),
        (size * 0.32, size * 0.40),
        (size * 0.55, size * 0.62),
        (size * 0.72, size * 0.45),
        (size * 0.95, size * 0.85),
    ]
    d.polygon(back, fill=BACK_MTN)
    # Front mountain — overlaps lower-left
    front = [
        (size * 0.00, size * 0.85),
        (size * 0.22, size * 0.55),
        (size * 0.45, size * 0.85),
    ]
    d.polygon(front, fill=FRONT_MTN)
    out = os.path.join(OUT_DIR, f"icon-{size}.png")
    img.save(out, "PNG", optimize=True)
    return out


# ── 1024×500 feature graphic ──────────────────────────────────────────────────
def find_font(*candidates):
    for path in candidates:
        if os.path.exists(path):
            return path
    return None


def render_feature_graphic():
    W, H = 1024, 500
    img = Image.new("RGB", (W, H), BG)
    d = ImageDraw.Draw(img)

    # A subtle horizon band on the left — landscape backdrop
    # Sun on the left
    cx, cy, r = 220, 175, 80
    d.ellipse([cx - r, cy - r, cx + r, cy + r], fill=SUN)
    # Back mountain wave behind sun
    back = [(0, 360), (130, 200), (310, 290), (450, 180), (610, 320), (610, 500), (0, 500)]
    d.polygon(back, fill=BACK_MTN)
    # Front mountain
    front = [(0, 380), (90, 260), (220, 380), (220, 500), (0, 500)]
    d.polygon(front, fill=FRONT_MTN)

    # Right side: app name + tagline
    title_font_path = find_font(
        "/System/Library/Fonts/Avenir Next.ttc",
        "/System/Library/Fonts/Helvetica.ttc",
    )
    body_font_path = title_font_path

    # Pillow can't pick a face from a TTC by name; default index 0 is Regular.
    title = ImageFont.truetype(title_font_path, 78)
    sub = ImageFont.truetype(body_font_path, 28)

    text_x = 660
    d.text((text_x, 180), "WeekendOut", font=title, fill=INK)
    d.text((text_x, 280), "Where to go this weekend —", font=sub, fill=INK)
    d.text((text_x, 318), "scenic, lively, or somewhere", font=sub, fill=INK)
    d.text((text_x, 356), "in between, picked for you.", font=sub, fill=INK)

    out = os.path.join(OUT_DIR, "feature-graphic-1024x500.png")
    img.save(out, "PNG", optimize=True)
    return out


if __name__ == "__main__":
    icon = render_icon()
    fg = render_feature_graphic()
    print(f"Wrote {icon}")
    print(f"Wrote {fg}")
