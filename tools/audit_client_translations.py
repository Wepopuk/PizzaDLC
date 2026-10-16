"""Inventory human-readable client labels without changing identifiers."""
import json
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "src/main/java/pizza/dlc"
STR = r'"(?:[^"\\]|\\.)*"'
catalog = json.loads((ROOT / "src/main/resources/assets/pizzadlc/lang/client/en.json").read_text(encoding="utf-8"))["labels"]
groups = {"modules": set(), "settings": set(), "ui": set(), "descriptions": set()}
for path in (JAVA / "client/modules").rglob("*.java"):
    text = path.read_text(encoding="utf-8")
    for name, category in (("name", "modules"), ("description", "descriptions")):
        for match in re.finditer(r'\b' + name + r'\s*=\s*(' + STR + ')', text):
            groups[category].add(json.loads(match[1]))
    # Capture constructor labels, all mode choices and section metadata.
    for match in re.finditer(r'new\s+\w*Setting\s*\(', text):
        cursor, depth = match.end(), 1
        while cursor < len(text) and depth:
            if text[cursor] == '"':
                token = re.match(STR, text[cursor:])
                value = json.loads(token[0])
                if value.strip(): groups["settings"].add(value)
                cursor += len(token[0]); continue
            depth += (text[cursor] == '(') - (text[cursor] == ')'); cursor += 1
    for match in re.finditer(r'\.(?:section|group)\(\s*(' + STR + ')', text): groups["settings"].add(json.loads(match[1]))
    for match in re.finditer(r'\bstatic\s+final\s+String\s+\w+\s*=\s*(' + STR + ')', text):
        value = json.loads(match[1])
        if re.search('[А-Яа-яЁё]', value): groups["settings"].add(value)
for folder in ("api/ui", "api/account", "mixin/client"):
    for path in (JAVA / folder).rglob("*.java"):
        for match in re.finditer(STR, path.read_text(encoding="utf-8")):
            value = json.loads(match[0])
            if re.search('[А-Яа-яЁё]', value) or value in ("Alt Manager", "Item Editor", "Enabled", "Disabled"):
                if not value.startswith(("import ", "package ")): groups["ui"].add(value)
output = ROOT / "build/translation-audit"
output.mkdir(parents=True, exist_ok=True)
for name, values in groups.items():
    missing = sorted(values - catalog.keys())
    (output / f"{name}.txt").write_text("\n".join(missing) + "\n", encoding="utf-8")
    print(name, len(values), "total,", len(missing), "missing")
