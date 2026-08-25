# Soft Opacity for Flat Forms (Billboard / Shape / Label)

Implementation plan to align **soft transparency** on flat forms with the pipeline that already works for **ModelForm** (and Extruded): `ShaderOpacityPatch` post-deferred queue.

Related docs:

- `docs/LIMB_TRANSPARENCY.md` — limb / bone soft opacity (ModelForm)
- `docs/SOFT_LIMB_BONE_SORT.md` — soft-limb batch sorting (one post-deferred entry per soft bone)

Related code (reference implementation):

- `ModelFormRenderer` — soft form / soft limb post-deferred submit
- `ExtrudedFormRenderer` — same `ShaderOpacityPatch` contract for a non-rigged form
- `ShaderOpacityPatch` — queue, flush timing, depth write, distance sort
- `BBSModClient` — `WorldRenderEvents.AFTER_TRANSLUCENT` / `LAST` hooks
- `BillboardFormRenderer` / `ShapeFormRenderer` / `LabelFormRenderer` — targets

**Out of scope:** per-face / per-fragment **depth write** experiments (not a drop-in fix). **Face-level sort keys** (queue order) are in scope where noted below — that is sorting, not fragment depth masking.

---

## Problem summary

| Form type | Soft opacity today | Result |
|-----------|-------------------|--------|
| ModelForm / Extruded | `ShaderOpacityPatch.submitPostDeferred*` | Correct see-through, clouds/fluids timing, depth vs later draws, lighting OK |
| Billboard | Soft stays **live** with `depthMask` off (defer only for Color Grade / noshading via paint-overlay) | Soft limbs / labels / later draws punch through; different timing than actors |
| Shape | Iris flat deferral via **paint-overlay** queue | Not the same flush/sort as ModelForm |
| Label | Mostly live | Same class of order bugs when soft |

**Root cause:** flats do not join the ModelForm soft-opacity contract (post-deferred + sort key + depth write + after-fluids flush).

---

## Goals

1. Soft billboards use the **same** soft-opacity path as ModelForm/Extruded.
2. Soft shapes and labels follow once billboards are validated.
3. Preserve live shading format/shader (no forced “fully lit” redraw).
4. Do **not** redraw body parts on a separate dark BBS path when only soft opacity is deferred (avoids dark transparent limbs).
5. Preview / UI (`modelRenderer` / local preview) keeps immediate draws (queues do not flush there).
6. Shadow pass stays live (do not enqueue soft into post-deferred during shadow).
7. Improve soft-vs-soft order vs soft limbs by using an appropriate **sort granularity** (see below) — without inventing a second pipeline.

---

## Non-goals

- Order-independent transparency (OIT).
- Guaranteeing perfect depth on every Iris pack after composite (flats remain slightly more fragile than volume meshes).
- Rewriting the shadow map from post-deferred soft draws.
- Per-face / per-fragment depth **masking** as the primary fix.
- Per-glyph / per-triangle sorting for labels or dense meshes.
- Per-face sorting for Block / Structure unless a concrete bug proves it necessary.

---

## Sort granularity policy

Rule: **sort unit ≈ cheap, stable draw unit** you already control — not “how many faces the mesh has in theory.”

| Form | Sort unit | Notes |
|------|-----------|--------|
| **Billboard** | **One face (the quad)** | High ROI. One post-deferred entry; sort key = depth of that face (centroid / plane), not only form origin. Analogous to one soft bone in `SOFT_LIMB_BONE_SORT.md`. |
| **Label** | **Whole label plane** | Looks “few faces” but is many glyph quads + shadow / outline / glow. **Do not** sort per glyph. One entry; key = plane / form depth. |
| **Shape** | Plane/form, or few explicit planes | Simple few-plane shapes may use plane keys like billboard. Dense meshes → form/group level, not per-triangle. |
| **Block** | Form (centroid / AABB) if ever soft-deferred | Vanilla baked mesh; extracting faces into the queue is costly/fragile. Face sort only if a filed bug demands it. |
| **Structure** | Form, or at most per-block | Many faces × many blocks — **no** per-face sort. |
| **Extruded / ModelForm** | Already settled | Soft limbs = per **bone**, not per face. |

Compatible approach (billboard soft vs soft limb):

- **Same** `ShaderOpacityPatch` queue and lighting contract as ModelForm.
- **Better** sort key for the billboard face (and film look-axis depth when matching soft-bone film keys).
- **Avoid** paint-overlay soft queue, forced lit shaders, and parent body-part redraws — those caused the old depth/lighting regressions, not face-level *sorting*.

