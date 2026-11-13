#!/usr/bin/env python3
"""Generates textures and 3D item models (including swing animation frames) for the carpenter's
weapons. Run from the repository root: python3 tools/generate_weapons.py

The swing animation is a strip of models picked by the "kalabushkin:swing" item predicate."""
import json
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


def main():
    os.makedirs(f"{ASSETS}/textures/item", exist_ok=True)
    os.makedirs(f"{ASSETS}/models/item", exist_ok=True)
    mallet_texture().save(f"{ASSETS}/textures/item/wooden_mallet.png")
    plane_texture().save(f"{ASSETS}/textures/item/carpenter_plane.png")
    write_models("wooden_mallet", mallet_elements, mallet_display)
    write_models("carpenter_plane", plane_elements, plane_display)


if __name__ == "__main__":
    main()
