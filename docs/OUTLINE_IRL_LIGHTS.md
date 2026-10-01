# Outline vs IRLights (addon) — deferred fix

**Status:** investigated; fix lives primarily in the IRLights CML port addon — not applied here yet.  
**Addon repo:** [bbs-irlights-addon-cml-port](https://github.com/ElGatoPro300/bbs-irlights-addon-cml-port)  
**Local trees (dev):** `../bbs-irlights-addon-cml-port`, `../irl-core`

Related BBS code:

- `FormOutlinePass` / `FormOutlineRenderer` / `ModelOutlineFramebufferCache`
- `FormRenderingContext.isShadowPass` (already skips outline when `true`)
- Iris shadow path already skipped via `BBSRendering.isIrisShadowPass()`

---

## Symptom

With **at least one form that has silhouette outline enabled** and **at least one IRLights point/spotlight form** in the scene, stray / broken outlines appear near the light (focus / radius), not only on the outlined form.

---

## Root cause (summary)

Outline and IRLights do **not** share the same named FBO (`outline_mask_32f` vs IRLights depth atlases). They collide because outline treats **whatever framebuffer is currently bound** as:

1. Depth blit **source**
2. Composite **destination**

IRLights shadow bake (`ShadowBaker` → `ShadowRenderer`) binds its **depth-only atlas FBO**, then re-draws BBS forms as occluders via `IRLiteBbsCasterSource` while `ShadowBakeState.isBaking() == true`.

Those caster draws build a normal `FormRenderingContext` **without** `isShadowPass = true`.  
`FormOutlinePass.shouldSkip` only gates on `context.isShadowPass` / Iris shadow / picking — **not** on `ShadowBakeState`. So outline still runs into / against the light’s shadow atlas (wrong depth size/content, light-space matrices) → ghost outlines near lights.

`ShadowBakeState` is already checked by `AbstractLightFormRenderer` (skip light re-registration). Light forms themselves do **not** call `FormOutlinePass`; the bug is **outlined casters** re-rendered during the bake.

---

## Recommended fix (addon-side, preferred)

Reuse the existing BBS contract already used by `ModelBlockEntityRenderer` / `FramebufferFormRenderer`:

```java
formContext.isShadowPass = true;
```

### 1. Model blocks + film replays (direct)

File: `src/client/java/org/qualet/irl/light/shadow/IRLiteBbsCasterSource.java`

In `drawModelBlock` and `drawReplay`, after creating the context:

```java
FormRenderingContext ctx = new FormRenderingContext()
    .set(/* existing args */)
    .camera(camera);
ctx.isShadowPass = true;
renderer.render(ctx);
```

`FormOutlinePass.shouldSkip` will then skip outline for those bake draws. Other shadow-pass early-outs in form renderers (paint overlays, soft, etc.) are desirable for depth baking.

### 2. Living / player morph casters (MorphRenderer path)

`drawEntity` calls `MorphRenderer.renderPlayer` / `renderLivingEntity`, which allocate their **own** `FormRenderingContext` inside BBS and never see the addon flag.

Pick one:

| Option | Where | Notes |
|--------|--------|--------|
| **A (preferred if morph shadows matter)** | Addon `drawEntity` | When morphing, draw the form the same way as `drawReplay` (`FormUtilsClient.getRenderer` + `FormRenderingContext` with `isShadowPass = true`) instead of/before MorphRenderer for the **bake only**. Keep MorphRenderer for the normal world path. |
| **B** | BBS `MorphRenderer` | Add an overload / flag to mark shadow bake when calling from addons. Touches CML; only if A is awkward. |
| **C** | BBS `FormOutlinePass` | Also skip when `ShadowBakeState.isBaking()` (optional class present). Couples BBS → irl-core; avoid unless needed as a safety net. |

Minimum viable fix for many scenes: **§1 alone** (model blocks + replays). Add **§2A** if morph actors still show the bug.

---

## What not to do in BBS (unless needed)

- Do **not** merge IRLights depth atlases into `outline_mask_*`.
- Do **not** require a hard compile dependency from BBS on `irl-core` just for this.
- Avoid broad Refactors of `FormOutlineRenderer` FBO save/restore unless §1–§2 prove insufficient (state leaks after bake).

Optional BBS hardening later (separate PR): document that any offscreen form re-draw must set `isShadowPass` (same rule as Iris/model-block shadows).

---

## Verification checklist (addon)

1. Form with outline + nearby point light / spotlight (shadows ON) — no ghost outlines at the light.
2. Same with shadows OFF on the light — still clean (regression).
3. Outline still correct on the form in the main world view (vanilla + Iris).
4. Light still casts shadows from model blocks / replays / morphs as before.
5. Film editor + world model blocks both tested.
6. Iris on/off.

---

## Quick file map

| Layer | Path |
|-------|------|
| Addon caster draw | `bbs-irlights-addon-cml-port/.../IRLiteBbsCasterSource.java` |
| Bake flag (lights only today) | `irl-core/.../ShadowBakeState.java` |
| Atlas FBO bind | `irl-core/.../ShadowRenderer.java`, `DepthTileAtlas.java` |
| Outline skip gate | BBS `FormOutlinePass.shouldSkip` |
| Outline FBO + depth blit | BBS `FormOutlineRenderer` |
