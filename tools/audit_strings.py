#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
audit_strings.py - Comprehensive scanner for all kinds of hardcoded user-facing strings,
literal IKey.raw/str usages, unlocalized UI labels/tooltips, and key cross-referencing.
"""

import os
import sys
import glob
import re
import json

if sys.platform == "win32":
    try:
        sys.stdout.reconfigure(encoding="utf-8")
        sys.stderr.reconfigure(encoding="utf-8")
    except Exception:
        pass

PROJECT_ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
CLIENT_SRC = os.path.join(PROJECT_ROOT, "src", "client", "java")
MAIN_SRC = os.path.join(PROJECT_ROOT, "src", "main", "java")
STRINGS_EN = os.path.join(PROJECT_ROOT, "src", "client", "resources", "assets", "bbs", "assets", "strings", "en_us.json")
LANG_EN = os.path.join(PROJECT_ROOT, "src", "main", "resources", "assets", "bbs", "lang", "en_us.json")

def is_untranslated_text(val):
    s = val.strip()
    if not s:
        return False
    if re.match(r'^[\W\d_]+$', s):
        return False
    if s.startswith("http://") or s.startswith("https://"):
        return False
    if s.startswith("bbs.") or s.startswith("key.") or s.startswith("item.") or s.startswith("block.") or s.startswith("itemGroup.") or s.startswith("gamerule.") or s.startswith("category."):
        return False
    if re.match(r'^[a-z0-9_]+$', s) and len(s) <= 8 and ' ' not in s:
        return False
    if s.startswith(".") or s in ("png", "wav", "mp4", "json", "nbt", "obj", "vox", "true", "false", "null"):
        return False
    if re.match(r'^(?:%[0-9]*[a-zA-Z]|\s|[\+\-\*/:,\.])*$', s):
        return False
    if re.search(r'[a-zA-Z]{3,}', s):
        return True
    return False

def check_all():
    java_files = glob.glob(os.path.join(CLIENT_SRC, "**", "*.java"), recursive=True) + \
                 glob.glob(os.path.join(MAIN_SRC, "**", "*.java"), recursive=True)

    with open(STRINGS_EN, "r", encoding="utf-8") as f:
        strings_en = json.load(f)
    with open(LANG_EN, "r", encoding="utf-8") as f:
        lang_en = json.load(f)

    all_json_keys = set(strings_en.keys()) | set(lang_en.keys())

    L10N_LANG = re.compile(r'L10n\.lang\(\s*"([^"]+)"\s*\)')
    IKEY_KEY = re.compile(r'IKey\.key\(\s*"([^"]+)"\s*\)')
    RAW_CALL = re.compile(r'(?:L10n|IKey)\.(?:raw|str)\(\s*"([^"]+)"\s*\)')
    TEXT_LITERAL = re.compile(r'Text\.(?:literal|of)\(\s*"([^"]+)"\s*\)')
    UI_METHOD_STRING = re.compile(r'\b(?:addNotification|notify|sendMessage|tooltip|label|title|action|context)\s*\([^)]*\"([^\"]+)\"')

    code_keys = []
    raw_calls = []
    text_literals = []
    ui_string_calls = []

    for f in sorted(java_files):
        with open(f, "r", encoding="utf-8") as fp:
            try:
                lines = fp.readlines()
            except Exception:
                continue

        for idx, line in enumerate(lines, 1):
            s = line.strip()
            if s.startswith("//") or s.startswith("*") or s.startswith("/*"):
                continue

            for m in L10N_LANG.finditer(line):
                code_keys.append((m.group(1), f, idx, s))
            for m in IKEY_KEY.finditer(line):
                code_keys.append((m.group(1), f, idx, s))

            for m in RAW_CALL.finditer(line):
                val = m.group(1).strip()
                if is_untranslated_text(val):
                    raw_calls.append((val, f, idx, s))

            for m in TEXT_LITERAL.finditer(line):
                val = m.group(1).strip()
                if is_untranslated_text(val):
                    text_literals.append((val, f, idx, s))

            for m in UI_METHOD_STRING.finditer(line):
                val = m.group(1).strip()
                if is_untranslated_text(val):
                    # Filter out logger calls or internal identifiers
                    if "LOGGER" not in s and "System.out" not in s:
                        ui_string_calls.append((val, f, idx, s))

    missing_in_json = []
    for k, f, idx, s in code_keys:
        if k not in all_json_keys:
            missing_in_json.append((k, f, idx, s))

    print(f"=======================================================")
    print(f"1. AUDIT REPORT: Code Localization Keys vs JSON Keys")
    print(f"=======================================================")
    print(f"Total L10n.lang / IKey.key references in Java: {len(code_keys)}")
    print(f"Unique keys referenced in code: {len({k for k, _, _, _ in code_keys})}")
    print(f"Total keys in strings/en_us.json: {len(strings_en)}")
    print(f"Total keys in lang/en_us.json: {len(lang_en)}")
    print(f"Keys referenced in code but MISSING in JSON: {len(missing_in_json)}")
    for k, f, idx, s in missing_in_json:
        rel = os.path.relpath(f, PROJECT_ROOT)
        print(f"  [MISSING] {rel}:{idx} -> '{k}'")

    print(f"\n=======================================================")
    print(f"2. AUDIT REPORT: Hardcoded IKey/L10n.raw/str English Text")
    print(f"=======================================================")
    print(f"Total raw text calls found: {len(raw_calls)}")
    for val, f, idx, s in raw_calls:
        rel = os.path.relpath(f, PROJECT_ROOT)
        print(f"  [RAW] {rel}:{idx} -> \"{val}\" (Line: {s})")

    print(f"\n=======================================================")
    print(f"3. AUDIT REPORT: Hardcoded Text.literal/of English Text")
    print(f"=======================================================")
    print(f"Total Text.literal/of calls found: {len(text_literals)}")
    for val, f, idx, s in text_literals:
        rel = os.path.relpath(f, PROJECT_ROOT)
        print(f"  [TEXT] {rel}:{idx} -> \"{val}\" (Line: {s})")

    print(f"\n=======================================================")
    print(f"4. AUDIT REPORT: UI Methods with Hardcoded Strings")
    print(f"=======================================================")
    print(f"Total UI method hardcoded calls found: {len(ui_string_calls)}")
    for val, f, idx, s in ui_string_calls:
        rel = os.path.relpath(f, PROJECT_ROOT)
        print(f"  [UI METHOD] {rel}:{idx} -> \"{val}\" (Line: {s})")

if __name__ == "__main__":
    check_all()
