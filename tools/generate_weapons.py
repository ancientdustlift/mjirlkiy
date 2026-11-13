#!/usr/bin/env python3
"""Generates textures and item models (including swing and spin animation frames) for the carpenter's
weapons, the Shatuni and the things Vasily hands out. Run from the repository root: python3 tools/generate_weapons.py

The swing animation is a strip of models picked by the "kalabushkin:swing" item predicate."""
import json
import math
import os
import random

from PIL import Image

ASSETS = "src/main/resources/assets/kalabushkin"
FRAMES = 10


def lerp_keys(keys, t):
    """Smoothstep interpolation through (time, value) keyframes."""
    for (t0, v0), (t1, v1) in zip(keys, keys[1:]):
        if t <= t1:
            k = (t - t0) / (t1 - t0)
            k = k * k * (3 - 2 * k)
            return v0 + (v1 - v0) * k
    return keys[-1][1]


def r(x):
    return round(x, 3)


# ---------------------------------------------------------------- textures

def wood(img, rng, rect, base, vertical=False, jitter=7):
    x0, y0, x1, y1 = rect
    for y in range(y0, y1):
        for x in range(x0, x1):
            line = x if vertical else y
            grain = -12 if (line * 7 + 3) % 5 == 0 else 0
            j = rng.randint(-jitter, jitter) + grain
            img.putpixel((x, y), tuple(max(0, min(255, c + j)) for c in base) + (255,))


def rings(img, rng, rect, base):
    x0, y0, x1, y1 = rect
    cx, cy = (x0 + x1 - 1) / 2, (y0 + y1 - 1) / 2
    for y in range(y0, y1):
        for x in range(x0, x1):
            ring = int(max(abs(x - cx), abs(y - cy)))
            j = (-16 if ring % 2 else 6) + rng.randint(-4, 4)
            img.putpixel((x, y), tuple(max(0, min(255, c + j)) for c in base) + (255,))


def metal(img, rng, rect, base):
    x0, y0, x1, y1 = rect
    for y in range(y0, y1):
        for x in range(x0, x1):
            j = rng.randint(-5, 5) + (18 if (x + y) % 4 == 0 else 0)
            img.putpixel((x, y), tuple(max(0, min(255, c + j)) for c in base) + (255,))


def mallet_texture():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    rng = random.Random(11)
    wood(img, rng, (0, 0, 16, 6), (196, 148, 88))        # head, side grain
    for x in (1, 8):                                      # darker bands near the striking faces
        for y in range(6):
            img.putpixel((x, y), (120, 82, 44, 255))
    rings(img, rng, (0, 6, 6, 12), (214, 170, 108))      # head, end grain
    wood(img, rng, (6, 6, 8, 16), (128, 88, 50), True)   # handle
    for y in (13, 14):                                    # grip wrap
        for x in (6, 7):
            img.putpixel((x, y), (84, 56, 34, 255))
    return img


def plane_texture():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    rng = random.Random(23)
    wood(img, rng, (0, 0, 14, 4), (186, 132, 70))        # body sides
    wood(img, rng, (0, 4, 14, 8), (200, 146, 80))        # body top
    metal(img, rng, (0, 8, 14, 12), (150, 156, 168))     # steel sole
    for y in range(4, 12):                                # the mouth the blade passes through
        img.putpixel((6, y), (36, 30, 28, 255))
    rings(img, rng, (0, 12, 4, 16), (206, 156, 92))      # body ends
    wood(img, rng, (4, 12, 6, 16), (112, 70, 40), True)  # handles
    metal(img, rng, (6, 12, 9, 16), (176, 184, 198))     # blade
    for x in range(6, 9):
        img.putpixel((x, 15), (236, 240, 248, 255))      # cutting edge
    wood(img, rng, (9, 12, 11, 16), (240, 214, 160), True, 5)  # shaving
    return img


# ---------------------------------------------------------------- models

def box(frm, to, faces, rotation=None):
    element = {"from": [r(v) for v in frm], "to": [r(v) for v in to],
               "faces": {name: ({"uv": uv, "texture": "#0"} if not isinstance(uv, dict) else uv)
                         for name, uv in faces.items()}}
    if rotation:
        element["rotation"] = rotation
    return element


def all_faces(uv):
    return {side: uv for side in ("north", "south", "east", "west", "up", "down")}


