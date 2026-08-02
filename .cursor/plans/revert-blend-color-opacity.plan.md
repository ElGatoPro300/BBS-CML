---
name: Revert Blend Color + Opacity track (temporal)
overview: Revertir commits SIRSYP inestables post-a75c46b y luego deshacer Blend Color + Opacity track, volviendo al color tradicional (RGB+alpha en un solo sitio), manteniendo Paint / Bright / Color Grade / Shape+transforms y el color picker nuevo con slider de transparencia.
todos:
  - id: phase-a-sirsyp
    content: "Fase A: revertir commits SIRSYP post-a75c46b (excepto limpieza 757b07335)"
    status: completed
  - id: phase-b-blend
    content: "Fase B: rollback quirúrgico Blend Color + Opacity track → color tradicional"
    status: completed
  - id: phase-c-ui
    content: "Fase C: UI Paint/Bright/Grade reorganizada (swatch clásico + Advanced); labels Forma/Hueso; Glow ES"
    status: completed
  - id: phase-d-verify
    content: "Fase D: verificación (código + compile OK; smoke visual in-game pendiente del usuario)"
    status: completed
isProject: true
---

# Revert temporal: Blend Color + Opacity track

> **Estado:** Fases A–D cerradas a nivel de implementación. **Fase D (2026-07-25):** auditoría de código + `compileClientJava`/`compileJava` OK. Confirmación visual final in-game queda como smoke del usuario.  
> Fase C: layout clásico Color (swatch + Resplandor + Avanzado), orden pose textura/color encima del grid, labels Forma/Hueso, paneles editor ~A50, ES Glow→Resplandor.  
> **Punto de reinicio:** `dae343357` (= árbol de `7334340d5`).  
> **Base estable:** `a75c46b6c4c2e3603dadec8e92cc948c70df2bfb` (*Remove 170º limit from fish eye effect*).  
> **Introducción del sistema a deshacer:** `c48885dd958fcb133368e54db9d9b563a6b2ff4d` (*New color and opacity*), completado en UI por `a5a1577ba` → `2d244a9fd` (ya incluidos en el beta estable).

---

## Objetivo

Volver a un sistema de color **tradicional y estable**:

- **Un solo sitio** para color + opacidad: `color` con `color.a` = transparencia real.
- **Quitar** el track `opacity` independiente y la semántica Blend Color (`color.a` = intensidad de tint / `applyBlendIntensity()`).
- **Quitar** migración/compatibilidad blend↔opacity con versiones antiguas (ya no hace falta).
- **Mantener** a primera instancia: Paint Color, Bright/Glow, Color Grade, Shape + transforms.
- **Mantener** el estilo del color picker nuevo (modos de selección), **añadiendo de nuevo** el slider de transparencia/alpha que Blend Color quitó.
- **Ajustar UI** de Paint / Bright / Grade para que sea user-friendly y coherente con el resto de interfaces (sin el layout “blend-centric”).

Los parches posteriores que solo existen para sostener Blend+Opacity **se pueden deshacer**; lo prioritario es que el color anterior vuelva a funcionar.

---

## Decisiones acordadas

| Tema | Decisión |
| --- | --- |
| Commits SIRSYP post-`a75c46b` | Revertir (introdujeron bugs) |
| `757b07335` (limpieza dumps) | **Conservar** |
| `58a86aee4` (Color Grade / compatible saves / UI color) | **Revertir** (ligado al color moderno; se rompería al quitar Blend) |
| `56dbeafb7` (render-depth / billboards) | **Conservar** (necesario para transparencia con body parts) |
| `a22ab2dba` / `f5ba4cff0` | **Conservar** (i18n + world films browser) |
| Features ajenas al color (equipamiento model blocks, cielo cromático, etc.) | **Conservar** si no vienen de los commits SIRSYP a revertir |
| Paint / Bright / Color Grade / Shape+transforms | **Conservar** datos/render; reajustar UI |
| Compatibilidad blend+opacity ↔ formatos antiguos | **Eliminar** |
| Método | Fase A = reverts de commits; Fase B = rollback **quirúrgico** (no `git revert` ciego de `c488`/`a5a`/`2d2`) |

---

## Contexto técnico (referencia rápida)

### Antes de Blend Color (`c48885dd9^`)

