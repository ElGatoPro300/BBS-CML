---
name: "Revert Render Depth (quirúrgico)"
overview: "Eliminar la feature Render depth / Profundidad de renderizado (datos, track, UI, fade/sort/oclusión y defer por film depth), conservando intacta la cola post-deferred de soft-opacity Iris."
todos:
  - id: phase-0-inventory
    content: "Fase 0: congelar inventario y criterios keep/drop (este plan)"
    status: completed
  - id: phase-1-data-ui
    content: "Fase 1: quitar ValueFloat/Boolean, UI General, track film, strings, UIFloatKeyframeFactory"
    status: completed
  - id: phase-2-core
    content: "Fase 2: eliminar FormRenderDepth + cableado BaseFilmController / contexts"
    status: completed
  - id: phase-3-renderers
    content: "Fase 3: limpiar FormRenderer + Model/Billboard/Shape/Extruded (fade, sort, body-part depth)"
    status: completed
  - id: phase-4-opacity-patch
    content: "Fase 4: ShaderOpacityPatch — quitar solo filmRenderDepth; conservar soft-opacity"
    status: completed
  - id: phase-5-compat
    content: "Fase 5: carga legacy — ignorar/borrar render_depth* al fromData (sin migración activa)"
    status: completed
  - id: phase-6-verify
    content: "Fase 6: verificación in-game (film multi-actor, soft opacity, billboards, body parts)"
    status: pending
isProject: true
---

# Revert quirúrgico: Render depth / Profundidad de renderizado

> **Estado:** Fases 1–5 implementadas; queda verificación in-game (Fase 6).  
> **No** es un `git revert` ciego del commit QOL: el depth está entremezclado con soft-opacity y fixes posteriores.  
> **Introducción:** `098868f3b` (*added +50 QOL features*, ítem 25) → consolidado en `FormRenderDepth.java` vía `22c99fdf8` / merges.  
> **Parche relacionado a deshacer con la feature:** `56dbeafb7` (*fix(render-depth): …*) en la parte de depth; **conservar** cualquier cambio de ese commit que solo mejore transparencia de billboards **si** sigue haciendo falta tras quitar depth (revisar caso a caso en Fase 3).

---

## Objetivo

Quitar por completo:

- Property / track `render_depth` y flag `render_depth_enabled`
- UI “Profundidad de renderizado” (General + keyframe factory)
- Fade entre actores, sort por profundidad de film, oclusión por occluders
- Defer post-deferred **solo** cuando la razón era “film render depth” en actores opacos

**Mantener:**

- Soft-opacity (`color.a` &lt; ~1) → cola `ShaderOpacityPatch` post-deferred / after fluids
- Paint / Glow / Color transforms y resto del sistema de color actual
- Orden natural de dibujo del engine (sin capa BBS de depth de film)

---

## Decisiones

| Tema | Decisión |
|------|---------|
| Método | Rollback **quirúrgico** (editar archivos), no `git revert 098868f3b` |
| Soft-opacity Iris | **Conservar** (`shouldDelayUntilPostDeferred(alpha)` por alpha) |
| Param `filmRenderDepth` / `shouldJoinPostDeferredQueue` | **Eliminar** o dejar no-op (`false`) |
| Campo `sortDepth` en cola post-deferred | **Conservar** el parámetro de ordenación, pero dejar de alimentar con `Form.renderDepth` (usar `0` o solo `distanceSq`) |
| Datos en morphs/films antiguos | Al cargar: **ignorar/strip** `render_depth` / `render_depth_enabled` (no migrar a otra property) |
| `BODY_PART_RENDER_DEPTH = false` | Desaparece con la clase; body parts vuelven a path simple |
| Track huérfano en films | Puede quedar en JSON; UI no lo ofrecerá; `applyProperty` no encontrará property → no-op (opcional: borrar en `FormProperties` como se hizo con `opacity`) |

---

## Mapa keep vs drop

### DROP (feature render depth)

