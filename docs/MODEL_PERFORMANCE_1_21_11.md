# Model rendering performance (1.21.11)

## Changes, 2026-09-09

BOBJ's CPU render-layer path previously submitted each consecutive run of dominant
bones separately, even when the bones had identical draw state. Neutral meshes now
use one draw per mesh, retaining the original triangle order and weighted skinning.
Picking, per-bone effects, texture overrides, and global glow retain the original
split path. Eligibility is checked on each draw so animated properties remain live.

Source-file inspection of the bundled Emoticons assets found these approximate
run counts (dominant vertex weight, majority bone per triangle):

| Asset | Triangles | Bone runs | Meshes |
| --- | ---: | ---: | ---: |
| alex | 4,760 | 978 | 1 |
| steve | 5,016 | 1,017 | 1 |
| alex_simple | 224 | 187 | 1 |
| steve_simple | 224 | 187 | 1 |

These are source topology counts, not measured GPU timings or FPS gains. The new
single-draw path applies only when its neutral-state checks pass.

BOBJ normal skinning now transforms directions directly with the bone matrix,
avoiding a temporary 3x3 matrix copy per vertex influence. Cubic models reuse their
rotation matrices without allocating two additional matrices per rotation. Mesh
shape-key names and weights are resolved once per mesh rather than per triangle;
zero-weight shapes are skipped.

## Validation

- `compileClientJava`: successful.
- `git diff --check`: successful.
- In-game performance and visual parity: pending.

Compare the same camera, resolution, shader pack and model count after loading has
settled. Check Emoticons idle/walk and simple joint bending, cubic shape keys,
per-bone textures/colors/glow, Film picking, and Iris shadows. FPS improvement has
not yet been measured. Animated geometry is still skinned on the CPU and uploaded
each draw; this change does not introduce a cross-frame pose cache or GPU skinning.