- `color.a` = opacidad del form.
- Paint / Glow ya existían como sistemas aparte (`paintSettings` / `paintColor`, `glowSettings` / `glowingColor`).
- Shape / effect transforms vivían en `Color.transform`.

### Con Blend Color (desde `c48885dd9`)

- `color.a` = intensidad del tint (Blend Color).
- `Form.opacity` + track `opacity` = transparencia real.
- `Color.applyBlendIntensity()` / `copyWithBlendIntensity()`.
- UI: `UIFormColorKeyframeFactory`, rediseño fuerte de `UIColorPicker`, paneles con Opacity aparte.
- Migración al cargar: `color.a` legacy → canal `opacity`.

### Por qué no basta revertir solo `c488` / `a5a` / `2d2`

Esos commits ya están **dentro** de `a75c46b`. Además, entre ellos y HEAD hay muchos fixes (sobre todo ElRedstoniano) y UI acoplados a Opacity track + blend. Un `git revert` puro de esos tres commits choca con historial posterior. Hace falta rollback quirúrgico de semántica + UI + render + migración.

---

## Fase A — Revertir SIRSYP post-`a75c46b` (con excepciones conservadas)

**Meta:** dejar el árbol lo más cerca posible del comportamiento post-`a75c46b` respecto a los cambios inestables de SIRSYP, **conservando** los commits listados en “Conservar” (limpieza, i18n, world browser, y el fix de render-depth necesario para transparencia).

### Commits a revertir (orden sugerido: del más reciente al más antiguo, omitiendo merges vacíos si el revert ya los cubre)

Revertir en orden inverso al de aplicación (más nuevo primero), resolviendo conflictos cuando aparezcan:

| Hash | Mensaje | Notas |
| --- | --- | --- |
| `58a86aee4` | fix(color): Color Grade / compatible saves / UI color | Ligado a color moderno → revertir |
| `ad1b4abae` | Animated ui panels | Revertir |
| `8471eb1c0` | Merge branch `master` … | Merge; valorar si hace falta revert explícito |
| `fda424b80` | feat(morph): previews / form list UX | Revertir (bugs + UI) |
| `7cdd40eb6` | feat(ui): polish form editor panels / overlays / keyframes | Revertir (incluye UI opaca / polish dudoso) |
| `781cc1541` | feat(film): letterbox/color/vignette clip panels | Revertir |
| `7a6ec494f` | fix(render): form/morph + shader opacity | Revertir (opacity stack); candidato a reaplicar si UI soft-opacity falla |
| `efd90d471` | fix(pose): BOBJ pose pivot / gizmo → General | Revertir |
| `0b63a91c8` | feat(ui): animate collapsible sections Color/Glow | Revertir |
| `9b6696908` | feat(morph): speed up form picker | Revertir |
| `ae8b38fd6` | feat(structure-picker): polish modes UI | Revertir |

### Conservar

| Hash | Mensaje | Motivo |
| --- | --- | --- |
| `757b07335` | chore: remove tracked local temp/debug frame dumps | Solo limpieza; no introduce bugs de runtime |
| `a22ab2dba` | chore(i18n): update EN/FA strings and related settings labels | Traducciones / labels útiles para el proyecto |
| `f5ba4cff0` | feat(world): improve world films browser and dashboard world menu | Mejora de UI world/films que conviene mantener |
| `56dbeafb7` (+ merge `257364f1c`) | fix(render-depth): nested billboards / legacy depth flags | **Necesario:** revertirlo reintroduce actores invisibles con body parts / soft-opacity en film |

### Commits de otros autores después de `a75c46b`

No forman parte del “lote SIRSYP a revertir” (p. ej. ElGatoPro300, ElRedstoniano: swimming/shield, panel resizing, prototype rules, etc.). Se mantienen salvo que un conflicto de merge obligue a tocarlos al aplicar los reverts.

### Criterio de éxito Fase A

- Los cambios de SIRSYP listados en “a revertir” ya no están en el working tree.
- Los commits de “Conservar” siguen aplicados (incl. `56dbeafb7`, i18n, world browser, limpieza).
- El proyecto compila (`./gradlew build` o al menos compilación client/main).
- ~~No se ha empezado aún el rollback de Blend Color (eso es Fase B).~~ **Hecho → Fase B.**

