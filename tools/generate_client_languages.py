"""Build client dictionaries, geometric flag icons and missing Polish SDF glyphs.

Requires Pillow, numpy, scipy and fontTools. Existing atlas pixels/metrics remain
unchanged; only missing letters are appended. Run from any directory.
"""
import json
from pathlib import Path

import numpy as np
from PIL import Image, ImageDraw, ImageFont
from scipy.ndimage import distance_transform_edt
from fontTools.ttLib import TTFont

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / "src/main/resources/assets/pizzadlc"
CODES = ("ru", "en", "uk", "pl", "de")
# key | Russian | English | Ukrainian | Polish | German
MESSAGES = """
language.title|Язык интерфейса|Interface language|Мова інтерфейсу|Język interfejsu|Sprache der Oberfläche
language.welcome|Добро пожаловать|Welcome|Ласкаво просимо|Witaj|Willkommen
language.choose|Выбери свой язык|Choose your language|Обери свою мову|Wybierz swój język|Wähle deine Sprache
language.hint|Его можно изменить в профиле ClickGUI|Change it later in your ClickGUI profile|Його можна змінити в профілі ClickGUI|Zmienisz go później w profilu ClickGUI|Später im ClickGUI-Profil ändern
language.continue|Продолжить|Continue|Продовжити|Kontynuuj|Weiter
language.apply|Сохранить|Save|Зберегти|Zapisz|Speichern
language.back|Назад|Back|Назад|Wstecz|Zurück
language.selected|Выбрано|Selected|Обрано|Wybrano|Ausgewählt
language.save_error|Не удалось сохранить язык. Попробуй ещё раз.|Could not save the language. Try again.|Не вдалося зберегти мову. Спробуй ще раз.|Nie udało się zapisać języka. Spróbuj ponownie.|Sprache konnte nicht gespeichert werden. Versuche es erneut.
profile.title|Профиль|Profile|Профіль|Profil|Profil
profile.user|Пользователь|User|Користувач|Użytkownik|Benutzer
profile.role|Роль|Role|Роль|Rola|Rolle
profile.expiry|Действует до|Expires on|Діє до|Ważny do|Gültig bis
profile.themes|Темы|Themes|Теми|Motywy|Designs
search.results|Найдено: %s|Found: %s|Знайдено: %s|Znaleziono: %s|Gefunden: %s
search.empty|Ничего не найдено по «%s»|No results for "%s"|Нічого не знайдено за «%s»|Brak wyników dla „%s”|Keine Ergebnisse für „%s“
menu.servers_saved|Сохранено серверов: %s|Saved servers: %s|Збережено серверів: %s|Zapisane serwery: %s|Gespeicherte Server: %s
commands.global.error|Неизвестная команда. Попробуй .help|Unknown command. Try .help|Невідома команда. Спробуй .help|Nieznane polecenie. Spróbuj .help|Unbekannter Befehl. Versuche .help
commands.help.available_list|Доступные команды клиента:|Available client commands:|Доступні команди клієнта:|Dostępne polecenia klienta:|Verfügbare Client-Befehle:
commands.help.description|Показывает команды клиента|Shows client commands|Показує команди клієнта|Wyświetla polecenia klienta|Zeigt Client-Befehle
commands.test.description|Проверяет команды|Runs a command test|Перевіряє команди|Testuje polecenia|Testet Befehle
commands.tp.description|Телепорт через пустоту; нужен /fly. Использование: .tp <x> <y> <z>|Teleports via the void; requires /fly. Usage: .tp <x> <y> <z>|Телепорт через порожнечу; потрібен /fly. Використання: .tp <x> <y> <z>|Teleportacja przez pustkę; wymaga /fly. Użycie: .tp <x> <y> <z>|Teleport durch die Leere; benötigt /fly. Verwendung: .tp <x> <y> <z>
commands.config.description|Управляет конфигами|Manages configs|Керує конфігами|Zarządza konfiguracjami|Verwaltet Konfigurationen
commands.friend.description|Управляет друзьями|Manages friends|Керує друзями|Zarządza znajomymi|Verwaltet Freunde
commands.language.description|Меняет язык интерфейса|Changes the interface language|Змінює мову інтерфейсу|Zmienia język interfejsu|Ändert die Sprache der Oberfläche
commands.macros.description|Управляет макросами|Manages key macros|Керує макросами|Zarządza makrami|Verwaltet Tastenmakros
commands.waypoints.description|Управляет точками|Manages waypoints|Керує точками|Zarządza punktami|Verwaltet Wegpunkte
commands.blockesp.description|Управляет списком блоков BlockESP|Manages the BlockESP block list|Керує списком блоків BlockESP|Zarządza listą bloków BlockESP|Verwaltet die BlockESP-Blockliste
commands.language.set|Язык изменён на %s|Language set to %s|Мову змінено на %s|Ustawiono język: %s|Sprache auf %s gesetzt
commands.language.error_not_found|Язык не найден: %s|Language not found: %s|Мову не знайдено: %s|Nie znaleziono języka: %s|Sprache nicht gefunden: %s
commands.language.available_list|Доступные языки: %s|Available languages: %s|Доступні мови: %s|Dostępne języki: %s|Verfügbare Sprachen: %s
"""
# Source label | Russian | English | Ukrainian | Polish | German
LABELS = """
Render|Визуалы|Render|Візуальні|Wygląd|Darstellung
Misc|Прочее|Misc|Інше|Inne|Sonstiges
World|Мир|World|Світ|Świat|Welt
Toggle|Переключать|Toggle|Перемикати|Przełącz|Umschalten
Hold|Удерживать|Hold|Утримувати|Przytrzymaj|Halten
Нажмите клавишу…|Нажми клавишу…|Press a key…|Натисни клавішу…|Naciśnij klawisz…|Taste drücken…
Не назначен|Не назначен|Unassigned|Не призначено|Nie przypisano|Nicht belegt
Не выбрано|Не выбрано|Not selected|Не обрано|Nie wybrano|Nicht ausgewählt
Здесь пусто|Здесь пусто|Nothing here|Тут порожньо|Tutaj jest pusto|Hier ist nichts
Первый цвет|Первый цвет|Primary color|Перший колір|Pierwszy kolor|Erste Farbe
Второй цвет|Второй цвет|Secondary color|Другий колір|Drugi kolor|Zweite Farbe
Загрузка ресурсов|Загрузка ресурсов|Loading resources|Завантаження ресурсів|Wczytywanie zasobów|Ressourcen werden geladen
Combat|Бой|Combat|Бій|Walka|Kampf
Movement|Движение|Movement|Рух|Ruch|Bewegung
Visuals|Визуалы|Visuals|Візуальні|Wygląd|Darstellung
Player|Игрок|Player|Гравець|Gracz|Spieler
Other|Прочее|Other|Інше|Inne|Sonstiges
Environment|Окружение|Environment|Оточення|Otoczenie|Umgebung
Themes|Темы|Themes|Теми|Motywy|Designs
Profile|Профиль|Profile|Профіль|Profil|Profil
User|Пользователь|User|Користувач|Użytkownik|Benutzer
Role|Роль|Role|Роль|Rola|Rolle
Expires on|Действует до|Expires on|Діє до|Ważny do|Gültig bis
Главная|Главная|Home|Головна|Strona główna|Startseite
Одиночная игра|Одиночная игра|Singleplayer|Одиночна гра|Gra jednoosobowa|Einzelspieler
Сетевая игра|Сетевая игра|Multiplayer|Мережева гра|Gra wieloosobowa|Mehrspieler
Аккаунты|Аккаунты|Accounts|Акаунти|Konta|Konten
Настройки|Настройки|Settings|Налаштування|Ustawienia|Einstellungen
Выход|Выход|Quit|Вихід|Wyjdź|Beenden
Назад|Назад|Back|Назад|Wstecz|Zurück
Готово|Готово|Done|Готово|Gotowe|Fertig
Сохранить|Сохранить|Save|Зберегти|Zapisz|Speichern
Отмена|Отмена|Cancel|Скасувати|Anuluj|Abbrechen
Удалить|Удалить|Delete|Видалити|Usuń|Löschen
Добавить|Добавить|Add|Додати|Dodaj|Hinzufügen
Играть|Играть|Play|Грати|Graj|Spielen
Сменить аккаунт|Сменить аккаунт|Switch account|Змінити акаунт|Zmień konto|Konto wechseln
Добро пожаловать,|Добро пожаловать,|Welcome,|Ласкаво просимо,|Witaj,|Willkommen,
Сервера и быстрый вход|Серверы и быстрый вход|Servers and quick join|Сервери та швидкий вхід|Serwery i szybkie wejście|Server und Schnellbeitritt
Миры и сохранения|Миры и сохранения|Worlds and saves|Світи та збереження|Światy i zapisy|Welten und Spielstände
Все сервера|Все серверы|All servers|Усі сервери|Wszystkie serwery|Alle Server
СЕРВЕРЫ|СЕРВЕРЫ|SERVERS|СЕРВЕРИ|SERWERY|SERVER
Серверов пока нет — добавь их в сетевой игре|Серверов пока нет — добавь их в сетевой игре|No servers yet — add them in Multiplayer|Серверів ще немає — додай їх у мережевій грі|Brak serwerów — dodaj je w grze wieloosobowej|Noch keine Server — füge sie im Mehrspieler-Menü hinzu
Недоступна для этого аккаунта|Недоступна для этого аккаунта|Unavailable for this account|Недоступна для цього акаунта|Niedostępna dla tego konta|Für dieses Konto nicht verfügbar
Поиск модулей...|Поиск модулей...|Search modules...|Пошук модулів...|Szukaj modułów...|Module suchen...
Ничего не найдено|Ничего не найдено|No results|Нічого не знайдено|Brak wyników|Keine Ergebnisse
Попробуйте другой запрос|Попробуй другой запрос|Try another search|Спробуй інший запит|Spróbuj innego zapytania|Versuche eine andere Suche
ВСЕГДА|ВСЕГДА|ALWAYS|ЗАВЖДИ|ZAWSZE|IMMER
Нет|Нет|None|Немає|Brak|Keine
Да|Да|Yes|Так|Tak|Ja
Все|Все|All|Усі|Wszystkie|Alle
Режим|Режим|Mode|Режим|Tryb|Modus
Тип|Тип|Type|Тип|Typ|Typ
Стиль|Стиль|Style|Стиль|Styl|Stil
Цвет|Цвет|Color|Колір|Kolor|Farbe
Размер|Размер|Size|Розмір|Rozmiar|Größe
Скорость|Скорость|Speed|Швидкість|Prędkość|Geschwindigkeit
Дальность|Дальность|Range|Дальність|Zasięg|Reichweite
Дистанция|Дистанция|Distance|Відстань|Odległość|Distanz
Радиус|Радиус|Radius|Радіус|Promień|Radius
Высота|Высота|Height|Висота|Wysokość|Höhe
Ширина|Ширина|Width|Ширина|Szerokość|Breite
Длина|Длина|Length|Довжина|Długość|Länge
Толщина|Толщина|Thickness|Товщина|Grubość|Dicke
Прозрачность|Прозрачность|Opacity|Прозорість|Krycie|Deckkraft
Плавность|Плавность|Smoothness|Плавність|Płynność|Glätte
Задержка|Задержка|Delay|Затримка|Opóźnienie|Verzögerung
Модель|Модель|Model|Модель|Model|Modell
Лицо|Лицо|Face|Обличчя|Twarz|Gesicht
На себя|На себя|Self|На себе|Na siebie|Eigener Spieler
На друзей|На друзей|Friends|На друзів|Na znajomych|Freunde
На всех игроков|На всех игроков|All players|На всіх гравців|Na wszystkich graczy|Alle Spieler
Рука от модели|Рука от модели|Model hand|Рука моделі|Ręka modelu|Modellhand
Обычное|Обычное|Normal|Звичайне|Normalna|Normal
Говорит|Говорит|Talking|Говорить|Mówi|Sprechend
Странное|Странное|Strange|Дивне|Dziwna|Seltsam
Странное говорит|Странное говорит|Strange talking|Дивне говорить|Dziwna mówiąca|Seltsam sprechend
Странное кричит|Странное кричит|Strange shouting|Дивне кричить|Dziwna krzycząca|Seltsam schreiend
Плохое|Плохое|Bad|Погане|Zła|Schlecht
Очень плохое|Очень плохое|Very bad|Дуже погане|Bardzo zła|Sehr schlecht
Игроки|Игроки|Players|Гравці|Gracze|Spieler
Друзья|Друзья|Friends|Друзі|Znajomi|Freunde
Мобы|Мобы|Mobs|Моби|Moby|Kreaturen
Животные|Животные|Animals|Тварини|Zwierzęta|Tiere
Невидимые|Невидимые|Invisible|Невидимі|Niewidoczni|Unsichtbare
Броня|Броня|Armor|Броня|Pancerz|Rüstung
Предметы|Предметы|Items|Предмети|Przedmioty|Gegenstände
Цели|Цели|Targets|Цілі|Cele|Ziele
Цель|Цель|Target|Ціль|Cel|Ziel
Здоровье|Здоровье|Health|Здоров'я|Zdrowie|Gesundheit
Урон|Урон|Damage|Шкода|Obrażenia|Schaden
Минимальный урон|Минимальный урон|Minimum damage|Мінімальна шкода|Minimalne obrażenia|Mindestschaden
Приоритет|Приоритет|Priority|Пріоритет|Priorytet|Priorität
Автоматически|Автоматически|Automatic|Автоматично|Automatycznie|Automatisch
Авто|Авто|Auto|Авто|Auto|Auto
Обычный|Обычный|Normal|Звичайний|Normalny|Normal
Стандарт|Стандарт|Default|Стандарт|Domyślny|Standard
Включено|Включено|Enabled|Увімкнено|Włączone|Aktiviert
Выключено|Выключено|Disabled|Вимкнено|Wyłączone|Deaktiviert
Фон|Фон|Background|Тло|Tło|Hintergrund
Текст|Текст|Text|Текст|Tekst|Text
Тень|Тень|Shadow|Тінь|Cień|Schatten
Обводка|Обводка|Outline|Контур|Obrys|Umrandung
Анимация|Анимация|Animation|Анімація|Animacja|Animation
Анимации|Анимации|Animations|Анімації|Animacje|Animationen
Радуга|Радуга|Rainbow|Веселка|Tęcza|Regenbogen
Свечение|Свечение|Glow|Сяйво|Poświata|Leuchten
Размытие|Размытие|Blur|Розмиття|Rozmycie|Unschärfe
Частицы|Частицы|Particles|Частинки|Cząsteczki|Partikel
Клавиша|Клавиша|Key|Клавіша|Klawisz|Taste
Бинд|Бинд|Keybind|Прив'язка|Skrót klawiszowy|Tastenbelegung
Сбросить|Сбросить|Reset|Скинути|Resetuj|Zurücksetzen
Сброс|Сброс|Reset|Скидання|Reset|Zurücksetzen
Позиция|Позиция|Position|Позиція|Pozycja|Position
Слева|Слева|Left|Ліворуч|Po lewej|Links
Справа|Справа|Right|Праворуч|Po prawej|Rechts
Сверху|Сверху|Top|Зверху|U góry|Oben
Снизу|Снизу|Bottom|Знизу|U dołu|Unten
По центру|По центру|Center|По центру|Na środku|Zentriert
Основной|Основной|Primary|Основний|Główny|Primär
Дополнительный|Дополнительный|Secondary|Додатковий|Dodatkowy|Sekundär
Акцент|Акцент|Accent|Акцент|Akcent|Akzent
Эмоции|Эмоции|Emotes|Емоції|Emotki|Emotes
центр - стоп|центр - стоп|center - stop|центр - стоп|środek - stop|Mitte - Stopp
веди мышью|веди мышью|move the mouse|рухай мишею|porusz myszą|Maus bewegen
отпусти|отпусти|release|відпусти|puść|loslassen
прокрутка|прокрутка|scroll|прокрутка|przewijanie|scrollen
Привет|Привет|Wave|Привіт|Pomachaj|Winken
Аплодисменты|Аплодисменты|Clap|Оплески|Oklaski|Klatschen
Указать|Указать|Point|Вказати|Wskaż|Zeigen
Иди сюда|Иди сюда|Come here|Іди сюди|Chodź tutaj|Komm her
Фейспалм|Фейспалм|Facepalm|Фейспалм|Facepalm|Facepalm
Плач|Плач|Cry|Плач|Płacz|Weinen
Сальто|Сальто|Backflip|Сальто|Salto|Rückwärtssalto
Тверк|Тверк|Twerk|Тверк|Twerk|Twerk
Казачок|Казачок|Kazotsky kick|Козачок|Kozaczek|Kasatschok
Гопак|Гопак|Hopak|Гопак|Hopak|Hopak
Танец пингвина|Танец пингвина|Penguin dance|Танець пінгвіна|Taniec pingwina|Pinguin-Tanz
Танец с зельем|Танец с зельем|Potion dance|Танець із зіллям|Taniec z miksturą|Trank-Tanz
Рампейдж|Рампейдж|Rampage|Рампейдж|Rampage|Rampage
Рампейдж 2|Рампейдж 2|Rampage 2|Рампейдж 2|Rampage 2|Rampage 2
Рампейдж 3|Рампейдж 3|Rampage 3|Рампейдж 3|Rampage 3|Rampage 3
Рампейдж 4|Рампейдж 4|Rampage 4|Рампейдж 4|Rampage 4|Rampage 4
"""


