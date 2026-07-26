# Soft Limb Bone Sort Plan

Plan to improve soft-vs-soft limb transparency by submitting **one post-deferred draw per soft bone** and letting the existing queue sort them back-to-front. This is the practical follow-up to the open item in `[LIMB_TRANSPARENCY.md](LIMB_TRANSPARENCY.md)` (*Soft limb behind soft limb, same actor*).

**Status:** implemented (v1 — `limbOnlySoft` per-bone submit + `distanceSq` sort).

**Non-goals for this pass:** per-triangle sorting, OIT, addon-based triangle reorder (ruled out as too heavy / fragile with Iris).

---

## Context

### What already works


| Layer                                         | Behavior                                                                                                    |
| --------------------------------------------- | ----------------------------------------------------------------------------------------------------------- |
| Form-wide soft (`color.a` < 1)                | Whole mesh deferred (Iris / BBS / vanilla LAST clouds path)                                                 |
| Limb-only soft (form opaque, some bones soft) | Opaque bones live; soft bones one batched deferred draw                                                     |
| Soft vs world / other actors / clouds         | Post-deferred + depth; vanilla clouds via `WorldRenderEvents.LAST`                                          |
| Soft vs soft **across actors**                | `ShaderOpacityPatch.flushPostDeferredForms` sorts by `renderDepth`, then **farther** `distanceSq` **first** |




### What still fails

On the **same** actor, all soft bones share **one** `Runnable` (`limbOnlySoft` block in `ModelFormRenderer`). Inside that draw, groups are rendered in **model hierarchy order**, not camera depth order. With depth write enabled for soft meshes, a nearer soft limb drawn first can incorrectly occlude a farther soft limb (or the reverse looks wrong depending on hierarchy).

Current key path (single submission):

```text
limbOnlySoft
  → softDistanceSq = entity↔camera (whole actor)
  → one softDeferredDraw { applyLimbSoftVisibility(soft=true); renderModelGeometry… }
  → submitPostDeferredForm / BbsForm / noshading deferred queue
```

Flush already sorts submissions; it cannot reorder bones inside one submission.

### Design choice (locked)

**Sort unit = ModelGroup (pose bone) with soft alpha**, not triangles.

- Feasible with current VAO/group draws (`CubicVAORenderer.renderGroup`).
- Reuses `ShaderOpacityPatch` / paint-overlay deferred queues.
- Acceptable approximation: bone origin / centroid distance; fails when soft meshes heavily interpenetrate (document as known limit).

---



## Goals

1. When `limbOnlySoft`, enqueue **N soft-bone submissions** instead of one full soft mesh.
2. Each submission’s `distanceSq` reflects that bone’s position relative to the camera (not only the entity root).
3. Keep opaque limbs on the live path; keep α≈0 bones hidden (no depth stamp).
4. Preserve Iris camera-matrix path, noshading BBS queue, and no-shader BBS post-deferred path (same three branches as today).
5. Do not regress form-wide soft, paint/glow overlays, or shadow pass.

---



## Approach



### Sort key

For each soft `ModelGroup`:

1. After pose/animator for the frame (same pipeline as today’s deferred draw), read a world/camera-space point for the bone:
  - Prefer **bone matrix translation** from the same capture used for body parts (`MatrixCache` / `captureMatrices` / group transform already applied in the deferred stack), **or**
  - Transform a simple local centroid (origin or bounding midpoint of group cubes/meshes) by the bone matrix × captured root matrix.
2. `distanceSq = |point − cameraPosition|²` (same convention as entity softDistanceSq today).
3. Submit with `renderDepth = 0D` (or keep current convention) so flush order is **farther bones first** via existing `distanceSq` comparator.

Optional refinement (later): secondary key = bone alpha (more opaque after more transparent) if two bones share nearly equal distance.

### Visibility per submission

Reuse `saveGroupVisibility` / `restoreGroupVisibility`, but per bone:

```text
for each soft drawable group G:
  submit Runnable:
    save visibility
    set all drawable groups invisible except G (and keep α≈0 hidden)
    renderModelGeometry… (same snapshots as today)
    restore visibility
```

