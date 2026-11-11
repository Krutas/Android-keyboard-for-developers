#!/usr/bin/env python3
"""Generate offline Unicode emoji catalog for Android, iOS and simulator."""
import json
import re
import subprocess
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SOURCE = "https://www.unicode.org/Public/emoji/latest/emoji-test.txt"
raw = subprocess.check_output(["curl", "--fail", "--location", "--silent", "--show-error", SOURCE], text=True)
version = re.search(r"^# Version: ([^\s]+)", raw, re.MULTILINE).group(1)
groups = []
current = None
for line in raw.splitlines():
    if line.startswith("# group: "):
        current = {"name": line.split(": ", 1)[1], "items": []}
        groups.append(current)
    elif current is not None and "; fully-qualified" in line:
        codepoints, details = line.split(";", 1)
        if not details.lstrip().startswith("fully-qualified"):
            continue
        tail = details.split("#", 1)[1].strip()
        match = re.search(r"\bE\d+(?:\.\d+)?\s+(.+)$", tail)
        if not match:
            raise ValueError(f"Emoji name missing: {line}")
        glyph = "".join(chr(int(cp, 16)) for cp in codepoints.split())
        current["items"].append([glyph, match.group(1)])
groups = [g for g in groups if g["items"]]
assert len(groups) == 9, [g["name"] for g in groups]
catalog = {"version": version, "source": SOURCE, "groups": groups}
encoded = json.dumps(catalog, ensure_ascii=False, separators=(",", ":")) + "\n"
for target in [ROOT / "app/src/main/assets/emoji.json", ROOT / "ios/DevBoardKeyboard/emoji.json", ROOT / "docs/emoji.json"]:
    target.parent.mkdir(parents=True, exist_ok=True)
    target.write_text(encoded, encoding="utf-8")
print(f"Unicode Emoji {version}: {sum(len(g['items']) for g in groups)} RGI emoji in {len(groups)} groups, {len(encoded.encode())} bytes each")