---

## Fase B — Rollback quirúrgico Blend Color + Opacity track

**Meta:** color tradicional otra vez; sin track Opacity; sin compatibilidad blend↔opacity.

### B1 — Modelo de datos

- Quitar (o dejar de usar) `Form.opacity` / `ValueFloat("opacity")`.
- Restaurar semántica: `color.a` = opacidad del form.
- Eliminar migración al cargar (`color.a` → canal `opacity`) en `Form` / `FormProperties`.
- Eliminar canal keyframe `opacity` de properties de replay (o dejar de registrarlo / aplicarlo).
- Eliminar flags/rutas de compatibilidad “compatible save” específicas de Blend Color + Opacity (incl. restos de `58a86aee4` si quedaran).

### B2 — Semántica `Color`

- Quitar o dejar de llamar `applyBlendIntensity()` / `copyWithBlendIntensity()` en el pipeline de forms.
- `color.a` deja de significar “intensidad de tint”.

### B3 — Render

- Forms (`Model`, `Billboard`, `Block`, `Extruded`, `Item`, `Shape`, `Structure`, `Trail`, etc.): dejar de combinar opacity track × blend intensity.
- Sombras / Iris / `ShaderOpacityPatch` / `FormColorBlend`: adaptar a alpha tradicional; se pueden deshacer parches que solo existían para el split opacity/blend.
- Model blocks: corregir inconsistencias de transparencia al volver a un solo canal alpha.
- **No eliminar** pipelines de Paint / Glow / Color Grade / Shape transforms; solo desacoplarlos de Blend Color.

### B4 — UI de forms / keyframes

- Sustituir paneles “Blend Color + Opacity aparte” por color tradicional (RGB + alpha).
- Quitar controles del track Opacity en General / Label / etc. donde se añadieron por este sistema.
- Restaurar uso de factories de color tradicionales donde proceda (`UIColorKeyframeFactory` vs `UIFormColorKeyframeFactory`), manteniendo acceso a Paint / Bright / Grade / Shape de forma ordenada (detalle en Fase C).

### Criterio de éxito Fase B

- Editar color de un form cambia tint + transparencia en el mismo color (alpha).
- No hay track Opacity funcional en timeline de forms.
- Films/forms cargan sin depender de migración blend→opacity.
- Paint / Bright / Grade / Shape siguen existiendo a nivel de datos.

---

## Fase C — UI user-friendly (Paint / Bright / Grade + color picker)

**Meta:** que lo conservado se vea y use bien, sin el diseño “blend-centric”.

### C1 — Color picker

- Conservar los **nuevos modos de selección de color**.
- **Reañadir** el slider de transparencia/alpha que Blend Color eliminó.
- El alpha del picker escribe `color.a` (opacidad real).

### C2 — Paint / Bright / Color Grade / Shape

- Mantener features.
- Reorganizar UI para estilo similar al resto de paneles BBS (labels, collapses, densidades coherentes).
- Evitar duplicar “intensidad de blend” en el color principal; intensidad de Paint/Glow sigue en sus propios campos.

### C3 — Preview / overlays

- Deshacer UI de preview con fondos opacos poco meditados si aún quedara tras Fase A (p. ej. restos de polish en form list / overlays), volviendo a preview más transparente/user-friendly cuando aplique.

### Criterio de éxito Fase C

- Color picker nuevo + slider alpha usable.
- Paint / Bright / Grade / Shape accesibles y legibles.
- No reaparece el layout de Blend Color + Opacity track.

---

## Fase D — Verificación

Checklist mínima in-game (`./gradlew runClient`):

