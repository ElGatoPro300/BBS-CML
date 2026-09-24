#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
organize_lang.py - Formats, sorts, and organizes JSON translation files for BBS CML EDITION 3.0.

Capabilities:
1. Alphabetically sorts all translation keys in each language/strings JSON file.
2. Formats with standard 4-space indentation and clean UTF-8 encoding (preserving native characters).
3. Works universally across any OS and workspace (resolves project root dynamically without hardcoded paths).
4. Scans both BBS UI strings (`assets/strings`) and Minecraft lang files (`assets/lang`).
5. Supports dry-run and CI check modes.

Usage:
  python tools/organize_lang.py                      # Sort and format all translation files
  python tools/organize_lang.py --dry-run            # Preview changes without modifying files
  python tools/organize_lang.py --check              # Check if files need formatting (exit code 1 if dirty)
  python tools/organize_lang.py <file_or_dir_path>  # Process specific file(s) or folder
"""

import os
import glob
import json
import sys
import argparse

# -----------------------------------------------------------------------------
# Configuration & Universal Path Resolution
# -----------------------------------------------------------------------------

# Locate project root dynamically (whether run from repo root or tools/ folder)
SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
if os.path.exists(os.path.join(SCRIPT_DIR, "src")):
    PROJECT_ROOT = SCRIPT_DIR
else:
    PROJECT_ROOT = os.path.abspath(os.path.join(SCRIPT_DIR, ".."))

# Default directories containing translation JSON files
DEFAULT_LANG_DIRS = [
    os.path.join(PROJECT_ROOT, "src", "client", "resources", "assets", "bbs", "assets", "strings"),
    os.path.join(PROJECT_ROOT, "src", "main", "resources", "assets", "bbs", "lang"),
]

def get_target_files(targets=None):
    """Resolve input targets or default directories to an absolute list of JSON files."""
    resolved = set()

    if targets:
        for t in targets:
            abs_t = os.path.abspath(t)
            if os.path.isdir(abs_t):
                for f in glob.glob(os.path.join(abs_t, "**", "*.json"), recursive=True):
                    resolved.add(os.path.abspath(f))
            elif os.path.isfile(abs_t) and abs_t.endswith(".json"):
                resolved.add(abs_t)
            else:
                for f in glob.glob(t, recursive=True):
                    if f.endswith(".json"):
                        resolved.add(os.path.abspath(f))
    else:
        for d in DEFAULT_LANG_DIRS:
            if os.path.exists(d):
                for f in glob.glob(os.path.join(d, "*.json")):
                    resolved.add(os.path.abspath(f))
            else:
                print(f"Warning: Directory not found: {d}")

    return sorted(list(resolved))

def process_file(filepath, dry_run=False, verbose=False):
    """Sort translation keys alphabetically and reformat JSON cleanly."""
    try:
        with open(filepath, 'r', encoding='utf-8') as f:
            raw_content = f.read()

        # Detect line endings
        eol = "\r\n" if "\r\n" in raw_content else "\n"

        data = json.loads(raw_content)
        if not isinstance(data, dict):
            if verbose:
                print(f"[SKIP] Non-dictionary JSON: {os.path.relpath(filepath, PROJECT_ROOT)}")
            return False

        # Sort keys alphabetically
        sorted_data = dict(sorted(data.items(), key=lambda item: item[0]))

        # Format JSON with 4-space indent and UTF-8 characters preserved
        formatted_json = json.dumps(sorted_data, indent=4, ensure_ascii=False)
        
        # Ensure proper newline formatting and single trailing newline
        formatted_json_lines = formatted_json.splitlines()
        final_text = eol.join(formatted_json_lines) + eol

        rel_path = os.path.relpath(filepath, PROJECT_ROOT)

        if final_text != raw_content:
            if not dry_run:
                with open(filepath, 'w', encoding='utf-8', newline='') as f:
                    f.write(final_text)
                print(f"[SORTED] {rel_path}")
            else:
                print(f"[NEEDS SORTING] {rel_path}")
            return True
        elif verbose:
            print(f"[OK] {rel_path}")

        return False

    except Exception as e:
        print(f"[ERROR] processing {filepath}: {e}", file=sys.stderr)
        return False

def check_parity(targets=None, verbose=False):
    """Check that all language files in each directory have 100% key parity with en_us.json."""
    parity_ok = True
    dirs_to_check = []

    if targets:
        for t in targets:
            abs_t = os.path.abspath(t)
            if os.path.isdir(abs_t):
                dirs_to_check.append(abs_t)
            elif os.path.isfile(abs_t):
                d = os.path.dirname(abs_t)
                if d not in dirs_to_check:
                    dirs_to_check.append(d)
    else:
        dirs_to_check = [d for d in DEFAULT_LANG_DIRS if os.path.exists(d)]

    print("\n--- Language Key Parity Check ---")
    for d in dirs_to_check:
        rel_dir = os.path.relpath(d, PROJECT_ROOT)
        en_path = os.path.join(d, "en_us.json")
        if not os.path.exists(en_path):
            if verbose:
                print(f"[SKIP] No en_us.json in {rel_dir}")
            continue

        with open(en_path, 'r', encoding='utf-8') as f:
            en_data = json.load(f)
        en_keys = set(en_data.keys())

        print(f"\nComparing with {rel_dir}/en_us.json ({len(en_keys)} keys):")
        dir_files = sorted(glob.glob(os.path.join(d, "*.json")))

        for fp in dir_files:
            fname = os.path.basename(fp)
            if fname == "en_us.json":
                continue
            with open(fp, 'r', encoding='utf-8') as f:
                data = json.load(f)
            file_keys = set(data.keys())
            missing = en_keys - file_keys
            extra = file_keys - en_keys

            if not missing and not extra:
                if verbose:
                    print(f"  [OK] {fname}: 100% parity ({len(file_keys)} keys)")
            else:
                parity_ok = False
                print(f"  [PARITY MISMATCH] {fname}:")
                if missing:
                    print(f"    Missing ({len(missing)}): {', '.join(sorted(missing)[:10])}{'...' if len(missing) > 10 else ''}")
                if extra:
                    print(f"    Extra ({len(extra)}): {', '.join(sorted(extra)[:10])}{'...' if len(extra) > 10 else ''}")

    if parity_ok:
        print("\nAll language files are in 100% parity with en_us.json!")
    else:
        print("\nWarning: Some language files have missing or extra keys compared to en_us.json.")

    return parity_ok

def main():
    parser = argparse.ArgumentParser(
        description="Sort, format, and verify parity of JSON translation files for BBS CML EDITION 3.0."
    )
    parser.add_argument(
        "targets",
        nargs="*",
        help="Optional JSON files or directories to process. Defaults to BBS strings and lang folders."
    )
    parser.add_argument(
        "--dry-run",
        action="store_true",
        help="Check files without modifying them."
    )
    parser.add_argument(
        "--check",
        action="store_true",
        help="Exit with code 1 if any file is unformatted or unsorted (CI mode)."
    )
    parser.add_argument(
        "--parity",
        action="store_true",
        help="Check key parity across all language files against en_us.json."
    )
    parser.add_argument(
        "-v", "--verbose",
        action="store_true",
        help="Print status for all scanned files."
    )

    args = parser.parse_args()
    dry_run = args.dry_run or args.check

    files = get_target_files(args.targets)
    if not files:
        print("No translation JSON files found to process.")
        return 0

    mode_label = " (dry run)" if dry_run else ""
    print(f"Scanning {len(files)} translation files{mode_label}...")

    modified_count = 0
    for f in files:
        if process_file(f, dry_run=dry_run, verbose=args.verbose):
            modified_count += 1

    print(f"\nDone. {modified_count}/{len(files)} files {'need sorting' if dry_run else 'updated'}.")

    parity_ok = True
    if args.parity or args.check:
        parity_ok = check_parity(args.targets, verbose=args.verbose)

    if (args.check and modified_count > 0) or (args.parity and not parity_ok):
        sys.exit(1)

    return 0

if __name__ == "__main__":
    main()
