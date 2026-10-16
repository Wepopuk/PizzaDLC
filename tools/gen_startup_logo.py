"""
Генератор логотипа для самого первого экрана загрузки (startup_logo.png).

При старте игры шейдеры и MSDF-шрифты клиента ещё не загружены, поэтому кристалл и надпись «PizzaDLC»
рисуются заранее в картинку — теми же формулами, что в crystal.fsh и msdf.fsh, и с той же раскладкой,
что MenuKit.loadingLogo: кристалл h*0.62, отступ h*0.16, текст Nexa Regular h*0.42.

Запуск из корня проекта:  python tools/gen_startup_logo.py
"""
import json
from pathlib import Path

import numpy as np
from PIL import Image

ROOT = Path(__file__).resolve().parent.parent
ASSETS = ROOT / "src/main/resources/assets/pizzadlc"
OUT = ASSETS / "textures/gui/startup_logo.png"

NAME = "PizzaDLC"
H = 256                       # высота логотипа (h в MenuKit.loadingLogo) в пикселях картинки
GEM = H * 0.62
FS = H * 0.42
GAP = H * 0.16
PAD = int(H * 0.12)           # запас под свечение вокруг кристалла
TEXT = (0xE9, 0xEA, 0xF2)
CRYSTAL_TOP = (0x4A, 0x4A, 0x4E)
CRYSTAL_BOTTOM = (0x2C, 0x2C, 0x2F)
SS = 4                        # суперсэмплинг кристалла


def load_font(name):
    data = json.loads((ASSETS / f"fonts/msdf/{name}.json").read_text(encoding="utf-8"))
    atlas = np.asarray(Image.open(ASSETS / f"fonts/msdf/{name}.png").convert("RGBA"), dtype=np.float32) / 255.0
    glyphs = {g["unicode"]: g for g in data["glyphs"]}
    return data, atlas, glyphs


def sample(atlas, u, v):
    """Билинейная выборка RGB атласа в пиксельных координатах (u вправо, v вниз)."""
    h, w = atlas.shape[:2]
    u = np.clip(u - 0.5, 0, w - 1.001)
    v = np.clip(v - 0.5, 0, h - 1.001)
    x0, y0 = np.floor(u).astype(int), np.floor(v).astype(int)
    fx, fy = (u - x0)[..., None], (v - y0)[..., None]
    c00, c10 = atlas[y0, x0, :3], atlas[y0, x0 + 1, :3]
    c01, c11 = atlas[y0 + 1, x0, :3], atlas[y0 + 1, x0 + 1, :3]
    return (c00 * (1 - fx) + c10 * fx) * (1 - fy) + (c01 * (1 - fx) + c11 * fx) * fy


def text_alpha(canvas_w, canvas_h, x, baseline):
    """Как MsdfTextRenderState + msdf.fsh: квад глифа от pen, верх = baseline - top*size."""
    data, atlas, glyphs = load_font("nexa_regular")
    rng = data["atlas"]["distanceRange"]
    aw, ah = data["atlas"]["width"], data["atlas"]["height"]
    out = np.zeros((canvas_h, canvas_w), dtype=np.float32)
    pen = x
    for ch in NAME:
        g = glyphs[ord(ch)]
        pb, ab = g.get("planeBounds"), g.get("atlasBounds")
        if pb and ab:
            gw, gh = (pb["right"] - pb["left"]) * FS, (pb["top"] - pb["bottom"]) * FS
            gx0, gy0 = pen, baseline - pb["top"] * FS
            xs = np.arange(int(np.floor(gx0)), int(np.ceil(gx0 + gw)))
            ys = np.arange(int(np.floor(gy0)), int(np.ceil(gy0 + gh)))
            px, py = np.meshgrid(xs + 0.5, ys + 0.5)
            tu = (px - gx0) / gw
            tv = (py - gy0) / gh
            u = ab["left"] + tu * (ab["right"] - ab["left"])
            v = ah - (ab["top"] - tv * (ab["top"] - ab["bottom"]))
            rgb = sample(atlas, u, v)
            med = np.maximum(np.minimum(rgb[..., 0], rgb[..., 1]), np.minimum(np.maximum(rgb[..., 0], rgb[..., 1]), rgb[..., 2]))
            texels_per_px = (ab["right"] - ab["left"]) / gw
            pixels = rng / (texels_per_px * np.sqrt(2.0))
            d = (med - 0.5) * pixels
            t = np.clip((d + 0.5), 0.0, 1.0)
            a = t * t * (3 - 2 * t)
            inside = (tu >= 0) & (tu <= 1) & (tv >= 0) & (tv <= 1)
            a = np.where(inside, a, 0.0)
            ys_c = np.clip(ys, 0, canvas_h - 1)
            xs_c = np.clip(xs, 0, canvas_w - 1)
            out[np.ix_(ys_c, xs_c)] = np.maximum(out[np.ix_(ys_c, xs_c)], a)
        pen += g["advance"] * FS
    return out


def text_width():
    _, _, glyphs = load_font("nexa_regular")
    return sum(glyphs[ord(c)]["advance"] for c in NAME) * FS


