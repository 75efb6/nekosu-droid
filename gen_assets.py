#!/usr/bin/env python3
"""Generate assets matching the dark/pink UI style from colors.xml."""

import subprocess, os, tempfile
from PIL import Image, ImageDraw, ImageFont

ASSETS = "/home/null/Documentos/nekosu!droid/assets"

# Colors from colors.xml
ACCENT      = (230, 62, 140, 255)   # #E63E8C
ACCENT_DARK = (194, 36, 110, 255)   # #C2246E
BG_BASE     = (15, 15, 26, 255)     # #0F0F1A
BG_SURFACE  = (26, 26, 46, 255)     # #1A1A2E
BG_HIGH     = (37, 37, 64, 255)     # #252540
TEXT_PRI    = (255, 255, 255, 255)
TEXT_SEC    = (153, 153, 187, 255)  # #9999BB
WHITE_AA    = (255, 255, 255, 170)  # #AAFFFFFF

FONT_BOLD    = "/usr/share/fonts/open-sans/OpenSans-Bold.ttf"
FONT_SEMI    = "/usr/share/fonts/open-sans/OpenSans-Semibold.ttf"
FONT_REGULAR = "/usr/share/fonts/open-sans/OpenSans-Regular.ttf"


# ── SVG → PNG via rsvg-convert ─────────────────────────────────────────────

def svg_to_png(svg: str, w: int, h: int, dest: str):
    subprocess.run(
        ["rsvg-convert", "-w", str(w), "-h", str(h), "-o", dest],
        input=svg.encode(), check=True, capture_output=True,
    )

def icon_svg(path_data: str, fill: str = "#FFFFFF", fill_opacity: float = 1.0,
             vp: int = 24, extra_paths: list[str] | None = None) -> str:
    paths = f'<path fill="{fill}" fill-opacity="{fill_opacity}" d="{path_data}"/>'
    if extra_paths:
        for p in extra_paths:
            paths += f'\n  <path fill="{fill}" fill-opacity="{fill_opacity}" d="{p}"/>'
    return (f'<svg xmlns="http://www.w3.org/2000/svg" '
            f'width="{vp}" height="{vp}" viewBox="0 0 {vp} {vp}">'
            f'{paths}</svg>')


# ── Rounded rectangle helper ───────────────────────────────────────────────

def draw_rounded_rect(draw: ImageDraw.ImageDraw, xy, radius: int, fill):
    x0, y0, x1, y1 = xy
    draw.rectangle([x0 + radius, y0, x1 - radius, y1], fill=fill)
    draw.rectangle([x0, y0 + radius, x1, y1 - radius], fill=fill)
    draw.ellipse([x0, y0, x0 + radius * 2, y0 + radius * 2], fill=fill)
    draw.ellipse([x1 - radius * 2, y0, x1, y0 + radius * 2], fill=fill)
    draw.ellipse([x0, y1 - radius * 2, x0 + radius * 2, y1], fill=fill)
    draw.ellipse([x1 - radius * 2, y1 - radius * 2, x1, y1], fill=fill)


# ── Music control icons ────────────────────────────────────────────────────

PLAY_PATH  = "M9.65,17.875Q9.05,18.275 8.425,17.925Q7.8,17.575 7.8,16.875V7.125Q7.8,6.425 8.425,6.075Q9.05,5.725 9.65,6.125L17.325,10.975Q17.9,11.325 17.9,12Q17.9,12.675 17.325,13.025Z"
PAUSE_PATH = ("M15.4,19.225Q14.475,19.225 13.863,18.613Q13.25,18 13.25,17.075V6.875"
              "Q13.25,5.95 13.863,5.337Q14.475,4.725 15.4,4.725H17.1Q18.025,4.725 18.638,5.337"
              "Q19.25,5.95 19.25,6.875V17.075Q19.25,18 18.638,18.613Q18.025,19.225 17.1,19.225Z"
              "M6.9,19.225Q5.975,19.225 5.363,18.613Q4.75,18 4.75,17.075V6.875"
              "Q4.75,5.95 5.363,5.337Q5.975,4.725 6.9,4.725H8.6Q9.525,4.725 10.137,5.337"
              "Q10.75,5.95 10.75,6.875V17.075Q10.75,18 10.137,18.613Q9.525,19.225 8.6,19.225Z")
