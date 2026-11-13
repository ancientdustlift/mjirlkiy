#!/usr/bin/env python3
"""Generates the mod's pixel-art textures. Run from the repository root: python3 tools/generate_textures.py"""
import colorsys
import math
import os
import random

from PIL import Image

OUT = "src/main/resources/assets/kalabushkin/textures"

SKIN = (226, 176, 154)
SKIN_SHADE = (204, 150, 130)
CHEEK = (222, 150, 136)
HAIR = (186, 186, 184)
HAIR_DARK = (150, 150, 150)
MAROON = (132, 34, 52)
MAROON_DARK = (98, 22, 38)
BLUE = (44, 134, 196)
BLUE_DARK = (30, 104, 160)
COLLAR = (70, 160, 215)
SHIRT = (52, 60, 82)
SHIRT_DOT = (150, 160, 185)
ZIP = (190, 190, 195)
TROUSERS = (58, 62, 78)
SHOE = (30, 28, 28)


def faces(u, v, w, h, d):
    """Rectangles (x, y, w, h) of a Minecraft box-UV cuboid. 'right' adjoins the front on its
    right edge, 'left' on its left edge; the bottom row of 'top' adjoins the front."""
    return {
        "top": (u + d, v, w, d),
        "bottom": (u + d + w, v, w, d),
        "right": (u, v + d, d, h),
        "front": (u + d, v + d, w, h),
        "left": (u + d + w, v + d, d, h),
        "back": (u + d + w + d, v + d, w, h),
    }


class Canvas:
    def __init__(self, size=64, seed=7):
        self.img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
        self.rng = random.Random(seed)

    def px(self, rect, x, y, color, jitter=0):
        j = self.rng.randint(-jitter, jitter) if jitter else 0
        rgb = tuple(max(0, min(255, c + j)) for c in color[:3])
        alpha = color[3] if len(color) > 3 else 255
        self.img.putpixel((rect[0] + x, rect[1] + y), rgb + (alpha,))

    def fill(self, rect, color, jitter=5, rows=None, cols=None):
        for y in rows if rows is not None else range(rect[3]):
            for x in cols if cols is not None else range(rect[2]):
                self.px(rect, x, y, color, jitter)

    def box(self, f, color, jitter=5):
        for rect in f.values():
            self.fill(rect, color, jitter)


def paint_head(c):
    f = faces(0, 0, 8, 8, 8)
    c.box(f, SKIN, 4)
    # Short grey hair: thin on top, receding at the front, fuller at the sides and back.
    c.fill(f["top"], HAIR, 8)
    for x in range(2, 6):
        c.px(f["top"], x, 7, SKIN, 4)
        c.px(f["top"], x, 6, (206, 190, 180), 4)
    for side, front_col in (("right", 7), ("left", 0)):
        c.fill(f[side], HAIR, 8, rows=range(3))
        c.fill(f[side], HAIR_DARK, 6, rows=[3], cols=[x for x in range(8) if x != front_col])
        ear_x = 4 if side == "right" else 3
        c.px(f[side], ear_x, 4, SKIN_SHADE)
        c.px(f[side], ear_x, 5, SKIN_SHADE)
        c.px(f[side], front_col, 2, SKIN, 4)
    c.fill(f["back"], HAIR, 8, rows=range(5))
    c.fill(f["back"], HAIR_DARK, 6, rows=[5])
    c.fill(f["bottom"], SKIN_SHADE, 3)

    face = f["front"]
    for x in (0, 1, 6, 7):
        c.px(face, x, 0, HAIR, 6)
    c.px(face, 0, 1, HAIR, 6)
    c.px(face, 7, 1, HAIR, 6)
    for x in (1, 2, 5, 6):
        c.px(face, x, 3, (168, 150, 132))  # brows
    c.px(face, 1, 4, (240, 240, 240))
    c.px(face, 2, 4, (84, 112, 132))
    c.px(face, 5, 4, (84, 112, 132))
    c.px(face, 6, 4, (240, 240, 240))
    c.px(face, 1, 5, CHEEK)
    c.px(face, 6, 5, CHEEK)
    c.px(face, 3, 5, SKIN_SHADE)
    c.px(face, 4, 5, SKIN_SHADE)
    for x in (3, 4):
        c.px(face, x, 6, (182, 116, 108))  # mouth
    c.px(face, 2, 6, SKIN_SHADE)
    c.px(face, 5, 6, SKIN_SHADE)
    c.fill(face, SKIN_SHADE, 3, rows=[7], cols=range(1, 7))

    nose = faces(24, 0, 2, 2, 1)
    c.box(nose, (216, 160, 140), 2)
    c.fill(nose["bottom"], SKIN_SHADE, 2)

    # Outer hair layer: gives the hair some volume over the scalp.
    hat = faces(32, 0, 8, 8, 8)
    c.fill(hat["top"], HAIR, 8, rows=range(6))
    for x in (0, 1, 6, 7):
        c.px(hat["top"], x, 6, HAIR, 8)
    for x in (0, 7):
        c.px(hat["top"], x, 7, HAIR, 8)
    c.fill(hat["right"], HAIR, 8, rows=range(2), cols=range(7))
    c.fill(hat["left"], HAIR, 8, rows=range(2), cols=range(1, 8))
    c.fill(hat["back"], HAIR, 8, rows=range(3))
    c.fill(hat["back"], HAIR_DARK, 6, rows=[3])


def paint_body(c):
    f = faces(16, 16, 8, 12, 4)
    c.box(f, MAROON, 5)
    for name in ("front", "back", "left", "right"):
        c.fill(f[name], BLUE, 5, rows=range(6, 9))
        c.fill(f[name], BLUE_DARK, 4, rows=[9])
        c.fill(f[name], MAROON_DARK, 4, rows=[11])
    front = f["front"]
    # Patterned shirt showing in the open neck, zip below it.
    for y in range(3):
        for x in (3, 4):
            c.px(front, x, y, SHIRT_DOT if (x + y) % 2 == 0 else SHIRT)
    for y in range(3, 12):
        c.px(front, 3, y, MAROON_DARK)
        c.px(front, 4, y, ZIP if y % 2 == 1 else (120, 120, 128))
    c.px(front, 2, 2, COLLAR)
    c.px(front, 5, 2, COLLAR)
    for x in (5, 6, 7):
        c.px(front, x, 6, BLUE_DARK)  # chest pocket flap

    collar = faces(0, 32, 9, 2, 5)
    c.box(collar, COLLAR, 5)
    c.fill(collar["top"], (96, 178, 226), 4)
    for y in range(2):
        c.px(collar["front"], 4, y, SHIRT_DOT if y == 0 else SHIRT)
    c.px(collar["front"], 3, 1, BLUE_DARK)
    c.px(collar["front"], 5, 1, BLUE_DARK)
    for y in range(3, 5):
        c.px(collar["top"], 4, y, SHIRT)