def mallet_elements(t):
    # Squash and stretch: the head flattens for a moment at the point of impact.
    squash = lerp_keys([(0, 0), (0.4, 0), (0.5, 1), (0.62, 0), (1, 0)], t)
    half = 5 - 0.7 * squash
    grow = 0.5 * squash
    side, end, handle = [0, 0, 10, 5], [0, 6, 6, 11], [6, 6, 8, 16]
    return [
        box([7, 0, 7], [9, 11.5, 9], {**all_faces(handle), "down": [6, 15, 8, 16], "up": [6, 6, 8, 7]}),
        box([8 - half, 11 - grow, 5 - grow], [8 + half, 16 + grow, 11 + grow],
            {"north": side, "south": side, "up": [0, 0, 10, 6], "down": [0, 0, 10, 6], "east": end, "west": end}),
    ]


def plane_elements(t):
    side, handle, blade, shaving = [0, 0, 14, 4], [4, 12, 6, 16], [6, 12, 9, 16], [9, 12, 11, 16]
    elements = [
        box([6, 0, 1], [10, 4, 15], {
            "east": side, "west": side, "north": [0, 12, 4, 16], "south": [0, 12, 4, 16],
            "up": {"uv": [0, 4, 14, 8], "texture": "#0", "rotation": 90},
            "down": {"uv": [0, 8, 14, 12], "texture": "#0", "rotation": 90}}),
        box([7, 4, 2], [9, 9, 4.5], all_faces(handle)),          # rear handle
        box([7, 8, 4.5], [9, 10, 6], all_faces(handle)),         # its horn
        box([7, 4, 11.5], [9, 7, 13.5], all_faces(handle)),      # front knob
        box([6.5, 3, 7.6], [9.5, 8.5, 8.4], all_faces(blade),
            {"angle": -22.5, "axis": "x", "origin": [8, 4, 8]}),
    ]
    # A wood shaving curls out of the mouth during the stroke.
    height = lerp_keys([(0, 0), (0.15, 0), (0.6, 5), (0.85, 5), (1, 0)], t)
    if height > 0.3:
        elements.append(box([7, 4, 9.2], [9, 4 + height, 9.7], all_faces(shaving)))
        if height > 2.5:
            elements.append(box([7, 3.5 + height, 9.7], [9, 4 + height, 9.7 + (height - 2.5)], all_faces(shaving)))
            elements.append(box([7, 2 + height, 9.2 + (height - 2.5)], [9, 4 + height, 9.7 + (height - 2.5)],
                                all_faces(shaving)))
    # Pull back, push the plane forwards along its sole, return (the model itself moves in the hand).
    slide = lerp_keys([(0, 0), (0.15, -1.5), (0.55, 4), (1, 0)], t)
    for element in elements:
        element["from"][2] = r(element["from"][2] + slide)
        element["to"][2] = r(element["to"][2] + slide)
        if "rotation" in element:
            element["rotation"] = {**element["rotation"], "origin": [8, 4, r(8 + slide)]}
    return elements


def mallet_display(t):
    # Wind up backwards, slam forwards, recover.
    tilt = lerp_keys([(0, 0), (0.18, -30), (0.5, 65), (0.72, 50), (1, 0)], t)
    return {
        "thirdperson_righthand": {"rotation": [0, -90, r(10 - tilt * 0.6)], "translation": [0, 4, 0.5],
                                  "scale": [0.85] * 3},
        "firstperson_righthand": {"rotation": [0, -90, r(-20 - tilt * 0.6)], "translation": [1.13, 3.0, 1.13],
                                  "scale": [0.48] * 3},
        "gui": {"rotation": [20, -35, -30], "translation": [0, 0, 0], "scale": [0.9] * 3},
        "ground": {"rotation": [0, 0, 90], "translation": [0, 2, 0], "scale": [0.5] * 3},
        "fixed": {"rotation": [0, 180, 0], "scale": [0.9] * 3},
    }


def plane_display(t):
    return {
        "thirdperson_righthand": {"rotation": [-100, 0, 0], "translation": [0, 4, 0.5], "scale": [0.85] * 3},
        "firstperson_righthand": {"rotation": [-18, 115, 0], "translation": [0, 7, 0],
                                  "scale": [0.6] * 3},
        "gui": {"rotation": [25, -40, 0], "translation": [0, 3, 0], "scale": [0.95] * 3},
        "ground": {"translation": [0, 2, 0], "scale": [0.5] * 3},
        "fixed": {"rotation": [0, 90, 0], "translation": [0, 3, 0], "scale": [0.9] * 3},
    }


