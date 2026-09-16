#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
organize_imports.py - Formats, cleans, and organizes Java imports for BBS CML EDITION 3.0.

Capabilities:
1. Reorganizes and sorts imports into logical groups with blank line separators.
2. Converts Fully Qualified Names (FQNs) in the Java code body into clean imports and short class names.
3. Automatically detects and removes dead/unused imports (imports not referenced in the code body).
4. Respects WHITELISTed classes to prevent ambiguous name collisions (e.g. java.lang.Math vs org.joml.Math).
5. Preserves string literals, character literals, and comments (line comments, block comments, javadoc).
6. Avoids redundant imports for java.lang.* and same-package classes.
7. Prevents name collision if two different packages share the same simple class name.
8. Preserves file headers/license comments and native line endings.

Usage:
  python tools/organize_imports.py                      # Process all Java files in src/
  python tools/organize_imports.py --dry-run            # Preview changes without modifying files
  python tools/organize_imports.py --check              # Check if files need formatting (exit code 1 if dirty)
  python tools/organize_imports.py <file_or_dir_path>  # Process specific file(s) or folder
"""

import os
import re
import glob
import sys
import argparse

# -----------------------------------------------------------------------------
# Configuration
# -----------------------------------------------------------------------------

# Locate project root (handle execution from repo root or tools/ directory)
SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
if os.path.exists(os.path.join(SCRIPT_DIR, "src")):
    PROJECT_ROOT = SCRIPT_DIR
else:
    PROJECT_ROOT = os.path.abspath(os.path.join(SCRIPT_DIR, ".."))
DEFAULT_JAVA_SRC_DIR = os.path.join(PROJECT_ROOT, "src")

# Import groups order
IMPORT_GROUPS = [
    # BBS Mod / Local Project
    "mchorse.bbs_mod",
    "mchorse",
    # Fabric
    "net.fabricmc",
    # Minecraft
    "net.minecraft",
    # Iris & Sodium
    "net.irisshaders",
    "net.caffeinemc",
    "me.jellysquid",
    # Mixin
    "org.spongepowered",
    "com.llamalad7",
    # Math & Mojang & Libraries
    "org.joml",
    "com.mojang",
    "com.google",
    "org.lwjgl",
    "io.netty",
    "org.slf4j",
    "org.apache",
    "org.jetbrains",
    "org.objectweb",
    "it.unimi",
    "com.ibm",
    "joptsimple",
    # Standard Java / JVM
    "java",
    "javax",
    "sun",
    # Dev / Third-party
    "dev",
]

# Classes that should remain fully qualified to avoid ambiguity.
# If these are found as FQNs in code, they will NOT be simplified.
# If they are already imported, they will be kept.
WHITELIST = {
    "java.lang.Math",
    "net.minecraft.world.phys.AABB",
}

# Regex for matching:
# Group 1: Character literals, string literals, block comments, and line comments (to be preserved untouched)
# Group 2: Fully qualified class names
FQN_REGEX = re.compile(
    r"('(?:\\.|[^'\\])*'"
    r'|"(?:\\.|[^"\\])*"'
    r"|/\*[\s\S]*?\*/"
    r"|//[^\r\n]*)"
    r"|\b((?:mchorse|net|com|org|java|javax|sun|dev|io|me|joptsimple|it)\.(?:[a-z0-9_]+\.)+[A-Z][a-zA-Z0-9_]*)\b"
)

# Stripper for literals and comments to check actual code usage
STRIP_LITERALS_COMMENTS = re.compile(
    r"('(?:\\.|[^'\\])*'"
    r'|"(?:\\.|[^"\\])*"'
    r"|/\*[\s\S]*?\*/"
    r"|//[^\r\n]*)"
)

# Standard Java lang single-depth classes that don't need imports
JAVA_LANG_CLASSES = {
    "Appendable", "AutoCloseable", "CharSequence", "Cloneable", "Comparable", "Iterable", "Readable", "Runnable",
    "Boolean", "Byte", "Character", "Class", "ClassLoader", "ClassValue", "Compiler", "Double", "Enum", "Float",
    "InheritableThreadLocal", "Integer", "Long", "Math", "Number", "Object", "Package", "Process", "ProcessBuilder",
    "Record", "Runtime", "RuntimePermission", "SecurityManager", "Short", "StackTraceElement", "StrictMath", "String",
    "StringBuffer", "StringBuilder", "System", "Thread", "ThreadGroup", "ThreadLocal", "Throwable", "Void",
    "Exception", "RuntimeException", "Error", "IllegalArgumentException", "IllegalStateException",
    "NullPointerException", "IndexOutOfBoundsException", "UnsupportedOperationException"
}

def get_target_files(targets=None):
    """Resolve input targets to an absolute list of Java files."""
    if not targets:
        search_dir = DEFAULT_JAVA_SRC_DIR
        if not os.path.exists(search_dir):
            print(f"Warning: Default source directory not found: {search_dir}")
            return []
        return sorted([os.path.abspath(f) for f in glob.glob(os.path.join(search_dir, "**", "*.java"), recursive=True)])

    resolved = set()
    for t in targets:
        abs_t = os.path.abspath(t)
        if os.path.isdir(abs_t):
            for f in glob.glob(os.path.join(abs_t, "**", "*.java"), recursive=True):
                resolved.add(os.path.abspath(f))
        elif os.path.isfile(abs_t) and abs_t.endswith(".java"):
            resolved.add(abs_t)
        else:
            # Handle glob patterns if passed
            for f in glob.glob(t, recursive=True):
                if f.endswith(".java"):
                    resolved.add(os.path.abspath(f))
    return sorted(list(resolved))

def sort_imports(imports):
    """Sort imports into static and grouped categories with proper spacing."""
    if not imports:
        return []

    # Clean and deduplicate imports
    cleaned = set()
    for imp in imports:
        s = imp.strip().rstrip(';').strip()
        if s:
            cleaned.add(s)

    static_imports = sorted([i for i in cleaned if i.startswith("import static ")])
    normal_imports = [i for i in cleaned if not i.startswith("import static ")]

    grouped = {group: [] for group in IMPORT_GROUPS}
    others = []

    for imp in normal_imports:
        # Determine package name without 'import '
        pkg_part = imp[len("import "):].strip()
        matched = False
        for group in IMPORT_GROUPS:
            if pkg_part == group or pkg_part.startswith(group + "."):
                grouped[group].append(imp)
                matched = True
                break
        if not matched:
            others.append(imp)

    result = []

    # 1. Static imports first
    if static_imports:
        result.extend(static_imports)

    # 2. Grouped normal imports
    for group in IMPORT_GROUPS:
        if grouped[group]:
            if result:
                result.append("")  # Empty line separator between groups
            result.extend(sorted(grouped[group]))

    # 3. Unmatched / other imports
    if others:
        if result:
            result.append("")
        result.extend(sorted(others))

    return result

def process_file(filepath, dry_run=False, verbose=False):
    """Process a single Java file: simplify FQNs and organize imports."""
    try:
        with open(filepath, 'r', encoding='utf-8') as f:
            content = f.read()

        # Detect line ending
        crlf = "\r\n" in content
        eol = "\r\n" if crlf else "\n"
        lines = content.splitlines()

        # Extract file components
        header_lines = []
        package_line = ""
        current_package = ""
        imports = []
        body_lines = []

        in_header = True
        in_imports = False
        finished_imports = False

        for line in lines:
            trimmed = line.strip()

            if in_header and not package_line and not trimmed.startswith("import ") and not trimmed.startswith("package "):
                if trimmed:
                    header_lines.append(line)
                elif header_lines:
                    header_lines.append(line)
                continue

            if trimmed.startswith("package "):
                in_header = False
                package_line = trimmed.rstrip(';')
                current_package = trimmed[len("package "):].rstrip(';').strip()
            elif trimmed.startswith("import "):
                in_header = False
                in_imports = True
                imports.append(trimmed.rstrip(';'))
            elif in_imports and not trimmed and not finished_imports:
                continue  # Skip existing spacers inside import block
            elif in_imports and not trimmed.startswith("import ") and trimmed:
                finished_imports = True
                body_lines.append(line)
            elif finished_imports or (not in_imports and trimmed and not trimmed.startswith("package ")):
                in_header = False
                finished_imports = True
                body_lines.append(line)

        body = eol.join(body_lines)

        # Primary class name for this file (to avoid collisions)
        file_class_name = os.path.splitext(os.path.basename(filepath))[0]

        # Map existing simple names to their imported FQN
        simple_to_fqn = {}
        for imp in imports:
            if imp.startswith("import static "):
                continue
            fqn = imp[len("import "):].strip()
            simple = fqn.split('.')[-1]
            simple_to_fqn[simple] = fqn

        # Replace FQNs in body
        def replace_fqn(match):
            literal_or_comment = match.group(1)
            fqn = match.group(2)

            if literal_or_comment is not None:
                return literal_or_comment

            # If FQN is whitelisted, keep as FQN
            if fqn in WHITELIST:
                return fqn

            parts = fqn.split('.')
            class_name = parts[-1]
            pkg_name = ".".join(parts[:-1])

            # If class belongs to java.lang (single depth e.g. java.lang.Math)
            if pkg_name == "java.lang" and class_name in JAVA_LANG_CLASSES:
                return class_name

            # If class belongs to the same package as current file, no import needed
            if pkg_name == current_package:
                return class_name

            # If the simple name is already imported from a different package, don't simplify
            if class_name in simple_to_fqn and simple_to_fqn[class_name] != fqn:
                return fqn

            # If the simple name conflicts with this file's main class name, don't simplify
            if class_name == file_class_name and pkg_name != current_package:
                return fqn

            # Register import and use simplified class name
            simple_to_fqn[class_name] = fqn
            imports.append(f"import {fqn}")
            return class_name

        new_body = FQN_REGEX.sub(replace_fqn, body)

        # Strip string literals, char literals, and comments to check actual code usage
        code_only_body = STRIP_LITERALS_COMMENTS.sub(" ", new_body)

        # Filter redundant and unused/dead imports
        filtered_imports = []
        for imp in imports:
            s_imp = imp.strip().rstrip(';').strip()
            if not s_imp:
                continue

            if s_imp.startswith("import static "):
                static_target = s_imp[len("import static "):].strip()
                static_member = static_target.split('.')[-1]
                # Wildcard static imports are kept
                if static_member == "*":
                    filtered_imports.append(s_imp)
                    continue
                # Check if static member is used in code
                if re.search(r'\b' + re.escape(static_member) + r'\b', code_only_body):
                    filtered_imports.append(s_imp)
                continue

            fqn = s_imp[len("import "):].strip()
            parts = fqn.split('.')
            class_name = parts[-1]
            pkg_name = ".".join(parts[:-1])

            # Whitelisted imports are always kept
            if fqn in WHITELIST:
                filtered_imports.append(s_imp)
                continue

            # Wildcard imports are kept
            if class_name == "*":
                filtered_imports.append(s_imp)
                continue

            # Redundant: same package
            if pkg_name == current_package:
                continue

            # Redundant: java.lang single-depth classes
            if pkg_name == "java.lang" and class_name in JAVA_LANG_CLASSES:
                continue

            # Check if class is actually used in code
            if re.search(r'\b' + re.escape(class_name) + r'\b', code_only_body):
                filtered_imports.append(s_imp)

        # Sort imports
        sorted_imps = sort_imports(filtered_imports)

        # Construct final file content
        new_content_parts = []

        if header_lines:
            header_text = eol.join(header_lines).strip()
            if header_text:
                new_content_parts.append(header_text + eol + eol)

        if package_line:
            new_content_parts.append(package_line + ";" + eol + eol)

        for imp in sorted_imps:
            if imp == "":
                new_content_parts.append(eol)
            else:
                new_content_parts.append(imp + ";" + eol)

        if sorted_imps:
            new_content_parts.append(eol)

        new_content_parts.append(new_body.lstrip())

        final_text = "".join(new_content_parts)
        # Ensure single trailing newline
        final_text = final_text.rstrip() + eol

        if final_text != content:
            rel_path = os.path.relpath(filepath, PROJECT_ROOT)
            if not dry_run:
                with open(filepath, 'w', encoding='utf-8') as f:
                    f.write(final_text)
                print(f"[MODIFIED] {rel_path}")
            else:
                print(f"[NEEDS UPDATE] {rel_path}")
            return True
        elif verbose:
            rel_path = os.path.relpath(filepath, PROJECT_ROOT)
            print(f"[OK] {rel_path}")

        return False

    except Exception as e:
        print(f"[ERROR] processing {filepath}: {e}", file=sys.stderr)
        return False

def main():
    parser = argparse.ArgumentParser(
        description="Clean, format, and organize imports for BBS CML EDITION 3.0 Java files."
    )
    parser.add_argument(
        "targets",
        nargs="*",
        help="Optional Java files or directories to process. Defaults to src/."
    )
    parser.add_argument(
        "--dry-run",
        action="store_true",
        help="Check files without modifying them."
    )
    parser.add_argument(
        "--check",
        action="store_true",
        help="Exit with code 1 if any file needs formatting (CI mode)."
    )
    parser.add_argument(
        "-v", "--verbose",
        action="store_true",
        help="Print detailed status for all scanned files."
    )

    args = parser.parse_args()
    dry_run = args.dry_run or args.check

    files = get_target_files(args.targets)
    if not files:
        print("No Java files found to process.")
        return 0

    mode_label = " (dry run)" if dry_run else ""
    print(f"Scanning {len(files)} Java files{mode_label}...")

    modified_count = 0
    for f in files:
        if process_file(f, dry_run=dry_run, verbose=args.verbose):
            modified_count += 1

    print(f"\nDone. {modified_count}/{len(files)} files {'need organizing' if dry_run else 'updated'}.")

    if args.check and modified_count > 0:
        sys.exit(1)

    return 0

if __name__ == "__main__":
    main()
