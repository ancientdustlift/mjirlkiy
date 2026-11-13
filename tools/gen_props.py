#!/usr/bin/env python3
"""Generates the box models and textures for bombs, the black hole, the tornado, aliens, ships and staffs.

Usage: python3 tools/gen_props.py [src/main/resources/assets/godbox]

Each group gets one texture atlas (textures/item/m/<group>.png) and one model per part
(models/item/m/<group>/<part>.json). Every box face is painted into its own atlas rectangle by a
material function that receives the texel's position in model space.
"""
import json, math, os, random, struct, sys, zlib

ROOT = sys.argv[1] if len(sys.argv) > 1 else "src/main/resources/assets/godbox"
FACES = ("north", "south", "west", "east", "up", "down")
SIDES = ("north", "south", "west", "east")
OVERRIDES = []


def clamp(v):
    return max(0, min(255, int(round(v))))


class Group:
    def __init__(self, name, size):
        self.name, self.size = name, size
        self.px = [[(0, 0, 0, 0)] * size for _ in range(size)]
        self.x, self.y, self.row = 1, 1, 0
        self.rng = random.Random(zlib.crc32(name.encode()))
        self.cache = {}
        self.parts = {}

    def alloc(self, w, h):
        if self.x + w + 1 > self.size:
            self.x, self.y, self.row = 1, self.y + self.row + 2, 0
        if self.y + h + 1 > self.size:
            raise SystemExit(f"{self.name}: atlas of {self.size}px is full")
        x, y = self.x, self.y
        self.x += w + 2
        self.row = max(self.row, h)
        return x, y

    def paint(self, frm, to, face, mat, density):
        (x0, y0, z0), (x1, y1, z1) = frm, to
        dx, dy, dz = x1 - x0, y1 - y0, z1 - z0
        w, h = {"north": (dx, dy), "south": (dx, dy), "west": (dz, dy), "east": (dz, dy), "up": (dx, dz), "down": (dx, dz)}[face]
        tw, th = max(1, round(w * density)), max(1, round(h * density))
        ax, ay = self.alloc(tw, th)
        for j in range(th):
            for i in range(tw):
                fu, fv = (i + 0.5) / tw, (j + 0.5) / th
                p = {
                    "north": (x1 - fu * dx, y1 - fv * dy, z0),
                    "south": (x0 + fu * dx, y1 - fv * dy, z1),
                    "west": (x0, y1 - fv * dy, z0 + fu * dz),
                    "east": (x1, y1 - fv * dy, z1 - fu * dz),
                    "up": (x0 + fu * dx, y1, z0 + fv * dz),
                    "down": (x0 + fu * dx, y0, z1 - fv * dz),
                }[face]
                r, g, b, a = mat(p, face, self.rng)
                self.px[ay + j][ax + i] = (clamp(r), clamp(g), clamp(b), clamp(a))
        # Extrude the border by one texel so mipmapping does not bleed in transparent neighbours.
        for j in range(th):
            self.px[ay + j][ax - 1] = self.px[ay + j][ax]
            self.px[ay + j][ax + tw] = self.px[ay + j][ax + tw - 1]
        for i in range(-1, tw + 1):
            self.px[ay - 1][ax + i] = self.px[ay][ax + i]
            self.px[ay + th][ax + i] = self.px[ay + th - 1][ax + i]
        k = 16.0 / self.size
        return [round(ax * k, 4), round(ay * k, 4), round((ax + tw) * k, 4), round((ay + th) * k, 4)]

    def box(self, frm, to, mat, faces=FACES, shade=True, rot=None, density=1, reuse=False):
        el = {"from": [float(v) for v in frm], "to": [float(v) for v in to], "faces": {}}
        if rot:
            el["rotation"] = {"angle": rot[1], "axis": rot[0], "origin": [8, 8, 8]}
        if not shade:
            el["shade"] = False
        for face in faces:
            key = (id(mat), face in ("up", "down"), tuple(round(to[i] - frm[i], 3) for i in range(3)), face) if reuse else None
            if key and key in self.cache:
                uv = self.cache[key]
            else:
                uv = self.paint(frm, to, face, mat, density)
                if key:
                    self.cache[key] = uv
            el["faces"][face] = {"uv": uv, "texture": "#t"}
        return el

    def part(self, name, cmd, elements, display=None):
        self.parts[name] = {
            "textures": {"t": f"godbox:item/m/{self.name}", "particle": f"godbox:item/m/{self.name}"},
            "elements": elements,
            "display": display
            or {
                "ground": {"translation": [0.0, 0.0, 0.0], "scale": [1.0, 1.0, 1.0]},
                "gui": {"rotation": [20.0, 30.0, 0.0], "scale": [0.7, 0.7, 0.7]},
            },
        }
        OVERRIDES.append((cmd, f"godbox:item/m/{self.name}/{name}"))

    def write(self):
        os.makedirs(f"{ROOT}/models/item/m/{self.name}", exist_ok=True)
        for name, model in self.parts.items():
            with open(f"{ROOT}/models/item/m/{self.name}/{name}.json", "w") as f:
                json.dump(model, f, separators=(",", ":"))
        raw = b"".join(b"\x00" + bytes(c for p in row for c in p) for row in self.px)

        def chunk(tag, data):
            return struct.pack(">I", len(data)) + tag + data + struct.pack(">I", zlib.crc32(tag + data) & 0xFFFFFFFF)

        with open(f"{ROOT}/textures/item/m/{self.name}.png", "wb") as f:
            f.write(b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", struct.pack(">IIBBBBB", self.size, self.size, 8, 6, 0, 0, 0)))
            f.write(chunk(b"IDAT", zlib.compress(raw, 9)) + chunk(b"IEND", b""))


# ---------------------------------------------------------------- materials


def plane(p, face):
    """The two coordinates that vary across a face."""
    if face in ("north", "south"):
        return p[0], p[1]
    if face in ("west", "east"):
        return p[2], p[1]
    return p[0], p[2]


def solid(rgb, noise=0.14, alpha=255):
    def mat(p, face, rng):
        k = 1 + (rng.random() - 0.5) * noise
        return rgb[0] * k, rgb[1] * k, rgb[2] * k, alpha

    return mat


def banded(base, bands, noise=0.12):
    """bands: list of (y_from, y_to, rgb) painted over the base colour."""

    def mat(p, face, rng):
        c = base
        for lo, hi, rgb in bands:
            if lo <= p[1] < hi:
                c = rgb
        k = 1 + (rng.random() - 0.5) * noise
        return c[0] * k, c[1] * k, c[2] * k, 255

    return mat


def wood(rgb, axis=1):
    def mat(p, face, rng):
        grain = 0.86 + 0.14 * math.sin(p[(axis + 1) % 3] * 2.3 + math.sin(p[axis] * 0.6) * 1.5)
        k = grain * (1 + (rng.random() - 0.5) * 0.1)
        return rgb[0] * k, rgb[1] * k, rgb[2] * k, 255

    return mat


def planks(rgb):
    def mat(p, face, rng):
        row = int(math.floor(p[1] / 1.6))
        seam = 0.72 if abs((p[1] / 1.6) % 1.0) < 0.18 else 1.0
        joint = 0.8 if int(p[2] + row * 3.7) % 9 == 0 else 1.0
        k = seam * joint * (0.95 + 0.1 * ((row * 37) % 5) / 5) * (1 + (rng.random() - 0.5) * 0.08)
        return rgb[0] * k, rgb[1] * k, rgb[2] * k, 255

    return mat


# ---------------------------------------------------------------- bombs


def finned(g, name, cmd, body, nose, fins, r=3.0, y0=2.0, y1=14.0):
    """A nose-down aerial bomb centred on the model origin."""
    lo, hi = 8 - r, 8 + r
    g.part(
        name,
        cmd,
        [
            g.box([lo, y0 + 2, lo], [hi, y1, hi], body),
            g.box([lo + 0.8, y0 + 0.6, lo + 0.8], [hi - 0.8, y0 + 2, hi - 0.8], nose),
            g.box([lo + 1.8, y0 - 0.8, lo + 1.8], [hi - 1.8, y0 + 0.6, hi - 1.8], nose),
            g.box([lo + 0.9, y1, lo + 0.9], [hi - 0.9, y1 + 1.4, hi - 0.9], body),
            g.box([7, y1 + 1.4, 7], [9, y1 + 4.5, 9], fins),
            g.box([lo - 1, y1 + 1.5, 7.6], [hi + 1, y1 + 6.5, 8.4], fins),
            g.box([7.6, y1 + 1.5, lo - 1], [8.4, y1 + 6.5, hi + 1], fins),
        ],
    )


def bombs():
    g = Group("bombs", 256)
    iron, dark, steel = (58, 60, 66), (34, 35, 40), (120, 124, 130)
    finned(g, "bomb", 4400, banded(iron, [(11.5, 12.5, (200, 170, 40))]), solid(dark), solid(steel))
    finned(g, "napalm", 4402, banded((176, 44, 30), [(6, 7.2, (250, 190, 40)), (10.5, 11.7, (250, 190, 40))]), solid((240, 130, 30)), solid((70, 30, 26)))
    finned(g, "water", 4403, banded((40, 110, 200), [(7, 9, (190, 225, 250))]), solid((150, 210, 245)), solid((225, 235, 245)))
    finned(g, "cluster", 4404, banded((92, 104, 58), [(5, 5.8, (230, 200, 40)), (8, 8.8, (230, 200, 40)), (11, 11.8, (230, 200, 40))]), solid((60, 70, 40)), solid((60, 70, 40)), r=3.5)
    finned(g, "santa", 4405, banded((196, 30, 36), [(7.5, 9.5, (245, 245, 245)), (12.6, 14, (40, 140, 60))]), solid((245, 245, 245)), solid((40, 140, 60)))
    finned(g, "atomic", 4406, banded((150, 154, 150), [(4, 5, (20, 20, 20)), (12.4, 13.4, (228, 196, 30))]), solid((228, 196, 30)), solid((70, 74, 72)), r=4.6, y0=3.0, y1=13.0)
    finned(g, "czar", 4407, banded((226, 228, 222), [(3.5, 4.5, (190, 30, 30)), (14.5, 15.5, (190, 30, 30))]), solid((60, 64, 62)), solid((60, 64, 62)), r=4.2, y0=0.0, y1=16.0)

    def pineapple(p, face, rng):
        a, b = plane(p, face)
        groove = (a % 2.0) < 0.4 or (b % 2.0) < 0.4
        c = (38, 58, 30) if groove else (70, 104, 52)
        k = 1 + (rng.random() - 0.5) * 0.12
        return c[0] * k, c[1] * k, c[2] * k, 255

    g.part(
        "grenade",
        4401,
        [
            g.box([5, 4, 5], [11, 11, 11], pineapple),
            g.box([4.2, 5, 6], [11.8, 10, 10], pineapple),
            g.box([6, 5, 4.2], [10, 10, 11.8], pineapple),
            g.box([6, 3.2, 6], [10, 11.8, 10], pineapple),
            g.box([7, 11.8, 7], [9, 13.4, 9], solid(steel)),
            g.box([6.4, 13.4, 7.4], [11.4, 14, 8.6], solid((170, 172, 176))),
            g.box([10.8, 9.5, 7.4], [11.4, 13.4, 8.6], solid((170, 172, 176))),
        ],
    )

    def glow_core(p, face, rng):
        d = math.dist(p, (8, 8, 8)) / 5.2
        return 255, 200 + 55 * (1 - d), 255, 255

    def shell(p, face, rng):
        swirl = 0.5 + 0.5 * math.sin(p[0] * 0.9 + p[1] * 1.3 + p[2] * 0.7)
        return 110 + 60 * swirl, 30 + 40 * swirl, 200 + 40 * swirl, 105 + 60 * swirl

    g.part(
        "antimatter",
        4408,
        [
            g.box([5.5, 5.5, 5.5], [10.5, 10.5, 10.5], glow_core, shade=False),
            g.box([3, 3, 3], [13, 13, 13], shell, shade=False),
            g.box([2, 5, 5], [14, 11, 11], shell, shade=False),
            g.box([5, 2, 5], [11, 14, 11], shell, shade=False),
            g.box([5, 5, 2], [11, 11, 14], shell, shade=False),
            g.box([0.5, 7.4, 0.5], [15.5, 8.6, 15.5], solid((40, 20, 70), 0.2), faces=SIDES),
        ],
    )

    def stick(p, face, rng):
        if face in ("up", "down"):
            return 210, 190, 150, 255
        k = 1 + (rng.random() - 0.5) * 0.1
        return 190 * k, 36 * k, 30 * k, 255

    def clock(p, face, rng):
        if face != "north":
            return 40, 40, 44, 255
        dx, dy = p[0] - 8, p[1] - 6
        if dx * dx + dy * dy > 4.6:
            return 40, 40, 44, 255
        if (abs(dx) < 0.3 and 0 <= dy < 1.7) or (abs(dy) < 0.3 and 0 <= dx < 1.3):
            return 30, 30, 30, 255
        return 236, 232, 214, 255

    sticks = [g.box([x, 0, z], [x + 3, 12, z + 3], stick) for x, z in ((3.4, 6.5), (6.5, 6.5), (9.6, 6.5), (5, 9.3), (8, 9.3))]
    g.part(
        "timed",
        4409,
        sticks
        + [
            g.box([3, 3, 6.1], [13, 4.4, 12.7], solid((30, 30, 32))),
            g.box([3, 8, 6.1], [13, 9.4, 12.7], solid((30, 30, 32))),
            g.box([5.5, 3.5, 5], [10.5, 8.5, 6.5], clock, density=3),
            g.box([7.7, 12, 7.7], [8.3, 14.5, 8.3], solid((60, 50, 40))),
            g.box([7.4, 14.5, 7.4], [8.6, 15.4, 8.6], solid((255, 200, 60), 0.3), shade=False),
        ],
    )
    g.write()


# ---------------------------------------------------------------- black hole


def blackhole():
    g = Group("blackhole", 256)

    def void(p, face, rng):
        v = rng.random()
        return 6 + 14 * v, 2 + 4 * v, 14 + 24 * v, 255

    g.part(
        "core",
        4500,
        [
            g.box([3, 3, 3], [13, 13, 13], void, shade=False),
            g.box([1, 4.5, 4.5], [15, 11.5, 11.5], void, shade=False),
            g.box([4.5, 1, 4.5], [11.5, 15, 11.5], void, shade=False),
            g.box([4.5, 4.5, 1], [11.5, 11.5, 15], void, shade=False),
            g.box([2, 2.8, 5.5], [14, 13.2, 10.5], void, shade=False),
            g.box([5.5, 2.8, 2], [10.5, 13.2, 14], void, shade=False),
            g.box([2, 5.5, 2.8], [14, 10.5, 13.2], void, shade=False),
        ],
    )

    def disc(p, face, rng):
        dx, dz = p[0] - 8, p[2] - 8
        r = math.hypot(dx, dz)
        if r < 9.5 or r > 23:
            return 0, 0, 0, 0
        f = (r - 9.5) / 13.5
        ang = math.atan2(dz, dx)
        streak = 0.6 + 0.4 * math.sin(ang * 5 + r * 0.9) * math.sin(ang * 2 - r * 0.35)
        heat = (1 - f) ** 1.4
        return 255, 120 + 135 * heat, 30 + 200 * heat * heat, (235 * (1 - f) ** 0.8 + 20) * streak

    g.part("ring", 4501, [g.box([-15, 7.6, -15], [31, 8.4, 31], disc, faces=("up", "down"), shade=False, density=2)])
    g.write()


# ---------------------------------------------------------------- tornado


def tornado():
    g = Group("tornado", 256)

    def wind(p, face, rng):
        t = (p[1] + 16) / 48.0
        streak = math.sin((p[0] + p[2]) * 0.55 + p[1] * 0.9) * 0.5 + 0.5
        band = 0.5 + 0.5 * math.sin(p[1] * 0.42)
        v = 150 + 80 * streak
        a = (70 + 110 * streak) * (0.75 + 0.25 * band) * (1.0 - 0.35 * t)
        if rng.random() < 0.05:
            a *= 0.3
        return v, v + 4, v + 10, a

    def dust(p, face, rng):
        if rng.random() < 0.45:
            return 0, 0, 0, 0
        k = 0.7 + rng.random() * 0.5
        return 128 * k, 104 * k, 76 * k, 150

    layers = []
    for k in range(8):
        y = -16 + 6 * k
        r = 1.3 + 1.35 * k
        for rot in (None, ("y", 45)):
            layers.append(g.box([8 - r, y, 8 - r], [8 + r, y + 6, 8 + r], wind, faces=SIDES, shade=False, rot=rot))
    g.part("funnel", 4600, layers)
    skirt = []
    for r, y0, y1 in ((5.5, -16, -11), (8.5, -16, -13)):
        for rot in (None, ("y", 45)):
            skirt.append(g.box([8 - r, y0, 8 - r], [8 + r, y1, 8 + r], dust, faces=SIDES, shade=False, rot=rot))
    g.part("skirt", 4601, skirt)
    g.write()


# ---------------------------------------------------------------- alien


def alien():
    g = Group("alien", 128)
    skin = solid((132, 150, 128), 0.1)

    def face_mat(p, face, rng):
        base = skin(p, face, rng)
        if face != "north":
            return base
        for cx, tilt in ((5.1, 0.45), (10.9, -0.45)):
            dx, dy = p[0] - cx, p[1] - 7.6
            u = dx * math.cos(tilt) - dy * math.sin(tilt)
            v = dx * math.sin(tilt) + dy * math.cos(tilt)
            if (u / 2.3) ** 2 + (v / 1.45) ** 2 <= 1.0:
                glint = 60 if (u < -0.8 and v > 0.3) else 0
                return 8 + glint, 10 + glint, 14 + glint, 255
        if abs(p[0] - 8) < 1.1 and abs(p[1] - 3.3) < 0.3:
            return 70, 84, 70, 255
        return base

    g.part(
        "head",
        4700,
        [
            g.box([2, 2, 3], [14, 13, 13], face_mat, density=2),
            g.box([3, 13, 4], [13, 15, 12], skin),
            g.box([4, 15, 5], [12, 16.5, 11], skin),
            g.box([4, 0, 4], [12, 2, 11.5], skin),
        ],
    )

    def suit(p, face, rng):
        k = 1 + (rng.random() - 0.5) * 0.1
        if face in ("north", "south") and (abs(p[0] - 8) < 0.6 or 5 <= p[1] < 6):
            return 70 * k, 220 * k, 150 * k, 255
        return 54 * k, 62 * k, 74 * k, 255

    g.part(
        "torso",
        4701,
        [
            g.box([5, 2, 6.5], [11, 11, 9.5], suit),
            g.box([4, 10, 6], [12, 14, 10], suit),
            g.box([7, 14, 7], [9, 17, 9], skin),
        ],
    )
    # Limbs hang below the model centre so they swing from the shoulder and hip.
    g.part("arm", 4702, [g.box([7.2, -9, 7.2], [8.8, 8, 8.8], skin), g.box([6.9, -11, 6.9], [9.1, -9, 9.1], skin)])
    g.part("leg", 4703, [g.box([7, -9, 7], [9, 8, 9], solid((54, 62, 74), 0.1)), g.box([6.8, -10, 5.8], [9.2, -9, 9.2], solid((40, 44, 52), 0.1))])
    g.write()


# ---------------------------------------------------------------- ship


def ship():
    g = Group("ship", 256)
    hull, trim, mast = planks((126, 86, 48)), wood((84, 56, 30), axis=2), wood((104, 76, 46))

    def sail(p, face, rng):
        stripe = int((p[0] + 0.5) // 3) % 2 == 0
        c = (236, 230, 212) if stripe else (190, 50, 44)
        k = (0.92 + 0.08 * math.sin(p[1] * 0.8)) * (1 + (rng.random() - 0.5) * 0.05)
        return c[0] * k, c[1] * k, c[2] * k, 255

    g.part(
        "ship",
        4800,
        [
            g.box([2.5, 2, -4], [13.5, 3, 20], hull),
            g.box([1.5, 2, -4], [2.5, 7.5, 20], hull),
            g.box([13.5, 2, -4], [14.5, 7.5, 20], hull),
            g.box([1.5, 2, 20], [14.5, 9, 21], hull),
            g.box([1.5, 7.5, 16], [14.5, 9, 20], hull, faces=("north", "south", "west", "east", "up")),
            g.box([3.5, 2, -7], [12.5, 7.5, -4], hull),
            g.box([5.5, 2.5, -10], [10.5, 7.5, -7], hull),
            g.box([7.5, 6.5, -15], [8.5, 7.5, -10], trim),
            g.box([1.2, 7.5, -4], [2.8, 8.2, 16], trim),
            g.box([13.2, 7.5, -4], [14.8, 8.2, 16], trim),
            g.box([7.4, 3, -2.2], [8.6, 30, -1], mast),
            g.box([0, 25.5, -3.2], [16, 26.5, -2.2], mast),
            g.box([0.5, 11, -3.9], [15.5, 25.5, -3.3], sail),
            g.box([2.5, 13, -4.5], [13.5, 23.5, -3.9], sail),
            g.box([8.6, 27.5, -1.8], [13.5, 30.5, -1.4], solid((190, 50, 44))),
        ],
    )
    g.write()


# ---------------------------------------------------------------- staffs


def staffs():
    g = Group("staffs", 256)

    def pole(mat, lo=-8.0, hi=24.0):
        return g.box([7.3, lo, 7.3], [8.7, hi, 8.7], mat)

    def skull(p, face, rng):
        if face == "north":
            for cx in (6.7, 9.3):
                if abs(p[0] - cx) < 0.75 and abs(p[1] - 27) < 0.8:
                    return 60, 235, 110, 255
            if abs(p[0] - 8) < 1.3 and abs(p[1] - 24.6) < 0.3:
                return 40, 40, 36, 255
        k = 1 + (rng.random() - 0.5) * 0.08
        return 226 * k, 220 * k, 200 * k, 255

    g.part(
        "necro",
        4900,
        [
            pole(wood((46, 40, 50)), hi=23.5),
            g.box([5.5, 23.5, 5.5], [10.5, 29, 10.5], skull, density=2),
            g.box([6.2, 22.4, 6], [9.8, 23.5, 10], solid((226, 220, 200), 0.08)),
            g.box([7.3, 29, 7.3], [8.7, 30.2, 8.7], solid((60, 235, 110), 0.1), shade=False),
        ],
    )

    def orb(p, face, rng):
        d = math.dist(p, (8, 27.5, 8)) / 3.6
        return 200 - 80 * d, 60 + 40 * (1 - d), 255, 255

    g.part(
        "evil",
        4901,
        [
            pole(wood((40, 24, 56)), hi=24),
            g.box([6, 25.5, 6], [10, 29.5, 10], orb, shade=False),
            g.box([5, 23.5, 7.4], [6, 27.5, 8.6], solid((30, 18, 40))),
            g.box([10, 23.5, 7.4], [11, 27.5, 8.6], solid((30, 18, 40))),
            g.box([7.4, 23.5, 5], [8.6, 27.5, 6], solid((30, 18, 40))),
            g.box([7.4, 23.5, 10], [8.6, 27.5, 11], solid((30, 18, 40))),
            g.box([5, 23, 5], [11, 24.2, 11], solid((30, 18, 40))),
        ],
    )
    gold = solid((238, 196, 64), 0.12)
    g.part(
        "white",
        4902,
        [
            pole(solid((236, 234, 226), 0.06), hi=23),
            g.box([7, 4, 7], [9, 5.2, 9], gold),
            g.box([7, 14, 7], [9, 15.2, 9], gold),
            g.box([6.6, 23, 6.6], [9.4, 24.4, 9.4], gold),
            g.box([5, 25.4, 7.5], [11, 29.6, 8.5], gold),
            g.box([7, 24.4, 7.4], [9, 31, 8.6], gold),
            g.box([4, 26.9, 7.5], [12, 28.1, 8.5], gold),
            g.box([7.1, 26.6, 7.2], [8.9, 28.4, 8.8], solid((255, 252, 220), 0.04), shade=False),
        ],
    )

    def leaves(p, face, rng):
        if rng.random() < 0.18:
            return 0, 0, 0, 0
        k = 0.7 + rng.random() * 0.5
        return 60 * k, 150 * k, 50 * k, 255

    bark = wood((104, 74, 44))
    g.part(
        "druid",
        4903,
        [
            pole(bark, hi=22),
            g.box([8.2, 10, 7], [9.4, 13, 8.4], bark),
            g.box([6.8, 21, 7.2], [8.4, 25.5, 8.8], bark),
            g.box([8, 24.5, 7.2], [10.6, 26, 8.8], bark),
            g.box([9.4, 26, 7.3], [10.8, 28.5, 8.7], bark),
            g.box([5, 24.5, 5.5], [9.5, 29, 10.5], leaves),
            g.box([8.5, 27, 6], [12.5, 31, 10], leaves),
            g.box([7.6, 15, 6.4], [9.2, 17, 7.4], leaves),
        ],
    )

    def glass(p, face, rng):
        a, b = plane(p, face)
        centre = 11.5 if face in ("north", "south") else 8
        if face in SIDES and (abs(a - centre) > 1.3 or abs(b - 20.1) > 1.9):
            return 44, 40, 36, 255
        k = 0.9 + rng.random() * 0.2
        return 255 * k, 214 * k, 96 * k, 255

    iron = solid((44, 40, 36), 0.1)
    g.part(
        "lantern",
        4904,
        [
            pole(wood((70, 52, 36)), hi=27),
            g.box([8.7, 25.6, 7.4], [12.4, 26.6, 8.6], iron),
            g.box([11.1, 23.4, 7.6], [11.9, 25.6, 8.4], iron),
            g.box([9.4, 22.4, 5.9], [13.6, 23.4, 10.1], iron),
            g.box([9.7, 17.8, 6.2], [13.3, 22.4, 9.8], glass, shade=False, density=2),
            g.box([9.4, 16.8, 5.9], [13.6, 17.8, 10.1], iron),
        ],
    )

    def crystal(p, face, rng):
        k = 0.75 + 0.25 * math.sin(p[1] * 1.7 + p[0] * 1.1 + p[2])
        return 110 * k, 225 * k, 255, 255

    g.part(
        "mage",
        4905,
        [
            pole(wood((40, 60, 120)), hi=23.5),
            g.box([6.4, 23.5, 6.4], [9.6, 24.8, 9.6], gold),
            g.box([6.6, 24.8, 6.6], [9.4, 29, 9.4], crystal, shade=False),
            g.box([7.3, 29, 7.3], [8.7, 31, 8.7], crystal, shade=False),
            g.box([5.2, 25.5, 7.4], [6.2, 28, 8.6], crystal, shade=False),
            g.box([9.8, 25.5, 7.4], [10.8, 28, 8.6], crystal, shade=False),
        ],
    )
    g.write()


def register():
    path = f"{ROOT}/models/item/model.json"
    with open(path) as f:
        raw = f.read()
    data = json.loads(raw)
    known = {o["predicate"]["custom_model_data"]: o for o in data["overrides"]}
    for cmd, model in OVERRIDES:
        known[cmd] = {"predicate": {"custom_model_data": cmd}, "model": model}
    data["overrides"] = [known[k] for k in sorted(known)]
    with open(path, "w") as f:
        json.dump(data, f, indent=1)
        if raw.endswith("\n"):
            f.write("\n")


if __name__ == "__main__":
    bombs()
    blackhole()
    tornado()
    alien()
    ship()
    staffs()
    register()
    print(f"wrote {len(OVERRIDES)} models")
