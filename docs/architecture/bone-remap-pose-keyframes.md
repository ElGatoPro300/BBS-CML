# Bone remap for pose keyframes (2.2 implementation guide)

This document is an implementation plan for a **2.2** feature: renaming / remapping bone (limb) names inside pose-related keyframes so animations and poses can be transferred between forms that use different bone names.

It is intentionally scoped for the current 2.2 codebase. A cleaner domain/API version of the same idea can later move into BBS 3.0.

## Problem

Pose data stores bone names as **string map keys** in `Pose.transforms`. Per-limb tracks use channel ids like `{formPath}/pose:{boneName}`.

When copying a dance/pose from one form to another, bone names often differ even if the rig topology is similar. Today the editor can:

- Convert model animations into pose keyframes (`Animation to pose keyframes...`).
- Split combined poses into limb tracks (`Pose to limbs`).
- Copy/paste poses.

But it **cannot remap bone names** inside selected keyframes. Model geometry rename also does not migrate keyframe keys.

## Goals

- Let users define an explicit bone name map: `oldName -> newName`.
- Apply that map only to **currently selected keyframes**.
- Support both:
  - Combined pose keyframes (`KeyframeFactories.POSE` / `Pose.transforms`).
  - Per-limb transform keyframes (`…/pose:bone` / `PoseTransform` channels).
- Reuse one remap UI from:
  - Advanced section inside **Animation to pose keyframes...**
  - Context menu **Edit bones/limbs** on selected pose/limb keyframes.
- Allow total user control: mapped names do not need to exist on the destination model.
- Provide mod-global **bone mapping presets** to avoid rebuilding maps repeatedly.
- Keep existing timelines/behavior unchanged when the feature is unused.



## Non-goals (2.2)

- Automatic heuristic remapping by bone similarity.
- Rewriting model armature geometry names.
- Baking modifiers or changing film format compatibility beyond additive UI/data tools.
- Icon-only timeline redesign (separate 3.0 UI work).



## Current integration points


| Area                           | Path / class                                           |
| ------------------------------ | ------------------------------------------------------ |
| Animation → pose overlay       | `UIAnimationToPoseOverlayPanel`                        |
| Film conversion callback       | `UIReplaysEditor` / `UIReplaysEditorUtils`             |
| Pose data                      | `Pose`, `PoseTransform`                                |
| Pose keyframe factory          | `PoseKeyframeFactory`                                  |
| Limb track detection           | `UITransformKeyframeFactory.isPoseLimbTrack()`         |
| Context menus                  | `UIReplaysEditor`, animation state editor              |
| Preset storage pattern         | `PresetManager` under `settings/presets/...`           |
| Pose asset presets (different) | `DataManager` / `PoseManager` for model pose libraries |




## UX overview



### Entry points

1. **Animation to pose keyframes...**
  - Existing overlay gains an **Advanced** button/section.
  - Advanced opens the shared bone remap UI for the current form/model.
  - On **Generate**, the remap is applied to newly generated pose keyframes.
2. **Edit bones/limbs**
  - New context-menu action when pose / pose-overlay / pose-limb keyframes are selected.
  - Opens the same shared remap UI.
  - On apply, remaps **only selected keyframes**.



### Shared remap panel layout

For each form group/tab:

```text
[ Left: current bone names ]   →   [ Right: renamed bone names ]
   head                            Head
   body                            Body
   arm_l                           left_arm
   arm_r                           right_arm   (dimmed if unchanged)
```

- Left column: source bone names.
- Right column: destination names (editable).
- Middle: one arrow per row.
- Unchanged rows keep the same name on the right and render dimmed / deselected.
- Right-click left column: add / remove mapping rows.
- Right-click right column: **Edit name** (inline text field + accept/cancel icons on that row) / remove mapping rows.
- Removing a row removes both sides.
- Users may add names that do not exist on any model.



### Nested / body-part forms

If the current selection includes pose keyframes from a parent form **and** nested body-part forms, the panel shows **one tab/group per form path** that has selected pose-related keyframes.

Each tab has its own mapping table, auto-filled from that form's current model when possible.