def crystal_rgba(size):
    """crystal.fsh: ромб-силуэт, корона/павильон, освещённая левая грань, линии огранки, блик."""
    n = int(round(size)) * SS
    c = (np.arange(n) + 0.5) / n
    px, py = np.meshgrid(c - 0.5, c - 0.5)
    GY = -0.05
    AP, SL, SR, BM = (0.0, -0.34), (-0.28, GY), (0.28, GY), (0.0, 0.36)

    def edge_side(p_x, p_y, a, b):
        return (b[0] - a[0]) * (p_y - a[1]) - (b[1] - a[1]) * (p_x - a[0])

    s1 = edge_side(px, py, AP, SR)
    s2 = edge_side(px, py, SR, BM)
    s3 = edge_side(px, py, BM, SL)
    s4 = edge_side(px, py, SL, AP)
    inside = ((s1 >= 0) & (s2 >= 0) & (s3 >= 0) & (s4 >= 0)) | ((s1 <= 0) & (s2 <= 0) & (s3 <= 0) & (s4 <= 0))

    crown = py < GY
    left = px < 0
    top = np.array(CRYSTAL_TOP, dtype=np.float32) / 255.0
    bottom = np.array(CRYSTAL_BOTTOM, dtype=np.float32) / 255.0
    base = np.where(crown[..., None], top, bottom)
    sh = np.where(crown, np.where(left, 1.12, 0.94), np.where(left, 0.98, 0.80))
    col = np.clip(base * sh[..., None], 0, 1)

    def seg_dist(p_x, p_y, a, b):
        ex, ey = b[0] - a[0], b[1] - a[1]
        wx, wy = p_x - a[0], p_y - a[1]
        t = np.clip((wx * ex + wy * ey) / (ex * ex + ey * ey), 0, 1)
        return np.hypot(wx - ex * t, wy - ey * t)

    pxw = 1.0 / size
    ew = pxw * 0.9
    e = np.minimum(seg_dist(px, py, AP, BM), seg_dist(px, py, SL, SR))
    edge = 1.0 - np.clip((e - ew * 0.5) / ew, 0, 1)
    col = col * (1 - edge[..., None] * 0.35) + np.minimum(top * 1.7, 1.0) * (edge[..., None] * 0.35)
    g = np.hypot((px + 0.05) * 1.2, py + 0.18)
    hl = (1.0 - np.clip((g - 0.03) / 0.06, 0, 1)) * 0.5
    col = col * (1 - hl[..., None]) + hl[..., None]

    rgba = np.zeros((n, n, 4), dtype=np.float32)
    rgba[..., :3] = col * inside[..., None]
    rgba[..., 3] = inside
    # Даунсэмпл с премультиплицированным альфа — гладкие края.
    rgba = rgba.reshape(n // SS, SS, n // SS, SS, 4).mean(axis=(1, 3))
    a = rgba[..., 3:4]
    rgba[..., :3] = np.where(a > 0, rgba[..., :3] / np.maximum(a, 1e-6), 0)
    return rgba


def main():
    tw = text_width()
    total = GEM + GAP + tw
    w = int(np.ceil(total)) + PAD * 2
    h = H + PAD * 2
    cy = h / 2.0
    x = PAD

    canvas = np.zeros((h, w, 4), dtype=np.float32)

    # Мягкое свечение вокруг кристалла (как Render2DUtil.glow с 0x1AFFFFFF).
    gcx, gcy = x + GEM / 2.0, cy
    yy, xx = np.mgrid[0:h, 0:w]
    dist = np.hypot(xx + 0.5 - gcx, yy + 0.5 - gcy) / (GEM * 0.75)
    glow = np.clip(1.0 - dist, 0, 1) ** 2 * (0x1A / 255.0)
    canvas[..., :3] = 1.0
    canvas[..., 3] = glow

    def over(dst, src_rgb, src_a, ox, oy):
        hh, ww = src_a.shape
        region = dst[oy:oy + hh, ox:ox + ww]
        a_d = region[..., 3:4]
        a_s = src_a[..., None]
        a_o = a_s + a_d * (1 - a_s)
        rgb = (src_rgb * a_s + region[..., :3] * a_d * (1 - a_s)) / np.maximum(a_o, 1e-6)
        region[..., :3] = rgb
        region[..., 3:4] = a_o

    crystal = crystal_rgba(GEM)
    ox, oy = int(round(x)), int(round(cy - crystal.shape[0] / 2.0))
    over(canvas, crystal[..., :3], crystal[..., 3], ox, oy)

    baseline = (cy - 0.35 * FS) + 0.7 * FS
    ta = text_alpha(w, h, x + GEM + GAP, baseline)
    text_rgb = np.broadcast_to(np.array(TEXT, dtype=np.float32) / 255.0, (h, w, 3))
    over(canvas, text_rgb, ta, 0, 0)

    OUT.parent.mkdir(parents=True, exist_ok=True)
    Image.fromarray((np.clip(canvas, 0, 1) * 255 + 0.5).astype(np.uint8), "RGBA").save(OUT)
    print(f"{OUT.relative_to(ROOT)}: {w}x{h} (логотип {H}px, поля {PAD}px)")


if __name__ == "__main__":
    main()
