# Creating Addons for BBS CML

This comprehensive guide explains how to create addons for BBS CML. The addon system allows you to extend the mod's functionality by adding new forms, clips, dashboard panels, custom Molang functions, model loaders, and more, without needing to modify the core mod code or use complex Mixins.

## Table of Contents

1. [Prerequisites](#prerequisites)
2. [Project Setup](#project-setup)
3. [Addon Structure](#addon-structure)
4. [Core Concepts](#core-concepts)
   - [Forms and Data](#forms-and-data)
   - [UI System (Flex & Widgets)](#ui-system-flex--widgets)
   - [Settings and Values](#settings-and-values)
   - [Event Bus](#event-bus)
5. [Server-Side Registration (BBSAddon)](#server-side-registration-bbsaddon)
6. [Client-Side Registration (BBSClientAddon)](#client-side-registration-bbsclientaddon)
7. [Advanced Topics](#advanced-topics)
   - [Custom UI Components](#custom-ui-components)
   - [Networking & PacketCrusher](#networking--packetcrusher)
   - [Localization (L10n)](#localization-l10n)
   - [Undo/Redo System](#undoredo-system)
8. [Utility Classes](#utility-classes)
9. [Best Practices](#best-practices)
10. [Troubleshooting](#troubleshooting)
11. [Step-by-Step Example](#step-by-step-example)

## Prerequisites

- Basic knowledge of Java and Fabric modding.
- A Fabric development environment set up.
- BBS Mod installed in your development environment.

## Project Setup

### fabric.mod.json

To register your addon, you need to add specific entrypoints to your `fabric.mod.json`.

- `bbs-addon`: For your common/server-side addon class.
- `bbs-addon-client`: For your client-side addon class.

```json
{
  "schemaVersion": 1,
  "id": "my_bbs_addon",
  "version": "1.0.0",
  "name": "My BBS Addon",
  "description": "An awesome addon for BBS Mod",
  "authors": [
    "Your Name"
  ],
  "contact": {
    "homepage": "https://example.com",
    "sources": "https://github.com/your/repo"
  },
  "license": "MIT",
  "icon": "assets/my_bbs_addon/icon.png",
  "environment": "*",
  "entrypoints": {
    "main": [
      "com.example.addon.MyAddon"
    ],
    "client": [
      "com.example.addon.client.MyAddonClient"
    ],
    "bbs-addon": [
      "com.example.addon.MyBBSAddon"
    ],
    "bbs-addon-client": [
      "com.example.addon.client.MyBBSClientAddon"
    ]
  },
  "depends": {
    "fabricloader": ">=0.14.21",
    "minecraft": "~1.20.1",
    "java": ">=17",
    "bbs_mod": ">=1.0.0"
  }
}
```

## Addon Structure

The BBS Mod addon system is divided into two main parts: the common/server side and the client side.

### BBSAddon (Common/Server)
Extend `mchorse.bbs_mod.addons.BBSAddon`. Handles logical registration: Forms, Clips, Settings, Molang functions.

### BBSClientAddon (Client)
Extend `mchorse.bbs_mod.addons.BBSClientAddon`. Handles visual registration: UI Panels, Renderers, Keyframe Editors.

## Core Concepts

### Forms and Data

**Forms** are the data structures for actors, blocks, and effects.
- **Inheritance**: All forms extend `mchorse.bbs_mod.forms.forms.Form`, which inherits from `ValueGroup`.
- **Serialization**: Forms automatically serialize to NBT/JSON via their `Value` fields.
- **Standard Fields**:
  - `visible` (ValueBoolean)
  - `transform` (ValueTransform: x, y, z, rotate, scale)
  - `color` (ValueInt)

To create a custom form:
```java
public class MyForm extends Form {
    public final ValueInt power = new ValueInt("power", 10);
    
    public MyForm() {
        super();
        this.register(this.power); // Important: Register value to be saved
    }
}
```

### UI System (Flex & Widgets)

BBS Mod uses a powerful **Flexbox-like** immediate mode UI system.

#### The Flex Layout
Every `UIElement` has a `flex` field (`this.flex`) to control positioning.
- **Size**: `.w(100)`, `.h(20)`, `.w(1F)` (100% width), `.wh(100, 20)`
- **Position**: `.x(10)`, `.y(50)`, `.xy(0.5F, 0.5F)` (center relative)
- **Anchors**: `.anchor(0.5F)` (center pivot point), `.anchor(1F)` (right/bottom pivot)
- **Relative**: `.relative(parent)` (default is parent, but can be other elements)
- **Layouts**: `.column(5)` (vertical stack), `.row(5)` (horizontal stack), `.grid(5)`

Example:
```java
UIElement container = new UIElement();
container.relative(parent).full(); // Fill parent

UIButton button = new UIButton(UIKeys.GENERAL_OK, (b) -> {});
button.relative(container).x(0.5F).y(0.5F).w(100).h(20).anchor(0.5F); // Centered button

container.add(button);
```

#### Common Widgets
- **UIButton**: Simple clickable button.
- **UIToggle**: Checkbox/Switch.
- **UIText**: Text input field.
- **UILabel**: Static text label.
- **UIIcon**: Renders an icon.
- **UIScrollView**: Scrollable container.
- **UIList / UISearchList**: Lists of items.

### Settings and Values

The `BaseValue` system is used for settings and data.
- **ValueBoolean**: `true` / `false`
- **ValueInt / ValueFloat / ValueDouble**: Numbers.
- **ValueString**: Text.
- **ValueEnum**: Enum selection.
- **ValueList**: A list of other values.
- **ValueGroup**: A map of name -> value (like a JSON object).

**Reactive Changes**:
```java
ValueBoolean toggle = new ValueBoolean("toggle", false);
toggle.postCallback((v) -> System.out.println("Changed to: " + v.get()));
```

### Event Bus

BBS Mod has its own `EventBus` for internal events, distinct from Fabric's callbacks.
Access it via `BBS.getEvents()`.

```java
BBS.getEvents().register(this);

@Subscribe
public void onFormRegister(RegisterFormsEvent event) {
    // alternative to BBSAddon method
}
```

## Server-Side Registration (BBSAddon)

Override these methods in your `BBSAddon` subclass.

### `registerForms(RegisterFormsEvent event)`
Registers actor forms.
```java
event.getForms().register("my_form", MyForm.class);
```

### `registerMolangFunctions(RegisterMolangFunctionsEvent event)`
Registers custom math functions for Molang.
```java
event.register("math.double", (ctx, args) -> args[0].get() * 2);
```

### `registerSettings(RegisterSettingsEvent event)`
Registers global config settings (appearing in the config panel).
```java
event.register(Icons.GEAR, "my_addon", (builder) -> {
    builder.category("general").register(new ValueBoolean("enabled", true));
});
```

### `registerCameraClips(RegisterCameraClipsEvent event)`
Registers camera clips.

### `registerActionClips(RegisterActionClipsEvent event)`
Registers action clips (Timeline).

### `registerActionConfigs(RegisterActionConfigsEvent event)`
Registers custom action timeline configuration data structures.
```java
event.register(myActionConfig);
```

### `registerParticleSimulations(RegisterParticleSimulationsEvent event)`
Registers custom particle simulation, update, and collision callbacks for particle emitters.
```java
event.register((emitter) -> {
    // Custom particle physics/simulation logic
});
```

## Client-Side Registration (BBSClientAddon)

Override these methods in your `BBSClientAddon` subclass.

### `registerDashboardPanels(RegisterDashboardPanelsEvent event)`
Adds tabs to the main dashboard.
```java
event.getDashboard().addPanel(new MyCustomPanel(event.getDashboard()));
```

### `registerFormsRenderers(RegisterFormsRenderersEvent event)`
Links a Form to its Renderer and Editor UI.
```java
event.registerRenderer(MyForm.class, MyFormRenderer::new);
event.registerPanel(MyForm.class, UIMyFormPanel::new);
```

### `registerModelLoaders(RegisterModelLoadersEvent event)`
Registers custom model formats for `models/<id>/...` loading.

```java
@Override
protected void registerModelLoaders(RegisterModelLoadersEvent event)
{
    event.registerLoader(new MyFormatModelLoader());
    event.registerRelodableSuffix(".myformat");
}
```

Notes:
- `registerLoader(...)` appends your `IModelLoader` to the model loader chain.
- `registerRelodableSuffix(...)` enables hot-reload invalidation for matching files.
- If your loader reads additional companion files, register all relevant suffixes.

### `registerUITheme(RegisterUIThemeEvent event)`
Registers a custom UI style/skin provider to customize widget colors, borders, font styles, and rendering without needing Mixins.
```java
event.register(new MyCustomUIStyleProvider());
```

### `registerFormEditorSection(RegisterFormEditorSectionEvent event)`
Injects custom UI sections or controls into existing Form editor panels (`UIFormPanel`).
```java
event.registerSection(MyForm.class, (formPanel, parentElement) -> {
    // Add custom widgets to the form editor UI
});
```

### `registerFormRenderPhase(RegisterFormRenderPhaseEvent event)`
Registers pre-render and post-render callbacks for Form drawing in the 3D world (e.g. motion blur, smears, silhouettes).
```java
event.registerPreRender((formRenderer) -> {
    // Pre-rendering logic
});
event.registerPostRender((formRenderer) -> {
    // Post-rendering logic
});
```

### `registerFormBlend(RegisterFormBlendEvent event)`
Registers form blending, deformation (smear), and shader pass handlers.
```java
event.registerBlendHandler((form, blendState) -> {
    // Custom blend / shader pass logic
});
```

### `registerClipInteraction(RegisterClipInteractionEvent event)`
Registers interaction, double-click, curve picker rerouting, and dope sheet post-rendering handlers for timeline clips.
```java
event.registerDoubleClick((clip) -> {
    // Handle double-click on clip
});
event.registerCurvePickerHandler((clipContext) -> {
    // Reroute or handle custom curve picker selection (e.g. Iris shader curve pickers)
});
event.registerDopeSheetRender((context, area) -> {
    // Render custom collaborator cursors, selection overlays, or markers on timeline graph
});
```

### `registerDockLayout(RegisterDockLayoutEvent event)`
Registers sub-panel layout extensions, mini-windows, docking layout modifications, and dashboard open/close lifecycle handlers.
```java
event.registerCustomizer((dashboard) -> {
    // Extend dashboard layout or dock mini-windows
});
event.registerDashboardOpen((dashboard) -> {
    // Handle dashboard opening (e.g. notify multiplayer presence)
});
event.registerDashboardClose((dashboard) -> {
    // Handle dashboard closing
});
```

### `registerParticleSchemeUI(RegisterParticleSchemeUIEvent event)`
Injects custom UI sections into the Particle Scheme editor panel (`UIParticleSchemePanel`).
```java
event.registerSection((appearanceView) -> {
    // Add custom sections to particle scheme UI
});
```

### `registerFilmControllerInteraction(RegisterFilmControllerInteractionEvent event)`
Routes mouse clicks, 3D viewport gizmo handles, and drag updates for film editing.
```java
event.registerClickHandler((filmController, context) -> {
    // Return true if click was consumed by custom viewport gizmo
    return false;
});
event.registerUpdateHandler((filmController, context) -> {
    // Update active gizmo drag state
});
```

### `registerSettingsUISection(RegisterSettingsUISectionEvent event)`
Appends custom UI sections or widgets to the Settings overlay panel (`UISettingsOverlayPanel`).
```java
event.registerSection((settingsOverlayPanel) -> {
    // Append custom sections to settings panel
});
```

### `registerFilmSync(RegisterFilmSyncEvent event)`
Registers handlers for film session lifecycle events (e.g. film open, save, and real-time multiplayer film collaboration syncing).
```java
event.registerOpenFilm((film) -> {
    // Handle film opened event for multiplayer sync
});
event.registerSaveFilm((film) -> {
    // Handle film saved event
});
```

### `registerL10n(RegisterL10nEvent event)`
Registers translation files.
```java
event.getL10n().register((lang) -> Link.create("my_addon", "strings/" + lang + ".json"));
```
*Note: See Advanced Topics for L10n reloading.*

### `registerIcons(RegisterIconsEvent event)`
Registers custom icons for use in UI.

### `registerGizmos(RegisterGizmoEvent event)`
Registers custom 3D gizmos for the scene editor.

### `registerPropTransforms(RegisterPropTransformEvent event)`
Registers custom property transformations.

### `registerFilmEditorFactories(RegisterFilmEditorFactoriesEvent event)`
Registers factories for custom film editor components.

### `registerReplayPanel(RegisterReplayPanelEvent event)`
Allows customization or extension of the Replay Panel.

### `registerReplayListContextMenu(RegisterReplayListContextMenuEvent event)`
Add custom actions to the Replay List context menu.

### `registerFilmPreview(RegisterFilmPreviewEvent event)`
Registers custom preview rendering logic for films.

### `registerRayTracing(RegisterRayTracingEvent event)`
Registers ray tracing extensions.

### `registerStencilMap(RegisterStencilMapEvent event)`
Registers stencil effects.

## Advanced Topics

### Customizing Replay Tracks

You can customize the visual appearance of property tracks in the Replay Editor (Timeline) by registering custom colors and icons. This is useful when your addon adds new animatable properties to forms.

To register a color or icon, call the static methods in `UIReplaysEditor` during your client-side initialization:

```java
import mchorse.bbs_mod.ui.film.replays.UIReplaysEditor;
import mchorse.bbs_mod.ui.utils.icons.Icons;

// In your client addon initialization
public void init() {
    // Register a custom color (0xRRGGBB)
    UIReplaysEditor.registerColor("Halo", 0xFFFFD3);
    UIReplaysEditor.registerColor("Horse", 0xFF1413);
    
    // Register a custom icon
    UIReplaysEditor.registerIcon("Photon", Icons.FADING);
    UIReplaysEditor.registerIcon("Sun", Icons.SUN);
    UIReplaysEditor.registerIcon("fog", Icons.SPRAY);
}
```

*Note: The ID should match the property name used in your Form.*

### Custom UI Components

To create a custom widget, extend `UIElement`.

```java
public class MyWidget extends UIElement {
    @Override
    public void render(UIContext context) {
        // Render background
        context.batcher.box(this.area.x, this.area.y, this.area.ex(), this.area.ey(), 0xFF000000);
        
        // Render text
        context.batcher.text("Hello", this.area.x + 5, this.area.y + 5, 0xFFFFFFFF);
        
        super.render(context); // Render children
    }
    
    @Override
    public boolean mouseClicked(UIContext context) {
        if (this.area.isInside(context)) {
            // Handle click
            return true; // Consume event
        }
        return super.mouseClicked(context);
    }
}
```

### Networking & PacketCrusher

If you need to send large data (like huge NBT tags) that exceeds standard packet limits, use `PacketCrusher`.

```java
// Sending
BBSMod.getNetwork().send(player, MY_PACKET_ID, myHugeData, (buf) -> {
    buf.writeInt(extraInfo);
});

// Receiving (use Crusher in handler)
crusher.receive(buf, (bytes, packetBuf) -> {
    // bytes contains the reconstructed full data
});
```

### UI Styling & Themes (IUIStyleProvider)

Addons can completely replace or customize the visual appearance of BBS widgets (buttons, toggles, textboxes, tooltips, panels) without Mixins by implementing `IUIStyleProvider` and registering it in `registerUITheme`:

```java
import mchorse.bbs_mod.ui.framework.theme.IUIStyleProvider;
import mchorse.bbs_mod.ui.framework.theme.UIThemeManager;

public class MyCustomUITheme implements IUIStyleProvider
{
    @Override
    public String getId()
    {
        return "my_theme";
    }

    @Override
    public String getName()
    {
        return "My Theme";
    }

    @Override
    public int getPrimaryColor()
    {
        return 0xFF3399FF;
    }

    @Override
    public int getBackgroundColor()
    {
        return 0xFF1E1E1E;
    }

    @Override
    public int getPanelBackgroundColor()
    {
        return 0xFF252526;
    }

    @Override
    public int getTextColor()
    {
        return 0xFFFFFFFF;
    }

    @Override
    public int getTooltipBackgroundColor()
    {
        return 0xFF000000;
    }

    @Override
    public int getTooltipTextColor()
    {
        return 0xFFEEEEEE;
    }

    @Override
    public boolean renderButtonSkin(UIContext context, UIButton button)
    {
        // Custom button rendering logic. Return true to consume drawing.
        return false;
    }
}
```

To activate a theme at runtime, use `UIThemeManager.setActiveTheme("my_theme");`.

### Localization (L10n)

The main mod loads translations before addons are fully registered. To ensure your addon's strings are loaded immediately:

```java
@Override
protected void registerL10n(RegisterL10nEvent event) {
    event.getL10n().register((lang) -> Link.create("my_addon", "strings/" + lang + ".json"));
    
    // Force reload to apply immediately
    event.getL10n().reload(); 
}
```

### Cross-Platform Compatibility (Sinytra Connector)

When running BBS Mod in hybrid environments (like using **Sinytra Connector** on NeoForge), the standard Fabric entrypoint detection for addons (`bbs-addon` and `bbs-addon-client`) might fail to populate the Addons Panel in the dashboard.

To ensure your addon is correctly displayed in the Addons Panel across all platforms, you can manually register your addon metadata using the `AddonInfo` class.

#### Manual Registration

In your client-side initialization (e.g., `onInitializeClient` or `BBSClientAddon` constructor), you can register your addon info directly:

```java
import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.addons.AddonInfo;
import mchorse.bbs_mod.resources.Link;

// ...

Link iconLink = new Link("my_addon_id", "icon.png"); // Path to icon in assets

AddonInfo info = new AddonInfo(
    "my_addon_id",
    "My Addon Name",
    "1.0.0",
    "Description of my addon.",
    java.util.List.of("Author1", "Author2"),
    iconLink,
    "https://website.com",
    "https://issues.com",
    "https://source.com"
);

BBSModClient.registerAddon(info);
```

This ensures that even if the mod loader fails to scrape the metadata from `fabric.mod.json`, the addon will still appear in the in-game UI.

#### Registering Assets for Icons

If your addon is running in a hybrid environment (Sinytra Connector), standard asset loading from the mod JAR might not work for the icon. You need to manually register an asset source pack to ensure the icon (and other assets) can be found.

```java
import mchorse.bbs_mod.resources.packs.InternalAssetsSourcePack;
import mchorse.bbs_mod.BBSMod;

// ...

// Register a source pack for your mod's namespace
BBSMod.getProvider().register(new InternalAssetsSourcePack("my_addon_id", "assets/my_addon_id", MyAddonClient.class));

// Register a root source pack for icons if they are in the root 'assets' folder (optional, if your icon is not in the namespace folder)
BBSMod.getProvider().register(new InternalAssetsSourcePack("my_addon_icons", "assets", MyAddonClient.class));
```

### Undo/Redo System

To support Undo/Redo in your editors, your UI elements must implement `IUndoElement` (which `UIElement` does).
When modifying values, use `BaseValue.edit()`:

```java
BaseValue.edit(this.myValue, (v) -> v.set(newValue));
```
This wraps the change in a transaction that the editor can undo.

## Utility Classes

- **BBS.getFactory()**: Access to various factories.
- **BBS.getFoundation()**: Core logic access.
- **BBSClient.getDashboard()**: Access the main UI.
- **Colors**: Utility for color manipulation (`Colors.A100` (alpha), `Colors.mulRGB`).
- **Icons**: Built-in icons (`Icons.CLOSE`, `Icons.ADD`).

## Best Practices

1.  **Use `Link`**: Always use `Link` (ResourceLocation) for IDs to avoid collisions.
2.  **Separate Client/Server**: Strict separation prevents `ClassNotFoundException` on servers.
3.  **Prefix Keys**: Prefix translation keys (`my_addon.key`) and NBT keys to avoid conflicts.
4.  **Use `UIOverlayPanel`**: For popups/selectors, extend `UIOverlayPanel` or `UIListOverlayPanel` for a native look.

## Troubleshooting

### "Class not found" on Server
**Cause**: Using client classes (`UIElement`, `MinecraftClient`) in `BBSAddon`.
**Fix**: Move code to `BBSClientAddon` or safe-guard with `FabricLoader`.

### Assets not loading
**Cause**: Wrong folder structure.
**Fix**: Must be `src/main/resources/assets/<namespace>/...`.

### Events not firing
**Cause**: Missing entrypoints in `fabric.mod.json`.
**Fix**: Verify `bbs-addon` and `bbs-addon-client` entries.

## Step-by-Step Example

Here is a comprehensive example of an addon that registers a custom form, a Molang function, and a dashboard panel.

**1. Common Addon Class**

```java
package com.example.addon;

import mchorse.bbs_mod.addons.BBSAddon;
import mchorse.bbs_mod.events.register.RegisterFormsEvent;
import mchorse.bbs_mod.events.register.RegisterMolangFunctionsEvent;

public class MyBBSAddon extends BBSAddon
{
    @Override
    protected void registerForms(RegisterFormsEvent event)
    {
        event.getForms().register("my_cube", MyCubeForm.class);
    }

    @Override
    protected void registerMolangFunctions(RegisterMolangFunctionsEvent event)
    {
        event.register("math.triple", (context, args) -> {
            if (args.length == 0) return 0;
            return args[0].get() * 3;
        });
    }
}
```

**2. Client Addon Class**

```java
package com.example.addon.client;

import mchorse.bbs_mod.addons.BBSClientAddon;
import mchorse.bbs_mod.events.register.RegisterDashboardPanelsEvent;
import mchorse.bbs_mod.events.register.RegisterFormsRenderersEvent;
import mchorse.bbs_mod.ui.dashboard.UIDashboard;

public class MyBBSClientAddon extends BBSClientAddon
{
    @Override
    protected void registerDashboardPanels(RegisterDashboardPanelsEvent event)
    {
        UIDashboard dashboard = event.getDashboard();
        dashboard.addPanel(new MyCustomPanel(dashboard));
    }

    @Override
    protected void registerFormsRenderers(RegisterFormsRenderersEvent event)
    {
        // Register renderer (how it looks in world)
        event.registerRenderer(MyCubeForm.class, MyCubeFormRenderer::new);
        // Register editor panel (how it looks in dashboard)
        event.registerPanel(MyCubeForm.class, UIMyCubeFormPanel::new);
    }
}
```

**3. Custom Form (Data)**

```java
package com.example.addon;

import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.settings.values.ValueInt;

public class MyCubeForm extends Form
{
    public final ValueInt size = new ValueInt("size", 1);

    public MyCubeForm()
    {
        super();
        this.register(this.size);
    }
}
```

**4. Editor UI Panel**

```java
package com.example.addon.client;

import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.input.UITrackpad;
import mchorse.bbs_mod.ui.utils.UI;
import mchorse.bbs_mod.ui.forms.UIFormPanel;
import com.example.addon.MyCubeForm;

public class UIMyCubeFormPanel extends UIFormPanel<MyCubeForm>
{
    public UITrackpad size;

    public UIMyCubeFormPanel(MyCubeForm form)
    {
        super(form);

        this.size = new UITrackpad((v) -> this.form.size.set(v.intValue()));
        this.size.setValue(this.form.size.get());

        // Layout
        this.add(UI.label(IKey.str("Size")), this.size);
        this.size.relative(this).w(100);
    }
}
```