Do **not** call `applyLimbSoftVisibility(showSoft=true)` for the whole model in one draw.

### Queues to hit (unchanged routing)


| Condition                   | API                                                                 |
| --------------------------- | ------------------------------------------------------------------- |
| Soft + noshading deferral   | `ModelVAORenderer.submitDeferredTranslucentModel(draw, depthWrite)` |
| Iris world, camera matrices | `ShaderOpacityPatch.submitPostDeferredForm(0, distanceSq, …)`       |
| Else (vanilla / BBS bake)   | `ShaderOpacityPatch.submitPostDeferredBbsForm(0, distanceSq, …)`    |


**Note:** the noshading / paint-overlay queue currently sorts mainly by `fullModel` flags, **not** by `distanceSq`. If limb-only soft + noshading still looks wrong after per-bone submit, add distance (or render-depth) sorting to that queue in a follow-up step — call it out in testing.

### Snapshot / closure cost

Today one draw captures many snapshots (pose, color, paint, glow, grade, matrices). Per-bone submit will either:

- **A (preferred v1):** share the same immutable snapshots across N runnables; each runnable only toggles visibility for one group id/name; or
- **B:** capture once, loop submit with `groupId` in the closure.

Avoid deep-copying pose/color N times if possible (memory + GC in film playback).

---



## Implementation steps



### Step 0 — Baseline & docs

1. Keep this document as the source of truth for the feature.
2. Link from `LIMB_TRANSPARENCY.md` known follow-ups → this file.
3. Manual baseline clip: two soft limbs overlapping on one actor (Iris on/off, with/without noshading).



### Step 1 — Collect soft bones

In `ModelFormRenderer` (near `getMinBoneOpacityAlpha` / `applyLimbSoftVisibility`):

1. Add helper e.g. `collectSoftDrawableBones(ModelInstance)` → list of `ModelGroup` where:
  - `groupHasDrawableGeometry`
  - `0.001F < color.a < LIVE_DEPTH_WRITE_ALPHA`
2. Use the same alpha thresholds as `applyLimbSoftVisibility` so live/deferred split stays consistent.



### Step 2 — Bone distance helper

1. Add helper e.g. `boneDistanceSqToCamera(ModelGroup, rootMatrix, cameraPos)` (or entity + transition).
2. Source of bone transform:
  - Ensure pose is applied before measuring (same as deferred draw’s `applyOverlayPosePipeline`), **or**
  - Measure inside the deferred runnable after pipeline apply (distance must be computed **at submit time** with current frame pose — preferred so sort matches drawn pose).
3. Prefer translation of bone matrix in camera/world space; document fallback if matrix missing (use entity `distanceSq`).

**Important:** if distance is computed at submit time before pose apply in the runnable, matrices may be stale. Either:

- apply pose once on the live thread, capture per-bone `distanceSq` into the closure, then deferred draw only renders; or
- compute distance at the start of each deferred runnable (sort order is fixed at **enqueue** time in `ShaderOpacityPatch`, so distance **must** be finalized before `submit`*).

So: **apply pose / capture matrices on the live path, then enqueue with baked** `distanceSq` **per bone.**

### Step 3 — Replace single soft submission

In the `if (limbOnlySoft)` block:

1. Keep existing snapshot capture (once).
2. Apply pose pipeline once on live path (or capture bone matrices) and compute `distanceSq` per soft bone.
3. Loop soft bones → build Runnable that shows only that bone → `submit*` with that bone’s `distanceSq`.
4. If soft bone list is empty, no-op (opaque-only / fully transparent already handled).
5. If only one soft bone, behavior matches today (single submit) — still OK to use the loop.



### Step 4 — Noshading deferred queue (if needed)

1. Test soft limbs + noshading under Iris.
2. If order is still wrong, extend `ModelVAORenderer` deferred translucent / paint overlay entries with `distanceSq` (or reuse `ShaderOpacityPatch` only for this path).
3. Sort farther-first before flush, same as post-deferred forms.



### Step 5 — Form-wide soft (optional, out of v1)

Form-wide soft still draws the whole mesh as one unit. Bone sort does **not** apply unless we also split form-wide soft into per-bone draws (higher cost, more Iris edge cases).