def paint_arm(c, u, v, fist_u, fist_v):
    f = faces(u, v, 4, 12, 4)
    c.box(f, MAROON, 5)
    for name in ("front", "back", "left", "right"):
        c.fill(f[name], BLUE, 5, rows=range(6, 8))
        c.fill(f[name], MAROON_DARK, 4, rows=[8])
        c.fill(f[name], SKIN, 4, rows=range(9, 12))
    c.fill(f["bottom"], SKIN_SHADE, 3)
    c.fill(f["top"], MAROON_DARK, 4)

    fist = faces(fist_u, fist_v, 4, 3, 4)
    c.box(fist, SKIN, 4)
    c.fill(fist["bottom"], SKIN_SHADE, 3)
    for name in ("front", "back", "left", "right"):
        c.fill(fist[name], SKIN_SHADE, 3, rows=[2])
    for x in (1, 3):
        c.px(fist["front"], x, 1, SKIN_SHADE)  # knuckles
        c.px(fist["bottom"], x, 1, (188, 134, 116))


def paint_leg(c, u, v):
    f = faces(u, v, 4, 12, 4)
    c.box(f, TROUSERS, 5)
    for name in ("front", "back", "left", "right"):
        c.fill(f[name], SHOE, 3, rows=range(10, 12))
    c.fill(f["bottom"], SHOE, 3)
    c.fill(f["top"], (44, 46, 58), 3)


def kalabushkin():
    c = Canvas()
    paint_head(c)
    paint_body(c)
    paint_arm(c, 40, 16, 0, 40)
    paint_arm(c, 32, 48, 16, 40)
    paint_leg(c, 0, 16)
    paint_leg(c, 16, 48)
    return c.img


def spirit(base):
    """Pale, bluish copy of the mob with a fading wisp where the legs were."""
    img = Image.new("RGBA", base.size, (0, 0, 0, 0))
    for y in range(base.size[1]):
        for x in range(base.size[0]):
            r, g, b, a = base.getpixel((x, y))
            if a == 0:
                continue
            lum = 0.3 * r + 0.59 * g + 0.11 * b
            mix = lambda ch, tint: int(min(255, 0.35 * ch + 0.35 * lum + 0.45 * tint))
            img.putpixel((x, y), (mix(r, 150), mix(g, 225), mix(b, 255), 255))
    for rect in (faces(0, 16, 4, 12, 4), faces(16, 48, 4, 12, 4)):
        for (rx, ry, rw, rh) in rect.values():
            img.paste((0, 0, 0, 0), (rx, ry, rx + rw, ry + rh))
    # Glowing eyes.
    face = faces(0, 0, 8, 8, 8)["front"]
    for x in (1, 2, 5, 6):
        img.putpixel((face[0] + x, face[1] + 4), (235, 255, 255, 255))

    rng = random.Random(11)
    tail = [(faces(0, 16, 5, 5, 3), 235, 170), (faces(0, 24, 3, 4, 2), 170, 100), (faces(10, 24, 1, 3, 1), 100, 40)]
    for f, a_top, a_bottom in tail:
        for name, (rx, ry, rw, rh) in f.items():
            for y in range(rh):
                t = y / max(1, rh - 1)
                if name == "top":
                    alpha = a_top
                elif name == "bottom":
                    alpha = a_bottom
                else:
                    alpha = int(a_top + (a_bottom - a_top) * t)
                for x in range(rw):
                    j = rng.randint(-8, 8)
                    img.putpixel((rx + x, ry + y), (150 + j, 215 + j, 245, alpha))
    return img


VLAD_SKIN = (236, 186, 172)
VLAD_SKIN_SHADE = (206, 154, 140)
VLAD_HAIR = (56, 40, 30)
VLAD_HAIR_DARK = (38, 28, 20)
# The entity texture is drawn on a 128 px grid and saved 2x larger so the face can be 16x16.
VLAD_DETAIL = 2
# Hand-drawn face: uneven dark fringe, heavy brows, grey-blue eyes, broad nose, closed mouth.
VLAD_FACE = [
    "hhhhhhhhhhhhhhhh",
    "hhihhhhhhihhhhhh",
    "hhhhhihhhhhhhihh",
    "hhhhhhhhhhhhhhhh",
    "hhhHhhhhhhhsslhh",
    "hhsshHhhsslllshh",
    "hsslssssllllsssh",
    "hsbbbbssssbbbbsh",
    "SsweewssssweewsS",
    "SscssssnnsssscsS",
    "SsccssnllnssccsS",
    "SsssssNnnNsssssS",
    "SssssssssssssssS",
    "jSssssMMMMssssSj",
    "jjSssssmmssssSjj",
    "jjjSssssssssSjjj",
]
VLAD_FACE_PALETTE = {
    "h": VLAD_HAIR, "H": VLAD_HAIR_DARK, "i": (78, 58, 44),
    "s": VLAD_SKIN, "S": (218, 168, 152), "l": (246, 208, 194), "c": (232, 162, 152), "j": VLAD_SKIN_SHADE,
    "b": (70, 50, 38), "w": (238, 236, 232), "e": (92, 116, 138),
    "n": (212, 158, 142), "N": (172, 118, 106), "m": (198, 122, 116), "M": (150, 86, 84),
}
JACKET = (84, 124, 122)
JACKET_DARK = (60, 94, 94)
HOOD = (30, 34, 42)
TEE = (22, 24, 30)
BLACK_TROUSERS = (26, 26, 30)
SNEAKER = (50, 50, 54)
SOLE = (206, 206, 204)
FRAME = (24, 26, 28)
DECAL = (236, 236, 232)
TYRE = (20, 20, 22)
KNOB = (48, 48, 50)
SPOKE = (156, 158, 162)
GRIP = (60, 60, 64)


def paint_vlad_head(c):
    f = faces(0, 0, 8, 8, 8)
    c.box(f, VLAD_SKIN, 4)
    # Short dark hair, cropped at the sides.
    c.fill(f["top"], VLAD_HAIR, 6)
    for side, front_col in (("right", 7), ("left", 0)):
        c.fill(f[side], VLAD_HAIR, 6, rows=range(2))
        c.fill(f[side], VLAD_HAIR_DARK, 4, rows=[2], cols=[x for x in range(8) if x != front_col])
        ear_x = 4 if side == "right" else 3
        c.px(f[side], ear_x, 4, VLAD_SKIN_SHADE)
        c.px(f[side], ear_x, 5, VLAD_SKIN_SHADE)
    c.fill(f["back"], VLAD_HAIR, 6, rows=range(3))
    c.fill(f["back"], VLAD_HAIR_DARK, 4, rows=[3])
    c.fill(f["bottom"], VLAD_SKIN_SHADE, 3)

    # The front is overdrawn by VLAD_FACE in vlad_logutov(); the outer layer only adds hair volume.
    hair = faces(32, 0, 8, 8, 8)
    c.fill(hair["top"], VLAD_HAIR, 8)
    for side in ("right", "left"):
        c.fill(hair[side], VLAD_HAIR, 8, rows=range(2))
    c.fill(hair["back"], VLAD_HAIR, 8, rows=range(3))

