"""Migrate fixed UI copy only. Usernames, addresses, passwords and protocol tokens are untouched."""
import json
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "src/main/java/pizza/dlc"
CATALOG = json.loads((ROOT / "src/main/resources/assets/pizzadlc/lang/client/en.json").read_text(encoding="utf-8"))["labels"]
STR = re.compile(r'"(?:[^"\\]|\\.)*"')
paths = list((JAVA / "api/ui").rglob("*.java"))
paths += [JAVA / path for path in ("api/account/MicrosoftAuth.java", "api/account/AccountSwitcher.java",
    "api/account/ServerAutoLogin.java", "api/account/ServerLoginStore.java",
    "mixin/client/ChatScreenCopyMixin.java", "mixin/client/ConnectScreenMixin.java")]
for path in paths:
    if not path.exists() or path.name in ("LanguageSelectScreen.java", "ModuleSearch.java"):
        continue
    source = path.read_text(encoding="utf-8")
    edits = []
    for match in STR.finditer(source):
        value = json.loads(match[0])
        if value not in CATALOG or not (re.search('[А-Яа-яЁё]', value) or value in ("Alt Manager", "Item Editor")):
            continue
        if re.search(r'Translations\.(?:text|tr)\(\s*$', source[max(0, match.start()-35):match.start()]):
            continue
        # Do not modify text quoted in a line comment.
        before = source[source.rfind('\n', 0, match.start())+1:match.start()]
        if before.lstrip().startswith(("//", "*")):
            continue
        if path.name == "SettingsNav.java" or path.name == "MainMenuScreen.java" and match.start() > source.index("private static enum Nav"):
            continue
        edits.append((match.start(), match.end(), "Translations.text(" + match[0] + ")"))
    for start, end, value in reversed(edits):
        source = source[:start] + value + source[end:]
    if edits:
        if "import pizza.dlc.api.translation.Translations;" not in source:
            source = source.replace("\n\n", "\n\nimport pizza.dlc.api.translation.Translations;\n", 1)
        path.write_text(source, encoding="utf-8")
        print(path.relative_to(JAVA), len(edits), "fixed UI labels localized")
