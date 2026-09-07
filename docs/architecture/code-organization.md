# Code organization (maintainability)

Lessons from comparing BBS CML with the [BBS FS](https://github.com/Wemppy4/bbs-fs) fork. Use this when adding features, refactoring, or migrating work onto the UI-redesign branch. Style details stay in `CONTRIBUTING.md`; this doc is about **structure**.

## Core idea

Treat large editors and renderers as **orchestrators**. They wire panels, controllers, and passes; they do not own layout math, batch geometry, sheet factories, or multi-hundred-line render pipelines.

FS stays smaller partly because it ships fewer features — but it also **finishes extractions**. CML already has several of the same abstractions (`UIDockLayout`, `UIReplaysEditorUtils`, render utils) and underuses them (e.g. film panel layout still inline; replay utils barely called).

## Patterns to prefer

### Panel = orchestrator

* Subsystems live in dedicated classes/packages: preview, recorder, dock, list panel, properties panel, controller, context menus.
* Panel owns **visibility and mode** (what is active); layout geometry belongs in a shared layout system (`UIDockLayout` + `ILayoutSource`), not a private copy of splitter/tab code.
* If a constructor only assigns twenty collaborators and mounts the dock, that is healthy. If it implements drop zones and home mosaics inline, extract.

### Shared layout once

* One dock/layout subsystem for film, particles, and future editors.
* Do not reimplement floating panels / splitters / tab stacks inside a product panel when `UIDockLayout` (or its successor on the redesign branch) already exists.
* Product chrome that is not layout (e.g. timeline toolbar) may be a separate package, but it must not fork dock geometry.

### Utils and pure logic

* Heavy editor logic (keyframe sheet building, gizmo drag helpers, pick helpers, batch pose ops) goes in `*Utils` or pure processors (`ReplayBatchProcessor`-style), not in the widget class.
* A utils class that exists but is almost unused is a smell: either migrate call sites or delete dead API.
* Zero-UI classes for export queues, activity tracking, scanners, batch geometry.

### Renderers as pipelines of passes

* Prefer a thin `ModelFormRenderer` (or equivalent) that sequences named passes over one giant `renderModel()`.
* Vertex consumers and overlay helpers are good; **pass classes** (main geometry, glow, soft limbs, equipment) keep the coordinator readable.
* Iris/shadow constraints still apply — see `docs/architecture/rendering-iris.md`.

### Domain packages

* Split by role when a subsystem grows: `config` / `runtime` / `io` / `debug` / `solver` (as FS does under `cubic/ik/`).
* Copy **package shape**, not necessarily FS’s product IK/UX when CML already has a different design.

### Registration

* Prefer CML’s **event-driven registration** (`RegisterFormsEvent`, client register events, etc.) over a monolithic `BBSMod` that lists everything inline.
* Keep `BBSFeatures` (or equivalent) for unfinished UI; do not ship half-wired surfaces without a gate.
* Entry points may grow; if they become hard to read, split into `*Registration` helpers **without** dropping the event model.

## Size heuristics (soft)

| Signal | Action |
|--------|--------|
| Touching a class already ≫ ~2–3k LOC | Extract before adding more behavior |
| New feature would add ≫ ~150–200 LOC to a god-file | New class/package first |
| Method approaching ~150 LOC (`CONTRIBUTING`) | Split |
| Nested types accumulating in a UI panel | Promote to top-level types |

These are guides, not hard CI limits. Framework widgets that are inherently large (dock layout, keyframe graphs) can be big **if the scope is cohesive**.

## What CML should keep

* Addon/event registration and `docs/ADDONS.md` contracts
* `BBSFeatures` feature gates
* Product layers that FS lacks (timeline toolbar, home/cross-world, extra clips) — as **isolated packages**, not inline inside `UIFilmPanel`
* Architecture deep-dives under `docs/architecture/`

## What not to copy blindly from FS

* Sparse registration that makes addons harder
* Dropping CML product features to “look clean”
* Replacing CML IK/limb systems wholesale with FS IK
* Assuming smaller LOC always means better design (missing features inflate the illusion)

## Related

* UI redesign / feature migration plan: `docs/architecture/ui-redesign-migration.md`
* UI framework: `docs/architecture/ui-framework.md`
* Style: `CONTRIBUTING.md`
