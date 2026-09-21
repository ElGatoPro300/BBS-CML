# Keyframe system

Handles interpolation of values over time on film/replay/form property tracks.

## Core classes

* `KeyframeChannel<T>`: sorted list of keyframes; binary search (`findSegment`) and `interpolate`
* `Keyframe<T>`: stores `tick`, `value`, `interp`, and Bezier handles (`lx`, `ly`, `rx`, `ry`)
* `Interpolation`: wraps `IInterp` (Linear, Bezier, Easing) and `EasingArgs` (v1–v4 params)

## Factories

`IKeyframeFactory<T>` must serialize/deserialize and interpolate type `T`.

* Registered in `KeyframeFactories`
* Example types: Float, Double, Integer, Pose, Color, Link, ItemStack, Boolean, ShadowSettings, MountLink, …

### Adding a new animatable type

1. Implement `IKeyframeFactory<MyType>`
2. Register: `KeyframeFactories.FACTORIES.put("my_type", new MyFactory())`

### `KeyframeChannel.interpolate(float)` empty-channel default

When the channel has **no** keyframes (or no segment), `interpolate(ticks)` picks a numeric zero only for:

* `FloatKeyframeFactory` → `0F`
* `DoubleKeyframeFactory` (and subclasses) → `0D`
* `IntegerKeyframeFactory` → `0`

For **every other factory** (Boolean, ItemStack, Pose, Color, ShadowSettings, MountLink, …) the one-arg overload returns **`null`**. Callers that need a typed empty value should use `interpolate(ticks, default)` or `factory.createEmpty()` / `interpolateHeld`.

This is intentional for the generic channel API — not a replay-recording limitation. Do **not** assume `interpolate(tick)` is non-null for non-numeric tracks.

## Viewport recording undo

Stopping a film-editor viewport take (`Only rotation` and other record-overlay groups) commits one explicit keyframe undo: pre-take `recordingOld` → post-simplify/seal state, flushed immediately and marked non-mergeable. `simplify`/`seal` notifies are suppressed during that finalize so they cannot poison the undo cache. Incoming `receiveActions` must edit `replay.actions` only (not the whole `Replay`), or `reduceUndoRedundancy` can drop the keyframes snapshot and Ctrl+Z will not restore the pre-take timeline.

## Procedural limb swing (ModelForm stubs)

`ProceduralAnimator` soft-fills walk swing when `horizontalSpeed > 0.08` but vanilla `LimbAnimator` is still idle. For **film-driven** targets (`StubEntity` and `ActorEntity`), that speed must come from **prev→pos XZ displacement only** — never from `vX`/`vZ` / entity velocity. Otherwise a stationary stub (position truncated, velocity leftover or apply-time velocity) gets ghost arm/leg motion timed to old walk data. Live players / non-film entities still use `max(velocity, displacement)`.

## Replay recording resume

Viewport re-record at tick `T` uses `ReplayKeyframes.bridgeRecordingFrom(T, groups, liveEntity)`:

1. Snapshot interpolated values from **non-empty** channels only (for the groups being recorded).
2. `clearFrom(T, groups)` (same channel set).
3. Restore snapshots at `T`.

**Position hard-cut:** position / velocity / fall are only cleared at `T` during the bridge (not freeze-restored). After capture + `simplify()`, `sealPositionRecordingCut` inserts a hold one tick before the first new-take keyframe when that value differs from the pre-record timeline — so old XYZ stays until the cut, then jumps. Rotation / sticks keep the freeze-at-`T` bridge.

Empty channels are never seeded with defaults (avoids planting `0°` / south yaw on from-scratch takes).

### Vanilla pose / action tracks

The record overlay has no dedicated pose/action buttons. Pose flags (`sneaking`, `sleeping`, …) and action doubles (`using_item`, `death_time`, …) — plus viewport `riding` via `recordMountKeyframes` — are only cleared / bridged / recorded when capturing **all groups** (`groups == null` or empty).

Position-only / rotation-only / stick takes **leave those tracks alone**.

When all-groups recording captures pose/action channels and `riding`, initial keyframes (e.g. `0`) are recorded at the start tick via `insertIfChanged`. This ensures that states do not improperly extrapolate backwards prior to later state changes (e.g. a sneak at tick 50 having `0` at tick 0 so the player is not seen crouching before tick 50). Subsequent unchanged ticks skip redundant insertions, and transitions automatically insert hold keyframes.

Bridge restore for those tracks uses a separate idle-zero policy: a snapshotted `0` is **not** re-inserted when there is no prior key to cut, or when the last key before `T` is already `0`. That stops legacy films (sole `0` at tick 0) from rewriting pose/action keys on every re-record. A `0` **is** still restored when a prior non-zero hold must end at `T`. The same idle-zero restore policy applies to **sticks / triggers / extras**. Live recording still seeds idle `0` so new takes stay correct.

Outside / world re-record uses `ReplayKeyframes.copyOver`. If a source channel is **empty** (no keys written in the new take), destination keys from the take start onward are still cleared — previously `KeyframeChannel.copyOver` no-op'd on empty sources and left legacy pose/action keys in place.

`riding` / `ridden` are cleared from `T` on all-groups re-record (same gate). They are **not** bridge-restored from old timeline values — `recordMountKeyframes` rewrites them from live mount state so a non-sitting re-take does not keep leftover sitting keys. On other replays, only `ridden` keys from `T` that **link to this rider index** are removed (other mounts' links stay intact).

### Factories / channels covered by the bridge

| Factory | Replay channels bridged |
|---------|-------------------------|
| **Double** | Position, velocity, fall, rotation (yaw/pitch/head/body), sticks/triggers/extras; vanilla pose/action flags **only for all-groups** (not `riding`) |
| **ItemStack** | Hands + armor (only when recording **all groups**) |
| **Integer** | `selected_slot` (all groups only) |

Yaw/pitch/body stay on plain `DOUBLE`. `apply()` unwraps prev yaw toward current with `Lerps.normalizeYaw` for short-arc **render** only (stored keys unchanged).

### Not bridge-restored (cleared + live-recaptured on all-groups)

* `riding` (**Double**), `ridden` (**MountLink**) — cleared from `T` in `clearFrom`; rewritten by `RecorderMobCapture.recordMountKeyframes`

### Not bridged / not viewport-recorded (pre-existing)

These exist on `ReplayKeyframes` but are **outside** `clearFrom` / `record` / `bridgeRecordingFrom` today:

* `invulnerable` (**Boolean**)
* `shadow_size` (**ShadowSettings**), `shadow_opacity` (**Double** — opacity exists but is not in the clear/record set either)

Form property tracks (Pose, Color, Transform, …) live under `FormProperties`, not this bridge.

**If you later add viewport recording (or resume-bridge) for a new factory type:** extend `bridgeRecordingFrom` / `clearFrom` / `record` with typed snapshot+restore (same pattern as `snapshotItem` / `snapshotInteger`), and decide empty-channel policy (`createEmpty()` vs skip). Do not rely on `interpolate(tick)` alone for non-numeric factories.

## Related

* Replays: `docs/architecture/replays.md`
* Frame/tick conversion: `docs/FILM_FRAME_TIMELINE.md`
