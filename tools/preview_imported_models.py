"""Offline geometry/material preview of the converted meshes; requires numpy/Pillow."""
import json
import math
import struct
from pathlib import Path

import numpy as np
from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[1]


def load(id_):
    folder = ROOT / "src/main/resources/assets/pizzadlc/models" / id_
    raw = (folder / "mesh.bin").read_bytes()
    assert raw[:4] == b"CSM1"
    pivots = np.frombuffer(raw, ">f4", count=18, offset=4).reshape(6, 3)
    count = struct.unpack_from(">I", raw, 100)[0]
    cursor = 104
    batches = []
    for _ in range(count):
        mat, vertices, indices = struct.unpack_from(">III", raw, cursor)
        cursor += 12
        data = np.frombuffer(raw, ">f4", count=vertices * 14, offset=cursor).reshape(-1, 14).astype(float)
        cursor += vertices * 14 * 4
        triangles = np.frombuffer(raw, ">i4", count=indices, offset=cursor).reshape(-1, 3)
        cursor += indices * 4
        batches.append((data, triangles, np.array(Image.open(folder / f"material_{mat}.png"))))
    return pivots, batches


def render(id_, yaw, walk):
    pivots, batches = load(id_)
    image = Image.new("RGB", (420, 540), (27, 32, 43))
    draw = ImageDraw.Draw(image)
    draw.line((20, 493, 400, 493), fill=(62, 72, 90))
    y = math.radians(yaw)
    view = np.array([[math.cos(y), 0, -math.sin(y)], [0, 1, 0], [math.sin(y), 0, math.cos(y)]])
    triangles = []
    for data, indices, texture in batches:
        p, n = data[:, :3].copy(), data[:, 5:8].copy()
        if walk:
            p[:] = 0; n[:] = 0
            for part in range(6):
                angle = [0, 0, -.7, .7, .7, -.7][part]
                c, s = math.cos(angle), math.sin(angle)
                rot = np.array([[1, 0, 0], [0, c, -s], [0, s, c]])
                p += data[:, 8 + part:9 + part] * ((data[:, :3] - pivots[part]) @ rot.T + pivots[part])
                n += data[:, 8 + part:9 + part] * (data[:, 5:8] @ rot.T)
        p, n = p @ view.T, n @ view.T
        for tri in indices:
            xyz = p[tri]
            uv = data[tri, 3:5].mean(axis=0) % 1
            px = min(int(uv[0] * texture.shape[1]), texture.shape[1] - 1)
            py = min(int(uv[1] * texture.shape[0]), texture.shape[0] - 1)
            pixel = texture[py, px]
            if pixel[3] < 128:
                continue
            normal = n[tri].mean(axis=0)
            normal /= max(np.linalg.norm(normal), 1e-8)
            brightness = .4 + .6 * max(0, float(np.dot(normal, [0, -.4, -.9165])))
            color = tuple(np.uint8(pixel[:3] * brightness))
            polygon = [(210 + x * 250, 119 + yy * 250) for x, yy, z in xyz]
            triangles.append((xyz[:, 2].mean(), polygon, color))
    for _, polygon, color in sorted(triangles, key=lambda t: t[0], reverse=True):
        draw.polygon(polygon, fill=color)
    draw.text((15, 16), f"{id_} / {'walk' if walk else 'rest'} / {yaw} deg", fill="white")
    return image


def main():
    result = Image.new("RGB", (1260, 1080))
    for row, id_ in enumerate(("putin", "boykisser")):
        for col, (yaw, walk) in enumerate(((0, False), (55, False), (20, True))):
            result.paste(render(id_, yaw, walk), (col * 420, row * 540))
    target = ROOT / "build/model-previews"
    target.mkdir(parents=True, exist_ok=True)
    result.save(target / "imported-models.jpg")


if __name__ == "__main__":
    main()
