import json
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parent
STRINGS_DIR = ROOT / "src/client/resources/assets/bbs/assets/strings"
EXPORT_DIR = ROOT / "run/export"
TARGET = STRINGS_DIR / "es_es.json"
EN_US = STRINGS_DIR / "en_us.json"

EXPORT_EN = EXPORT_DIR / "lang.assets_strings.en_us.json"
EXPORT_ES = EXPORT_DIR / "lang.assets_strings.es_es.json"

LINE_PATTERN = re.compile(r'^(\s*)"((?:\\.|[^"\\])*)":\s*"((?:\\.|[^"\\])*)"\s*,?\s*$')


def load_json(path: Path) -> dict:
    return json.loads(path.read_text(encoding="utf-8"))


def escape_json_string(value: str) -> str:
    return json.dumps(value, ensure_ascii=False)[1:-1]


def collect_updates() -> dict:
    current = load_json(TARGET)
    en = load_json(EN_US)
    export_en = load_json(EXPORT_EN)
    export_es = load_json(EXPORT_ES)

    desired = {}

    for key, value in export_en.items():
        if en.get(key) != value:
            desired[key] = value

    for key, value in export_es.items():
        desired[key] = value

    return {key: value for key, value in desired.items() if current.get(key) != value}


def merge_preserving_order(updates: dict) -> tuple[int, int]:
    en = load_json(EN_US)
    text = TARGET.read_text(encoding="utf-8")
    lines = text.splitlines()
    out = []
    updated = 0

    for line in lines:
        match = LINE_PATTERN.match(line)
        if match and match.group(2) in updates:
            indent, key = match.group(1), match.group(2)
            value = escape_json_string(updates[key])
            out.append(f'{indent}"{key}": "{value}",')
            del updates[key]
            updated += 1
        else:
            out.append(line)

    order = [key for key in en if key in updates]
    order.extend(sorted(key for key in updates if key not in order))

    added = 0
    for key in order:
        if key not in updates:
            continue

        entry = f'    "{key}": "{escape_json_string(updates[key])}",'
        inserted = False

        for index, line in enumerate(out):
            match = LINE_PATTERN.match(line)
            if match and match.group(2) > key:
                out.insert(index, entry)
                inserted = True
                added += 1
                break

        if not inserted:
            for index in range(len(out) - 1, -1, -1):
                if out[index].strip() == "}":
                    out.insert(index, entry)
                    added += 1
                    break

        del updates[key]

    result = "\n".join(out)
    if not result.endswith("\n"):
        result += "\n"

    TARGET.write_text(result, encoding="utf-8")
    return updated, added


def main() -> None:
    if not EXPORT_EN.is_file():
        raise SystemExit(f"Missing export file: {EXPORT_EN}")

    if not EXPORT_ES.is_file():
        raise SystemExit(f"Missing export file: {EXPORT_ES}")

    updates = collect_updates()

    if not updates:
        print(f"No changes needed for {TARGET.relative_to(ROOT)}")
        return

    updated, added = merge_preserving_order(updates)

    print(f"Merged into {TARGET.relative_to(ROOT)}")
    print(f"Updated existing keys: {updated}")
    print(f"Added new keys: {added}")
    print(f"Total changes: {updated + added}")


if __name__ == "__main__":
    main()