def table(source):
    catalogs = {code: {} for code in CODES}
    for row in source.strip().splitlines():
        key, *values = row.split("|")
        if len(values) != 5 or key in catalogs["ru"]:
            raise ValueError(f"Invalid/duplicate translation row: {row}")
        for code, value in zip(CODES, values):
            catalogs[code][key] = value
    return catalogs


def flags():
    folder = ASSETS / "textures/gui/languages"
    folder.mkdir(parents=True, exist_ok=True)
    colors = {"ru": ("#ffffff", "#0039a6", "#d52b1e"), "ua": ("#0057b7", "#ffd700"),
              "pl": ("#ffffff", "#dc143c"), "de": ("#111111", "#dd0000", "#ffce00")}
    for name in (*colors, "gb"):
        im = Image.new("RGBA", (384, 256))
        draw = ImageDraw.Draw(im)
        if name == "gb":
            draw.rectangle((0, 0, 384, 256), fill="#012169")
            draw.line((0, 0, 384, 256), fill="white", width=60)
            draw.line((0, 256, 384, 0), fill="white", width=60)
            draw.line((0, -7, 384, 249), fill="#c8102e", width=20)
            draw.line((0, 263, 384, 7), fill="#c8102e", width=20)
            draw.rectangle((160, 0, 224, 256), fill="white")
            draw.rectangle((0, 94, 384, 162), fill="white")
            draw.rectangle((173, 0, 211, 256), fill="#c8102e")
            draw.rectangle((0, 108, 384, 148), fill="#c8102e")
        else:
            for i, color in enumerate(colors[name]):
                draw.rectangle((0, round(i * 256 / len(colors[name])), 384,
                                round((i + 1) * 256 / len(colors[name]))), fill=color)
        im.resize((96, 64), Image.Resampling.LANCZOS).save(folder / f"{name}.png")