NEXT_PATH  = ("M17.75,18.25Q17.325,18.25 17,17.938Q16.675,17.625 16.675,17.175V6.825"
              "Q16.675,6.375 17,6.062Q17.325,5.75 17.75,5.75Q18.2,5.75 18.512,6.062"
              "Q18.825,6.375 18.825,6.825V17.175Q18.825,17.625 18.512,17.938Q18.2,18.25 17.75,18.25Z"
              "M7.05,17Q6.45,17.4 5.812,17.075Q5.175,16.75 5.175,16.025V7.975"
              "Q5.175,7.25 5.812,6.925Q6.45,6.6 7.05,7L13.075,11Q13.6,11.35 13.6,12"
              "Q13.6,12.65 13.075,13Z")
PREV_PATH  = ("M6.25,18.25Q5.8,18.25 5.488,17.938Q5.175,17.625 5.175,17.175V6.825"
              "Q5.175,6.375 5.488,6.062Q5.8,5.75 6.25,5.75Q6.675,5.75 7,6.062"
              "Q7.325,6.375 7.325,6.825V17.175Q7.325,17.625 7,17.938Q6.675,18.25 6.25,18.25Z"
              "M16.95,17L10.925,13Q10.4,12.65 10.4,12Q10.4,11.35 10.925,11L16.95,7"
              "Q17.55,6.6 18.188,6.925Q18.825,7.25 18.825,7.975V16.025"
              "Q18.825,16.75 18.188,17.075Q17.55,17.4 16.95,17Z")
STOP_PATH  = "M7,17V7H17V17Z"  # filled square
LIST_PATH  = "M3,17v2h6v-2H3zM3,5v2h10V5H3zm10,16v-2h8v-2h-8v-2h-2v6h2zM7,9v2H3v2h4v2h2V9H7zm14,4v-2H11v2h10zm-6,-4h2V7h4V5h-4V3h-2v6z"
CHAT_PATH  = "M20,2H4C2.9,2 2,2.9 2,4v18l4,-4h14c1.1,0 2,-0.9 2,-2V4C22,2.9 21.1,2 20,2z"
LOCK_PATH  = ("M18,8h-1V6c0-2.76-2.24-5-5-5S7,3.24 7,6v2H6c-1.1,0-2,.9-2,2v10"
              "c0,1.1.9,2 2,2h12c1.1,0 2-.9 2-2V10c0-1.1-.9-2-2-2z"
              "m-6,9c-1.1,0-2-.9-2-2s.9-2 2-2 2,.9 2,2-.9,2-2,2z"
              "m3.1-9H8.9V6c0-1.71 1.39-3.1 3.1-3.1 1.71,0 3.1,1.39 3.1,3.1v2z")
CROWN_PATH = ("M5,16L3,5l5.5,5L12,4l3.5,6L21,5l-2,11H5z"
              "M19,19c0,.55-.45,1-1,1H6c-.55,0-1-.45-1-1v-1h14v1z")

def make_music_icons():
    icons = {
        "music_play.png":  (PLAY_PATH,  60, 62),
        "music_pause.png": (PAUSE_PATH, 66, 66),
        "music_next.png":  (NEXT_PATH,  64, 62),
        "music_prev.png":  (PREV_PATH,  64, 62),
        "music_stop.png":  (STOP_PATH,  66, 66),
        "music_list.png":  (LIST_PATH,  66, 62),
    }
    for fname, (path, w, h) in icons.items():
        # Render at 4x for quality then downscale
        scale = 4
        with tempfile.NamedTemporaryFile(suffix=".svg", delete=False) as tf:
            tf.write(icon_svg(path).encode())
            tmp_svg = tf.name
        tmp_png = tmp_svg.replace(".svg", ".png")
        subprocess.run(
            ["rsvg-convert", "-w", str(w * scale), "-h", str(h * scale), "-o", tmp_png],
            input=open(tmp_svg).read().encode(), check=True, capture_output=True,
        )
        img = Image.open(tmp_png).resize((w, h), Image.LANCZOS)
        img.save(os.path.join(ASSETS, fname))
        os.unlink(tmp_svg); os.unlink(tmp_png)
        print(f"  {fname}")


