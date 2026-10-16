"""Offline layout proof from OptionControlsCheck geometry and the client's actual fonts.

This verifies spacing and glyph rendering; it is not an in-game screenshot.
"""
import json
from pathlib import Path
from PIL import Image
from preview_language_ui import Canvas, ASSETS, S, TEXT, UID, ACCENT

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / "build/settings-previews"


def fit(canvas, text, size, width, book=True):
    if canvas.width(text, size, book) <= width:
        return text
    while text and canvas.width(text + "…", size, book) > width:
        text = text[:-1]
    return text + "…"


def centered(canvas, text, x, y, w, h, size=7.5, color=TEXT):
    text = fit(canvas, text, size, w)
    canvas.text(text, x + (w-canvas.width(text, size, True))/2, y+(h-.7*size)/2, size, color, True)


def render(screen, code):
    canvas = Canvas(screen["width"], screen["height"])
    f = screen["frame"]
    labels = json.loads((ASSETS / f"lang/client/{code}.json").read_text(encoding="utf-8"))["labels"]
    local = lambda text: labels.get(text, text)
    wx, wy, ww, wh = [f[k] for k in ("winX", "winY", "winW", "winH")]
    canvas.rect(wx, wy, ww, wh, 10, "#0f0f0f", "#303030")
    canvas.text("PizzaDLC", wx+39, wy+12, 11)
    canvas.text(local("Настройки"), wx+39, wy+23, 6.5, UID)
    categories = ["Основное", "Графика", "Звук", "Управление", "Клавиши", "Чат", "Скин", "Язык", "Ресурспаки", "Доступность"]
    sidebar_w = f["contentX"] - f["sideX"] - 8
    step = max(20, min(28, (wh-10-44)//10))
    for i, category in enumerate(categories):
        y = wy + 44 + i*step
        if y + step > wy + wh - 8:
            break
        selected = i == 1
        if selected:
            canvas.rect(f["sideX"], y, sidebar_w, min(24, step-3), 5, "#252323", "#505050")
        canvas.rect(f["sideX"]+10, y+(step-3)/2-3, 6, 6, 2, "#e9eaf2" if selected else UID)
        label = fit(canvas, local(category), 9 if step < 23 else 10, sidebar_w-43, False)
        canvas.text(label, f["sideX"]+27, y+(step-3-.7*9)/2, 9, TEXT if selected else UID)
    canvas.rect(f["contentX"]-5, wy+12, 1, wh-24, 0, "#292929")
    centered(canvas, local("Графика"), f["contentX"], wy+4, f["contentW"], 26, 10)
    names = {
        "ru": ["Частота кадров", "Вертикальная синхронизация", "Частота кадров в фоне", "Масштаб интерфейса", "Полный экран", "Эксклюзивный полный экран", "Яркость", "Графический движок"],
        "de": ["Maximale Bildrate", "VSync verwenden", "Bildrate bei Inaktivität", "GUI-Größe", "Vollbild", "Exklusives Vollbild", "Helligkeit", "Grafik-Backend"],
    }[code]
    kinds = ["slider", "on", "cycle", "cycle", "off", "off", "slider", "cycle"]
    values = ["120", "Вкл." if code == "ru" else "An", "30", "3", "Выкл." if code == "ru" else "Aus", "Выкл." if code == "ru" else "Aus", "70%", "OpenGL"]
    for i, item in enumerate(screen["rows"]):
        if item["y"] + item["height"] > wy + wh - 32:
            break
        row, slider = item["row"], item["slider"]
        x, y, w, h = [item[k] for k in ("x", "y", "width", "height")]
        canvas.rect(x, y, w, h, 7, "#151414", "#222121")
        label = fit(canvas, names[i], 8, row["labelW"])
        canvas.text(label, row["labelX"], y+(h-5.6)/2, 8, TEXT, True)
        cx, cy, cw, ch = [row[k] for k in ("controlX", "controlY", "controlW", "controlH")]
        if kinds[i] == "slider":
            tx, ty, tw, th = [slider[k] for k in ("trackX", "trackY", "trackW", "trackH")]
            ratio = .55 if i == 0 else .7
            canvas.rect(tx-2, ty, tw+4, th, 2, "#252323")
            canvas.rect(tx-2, ty, tw*ratio+2, th, 2, ACCENT)
            canvas.rect(tx+tw*ratio-3, ty-1, 6, 6, 3, "white")
            vx, vy, vw, vh = [slider[k] for k in ("valueX", "valueY", "valueW", "valueH")]
            canvas.rect(vx, vy, vw, vh, 3, "#1c1c1c", "#353535")
            centered(canvas, values[i], vx+3, vy, vw-6, vh)
        elif kinds[i] in ("on", "off"):
            on = kinds[i] == "on"
            canvas.text(fit(canvas, values[i], 7.5, cw-39), cx+2, cy+(ch-5.25)/2, 7.5, UID, True)
            toggle_x, toggle_y = cx+cw-31, cy+(ch-14)/2
            canvas.rect(toggle_x, toggle_y, 29, 14, 7, ACCENT if on else "#252323")
            canvas.rect(toggle_x+(17 if on else 2), toggle_y+2, 10, 10, 5, "white" if on else "#929292")
        else:
            canvas.rect(cx, cy+2, cw, ch-4, 4, "#252323", "#353535")
            centered(canvas, values[i], cx+6, cy, cw-18, ch)
            canvas.draw.line([(round((cx+cw-10)*S), round((cy+ch/2-1)*S)),
                (round((cx+cw-8)*S), round((cy+ch/2+1)*S)),
                (round((cx+cw-6)*S), round((cy+ch/2-1)*S))], fill=UID, width=2)
    done_w = min(200, f["contentW"]-16)
    dx, dy = f["contentX"]+(f["contentW"]-done_w)/2, wy+wh-26
    canvas.rect(dx, dy, done_w, 20, 3, "#252323", "#4a4a4a")
    centered(canvas, "Готово" if code == "ru" else "Fertig", dx, dy, done_w, 20)
    return canvas.image


if __name__ == "__main__":
    screens = json.loads((OUT / "layout.json").read_text(encoding="utf-8"))
    for code in ("ru", "de"):
        render(screens[0], code).save(OUT / f"settings-{code}.png")
    render(screens[1], "ru").save(OUT / "settings-small.png")
    print(OUT / "settings-ru.png")
