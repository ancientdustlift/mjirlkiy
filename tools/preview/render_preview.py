#!/usr/bin/env python3
"""Draws the poses written by the dumpModels Gradle task as a contact sheet.

Usage: ./gradlew dumpModels && python3 tools/preview/render_preview.py build/preview/vlad_logutov.txt out.png
Needs Pillow and numpy. Software rasteriser with a z-buffer; alpha below 0.1 is cut out like in game.
"""
import math
import sys

import numpy as np
from PIL import Image, ImageDraw

TEXTURE = "src/main/resources/assets/kalabushkin/textures/entity/vlad_logutov.png"
SIZE = 360
SCALE = 130  # pixels per block
VIEWS = [("side", -90, 8), ("front 3/4", 145, 15), ("back 3/4", -35, 15)]


def load(path):
    poses, name, verts = [], None, []
    for line in open(path):
        if line.startswith("pose "):
            if name:
                poses.append((name, np.array(verts).reshape(-1, 4, 8)))
            name, verts = line.split()[1], []
        else:
            verts.append([float(v) for v in line.split()])
    poses.append((name, np.array(verts).reshape(-1, 4, 8)))
    return poses


def render(quads, tex, yaw, pitch):
    color = np.zeros((SIZE, SIZE, 3), np.float32)
    color[:] = (0.62, 0.75, 0.9)
    color[int(SIZE * 0.86):] = (0.45, 0.62, 0.35)
    depth = np.full((SIZE, SIZE), -1e9, np.float32)
    cy, sy = math.cos(math.radians(yaw)), math.sin(math.radians(yaw))
    cp, sp = math.cos(math.radians(pitch)), math.sin(math.radians(pitch))
    th, tw = tex.shape[:2]
    for quad in quads:
        # LivingEntityRenderer flips x and y and lifts the model by 1.501 blocks.
        x, y, z = -quad[:, 0], 1.501 - quad[:, 1], quad[:, 2]
        nx, ny, nz = -quad[0, 5], -quad[0, 6], quad[0, 7]
        rx, rz = x * cy + z * sy, -x * sy + z * cy
        ry, rz = y * cp - rz * sp, y * sp + rz * cp
        sx = SIZE / 2 + rx * SCALE
        syy = SIZE * 0.86 - ry * SCALE
        shade = 0.62 + 0.38 * abs(ny) if abs(ny) > 0.5 else 0.72 + 0.2 * abs(nx * cy + nz * sy)
        for tri in ((0, 1, 2), (0, 2, 3)):
            a, b, c = tri
            x0, x1 = int(max(min(sx[[a, b, c]]), 0)), int(min(max(sx[[a, b, c]]) + 1, SIZE - 1))
            y0, y1 = int(max(min(syy[[a, b, c]]), 0)), int(min(max(syy[[a, b, c]]) + 1, SIZE - 1))
            if x1 < x0 or y1 < y0:
                continue
            px, py = np.meshgrid(np.arange(x0, x1 + 1) + 0.5, np.arange(y0, y1 + 1) + 0.5)
            det = (syy[b] - syy[c]) * (sx[a] - sx[c]) + (sx[c] - sx[b]) * (syy[a] - syy[c])
            if abs(det) < 1e-9:
                continue
            wa = ((syy[b] - syy[c]) * (px - sx[c]) + (sx[c] - sx[b]) * (py - syy[c])) / det
            wb = ((syy[c] - syy[a]) * (px - sx[c]) + (sx[a] - sx[c]) * (py - syy[c])) / det
            wc = 1 - wa - wb
            inside = (wa >= 0) & (wb >= 0) & (wc >= 0)
            d = wa * rz[a] + wb * rz[b] + wc * rz[c]
            u = wa * quad[a, 3] + wb * quad[b, 3] + wc * quad[c, 3]
            v = wa * quad[a, 4] + wb * quad[b, 4] + wc * quad[c, 4]
            texel = tex[np.clip((v * th).astype(int), 0, th - 1), np.clip((u * tw).astype(int), 0, tw - 1)]
            sub_depth = depth[y0:y1 + 1, x0:x1 + 1]
            visible = inside & (texel[..., 3] > 25) & (d > sub_depth)
            sub_depth[visible] = d[visible]
            color[y0:y1 + 1, x0:x1 + 1][visible] = texel[..., :3][visible] / 255.0 * shade
    return Image.fromarray((np.clip(color, 0, 1) * 255).astype(np.uint8))


def main():
    poses = load(sys.argv[1])
    tex = np.array(Image.open(TEXTURE).convert("RGBA"))
    sheet = Image.new("RGB", (SIZE * len(VIEWS), SIZE * len(poses)))
    draw = ImageDraw.Draw(sheet)
    for row, (name, quads) in enumerate(poses):
        for col, (view, yaw, pitch) in enumerate(VIEWS):
            sheet.paste(render(quads, tex, yaw, pitch), (col * SIZE, row * SIZE))
            draw.text((col * SIZE + 6, row * SIZE + 4), f"{name} / {view}", fill=(0, 0, 0))
    sheet.save(sys.argv[2])


if __name__ == "__main__":
    main()
