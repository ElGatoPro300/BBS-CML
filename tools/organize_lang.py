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

def main():
    parser = argparse.ArgumentParser(
        description="Sort and format JSON translation files for BBS CML EDITION 3.0."
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

    if args.check and modified_count > 0:
        sys.exit(1)

    return 0

if __name__ == "__main__":
    main()