Residual limit: if a soft plane **interpenetrates** a soft limb volume, order can still be wrong (bone-level vs plane-level, no OIT). That is accepted.

---

## Shared contract (copy from ModelForm)

For each soft draw (`0.001 < alpha < LIVE_DEPTH_WRITE_ALPHA` ≈ `0.999`):

1. **Gate:** `ShaderOpacityPatch.shouldDelayUntilPostDeferred(alpha)` (and skip if preview / shadow / picking as appropriate).
2. **Matrices:**
   - Iris world model pass: entity/form-local position matrix; `submitPostDeferredForm` restores camera ModelView (`irisCamera = true`).
   - No-shader / BBS: `ModelVAORenderer.capturePaintOverlayRootMatrix(...)` + `submitPostDeferredBbsForm`.
3. **Normals:** capture and restore (billboard look-at may use identity scale-aware normals like live path — stay consistent with live shading).
4. **Depth write:** `ShaderOpacityPatch.shouldWriteDepthForOpacity(alpha)`. Align with soft-limb multi-entry policy (color then depth stamp) only if soft-vs-soft across forms requires it after playtest — do not invent a separate depth system.
5. **After fluids:** `ShaderOpacityPatch.shouldFlushAfterFluids(alpha)`.
6. **Sort key:** farther first within same `renderDepth`. Prefer:
   - Billboard: **face** depth (quad centroid / plane; film: look-axis depth like soft bones when applicable).
   - Label / typical shape: **plane or form** depth.
   - Avoid form-origin-only keys when the drawable plane is offset from the form origin.
7. **Live pass:** do not draw soft color on the live path when queued (omit live soft draw), same idea as ModelForm soft handoff.
8. **Shader/format:** reuse the live `VertexFormat` / `Supplier<ShaderProgram>` unless Color Grade forces `BBSShaders.getModel()`.

Flush timing is already owned by `ShaderOpacityPatch`:

- Iris: `onAfterTranslucentTerrain()`
- Vanilla: `onAfterVanillaClouds()` (`WorldRenderEvents.LAST`)

---

## Phase A — Billboard → ModelForm soft pipeline + face sort key (priority)

**Status:** implemented in `BillboardFormRenderer` (soft → `ShaderOpacityPatch` + face sort key; opaque Iris grade/noshading still paint-overlay).

**Primary bugs fixed:** soft limbs/labels drawing on top of soft billboards; soft billboard order inconsistent with actors; clouds/fluids timing vs billboard soft.

**Refinement:** same pipeline as ModelForm; sort key = **billboard face**, not a second system.

### A.1 — Detect soft and enqueue

In `BillboardFormRenderer` (world path only: `!modelRenderer`, `!shadowPass`, not picking):

- If `shouldDelayUntilPostDeferred(color.a)`:
  - Snapshot color, quads, UVs, light, overlay, texture link, linear/mipmap, glow if needed.
  - Compute sort key from the **drawn face** (quad centroid / plane depth; prefer look-axis depth in film when matching soft-limb keys).
  - Choose Iris vs BBS submit like ModelForm / Extruded.
  - `depthWrite` / `afterFluids` from `ShaderOpacityPatch`.
  - Enqueue redraw that calls existing `drawBillboardFaces` (or equivalent) with snapshots.
  - **Return without** live soft draw.

### A.2 — Look-at / matrix split

- Keep camera-facing billboard logic, but apply the same Iris vs baked-matrix split as ModelForm so post-deferred MVP is correct.
- Do not force lit `POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL` when the form has shading off.

### A.3 — Body parts

- Soft-opacity-only enqueue: **do not** set a “defer all body parts into this redraw” path.
- Child forms keep their own renderers; soft ModelForm children already use post-deferred on their own.

### A.4 — Color Grade / noshading

- Prefer not to invent a second soft queue.
- Soft opacity always uses `ShaderOpacityPatch`.
- Grade / noshading may remain as existing overlays or fold into the same deferred redraw only if lighting stays correct (match Extruded/ModelForm patterns).

### A.5 — Test checklist (Phase A)

