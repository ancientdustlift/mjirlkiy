#!/usr/bin/env python3
"""Generates the mod's pixel-art textures. Run from the repository root: python3 tools/generate_textures.py"""
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


def item(rows, palette):
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch != ".":
                img.putpixel((x, y), palette[ch])
    return img


ITEMS = {
    "architect_ruler": (
        [
            "................",
            ".............oo.",
            "............oyyo",
            "...........oyhyo",
            "..........oykyo.",
            ".........oyhyo..",
            "........oykyo...",
            ".......oyhyo....",
            "......oykyo.....",
            ".....oyhyo......",
            "....oykyo.......",
            "...oyhyo........",
            "..oykyo.........",
            ".oyhyo..........",
            ".oyyo...........",
            "..oo............",
        ],
        {"o": (120, 84, 30, 255), "y": (232, 196, 96, 255), "h": (248, 224, 140, 255), "k": (70, 50, 24, 255)},
    ),
    "drafting_compass": (
        [
            ".......kk.......",
            "......kggk......",
            "......kggk......",
            ".....kswwsk.....",
            ".....kswwsk.....",
            "....ksw..wsk....",
            "....ksk..ksk....",
            "...ksw....wsk...",
            "...ksk....ksk...",
            "..ksw......wsk..",
            "..ksk......ksk..",
            ".ksw........wsk.",
            ".ksk........kyk.",
            ".kk..........yk.",
            ".k...........p..",
            "................",
        ],
        {"k": (54, 58, 68, 255), "s": (150, 156, 168, 255), "w": (214, 220, 230, 255),
         "g": (212, 170, 60, 255), "y": (232, 196, 96, 255), "p": (40, 40, 44, 255)},
    ),
    "set_square": (
        [
            "................",
            ".dd.............",
            ".dtd............",
            ".dttd...........",
            ".dtttd..........",
            ".dtlttd.........",
            ".dtl.ttd........",
            ".dtl..ttd.......",
            ".dtl...ttd......",
            ".dtl....ttd.....",
            ".dtl.....ttd....",
            ".dtllllllltdd...",
            ".dtttttttttttd..",
            ".dtmtmtmtmtmttd.",
            ".dddddddddddddd.",
            "................",
        ],
        {"d": (24, 110, 110, 255), "t": (70, 196, 190, 215), "l": (150, 232, 226, 235), "m": (20, 80, 84, 255)},
    ),
    "architect_pencil": (
        [
            "................",
            "............pp..",
            "...........prrp.",
            "..........pmrrp.",
            ".........oymmp..",
            "........oyhyo...",
            ".......oyhyo....",
            "......oyhyo.....",
            ".....oyhyo......",
            "....oyhyo.......",
            "...oyhyo........",
            "..owyyo.........",
            "..owwo..........",
            ".kwwo...........",
            ".kk.............",
            "................",
        ],
        {"o": (130, 90, 20, 255), "y": (240, 190, 40, 255), "h": (252, 226, 110, 255), "w": (226, 190, 140, 255),
         "k": (40, 40, 44, 255), "p": (150, 60, 80, 255), "r": (230, 120, 140, 255), "m": (170, 176, 186, 255)},
    ),
    "blueprint": (
        [
            "................",
            ".dddddddddddddd.",
            ".dbbbbbbbbbbbbd.",
            ".dbwwwwwwbbbbbd.",
            ".dbwbbbbwbbwbbd.",
            ".dbwbbbbwbwbwbd.",
            ".dbwbbbbwbbwbbd.",
            ".dbwwbwwwbbbbbd.",
            ".dbbbbbbbbbbbbd.",
            ".dbwwwwwwwwwwbd.",
            ".dbbbbbbbbbbbbd.",
            ".dbwwwwbbwwwbbd.",
            ".dbbbbbbbbbbbbd.",
            ".dllllllllllllld",
            ".ddddddddddddddd",
            "................",
        ],
        {"d": (20, 50, 120, 255), "b": (40, 96, 190, 255), "w": (220, 236, 255, 255), "l": (90, 140, 220, 255)},
    ),
}


def main():
    os.makedirs(f"{OUT}/entity", exist_ok=True)
    os.makedirs(f"{OUT}/item", exist_ok=True)
    base = kalabushkin()
    base.save(f"{OUT}/entity/kalabushkin.png")
    spirit(base).save(f"{OUT}/entity/kalabushkin_spirit.png")
    for name, (rows, palette) in ITEMS.items():
        assert len(rows) == 16 and all(len(r) == 16 for r in rows), name
        item(rows, palette).save(f"{OUT}/item/{name}.png")


if __name__ == "__main__":
    main()
