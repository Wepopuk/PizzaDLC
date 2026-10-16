"""Offline preview of poses exported by the Java movement animation check."""
import json
from pathlib import Path

import numpy as np
from PIL import Image, ImageDraw
from preview_emote import BOXES, FACES, rotation

ROOT = Path(__file__).resolve().parents[1]
TARGET = ROOT / "build/movement-previews"


def render(frame, title, yaw=55):
    image = Image.new("RGB", (360, 480), (27, 32, 43))
    draw = ImageDraw.Draw(image)
    draw.line((15, 430, 345, 430), fill=(68, 78, 94))
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
            polygon = [(180 + x * 11, 156 + y * 11) for x, y, z in xyz]
            faces.append((xyz[:, 2].mean(), polygon, tuple(int(c * light) for c in color)))
    for _, polygon, color in sorted(faces, key=lambda face: face[0], reverse=True):
        draw.polygon(polygon, fill=color, outline=(35, 42, 53))
    draw.text((16, 17), f"{title} / {frame['tick'] / 20:.2f} s", fill="white")
    draw.text((16, 452), "Offline model pose - no game physics", fill=(165, 177, 197))
    return image


def main():
    poses = json.loads((TARGET / "poses.json").read_text(encoding="utf-8"))
    rows = (("walking", "Walking"), ("running", "Running"),
            ("jump", "Jump - gentle arm sway"), ("sprintJump", "Sprint jump - overhead reach"))
    sheet = Image.new("RGB", (360 * 6, 480 * len(rows)))
    for row, (key, title) in enumerate(rows):
        frames = poses[key]
        for col, fraction in enumerate((0, .15, .3, .5, .7, .9)):
            sheet.paste(render(frames[round((len(frames) - 1) * fraction)], title), (col * 360, row * 480))
        images = [render(frame, title) for frame in frames]
        images[0].save(TARGET / f"{key}.gif", save_all=True, append_images=images[1:], duration=50, loop=0)
    sheet.save(TARGET / "movement-poses.jpg", quality=94)
    print(TARGET / "movement-poses.jpg")


if __name__ == "__main__":
    main()
