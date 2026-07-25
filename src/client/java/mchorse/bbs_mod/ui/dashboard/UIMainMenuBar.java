package mchorse.bbs_mod.ui.dashboard;

import mchorse.bbs_mod.BBSMod;
import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.l10n.L10n;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.settings.values.core.ValueGroup;
import mchorse.bbs_mod.text.RtlAwtTextRenderer;
import mchorse.bbs_mod.text.RtlTextEngine;
import mchorse.bbs_mod.ui.ContentType;
import mchorse.bbs_mod.ui.Keys;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.dashboard.panels.UIDashboardPanel;
import mchorse.bbs_mod.ui.dashboard.panels.UIDataDashboardPanel;
import mchorse.bbs_mod.ui.dashboard.panels.overlay.UIAboutOverlayPanel;
import mchorse.bbs_mod.ui.dashboard.panels.overlay.UIOpenAssetOverlayPanel;
import mchorse.bbs_mod.ui.dashboard.utils.UIGraphPanel;
import mchorse.bbs_mod.ui.film.UIFilmLogOverlayPanel;
import mchorse.bbs_mod.ui.film.UIFilmPanel;
import mchorse.bbs_mod.ui.film.UIWorldFilmsBrowserPanel;
import mchorse.bbs_mod.ui.film.utils.FilmProjectHandler;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIButton;
import mchorse.bbs_mod.ui.framework.elements.events.UIRemovedEvent;
import mchorse.bbs_mod.ui.framework.elements.overlay.UICreateAssetOverlayPanel;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIOverlay;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIPromptOverlayPanel;
import mchorse.bbs_mod.ui.model_blocks.UIModelBlockPanel;
import mchorse.bbs_mod.ui.selectors.UISelectorsOverlayPanel;
import mchorse.bbs_mod.ui.triggers.UITriggerBlockPanel;
import mchorse.bbs_mod.ui.utils.context.ContextMenuManager;
import mchorse.bbs_mod.ui.utils.context.ContextSeparatorAction;
import mchorse.bbs_mod.ui.utils.icons.Icon;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import mchorse.bbs_mod.ui.utils.keys.KeyCombo;
import mchorse.bbs_mod.ui.utils.keys.Keybind;
import mchorse.bbs_mod.utils.RecentAssetsTracker;
import mchorse.bbs_mod.utils.colors.Colors;
import mchorse.bbs_mod.utils.repos.IRepository;

import net.minecraft.client.MinecraftClient;

import java.util.function.Consumer;

public class UIMainMenuBar extends UIElement
{
    private UIDashboard dashboard;
    UIMenuButton activeButton = null;
    UIWorldMenuButton activeWorldButton = null;
    private UIMenuButton toolsMenu;
    private UIWorldMenuButton worldButton;

    public UIMainMenuBar(UIDashboard dashboard)
    {
        this.dashboard = dashboard;

        this.h(20);

        UIElement brand = new UIElement()
        {
            @Override
            public void render(UIContext context)
            {
                String title = "BBS";
                int y = this.area.my(context.batcher.getFont().getHeight());

                context.batcher.textShadow(title, this.area.x, y, 0xFFCCCCCC);
                super.render(context);
            }
        };
        brand.w(25).marginLeft(6);

        this.add(brand);
        this.add(new UIMenuButton(UIKeys.RAW_FILE, this, this::buildFileMenu));
        this.add(new UIMenuButton(UIKeys.RAW_EDIT, this, this::buildEditMenu));
        this.toolsMenu = new UIMenuButton(UIKeys.RAW_TOOLS, this, this::buildToolsMenu);
        this.add(this.toolsMenu);
        /* Window menu is always visible; its content adapts to the active panel
           (currently only the Model Editor populates it). */
        this.add(new UIMenuButton(UIKeys.RAW_WINDOW, this, this::buildWindowMenu));

        this.add(new UIMenuButton(UIKeys.RAW_HELP, this, this::buildHelpMenu));
        this.worldButton = new UIWorldMenuButton(UIKeys.RAW_WORLD, this);
        this.add(this.worldButton);

        this.row(2).preferred(999);
    }

    public void updateForPanel(UIDashboardPanel panel)
    {
        boolean stripped = UIWorldFilmsBrowserPanel.isBrowserPanel(panel);

        if (this.toolsMenu != null)
        {
            this.toolsMenu.setVisible(!stripped);
        }

        if (this.worldButton != null)
        {
            this.worldButton.setVisible(!stripped);
        }
    }

