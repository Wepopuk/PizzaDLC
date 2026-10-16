"""Wrap static display labels in existing UI calls, preserving user data/IDs.

This migration touches only drawing and width-measurement calls in the listed
files. Runtime data such as account names, search input and mode IDs is unchanged.
"""
import json
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
LABELS = json.loads((ROOT / "src/main/resources/assets/pizzadlc/lang/client/en.json").read_text(encoding="utf-8"))["labels"]
UI = ROOT / "src/main/java/pizza/dlc/api/ui"
FILES = ("clickgui/ClickGuiScreen.java", "clickgui/ColorPicker.java", "menu/MenuKit.java", "menu/MainMenuScreen.java")
CALLS = re.compile(r"\b(?:regText|bookText|sfText|regW|bookW|sfW|msdfText|msdfWidth|msdfVisualWidth|section|pill|sidebarRow|profileCard|tile|truncateToWidth|truncate)\s*\(")
STRING = re.compile(r'"(?:[^"\\]|\\.)*"')


def main():
    for filename in FILES:
        path = UI / filename
        source = path.read_text(encoding="utf-8")
        ranges = []
        for call in CALLS.finditer(source):
            cursor, depth = call.end(), 1
            while cursor < len(source) and depth:
                if source[cursor] == '"':
                    cursor = STRING.match(source, cursor).end()
                    continue
                depth += (source[cursor] == '(') - (source[cursor] == ')')
                cursor += 1
            ranges.append((call.end(), cursor))
        changes = []
        for match in STRING.finditer(source):
            value = json.loads(match.group())
            if value not in LABELS or not any(a <= match.start() < b for a, b in ranges):
                continue
            if re.search(r"Translations\.(?:text|tr)\($", source[max(0, match.start() - 25):match.start()]):
                continue
            changes.append((match.start(), match.end(), "Translations.text(" + match.group() + ")"))
        for a, b, replacement in reversed(changes):
            source = source[:a] + replacement + source[b:]
        if changes and "import pizza.dlc.api.translation.Translations;" not in source:
            source = source.replace("\n\n", "\n\nimport pizza.dlc.api.translation.Translations;\n", 1)
        path.write_text(source, encoding="utf-8")
        print(filename, len(changes), "display labels localized")


if __name__ == "__main__":
    main()
