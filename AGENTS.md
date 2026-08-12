# AGENTS.md

This file provides guidance to Codex (Codex.ai/code) when working with code in this repository.

## Project Overview

BBS CML EDITION is a Minecraft animation/video-production mod for Fabric 1.20.4 (also 1.20.1, 1.21.1). It enables creating animated films, controlling cameras, morphing entities, and more within Minecraft.

## Build Commands

```bash
./gradlew build          # Build the project
./gradlew runClient      # Launch Minecraft client with the mod (primary testing method)
./gradlew runServer      # Launch Minecraft server
./gradlew jar            # Create JAR artifact only
./gradlew clean          # Clean build directory
```

There is no automated test suite — testing is done by running the mod in-game via `./gradlew runClient`.

**Do not modify gradle configuration** — this is an explicit rule from CONTRIBUTING.md.

## Code Style (from CONTRIBUTING.md — strictly enforced)

- Braces on new lines: `if (condition)\n{\n    ...\n}`
- All instance field/method access must use `this.`
- Max ~150 lines per method
- Float/Double/Long literals require capital suffixes: `1F`, `2D`, `3L`
- No `var` keyword; no redundant generic type args: use `new ArrayList<>()` not `new ArrayList<String>()`
- Comments in English; inline body comments use `/* */` not `//`
- Field/method order per class: constants → static fields → instance fields → static constructor → static methods → constructors → instance methods
- No pure AI-generated code (must be understood and manually tested)

## Architecture

### Module Split

The mod has a strict client/server split:

- `src/main/java/mchorse/bbs_mod/` — Server-side and shared logic
- `src/client/java/mchorse/bbs_mod/` — Client-only rendering and UI

Entry points (defined in `fabric.mod.json`):
- `BBSMod` — Server initializer
- `BBSModClient` — Client initializer

### Key Subsystems

**Film & Animation Pipeline**
- `film/` — Film and timeline management (the core "recording" concept)
- `camera/` — Camera clip types: Dolly, Keyframe, Path, Idle
- `utils/` — Keyframe interpolation, clip composition, Molang math expressions
- `math/` — MolangParser for animation expressions
- Clips are composable/stackable segments forming a timeline
- Action clips extend `ActionClip`: override `applyAction()` for server/fake-player logic and `applyClientAction()` for client-only effects; `frequency` controls one-shot (0) vs. repeating (N ticks) execution
- Frame↔tick conversion: `tickFloat = (frame / videoFPS) * 20`; internals stay tick-based (20 TPS)

**Forms System** (`forms/`)
- Data-driven visual elements (Models, Particles, Light, Block, Mobs, etc.)
- Each form has automatic serialization (NBT/JSON) and a paired UI editor
- Extensible via addon registration

**3D Models & Rendering**
- `bobj/` — BOBJ model format (Blender export), armature/skeleton system
- `cubic/` — glTF and BOBJ loaders; client side uses VAO rendering with vanilla fallback
- `client/graphics/` — OpenGL framebuffers, texture manager, `Draw` utility class
- Shader support through Iris integration (GLSL Transformer for compilation)

**UI Framework** (`client/ui/`)
- Custom widget system built on SprucheUI with Flex layout
- Dashboard + panel architecture for the in-game editor
- Server-side UI utilities in `ui/`
- `UIDebugPanel` — in-game drag-and-drop layout builder; exports widget trees to clipboard/file for rapid prototyping

**Addon System** (documented in `docs/ADDONS.md`)
- Extend via `BBSAddon` (server) / `BBSClientAddon` (client)
- Central `EventBus` in `events/` for registration hooks
- Addons can register custom Forms, Clips, UI panels, settings, and networking

**Networking** (`network/`)
- Fabric networking API wrapped in `PacketCrusher`
- `ServerNetwork` handles packet dispatch
- Large payloads (>30KB) are automatically chunked by `PacketCrusher` and reassembled via `IBufferReceiver`

**Audio** (`audio/`, `client/audio/`)
- Server: WAV/OGG loading and sound management
- Client: `SoundManager`, `AudioRenderer`, `Waveform` visualization; uses OpenAL via LWJGL

### Mixin Usage

Heavy bytecode patching via 44 mixins:
- Server-side (`bbs.mixins.json`): entity, player, world, item behavior patches
- Client-side (`bbs.client.mixins.json`): rendering, input, Iris/Sodium compatibility

When adding mixins, register them in the appropriate JSON file and use the access widener (`bbs.accesswidener`) when private member access is needed.

## Key Docs

- `docs/ADDONS.md` — Full addon API reference (forms, clips, UI, Molang, networking)
- `docs/FILM_FRAME_TIMELINE.md` — Frame/tick timeline model and export FPS conversion
- `docs/FLUID_SIMULATION.md` — Fluid simulation subsystem internals
- `CONTRIBUTING.md` — Complete code style rules and contribution process