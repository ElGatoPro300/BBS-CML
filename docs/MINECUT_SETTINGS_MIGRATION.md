# Minecut Architectural & Settings Migration Guide

This guide is intended for the developer of the **Minecut** addon (and their AI coding assistant). It provides full technical details on the decoupling of Minecut-specific hooks from the core BBS mod, replacing them with a clean, extensible **Addon UI and Settings API**.

All changes are 100% backwards-compatible via `@Deprecated` aliases, so existing versions will continue to function. However, future versions of Minecut should migrate to the standard addon APIs detailed below.

---

## 1. Replay Tracks & Overlay Configuration

BBS previously had hardcoded static fields in `BBSSettings.java` for Minecut track defaults. These have been moved to standard, generic BBS settings under the `recording` category.

### Field & Key Mapping
| Legacy Field (`BBSSettings`) | New BBS Key (`recording`) | New BBS Field in `BBSSettings` | Type | Default Value | Description |
|---|---|---|---|---|---|
| `minecutDefaultTrackTransform` | `"default_track_transform"` | `BBSSettings.recordingDefaultTrackTransform` | `ValueBoolean` | `true` | Include Transform track by default on new replays |
| `minecutDefaultTrackPose` | `"default_track_pose"` | `BBSSettings.recordingDefaultTrackPose` | `ValueBoolean` | `true` | Include Pose track by default on new replays |
| `minecutDefaultTrackVisible` | `"default_track_visible"` | `BBSSettings.recordingDefaultTrackVisible` | `ValueBoolean` | `false` | Include Visible track by default on new replays |
| `minecutDefaultTrackColor` | `"default_track_color"` | `BBSSettings.recordingDefaultTrackColor` | `ValueBoolean` | `false` | Include Color track by default on new replays |
| `minecutDefaultTrackOpacity` | `"default_track_opacity"` | `BBSSettings.recordingDefaultTrackOpacity` | `ValueBoolean` | `false` | Include Opacity track by default on new replays |
| `minecutDefaultTransformOverlays` | `"transform_overlays"` | `BBSSettings.recordingTransformOverlays` | `ValueInt` | `0` (range: 0..42) | Number of default transform overlay tracks |
| `minecutDefaultPoseOverlays` | `"pose_overlays"` | `BBSSettings.recordingPoseOverlays` | `ValueInt` | `0` (range: 0..42) | Number of default pose overlay tracks |
| `minecutDefaultColorOverlays` | `"color_overlays"` | `BBSSettings.recordingColorOverlays` | `ValueInt` | `0` (range: 0..42) | Number of default color overlay tracks |
| *(New in BBS)* | `"illusion_overlays"` | `BBSSettings.recordingIllusionOverlays` | `ValueInt` | `0` (range: 0..42) | Number of default illusion overlay tracks |

### Centralized Getters
Always retrieve the effective overlay counts through the centralized getters in `BBSSettings`:
```java
int transformCount = BBSSettings.getTransformOverlaysCount();
int poseCount      = BBSSettings.getPoseOverlaysCount();
int colorCount     = BBSSettings.getColorOverlaysCount();
int illusionCount  = BBSSettings.getIllusionOverlaysCount();
```

---

## 2. UI Style & Appearance Decoupling

Minecut UI styling is now part of the generic Addon UI Style system rather than hardcoded in core BBS appearance calculations.

### `UiStyleCapabilities` (`mchorse.bbs_mod.settings.UiStyleCapabilities`)
- **Style Identifier:** Use `UiStyleCapabilities.ADDON` (`1`) instead of `MINECUT`.
- **Enabling Addon Style:**
  ```java
  // New standard method:
  UiStyleCapabilities.enableAddonStyle();

  // Deprecated alias (still functional):
  UiStyleCapabilities.enableMinecutStyle();
  ```
- **Custom Theme Colors:**
  Addons can now customize surface background and accent colors dynamically without hardcoding them into BBS:
  ```java
  UiStyleCapabilities.setAddonChromeSurface(0xFF101014);
  UiStyleCapabilities.setAddonBaseSurface(0xFF16161A);
  UiStyleCapabilities.setAddonRaisedSurface(0xFF1E1E24);
  UiStyleCapabilities.setAddonDeepSurface(0xFF0A0A0C);
  UiStyleCapabilities.setAddonDividerColor(0xFF00C2D4);
  UiStyleCapabilities.setAddonAccentColor(0x00C2D4);
  ```