- [ ] Soft billboard + soft ModelForm limbs behind → limbs visible *through* / correctly ordered, not punching opaque texels on top.
- [ ] Soft billboard brought near a soft limb → order follows face vs bone keys reasonably (interpenetration still imperfect).
- [ ] Soft billboard + labels behind → same.
- [ ] Soft billboard + clouds (Iris and vanilla) → same qualitative behavior as soft ModelForm actors.
- [ ] Soft billboard in front of walls → occluded by nearer solid geometry (best-effort under Iris).
- [ ] Shading off soft billboard → not unusually bright / “shadowless” vs live opaque.
- [ ] 100% opaque billboard → unchanged live path.
- [ ] Form editor / model-block preview → still correct (no post-deferred flush).
- [ ] Shadow pass → soft billboard still contributes as today (live); no queue during shadow.
- [ ] No regression: dark transparent limbs on body parts attached to soft billboards.

**Exit criteria:** Phase A checklist green before starting Phase B.

---

## Phase B — Shape and Label → same pipeline (plane / form keys)

**Bugs fixed:** remaining flat soft order issues once billboards are correct.

### B.1 — ShapeFormRenderer

- Replace Iris `needsIrisTranslucentFlatDeferral` + `submitDeferredTranslucentModel` soft path with `ShaderOpacityPatch` post-deferred (same contract as Phase A).
- Sort key: **plane or form** depth. Few explicit planes may use plane keys; dense mesh → one form-level entry (no per-triangle sort).
- Keep shape mesh / lighting snapshots analogous to Extruded.

### B.2 — LabelFormRenderer

- Soft label alpha → post-deferred submit with **label plane / form** depth key.
- **Do not** split into per-glyph queue entries.
- Redraw via existing label draw helpers (`render3DInternal` or split draw).
- Keep outline/shadow ordering fixes that do not fight depth write (outline/shadow may still use temporary `depthMask(false)` *within* the deferred draw, then restore).

### B.3 — Shared sort

- All soft flats share `ShaderOpacityPatch` sorting with ModelForm soft entries (far → near). No separate unsorted paint-overlay soft queue for these forms.

### B.4 — Test checklist (Phase B)

- [ ] Soft shape vs soft billboard / soft actor order.
- [ ] Soft label vs soft billboard (near label must not redraw after farther billboard incorrectly).
- [ ] Outline / shadow / glow on soft labels still readable.
- [ ] Repeat Phase A cloud / wall / preview / shading checks for shape + label.

**Exit criteria:** Phase B checklist green; no Phase A regressions.

---

## Phase C — Other forms (optional)

Only if outliers remain after A/B.

Candidates:

- Other flat or panel-like forms that still use live soft `depthMask(false)` or paint-overlay soft deferral.
- Composite cases (body-part trees mixing soft flats and soft models) — verify they only rely on the shared queue, not ad-hoc parent redraws.
- **Block / Structure:** join ModelForm soft pipeline **only if** a concrete soft-opacity bug is filed. Default sort = form (or at most per-block for structures). **Per-face sort stays out** unless evidence shows form-level is insufficient.
- Extruded already follows ModelForm — do not rewrite unless regressing.

### C.1 — Test checklist (Phase C)

- [ ] Reported outlier form matches ModelForm soft behavior.
- [ ] No new dark-limb / full-bright soft regressions.
- [ ] If Block/Structure were touched: no per-face explosion; lighting/depth unchanged for opaque paths.

---

## Implementation order

```text
1. Phase A — Billboard + face sort key (same ShaderOpacityPatch pipeline)
        → playtest
2. Phase B — Label / Shape on the same queue; plane or form keys
        (no per-glyph / no per-triangle)
        → playtest
3. Phase C — Block / Structure only if needed; face sort out unless evidence
```

Do not start B until A exit criteria pass. Do not broaden to C without a concrete remaining bug.

---

## Risks and mitigations

| Risk | Mitigation |
|------|------------|
| Zero-thickness quad + Iris depth mismatch | Reuse ModelForm matrix split; keep `FACE_Z_BIAS`; validate per pack; avoid disabling depth test globally |
| Soft billboard looks fully lit | Preserve live format/shader in deferred redraw |
| Dark limbs | Never batch-redraw body parts in a foreign BBS pass for soft-only |
| Double draw (live + deferred) | Skip live soft color when enqueued |
| Preview empty/wrong | Gate enqueue with `modelRenderer` / local preview like ModelForm |
| Soft billboard vs soft limb near / interpenetrating | Face key + shared queue; accept residual without OIT or fragment depth hacks |
| Over-sorting labels / blocks | Stick to plane/form (label) and form-only (block/structure) policy above |

---

## Success definition

Soft Billboard / Shape / Label transparency behaves **qualitatively like soft ModelForm actors**: correct see-through, coherent order vs other soft forms (billboard face keys vs soft bones), clouds/fluids timing consistent with actors, no dark-limb regression, opaque and preview paths unchanged.
