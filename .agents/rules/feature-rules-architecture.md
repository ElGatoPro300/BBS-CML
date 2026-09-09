---
trigger: always_on
glob:
description: Part 2 of 2 of BBS Engineering Standards — Domain/Adapter separation, DRY centralization, state rules, multi-version portability, and 3.0 readiness.
---

# Feature Development Rules (Part 2/2) — Architecture & Multi-Version Standards

> [!IMPORTANT]
> **This is Part 2 of a 2-part engineering constitution.**
> - **Part 1**: `.agents/rules/feature-rules-protocol.md` — Planning protocol, AI agent behavior, and pre-implementation workflow.
> - **Part 2 (This document)**: `.agents/rules/feature-rules-architecture.md` — Architecture rules, centralization, state management, and multi-version portability.
>
> **Both documents are mandatory and must be followed together for every code modification.**

---

## §1 — Domain / Adapter Separation (High-Level vs Low-Level)

The BBS codebase strictly enforces a two-layer architectural boundary:

### Domain Layer (High-Level — Version-Agnostic)
- Contains data models, `Value` fields, timeline logic, keyframe interpolation, and math algorithms.
- **Rules**: Pure Java only. **Zero direct imports** from `net.minecraft.*`, `com.mojang.*`, `me.jellysquid.*`, or `net.irisshaders.*`.
- This layer must remain **100% identical** across all versions (1.20.1, 1.20.4, 1.21.1) and directly portable to the BBS 3.0 rewrite.

### Adapter Layer (Low-Level — Version-Specific)
- Contains renderers, shaders, mixin hooks, `VertexConsumer` / `BufferBuilder` usage, `RenderSystem` calls, and Sodium/Iris integrations.
- **Rules**: All version-volatile code must be isolated behind interfaces or adapter classes.

```java
/* WRONG — Domain logic directly coupled to Minecraft rendering API */
public class ColorMaskData
{
    public void apply(MatrixStack matrices, VertexConsumerProvider provider) { ... }
}

/* RIGHT — Domain defines pure data; Adapter executes low-level rendering */
public class ColorMaskData
{
    public ColorPassConfig buildPassConfig() { ... }  /* Pure data, no MC imports */
}

public class ColorMaskRenderer  /* Adapter layer handles OpenGL/Minecraft */
{
    public void execute(ColorPassConfig config, MatrixStack matrices, ...) { ... }
}
```

---

## §2 — Mandatory Centralization (DRY)

**If any logic or pattern appears in 2 or more places, extract it to a shared component.**

1. **Java Logic**: Common rendering pass sequences, matrix transformations, or uniform uploads must live in shared `*Helper`, `*Pipeline`, or `*Executor` classes. Never copy-paste render loops between `BlockFormRenderer`, `ItemFormRenderer`, `StructureFormRenderer`, etc.
2. **GLSL Shaders**: Shared math, masking, and color operations must use includes (`#moj_import`) or shared utility files. Verbatim duplication across `.fsh` files is strictly prohibited.
3. **Search First**: Before creating any new helper, search existing utilities (`*Utils`, `*Helper`, `Draw`, and existing pipelines). Extend existing solutions instead of creating parallel helpers.

---

## §3 — State Management & Code Boundaries

### §3.1 — Zero Mutable Global State
- **Prohibited**: `public static` fields holding mutable rendering state, active colors, alpha multipliers, or flags (e.g., `BlockFormRenderer.color`).
- **Allowed**: `public static final` for immutable, compile-time constants only.
- **Rule**: All rendering parameters must travel explicitly via method arguments or scoped context objects (`RenderContext`, `DrawPassConfig`).

### §3.2 — Size and Responsibility Limits
- **Methods**: Maximum ~150 lines. Extract sub-routines with descriptive names.
- **Classes**: Soft limit of ~600–800 lines. Split multi-responsibility classes into focused components.
- **Shaders**: Avoid declaring unused uniforms (target ≤30 active uniforms per draw call).
- **God Objects**: Zero tolerance. Classes handling state, rendering, uniforms, and textures simultaneously must be decomposed.

### §3.3 — Persistence & Localization
- **Data Persistence**: All persistent form, clip, or replay data **must** use the `Value` system (`ValueFloat`, `ValueBoolean`, `ValueGroup`). Never use raw fields with manual NBT/JSON serialization.
- **UI Strings**: All user-facing text **must** use `IKey` / `UIKeys` resolved from localization JSON files. Never hardcode English text strings in UI components.

---

## §4 — Multi-Version Portability Rules

BBS maintains multiple branches (1.21.1 master, 1.20.4, 1.20.1). High-level domain changes should transfer with zero merge conflicts; only adapter code should require version adaptation.

### §4.1 — Known Critical API Differences

| Component | 1.21.1 (master) | 1.20.4 | 1.20.1 |
|---|---|---|---|
| Shader Program | `ShaderProgram` | `ShaderProgram` | `Shader` |
| Uniform Access | `GlUniform` | `GlUniform` | `Uniform` |
| Uniform Retrieval | `program.getUniform("x")` | `program.getUniform("x")` | `shader.getUniform("x")` (nullable) |
| ModelView Stack | Returns `Matrix4fStack` | Returns `MatrixStack` | Returns `MatrixStack` |
| Sodium Packages | `net.caffeinemc.*` | `me.jellysquid.*` | `me.jellysquid.*` |
| Buffer Builders | `BufferBuilder` (new API) | `BufferBuilder` (legacy) | `BufferBuilder` (legacy) |
| Identifier Factory | `Identifier.of(id, path)` | `Identifier.of(id, path)` | `new Identifier(id, path)` |

### §4.2 — Adapter Pattern for Version Differences
When an API differs across versions, encapsulate it behind a shared domain interface implemented by versioned adapters:

```java
/* Shared interface — identical on all branches */
public interface IShaderBridge
{
    void uploadUniform(String name, float value);
    void bindTexture(int unit, Identifier texture);
}

/* Branch 1.21.1 implementation uses GlUniform */
public class ShaderBridge121 implements IShaderBridge { ... }

/* Branch 1.20.1 implementation uses Uniform */
public class ShaderBridge1201 implements IShaderBridge { ... }
```

### §4.3 — Backporting Protocol
1. Classify all modified files as version-agnostic (domain) or version-specific (adapter).
2. Domain files are cherry-picked without modification.
3. Adapter files are adapted to target API signatures using the table above.
4. Compile and perform full in-game testing on the target branch (with and without shaders).

---

## §5 — Preparing for BBS 3.0 Rewrite

Version 3.0 (`BBS-CML-3.0`) will be a complete rewrite from scratch. Following these rules directly accelerates 3.0 development:

- **Clean Domain Code = 3.0 Code**: Any domain logic (data models, interpolation, timelines, state machines) written decoupled from Minecraft APIs will be directly transferable to 3.0 without rewriting.
- **Architectural Reference**: The abstractions and interfaces built in 2.2 will serve as the architectural specification for 3.0 subsystems.
- Never tightly couple new features to Minecraft internal classes. Always isolate engine dependencies behind adapters.

---

## §6 — Architecture Quick Checklist

Before any commit or PR:

- [ ] Domain logic has zero direct Minecraft/Fabric/Sodium/Iris imports.
- [ ] No duplicated render loops or shader routines across classes.
- [ ] No `public static` mutable variables used for passing state.
- [ ] All persistent data uses the `Value` system.
- [ ] All UI strings use `IKey` / `UIKeys`.
- [ ] Methods are under ~150 lines; classes under ~800 lines.
- [ ] Version-volatile APIs are isolated behind adapters.