def paint_vlad_body(c):
    f = faces(16, 16, 8, 12, 4)
    c.box(f, JACKET, 5)
    for name in ("front", "back", "left", "right"):
        c.fill(f[name], JACKET_DARK, 4, rows=range(10, 12))  # ribbed hem
    front = f["front"]
    for y in range(3):
        for x in (3, 4):
            c.px(front, x, y, TEE, 2)
    for y in range(3, 12):
        c.px(front, 4, y, ZIP if y % 2 == 1 else (120, 120, 128))
    c.px(front, 6, 3, HOOD)  # chest logo
    c.px(front, 6, 8, JACKET_DARK)  # pocket zips
    c.px(front, 1, 8, JACKET_DARK)

    hood = faces(0, 32, 9, 3, 5)
    c.box(hood, HOOD, 4)
    c.fill(hood["front"], TEE, 2, cols=range(3, 6))


def paint_vlad_arm(c, u, v, fist_u, fist_v):
    f = faces(u, v, 4, 12, 4)
    c.box(f, JACKET, 5)
    for name in ("front", "back", "left", "right"):
        c.fill(f[name], JACKET_DARK, 4, rows=[8])  # cuff
        c.fill(f[name], VLAD_SKIN, 4, rows=range(9, 12))
    c.fill(f["bottom"], VLAD_SKIN_SHADE, 3)
    c.fill(f["top"], JACKET_DARK, 4)

    fist = faces(fist_u, fist_v, 4, 3, 4)
    c.box(fist, VLAD_SKIN, 4)
    c.fill(fist["bottom"], VLAD_SKIN_SHADE, 3)
    for name in ("front", "back", "left", "right"):
        c.fill(fist[name], VLAD_SKIN_SHADE, 3, rows=[2])


def paint_vlad_leg(c, u):
    c.box(faces(u, 0, 4, 6, 4), BLACK_TROUSERS, 3)
    shin = faces(u, 10, 4, 6, 4)
    c.box(shin, BLACK_TROUSERS, 3)
    for name in ("front", "back", "left", "right"):
        c.fill(shin[name], SNEAKER, 3, rows=[4])
        c.fill(shin[name], SOLE, 3, rows=[5])
    c.fill(shin["bottom"], SOLE, 3)


def paint_bike(c):
    # Wheel slab (2 x 12 x 5): tread on the end caps, a 2 px tyre segment under each cap and one spoke.
    slab = faces(0, 64, 2, 12, 5)
    for cap in ("top", "bottom"):
        for y in range(5):
            for x in range(2):
                c.px(slab[cap], x, y, KNOB if (x + y) % 2 == 0 else TYRE, 2)
    for side in ("right", "left"):
        c.fill(slab[side], TYRE, 3, rows=[0, 1, 10, 11])
        c.fill(slab[side], SPOKE, 4, rows=range(2, 10), cols=[2])
    c.box(faces(24, 64, 2, 2, 2), (70, 72, 76), 3)  # hub

    c.box(faces(0, 84, 1, 13, 1), FRAME, 3)
    down_tube = faces(8, 84, 1, 13, 2)
    c.box(down_tube, FRAME, 3)
    for side in ("right", "left"):
        for y in (2, 3, 5, 6, 8, 9):  # white lettering along the down tube
            c.px(down_tube[side], 1 if side == "right" else 0, y, DECAL)

    bar = faces(16, 84, 12, 1, 1)
    c.box(bar, FRAME, 3)
    for name in ("top", "bottom", "front", "back"):
        c.fill(bar[name], GRIP, 3, cols=[0, 1, 10, 11])
    saddle = faces(16, 88, 3, 1, 5)
    c.box(saddle, (18, 18, 20), 2)
    c.fill(saddle["top"], (34, 34, 38), 2, cols=[1])
    c.box(faces(36, 88, 2, 1, 3), GRIP, 4)  # pedal
    ring = faces(48, 88, 1, 3, 3)
    c.box(ring, (124, 126, 130), 4)
    for side in ("right", "left"):
        c.px(ring[side], 1, 1, FRAME)
    c.box(faces(16, 96, 4, 1, 1), FRAME, 3)  # fork crown


def vlad_logutov():
    c = Canvas(size=128, seed=11)
    paint_vlad_head(c)
    paint_vlad_body(c)
    paint_vlad_arm(c, 40, 16, 0, 40)
    paint_vlad_arm(c, 32, 48, 16, 40)
    paint_vlad_leg(c, 64)
    paint_vlad_leg(c, 80)
    paint_bike(c)
    img = c.img.resize((128 * VLAD_DETAIL, 128 * VLAD_DETAIL), Image.NEAREST)
    front = faces(0, 0, 8, 8, 8)["front"]
    assert len(VLAD_FACE) == 8 * VLAD_DETAIL and all(len(r) == 8 * VLAD_DETAIL for r in VLAD_FACE)
    for y, row in enumerate(VLAD_FACE):
        for x, key in enumerate(row):
            img.putpixel((front[0] * VLAD_DETAIL + x, front[1] * VLAD_DETAIL + y), VLAD_FACE_PALETTE[key] + (255,))
    return img


ICON = [
    "................................",
    "................................",
    "......................yyyy......",
    ".....................yyyyyy.....",
    ".....................yyyyyy.....",
    ".....................yyyyyy.....",
    "......................yyyy......",
    "................................",
    "................................",
    "....r...........................",
    "...rrr..............r...........",
    "..rrrrr............rrr..........",
    ".rrrrrrr..........rrrrr.........",
    ".wwwwwww.........rrrrrrr........",
    ".wwlwwlw.........wwwwwww........",
    ".wwwwwww.........wlwwwlw........",
    "gggggggggg.....ggggggggggggg....",
    "gggggggggggggggggggggggggggggggg",
    "gggggggggggggggggggggggggggggggg",
    "GGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGG",
    "GGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGG",
    "GGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGG",
    "GGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGG",
    "GGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGG",
    "GGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGG",
    "GGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGG",
    "GGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGG",
    "GGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGG",
    "GGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGG",
    "GGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGG",
    "dddddddddddddddddddddddddddddddd",
    "dddddddddddddddddddddddddddddddd",
]
ICON_WHEEL = ["...ttt...", ".tt...tt.", ".t.....t.", "t.......t", "t.......t", "t.......t", ".t.....t.",
              ".tt...tt.", "...ttt..."]
# Bike frame as (x, y) pixels: stays, seat tube, top tube, down tube, fork.
ICON_FRAME = [(10, 24), (11, 23), (11, 22), (12, 21), (10, 25), (11, 25), (12, 26), (13, 26), (14, 26),
              (13, 20), (13, 21), (14, 22), (14, 23), (14, 24), (15, 25), (15, 26),
              (14, 20), (15, 20), (16, 20), (17, 20), (18, 20), (19, 20), (20, 20), (21, 20),
              (20, 21), (19, 22), (18, 23), (17, 24), (16, 25),
              (21, 21), (22, 22), (22, 23), (22, 24), (23, 25)]
ICON_SADDLE = [(11, 19), (12, 19), (13, 19), (14, 19)]
ICON_BAR = [(21, 19), (21, 18), (20, 18), (22, 18)]
ICON_PALETTE = {
    "y": (255, 214, 96), "r": (112, 50, 40), "w": (150, 104, 66), "l": (255, 226, 130),
    "g": (74, 132, 70), "G": (98, 164, 84), "d": (146, 116, 80), "b": (24, 26, 28),
    "t": (36, 36, 40), "s": (18, 18, 20), "h": (60, 60, 64),
}


