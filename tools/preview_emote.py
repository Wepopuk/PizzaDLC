"""Offline six-part Minecraft pose preview; this is not an in-game screenshot."""
import argparse
import json
import math
from pathlib import Path

import numpy as np
from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[1]
BOXES = {
    "head": ((-4, -8, -4), (4, 0, 4), (222, 178, 145)),
    "body": ((-4, 0, -2), (4, 12, 2), (60, 156, 176)),
    "rightArm": ((-3, -2, -2), (1, 10, 2), (222, 178, 145)),
    "leftArm": ((-1, -2, -2), (3, 10, 2), (222, 178, 145)),
    "rightLeg": ((-2, 0, -2), (2, 12, 2), (82, 97, 164)),
    "leftLeg": ((-2, 0, -2), (2, 12, 2), (82, 97, 164)),
}
FACES = ((0, 1, 3, 2), (4, 6, 7, 5), (0, 4, 5, 1),
         (2, 3, 7, 6), (0, 2, 6, 4), (1, 5, 7, 3))
CHANNELS = ("x", "y", "z", "pitch", "yaw", "roll")


def rotation(pitch, yaw, roll):
    x, y, z = map(math.radians, (pitch, yaw, roll))
    cx, sx, cy, sy, cz, sz = math.cos(x), math.sin(x), math.cos(y), math.sin(y), math.cos(z), math.sin(z)
    return (np.array([[cz, -sz, 0], [sz, cz, 0], [0, 0, 1]])
            @ np.array([[cy, 0, sy], [0, 1, 0], [-sy, 0, cy]])
            @ np.array([[1, 0, 0], [0, cx, -sx], [0, sx, cx]]))


def sample(moves, tick):
    right = next(i for i, frame in enumerate(moves) if frame["tick"] >= tick)
    left = max(right - 1, 0)
    a, b = moves[left], moves[right]
    span = b["tick"] - a["tick"]
    t = 0 if span == 0 else (tick - a["tick"]) / span
    t = (1 - math.cos(math.pi * t)) / 2
    return {part: {key: a[part][key] + (b[part][key] - a[part][key]) * t
                   for key in CHANNELS} for part in BOXES}


def render(moves, tick, title, yaw=25):
    frame = sample(moves, tick)
    image = Image.new("RGB", (320, 460), (27, 32, 43))
    draw = ImageDraw.Draw(image)
    draw.line((15, 407, 305, 407), fill=(68, 78, 94))
    view = rotation(0, yaw, 0)
    faces = []
    for part, (lo, hi, color) in BOXES.items():
        bone = frame[part]
        rot = rotation(bone["pitch"], bone["yaw"], bone["roll"])
        corners = np.array([[x, y, z] for x in (lo[0], hi[0])
                            for y in (lo[1], hi[1]) for z in (lo[2], hi[2])])
        corners = (corners @ rot.T + [bone["x"], bone["y"], bone["z"]]) @ view.T
        for indices in FACES:
            xyz = corners[list(indices)]
            normal = np.cross(xyz[1] - xyz[0], xyz[2] - xyz[0])
            normal /= np.linalg.norm(normal)
            light = .55 + .45 * abs(float(np.dot(normal, [0, -.4, -.9165])))
            polygon = [(160 + x * 12, 119 + y * 12) for x, y, z in xyz]
            faces.append((xyz[:, 2].mean(), polygon, tuple(int(c * light) for c in color)))
    for _, polygon, color in sorted(faces, key=lambda face: face[0], reverse=True):
        draw.polygon(polygon, fill=color, outline=(35, 42, 53))
    draw.text((12, 14), f"{title} / {tick / 20:.1f} s", fill="white")
    draw.text((12, 435), "Offline pose preview", fill=(165, 177, 197))
    return image


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("ids", nargs="+", help="Resource IDs")
    args = parser.parse_args()
    target = ROOT / "build/dance-previews"
    target.mkdir(parents=True, exist_ok=True)
    sheet = Image.new("RGB", (1920, len(args.ids) * 460))
    for row, id_ in enumerate(args.ids):
        data = json.loads((ROOT / f"src/main/resources/assets/pizzadlc/emotes/{id_}.json").read_text(encoding="utf-8"))
        moves, duration = data["emote"]["moves"], data["emote"]["endTick"]
        for col, fraction in enumerate((0, .2, .4, .55, .72, .9)):
            sheet.paste(render(moves, duration * fraction, data["name"]), (col * 320, row * 460))
        frames = [render(moves, tick, data["name"]) for tick in range(0, duration, 2)]
        frames[0].save(target / f"{id_}.gif", save_all=True, append_images=frames[1:], duration=100, loop=0)
    sheet.save(target / "larp333-poses.jpg", quality=94)
    print(target)


if __name__ == "__main__":
    main()
