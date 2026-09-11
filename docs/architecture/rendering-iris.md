# Rendering / Iris compatibility

When implementing renderers, follow these rules for Iris/shaders compatibility.

## 1. Shadow pass check

Avoid rendering 2D elements or complex transparency during shadow passes.

```java
if (BBSSettings.isIrisShadowPass()) return;
```

## 2. State management

Always push/pop the matrix stack and restore render state.

```java
RenderSystem.enableBlend();
// ... draw ...
RenderSystem.disableBlend();
```

## 3. Shader attributes

Check `BBSRendering.isIrisShadersEnabled()` before using custom vertex attributes (e.g. tangents) that might crash vanilla shaders.

## 4. World vs GUI rendering

Use `BBSRendering.isRenderingWorld()` to distinguish world vs GUI, and adjust lighting (GUI typically needs fake lighting).

## Actor Mode note

For actor replays, stub bodies must not cast a second shadow — see `docs/architecture/actor-mode.md` (`ShadowRendererMixin`).

## 5. Film replay lighting isolation (ModelForm / NeoForge)

Film stub renders sort by **camera distance** (far → near) for translucency. ModelForm draws enable/disable the lightmap and can leave diffuse / Sampler2 / `shaderColor` dirty. On **Fabric** that leak is often invisible; on **NeoForge via Sinytra Connector** the next ModelForm can pick up slightly wrong lighting. Moving the camera or another replay changes draw order, so the “bad” lighting can jump between actors (rotation alone usually does not).

**Why it did not show up in ~2.0:** films iterated replays in map order (no distance sort), and ModelForm’s lightmap/equipment path was much thinner — less dirty state and no camera-driven reorder.

**Mitigation (keep Fabric visuals unchanged):** in `BaseFilmController#render`, when not an Iris shadow pass, call `BBSRendering.prepareVanillaEntityLighting()` before each replay and `BBSRendering.restoreWorldRenderState()` after (plus a final prepare after the loop). Do **not** “fix” this by changing packed world light sampling or `LightingSettings` semantics unless the bug is actually a track/migration issue.

Related pause/HUD darkness (model-block in hotbar): same GL-state family.
* **1.21+:** `restoreAfterGuiItemForm` (re-enable lightmap) + `prepareMenuBackgroundState` before `GameRenderer.renderBlur` + `prepareWorldPresentState` at world-begin.
* **1.20.1 / 1.20.4:** no menu blur — use `preparePauseScreenState` before `Screen.renderBackground` / `renderInGameBackground` (blend stays on for the gradient), plus the same HUD / world present restores. Do not inject `renderBlur` on these versions (`defaultRequire: 1` would fail boot).

## Related

* Forms: `docs/architecture/forms.md`
* ModelForm pipeline: `docs/architecture/model-form.md`
* Replays / film stubs: `docs/architecture/replays.md`
* Opacity / limb docs under `docs/` (`LIMB_TRANSPARENCY.md`, `SOFT_OPACITY_*.md`, …)