def icon():
    """Mod icon: the village at dusk with the bike in front, 32x32 art scaled up to 128x128."""
    img = Image.new("RGBA", (32, 32))
    for y, row in enumerate(ICON):
        t = y / 16
        sky = (int(72 + 170 * t), int(96 + 70 * t), int(170 - 60 * t))
        for x, key in enumerate(row):
            img.putpixel((x, y), ICON_PALETTE.get(key, sky) + (255,))
    for left in (5, 19):
        for y, row in enumerate(ICON_WHEEL):
            for x, key in enumerate(row):
                if key == "t":
                    img.putpixel((left + x, 21 + y), ICON_PALETTE["t"] + (255,))
    for points, key in ((ICON_FRAME, "b"), (ICON_SADDLE, "s"), (ICON_BAR, "h")):
        for x, y in points:
            img.putpixel((x, y), ICON_PALETTE[key] + (255,))
    return img.resize((128, 128), Image.NEAREST)


KOCH_SKIN = (246, 196, 174)
KOCH_SKIN_SHADE = (216, 162, 144)
KOCH_HAIR = (124, 88, 64)
KOCH_HAIR_DARK = (98, 66, 48)
TEE_RED = (196, 36, 40)
TEE_RED_DARK = (150, 24, 30)
TEE_PRINT = (112, 104, 108)
TEE_PRINT_DARK = (78, 72, 78)
# Hand-drawn face: short brown fringe with a low arched hairline, open grey-blue eyes,
# full rosy cheeks, broad nose, wide open smile showing teeth.
KOCH_FACE = [
    "hhhhhhhhhhhhhhhh",
    "hhhHhhhhhhhhHhhh",
    "hhhhssssssssshhh",
    "hhssslllllllsshh",
    "hsssssllllsssssh",
    "hsbbbbssssbbbbsh",
    "sssweessssweesss",
    "ssslllsssslllsss",
    "scccccsnnscccccs",
    "sccccsNnnNsccccs",
    "sssmssssssssmsss",
    "ssssmttttttmssss",
    "sssssmttttmsssss",
    "ssssssmmmmssssss",
    "jssssssssssssssj",
    "jjjjssssssssjjjj",
]
KOCH_FACE_PALETTE = {
    "h": KOCH_HAIR, "H": KOCH_HAIR_DARK, "s": KOCH_SKIN, "l": (254, 214, 196), "c": (238, 164, 150),
    "j": KOCH_SKIN_SHADE, "b": (166, 114, 94), "w": (240, 238, 234), "e": (96, 124, 150),
    "n": (226, 170, 152), "N": (184, 122, 108), "m": (172, 92, 86), "t": (246, 240, 232),
}


def little_koch():
    c = Canvas(size=64, seed=31)
    head = faces(0, 0, 8, 8, 8)
    c.box(head, KOCH_SKIN, 4)
    c.fill(head["top"], KOCH_HAIR, 6)
    for side in ("right", "left"):
        c.fill(head[side], KOCH_HAIR, 6, rows=range(3))
        ear_x = 4 if side == "right" else 3
        c.px(head[side], ear_x, 4, KOCH_SKIN_SHADE)
        c.px(head[side], ear_x, 5, KOCH_SKIN_SHADE)
    c.fill(head["back"], KOCH_HAIR, 6, rows=range(4))
    c.fill(head["bottom"], KOCH_SKIN_SHADE, 3)
    # Outer layer: a thin cap of cropped hair.
    cap = faces(32, 0, 8, 8, 8)
    c.fill(cap["top"], KOCH_HAIR, 8)
    for side in ("right", "left"):
        c.fill(cap[side], KOCH_HAIR, 8, rows=range(2))
    c.fill(cap["back"], KOCH_HAIR, 8, rows=range(3))
    c.fill(cap["front"], KOCH_HAIR, 8, rows=[0])

    body = faces(0, 16, 12, 8, 6)
    c.box(body, TEE_RED, 5)
    c.fill(body["front"], TEE_RED_DARK, 3, rows=[0], cols=range(4, 8))  # collar
    c.fill(body["bottom"], BLACK_TROUSERS, 3)
    for name in ("front", "back", "left", "right"):
        c.fill(body[name], TEE_RED_DARK, 4, rows=[7])  # hem
    belly = faces(36, 16, 10, 5, 2)
    c.box(belly, TEE_RED, 5)
    # Grey chest print, on the belly because it covers that part of the torso.
    c.fill(belly["front"], TEE_PRINT, 6, rows=[0, 1], cols=range(2, 8))
    for x in (3, 5, 6):
        c.px(belly["front"], x, 1, TEE_PRINT_DARK)
    c.fill(belly["front"], TEE_RED_DARK, 4, rows=[4])
    c.fill(belly["bottom"], TEE_RED_DARK, 3)

    for u in (0, 16):
        arm = faces(u, 32, 4, 9, 4)
        c.box(arm, KOCH_SKIN, 4)
        for name in ("front", "back", "left", "right"):
            c.fill(arm[name], TEE_RED, 5, rows=range(3))
            c.fill(arm[name], TEE_RED_DARK, 4, rows=[3])  # sleeve hem
            c.fill(arm[name], KOCH_SKIN_SHADE, 3, rows=[8])
        c.fill(arm["top"], TEE_RED, 5)
        c.fill(arm["bottom"], KOCH_SKIN_SHADE, 3)
    for u, v in ((32, 32), (0, 48)):
        leg = faces(u, v, 5, 6, 5)
        c.box(leg, BLACK_TROUSERS, 3)
        for name in ("front", "back", "left", "right"):
            c.fill(leg[name], SNEAKER, 3, rows=[4])
            c.fill(leg[name], (18, 18, 20), 2, rows=[5])
        c.fill(leg["bottom"], (18, 18, 20), 2)

    img = c.img.resize((128, 128), Image.NEAREST)
    front = head["front"]
    assert len(KOCH_FACE) == 16 and all(len(r) == 16 for r in KOCH_FACE)
    for y, row in enumerate(KOCH_FACE):
        for x, key in enumerate(row):
            img.putpixel((front[0] * 2 + x, front[1] * 2 + y), KOCH_FACE_PALETTE[key] + (255,))
    return img


FAT = [
    "................",
    "................",
    "....oooooo......",
    "..oofffffoooo...",
    ".offlllfffpppo..",
    ".offlffffppfffo.",
    "offfffppppffffo.",
    "offppppffffflfo.",
    "oppffffffflllfo.",
    "offfffffpppfffo.",
    ".offfppppfffffo.",
    ".ooffffffffffo..",
    "...oooffffooo...",
    "......oooo......",
    "................",
    "................",
]
FAT_PALETTE = {"o": (198, 172, 142), "f": (250, 240, 220), "p": (236, 170, 160), "l": (255, 252, 244)}


def fat():
    """A slab of pork fat with thin streaks of meat."""
    assert len(FAT) == 16 and all(len(r) == 16 for r in FAT)
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y, row in enumerate(FAT):
        for x, key in enumerate(row):
            if key != ".":
                img.putpixel((x, y), FAT_PALETTE[key] + (255,))
    return img