def make_chat_icon():
    w, h = 25, 25
    scale = 4
    svg = icon_svg(CHAT_PATH, fill="#FFFFFF", fill_opacity=0.67)
    with tempfile.NamedTemporaryFile(suffix=".png", delete=False) as tf:
        tmp = tf.name
    subprocess.run(
        ["rsvg-convert", "-w", str(w * scale), "-h", str(h * scale), "-o", tmp],
        input=svg.encode(), check=True, capture_output=True,
    )
    img = Image.open(tmp).resize((w, h), Image.LANCZOS)
    img.save(os.path.join(ASSETS, "chat.png"))
    os.unlink(tmp)
    print("  chat.png")


def make_lock_icon():
    w, h = 27, 32
    scale = 4
    svg = (f'<svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24">'
           f'<path fill="#FFFFFF" d="{LOCK_PATH}"/></svg>')
    with tempfile.NamedTemporaryFile(suffix=".png", delete=False) as tf:
        tmp = tf.name
    subprocess.run(
        ["rsvg-convert", "-w", str(w * scale), "-h", str(h * scale), "-o", tmp],
        input=svg.encode(), check=True, capture_output=True,
    )
    img = Image.open(tmp).resize((w, h), Image.LANCZOS)
    img.save(os.path.join(ASSETS, "lock.png"))
    os.unlink(tmp)
    print("  lock.png")


def make_crown_icon():
    w, h = 35, 30
    scale = 4
    svg = (f'<svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24">'
           f'<path fill="#E63E8C" d="{CROWN_PATH}"/></svg>')
    with tempfile.NamedTemporaryFile(suffix=".png", delete=False) as tf:
        tmp = tf.name
    subprocess.run(
        ["rsvg-convert", "-w", str(w * scale), "-h", str(h * scale), "-o", tmp],
        input=svg.encode(), check=True, capture_output=True,
    )
    img = Image.open(tmp).resize((w, h), Image.LANCZOS)
    img.save(os.path.join(ASSETS, "crown.png"))
    os.unlink(tmp)
    print("  crown.png")


# ── Menu buttons ───────────────────────────────────────────────────────────

