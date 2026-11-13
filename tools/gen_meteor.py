"""Generates the meteor texture and its two item models (rock + fire trail)."""
import json, math, random, struct, sys, zlib

ROOT = sys.argv[1]
S = 64
rnd = random.Random(7)
px = [[(0, 0, 0, 0)] * S for _ in range(S)]


def clamp(v):
    return max(0, min(255, int(v)))


# Rock: 32x32 dark basalt with glowing cracks, tileable enough for arbitrary face crops.
h = [[rnd.random() for _ in range(32)] for _ in range(32)]
for _ in range(2):
    h = [[(h[y][x] * 2 + h[(y + 1) % 32][x] + h[y][(x + 1) % 32] + h[(y - 1) % 32][x] + h[y][(x - 1) % 32]) / 6 for x in range(32)] for y in range(32)]
lo = min(map(min, h)); hi = max(map(max, h))
heat = [[0.0] * 32 for _ in range(32)]
for _ in range(9):
    x, y = rnd.randrange(32), rnd.randrange(32)
    dx, dy = rnd.choice([(1, 0), (0, 1), (1, 1), (1, -1)])
    for _ in range(rnd.randint(8, 18)):
        heat[y % 32][x % 32] = 1.0
        for ox, oy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            c = heat[(y + oy) % 32][(x + ox) % 32]
            heat[(y + oy) % 32][(x + ox) % 32] = max(c, 0.35)
        if rnd.random() < 0.35:
            dx, dy = rnd.choice([(1, 0), (0, 1), (1, 1), (1, -1), (-1, 1)])
        x += dx; y += dy
for y in range(32):
    for x in range(32):
        v = (h[y][x] - lo) / (hi - lo)
        g = 0.55 + 0.9 * v + (rnd.random() - 0.5) * 0.18
        r, gr, b = 52 * g, 42 * g, 40 * g
        k = heat[y][x]
        if k >= 1.0:
            r, gr, b = 255, 214 - rnd.randint(0, 40), 90
        elif k > 0:
            r, gr, b = r + 150, gr + 48, b + 4
        px[y][x] = (clamp(r), clamp(gr), clamp(b), 255)

# Fire: 32 wide, 64 tall. Bottom row is the hot end next to the rock, top fades out.
tongue = [0.55 + 0.45 * rnd.random() for _ in range(32)]
tongue = [(tongue[i - 1] + tongue[i] * 2 + tongue[(i + 1) % 32]) / 4 for i in range(32)]
for y in range(S):
    k = (S - 1 - y) / (S - 1)  # 0 at the rock, 1 at the tail
    for x in range(32):
        reach = tongue[x]
        f = k / reach
        if f >= 1.0:
            continue
        if f < 0.25:
            r, g, b = 255, 226 - 200 * f, 110 - 280 * f
        elif f < 0.6:
            t = (f - 0.25) / 0.35
            r, g, b = 255, 176 - 96 * t, 40 - 25 * t
        else:
            t = (f - 0.6) / 0.4
            r, g, b = 255 - 70 * t, 80 - 55 * t, 15
        a = 235 * (1 - f) ** 0.7 + (rnd.random() - 0.5) * 24
        px[y][32 + x] = (clamp(r), clamp(g), clamp(b), clamp(a))