VASILY_SKIN = (234, 190, 170)
VASILY_SKIN_SHADE = (208, 160, 142)
VASILY_SKIN_LIGHT = (244, 208, 190)
VASILY_HAIR = (100, 84, 72)
VASILY_HAIR_DARK = (74, 62, 54)
SWEATPANTS = (158, 158, 162)
SWEATPANTS_DARK = (128, 128, 134)
# Hand-drawn face: short ash-brown hair with a straight, choppy fringe, low straight brows over
# wide grey-blue eyes one pixel high, broad nose, thin closed mouth with a slight smirk. Hair down the temples and
# shaded cheek and jaw edges make the face read narrower than the square head it is drawn on.
VASILY_FACE = [
    "hhhhhhhhhhhhhhhh",
    "hhHhhihhhhHhhihh",
    "hhhhhhhhhhhhhhhh",
    "hhhihhHhhihhhHhh",
    "hhhshsslsshsshhh",
    "hhssllllllllsshh",
    "hjbbbbssssbbbbjh",
    "hjswEesssseEwsjh",
    "hjssssssssssssjh",
    "JjssssslllssssjJ",
    "JjsssssnnsssssjJ",
    "JjssssNnnNssssjJ",
    "JjjsssssssmssjjJ",
    "JjjsssMmmmsssjjJ",
    "JJjjsssjjsssjjJJ",
    "JJJjjssssssjjJJJ",
]
VASILY_FACE_PALETTE = {
    "h": VASILY_HAIR, "H": VASILY_HAIR_DARK, "i": (128, 108, 92), "s": VASILY_SKIN, "j": VASILY_SKIN_SHADE,
    "J": (182, 136, 120), "l": VASILY_SKIN_LIGHT, "b": (92, 72, 60), "w": (234, 230, 226),
    "E": (44, 40, 44), "e": (92, 112, 134), "n": (218, 172, 154), "N": (178, 128, 114),
    "m": (186, 124, 116), "M": (160, 100, 96),
}


def vasily():
    c = Canvas(size=64, seed=47)
    head = faces(0, 0, 8, 8, 8)
    c.box(head, VASILY_SKIN, 4)
    c.fill(head["top"], VASILY_HAIR, 7)
    for side in ("right", "left"):
        c.fill(head[side], VASILY_HAIR, 7, rows=range(4))
        ear_x = 4 if side == "right" else 3
        c.px(head[side], ear_x, 4, VASILY_SKIN_SHADE)
        c.px(head[side], ear_x, 5, VASILY_SKIN_SHADE)
    c.fill(head["back"], VASILY_HAIR, 7, rows=range(5))
    c.fill(head["bottom"], VASILY_SKIN_SHADE, 3)
    # Outer layer: short hair standing slightly proud of the head; the fringe's choppy edge is on the face.
    mop = faces(32, 0, 8, 8, 8)
    c.fill(mop["top"], VASILY_HAIR, 9)
    c.fill(mop["front"], VASILY_HAIR, 9, rows=range(2))
    for side in ("right", "left"):
        c.fill(mop[side], VASILY_HAIR, 9, rows=range(2))
    c.fill(mop["back"], VASILY_HAIR, 9, rows=range(4))

    # Bare torso: collarbones, chest and stomach shading; a strip of white waistband above the sweatpants.
    body = faces(16, 16, 8, 12, 4)
    c.box(body, VASILY_SKIN, 4)
    front = body["front"]
    c.fill(front, VASILY_SKIN_SHADE, 3, rows=[0], cols=[1, 2, 5, 6])
    c.fill(front, VASILY_SKIN_LIGHT, 3, rows=[2, 3], cols=[1, 2, 5, 6])
    c.fill(front, VASILY_SKIN_SHADE, 3, rows=[4], cols=[1, 2, 3, 4, 5, 6])
    for y in (6, 8):
        c.fill(front, VASILY_SKIN_SHADE, 3, rows=[y], cols=[2, 3, 4, 5])
    for y in range(5, 10):
        c.px(front, 3 if y % 2 else 4, y, VASILY_SKIN_SHADE)
    c.px(front, 4, 9, (190, 142, 124))  # navel
    c.fill(body["back"], VASILY_SKIN_SHADE, 3, cols=[3, 4], rows=range(1, 9))
    for name in ("front", "back", "left", "right"):
        c.fill(body[name], (236, 238, 244), 3, rows=[10])
        c.fill(body[name], SWEATPANTS, 4, rows=[11])
    c.px(front, 3, 10, (60, 110, 200))
    c.px(front, 5, 10, (60, 110, 200))
    c.fill(body["bottom"], SWEATPANTS, 4)

    for u, v in ((40, 16), (32, 48)):
        arm = faces(u, v, 4, 12, 4)
        c.box(arm, VASILY_SKIN, 4)
        for name in ("front", "back", "left", "right"):
            c.fill(arm[name], VASILY_SKIN_LIGHT, 3, rows=[2, 3], cols=[1, 2])
            c.fill(arm[name], VASILY_SKIN_SHADE, 3, rows=[9])
            c.fill(arm[name], (228, 176, 160), 3, rows=[10, 11])  # reddened hands
        c.fill(arm["bottom"], VASILY_SKIN_SHADE, 3)
    for u, v in ((0, 16), (16, 48)):
        leg = faces(u, v, 4, 12, 4)
        c.box(leg, SWEATPANTS, 5)
        for name in ("front", "back", "left", "right"):
            c.fill(leg[name], SWEATPANTS_DARK, 4, rows=[0, 9])  # waistband and cuff
            c.fill(leg[name], (232, 232, 234), 3, rows=[10])
            c.fill(leg[name], (190, 190, 194), 3, rows=[11])
        c.px(leg["front"], 3 if u == 0 else 0, 1, (226, 226, 230))  # drawstring
        c.px(leg["front"], 3 if u == 0 else 0, 2, (226, 226, 230))
        c.fill(leg["bottom"], (190, 190, 194), 3)

    img = c.img.resize((128, 128), Image.NEAREST)
    front = head["front"]
    assert len(VASILY_FACE) == 16 and all(len(r) == 16 for r in VASILY_FACE)
    for y, row in enumerate(VASILY_FACE):
        for x, key in enumerate(row):
            img.putpixel((front[0] * 2 + x, front[1] * 2 + y), VASILY_FACE_PALETTE[key] + (255,))
    return img