    @Override
    public void render(UIContext context)
    {
        context.batcher.box(this.area.x, this.area.y, this.area.ex(), this.area.ey(), 0xFF141418);
        context.batcher.box(this.area.x, this.area.ey() - 1, this.area.ex(), this.area.ey(), 0xFF2A2A35);

        super.render(context);
    }

    /* ------------------------------------------------------------------ */
    /* Menu open/close                                                       */
    /* ------------------------------------------------------------------ */

    void toggleMenu(UIMenuButton button, Consumer<ContextMenuManager> consumer)
    {
        UIContext context = this.getContext();

        context.closeContextMenu();
        this.activeButton = null;
        this.activeWorldButton = null;

        /* Use wasActiveLastFrame (captured in render, before events fire) so that
           the context menu closing itself first doesn't confuse the toggle check. */
        if (!button.wasActiveLastFrame)
        {
            this.openMenuBelow(button, consumer);
        }
    }

    void openMenuBelow(UIMenuButton button, Consumer<ContextMenuManager> consumer)
    {
        UIContext context = this.getContext();

        this.activeWorldButton = null;
        context.replaceContextMenu((menu) ->
        {
            consumer.accept(menu);
            menu.onClose((e) -> this.activeButton = null);
        });

        if (context.contextMenu != null)
        {
            context.contextMenu.getFlex().x.set(0, button.area.x);
            context.contextMenu.getFlex().y.set(0, button.area.ey());
            context.contextMenu.bounds(context.menu.overlay, 5);
            context.contextMenu.resize();
        }

        this.activeButton = button;
    }

    void toggleWorldMenu(UIWorldMenuButton button)
    {
        UIContext context = this.getContext();

        context.closeContextMenu();
        this.activeButton = null;
        this.activeWorldButton = null;

        if (!button.wasActiveLastFrame)
        {
            this.openWorldDropdown(button);
        }
    }

    void openWorldDropdown(UIWorldMenuButton button)
    {
        UIContext context = this.getContext();
        UIWorldDropdownMenu menu = new UIWorldDropdownMenu();

        menu.getEvents().register(UIRemovedEvent.class, (e) -> this.activeWorldButton = null);
        this.activeButton = null;
        context.replaceContextMenu(menu);

        if (context.contextMenu != null)
        {
            int maxH = Math.max(114, context.menu.height - button.area.ey() - 10);

            menu.setMaxHeight(maxH);
            context.contextMenu.getFlex().x.set(0, button.area.x);
            context.contextMenu.getFlex().y.set(0, button.area.ey());
            context.contextMenu.bounds(context.menu.overlay, 5);
            context.contextMenu.resize();
        }

        this.activeWorldButton = button;
    }

    boolean hasAnyMenuOpen()
    {
        return this.activeButton != null || this.activeWorldButton != null;
    }

    /* ------------------------------------------------------------------ */
    /* Menu builders                                                         */
    /* ------------------------------------------------------------------ */

    private void buildFileMenu(ContextMenuManager menu)
    {
        menu.action(Icons.ADD, UIKeys.RAW_NEW, () -> this.openNewSubmenu());
        menu.action(Icons.FOLDER, UIKeys.RAW_OPEN, () -> this.openOpenPopup());
        menu.action(Icons.TIME, UIKeys.RAW_RECENT, () -> this.openRecentSubmenu());

        if (this.dashboard.panels.panel instanceof UIFilmPanel filmPanel && filmPanel.getData() != null)
        {
            menu.action(Icons.UPLOAD, UIKeys.FILM_EXPORT_PROJECT, () -> FilmProjectHandler.exportProject(filmPanel));
        }

        menu.action(Icons.SETTINGS, UIKeys.CONFIG_TITLE, () -> UIOverlay.addOverlay(this.getContext(), this.dashboard.settingsPanel, 580, 340));
        menu.action(Icons.JOYSTICK, UIKeys.ADDONS_TITLE, () -> UIOverlay.addOverlay(this.getContext(), this.dashboard.addonsPanel, 520, 320));
    }

    private void buildEditMenu(ContextMenuManager menu)
    {
        menu.action(Icons.UNDO, UIKeys.CAMERA_EDITOR_KEYS_EDITOR_UNDO, () -> this.triggerKey(Keys.UNDO));
        menu.action(Icons.REDO, UIKeys.CAMERA_EDITOR_KEYS_EDITOR_REDO, () -> this.triggerKey(Keys.REDO));

        if (this.dashboard.panels.panel instanceof UIFilmPanel film)
        {
            menu.action(Icons.LIST, UIKeys.FILM_OPEN_HISTORY, film::openUndoHistory);
        }
    }

