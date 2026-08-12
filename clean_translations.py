#!/usr/bin/env python3
import os
import re
import argparse

def main():
    parser = argparse.ArgumentParser(description="Find and clean up hardcoded L10n.lang(\"...\") translation calls in client Java files.")
    parser.add_argument("--dry-run", action="store_true", help="Print the proposed changes without modifying any files.")
    args = parser.parse_args()

    # Determine paths relative to this script
    script_dir = os.path.dirname(os.path.abspath(__file__))
    client_java_dir = os.path.join(script_dir, "src", "client", "java")
    uikeys_path = os.path.join(client_java_dir, "mchorse", "bbs_mod", "ui", "UIKeys.java")
    triggerkeys_path = os.path.join(client_java_dir, "mchorse", "bbs_mod", "ui", "triggers", "TriggerKeys.java")

    if not os.path.exists(client_java_dir):
        print(f"Error: Client java directory not found at: {client_java_dir}")
        print("Please place and run this script in the root directory of the bbs-mod repository.")
        return

    # 1. Parse UIKeys.java for existing mappings
    if not os.path.exists(uikeys_path):
        print(f"Error: UIKeys.java not found at: {uikeys_path}")
        return

    with open(uikeys_path, "r", encoding="utf-8") as f:
        uikeys_content = f.read()

    mapped_keys = {}
    existing_constant_names = set()

    for match in re.finditer(r"public\s+static\s+final\s+IKey\s+([A-Z0-9_]+)\s*=\s*L10n\.lang\(\"([^\"]+)\"\);", uikeys_content):
        var_name, key = match.groups()
        mapped_keys[key] = f"UIKeys.{var_name}"
        existing_constant_names.add(var_name)

    # 2. Parse TriggerKeys.java for existing mappings
    if os.path.exists(triggerkeys_path):
        with open(triggerkeys_path, "r", encoding="utf-8") as f:
            triggerkeys_content = f.read()
        for match in re.finditer(r"public\s+static\s+final\s+IKey\s+([A-Z0-9_]+)\s*=\s*L10n\.lang\(\"([^\"]+)\"\);", triggerkeys_content):
            var_name, key = match.groups()
            mapped_keys[key] = f"TriggerKeys.{var_name}"

    # 3. Scan other client Java files for inline L10n.lang("...")
    pattern = re.compile(r'L10n\.lang\("([^"]+)"\)')
    inline_calls = []

    for root, dirs, files in os.walk(client_java_dir):
        for file in files:
            if not file.endswith(".java"):
                continue
            full_path = os.path.abspath(os.path.join(root, file))
            if full_path.lower() in [os.path.abspath(uikeys_path).lower(), os.path.abspath(triggerkeys_path).lower()]:
                continue
                
            with open(full_path, "r", encoding="utf-8") as f:
                lines = f.readlines()
                
            for line_num, line in enumerate(lines, 1):
                for match in pattern.finditer(line):
                    key = match.group(1)
                    inline_calls.append({
                        "file_path": full_path,
                        "relative_path": os.path.relpath(full_path, client_java_dir),
                        "line": line_num,
                        "key": key,
                        "content": line.strip()
                    })

    if not inline_calls:
        print("No inline L10n.lang(\"...\") calls found! The codebase is clean.")
        return

    print(f"Found {len(inline_calls)} inline translation calls.")

    # 4. Generate proposed replacements and new constants
    new_constants = {}
    file_modifications = {}

    def generate_const_name(key):
        for prefix in ["bbs.ui.", "bbs.", "studio.ui."]:
            if key.startswith(prefix):
                key_part = key[len(prefix):]
                break
        else:
            key_part = key
        return key_part.replace(".", "_").replace(":", "_").replace("-", "_").upper()

    for call in inline_calls:
        key = call["key"]
        file_path = call["file_path"]
        
        if key in mapped_keys:
            replacement = mapped_keys[key]
        else:
            if key not in new_constants:
                const_name = generate_const_name(key)
                # Handle potential duplicate name generation
                original_const_name = const_name
                counter = 1
                while const_name in existing_constant_names or const_name in new_constants.values():
                    const_name = f"{original_const_name}_{counter}"
                    counter += 1
                new_constants[key] = const_name
            replacement = f"UIKeys.{new_constants[key]}"
            
        if file_path not in file_modifications:
            file_modifications[file_path] = {
                "relative_path": call["relative_path"],
                "replacements": []
            }
        file_modifications[file_path]["replacements"].append((key, replacement))

    # 5. Output / Apply changes
    if args.dry_run:
        print("\n--- DRY RUN: Proposed additions to UIKeys.java ---")
        for key, const_name in sorted(new_constants.items()):
            print(f'public static final IKey {const_name} = L10n.lang("{key}");')
            
        print("\n--- DRY RUN: Proposed file refactoring ---")
        for file_path, data in sorted(file_modifications.items()):
            print(f"\nFile: {data['relative_path']}")
            for key, repl in data["replacements"]:
                print(f'  L10n.lang("{key}") -> {repl}')
        print("\nDry run completed. No files were modified.")
        return

    # Real mode: apply changes
    # A. Write new constants to UIKeys.java
    if new_constants:
        insertion_marker = "    /* Key collections */"
        if insertion_marker in uikeys_content:
            new_lines = []
            for key, const_name in sorted(new_constants.items()):
                new_lines.append(f'    public static final IKey {const_name} = L10n.lang("{key}");')
            block_to_insert = "\n".join(new_lines) + "\n\n"
            uikeys_content = uikeys_content.replace(insertion_marker, block_to_insert + insertion_marker)
            
            with open(uikeys_path, "w", encoding="utf-8") as f:
                f.write(uikeys_content)
            print(f"Added {len(new_constants)} new constants to UIKeys.java.")
        else:
            print("Error: Insertion marker '    /* Key collections */' not found in UIKeys.java. Cannot add new constants.")
            return

    # B. Update each client Java file
    for file_path, data in file_modifications.items():
        with open(file_path, "r", encoding="utf-8") as f:
            content = f.read()
            
        new_content = content
        # Sort replacements by key length descending to avoid partial replacements
        sorted_replacements = sorted(data["replacements"], key=lambda x: len(x[0]), reverse=True)
        
        for key, replacement in sorted_replacements:
            new_content = new_content.replace(f'L10n.lang("{key}")', replacement)
            
        # Check and inject imports if needed
        pkg_match = re.search(r"package\s+([a-zA-Z0-9_\.]+);", new_content)
        pkg = pkg_match.group(1) if pkg_match else ""
        
        imports_to_add = []
        if any("UIKeys." in r[1] for r in data["replacements"]):
            if pkg != "mchorse.bbs_mod.ui":
                if "import mchorse.bbs_mod.ui.UIKeys;" not in new_content and "import mchorse.bbs_mod.ui.*;" not in new_content:
                    imports_to_add.append("import mchorse.bbs_mod.ui.UIKeys;")
                    
        if any("TriggerKeys." in r[1] for r in data["replacements"]):
            if pkg != "mchorse.bbs_mod.ui.triggers":
                if "import mchorse.bbs_mod.ui.triggers.TriggerKeys;" not in new_content and "import mchorse.bbs_mod.ui.triggers.*;" not in new_content:
                    imports_to_add.append("import mchorse.bbs_mod.ui.triggers.TriggerKeys;")
                    
        if imports_to_add:
            lines = new_content.splitlines()
            pkg_line_idx = -1
            for idx, line in enumerate(lines):
                if line.strip().startswith("package "):
                    pkg_line_idx = idx
                    break
            
            if pkg_line_idx != -1:
                insert_lines = [f"\n{imp}" for imp in imports_to_add]
                lines = lines[:pkg_line_idx+1] + insert_lines + lines[pkg_line_idx+1:]
                new_content = "\n".join(lines)
                
        with open(file_path, "w", encoding="utf-8") as f:
            f.write(new_content)
        print(f"Refactored {data['relative_path']} ({len(data['replacements'])} replacements)")

    print("\nRefactoring complete! All translation keys are now centralized in UIKeys.java / TriggerKeys.java.")

if __name__ == "__main__":
    main()