TABBY = (112, 100, 88)
TABBY_STRIPE = (54, 47, 41)
TABBY_DARK = (86, 76, 66)
TABBY_LIGHT = (152, 138, 120)
TABBY_CREAM = (214, 204, 186)
TABBY_WHITE = (238, 236, 230)
KHAKI = (118, 112, 70)
CANOPY_BLUE = (70, 140, 220)
CANOPY_WHITE = (240, 242, 246)
# Hand-drawn 16x14 face: striped forehead with the tabby "M", green-yellow eyes with slit pupils,
# white muzzle, cheeks and chin, dark cheek stripes.
BARSIK_FACE = [
    "ffffdffddffdffff",
    "fffdffdffdffdfff",
    "ffdffdffffdffdff",
    "ffllllffffllllff",
    "fdKKKKlffllKKKdf",
    "fleGGelffleGGelf",
    "fdllllfffflllldf",
    "ddfllwwwwwwllfdd",
    "fdfwwwwwwwwwwfdf",
    "ffwwwwwwwwwwwwff",
    "dfwwwwwwwwwwwwfd",
    "ffdwwwwwwwwwwdff",
    "fdffwwwwwwwwffdf",
    "ffdfffwwwwfffdff",
]
# Front of the muzzle block: pink-brown nose, mouth line and whisker dots.
BARSIK_MUZZLE = [
    "wwwnnwww",
    "wqwmmwqw",
    "wwmwwmww",
    "wwwwwwww",
]
BARSIK_PALETTE = {
    "f": TABBY, "d": TABBY_STRIPE, "l": TABBY_LIGHT, "K": (40, 34, 30), "e": (176, 186, 84),
    "G": (22, 22, 20), "w": TABBY_WHITE, "n": (156, 98, 92), "m": (96, 70, 66), "q": (200, 196, 190),
}


