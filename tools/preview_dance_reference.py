"""Extract a contact sheet from a local dance reference (OpenCV/Pillow)."""
import argparse
from pathlib import Path

import cv2
from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[1]


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("video", type=Path)
    parser.add_argument("--start", type=float, default=0)
    parser.add_argument("--end", type=float)
    parser.add_argument("--count", type=int, default=12)
    parser.add_argument("--label", default="sheet")
    args = parser.parse_args()
    cap = cv2.VideoCapture(str(args.video))
    if not cap.isOpened():
        raise ValueError(f"Cannot open {args.video}")
    fps = cap.get(cv2.CAP_PROP_FPS)
    duration = cap.get(cv2.CAP_PROP_FRAME_COUNT) / fps
    end = duration if args.end is None else min(args.end, duration)
    print(f"{args.video.name}: {duration:.3f} seconds, {fps:g} fps, "
          f"{int(cap.get(3))}x{int(cap.get(4))}")
    sheet = Image.new("RGB", (1200, ((args.count + 3) // 4) * 554), (24, 28, 36))
    draw = ImageDraw.Draw(sheet)
    for i in range(args.count):
        time = args.start + (i + .25) * (end - args.start) / args.count
        cap.set(cv2.CAP_PROP_POS_MSEC, time * 1000)
        ok, frame = cap.read()
        if not ok:
            raise ValueError(f"Frame missing at {time}")
        picture = Image.fromarray(cv2.cvtColor(frame, cv2.COLOR_BGR2RGB))
        picture.thumbnail((300, 524))
        x, y = (i % 4) * 300, (i // 4) * 554
        sheet.paste(picture, (x + (300 - picture.width) // 2, y + 24))
        draw.text((x + 8, y + 5), f"{time:.2f} s", fill="white")
    cap.release()
    target = ROOT / "build/dance-previews"
    target.mkdir(parents=True, exist_ok=True)
    path = target / f"{args.video.stem}-{args.label}.jpg"
    sheet.save(path, quality=93)
    print(path)


if __name__ == "__main__":
    main()
