# Framebuffer look-at (Billboard + Camera content) — master plan for BBS CML 3.0

> **Status (2026-09):** Deferred out of the 2.2 line. In 2.2, FramebufferForm `billboard` + `camera_content` work for normal viewing angles and editor gizmos track limbs correctly, but **pitch past ±90°** (orbit through zenith/nadir, world appears upside-down) exposes singularities in world-up `lookAt` and Euler `camera.view`. This document captures the diagnosis and the agreed 3.0 approach (**continuous up + quaternion face-camera**) so 3.0 can implement it on the quaternion stack without rediscovering the same ground.
>
> **Not in scope for 2.2 implementation** from this doc. Do not port “fix past ±90°” into 2.2 unless a separate, explicit task asks for a stop-gap (e.g. pitch clamp only).

Related:

- Greenfield repo: [BBS-CML-3.0](https://github.com/ElGatoPro300/BBS-CML-3.0)
- 2.2 implementation surface: `FramebufferForm` / `FramebufferFormRenderer` (this repo)
- Feature rules: `.cursor/rules/FEATURE-RULES-PROTOCOL.mdc` + `FEATURE-RULES-ARCHITECTURE.mdc`
- Quaternion / transform direction for 3.0: see 3.0 bootstrap / code-organization docs in that repo when available

---

## 1. Goals for 3.0

1. **Billboard** (display quad faces the lens) and **Camera content** (FBO impostor re-oriented by view) remain independent toggles, as in 2.2.
2. **Continuous behaviour through zenith/nadir**: orbiting past ±90° pitch must not freeze, flip, or desync the impostor / postcard.
3. **Shared orientation math** with the rest of 3.0: prefer **quaternions** (and continuous up-vector tracking) over Euler yaw/pitch for face-camera and content look-at.
4. **Gizmos / picking stay aligned** with the same matrices used to draw the postcard (2.2 already taught that `collectMatrices` must follow the blit path, not a parallel ad-hoc view inverse).
5. Keep **domain/adapter split**: pure orientation maths in domain; Minecraft/render upload in adapters.

Non-goals for this plan:

- Changing the artistic intent of Camera content (world-up subject vs camera-roll) without an explicit product decision (see §4).
- Re-litigating 2.2 gizmo/highlight fixes; those stay on 2.2.

---

## 2. Problems observed on 2.2 (motivation)

### 2.1 What already works (baseline to preserve)

| Mode | Render | Highlight / stencil | Gizmos (after 2.2 fixes) |
|------|--------|---------------------|---------------------------|
| Neither toggle | Canonical FBO + form quad | OK | OK |
| Camera content only | FBO filled with world-up `lookAt` | OK | OK (replay fill capture in `collectMatrices`) |
| Billboard only | Quad uses face-camera | OK | OK (editor: reconstruct parent from captured blit MV; world: bake `camera.view`) |
| Both | Content lookAt + facing quad | OK | OK when pitch is in the “normal” range |

### 2.2 Failure past ±90° pitch (model-block / free orbit)

Repro context: immersive model-block (or any orbit whose pitch is **not** clamped like vanilla player pitch). When the user tips past looking straight down/up and the world appears inverted:

- **Camera content** uses `Matrix4f.lookAt(eye, target, worldUp)`. Near the pole, `worldUp ∥ view` → singularity. 2.2 patches with a yaw-derived up when `horizSq` is tiny, but **crossing** the pole still produces a discontinuous twist or a “stuck” impostor.
- **Billboard** uses Euler-derived `camera.view` (`rotateZ/X/Y` or MC camera rotation). Past ±90° pitch, Euler yaw/pitch is gimbal-ambiguous; the postcard orientation can disagree with the content bake.
- **UIModelRenderer** does not clamp pitch; vanilla player camera does (±90°). So this shows up in editor/immersive orbits more than in normal survival look.

### 2.3 Why not fix fully in 2.2

- A robust fix wants **quaternion slerp / continuous up** and a single face-camera helper shared with Video/Label billboards — that matches the 3.0 transform rewrite, not another one-off in `FramebufferFormRenderer`.
- Stop-gaps (hard pitch clamp for these toggles only) are optional 2.2 polish; they are **not** the master design.

---

## 3. Architecture snapshot: 2.2 vs desired 3.0

| Concern | CML 2.2 | Target 3.0 |
|---------|---------|------------|
| Billboard rotation | Clear 3×3, mul Euler/`camera.view` (world); editor clears MV that already has view | Quaternion “face camera” (or equivalent polar-decomp) on the display quad |
| Camera content | `lookAt(eye, target, worldUp)` + zenith yaw hack | Look-at with **continuous up** (carry previous up, Gram–Schmidt against view); optional mode for camera-up |
| Pitch past pole | Undefined / jumps | Defined continuous path; document expected roll |
| Gizmo / pick matrices | Must match blit MV (capture pre/post billboard in editor) | Same rule: one orientation source for draw + `collectMatrices` / bone cache |
| Shared helpers | Duplicated vs Video/Label | Domain `FaceCamera` / `ViewOrientedBasis` used by Framebuffer, Video, Label, etc. |

---

## 4. Design choices for 3.0 (2 + 3)

Agreed direction from 2.2 discussion: implement **(2) continuous up** and **(3) quaternion face-camera**, not a pitch clamp as the primary solution.

### 4.1 Continuous up (Camera content)

**Problem:** `lookAt(..., (0,1,0))` is discontinuous when the view direction passes through ±Y.

**Approach:**

1. Maintain a **previous screen-up** (or previous full basis) per FramebufferForm render instance (or per trail/instance id if multiple draws).
2. Each frame: `forward = normalize(target - eye)`; project previous up onto the plane ⊥ forward; renormalize. If the projected length is tiny, fall back to a stable reference (e.g. previous right × forward, or yaw-derived up once).
3. Build the content orientation from `{forward, up, right}` (or a quaternion from that basis), not from a fresh world-up `lookAt` every time.
4. Reset previous up when the form is first shown, toggles flip, or eye–target distance collapses.

**Product note:** Continuous up preserves “no sudden 180° twist” when crossing the pole. It may introduce a slow roll if the user orbits in a full loop; that is acceptable and should be documented. If CML later wants strict world-up at all costs, offer a Value enum: `WorldUp` (current 2.2 intent, pole singularity) vs `ContinuousUp` (default in 3.0 for impostors).

### 4.2 Quaternion face-camera (Billboard)

**Problem:** Euler `camera.view` is a poor face-camera operator past ±90° pitch.

**Approach:**

1. Domain helper: given camera orientation as quaternion (or view matrix → quat), produce a **billboard rotation** that maps local +Z (or whatever the postcard normal is) to `-forward`, with up taken from the **same continuous-up policy** as content when both toggles are on (so postcard axes and FBO UV axes stay aligned).
2. When **only** Billboard is on, up may follow camera-up or continuous-up; pick one and keep Video/Label consistent.
3. Adapter applies the quaternion to the model matrix (no “clear 3×3 then mul Euler view” in editor vs world forks if avoidable). Prefer one code path: build the facing rotation in domain, multiply in adapter.

### 4.3 Both toggles together

When both are enabled (2.2 legacy `look_at` migration):

- Content basis and billboard basis should share the **same up policy** so UV ↔ gizmo projection stays 1:1 (lesson from 2.2).
- Draw path remains: fill FBO with content orientation → blit textured quad with billboard orientation.
- `collectMatrices` / bone cache must consume the **same** orientations produced for draw (single builder object per frame, no parallel Euler path).

### 4.4 Editor vs world

3.0 should avoid the 2.2 split where the form-editor orbit already multiplies view into the stack and billboard “clears” it. Prefer:

- Always compute face-camera in **world/entity space**, then let the viewport apply view once; or
- Always compute in **view space** with an explicit `ViewSpace` flag on the orientation builder.

Document the chosen space in the 3.0 form renderer contract so gizmos never double-apply view.

---

## 5. Phased implementation (3.0)

| Phase | Deliverable | Exit criteria |
|-------|-------------|---------------|
| **A. Domain orientation** | `FaceCamera` / `ContinuousUp` (or equivalent) with unit tests: orbit through zenith/nadir, full loop, eye≈target | No NaNs; angle change continuous (no 180° jump on pole cross) |
| **B. Framebuffer adapter** | Wire Billboard + Camera content to domain helpers; one matrix source for fill, blit, and bone/`collectMatrices` | Gizmos track limbs with both toggles while orbiting past ±90° in model-block-like preview |
| **C. Share with Video/Label** | Replace duplicated Euler billboard mul where applicable | Same face-camera helper; no regression on normal pitch ranges |
| **D. UX / values** | Optional up-policy Value if product wants WorldUp vs ContinuousUp | Localized; defaults match §4.1 |
| **E. Docs** | Short architecture note in 3.0 `docs/architecture/` + wiki if user-facing | Agents know not to reintroduce Euler-only look-at for impostors |

Suggested order: A → B → E (minimal ship), then C → D.

---

## 6. Test plan (manual, 3.0)

1. FramebufferForm with ModelForm child; **Camera content** only — orbit through straight-down and past inverted; impostor keeps showing coherent sides, no freeze.
2. **Billboard** only — same orbit; postcard always faces lens; no sudden flip at pole.
3. **Both** — same orbit; limbs, highlight, and gizmos stay stacked; drag axes not mirrored.
4. Frontal / side / 45° pitches (regression of 2.2 “happy path”).
5. Eye coincident with target (degenerate distance) — no crash; previous frame or skip look-at.
6. With / without shader pack if 3.0 still has an Iris-equivalent path for the parent blit.

---

## 7. Risks and open questions

| Risk / question | Notes |
|-----------------|--------|
| Continuous up “drift” after multiple full loops | Expected; document or offer WorldUp mode |
| Editor stack space (view already applied) | Must be decided in §4.4 early or gizmos regress |
| Matching Video/Label artist expectations | Billboard-only may prefer camera-up; both-toggles prefer shared continuous up |
| Performance | Negligible (one quat + basis per FBO form per draw) |

Open product questions (resolve in 3.0 planning, not in 2.2):

1. Default up policy for Camera content: ContinuousUp vs WorldUp?
2. When only Billboard is on, should up match Camera content’s policy or stay camera-up?

---

## 8. Carry-over checklist (copy into BBS-CML-3.0)

When bootstrapping or scheduling this work in the 3.0 repo:

- [ ] Copy or link this file under `docs/architecture/` in [BBS-CML-3.0](https://github.com/ElGatoPro300/BBS-CML-3.0).
- [ ] Place domain orientation types next to other 3.0 math/quaternion utilities (no `net.minecraft` imports).
- [ ] Do not reintroduce “clear 3×3 + mul Euler view” as the only billboard path.
- [ ] Do not invent a second gizmo matrix path that ignores the blit orientation.
- [ ] Re-read 2.2 `FramebufferFormRenderer` only as behaviour reference (fill Unlit / lit blit / Iris notes are separate from pole continuity).

---

## 9. 2.2 reference (behaviour, not copy-paste)

Useful 2.2 anchors for agents porting intent:

- Data: `FramebufferForm.billboard`, `FramebufferForm.cameraContent` (+ legacy `look_at` → both true).
- Fill orientation: `applyLookAtContentOrientation` (world-up + zenith yaw hack).
- Blit orientation: `applyLookAtBillboard`.
- Gizmo alignment: replay look-at capture in `collectMatrices`; editor billboard parent from captured pre/post blit MV so `S0 · parent = billboardMV`.

These remain the **behavioural** checklist for 3.0; the **implementation** should follow §4–§5 on quaternions / continuous up.
