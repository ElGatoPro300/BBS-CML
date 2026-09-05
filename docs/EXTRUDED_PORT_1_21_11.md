# ExtrudedForm: base rendering in 1.21.11

Billboard visibility was confirmed in-game by the user. ExtrudedForm had a different
submission problem: its mesh used raw VAOs and the legacy model shader/uniform path.
That does not provide the render pass bindings required by 1.21.11.

## Changes

- TextureExtruder caches ModelVAOData separately from the legacy GPU VAO. Both caches
  are invalidated by texture deletion/reload. The pixel extrusion algorithm is unchanged:
  front/back triangles and side faces around opaque pixel edges retain their UVs and normals.
- Normal world and UI drawing use BufferBuilder triangles and BillboardRenderLayers,
  with an explicit texture sampler and shaded/unshaded vertex layout. Positions and
  normals are transformed by the form's MatrixStack. Camera-facing transforms remain
  in the existing renderer. Culling is disabled to accommodate reflected UI/form transforms.
- The base route retains form color, opacity, and two-pass texture crossfades. It uses
  nearest texture filtering and preserves terrain depth testing. CPU mesh data is cached,
  but transformed vertices are uploaded each draw; GPU mesh caching is a future optimization.
- The UI no longer depends on successful compilation of the old BBS model shader.

## Scope and validation

The initial Java compilation passed. In-world visual validation remains pending.
The base route bypasses the old effect rendering code: paint, glow, spatial color-grade
masks, PBR and Iris pack-specific shadows/deferred effects are not restored here.
Basic color-grade baking uses the existing copyBakingColorGrade helper, not the old
per-fragment shader. Pixel picking still follows the legacy path and is not certified.

Restart the client and check a PNG without effects from the front, back, and edge.
Confirm that the edge has thickness, transparent holes remain clear, terrain hides
buried faces, shading can be toggled, and the editor preview renders. Then test color,
opacity, nonuniform scaling, camera-facing mode, and texture crossfades. Successful
compilation does not establish that these visual checks pass.