def png(path):
    raw = b"".join(b"\x00" + bytes(c for p in row for c in p) for row in px)

    def chunk(tag, data):
        return struct.pack(">I", len(data)) + tag + data + struct.pack(">I", zlib.crc32(tag + data) & 0xFFFFFFFF)

    with open(path, "wb") as f:
        f.write(b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", struct.pack(">IIBBBBB", S, S, 8, 6, 0, 0, 0)) + chunk(b"IDAT", zlib.compress(raw, 9)) + chunk(b"IEND", b""))


def face_dims(frm, to, face):
    dx, dy, dz = (to[i] - frm[i] for i in range(3))
    return {"north": (dx, dy), "south": (dx, dy), "west": (dz, dy), "east": (dz, dy), "up": (dx, dz), "down": (dx, dz)}[face]


def rock_box(frm, to):
    faces = {}
    for face in ("north", "south", "west", "east", "up", "down"):
        w, hgt = face_dims(frm, to, face)
        uw, uh = w / 2, hgt / 2  # 2 texels per model pixel
        u = round(rnd.uniform(0, 8 - uw) * 4) / 4
        v = round(rnd.uniform(0, 8 - uh) * 4) / 4
        faces[face] = {"uv": [u, v, u + uw, v + uh], "texture": "#t"}
    return {"from": frm, "to": to, "faces": faces}


FIRE_Y0, FIRE_Y1 = 7.0, 32.0


def fire_box(frm, to):
    faces = {}
    for face in ("north", "south", "west", "east"):
        w, _ = face_dims(frm, to, face)
        pad = (16 - w) / 4
        v_top = 16 - (to[1] - FIRE_Y0) / (FIRE_Y1 - FIRE_Y0) * 16
        v_bot = 16 - (frm[1] - FIRE_Y0) / (FIRE_Y1 - FIRE_Y0) * 16
        faces[face] = {"uv": [8 + pad, round(v_top, 4), 16 - pad, round(v_bot, 4)], "texture": "#t"}
    return {"from": frm, "to": to, "shade": False, "faces": faces}


def model(elements):
    return {
        "textures": {"t": "godbox:item/m/meteor", "particle": "godbox:item/m/meteor"},
        "elements": elements,
        "display": {
            "ground": {"translation": [0.0, 0.0, 0.0], "scale": [1.0, 1.0, 1.0]},
            "gui": {"rotation": [20.0, 30.0, 0.0], "scale": [1.2, 1.2, 1.2]},
        },
    }


rock = model([
    rock_box([3, 3, 3], [13, 13, 13]),
    rock_box([2, 5, 5], [14, 11, 11]),
    rock_box([5, 2, 5], [11, 14, 11]),
    rock_box([5, 5, 2], [11, 11, 14]),
    rock_box([10, 9, 9], [14.5, 13.5, 13.5]),
    rock_box([1.5, 3.5, 6], [6, 8, 10.5]),
    rock_box([6, 10, 1.5], [10.5, 14.5, 6]),
    rock_box([8.5, 1.5, 9], [12.5, 5.5, 13]),
])
# The trail runs along +Y so the 180 degree Y flip item displays apply cannot reverse it.
fire = model([
    fire_box([4.5, 9, 4.5], [11.5, 20, 11.5]),
    fire_box([1, 7, 1], [15, 15, 15]),
    fire_box([3, 15, 3], [13, 24, 13]),
    fire_box([5, 24, 5], [11, 32, 11]),
])

png(f"{ROOT}/textures/item/m/meteor.png")
import os
os.makedirs(f"{ROOT}/models/item/m/meteor", exist_ok=True)
for name, m in (("rock", rock), ("fire", fire)):
    with open(f"{ROOT}/models/item/m/meteor/{name}.json", "w") as f:
        json.dump(m, f, separators=(",", ":"))

base = f"{ROOT}/models/item/model.json"
with open(base) as f:
    raw = f.read()
d = json.loads(raw)
have = {o["predicate"]["custom_model_data"] for o in d["overrides"]}
for cmd, name in ((4300, "rock"), (4301, "fire")):
    if cmd not in have:
        d["overrides"].append({"predicate": {"custom_model_data": cmd}, "model": f"godbox:item/m/meteor/{name}"})
d["overrides"].sort(key=lambda o: o["predicate"]["custom_model_data"])
with open(base, "w") as f:
    json.dump(d, f, indent=1)
    if raw.endswith("\n"):
        f.write("\n")
print("ok", len(d["overrides"]))