### Validation / conflicts

Show a visual error (red background + red destination text) when:

- Destination name already exists as another destination in the same table.
- Destination name would collide with an existing bone key inside the affected keyframe after remap (`old -> new` where `new` already exists and is not the same row's identity case).
- Duplicate source (`old`) rows, if manually created or loaded from bad data.

Hover tooltip example:

> That name already exists. If you apply changes, this remap row will be skipped.

Apply rules:

- Valid rows are applied.
- Invalid rows are skipped / discarded for the apply pass.
- Errors do **not** block applying the rest of the map.



## Data model



### Mapping table

Domain-friendly structure (names illustrative):

```text
BoneRemapTable
  formPath: String
  rows: List<BoneRemapRow>

BoneRemapRow
  source: String
  destination: String
```

A row is an identity/no-op when `source.equals(destination)`.

Suggested serialized preset shape:

```json
{
  "version": 1,
  "rows": [
    { "from": "arm_l", "to": "left_arm" },
    { "from": "arm_r", "to": "right_arm" }
  ]
}
```

Presets store only the mapping rows (global, reusable). They are not tied to a film/project.

### Apply semantics

For each selected keyframe:

**Combined pose keyframe (**`Pose`**)**

1. Build a new `transforms` map.
2. For each bone key:
  - If a valid remap row exists for that key, write under `destination`.
  - Else keep the original key.
3. Skip invalid remap rows (collision/error).
4. Replace the keyframe value with the remapped pose.

**Per-limb keyframe (**`…/pose:bone`**)**

1. If the selected keyframe belongs to a limb channel whose bone segment matches a valid remap `from`:
  - Move/rename that channel contribution for the selected key only.
2. Practical 2.2 approach:
  - Preferred: rename channel id for selected keys by creating/ensuring destination channel and transferring only selected keyframes, leaving unselected keys on the source channel untouched.
  - If destination channel already has a key at the same tick, treat as conflict for that row/key and skip with the same error policy where detectable in UI, or skip silently at apply with count in a result summary.

Important: **do not remap unselected keyframes**, even on the same limb track.

## Auto-fill rules

When opening a form tab:

1. Resolve the form at `formPath`.
2. If the form is a `ModelForm` with a loadable `ModelInstance`, auto-fill left-column rows from model bone/group keys.
3. Right column starts equal to left column (all identity rows, dimmed).
4. User edits destinations, adds/removes rows, or loads a preset.



### Nested form with no loadable model

If a tab's form cannot provide bones (missing model, non-model form, unloadable asset):

- Do **not** fail to open the panel.
- Open the tab with an **empty table**.
- Show a short notice in that tab, e.g. “No model bones available; add mappings manually or load a preset.”
- Still allow add/remove/edit and presets.
- Optional helper: “Load bones from selected keyframes” can populate sources from keys actually present in the selected pose keyframes for that form path.



## Animation → pose flow

In `UIAnimationToPoseOverlayPanel`:

1. Keep current animation list / only-keyframes / length / step / generate.
2. Add Advanced → shared remap panel for the target form.
3. Extend generate callback to receive the active remap table (or store it on the overlay).
4. After each generated `Pose` snapshot (`model.createPose()`), apply the remap before inserting keyframes.

Edit of already existing keyframes stays exclusive to **Edit bones/limbs**.

## Presets



### Storage

Follow the existing mod-global preset pattern used by keyframes/clips/layouts:

- Prefer a new `PresetManager` entry, for example:

```text
PresetManager.BONE_REMAPS = new PresetManager(BBSMod.getSettingsPath("presets/bone_remaps"));
```

- Files live under the user settings presets folder (global to the mod install), not inside a film/world.
- Do **not** overload `PoseManager` / model `poses.json` libraries; those are pose snapshots, not bone maps.



### UI

Button label suggestion: **Bone mapping presets** (clearer than “Bone presets”).

Actions:

- Save current table as preset.
- Load preset into the active form tab.
- Delete/rename preset via the same style of preset UI already used elsewhere if practical.

Loading a preset replaces or merges rows in the active tab; 2.2 default recommendation: **replace active tab rows**, with identity fill for any model bones not mentioned by the preset kept optional (open question below).

## Undo

All apply operations must be undoable.

Recommended approach:

- Wrap mutations through the existing keyframe/`BaseValue` edit + undo path used by pose conversion / limb conversion.
- One undo step per Apply / Generate that includes all selected-keyframe remaps across all form tabs.
- Do not create one undo entry per bone row.

If limb-channel transfers need temporary channel creation, include channel create/move/delete side effects in the same undo record where possible.

## Implementation plan (incremental)



### Step 1 — Domain remap utility

Create a small shared helper (no Minecraft UI dependencies), e.g. under `utils/pose/`:

- `BoneRemapTable` / `BoneRemapRow`
- Validate rows (duplicate from/to, optional existing-key collision checks given a `Pose`)
- `Pose remap(Pose source, BoneRemapTable table, RemapResult result)`
- Helpers for limb channel id parse/build (`pose:bone`, overlays)



### Step 2 — Shared overlay UI

New overlay/panel reusable by both entry points:

- Form tabs
- Two columns + arrows
- Inline rename on right column
- Context menus add/remove/edit
- Conflict highlighting + tooltips
- Preset button
- Apply / Cancel



### Step 3 — Edit bones/limbs entry

- Context menu on selected pose/pose-overlay/limb keyframes.
- Collect selection grouped by form path.
- Open shared panel.
- Apply only to selection.



### Step 4 — Animation → pose Advanced

- Wire Advanced into `UIAnimationToPoseOverlayPanel`.
- Pass remap into generate path.
- Apply after pose capture, before insert.



### Step 5 — Localization

All user-facing strings via `IKey` / `UIKeys` + `en_us.json` / `es_es.json` (and other locales as needed).

Suggested keys (illustrative):

- `bbs.ui.film.replay.context.edit_bones`
- `bbs.ui.film.replay.bone_remap.title`
- `bbs.ui.film.replay.bone_remap.advanced`
- `bbs.ui.film.replay.bone_remap.edit_name`
- `bbs.ui.film.replay.bone_remap.add_row`
- `bbs.ui.film.replay.bone_remap.remove_row`
- `bbs.ui.film.replay.bone_remap.presets`
- `bbs.ui.film.replay.bone_remap.error.name_exists`
- `bbs.ui.film.replay.bone_remap.no_model_bones`



### Step 6 — Manual test checklist

- Remap selected combined pose keys only; unselected keys unchanged.
- Remap selected limb-track keys only; unselected keys on same track unchanged.
- Nested body-part selection creates multiple tabs.
- Identity rows remain dimmed and no-op.
- Conflict rows skipped; valid rows applied.
- Animation → pose generate uses Advanced map.
- Preset save/load global across films.
- Undo restores previous bone names/channels.
- Form tab with unloadable model opens empty and still accepts manual rows/presets.
- Unknown destination names are allowed and preserved in data.



## Open notes / minor decisions

These do not block starting implementation, but should be decided during coding:

1. **Preset load vs model bones**
  Replace table entirely, or replace and re-append missing model bones as identity rows?
2. **Case sensitivity**
  Bone names should probably remain case-sensitive to match model keys.
3. **Overlay limb track prefixes**
  Confirm exact handling for `pose_overlay` / `pose_overlayN:bone` channel ids using the same parser as current limb-track UI.
4. **Result toast**
  After apply, optionally show “Remapped N keys, skipped M conflicts.”
5. **Selection spanning many forms**
  If dozens of nested forms are selected, tabs may need scrolling; keep one tab per form path anyway.
6. **3.0 follow-up**
  Keep domain remap pure and UI-thin so 3.0 can reuse `BoneRemapTable` without the 2.2 overlay specifics.



## Summary

This feature is a focused 2.2 editor tool: an explicit bone-name remap applied to selected pose/limb keyframes, reusable from Animation → pose Advanced and from Edit bones/limbs, with mod-global mapping presets and non-blocking conflict handling. It fills a real gap in transferring animations between differently named rigs without changing the existing timeline formats.