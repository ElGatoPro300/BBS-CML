"""Check the migrated rendering pieces against the project's local Minecraft JARs."""

import argparse
import json
import os
from pathlib import Path
import re
import subprocess
import zipfile


ROOT = Path(__file__).resolve().parents[2]
OUTPUT = ROOT / "build/port-check"


def run_java_tool(tool, arguments):
    executable = Path(os.environ["JAVA_HOME"]) / "bin" / (tool + ".exe")
    if tool == "javap":
        return subprocess.run(
            [str(executable)] + [str(arg) for arg in arguments], cwd=ROOT,
            check=True, capture_output=True, text=True,
        ).stdout
    argument_file = OUTPUT / (tool + "-validation.args")
    argument_file.write_text(
        "\n".join('"' + str(arg).replace("\\", "/") + '"' for arg in arguments),
        encoding="utf-8",
    )
    return subprocess.run(
        [str(executable), "@" + str(argument_file)], cwd=ROOT,
        check=True, capture_output=True, text=True,
    ).stdout


def named_jar(kind, version, mappings):
    candidates = sorted(
        path for path in (ROOT / ".gradle/loom-cache/minecraftMaven").rglob("*.jar")
        if path.name.startswith(kind + "-")
        and "sources" not in path.name
        and version + "-net.fabricmc.yarn." in path.name
        and mappings + "-v2" in path.name
    )
    if not candidates:
        raise RuntimeError("Missing named Minecraft JAR: " + kind + ". Run Gradle first.")
    return candidates[0]


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--gpu", action="store_true", help="Also test the GLSL in a hidden OpenGL window.")
    options = parser.parse_args()
    OUTPUT.mkdir(parents=True, exist_ok=True)
    properties = dict(
        line.strip().split("=", 1)
        for line in (ROOT / "gradle.properties").read_text().splitlines()
        if "=" in line and not line.lstrip().startswith("#")
    )
    version = properties["minecraft_version"]
    mappings = properties["yarn_mappings"]
    client = named_jar("minecraft-clientOnly", version, mappings)
    common = named_jar("minecraft-common", version, mappings)
    dependencies = (ROOT / ".gradle/loom-cache/remapClasspath.txt").read_text().strip().split(";")
    classpath = [str(OUTPUT), str(ROOT / "build/classes/java/main"), str(client), str(common)]
    classpath.extend(path for path in dependencies if not Path(path).name.startswith("minecraft-"))
    cp = ";".join(classpath)
    sources = ["PickerPreviewRenderState", "ModelPreviewRenderer"]
    print(run_java_tool("javac", ["-proc:none", "-classpath", cp, "-d", OUTPUT] + [
        ROOT / "src/client/java/mchorse/bbs_mod/graphics" / (name + ".java") for name in sources
    ]))
    print("PASS: isolated compilation of", ", ".join(sources))
    print("Minecraft API:", client)
    json.loads((ROOT / "src/client/resources/bbs.client.mixins.json").read_text())
    api_classpath = str(client) + ";" + str(common)
    layer_api = run_java_tool("javap", ["-s", "-classpath", api_classpath, "net.minecraft.client.render.RenderLayers"])
    mixin = (ROOT / "src/client/java/mchorse/bbs_mod/mixin/client/RenderLayerTextureOverrideMixin.java").read_text()
    descriptors = re.findall(r'"(\w+)(\(Lnet/minecraft/util/Identifier;[^\"]+)"', mixin)
    if len(descriptors) != 5:
        raise AssertionError("Expected five explicit texture override injection descriptors")
    for name, descriptor in descriptors:
        if not re.search(r"\b" + name + r"\([^\n]+\);\s+descriptor: " + re.escape(descriptor), layer_api):
            raise AssertionError("Missing mixin target: " + name + descriptor)
    print("PASS: client mixin JSON and five RenderLayers injection descriptors")
    fog_api = run_java_tool("javap", ["-p", "-classpath", api_classpath, "net.minecraft.client.render.fog.FogRenderer"])
    if "private org.joml.Vector4f getFogColor(net.minecraft.client.render.Camera, float, net.minecraft.client.world.ClientWorld, int, float);" not in fog_api:
        raise AssertionError("FogRenderer.getFogColor signature changed")
    print("PASS: instance FogRenderer.getFogColor signature")
    if not options.gpu:
        return
    with zipfile.ZipFile(client) as archive:
        vertex = (ROOT / "src/main/resources/assets/bbs/shaders/core/picker_preview.vsh").read_text()
        for name in ["dynamictransforms", "projection"]:
            include = archive.read("assets/minecraft/shaders/include/" + name + ".glsl").decode()
            include = re.sub(r"^#version[^\n]*", "", include, flags=re.MULTILINE)
            vertex = vertex.replace("#moj_import <minecraft:" + name + ".glsl>", include)
        (OUTPUT / "picker_preview.vsh").write_text(vertex)
    for module in ["lwjgl", "lwjgl-glfw", "lwjgl-opengl"]:
        library = next(Path(path) for path in dependencies if Path(path).name.startswith(module + "-")
                       and "natives" not in Path(path).name and Path(path).parent.parent.parent.name == module)
        native_jars = sorted(library.parent.parent.rglob(module + "-*-natives-windows.jar"))
        if not native_jars:
            raise RuntimeError("Missing Windows native library for " + module)
        classpath.extend(str(path) for path in native_jars)
    cp = ";".join(classpath)
    print(run_java_tool("javac", ["-proc:none", "-classpath", cp, "-d", OUTPUT, ROOT / "tools/port/PickerShaderProbe.java"]))
    print(run_java_tool("java", ["-classpath", cp, "PickerShaderProbe"]))


if __name__ == "__main__":
    main()