    private void buildToolsMenu(ContextMenuManager menu)
    {
        menu.action(Icons.PROPERTIES, UIKeys.SELECTORS_TITLE, () ->
            UIOverlay.addOverlayRight(this.getContext(), new UISelectorsOverlayPanel(), 240));
        menu.action(Icons.GRAPH, UIKeys.GRAPH_TOOLTIP, () -> {
            if (this.dashboard.documentTabsBar != null)
            {
                this.dashboard.documentTabsBar.addOrActivate(ContentType.GRAPH, "graph_calculator");
            }
        });

        if (this.dashboard.panels.panel instanceof UIFilmPanel filmPanel && filmPanel.getData() != null)
        {
            filmPanel.addToolMenuActions(menu);

            menu.action(Icons.TIME, UIKeys.FILM_LOG_TOOLS, () -> {
                UIOverlay.addOverlay(this.getContext(), new UIFilmLogOverlayPanel(filmPanel), 280, 240);
            });
        }
    }

    private void buildHelpMenu(ContextMenuManager menu)
    {
        menu.action(Icons.HELP, UIKeys.RAW_ABOUT, () -> UIOverlay.addOverlay(this.getContext(), new UIAboutOverlayPanel(UIKeys.RAW_ABOUT, this.dashboard), 560, 440));
    }

    private void buildWindowMenu(ContextMenuManager menu)
    {
        if (this.dashboard.panels.panel instanceof UIModelBlockPanel panel)
        {
            menu.action(panel.isLeftVisible() ? Icons.CHECKMARK : Icons.NONE, UIKeys.MODEL_BLOCKS_TITLE, () ->
            {
                panel.setLeftVisible(!panel.isLeftVisible());
            });
            menu.action(panel.isMiddleVisible() ? Icons.CHECKMARK : Icons.NONE, UIKeys.MODEL_BLOCKS_PROPERTIES, () ->
            {
                panel.setMiddleVisible(!panel.isMiddleVisible());
            });
            menu.action(panel.isRightVisible() ? Icons.CHECKMARK : Icons.NONE, UIKeys.MODEL_BLOCKS_TRANSFORMS, () ->
            {
                panel.setRightVisible(!panel.isRightVisible());
            });
            menu.action(Icons.REFRESH, UIKeys.DASHBOARD_MENU_RESET_LAYOUT, panel::resetLayout);
        }
        else if (this.dashboard.panels.panel instanceof UITriggerBlockPanel trigger)
        {
            menu.action(trigger.isListVisible() ? Icons.CHECKMARK : Icons.NONE, IKey.constant("Triggers"), () ->
            {
                trigger.setListVisible(!trigger.isListVisible());
            });
            menu.action(trigger.isActionsVisible() ? Icons.CHECKMARK : Icons.NONE, IKey.constant("Actions"), () ->
            {
                trigger.setActionsVisible(!trigger.isActionsVisible());
            });
            menu.action(trigger.isGeometryVisible() ? Icons.CHECKMARK : Icons.NONE, IKey.constant("Geometry"), () ->
            {
                trigger.setGeometryVisible(!trigger.isGeometryVisible());
            });
            menu.action(Icons.REFRESH, IKey.constant("Reset Layout"), trigger::resetLayout);
        }
        else if (this.dashboard.panels.panel instanceof UIFilmPanel film)
        {
            for (String panelId : film.getWindowPanelIds())
            {
                menu.action(film.isWindowPanelVisible(panelId) ? Icons.CHECKMARK : Icons.NONE, film.getWindowPanelTitle(panelId), () ->
                {
                    film.setWindowPanelVisible(panelId, !film.isWindowPanelVisible(panelId));
                });
            }

            menu.action(new ContextSeparatorAction());
            menu.action(Icons.REFRESH, UIKeys.DASHBOARD_MENU_RESET_LAYOUT, film::resetLayout);
            menu.action(film.isLayoutLocked() ? Icons.LOCKED : Icons.UNLOCKED, film.isLayoutLocked() ? UIKeys.FILM_LAYOUT_UNLOCK : UIKeys.FILM_LAYOUT_LOCK, film::toggleLayoutLockFromMenu);
            menu.action(Icons.SAVED, UIKeys.FILM_LAYOUT_PRESETS, () ->
            {
                int x = this.activeButton == null ? this.area.x : this.activeButton.area.x;
                int y = this.activeButton == null ? this.area.ey() : this.activeButton.area.ey();

                film.openLayoutPresetsFromMenu(x, y);
            });
        }
        else
        {
            menu.action(Icons.NONE, UIKeys.DASHBOARD_MENU_NO_WINDOWS, () -> {});
        }
    }