| Pieza | Ubicación |
|-------|-----------|
| Clase core | `src/client/java/.../film/FormRenderDepth.java` (**borrar archivo**) |
| Datos form | `Form.renderDepth`, `Form.renderDepthEnabled` + `add()` + `stripLegacyDefaultRenderDepthEnabled` |
| Film collect/frame | `BaseFilmController` (`currentRenderDepthOccluders`, `collectOccluders`, `renderDepthFrame(...)`) |
| Contexts | `FilmControllerContext.renderDepthFrame`, `FormRenderingContext.renderDepthFrame` |
| Fade en draw | `FormRenderer` (~líneas con `getFade` / early return / alpha multiply) |
| Body-part sort | `FormRenderer` / `ModelFormRenderer` (`renderDepthSortedBodyParts`, `BODY_PART_RENDER_DEPTH` branches) |
| Defer por occluders | `ModelFormRenderer.deferForRenderDepth` / `needsDeferredDepthOcclusion` |
| Sort key desde form | `FormRenderDepth.resolveSortDepth(...)` en Billboard / Shape / Extruded / Model submits |
| UI General | `UIGeneralFormPanel` trackpad + toggle |
| Track UI | `UIReplaysEditor` (`COLORS`/`ICONS`/`MODEL_PROPERTIES`/label/orden), `UIFloatKeyframeFactory` toggle especial |
| i18n | `UIKeys` + `en_us` / `es_es` / `fa_ir` keys `render_depth*` |

### KEEP (no tocar salvo desacoplar)

| Pieza | Nota |
|-------|------|
| `ShaderOpacityPatch` cola post-deferred | Soft opacity, depth write, after fluids, flush |
| `shouldDelayUntilPostDeferred(alpha)` | Solo por alpha; **sin** segundo arg film depth |
| `submitPostDeferredForm(sortDepth, distanceSq, ...)` | API puede quedarse; callers pasan `0D` / distance |
| Transparencia billboard / depthMask por alpha | Si venía de `56dbeafb7` y no depende de `renderDepthEnabled`, evaluar conservar |
| `BBSRendering` helpers de opacity | No ligados a FormRenderDepth |

---

## Fases de implementación

### Fase 0 — Inventario (este documento)

- Criterios keep/drop acordados.
- Referencia de commits: `098868f3b`, `22c99fdf8`, `56dbeafb7`.

### Fase 1 — Datos + UI + track

1. Quitar de `Form.java`: `renderDepth`, `renderDepthEnabled`, registro, `stripLegacyDefaultRenderDepthEnabled`.
2. En `fromData`: si llegan esas keys, **removerlas** del map (compat silenciosa).
3. `UIGeneralFormPanel`: quitar campos y filas de options.
4. `UIReplaysEditor`: quitar `render_depth` de listas/colores/iconos/labels/orden.
5. `UIFloatKeyframeFactory`: quitar bloque especial del toggle `renderDepthEnabled`.
6. Strings + `UIKeys`.

**Criterio de hecho:** no aparece “Profundidad de renderizado” en General ni track en film editor; compile OK.

### Fase 2 — Core film

1. Borrar `FormRenderDepth.java`.
2. `BaseFilmController`: eliminar collect de occluders y construcción de `Frame`; no pasar frame al context.
3. Limpiar `FilmControllerContext` / `FormRenderingContext` (campo + builder + reset).

**Criterio de hecho:** ningún import de `FormRenderDepth` en film/.

### Fase 3 — Renderers

1. `FormRenderer`: quitar fade por occluders; quitar nulling/`renderDepthSortedBodyParts`; body parts siempre path simple.
2. `ModelFormRenderer`: quitar `deferForRenderDepth`; `deferTranslucentModel` solo por opacity/render-depth-frame **ajeno** (opacityDefer / render-depth-of-queue ya no); submits usan `sortDepth = 0` o solo distance; borrar `renderDepthSortedBodyParts`.
3. `BillboardFormRenderer` / `ShapeFormRenderer` / `ExtrudedFormRenderer`: quitar `resolveSortDepth`; no condicionar depth-write a `renderDepthEnabled` (ya parcialmente así — dejar solo opacity).
4. Grep final: cero referencias a `FormRenderDepth`, `renderDepthFrame`, `render_depth`, `renderDepthEnabled`.