MENU_BUTTONS = [
    ("back.png",    "Back",
     "M20,11H7.83l5.59,-5.59L12,4l-8,8 8,8 1.41,-1.41L7.83,13H20V11Z"),
    ("play.png",    "Play",    PLAY_PATH),
    ("solo.png",    "Solo",
     "M12,2C6.48,2 2,6.48 2,12s4.48,10 10,10 10,-4.48 10,-10S17.52,2 12,2z"
     "m-2,14.5v-9l6,4.5-6,4.5z"),
    ("options.png", "Options",
     "M19.14,12.94c.04-.3.06-.61.06-.94 0-.32-.02-.64-.07-.94l2.03-1.58c.18-.14.23-.41.12-.61"
     "l-1.92-3.32c-.12-.22-.37-.29-.59-.22l-2.39.96c-.5-.38-1.03-.7-1.62-.94"
     "l-.36-2.54c-.04-.24-.24-.41-.48-.41h-3.84c-.24,0-.43.17-.47.41"
     "l-.36,2.54c-.59.24-1.13.57-1.62.94l-2.39-.96c-.22-.08-.47,0-.59.22"
     "L2.74,8.87c-.12.21-.08.47.12.61l2.03,1.58C4.84,11.36 4.8,11.69 4.8,12"
     "s.02.64.07.94l-2.03,1.58c-.18.14-.23.41-.12.61l1.92,3.32c.12.22.37.29.59.22"
     "l2.39-.96c.5.38,1.03.7,1.62.94l.36,2.54c.05.24.24.41.48.41h3.84"
     "c.24,0,.44-.17.47-.41l.36-2.54c.59-.24,1.13-.56,1.62-.94l2.39.96"
     "c.22.08.47,0,.59-.22l1.92-3.32c.12-.22.07-.47-.12-.61l-2.01-1.58z"
     "M12,15.6c-1.98,0-3.6-1.62-3.6-3.6s1.62-3.6 3.6-3.6 3.6,1.62 3.6,3.6-1.62,3.6-3.6,3.6z"),
    ("exit.png",    "Exit",
     "M17,7l-1.41,1.41L18.17,11H8v2h10.17l-2.58,2.58L17,17l5-5zM4,5h8V3H4C2.9,3 2,3.9 2,5v14"
     "c0,1.1.9,2 2,2h8v-2H4V5z"),
    ("multi.png",   "Multi",
     "M16,11c1.66,0 2.99,-1.34 2.99,-3S17.66,5 16,5c-1.66,0-3,1.34-3,3s1.34,3 3,3z"
     "M8,11c1.66,0 2.99,-1.34 2.99,-3S9.66,5 8,5C6.34,5 5,6.34 5,8s1.34,3 3,3z"
     "M8,13c-2.33,0-7,1.17-7,3.5V19h14v-2.5c0,-2.33-4.67,-3.5-7,-3.5z"
     "M16,13c-.29,0-.62.02-.97.05 1.16.84 1.97,1.97 1.97,3.45V19h6v-2.5"
     "c0,-2.33-4.67,-3.5-7,-3.5z"),
    ("editor.png",  "Editor",
     "M3,17.25V21h3.75L17.81,9.94l-3.75,-3.75L3,17.25zM20.71,7.04c.39,-.39.39,-1.02"
     "0,-1.41l-2.34,-2.34c-.39,-.39-1.02,-.39-1.41,0l-1.83,1.83 3.75,3.75 1.83,-1.83z"),
]

