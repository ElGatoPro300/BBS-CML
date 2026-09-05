# Billboard rendering in 1.21.11

## Cause of the invisible base surface

`BillboardFormRenderer` previously submitted its triangle buffer to the local
`net.minecraft.client.render.BufferRenderer` compatibility shim. That shim chooses
`GUI_TEXTURED` for both billboard vertex formats and creates a `RenderSetup` without
textures. For shaded billboards this also mismatches the entity vertex layout; the
pipeline uses quads while the buffer contains triangles. Binding a raw GL program
and texture before that call does not configure the subsequent vanilla render pass.

## Implemented change

`BillboardRenderLayers` supplies explicit triangle pipelines for the live base draw
and deferred base surface. It uses the 1.21.11 vanilla entity shader with lighting
and overlay samplers for shaded billboards, and the vanilla position/texture/color
shader for unshaded billboards. Both use alpha blending and LEQUAL depth testing.
Depth writes and culling are pipeline variants rather than ambient GL state.
The existing paired front/back triangles, UV crop, transforms and subdivisions are
preserved. Shaded alpha discard is 0.001 to retain low-opacity fades.

The BBS texture is exposed through `AdoptedTexture`, bound as `Sampler0`, and given
an explicit sampler for the requested linear/mipmap settings. Shaded draws also
bind the lightmap and overlay. The source texture is rebound to unit zero after
the pass because the remaining legacy code changes its filter parameters.

## Verification and remaining work

- `gradlew compileClientJava` passed after the initial implementation.
- `gradlew runClient` reached client resource initialization with the change.
- Visibility in the user's world has not been visually verified.
- This change does not certify the entire BillboardForm port: pixel picking,
  paint/glow/tint overlays and shader-based color grading still contain legacy
  submission/uniform paths. The new base shader does not implement custom BBS
  color-grade uniforms. Iris shader-pack shadows and deferred output targets also
  need in-world verification.

For the next manual check, restart with the latest classes and use an ordinary
billboard with a known PNG, no effects and full opacity. Check shading on/off,
front/back visibility, terrain occlusion, crop, camera facing, and linear/mipmap
filters. Then check fractional opacity and the form editor preview. Test special
effects and picking separately so failures are not mistaken for a missing base
texture. Compilation and successful startup are not visual acceptance tests.