**Criterio de hecho:** film con varios actores semi-transparentes se dibuja sin fade forzado por “capas” BBS.

### Fase 4 — ShaderOpacityPatch (desacople fino)

1. `shouldDelayUntilPostDeferred(float alpha, boolean filmRenderDepth)` → eliminar overload o forzar `filmRenderDepth` ignorado; callers con `true`/`shouldJoinPostDeferredQueue` pasan a solo-alpha o se eliminan.
2. Comentarios que hablen de “opaque film actors share sorted depth queue” → actualizar.
3. **No** cambiar `LIVE_DEPTH_WRITE_ALPHA`, after-fluids, ni flush.

**Criterio de hecho:** soft opacity Iris sigue diferida; actor opaco **no** entra a post-deferred solo por depth.

### Fase 5 — Compat films/morphs

1. Strip en `Form.fromData` (Fase 1).
2. Opcional: en `FormProperties` / apply, si queda canal `render_depth` en properties del film, borrarlo al cargar (mismo patrón que `opacity` huérfano) para no ensuciar UI.

### Fase 6 — Verificación in-game

Checklist:

- [ ] Film con 2+ actors opacos: sin agujeros / see-through nuevos
- [ ] Actor con `color.a` &lt; 1 (soft opacity) + Iris: agua y sin X-ray de limbos
- [ ] Billboard / label / shape en body part: transparencia estable
- [ ] Model form en editor (UI preview): sin regresiones de opacity
- [ ] Morph guardado antiguo con `render_depth` / `render_depth_enabled`: carga sin error; valores ignorados
- [ ] Track list: no ofrece `render_depth`
- [ ] Compile `compileClientJava` + smoke `runClient`

---

## Orden recomendado de PRs / commits (si se parte)

1. **UI + data strip** (Fase 1 + 5) — bajo riesgo visual  
2. **Core + renderers** (Fase 2–3) — quita comportamiento  
3. **Opacity patch desacople** (Fase 4) — revisar Iris con cuidado  
4. Commit de limpieza strings / docs si hace falta  

Mensaje de commit sugerido (cuando se pida):

```
Revert render depth feature (track, fade/sort, film occluders)

Remove form render_depth / render_depth_enabled and FormRenderDepth
layering while keeping soft-opacity post-deferred draws intact.
```

---

## Riesgos

| Riesgo | Mitigación |
|--------|------------|
| Romper soft-opacity al tocar `ShaderOpacityPatch` | Fase 4 mínima; siempre `filmRenderDepth=false` primero y probar Iris |
| Callers que pasaban `shouldDelayUntilPostDeferred(alpha, true)` | Grep y sustituir por overload de un solo arg |
| Films con keyframes `render_depth` | Strip/ignore; no reintroducir property |
| Regresión billboards de `56dbeafb7` | Diff ese commit; conservar depthMask/transparency fixes no ligados al Value |

---

## Fuera de alcance

- Reimplementar otra forma de ordenación de translúcidos
- Cambiar Paint/Glow/Color transforms
- Revertir el temporal Blend Color / Opacity track (ya hecho en otro plan)

---

## Grep de cierre (obligatorio antes de dar por terminado)

```text
FormRenderDepth
renderDepthFrame
renderDepthEnabled
render_depth
FORMS_EDITORS_GENERAL_RENDER_DEPTH
needsDeferredDepthOcclusion
resolveSortDepth
getFade(  // solo usos de FormRenderDepth
filmRenderDepth
shouldJoinPostDeferredQueue
```

Todo debe quedar vacío o solo en este plan / historial git.
