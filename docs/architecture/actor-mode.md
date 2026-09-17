# Actor Mode (ActorEntity vs stubs)

Two representations. **Do not collapse them.** Toggling Actor never deletes the stub.

| | **Stub** (normal replay) | **Actor** (`ActorEntity`) |
|---|---|---|
| Type | Client-only `StubEntity` (`IEntity`) | Server `LivingEntity`, wrapped as `MCEntity` |
| Spawn | `BaseFilmController.createEntities()` for every enabled replay | `ActionPlayer.spawnActor()` when `actor` (or `fp`) is true; IDs via `ServerNetwork.sendActors` |
| Draw | Form pipeline in `BaseFilmController.renderEntity` | `ActorEntityRenderer`; stub body is skipped (`physicalActor`) except stencil/gizmo |
| Physics / HP | None | Real collision, combat, death, item pickup/drops |
| Relative camera | `Replay.relative` works | Stub-only — toggle **must** clear `relative` (`Replay.isCameraRelative()`) |

## Dual representation

* On actor replays the stub still receives `ReplayKeyframes.apply` (recording, onion, puppeteer source). The visible body is the world entity.
* Use `getPhysicalActorEntity` / `getRenderEntity`.
* **Never** draw or pick the stub as a fallback after combat death (standing ghost).
* `isActorPickingBlocked` also covers keyframed `death_time`.

## Server vs client

* `ActionPlayer.apply()` teleports the actor, runs `ActorReplayStateSync.applyFromKeyframes`, applies `ActionClip`s.
* Client copies stub pose/equipment onto the actor in `BaseFilmController.updateEntities` (`syncFromSource`, `syncActorEquipmentFromStub`).
* Do not skip that sync or armor/pose go empty until respawn.

## `fp` (first person / real player)

* Uses the real player instead of spawning `ActorEntity`.
* Treat as actor-like (`actor` or `fp`) in spawn/ensure paths only.

## Toggle UI (`UIReplaysOverlayPanel` Actor)

* `notifyServer(SEEK)` **before** `actor.set` so combat HP matches the cursor.
* `updateChannelsList(true)` (preserve keyframe/limb/ghost preview).
* `createEntities()` at **current tick** (tick 0 wipes later equipment).
* Preserve the active editor tab (`beginSuppressLinkedPropertiesTabFocus` + restore).
* Do not steal Properties when Replays/General share a tab group.
* Do not discard untouched provisional keyframes when preserving selection.

## Tracks / settings

* `invulnerable` is actor-only on the timeline.
* Combat death: do **not** feed `death_time` keyframes into `ActorEntity.deathTime` (stuck red overlay).
* Related settings: `BBSSettings.actorDamageFlash`, `actorDamageAnimation`, `editorActorPauseAnimations`.

## Actor control mouse capture

* Enabling control must **center** the cursor (`Window.centerCursor`) and **skip the first look/stick delta** (`controlLookPrimed`), same idea as editor free-look flight.
* `Window.centerCursor` also **force-syncs** Minecraft `Mouse` x/y to the warp and clears `cursorDeltaX/Y` (`MouseAccessor`). Without that, `Mouse.getX/Y` can stay on the UI click for several frames after `glfwSetCursorPos`, and the first real look movement jumps yaw/pitch.
* After enabling control, a **deferred second grab** (`regrabControlMouse` on next client tick) matches the multi-grab timing of Only-rotation / Record overlay close — one priming frame is not enough on some platforms after a POSE-icon click.
* `PlayerUtils.teleport` seeds `prevYaw` / `prevHeadYaw` / `prevBodyYaw` / `prevPitch` to the teleported pose. Actor control uses `setupActorControlPlayer` (no full stub `copy`), so without prev seed the first `changeLookDirection` lerps from the old player look and snaps procedural head/limbs/bodyYaw.
* Look/stick **deltas** while controlling come from Minecraft `Mouse.getX/Y()` (cursor callbacks). Do **not** derive deltas from `glfwGetCursorPos` under `GLFW_CURSOR_DISABLED` — on many platforms that position stays at the warp center and rotation becomes zero while WASD position still works.
* Disabling while flight + `editorFlightFreeLook` is active hands the grab to `UIFilmPanel.captureFreeFlightMouse()` (no `NORMAL` flash between owners).
* Record / insert-frame overlays unlock the cursor but `canControl()` stays false while they are open; cancel/close re-grabs via `onClose` → `toggleMousePointer(controlled != null)`.

## Iris

* `ShadowRendererMixin` must skip stub bodies for actor replays (the physical entity already casts the shadow).

## Gizmos

* Actor gizmos come from the live `ActorEntity` (`FilmEditorController.renderActorModeEntity`).
* Hide while dying.
* Drag needs the Properties host mounted (`focusLinkedPropertiesTab` on gizmo grab) because `UIPropTransform` ticks drag from `render()`.

## Key classes

`Replay`, `StubEntity`, `ActorEntity`, `ActorEntityRenderer`, `ActorReplayStateSync`, `ActionPlayer`, `BaseFilmController`, `FilmEditorController`, `UIFilmController`, `UIReplaysOverlayPanel`, `UIReplaysEditor`.

## Related

* Replays overview: `docs/architecture/replays.md`
* Rendering / Iris: `docs/architecture/rendering-iris.md`
