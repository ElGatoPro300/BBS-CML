import os
import re
import glob
import subprocess
import sys

# -----------------------------------------------------------------------------
# Console Command: python organize_imports.py
# -----------------------------------------------------------------------------

# Configuration
# Use the directory where the script is located as the project root
PROJECT_ROOT = os.path.dirname(os.path.abspath(__file__))
JAVA_SRC_DIR = os.path.join(PROJECT_ROOT, "src")

# Import groups order
IMPORT_GROUPS = [
    "mchorse.bbs_mod",
    "net.fabricmc",
    "net.minecraft",
    "net.irisshaders",
    "net.caffeinemc",
    "org.joml",
    "com.mojang",
    "com.google",
    "org.lwjgl",
    "java",
    "javax",
    "dev"
]

# Classes that should remain fully qualified to avoid ambiguity
# If these are found as FQNs in code, they will NOT be simplified.
# If they are already imported, they will be kept in imports.
WHITELIST = [
    "mchorse.bbs_mod.camera.Camera",
    "net.minecraft.client.render.Camera",
    "mchorse.bbs_mod.graphics.window.Window",
    "net.minecraft.client.util.Window",
    "mchorse.bbs_mod.graphics.Framebuffer",
    "net.minecraft.client.gl.Framebuffer",
    "mchorse.bbs_mod.utils.colors.Color",
    "java.awt.Color",
    "java.util.Random",
    "net.minecraftforge.common.MinecraftForge",
    # Iris API / Core conflicts
    "net.irisshaders.iris.api.v0.Iris",
    "net.irisshaders.iris.Iris",
    "net.irisshaders.iris.api.v0.ShaderPack",
    "net.irisshaders.iris.shaderpack.ShaderPack",
    "net.irisshaders.iris.api.v0.CustomUniforms",
    "net.irisshaders.iris.uniforms.custom.CustomUniforms",
    "net.irisshaders.iris.pbr.PBRType",
    "net.irisshaders.iris.pbr.texture.PBRType",
    "net.irisshaders.iris.texture.pbr.PBRType",
    "net.irisshaders.iris.pbr.TextureTracker",
    "net.irisshaders.iris.texture.TextureTracker",
    "net.irisshaders.iris.texture.tracking.TextureTracker",
    "net.irisshaders.iris.pbr.loader.PBRTextureLoader",
    "net.irisshaders.iris.texture.pbr.loader.PBRTextureLoader",
    "net.irisshaders.iris.pbr.loader.PBRTextureLoaderRegistry",
    "net.irisshaders.iris.texture.pbr.loader.PBRTextureLoaderRegistry",
    "net.irisshaders.iris.targets.backed.NativeImageBackedSingleColorTexture",
    "net.irisshaders.iris.compat.sodium.impl.vertex_format.entity_xhfp.EntityVertex"
]

# Regex for finding fully qualified names (FQNs)
# Group 1: String literals (to be ignored)
# Group 2: FQNs
FQN_REGEX = r'("(?:\\.|[^"\\])*")|\b((?:mchorse|net|com|org|java|javax|dev|joptsimple)\.(?:[a-z0-9_]+\.)+[A-Z][a-zA-Z0-9_]*)\b'

def get_all_java_files():
    """Get all Java files in the project's src directory."""
    print(f"Scanning for Java files in {JAVA_SRC_DIR}...")
    files = glob.glob(os.path.join(JAVA_SRC_DIR, "**", "*.java"), recursive=True)
    return [os.path.abspath(f) for f in files]

def sort_imports(imports):
    """Sort imports into groups and alphabetically."""
    if not imports:
        return []

    # Remove duplicates and clean up
    imports = sorted(list(set(i.strip().rstrip(';') for i in imports if i.strip())))
    
    grouped = {group: [] for group in IMPORT_GROUPS}
    others = []

    for imp in imports:
        matched = False
        for group in IMPORT_GROUPS:
            if imp.startswith(f"import {group}."):
                grouped[group].append(imp)
                matched = True
                break
        if not matched:
            others.append(imp)

    result = []
    # Add groups in order
    for group in IMPORT_GROUPS:
        if grouped[group]:
            if result:
                result.append("") # Spacer
            result.extend(sorted(grouped[group]))
    
    # Add others at the end if any
    if others:
        if result:
            result.append("")
        result.extend(sorted(others))
        
    return result

def process_file(filepath):
    try:
        with open(filepath, 'r', encoding='utf-8') as f:
            lines = f.readlines()

        content = "".join(lines)
        
        # 1. Extract package, imports, and body
        package_line = ""
        imports = []
        body_lines = []
        
        in_imports = False
        finished_imports = False
        
        for line in lines:
            trimmed = line.strip()
            if trimmed.startswith("package "):
                package_line = trimmed.rstrip(';')
            elif trimmed.startswith("import "):
                imports.append(trimmed.rstrip(';'))
                in_imports = True
            elif in_imports and not trimmed and not finished_imports:
                continue # Skip empty lines between imports
            elif in_imports and not trimmed.startswith("import ") and trimmed:
                finished_imports = True
                body_lines.append(line)
            elif finished_imports or (not in_imports and trimmed and not trimmed.startswith("package ")):
                finished_imports = True
                body_lines.append(line)
        
        body = "".join(body_lines)
        
        # 2. Find and replace FQNs in the body
        def replace_fqn(match):
            string_literal = match.group(1)
            fqn = match.group(2)
            
            if string_literal:
                return string_literal
            
            # If FQN is whitelisted, KEEP it as FQN and don't add to imports
            if fqn in WHITELIST:
                return fqn
            
            class_name = fqn.split('.')[-1]
            imports.append(f"import {fqn}")
            return class_name

        new_body = re.sub(FQN_REGEX, replace_fqn, body)
        
        # 3. Sort imports
        sorted_imps = sort_imports(imports)
        
        # 4. Construct new content
        new_content = []
        if package_line:
            new_content.append(package_line + ";\n\n")
        
        for imp in sorted_imps:
            if imp == "":
                new_content.append("\n")
            else:
                new_content.append(imp + ";\n")
        
        if sorted_imps:
            new_content.append("\n")
            
        new_content.append(new_body.lstrip())
        
        final_text = "".join(new_content)
        final_text = final_text.replace(";;", ";")
        
        if final_text != content:
            with open(filepath, 'w', encoding='utf-8') as f:
                f.write(final_text)
            print(f"Organized imports in {os.path.basename(filepath)}")
            return True
        return False

    except Exception as e:
        print(f"Error processing {filepath}: {e}")
        return False

if __name__ == "__main__":
    files = get_all_java_files()
    print(f"Found {len(files)} files to check...")
    count = 0
    for f in files:
        if process_file(f):
            count += 1
    print(f"Finished. Modified {count} files.")