def write_models(name, elements, display):
    def model(t):
        return {"textures": {"0": f"kalabushkin:item/{name}", "particle": f"kalabushkin:item/{name}"},
                "elements": elements(t), "display": display(t)}

    base = model(0.0)
    base["overrides"] = []
    for i in range(FRAMES):
        frame = f"{name}_swing_{i}"
        # Frame i shows from swing progress i / FRAMES on; the predicate is never exactly 0 mid-swing.
        base["overrides"].append({"predicate": {"kalabushkin:swing": max(i / FRAMES, 0.001)},
                                  "model": f"kalabushkin:item/{frame}"})
        with open(f"{ASSETS}/models/item/{frame}.json", "w") as f:
            json.dump(model((i + 0.5) / FRAMES), f, separators=(",", ":"))
    with open(f"{ASSETS}/models/item/{name}.json", "w") as f:
        json.dump(base, f, separators=(",", ":"))


SPIN_FRAMES = 8


def shatuni_texture():
    """Crankset seen side-on, 32x32: toothed chainring on a four-arm spider and two opposite crank arms.
    It looks the same after half a turn, so the spin animation only has to cover 180 degrees."""
    img = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
    rng = random.Random(23)
    c = 15.5
    arm = (math.cos(math.radians(45)), math.sin(math.radians(45)))
    for y in range(32):
        for x in range(32):
            dx, dy = x - c, y - c
            dist = math.hypot(dx, dy)
            angle = math.atan2(dy, dx)
            along = dx * arm[0] + dy * arm[1]
            across = abs(-dx * arm[1] + dy * arm[0])
            tooth = math.cos(16 * angle) > 0.15
            color = None
            if 8.0 <= dist <= (12.2 if tooth else 10.2):
                color = (74, 78, 86) if dist > 10.2 else (46, 48, 54)
            elif 2.5 < dist < 8.0 and min(abs(dx), abs(dy)) < 0.9:
                color = (58, 60, 66)  # spider
            if across < 1.6 and abs(along) <= 19.5:
                color = (30, 30, 34) if across > 0.6 else (64, 66, 74)
                if abs(along) > 17.5 and across < 0.6:
                    color = (150, 152, 158)  # pedal thread
            if dist <= 3.0:
                color = (96, 98, 106) if dist > 1.3 else (28, 28, 32)
            if color:
                light = 10 if dx + dy < 0 else -6
                j = rng.randint(-4, 4) + light
                img.putpixel((x, y), tuple(max(0, min(255, v + j)) for v in color) + (255,))
    return img


def write_shatuni_models():
    """Flat sprite model; the "kalabushkin:spin" predicate picks a frame turned about the sprite's centre."""
    def model(angle):
        return {"parent": "minecraft:item/handheld", "textures": {"layer0": "kalabushkin:item/shatuni"},
                "display": {
                    "thirdperson_righthand": {"rotation": [0, -90, r(55 + angle)], "translation": [0, 5, 2.5], "scale": [1.2, 1.2, 1.2]},
                    "thirdperson_lefthand": {"rotation": [0, 90, r(-55 - angle)], "translation": [0, 5, 2.5], "scale": [1.2, 1.2, 1.2]},
                    "firstperson_righthand": {"rotation": [0, -90, r(25 + angle)], "translation": [1.13, 3.2, 1.13], "scale": [0.85, 0.85, 0.85]},
                    "firstperson_lefthand": {"rotation": [0, 90, r(-25 - angle)], "translation": [1.13, 3.2, 1.13], "scale": [0.85, 0.85, 0.85]},
                }}

    base = model(0.0)
    base["overrides"] = []
    for i in range(1, SPIN_FRAMES):
        frame = f"shatuni_spin_{i}"
        base["overrides"].append({"predicate": {"kalabushkin:spin": r(i / SPIN_FRAMES)}, "model": f"kalabushkin:item/{frame}"})
        with open(f"{ASSETS}/models/item/{frame}.json", "w") as f:
            json.dump(model(180.0 * i / SPIN_FRAMES), f, separators=(",", ":"))
    with open(f"{ASSETS}/models/item/shatuni.json", "w") as f:
        json.dump(base, f, separators=(",", ":"))


# ---------------------------------------------------------------- Vasily's wares