def make_menu_button(fname, title, icon_path):
    W, H = 586, 92
    RADIUS = 10
    ACCENT_BAR = 5
    ICON_SIZE = 44
    ICON_MARGIN = 18

    img = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)

    draw_rounded_rect(draw, (0, 0, W - 1, H - 1), RADIUS, BG_SURFACE)

    for i in range(5):
        alpha = int(25 * (1 - i / 5))
        draw.line([(RADIUS, i), (W - RADIUS, i)], fill=(255, 255, 255, alpha))

    draw_rounded_rect(draw, (0, H - 1 - ACCENT_BAR, W - 1, H - 1), 2, ACCENT)

    # Icon
    svg = (f'<svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24">'
           f'<path fill="#FFFFFF" fill-opacity="0.6" d="{icon_path}"/></svg>')
    with tempfile.NamedTemporaryFile(suffix=".png", delete=False) as tf:
        tmp = tf.name
    subprocess.run(
        ["rsvg-convert", "-w", str(ICON_SIZE * 2), "-h", str(ICON_SIZE * 2), "-o", tmp],
        input=svg.encode(), check=True, capture_output=True,
    )
    icon_img = Image.open(tmp).resize((ICON_SIZE, ICON_SIZE), Image.LANCZOS)
    os.unlink(tmp)
    icon_x = W - ICON_MARGIN - ICON_SIZE
    img.paste(icon_img, (icon_x, (H - ICON_SIZE) // 2), icon_img)

    # Title centered in the usable area (above accent bar)
    title_font = ImageFont.truetype(FONT_BOLD, 38)
    cx = 80 + (icon_x - 80) // 2
    cy = (H - ACCENT_BAR) // 2
    draw.text((cx, cy), title, font=title_font, fill=TEXT_PRI, anchor="mm")

    img.save(os.path.join(ASSETS, fname))
    print(f"  {fname}")


# ── Ranking assets ─────────────────────────────────────────────────────────

def make_ranking_button():
    W, H = 256, 64
    img = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)
    draw_rounded_rect(draw, (0, 0, W - 1, H - 1), 8, BG_HIGH)
    draw_rounded_rect(draw, (0, 0, 4, H - 1), 2, ACCENT)
    img.save(os.path.join(ASSETS, "ranking_button.png"))
    print("  ranking_button.png")


def make_ranking_online(fname, enabled: bool):
    W = H = 50
    img = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)
    color = ACCENT if enabled else (*TEXT_SEC[:3], 180)
    draw_rounded_rect(draw, (0, 0, W - 1, H - 1), 10, (*BG_HIGH[:3], 200))
    draw.ellipse([6, 6, W - 7, H - 7], outline=color, width=2)
    path = ("M12,2C8.13,2 5,5.13 5,9c0,5.25 7,13 7,13s7,-7.75 7,-13"
            "c0,-3.87-3.13,-7-7,-7zm0,9.5c-1.38,0-2.5,-1.12-2.5,-2.5s1.12,-2.5 2.5,-2.5"
            "2.5,1.12 2.5,2.5-1.12,2.5-2.5,2.5z") if enabled else (
            "M19,3H5C3.9,3 3,3.9 3,5v14c0,1.1.9,2 2,2h14c1.1,0 2-.9 2-2V5"
            "c0-1.1-.9-2-2-2zm-7,3c1.93,0 3.5,1.57 3.5,3.5S13.93,13 12,13"
            "s-3.5,-1.57-3.5,-3.5S10.07,6 12,6zm7,13H5v-.23"
            "c0,-1.31.72,-2.47 1.88,-3.08C8.11,14.89 9.99,14.5 12,14.5"
            "s3.89.39 5.12,1.19C18.28,16.3 19,17.46 19,18.77V19z")
    svg = (f'<svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24">'
           f'<path fill="#{"%02X%02X%02X" % color[:3]}" d="{path}"/></svg>')
    with tempfile.NamedTemporaryFile(suffix=".png", delete=False) as tf:
        tmp = tf.name
    sz = 32
    subprocess.run(
        ["rsvg-convert", "-w", str(sz), "-h", str(sz), "-o", tmp],
        input=svg.encode(), check=True, capture_output=True,
    )
    icon = Image.open(tmp).resize((32, 32), Image.LANCZOS)
    img.paste(icon, (9, 9), icon)
    os.unlink(tmp)
    img.save(os.path.join(ASSETS, fname))
    print(f"  {fname}")


