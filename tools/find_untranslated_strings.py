#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
find_untranslated_strings.py - Detects keys in translation files where the value
is still in English (identical to en_us.json or contains untranslated English text).
"""

import os
import sys
import json
import glob
import re

if sys.platform == "win32":
    try:
        sys.stdout.reconfigure(encoding="utf-8")
        sys.stderr.reconfigure(encoding="utf-8")
    except Exception:
        pass

PROJECT_ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
STRINGS_DIR = os.path.join(PROJECT_ROOT, "src", "client", "resources", "assets", "bbs", "assets", "strings")
LANG_DIR = os.path.join(PROJECT_ROOT, "src", "main", "resources", "assets", "bbs", "lang")

# Common words/acronyms that are naturally the same across languages
NATURAL_SAME = {
    "FPS", "FOV", "PNG", "WAV", "MP4", "NBT", "URL", "ID", "UUID", "RGB", "RGBA", "JSON", "VOX",
    "GUI", "BBS", "CML", "GIF", "HD", "2D", "3D", "IK", "VAO", "VBO", "FBO", "API",
    "BBS mod", "Minecraft", "Discord", "YouTube", "GitHub", "Wiki", "CDN",
    "1080p (1920x1080 - 60 FPS)", "1440p (2560x1440 - 60 FPS)", "4K (3840x2160 - 60 FPS)", "720p (1280x720 - 60 FPS)",
}

def is_natural_same(text):
    s = text.strip()
    if not s:
        return True
    if s in NATURAL_SAME:
        return True
    if s.startswith("http://") or s.startswith("https://"):
        return True
    if re.match(r'^[A-Z0-9_\-]+$', s) and len(s) <= 8:
        return True
    if re.match(r'^(?:%[0-9]*[a-zA-Z]|\s|[\+\-\*/:,\._%#|><=\(\)])*$', s):
        return True
    if re.match(r'^\d+p\s*\(', s):
        return True
    return False

def scan_directory(directory):
    ref_file = os.path.join(directory, "en_us.json")
    if not os.path.exists(ref_file):
        return

    with open(ref_file, "r", encoding="utf-8") as f:
        ref_data = json.load(f)

    print(f"\n=======================================================")
    print(f"Scanning directory: {os.path.relpath(directory, PROJECT_ROOT)}")
    print(f"=======================================================")

    total_untranslated_all = 0

    for lang_file in sorted(glob.glob(os.path.join(directory, "*.json"))):
        base = os.path.basename(lang_file)
        if base == "en_us.json":
            continue

        with open(lang_file, "r", encoding="utf-8") as f:
            lang_data = json.load(f)

        untranslated = []
        for k, v in lang_data.items():
            en_val = ref_data.get(k, "")
            if not en_val:
                continue

            if v.strip() == en_val.strip() and not is_natural_same(en_val):
                untranslated.append((k, en_val))

        total_untranslated_all += len(untranslated)
        print(f"[{base}] Found {len(untranslated)} untranslated / identical English strings")
        
        # Display sample for es_es or others if <= 20
        if base == "es_es.json" or (len(untranslated) > 0 and len(untranslated) <= 15):
            for k, val in untranslated[:30]:
                print(f"   -> {k}: \"{val}\"")
            if len(untranslated) > 30:
                print(f"   ... and {len(untranslated) - 30} more")

    print(f"\nTotal untranslated strings across directory: {total_untranslated_all}")

def main():
    scan_directory(STRINGS_DIR)
    scan_directory(LANG_DIR)

if __name__ == "__main__":
    main()