def swatches(colors, seed, jitter=4):
    """16x16 texture of four 4 px wide columns, one colour each, for model boxes to sample from."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    rng = random.Random(seed)
    for i, color in enumerate(colors):
        for y in range(16):
            for x in range(4):
                j = rng.randint(-jitter, jitter)
                img.putpixel((4 * i + x, y), tuple(max(0, min(255, c + j)) for c in color) + (255,))
    return img


def swatch(i, w=1.0, h=1.0):
    return [4 * i, 0, r(4 * i + min(w, 4)), r(min(h, 16))]


def rod(cx, cz, y0, y1, thickness, i):
    """Upright square rod whose sides and caps all sample swatch i."""
    half = thickness / 2
    side = swatch(i, thickness, y1 - y0)
    cap = swatch(i, thickness, thickness)
    return box([cx - half, y0, cz - half], [cx + half, y1, cz + half],
               {"north": side, "south": side, "east": side, "west": side, "up": cap, "down": cap})


def held_display(gui_rotation, gui_scale):
    return {
        "thirdperson_righthand": {"rotation": [0, -90, 10], "translation": [0, 4, 0.5], "scale": [0.85] * 3},
        "firstperson_righthand": {"rotation": [0, -90, -20], "translation": [1.13, 3.0, 1.13], "scale": [0.6] * 3},
        "gui": {"rotation": gui_rotation, "translation": [0, 0, 0], "scale": [gui_scale] * 3},
        "ground": {"rotation": [0, 0, 90], "translation": [0, 2, 0], "scale": [0.5] * 3},
        "fixed": {"rotation": [0, 180, 0], "scale": [0.9] * 3},
    }


def write_static_model(name, texture, elements, display):
    texture.save(f"{ASSETS}/textures/item/{name}.png")
    model = {"textures": {"0": f"kalabushkin:item/{name}", "particle": f"kalabushkin:item/{name}"},
             "elements": elements, "display": display}
    with open(f"{ASSETS}/models/item/{name}.json", "w") as f:
        json.dump(model, f, separators=(",", ":"))


def cigarette_model():
    """Filter at the bottom, white paper, a ring of ash and the ember on top."""
    texture = swatches([(240, 238, 232), (214, 150, 70), (120, 116, 114), (238, 84, 28)], 3)
    for x, y in ((4, 1), (6, 2), (5, 0), (7, 1)):
        texture.putpixel((x, y), (176, 116, 50, 255))  # cork speckles on the filter
    elements = [rod(8, 8, 3, 6, 1.2, 1), rod(8, 8, 6, 12, 1.2, 0), rod(8, 8, 12, 12.6, 1.2, 2),
                rod(8, 8, 12.6, 13.2, 1.0, 3)]
    write_static_model("cigarette", texture, elements, held_display([0, -20, -45], 1.5))


def dyadkovo_cigarette_model():
    """Hand-rolled and lumpy: a twisted end, three uneven sections out of line, a fat ember."""
    texture = swatches([(206, 186, 140), (168, 146, 104), (84, 80, 78), (244, 64, 20)], 5, jitter=10)
    elements = [rod(8, 8, 3, 4, 0.8, 1), rod(8, 8, 4, 7.5, 1.4, 0), rod(8.35, 8, 7.5, 10.5, 1.7, 1),
                rod(7.9, 8.2, 10.5, 12.5, 1.4, 0), rod(7.9, 8.2, 12.5, 13.3, 1.4, 2),
                rod(7.9, 8.2, 13.3, 14, 1.1, 3)]
    write_static_model("dyadkovo_cigarette", texture, elements, held_display([0, -20, -45], 1.5))


def vape_model():
    """Box mod: purple body with a fire button and LED, a glass tank and a black drip tip."""
    texture = swatches([(132, 84, 214), (70, 74, 86), (24, 24, 30), (200, 204, 212)], 7)
    for y in range(16):
        texture.putpixel((1, y), (170, 128, 236, 255))  # highlight down the body
        texture.putpixel((5, y), (120, 150, 190, 255) if y < 2 else (96, 102, 118, 255))  # liquid in the tank
    for y in range(8, 16):
        for x in range(12, 16):
            texture.putpixel((x, y), (120, 255, 180, 255))  # LED green
    body_wide, body_narrow = [0, 0, 3, 7], [0, 0, 2, 7]
    tank = [4, 0, 6, 2.5]
    elements = [
        box([6.5, 2, 7], [9.5, 9, 9], {"north": body_wide, "south": body_wide, "east": body_narrow,
                                       "west": body_narrow, "up": [0, 0, 3, 2], "down": [0, 0, 3, 2]}),
        box([7, 9, 7], [9, 11.5, 9], {"north": tank, "south": tank, "east": tank, "west": tank,
                                      "up": [6, 0, 8, 2], "down": [6, 0, 8, 2]}),
        rod(8, 8, 11.5, 13.5, 1, 2),
        box([7.5, 6.5, 6.7], [8.5, 7.5, 7], all_faces([12, 0, 13, 1])),
        box([7.5, 3, 6.8], [8.5, 3.5, 7], all_faces([12, 8, 13, 9])),
    ]
    write_static_model("vape", texture, elements, held_display([20, -35, 0], 1.4))


def snus_model():
    """Round can lying flat, built as an octagon from three overlapping boxes."""
    texture = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    rim, white, blue = (30, 70, 150), (236, 238, 244), (44, 104, 200)
    for y in range(8):
        for x in range(8):
            if math.hypot(x - 3.5, y - 3.5) > 3.3:
                color = rim
            elif 3 <= y <= 4:
                color = blue if (x + y) % 3 else white  # label band
            else:
                color = white if x + y < 8 else (214, 218, 228)
            texture.putpixel((x, y), color + (255,))
            texture.putpixel((x, y + 8), rim + (255,))  # underside
    for x in range(8, 16):
        for y, color in enumerate((white, blue, rim)):
            texture.putpixel((x, y), color + (255,))  # side band
    elements = []
    for x0, x1, z0, z1 in ((4, 12, 5.5, 10.5), (5.5, 10.5, 4, 12), (4.7, 11.3, 4.7, 11.3)):
        along_x, along_z = [8, 0, r(8 + x1 - x0), 3], [8, 0, r(8 + z1 - z0), 3]
        # The lid texture covers the can's 8x8 footprint, so each box samples its own part of it.
        lid = [r(x0 - 4), r(z0 - 4), r(x1 - 4), r(z1 - 4)]
        elements.append(box([x0, 6.75, z0], [x1, 9.25, z1], {
            "north": along_x, "south": along_x, "east": along_z, "west": along_z, "up": lid,
            "down": [lid[0], r(lid[1] + 8), lid[2], r(lid[3] + 8)]}))
    write_static_model("snus", texture, elements, held_display([30, 45, 0], 1.5))


def diamond_phone_model():
    """A slim slab: black screen on one side, a faceted diamond case with a camera on the other."""
    texture = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    diamond, facet, edge = (92, 220, 214), (176, 246, 240), (44, 164, 172)
    icons = {(1, 2): (236, 84, 76), (3, 2): (90, 200, 110), (1, 4): (80, 150, 240), (3, 4): (244, 196, 70),
             (1, 6): (190, 110, 230), (3, 6): (236, 236, 240)}
    for y in range(10):
        for x in range(5):
            rim = x in (0, 4) or y in (0, 9)
            texture.putpixel((x, y), (edge if rim else facet if (x + y) % 4 == 0 or (x - y) % 4 == 0 else diamond) + (255,))
            texture.putpixel((5 + x, y), ((20, 20, 26) if rim else icons.get((x, y), (36, 42, 60))) + (255,))
    for x, y in ((2, 4), (1, 5), (2, 5), (3, 5), (2, 6)):
        texture.putpixel((x, y), (236, 255, 252, 255))  # gem motif on the case
    for y in range(16):
        for x in range(11, 16):
            texture.putpixel((x, y), ((24, 24, 30) if y >= 11 else edge) + (255,))
    side = [12, 0, 13, 10]
    elements = [
        box([5.5, 3, 7.5], [10.5, 13, 8.5], {"south": [0, 0, 5, 10], "north": [5, 0, 10, 10], "east": side,
                                             "west": side, "up": [11, 0, 16, 1], "down": [11, 0, 16, 1]}),
        box([6.2, 10.8, 8.5], [7.8, 12.4, 8.8], all_faces([12, 12, 13, 13])),  # camera bump
    ]
    write_static_model("diamond_phone", texture, elements, held_display([15, -25, 0], 1.3))


def main():
    os.makedirs(f"{ASSETS}/textures/item", exist_ok=True)
    os.makedirs(f"{ASSETS}/models/item", exist_ok=True)
    mallet_texture().save(f"{ASSETS}/textures/item/wooden_mallet.png")
    plane_texture().save(f"{ASSETS}/textures/item/carpenter_plane.png")
    write_models("wooden_mallet", mallet_elements, mallet_display)
    write_models("carpenter_plane", plane_elements, plane_display)
    shatuni_texture().save(f"{ASSETS}/textures/item/shatuni.png")
    write_shatuni_models()
    cigarette_model()
    dyadkovo_cigarette_model()
    vape_model()
    snus_model()
    diamond_phone_model()


if __name__ == "__main__":
    main()