    /* ------------------------------------------------------------------ */
    /* Submenu actions                                                       */
    /* ------------------------------------------------------------------ */

    private void openNewSubmenu()
    {
        this.getContext().replaceContextMenu((menu) ->
        {
            menu.action(Icons.FILM, UIKeys.FILM_TITLE, () -> this.createNewAsset(ContentType.FILMS));
            menu.action(Icons.PARTICLE, UIKeys.PANELS_PARTICLES, () -> this.createNewAsset(ContentType.PARTICLES));
            menu.action(Icons.PLAYER, UIKeys.MODELS_TITLE, () -> this.createNewAsset(ContentType.MODELS));
        });
    }

    private void openRecentSubmenu()
    {
        this.getContext().replaceContextMenu((menu) ->
        {
            if (RecentAssetsTracker.RECENT.isEmpty())
            {
                menu.action(Icons.NONE, UIKeys.RAW_NO_RECENT_ASSETS, () -> {});
                return;
            }

            for (RecentAssetsTracker.Entry entry : RecentAssetsTracker.RECENT)
            {
                if (RecentAssetsTracker.shouldExcludeFromRecent(entry.type, entry.id))
                {
                    continue;
                }

                menu.action(this.iconFor(entry.type), IKey.raw(entry.id), () ->
                {
                    UIDataDashboardPanel panel = entry.type != null ? entry.type.get(this.dashboard) : null;

                    if (panel != null)
                    {
                        this.dashboard.setPanel(panel);
                        panel.pickData(entry.id);
                    }
                });
            }
        });
    }

    private void createNewAsset(ContentType type)
    {
        UICreateAssetOverlayPanel panel = new UICreateAssetOverlayPanel(
            type,
            (name) ->
            {
                IRepository repository = type.getRepository();
                ValueGroup created = (ValueGroup) repository.create(name);

                if (created != null)
                {
                    repository.save(name, created.toData().asMap());
                }

                UIDataDashboardPanel dashboardPanel = type.get(this.dashboard);

                if (dashboardPanel != null)
                {
                    this.dashboard.setPanel(dashboardPanel);
                    dashboardPanel.pickData(name);
                }
            }
        );
        UIOverlay.addOverlay(this.getContext(), panel, 260, 160);
    }

    private void openOpenPopup()
    {
        UIOverlay.addOverlay(this.getContext(), new UIOpenAssetOverlayPanel(UIKeys.RAW_OPEN_ASSET, this.dashboard), 520, 320);
    }

    private void triggerKey(KeyCombo combo)
    {
        if (this.dashboard.panels.panel == null)
        {
            return;
        }

        for (Keybind keybind : this.dashboard.panels.panel.keys().keybinds)
        {
            if (combo.equals(keybind.getCombo()) && keybind.isActive())
            {
                keybind.callback.run();
                return;
            }
        }
    }

    private Icon iconFor(ContentType type)
    {
        if (type == ContentType.FILMS) return Icons.FILM;
        if (type == ContentType.PARTICLES) return Icons.PARTICLE;
        if (type == ContentType.MODELS) return Icons.PLAYER;
        if (type == ContentType.SOUNDS) return Icons.SOUND;

        return Icons.NONE;
    }

    /* ------------------------------------------------------------------ */
    /* Menu button                                                           */
    /* ------------------------------------------------------------------ */

    public static class UIWorldMenuButton extends UIButton
    {
        final UIMainMenuBar bar;
        private boolean prevHover = false;
        boolean wasActiveLastFrame = false;

        public UIWorldMenuButton(IKey label, UIMainMenuBar bar)
        {
            super(label, null);

            this.bar = bar;
            this.callback = (b) -> this.bar.toggleWorldMenu(this);
            this.setSizeFromLabel(label);
        }

        private void setSizeFromLabel(IKey label)
        {
            try
            {
                int textWidth = RtlAwtTextRenderer.isReady() && RtlTextEngine.isActive()
                    ? RtlAwtTextRenderer.getWidth(label.get())
                    : MinecraftClient.getInstance().textRenderer.getWidth(label.get());
                this.w(textWidth + 10);
            }
            catch (Exception e)
            {
                this.w(28);
            }
        }