def barsik():
    """256x256 image for a 128x128 model texture, so every model pixel is 2x2 texture pixels."""
    img = Image.new("RGBA", (256, 256), (0, 0, 0, 0))
    rng = random.Random(61)

    def box(u, v, w, h, d):
        return faces(2 * u, 2 * v, 2 * w, 2 * h, 2 * d)

    def paint(rect, color_at, jitter=5):
        for y in range(rect[3]):
            for x in range(rect[2]):
                j = rng.randint(-jitter, jitter)
                color = color_at(x, y)
                img.putpixel((rect[0] + x, rect[1] + y), tuple(max(0, min(255, c + j)) for c in color) + (255,))

    def flat(color):
        return lambda x, y: color

    def grid(rect, rows):
        for y, row in enumerate(rows):
            for x, key in enumerate(row):
                img.putpixel((rect[0] + x, rect[1] + y), BARSIK_PALETTE[key] + (255,))

    body = box(0, 0, 12, 9, 14)
    # Back: a dark spine with chevron stripes running off it.
    paint(body["top"], lambda x, y: TABBY_STRIPE if x in (11, 12) or (y + abs(2 * x - 23) // 5) % 6 < 2 else TABBY)
    for side in ("right", "left"):
        paint(body[side], lambda x, y: TABBY_LIGHT if y >= 14 else
              TABBY_STRIPE if (x + y // 3) % 6 < 2 else TABBY)
    paint(body["front"], lambda x, y: TABBY_CREAM if 8 <= x < 16 and y >= 9 else TABBY)
    paint(body["back"], lambda x, y: TABBY_STRIPE if y % 6 < 2 and y < 14 else TABBY)
    paint(body["bottom"], flat(TABBY_CREAM))
    belly = box(0, 36, 11, 2, 10)
    for name, rect in belly.items():
        paint(rect, flat(TABBY_CREAM if name == "bottom" else TABBY_LIGHT))

    head = box(0, 23, 8, 7, 6)
    paint(head["top"], lambda x, y: TABBY_STRIPE if x in (2, 3, 7, 8, 12, 13) else TABBY)
    for side in ("right", "left"):
        # Cheek stripes sweep back from the eye; 'right' has the face on its right edge.
        paint(head[side], lambda x, y, side=side: TABBY_STRIPE
              if y in (5, 6, 9, 10) and (x >= 3 if side == "right" else x <= 8) else TABBY)
    paint(head["back"], lambda x, y: TABBY_STRIPE if y % 5 < 2 else TABBY)
    paint(head["bottom"], flat(TABBY_WHITE))
    grid(head["front"], BARSIK_FACE)
    for u, v in ((52, 0), (52, 3)):
        ear = box(u, v, 2, 2, 1)
        for rect in ear.values():
            paint(rect, flat(TABBY_DARK))
        for x, y in ((1, 2), (2, 2), (1, 3), (2, 3), (1, 1), (2, 1)):
            img.putpixel((ear["front"][0] + x, ear["front"][1] + y), (176, 132, 122, 255))
    muzzle = box(52, 6, 4, 2, 1)
    for rect in muzzle.values():
        paint(rect, flat(TABBY_WHITE), 3)
    grid(muzzle["front"], BARSIK_MUZZLE)

    for u, v in ((28, 23), (40, 23), (28, 29), (40, 29)):
        leg = box(u, v, 3, 3, 3)
        for name in ("front", "back", "left", "right"):
            paint(leg[name], lambda x, y: TABBY_STRIPE if y in (1, 2) else TABBY_LIGHT if y >= 4 else TABBY)
        paint(leg["top"], flat(TABBY))
        paint(leg["bottom"], flat((128, 96, 90)))  # paw pads
    for (u, v, length), tip in (((42, 36, 7), False), ((0, 48, 5), True)):
        tail = box(u, v, 2, 2, length)
        # The tail runs along the box's depth: that is x on its sides and y on its top and bottom.
        for name in ("right", "left"):
            paint(tail[name], lambda x, y, tip=tip: TABBY_STRIPE if tip or x % 5 < 2 else TABBY)
        for name in ("top", "bottom"):
            paint(tail[name], lambda x, y, tip=tip: TABBY_STRIPE if tip or y % 5 < 2 else TABBY)
        paint(tail["front"], flat(TABBY))
        paint(tail["back"], flat(TABBY_STRIPE))

    leather, brass, glass = (96, 62, 38), (190, 150, 60), (112, 182, 216)
    for rect in box(14, 48, 8, 2, 6).values():
        paint(rect, flat(leather), 4)
    for u in (42, 50):
        lens = box(u, 48, 3, 3, 1)
        for rect in lens.values():
            paint(rect, flat(brass), 4)
        paint(lens["front"], lambda x, y: brass if x in (0, 5) or y in (0, 5) else
              (230, 244, 250) if (x, y) in ((1, 1), (2, 1), (1, 2)) else glass, 3)

    harness = box(88, 64, 12, 9, 3)
    for rect in harness.values():
        paint(rect, flat(KHAKI), 6)
    paint(harness["top"], lambda x, y: (176, 178, 184) if 10 <= x < 14 and 1 <= y < 5 else KHAKI, 4)  # buckle
    for rect in box(88, 76, 6, 2, 4).values():
        paint(rect, flat((92, 88, 56)), 6)
    for rect in box(120, 64, 1, 22, 1).values():
        paint(rect, flat((64, 64, 70)), 3)

    def gores(size):
        """Eight alternating blue and white gores radiating from the centre of a square panel."""
        c = (size - 1) / 2
        return lambda x, y: CANOPY_BLUE if int((math.atan2(y - c, x - c) + math.pi) / (2 * math.pi) * 8) % 2 else CANOPY_WHITE

    def darker(color_at):
        return lambda x, y: tuple(int(c * 0.72) for c in color_at(x, y))

    for (u, v, w, h), band in (((0, 64, 22, 4), 11), ((0, 90, 16, 2), 8)):
        canopy = box(u, v, w, h, w)
        paint(canopy["top"], gores(2 * w), 4)
        paint(canopy["bottom"], darker(gores(2 * w)), 4)
        for name in ("front", "back", "left", "right"):
            paint(canopy[name], lambda x, y, band=band: CANOPY_BLUE if (x // band) % 2 else CANOPY_WHITE, 4)
    return img


class Hi:
    """128x128 canvas for a 64x64 model texture: every model pixel is 2x2 texture pixels."""

    SIDES = ("front", "back", "left", "right")

    def __init__(self, seed):
        self.img = Image.new("RGBA", (128, 128), (0, 0, 0, 0))
        self.rng = random.Random(seed)

    @staticmethod
    def box(u, v, w, h, d):
        return faces(2 * u, 2 * v, 2 * w, 2 * h, 2 * d)

    def paint(self, rect, color_at, jitter=4):
        """color_at is a colour or a function of (x, y) that may return None to leave a pixel clear."""
        for y in range(rect[3]):
            for x in range(rect[2]):
                color = color_at(x, y) if callable(color_at) else color_at
                if color is None:
                    continue
                j = self.rng.randint(-jitter, jitter)
                self.img.putpixel((rect[0] + x, rect[1] + y), tuple(max(0, min(255, c + j)) for c in color) + (255,))

    def paint_box(self, box, color_at, jitter=4, only=None):
        for name, rect in box.items():
            if only is None or name in only:
                self.paint(rect, color_at, jitter)

    def grid(self, rect, rows, palette):
        assert len(rows) == rect[3] and all(len(row) == rect[2] for row in rows)
        for y, row in enumerate(rows):
            for x, key in enumerate(row):
                self.img.putpixel((rect[0] + x, rect[1] + y), palette[key] + (255,))


def strands(base, light):
    """Hair with thin lighter streaks running down it."""
    return lambda x, y: light if (3 * x + y // 3) % 7 == 0 else base


ALEXEY_SKIN = (226, 180, 152)
ALEXEY_SKIN_SHADE = (198, 150, 126)
ALEXEY_HAIR = (50, 38, 32)
ALEXEY_HAIR_LIGHT = (72, 56, 46)
HOODIE = (88, 80, 80)
HOODIE_DARK = (66, 60, 60)
PUFFER = (40, 36, 38)
PUFFER_SEAM = (22, 20, 22)
BLACK_JEANS = (28, 28, 32)
# Hand-drawn face: dark hair with a straight fringe down to the brows, dark eyes, a slight closed
# smile. Shaded cheek and jaw edges make the face read slimmer than the square head it is drawn on.
ALEXEY_FACE = [
    "hhhhhhhhhhhhhhhh",
    "hhHhhhhHhhhhHhhh",
    "hhhhhhhhhhhhhhhh",
    "hhhHhhhhhHhhhhhh",
    "hhhhhhHhhhhhHhhh",
    "hhsssssssssssshh",
    "hjbbbbssssbbbbjh",
    "hjswEesssseEwsjh",
    "hjssssssssssssjh",
    "JjsssssnnsssssjJ",
    "JjssssNnnNssssjJ",
    "JjssssssssssssjJ",
    "JjjssmMMMMmssjjJ",
    "JjjssssssssssjjJ",
    "JJjjssssssssjjJJ",
    "JJJjjssssssjjJJJ",
]
ALEXEY_FACE_PALETTE = {
    "h": ALEXEY_HAIR, "H": ALEXEY_HAIR_LIGHT, "s": ALEXEY_SKIN, "j": ALEXEY_SKIN_SHADE, "J": (172, 126, 106),
    "b": (44, 34, 30), "w": (236, 232, 226), "E": (30, 24, 22), "e": (84, 58, 42),
    "n": (212, 164, 138), "N": (172, 124, 104), "m": (204, 140, 124), "M": (170, 104, 96),
}


def alexey():
    c = Hi(71)
    head = c.box(0, 0, 8, 8, 8)
    c.paint_box(head, ALEXEY_SKIN)
    c.paint(head["top"], strands(ALEXEY_HAIR, ALEXEY_HAIR_LIGHT), 6)
    for side in ("right", "left"):
        # Short at the sides, fading out above the ear.
        c.paint(head[side], lambda x, y: ALEXEY_HAIR if y < 5 else (112, 86, 72) if y < 7 else
                ALEXEY_SKIN_SHADE if 8 <= y <= 10 and 7 <= x <= 8 else ALEXEY_SKIN)
    c.paint(head["back"], lambda x, y: ALEXEY_HAIR if y < 9 else (112, 86, 72) if y < 11 else ALEXEY_SKIN)
    c.paint(head["bottom"], ALEXEY_SKIN_SHADE, 3)
    c.grid(head["front"], ALEXEY_FACE, ALEXEY_FACE_PALETTE)
    # Outer layer: the volume of the hair on top and the fringe standing off the forehead.
    hair = c.box(32, 0, 8, 8, 8)
    c.paint(hair["top"], strands(ALEXEY_HAIR, ALEXEY_HAIR_LIGHT), 6)
    c.paint(hair["front"], lambda x, y: ALEXEY_HAIR if y < 4 else None, 6)
    for side in ("right", "left"):
        c.paint(hair[side], lambda x, y: ALEXEY_HAIR if y < 4 else None, 6)
    c.paint(hair["back"], lambda x, y: ALEXEY_HAIR if y < 8 else None, 6)

    # Hoodie with a white T-shirt showing at the neck; the vest goes over it.
    body = c.box(16, 16, 8, 12, 4)
    c.paint_box(body, HOODIE)
    c.paint(body["front"], lambda x, y: (238, 238, 236) if y < 5 and abs(x - 7.5) <= 2.5 - y * 0.5 else HOODIE)
    c.paint(body["bottom"], BLACK_JEANS, 3)

    def quilt(x, y):
        return PUFFER_SEAM if y % 4 == 3 else (54, 50, 52) if y % 4 == 0 else PUFFER

    vest = c.box(24, 32, 8, 12, 4)
    c.paint_box(vest, quilt, 3, only=("back", "left", "right"))
    # Open at the collar, zipped below it.
    c.paint(vest["front"], lambda x, y: (None if abs(x - 7.5) <= 2.5 - y * 0.5 else quilt(x, y)) if y < 5 else
            (150, 150, 156) if x == 8 else PUFFER_SEAM if x == 7 else quilt(x, y), 3)
    c.paint(vest["top"], lambda x, y: None if 5 <= x <= 10 else PUFFER, 3)
    c.paint(vest["bottom"], PUFFER, 3)
    hood = c.box(0, 32, 8, 3, 2)
    c.paint_box(hood, HOODIE)
    c.paint(hood["top"], HOODIE_DARK)
    c.paint(hood["back"], lambda x, y: HOODIE_DARK if y == 5 else HOODIE)

    for u, v in ((40, 16), (32, 48)):
        arm = c.box(u, v, 4, 12, 4)
        c.paint_box(arm, HOODIE)
        for name in Hi.SIDES:
            c.paint(arm[name], lambda x, y: HOODIE_DARK if y in (18, 19) else ALEXEY_SKIN if y >= 20 else HOODIE)
        c.paint(arm["bottom"], ALEXEY_SKIN_SHADE, 3)
    for u, v in ((0, 16), (16, 48)):
        leg = c.box(u, v, 4, 12, 4)
        c.paint_box(leg, BLACK_JEANS, 3)
        for name in Hi.SIDES:
            c.paint(leg[name], lambda x, y: (48, 48, 52) if y in (20, 21) else (84, 84, 88) if y >= 22 else BLACK_JEANS, 3)
        c.paint(leg["bottom"], (84, 84, 88), 3)
    return c.img


RITA_SKIN = (234, 192, 168)
RITA_SKIN_SHADE = (208, 162, 140)
RITA_HAIR = (64, 46, 38)
RITA_HAIR_LIGHT = (96, 70, 54)
RITA_TEE = (150, 170, 178)
RITA_TEE_SHADE = (124, 144, 154)
# Hand-drawn face: long dark hair parted in the middle and framing the face, defined brows,
# brown eyes, full lips.
RITA_FACE = [
    "hhhhhhhhhhhhhhhh",
    "hhhhhhhHHhhhhhhh",
    "hhhhhsssssshhhhh",
    "hhhsssssssssshhh",
    "hhsssssssssssshh",
    "hhsbbbssssbbbshh",
    "hhwEesssssseEwhh",
    "hhsssssssssssshh",
    "hhcssssnnsssschh",
    "hhssssNnnNsssshh",
    "hhsssssssssssshh",
    "hhssssMMMMsssshh",
    "hhsssssmmssssshh",
    "hhhsssssssssshhh",
    "hhhjssssssssjhhh",
    "hhhhjjssssjjhhhh",
]
RITA_FACE_PALETTE = {
    "h": RITA_HAIR, "H": (44, 32, 28), "s": RITA_SKIN, "j": RITA_SKIN_SHADE, "c": (236, 176, 160),
    "b": (56, 40, 34), "w": (240, 236, 230), "E": (34, 26, 24), "e": (110, 70, 44),
    "n": (220, 174, 152), "N": (184, 134, 114), "M": (186, 96, 98), "m": (212, 128, 126),
}


def rita():
    c = Hi(83)
    hair_color = strands(RITA_HAIR, RITA_HAIR_LIGHT)
    head = c.box(0, 0, 8, 8, 8)
    c.paint_box(head, hair_color, 5)
    c.paint(head["top"], lambda x, y: (44, 32, 28) if x in (7, 8) and y > 6 else hair_color(x, y), 5)
    c.paint(head["bottom"], RITA_SKIN_SHADE, 3)
    c.grid(head["front"], RITA_FACE, RITA_FACE_PALETTE)
    # Outer layer: hair all round, open at the front except for the locks either side of the face.
    hair = c.box(32, 0, 8, 8, 8)
    c.paint_box(hair, hair_color, 5, only=("top", "back", "left", "right"))
    c.paint(hair["front"], lambda x, y: hair_color(x, y) if y < 2 and not 5 <= x <= 10 or x < 2 or x > 13 else None, 5)

    # Cropped T-shirt, bare midriff, high-waisted black jeans with a button.
    def outfit(x, y):
        return RITA_TEE if y < 14 else RITA_SKIN if y < 18 else BLACK_JEANS

    body = c.box(16, 16, 8, 12, 4)
    c.paint_box(body, outfit, only=Hi.SIDES)
    c.paint(body["front"], lambda x, y: (198, 150, 130) if (x, y) in ((7, 16), (8, 16)) else
            (170, 172, 178) if (x, y) in ((7, 18), (8, 18)) else
            (46, 46, 52) if y == 19 else outfit(x, y))
    c.paint(body["top"], RITA_TEE)
    c.paint(body["bottom"], BLACK_JEANS, 3)
    # The loose T-shirt, standing off the body and open at the hem.
    tee = c.box(36, 32, 8, 7, 4)
    c.paint_box(tee, lambda x, y: RITA_TEE_SHADE if y == 13 or (x + 2 * y) % 11 == 0 else RITA_TEE, only=Hi.SIDES)
    c.paint(tee["front"], lambda x, y: RITA_SKIN if y < 2 and 5 <= x <= 10 else
            RITA_TEE_SHADE if y == 2 and 5 <= x <= 10 or y == 13 else RITA_TEE)
    c.paint(tee["top"], lambda x, y: None if 5 <= x <= 10 else RITA_TEE)

    for u, v in ((40, 16), (32, 48)):
        arm = c.box(u, v, 3, 12, 4)
        c.paint_box(arm, RITA_SKIN)
        for name in Hi.SIDES:
            # Sleeve to the elbow; pale nails at the fingertips.
            c.paint(arm[name], lambda x, y: RITA_TEE if y < 9 else RITA_TEE_SHADE if y == 9 else
                    (244, 236, 232) if y == 23 and x % 2 == 0 else RITA_SKIN)
        c.paint(arm["top"], RITA_TEE)
        c.paint(arm["bottom"], RITA_SKIN_SHADE, 3)
    for u, v in ((0, 16), (16, 48)):
        leg = c.box(u, v, 4, 12, 4)
        c.paint_box(leg, BLACK_JEANS, 3)
        for name in Hi.SIDES:
            c.paint(leg[name], lambda x, y: (232, 232, 234) if y in (20, 21) else (196, 196, 200) if y >= 22 else
                    (40, 40, 46) if x == 0 else BLACK_JEANS, 3)
        c.paint(leg["bottom"], (196, 196, 200), 3)

    c.paint_box(c.box(0, 32, 8, 9, 1), hair_color, 5)
    for u in (20, 28):
        c.paint_box(c.box(u, 32, 2, 7, 1), hair_color, 5)
    return c.img


def trip_icon():
    """18x18 status effect icon: a rainbow spiral."""
    img = Image.new("RGBA", (18, 18), (0, 0, 0, 0))
    for y in range(18):
        for x in range(18):
            dx, dy = x - 8.5, y - 8.5
            d = math.hypot(dx, dy)
            if d > 8.2:
                continue
            turn = (math.atan2(dy, dx) / (2 * math.pi) + d / 6.0) % 1.0
            r, g, b = colorsys.hsv_to_rgb(turn, 0.85, 1.0 if int(turn * 6) % 2 == 0 else 0.75)
            img.putpixel((x, y), (int(r * 255), int(g * 255), int(b * 255), 255))
    return img


def main():
    os.makedirs(f"{OUT}/entity", exist_ok=True)
    base = kalabushkin()
    base.save(f"{OUT}/entity/kalabushkin.png")
    spirit(base).save(f"{OUT}/entity/kalabushkin_spirit.png")
    vlad_logutov().save(f"{OUT}/entity/vlad_logutov.png")
    assert len(ICON) == 32 and all(len(r) == 32 for r in ICON)
    icon().save(f"{OUT}/../icon.png")
    little_koch().save(f"{OUT}/entity/little_koch.png")
    os.makedirs(f"{OUT}/item", exist_ok=True)
    fat().save(f"{OUT}/item/fat.png")
    vasily().save(f"{OUT}/entity/vasily.png")
    barsik().save(f"{OUT}/entity/barsik.png")
    alexey().save(f"{OUT}/entity/alexey_srpava.png")
    rita().save(f"{OUT}/entity/rita.png")
    os.makedirs(f"{OUT}/mob_effect", exist_ok=True)
    trip_icon().save(f"{OUT}/mob_effect/trip.png")


if __name__ == "__main__":
    main()