### `UIStyle` & `BBSSettings`
- Querying if an addon style is active:
  ```java
  boolean isAddon = BBSSettings.isAddonUiStyle(); // replaces BBSSettings.isMinecutUiStyle()
  boolean isAddonStyle = UIStyle.isAddon();        // replaces UIStyle.isMinecut()
  ```
- Invalidating cached style instances:
  ```java
  UIStyle.invalidateAddonCache(); // replaces UIStyle.invalidateMinecutCache()
  ```

### Registration via `RegisterFilmUiAddonEvent`
When registering the UIStyle factory in your client addon:
```java
@Subscribe
public void onRegisterFilmUiAddon(RegisterFilmUiAddonEvent event)
{
    // Preferred:
    event.registerAddonStyleFactory(() -> new MyMinecutUIStyle());
    event.registerWorkspaceFactory(UIFilmPanel -> new MyMinecutWorkspace(panel));
    event.setSparseTracksPreferred(true);

    // Deprecated alias (still supported):
    // event.registerMinecutStyleFactory(...)
}
```

---

## 3. Workspace & Dock Layout Decoupling

### `ValueEditorLayout` (`BBSSettings.editorLayoutSettings`)
The dock node tree for NLE/addon layout is now generalized:
- **Root Node Access:**
  - `layout.getAddonLayoutRoot()` (replaces `getMinecutLayoutRoot()`)
  - `layout.setAddonLayoutRoot(root)` (replaces `setMinecutLayoutRoot(root)`)
  - `layout.wasAddonLayoutRootLoaded()` (replaces `wasMinecutLayoutRootLoaded()`)
- **Splitters Access:**
  - `layout.getAddonSplitters()` (replaces `getMinecutSplitters()`)
  - `layout.syncAddonSplittersFromRoot(root)` (replaces `syncMinecutSplittersFromRoot(root)`)
  - `layout.setAddonSplitterRatio(index, ratio)` (replaces `setMinecutSplitterRatio(index, ratio)`)
- **NBT Persistence:**
  - Primary key: `"addon_layout_root"`
  - Core BBS dual-writes `"minecut_layout_root"` and reads either key so legacy user config files load without data loss.

### `ValueUILayoutPreferences` (`BBSSettings.layoutPreferences`)
- **Methods:**
  - `layoutPreferences.getAddonLayout()` (replaces `getMinecutLayout()`)
  - `layoutPreferences.setAddonLayout(data)` (replaces `setMinecutLayout(data)`)
- **NBT Persistence:**
  - Primary key: `"addon_layout"` (with automatic fallback to `"minecut_layout"`).

### `EditorLayoutNode`
- `EditorLayoutNode.defaultAddonLayout()` (replaces `defaultMinecutLayout()`)
- `EditorLayoutNode.fromAddonData(data)` (replaces `fromMinecutData(data)`)

### `FilmUiPanelIds`
- `FilmUiPanelIds.isAddonPanelId(panelId)` returns `true` for IDs starting with `"addon"`, `"minecut"`, or `"nle"`.
- `FilmUiPanelIds.isMinecutPanelId(panelId)` is preserved as a `@Deprecated` alias.

---

## 4. Migration Checklist for Minecut

1. **Track Configuration:**
   - Stop defining or assigning `BBSSettings.minecutDefault*`.
   - Use `BBSSettings.recordingDefaultTrack*` and `BBSSettings.get*OverlaysCount()`.
2. **Style Registration:**
   - Change `event.registerMinecutStyleFactory(...)` to `event.registerAddonStyleFactory(...)`.
   - Call `UiStyleCapabilities.enableAddonStyle()`.
   - Optionally set custom colors via `UiStyleCapabilities.setAddon*Surface(...)`.
3. **Workspace Checks:**
   - Replace calls to `panel.isMinecutFilmUi()` with `panel.isAddonFilmUi()`.
   - Replace calls to `UIStyle.isMinecut()` with `UIStyle.isAddon()`.
4. **Layout Preferences:**
   - Update references to `getMinecutLayoutRoot()` / `getMinecutSplitters()` to their `*Addon*` equivalents.
