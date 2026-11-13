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
    os.makedirs(f"{OUT}/mob_effect", exist_ok=True)
    trip_icon().save(f"{OUT}/mob_effect/trip.png")


if __name__ == "__main__":
    main()