        @Override
        public void resize()
        {
            this.setSizeFromLabel(this.label);
            super.resize();
        }

        @Override
        public void render(UIContext context)
        {
            /* Freeze while pressed: context menu closes on mouse-down (outside click) before
             * release fires the toggle — updating here would clear wasActive and reopen. */
            if (!this.pressed)
            {
                this.wasActiveLastFrame = this.bar.activeWorldButton == this;
            }

            boolean nowHovered = this.area.isInside(context);

            if (nowHovered && !this.prevHover && this.bar.hasAnyMenuOpen() && this.bar.activeWorldButton != this)
            {
                this.bar.openWorldDropdown(this);
            }

            this.prevHover = nowHovered;

            super.render(context);
        }

        @Override
        protected void renderSkin(UIContext context)
        {
            boolean active = this.bar.activeWorldButton == this;
            boolean hovered = this.area.isInside(context);

            if (active)
            {
                context.batcher.box(this.area.x, this.area.y, this.area.ex(), this.area.ey(),
                    Colors.setA(BBSSettings.primaryColor.get(), 0.55F));
            }
            else if (hovered)
            {
                context.batcher.box(this.area.x, this.area.y, this.area.ex(), this.area.ey(), Colors.A25);
            }

            int x = this.area.mx(context.batcher.getFont().getWidth(this.label.get()));
            int y = this.area.my(context.batcher.getFont().getHeight());

            context.batcher.textShadow(this.label.get(), x, y, Colors.WHITE);
        }
    }

    public static class UIMenuButton extends UIButton
    {
        final UIMainMenuBar bar;
        final Consumer<ContextMenuManager> menuConsumer;
        private boolean prevHover = false;

        /* Captured during render (before events fire) — used by toggleMenu to
           determine whether this button's menu was open when the click started. */
        boolean wasActiveLastFrame = false;

        public UIMenuButton(IKey label, UIMainMenuBar bar, Consumer<ContextMenuManager> menuConsumer)
        {
            super(label, null);

            this.bar = bar;
            this.menuConsumer = menuConsumer;
            this.callback = (b) -> this.bar.toggleMenu(this, this.menuConsumer);

            try
            {
                int textWidth = RtlAwtTextRenderer.isReady() && RtlTextEngine.isActive()
                    ? RtlAwtTextRenderer.getWidth(label.get())
                    : MinecraftClient.getInstance().textRenderer.getWidth(label.get());
                this.w(textWidth + 10);
            }
            catch (Exception e)
            {
                this.w(28);
            }
        }

        @Override
        public void resize()
        {
            try
            {
                int textWidth = RtlAwtTextRenderer.isReady() && RtlTextEngine.isActive()
                    ? RtlAwtTextRenderer.getWidth(this.label.get())
                    : MinecraftClient.getInstance().textRenderer.getWidth(this.label.get());
                this.w(textWidth + 10);
            }
            catch (Exception e)
            {
                this.w(28);
            }
            super.resize();
        }

        @Override
        public void render(UIContext context)
        {
            /* Freeze while pressed — same as World: menu closes on mouse-down before release. */
            if (!this.pressed)
            {
                this.wasActiveLastFrame = this.bar.activeButton == this;
            }

            boolean nowHovered = this.area.isInside(context);

            /* Switch menus on hover when another menu is already open */
            if (nowHovered && !this.prevHover
                && this.bar.hasAnyMenuOpen()
                && this.bar.activeButton != this)
            {
                this.bar.openMenuBelow(this, this.menuConsumer);
            }

            this.prevHover = nowHovered;

            super.render(context);
        }

        @Override
        protected void renderSkin(UIContext context)
        {
            boolean active = this.bar.activeButton == this;
            boolean hovered = this.area.isInside(context);

            if (active)
            {
                context.batcher.box(this.area.x, this.area.y, this.area.ex(), this.area.ey(),
                    Colors.setA(BBSSettings.primaryColor.get(), 0.55F));
            }
            else if (hovered)
            {
                context.batcher.box(this.area.x, this.area.y, this.area.ex(), this.area.ey(), Colors.A25);
            }

            int x = this.area.mx(context.batcher.getFont().getWidth(this.label.get()));
            int y = this.area.my(context.batcher.getFont().getHeight());

            context.batcher.textShadow(this.label.get(), x, y, Colors.WHITE);
        }
    }
}