- [x] Form Model: color + alpha en un solo control; transparencia visible. — **código:** `UIModelFormPanel` + `.withAlpha()`; `color.a` vía `getFormOpacity`/`applyFormOpacity`; `applyBlendIntensity()` es no-op.
- [x] Model blocks: transparencia coherente (sin el bug del Opacity track). — **código:** sin `Form.opacity`; model blocks usan el mismo path de form renderers + `applyFormOpacity`.
- [x] Billboard / Shape / Block / Item: mismo comportamiento de color+alpha. — **código:** paneles con `.withAlpha()`; renderers llaman `applyFormOpacity`.
- [x] Paint Color funciona (positivo/negativo si aplica). — **código:** `paintColor`/`paintSettings` + UI `UIFormColorLayout` presentes en Model/Billboard/Shape/Block/Item/Extruded/Trail.
- [x] Bright/Glow funciona. — **código:** `glowingColor`/`glowSettings` + sección Resplandor en layout.
- [x] Color Grade / Shape transforms no crashean. — **código:** grade bake en `copyWithBlendIntensity()`; transforms en Color; compile OK.
- [x] Sombras en suelo con alpha ≠ 1 (vanilla + Iris si es posible). — **código:** `FormColorBlend` documenta alpha tradicional; soft-opacity queue intacta. *Iris visual: smoke usuario.*
- [x] Timeline: no aparece / no aplica track Opacity de forms. — **código:** `opacity` no está en `MODEL_PROPERTIES`; huérfanos se strippean en `FormProperties`.
- [x] Guardar/cargar form/film no reintroduce migración blend/opacity. — **código:** `Form.toData()` hace `map.remove("opacity")`; al cargar, merge one-shot opacity→`color.a` y elimina el canal.

**Build:** `compileClientJava` + `compileJava` OK (2026-07-25).

**Notas / restos cosméticos (no bloquean):**
- Renombrado posterior: `FormColorBlend` → `FormColorEffects`; `copyWithBlendIntensity*` → `copyBakingColorGrade` / `copyDeferringColorGrade`. El campo serializado legacy `blend_a` se mantiene solo para migración.
- Plan histórico sigue usando el nombre “Blend Color” para referirse al sistema revertido.

**Smoke visual recomendado (usuario):** abrir un Model con `color.a` &lt; 1, un model block, Paint/Glow/Grade, y un film viejo con track `opacity` legacy — confirmar a ojo que no vuelve el layout Blend+Opacity.

---

## Orden de ejecución (estricto)

```
1. Fase A  → reverts SIRSYP (excepto 757b07335)
2. Fase B  → rollback quirúrgico Blend + Opacity
3. Fase C  → UI picker alpha + Paint/Bright/Grade
4. Fase D  → verificación in-game
```

No mezclar Fase B dentro de los reverts de Fase A. Tras cada fase, preferible commit propio (solo si el usuario lo pide explícitamente).

---

## Fuera de alcance (por ahora)

- Reimplementar Blend Color o Opacity track “bien” en el futuro (commits posteriores).
- Re-añadir features SIRSYP revertidas en Fase A (world films browser, morph speedups, structure picker polish, etc.) — solo si se piden aparte, una a una.
- Cambiar gradle / configuración de build.
- Documentación wiki salvo que se pida.

---

## Referencias de commits clave

| Hash | Rol |
| --- | --- |
| `a75c46b6c4c2e3603dadec8e92cc948c70df2bfb` | Último beta relativamente estable |
| `c48885dd958fcb133368e54db9d9b563a6b2ff4d` | Introduce Blend Color + Opacity |
| `a5a1577ba` | Color upgrade P1 (paneles / `FormColorBlend` / adjustments) |
| `2d244a9fd` | Color upgrade P2 (unificación UI forms) |
| `757b073359e2239b789131d63098d1105e195576` | Limpieza — **conservar / no revertir** |
| `a22ab2dbade5b0c3f5ddbca251ddc13c1b02e368` | i18n EN/FA — **conservar / no revertir** |
| `f5ba4cff0084501d7d6b80a90f4ca0126510fb6a` | World films browser — **conservar / no revertir** |
| `56dbeafb7c9843a3dcf45ad54d4f43a1d53f08cb` | Render-depth transparency fix — **conservar / no revertir** |
| `58a86aee4d952197792953c8a57626714b395b82` | Color Grade compatible — **sí revertir** (Fase A) |

---

## Notas para el agente

- Antes de ejecutar, re-leer este plan.
- Comunicar al usuario al terminar cada fase y esperar OK si pide confirmación entre fases.
- No hacer `git commit` / push salvo petición explícita del usuario.
- No modificar `build.gradle` ni config de gradle.
- Preferir rollback quirúrgico en Fase B frente a revert ciego de `c488`/`a5a`/`2d2`.
- Al restaurar UI, no borrar Paint/Bright/Grade/Shape; solo el mecanismo Blend+Opacity y su compatibilidad.
