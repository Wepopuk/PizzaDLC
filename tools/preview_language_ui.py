"""Offline layout proof using the bundled MSDF atlases and language/flag assets.

This does not launch Minecraft or replace an in-game rendering check.
"""
import json
from functools import lru_cache
from pathlib import Path
import numpy as np
from PIL import Image, ImageDraw, ImageFilter
from gen_startup_logo import load_font, sample, crystal_rgba

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / "src/main/resources/assets/pizzadlc"
OUT = ROOT / "build/language-previews"
S = 3
TEXT, UID, ACCENT = "#e9eaf2", "#8e8e93", "#8e8bff"
load_font = lru_cache(maxsize=2)(load_font)


class Canvas:
    def __init__(self, w, h):
        self.image = Image.new("RGBA", (w*S, h*S), "#101014")
        self.draw = ImageDraw.Draw(self.image)

    def rect(self, x, y, w, h, r=0, fill="#151414", border=None):
        self.draw.rounded_rectangle((round(x*S), round(y*S), round((x+w)*S), round((y+h)*S)),
            radius=r*S, fill=fill, outline=border, width=2)

    def text(self, text, x, y, size, color=TEXT, book=False):
        data, atlas, glyphs = load_font("nexa_book" if book else "nexa_regular")
        size *= S
        pen, baseline = x*S, (y*S + .7*size)
        ah = data["atlas"]["height"]
        for c in text:
            g = glyphs[ord(c)]
            pb, ab = g.get("planeBounds"), g.get("atlasBounds")
            if pb and ab:
                gw, gh = (pb["right"]-pb["left"])*size, (pb["top"]-pb["bottom"])*size
                gx, gy = pen, baseline-pb["top"]*size
                xs = np.arange(max(0, int(np.floor(gx))), min(self.image.width, int(np.ceil(gx+gw))))
                ys = np.arange(max(0, int(np.floor(gy))), min(self.image.height, int(np.ceil(gy+gh))))
                if len(xs) and len(ys):
                    px, py = np.meshgrid(xs+.5, ys+.5)
                    u = ab["left"]+(px-gx)/gw*(ab["right"]-ab["left"])
                    v = ah-(ab["top"]-(py-gy)/gh*(ab["top"]-ab["bottom"]))
                    median = np.median(sample(atlas, u, v), axis=2)
                    factor = data["atlas"]["distanceRange"] / ((ab["right"]-ab["left"])/gw*np.sqrt(2))
                    t = np.clip((median-.5)*factor+.5, 0, 1)
                    alpha = Image.fromarray((t*t*(3-2*t)*255).astype(np.uint8))
                    layer = Image.new("RGBA", alpha.size, color)
                    layer.putalpha(alpha)
                    self.image.alpha_composite(layer, (int(xs[0]), int(ys[0])))
            pen += g["advance"]*size
        return (pen-x*S)/S

    def width(self, text, size, book=False):
        _, _, glyphs = load_font("nexa_book" if book else "nexa_regular")
        return sum(glyphs[ord(c)]["advance"] for c in text)*size

    def flag(self, name, x, y, w, h):
        flag = Image.open(ASSETS / f"textures/gui/languages/{name}.png").convert("RGBA").resize((round(w*S), round(h*S)))
        mask = Image.new("L", flag.size)
        ImageDraw.Draw(mask).rounded_rectangle((0, 0, flag.width-1, flag.height-1), radius=3*S, fill=255)
        flag.putalpha(mask)
        self.image.alpha_composite(flag, (round(x*S), round(y*S)))


def picker(code):
    messages = json.loads((ASSETS/f"lang/client/{code}.json").read_text(encoding="utf-8"))["messages"]
    canvas = Canvas(440, 422)
    ox, oy = 24, 24
    canvas.rect(ox, oy, 392, 374, 12, "#0f0f0f", "#303030")
    crystal = Image.fromarray((crystal_rgba(18*S)*255).astype(np.uint8))
    canvas.image.alpha_composite(crystal, (round((ox+16)*S), round((oy+10)*S)))
    canvas.text("PizzaDLC", ox+39, oy+12.2, 11)
    canvas.text(messages["language.welcome"], ox+39, oy+23, 6.5, "#878787")
    canvas.text(messages["language.choose"], ox+22, oy+53, 15)
    canvas.text(messages["language.hint"], ox+22, oy+76, 7.5, UID, True)
    languages = [("en", "English", "English", "gb"), ("ru", "Русский", "Russian", "ru"),
        ("uk", "Українська", "Ukrainian", "ua"), ("pl", "Polski", "Polish", "pl"), ("de", "Deutsch", "German", "de")]
    for i, (lc, native, english, flag) in enumerate(languages):
        y = oy+92+i*44
        selected = lc == code
        canvas.rect(ox+22, y, 348, 38, 8, "#211f2c" if selected else "#151414", ACCENT if selected else "#303030")
        canvas.flag(flag, ox+33, y+8, 33, 22)
        canvas.text(native, ox+78, y+8, 10)
        canvas.text(english+"  ·  "+lc.upper(), ox+78, y+24, 6.5, UID, True)
        if selected:
            badge = messages["language.selected"]
            bw = canvas.width(badge, 7, True)+16
            canvas.rect(ox+357-bw, y+11, bw, 16, 5, "#303044")
            canvas.text(badge, ox+365-bw, y+16, 7, ACCENT, True)
    canvas.rect(ox+22, oy+324, 348, 27, 3, "#252323", "#3c3c3c")
    label = messages["language.continue"]
    canvas.text(label, ox+22+(348-canvas.width(label, 7.5, True))/2, oy+324+(27-5.25)/2, 7.5, TEXT, True)
    canvas.text("English · Русский · Українська · Polski · Deutsch", ox+22, oy+360, 6, UID, True)
    return canvas.image


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    for code in ("ru", "en", "uk", "pl", "de"):
        picker(code).save(OUT/f"language-{code}.png")
    result = Image.new("RGBA", (440*S*2, 422*S))
    result.paste(picker("pl"), (0, 0))
    result.paste(picker("de"), (440*S, 0))
    result.save(OUT/"languages-preview.png")
    print(OUT / "languages-preview.png")


if __name__ == "__main__":
    main()