def make_ranking_nextpage():
    W, H = 128, 48
    img = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)
    draw_rounded_rect(draw, (0, 0, W - 1, H - 1), 8, BG_HIGH)
    path = "M6,18l8.5,-6L6,6v12zm2.5,-8.08L11.03,12 8.5,14.08V9.92zM14,6v12l8.5,-6L14,6zm2.5,8.08V9.92L19.03,12 16.5,14.08z"
    svg = (f'<svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24">'
           f'<path fill="#FFFFFF" fill-opacity="0.8" d="{path}"/></svg>')
    with tempfile.NamedTemporaryFile(suffix=".png", delete=False) as tf:
        tmp = tf.name
    subprocess.run(
        ["rsvg-convert", "-w", "28", "-h", "28", "-o", tmp],
        input=svg.encode(), check=True, capture_output=True,
    )
    icon = Image.open(tmp)
    img.paste(icon, ((W - 28) // 2, (H - 28) // 2), icon)
    os.unlink(tmp)
    img.save(os.path.join(ASSETS, "ranking_nextpage.png"))
    print("  ranking_nextpage.png")


# ── Selection status icons ─────────────────────────────────────────────────

SELECTION_ICONS = {
    "selection-ranked.png":   ("#5C9EE8", 28, 28,
        "M4,6l8,-4 8,4v6c0,5.55-3.84,10.74-8,12-4.16,-1.26-8,-6.45-8,-12V6z"
        "m6,9.58l5.66,-5.66-1.41,-1.41L10,12.75l-2.25,-2.25-1.42,1.41L10,15.58z"),
    "selection-approved.png": ("#4CAF50", 28, 28,
        "M9,16.17L4.83,12l-1.42,1.41L9,19 21,7l-1.41,-1.41z"),
    "selection-loved.png":    ("#E63E8C", 28, 28,
        "M12,21.35l-1.45,-1.32C5.4,15.36 2,12.28 2,8.5 2,5.42 4.42,3 7.5,3"
        "c1.74,0 3.41.81 4.5,2.09C13.09,3.81 14.76,3 16.5,3 19.58,3 22,5.42 22,8.5"
        "c0,3.78-3.4,6.86-8.55,11.54L12,21.35z"),
    "selection-question.png": ("#9999BB", 15, 26,
        "M11,18h2v-2h-2v2zm1,-16C6.48,2 2,6.48 2,12s4.48,10 10,10 10,-4.48 10,-10"
        "S17.52,2 12,2zm0,18c-4.41,0-8,-3.59-8,-8s3.59,-8 8,-8 8,3.59 8,8-3.59,8-8,8z"
        "m0,-14c-2.21,0-4,1.79-4,4h2c0,-1.1.9,-2 2,-2s2,.9 2,2c0,2-3,1.75-3,5h2"
        "c0,-2.25 3,-2.5 3,-5 0,-2.21-1.79,-4-4,-4z"),
}

"""
def make_selection_icons():
    for fname, (color, w, h, path) in SELECTION_ICONS.items():
        scale = 4
        svg = (f'<svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24">'
               f'<path fill="{color}" d="{path}"/></svg>')
        with tempfile.NamedTemporaryFile(suffix=".png", delete=False) as tf:
            tmp = tf.name
        subprocess.run(
            ["rsvg-convert", "-w", str(w * scale), "-h", str(h * scale), "-o", tmp],
            input=svg.encode(), check=True, capture_output=True,
        )
        img = Image.open(tmp).resize((w, h), Image.LANCZOS)
        img.save(os.path.join(ASSETS, fname))
        os.unlink(tmp)
        print(f"  {fname}")
"""

# ── Beatmap downloader sidebar ─────────────────────────────────────────────

def make_beatmap_downloader():
    W, H = 80, 284
    img = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)
    draw_rounded_rect(draw, (0, 0, W - 1, H - 1), 10, (*BG_HIGH[:3], 230))
    draw_rounded_rect(draw, (0, 0, W - 1, 4), 2, ACCENT)

    # Download icon at top
    dl_path = "M19,9h-4V3H9v6H5l7,7 7,-7zM5,18v2h14v-2H5z"
    svg = (f'<svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24">'
           f'<path fill="#FFFFFF" fill-opacity="0.9" d="{dl_path}"/></svg>')
    with tempfile.NamedTemporaryFile(suffix=".png", delete=False) as tf:
        tmp = tf.name
    subprocess.run(
        ["rsvg-convert", "-w", "32", "-h", "32", "-o", tmp],
        input=svg.encode(), check=True, capture_output=True,
    )
    icon = Image.open(tmp)
    img.paste(icon, ((W - 32) // 2, 16), icon)
    os.unlink(tmp)

    # Vertical text "Download Beatmaps"
    font = ImageFont.truetype(FONT_BOLD, 13)
    txt_img = Image.new("RGBA", (200, 16), (0, 0, 0, 0))
    txt_draw = ImageDraw.Draw(txt_img)
    txt_draw.text((0, 0), "Download Beatmaps", font=font, fill=TEXT_PRI)
    txt_rot = txt_img.rotate(90, expand=True)
    tx = (W - txt_rot.width) // 2
    ty = 60
    img.paste(txt_rot, (tx, ty), txt_rot)

    img.save(os.path.join(ASSETS, "beatmap_downloader.png"))
    print("  beatmap_downloader.png")


# ── missing.png (beatmap-missing indicator, 20x20) ─────────────────────────

def make_missing_icon():
    W = H = 20
    path = ("M12,3c-4.97,0-9,4.03-9,9s4.03,9 9,9 9,-4.03 9,-9-4.03,-9-9,-9z"
            "m1,14h-2v-2h2v2zm0,-4h-2V7h2v6z")
    svg = (f'<svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24">'
           f'<path fill="#E63E8C" d="{path}"/></svg>')
    with tempfile.NamedTemporaryFile(suffix=".png", delete=False) as tf:
        tmp = tf.name
    subprocess.run(
        ["rsvg-convert", "-w", str(W * 3), "-h", str(H * 3), "-o", tmp],
        input=svg.encode(), check=True, capture_output=True,
    )
    img = Image.open(tmp).resize((W, H), Image.LANCZOS)
    img.save(os.path.join(ASSETS, "missing.png"))
    os.unlink(tmp)
    print("  missing.png")


# ── logo.png (540x540, restyle background keeping cat) ─────────────────────

def make_logo():
    import colorsys
    src = Image.open(os.path.join(ASSETS, "logo.png")).convert("RGBA")
    W, H = src.size
    out = src.copy()
    px_in  = src.load()
    px_out = out.load()

    for y in range(H):
        for x in range(W):
            r, g, b, a = px_in[x, y]
            if a < 10:
                continue
            hue, sat, val = colorsys.rgb_to_hsv(r / 255, g / 255, b / 255)
            hue_deg = hue * 360
            # Pink background hue: roughly 300–360° with high saturation
            in_pink_range = (hue_deg > 285 or hue_deg < 20)
            if sat > 0.45 and in_pink_range and val > 0.25:
                # Map brightness to dark-theme range: dim→BG_BASE, bright→BG_HIGH
                t = max(0.0, min(1.0, (val - 0.25) / 0.75))
                nr = int(BG_BASE[0] + t * (BG_HIGH[0] - BG_BASE[0]))
                ng = int(BG_BASE[1] + t * (BG_HIGH[1] - BG_BASE[1]))
                nb = int(BG_BASE[2] + t * (BG_HIGH[2] - BG_BASE[2]))
                px_out[x, y] = (nr, ng, nb, a)

    # Accent ring around the circle edge
    draw = ImageDraw.Draw(out)
    cx, cy = W // 2, H // 2
    # The circular logo radius — find it from alpha channel
    rad = 0
    for rx in range(cx, W):
        if px_in[rx, cy][3] < 10:
            rad = rx - cx - 1
            break
    if rad > 10:
        draw.ellipse(
            [cx - rad, cy - rad, cx + rad, cy + rad],
            outline=(*ACCENT[:3], 200), width=6,
        )

    out.save(os.path.join(ASSETS, "logo.png"))
    print("  logo.png")


# ── gfx/button.png (white tintable rounded rect for TextButton) ────────────

def make_gfx_button():
    W, H = 315, 71
    img = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)
    RADIUS = 10

    # White body, alpha = 230 so tinting gives slightly transparent buttons
    draw_rounded_rect(draw, (0, 0, W - 1, H - 1), RADIUS, (255, 255, 255, 230))

    # Soft highlight at top edge (helps readability when tinted dark)
    for i in range(4):
        alpha = int(60 * (1 - i / 4))
        draw.line([(RADIUS, i), (W - RADIUS, i)], fill=(255, 255, 255, alpha))

    img.save(os.path.join(ASSETS, "gfx", "button.png"))
    print("  gfx/button.png")


# ── Main ───────────────────────────────────────────────────────────────────

if __name__ == "__main__":
    print("Generating music control icons...")
    make_music_icons()
    make_chat_icon()
    make_lock_icon()
    make_crown_icon()

    print("Generating menu buttons...")
    for fname, title, icon in MENU_BUTTONS:
        make_menu_button(fname, title, icon)

    print("Generating ranking assets...")
    make_ranking_button()
    make_ranking_nextpage()

    print("Generating misc assets...")
    make_beatmap_downloader()
    make_missing_icon()

    print("Generating gfx button texture...")
    make_gfx_button()

    print("Restyling logo...")
    make_logo()

    print("Done.")
