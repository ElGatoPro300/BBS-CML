# IK / Physics — master plan for BBS CML 3.0

> **Status (2026-09):** Deferred out of the 2.2 line. The current 2.2 IK/physics stack (Exocolt-era tree solver + MapType form blobs + form-editor panels) is considered too costly to keep patching. This document captures the investigation, decisions, and phased design so 3.0 can implement a cleaner IK/physics layer (UI closer to [bbs-fs](https://github.com/Wemppy4/bbs-fs), with room for CML improvements) without rediscovering the same ground.
>
> **Not in scope for 2.2 implementation** from this doc. Short-term 2.2 work may still *revert* toward pre-Exocolt IK for stability and `bbs_physics` CML compatibility (see [Git archaeology](#11-git-archaeology--revert-reference)).

Related:

- Greenfield repo: [BBS-CML-3.0](https://github.com/ElGatoPro300/BBS-CML-3.0)
- Upstream reference fork: [bbs-fs](https://github.com/Wemppy4/bbs-fs)
- Feature rules: `.cursor/rules/FEATURE-RULES-PROTOCOL.mdc` + `FEATURE-RULES-ARCHITECTURE.mdc`

---

## 1. Goals for 3.0

1. **Eliminate root causes** of the world-join freeze when morphs carry heavy/broken IK config.
2. **Ship a solid IK data + runtime model** aligned with bbs-fs (`FormBone` / tip→root chains / frame memo), not the CML MapType limb-blob adapter.
3. **UI** at least as elaborate as bbs-fs (bone tree, pickers, chain preview, joint DoF), with optional CML improvements.
4. **Physics addon policy (primary):** remain compatible with the **CML** build of `bbs_physics` (mixin on `DynamicBoneOrchestrator` + `ModelForm.springs`), *or* an equivalent 3.0 contract designed with that author — not by inventing a second fragile redirect surface.
5. Keep **CML film / pose tracks** as the animation surface unless 3.0 deliberately redesigns tracks (out of scope of the hybrid plan below).

Non-goals for the **primary** plan:

- Making the **bbs-fs** `bbs_physics` jar drop-in on CML without also porting FS pose API (`FormPoseEvents`, etc.). That path is documented only as an [appendix alternative](#10-appendix--alternative-full-fs-alignment-for-fs-bbs_physics).

---

## 2. Problems observed on 2.2 (motivation)

### 2.1 World-join freeze after applying IK in the form editor

- Preset / panel writes **`ModelForm.ik`** (`ValueData` MapType); morph NBT persists the whole form → rejoin always loads IK.
- Every draw: `ModelFormRenderer.applyIKOnce` → `LimbConstraintProcessor.process`.
- Deferred Iris overlays call `applyOverlayPosePipeline`, which **clears** `ikAppliedThisRender` and **re-solves IK** many times per frame.
- Compile path: `LimbConstraintCompiler.descendantChainLength` / `pickIkChild` use unguarded `while (true)` child walks (cycle hang or exponential cost on wide rigs).
- Cache defeat: `mergeIkMap` returns `map.copy()` every call → `WeakHashMap` compile cache keyed by `MapType` identity **always misses**.
- Solver cost: post-Exocolt **merged overlapping limbs** + `IKTreeSolver` (up to 64 iters, Cholesky) ≫ 2.1.x classic per-limb cost. Presents as freeze / `Can't keep up`, not always a crash dump.
- User workaround that restored join: roll back to **2.1.2** / pre-tree-stack behaviour (no usable heavy IK path).

### 2.2 `bbs_physics` CML crash (`InjectionError`)

Jar under test: `bbs_physics-1.1-cml-1.21.1.jar` (“BBS Physics Engine (**CML**) — will not work on plain BBS”).

```text
@Mixin(DynamicBoneOrchestrator.class)
@Redirect(method = "apply",
  at = @At(INVOKE, target = "Lmchorse/bbs_mod/settings/values/core/ValueData;get()Ljava/lang/Object;"))
→ ChainMute.filter(form, map) when value == form.springs
```

From commit **`629d43ba8`** (*IK and Physics by Exocolt*), `apply` stopped calling `springs.get()` in-place and used `resolveSpringsMap()` instead → **0 redirect targets** → startup / thumbnail-path `InjectionError`.

CML never had a class named `ModelPhysicsRuntime`; the mixin *class* name is historical. The live target is **`DynamicBoneOrchestrator`**.

### 2.3 Why “keep patching 2.2 spaghetti” lost

Further surgical fixes (guards, cache keys, overlay once-per-frame) could mitigate freeze without a full FS port, but the data model (`form.ik` blob), UI, and dual physics story keep accumulating debt. Decision: **design properly in 3.0**; stabilize 2.2 by simplifying/reverting the Exocolt-era surface where needed.

---

## 3. Architecture snapshot: CML 2.2 vs bbs-fs

| Layer | CML 2.2 (Exocolt-era) | bbs-fs |
|-------|----------------------|--------|
| IK data | `ModelForm.ik` / `ModelConfig.ik` MapType (`LimbConstraint*`) | `ModelForm.bones` → `FormBone` (`ikTarget`, `ikChainLength`, `IKControl`, `JointDoF`, …) |
| Compile | `LimbConstraintCompiler` (child walk down, dangerous) | `ModelIKCache` (walk **up** parents, cycle checks, per-frame memo) |
| Runtime entry | `LimbConstraintProcessor` → `SkeletonPoseWriter` | `ModelIKRuntime` → `ModelIKApplier` |
| Shared solver | `IKTree` / `IKTreeSolver` / `IKJoint` | Same family (shared lineage) |
| Physics data | `ModelForm.springs` MapType | `FormBone.physics*` + form `wind`; exchange IO |
| Physics runtime | `DynamicBoneOrchestrator` | `ModelPhysicsRuntime` + `ModelPhysicsCache` (+ `FormPoseEvents.CLAIM_CHAIN`) |
| Addon integration | Brittle mixin on `springs.get()` in `apply` | Official client API: `FormPoseEvents`, `FormPropertyAliases`, `BBSApi` (bbs ≥ 2.7) |
| Film tracks | CML pose / spring overrides | Per-bone animatable `IKControl` / `PhysicsControl` (different track story) |

**Shared asset:** the tree DLS solver is not the main differentiator; **data model, compile safety, caching, and pose hooks** are.

---

## 4. UI: CML IK panels vs bbs-fs (target UX for 3.0)

Both are “select a bone → edit its chain”, but FS is the richer baseline.

| Aspect | CML 2.2 UI | bbs-fs UI | 3.0 intent |
|--------|------------|-----------|------------|
| Shell | `UIFormPanel` + flat `UIStringList` | `UIBoneListFormPanel` + bone tree / markers | Prefer FS-like tree + markers; allow CML polish |
| Presets | Top buttons / context on list | Context on tree (`ModelIKManager`) | FS-like exchange format (`BoneIKIO`) |
| Writes | In-memory `LimbData` → `form.ik` blob | Live `FormBone` fields | `FormBone` only |
| Target | Button “controller bone” | `UIBonePicker` + cyclic-target gate | FS-like pickers + gates |
| Length | `depth` (1–32) | `chainLength` (0 = to root) + **live chain preview** | Prefer FS semantics + preview |
| Pole | Toggle + bone button | Toggle + picker + **pole angle** | FS-like |
| Scalars | `bendOffset`, `flexibility`, `influence` | `softness`, `weight` (`IKControl`) | FS naming; map carefully if keeping classic toggle |
| Tip / stretch | `orientTip`, `extensible` | `tipRotation`, `stretch` / `squash` | FS-like |
| Joint DoF | Mostly preserved in data; little form-panel UI | Full lock/limit/stiffness on **selected bone** (even non-tips) | Expose FS-like joint section |
| Enabled UX | Always show limb fields | Hide chain rows until enabled | Prefer FS progressive disclosure |

3.0 may improve beyond FS (better overlap warnings, classic-shape hints, etc.) without keeping the MapType limb editor.

---

## 5. `bbs_physics` jars (do not confuse)

| Jar | Role | Integration | Compatible with primary 3.0 plan? |
|-----|------|-------------|-----------------------------------|
| `bbs_physics-1.1-cml-1.21.1.jar` | CML fork | Mixin → `DynamicBoneOrchestrator.apply` + `ModelForm.springs` | **Yes — primary compat target** (keep call site / field contract or renegotiate with author) |
| `bbs_physics-1.1-1.21.1.jar` | FS / plain BBS (≥ 2.7 API) | Almost no physics-runtime mixins; `PhysicsApiIntegration` → `FormPoseEvents` (`CLAIM_CHAIN` uses **`FormBone`**) | Only under [appendix alternative](#10-appendix--alternative-full-fs-alignment-for-fs-bbs_physics) |

---

## 6. Primary recommendation for 3.0 — “Option C hybrid”

**Name used in planning chats:** Option **C′** (FS-aligned IK + constraints; CML physics addon contract preserved).

```text
Strong FS alignment on IK (+ constraints, FormBone)
+ Physics can grow FormBone.physics fields
+ Keep ModelForm.springs + DynamicBoneOrchestrator.apply shape for CML bbs_physics
+ Do NOT require FormPoseEvents / FS jar drop-in
+ Keep CML film tracks unless 3.0 redesigns them separately
```

### Why this is still the right 3.0 default

- Fixes freeze at the **data/compile/runtime** layer (same root causes identified on 2.2).
- Moves UI/data to the design FS already proved.
- Does not force a second physics-addon story or a full FS client API port just to ship IK.
- Leaves a clean door: if both forks later standardize on **one** physics addon (FS-style), see appendix.

### Explicitly rejected as 3.0 *primary* path

- “Only surgical 2.2 patches forever” (guards/cache/overlay) — useful as temporary 2.2 firefighting, not the 3.0 architecture.
- “Port only `IKTreeSolver`” — solver already shared; wrong bottleneck.
- “Full FS physics API + FS jar as the only story” — valid **alternative**, not the default (appendix).

---

## 7. Locked product decisions (from planning Q&A)

| Topic | Decision |
|-------|----------|
| Legacy `form.ik` morphs | **No migration.** If only the old blob exists → **IK silently off**. |
| Preset / model config export | Use **FS-like exchange** (`BoneIKIO`: `chains` / `bones` wrapper). |
| `FormBone.physics` | **Include** alongside retained **`springs`** when implementing the FS-like bone model (springs remain for CML addon / native spring map). |
| Delivery granularity | Prefer **one commit (or PR) per phase** when implementing. |
| Film tracks | Keep **CML track model**; do not require FS per-bone `IKControl` film tracks for the first 3.0 IK ship. |
| Constraints | Same wave as IK bone data (per-bone / FS-like), not a detached afterthought. |
| Model editor | Remains **defaults source** (`config.json` / model panel) that forms can import; form instance stays independent. |
| UI | Prefer **FS-like bone-centric** panels; CML may polish beyond FS. |

---

## 8. Phased blueprint (for 3.0 implementation)

Phases are logical; 3.0 greenfield may collapse some file ports, but **order of acceptance tests** should stay.

### Phase 0 — Physics CML contract (acceptance: CML `bbs_physics` loads)

- Ensure the public apply path still exposes `ValueData.get()` on `form.springs` **or** ship an agreed replacement API with the addon author.
- Keep `ModelForm.springs` as `ValueData` while CML jar remains supported.
- If 3.0 renames orchestrator classes, provide a **facade** with the historical FQCN/method shape the mixin expects, or coordinate a new addon build first.

### Phase 1 — Domain: `FormBone` / `ValueBones`

- Port/adapt: `FormBone`, `ValueBones`, `IKControl`, `JointDoF`, `ValueBoneIK`, constraints-on-bone, optional `physics*` fields.
- `ModelForm.bones` is the source of truth for new IK/constraints.
- No reader that upgrades old `form.ik` blobs (silent off).
- Presets ↔ `BoneIKIO`.
- UI skeleton: bone list/tree writing `FormBone`.

### Phase 2 — IK runtime

- `ModelIKCache` (parent walk, cycle skip, per-frame memo — e.g. `RenderFrame` epoch or 3.0 equivalent).
- `ModelIKRuntime` + `ModelIKApplier` (reuse / rehome `IKTreeSolver`).
- Classic path where needed (`ClassicLimbSolver` / equivalent).
- Apply CML track overrides on top without baking FS film IK tracks.

### Phase 3 — Render pipeline / freeze acceptance

- One IK solve per visible frame unless pose inputs actually change.
- Overlays must **not** blindly re-solve full IK.
- Remove MapType limb hot path from the default draw.

### Phase 4 — Physics internals behind the CML facade

- Optionally structure simulation like FS `ModelPhysicsCache` / runtime **behind** the CML-compatible entrypoint.
- Do not drop `springs` or break `ChainMute.filter` semantics without an addon update.
- `FormBone.physics*` may drive native chains *and/or* coexist with the springs map; document precedence in 3.0.

### Suggested acceptance checklist

- [ ] Join world with dense IK preset on player morph — stable FPS, no hang.
- [ ] Shaders ON/OFF; paint/glow overlays match base pass.
- [ ] `bbs_physics` CML jar loads; chain mute / ragdoll paths smoke-tested.
- [ ] Legacy morph with only `form.ik` → no IK, no crash.
- [ ] New preset round-trip via FS-like exchange.
- [ ] Model defaults import → form `bones`.

---

## 9. Multi-version / domain rules (still apply in 3.0)

- Domain (bones, IO, compile topology, math): **no** `net.minecraft.*` / Iris / Sodium.
- Adapters: render, collisions, mixins, MC matrices.
- Prefer one IK/physics pipeline (DRY); no parallel MapType + FormBone solvers both hot.
- No mutable `public static` render state for IK params.

---

## 10. Appendix — Alternative: full FS alignment for FS `bbs_physics`

> **Optional strategy** if the project later decides that **one** physics addon (the FS build) should serve both bbs-fs and CML, instead of maintaining CML-specific mixin compat.

### What the FS jar actually needs

- `bbs >= 2.7-` style API surface: `mchorse.bbs_mod.api.BBSApi`, `FormPropertyAliases`, client events especially **`FormPoseEvents`** (`MODEL_POSE`, **`CLAIM_CHAIN(ModelForm, IModel, FormBone)`**, `TRANSFORM`, `PARENT_FRAME`, `PIVOT_OFFSETS`, `ANCHOR`, `ACTOR_BEFORE`, …).
- Renderer / physics compile must **invoke** those events (as bbs-fs does from `ModelFormRenderer`, `ModelPhysicsCache`, etc.).
- `FormBone` with `physicsEnd` (and related fields) so `ChainMute.claims(...)` works.
- Minimal mixins on Form/ModelForm only (fields for ragdoll/chain), not `DynamicBoneOrchestrator` redirects.

### Implications

| Upside | Cost |
|--------|------|
| One addon story aligned with bbs-fs | Port/maintain FS client pose API in CML/3.0 |
| Clean claim/mute without bytecode redirects | CML `bbs_physics` jar becomes obsolete |
| Natural fit with `ModelPhysicsRuntime` naming | Larger blast radius (film matrices, anchors, UI events) |

### Relationship to the primary plan

The primary Option C′ **already** ports `FormBone` + IK cache/runtime. The appendix adds **pose-event API + physics runtime ownership model**. Do **not** treat appendix work as required to ship 3.0 IK; treat it as a **fork-unification** decision with the physics author.

---

## 11. Git archaeology — revert reference

Fill the “post-revert” rows when the 2.2 line is actually rolled back.

| Milestone | Hash (abbrev) | Full hash | Notes |
|-----------|---------------|-----------|--------|
| **IK/Physics by Exocolt** (introduces tree solver stack + `resolveSpringsMap` era) | `629d43ba8` | `629d43ba8ec9610e328078167a8146c808067a7f` | Message: *IK and Physics by Exocolt* (2026-09-06). Start of the “current” heavy IK/physics surface discussed in this doc. |
| **Parent of Exocolt** (last commit *before* that stack) | `037210c6c` | `037210c6cecee200dba27382bae7de22ee023548` | Behavioural baseline used when restoring classic `LimbConstraint*` / `LimbResolver` / `springs.get()`-in-`apply` for 2.2 stability. |
| Tag **2.1.2** (user-confirmed join OK without heavy IK workflow) | `468a09396` | `468a0939684680942861c8132451f81ba1ba4836` | Classic IK existed in reduced form; not “zero IK”. Useful behavioural baseline, not necessarily identical to Exocolt’s parent. |
| **Last commit that still contained the Exocolt-era IK stack** (before revert) | `f5b3d23ea` | `f5b3d23ea00c4b3e0a6b9fcd01f9102cc6fdb5d5` | Tip of `master` immediately before the surgical Exocolt-era revert (*Fix mob form highlighting…*, 2026-09-27). |
| **First commit where Exocolt-era tree IK is gone / classic path restored** | _(child of `f5b3d23ea`)_ | `git log -1 --grep="Revert Exocolt-era tree IK"` | Surgical revert commit on `master` (message: *Revert Exocolt-era tree IK to classic limb path for 2.2 stability.*). Removes `IKTree*` / form IK·physics panels / debug overlays; restores classic writer + `springs.get()` in `DynamicBoneOrchestrator.apply` for CML `bbs_physics`. |

### Suggested commands when reverting (for operators)

```bash
# Inspect what Exocolt changed
git show 629d43ba8 --stat

# Confirm parent
git log -1 629d43ba8^

# After revert lands, append hashes into the table above
git rev-parse HEAD
git log -1 --format="%H %ci %s"
```

Do **not** force-push shared branches without an explicit maintainer decision.

---

## 12. Reference file map (2.2 CML vs bbs-fs)

Useful when porting into 3.0 (paths as of the planning clone / CML tree):

**CML 2.2 (hot path):**

- `LimbConstraintProcessor`, `LimbConstraintCompiler`, `SkeletonPoseWriter`, `LimbResolver`
- `cubic/ik/solver/IKTreeSolver` (+ `IKTree`, `IKJoint`)
- `DynamicBoneOrchestrator`, `SpringChainCompiler`
- `UIModelIKFormPanel`, `UIModelIKPanel`, `UIModelPhysicsFormPanel`, `UIModelConstraintsFormPanel`
- `ModelForm.ik` / `.springs` / `.constraints`

**bbs-fs (target shapes):**

- `FormBone`, `ValueBones`, `BoneIKIO`, `BonePhysicsIO`
- `ModelIKCache`, `ModelIKRuntime`, `ModelIKApplier`, `ClassicLimbSolver`
- `ModelPhysicsRuntime`, `ModelPhysicsCache`
- `api/client/events/FormPoseEvents` (appendix only for CML)
- `UIModelIKFormPanel` (extends `UIBoneListFormPanel`)

---

## 13. Document history

| Date | Change |
|------|--------|
| 2026-09-28 | Initial master plan from 2.2 investigation + Option C′ decisions; IK implementation deferred to 3.0; appendix for FS-addon unification; git markers for Exocolt / pending revert. |

When 3.0 implementation starts, prefer updating **this** file (or splitting phase checklists into linked ADRs) rather than re-deriving from chat logs.