def polish_glyphs():
    fonts = {"nexa_regular": "Nexa-Regular.ttf", "nexa_book": "Nexa-Book.ttf",
             "sfpro": "sfprosemibold.otf", "regular": "sfpromedium.otf"}
    catalogs = [json.loads(path.read_text(encoding="utf-8")) for path in (ASSETS / "lang/client").glob("*.json")]
    letters = {c for data in catalogs for section in data.values() for value in section.values() for c in value if not c.isspace()}
    # Coverage from Minecraft 26.2's actual English/Russian/Ukrainian/Polish/German menu translations.
    menu_glyphs = json.loads((ROOT / "tools/minecraft_ui_glyphs.json").read_text(encoding="utf-8"))
    letters.update(map(chr, menu_glyphs["codepoints"]))
    letters = sorted(letters)
    for name, filename in fonts.items():
        path = ASSETS / f"fonts/msdf/{name}.json"
        data = json.loads(path.read_text(encoding="utf-8"))
        existing = {g["unicode"] for g in data["glyphs"]}
        missing = [c for c in letters if ord(c) not in existing]
        if not missing:
            continue
        source = ASSETS / "fonts" / filename
        sources = [source, Path("C:/Windows/Fonts/segoeui.ttf"), Path("C:/Windows/Fonts/seguisym.ttf")]
        cmaps = {p: TTFont(p).getBestCmap() for p in sources if p.exists()}
        image_path = path.with_suffix(".png")
        original = Image.open(image_path).convert("RGBA")
        old_width, height = original.size
        atlas = Image.new("RGBA", (old_width + 320, height), (0, 0, 0, 0))
        atlas.paste(original, (0, 0))
        for i, char in enumerate(missing):
            chosen = next((p for p, cmap in cmaps.items() if ord(char) in cmap), None)
            if chosen is None:
                raise ValueError(f"No source glyph for {char!r} in {filename}")
            font = ImageFont.truetype(str(chosen), 64)
            # Same 64 px/em and 12 px distance range as the bundled MSDF shader.
            left, top, right, bottom = font.getbbox(char, anchor="ls")
            pad = 7
            w, h = right - left + 2 * pad, bottom - top + 2 * pad
            mask = Image.new("L", (w * 4, h * 4))
            high_font = ImageFont.truetype(str(chosen), 256)
            ImageDraw.Draw(mask).text(((pad - left) * 4, (pad - top) * 4), char, font=high_font, fill=255, anchor="ls")
            inside = np.asarray(mask) >= 128
            distance = (distance_transform_edt(inside) - distance_transform_edt(~inside)) / 4
            distance = np.clip(127.5 + distance * 255 / 12, 0, 255).astype(np.uint8)
            sdf = Image.fromarray(distance).resize((w, h), Image.Resampling.LANCZOS)
            rgba = Image.merge("RGBA", (sdf, sdf, sdf, Image.new("L", (w, h), 255)))
            x, y = old_width + (i % 4) * 80, (i // 4) * 96
            atlas.paste(rgba, (x, y))
            data["glyphs"].append({"unicode": ord(char), "advance": font.getlength(char) / 64,
                "planeBounds": {"left": (left - pad) / 64, "right": (right + pad) / 64,
                                "top": (-top + pad) / 64, "bottom": (-bottom - pad) / 64},
                "atlasBounds": {"left": x, "right": x + w, "top": height - y, "bottom": height - y - h}})
        data["atlas"]["width"] = atlas.width
        atlas.save(image_path)
        path.write_text(json.dumps(data, ensure_ascii=False, separators=(",", ":")) + "\n", encoding="utf-8")
        print(f"{name}: appended {len(missing)} language glyphs")


def main():
    messages, labels = table(MESSAGES), table(LABELS)
    for filename in ("client_translations_extra.tsv", "client_settings_translations.tsv", "client_descriptions_translations.tsv"):
        path = Path(__file__).with_name(filename)
        if not path.exists():
            continue
        seen = set()
        for row in path.read_text(encoding="utf-8").splitlines():
            if not row.strip() or row.startswith("#"):
                continue
            parts = row.split("|")
            if len(parts) == 5:
                parts.insert(1, parts[0])
            if len(parts) != 6 or parts[0] in seen:
                raise ValueError(f"Invalid/duplicate translation row in {filename}: {row}")
            seen.add(parts[0])
            for code, value in zip(CODES, parts[1:]):
                labels[code][parts[0]] = value
    # Product names, anti-cheat names and measurement symbols are intentionally invariant.
    for name in ("Chaos", "Dragon Fly", "Grim", "Haze", "HvH", "Intave", "Jello", "Peanut", "Polar", "S9", "Tonos", "°", "–"):
        for code in CODES:
            labels[code][name] = name
    target = ASSETS / "lang/client"
    target.mkdir(parents=True, exist_ok=True)
    for code in CODES:
        (target / f"{code}.json").write_text(json.dumps({"messages": messages[code], "labels": labels[code]},
            ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    flags()
    polish_glyphs()
    print(f"Built five languages: {len(messages['en'])} messages, {len(labels['en'])} labels each")


if __name__ == "__main__":
    main()