**v1 decision:** only `limbOnlySoft`. Document form-wide soft-vs-soft as unchanged.

### Step 6 — Non-ModelForm audit (optional)


| Path                                | v1                                                           | Later                |
| ----------------------------------- | ------------------------------------------------------------ | -------------------- |
| ModelForm VAO                       | Yes                                                          | —                    |
| BOBJ / non-VAO model path           | Skip unless same group visibility API exists                 | Audit                |
| Body-part child forms on soft bones | Parent soft bone sort only; child form is separate form draw | Verify no regression |
| Billboards / labels / items         | Out of scope                                                 | —                    |




### Step 7 — Tests & docs closeout

Update `LIMB_TRANSPARENCY.md` checklist when done; mark this plan **implemented** with date/notes.

---



## Test plan (manual)

- [ ] Same actor: two soft limbs overlapping; back limb visible through front (Iris on).
- [ ] Same with Iris off (vanilla LAST soft flush).
- [ ] Soft limb + noshading on/off.
- [ ] Soft limb in front of another actor / clouds (no regression).
- [ ] Form-wide soft (unchanged, no crash).
- [ ] Many soft bones (performance smoke: film with full soft character).
- [ ] Shadow pass / F7: no crash.
- [ ] Model-block / UI preview: still live (no deferred sort required).
- [ ] Alpha 0 bone: still hidden, no depth punch.

---



## Risks & limitations


| Risk                                   | Mitigation                                                     |
| -------------------------------------- | -------------------------------------------------------------- |
| N× draw calls / state setup            | Only soft bones; usually few; share snapshots                  |
| Interpenetrating soft meshes           | Accept; no triangle sort                                       |
| Stale `distanceSq` vs animated bone    | Capture after pose on submit frame                             |
| Noshading queue ignores distance       | Step 4                                                         |
| Iris entity shader + many submits      | Keep irisCamera matrix path; smoke-test Complementary/BSL      |
| Visibility race if flush is re-entrant | save/restore visibility inside each runnable (already pattern) |


---



## Suggested file touch list


| File                          | Change                                                                |
| ----------------------------- | --------------------------------------------------------------------- |
| `ModelFormRenderer.java`      | Collect soft bones; per-bone `distanceSq`; replace single soft submit |
| `ShaderOpacityPatch.java`     | Usually none (sort already exists)                                    |
| `ModelVAORenderer.java`       | Only if noshading queue needs distance sort                           |
| `docs/LIMB_TRANSPARENCY.md`   | Link + mark follow-up                                                 |
| `docs/SOFT_LIMB_BONE_SORT.md` | This plan → mark done when shipped                                    |


---



## Implementation notes (v1 shipped)

- `ModelFormRenderer` `limbOnlySoft`: `collectSoftDrawableBones` → capture matrices → one draw per bone with `softBoneDistanceSq` (length-squared) → `applyOnlySoftBoneVisible`.
- **World model blocks + film actors:** post-deferred queue. Soft must not draw during `AFTER_ENTITIES` or depth stamps erase clouds/fluids/other translucents (same rule as form-wide soft opacity).
- **Sort key (deferred):** always `ModelView × stack` (`capturePaintOverlayRootMatrix`), even when Iris draws with entity-local matrices — film `relative` actors sit near stack origin, so stack-only lengthSq does not order by camera depth.
- **UI / form / model-block edit preview (`localPreview`):** immediate sorted draws (post-deferred queues are not flushed there).
- Opaque-only actors: no collect/sort extras.
- Form-wide soft unchanged.



## Decision log


| Decision              | Choice                               | Why                                                   |
| --------------------- | ------------------------------------ | ----------------------------------------------------- |
| Sort granularity      | Per soft `ModelGroup`                | Matches draw API; affordable                          |
| Scope v1              | `limbOnlySoft` only                  | Highest ROI; form-wide already one alpha              |
| Addon / triangle sort | No                                   | Too heavy; Iris-hostile                               |
| Sort key              | Camera `distanceSq` of bone          | Matches existing flush comparator                     |
| Depth write           | Keep current soft depth-write policy | Self-occlusion; world already handled by queue timing |


