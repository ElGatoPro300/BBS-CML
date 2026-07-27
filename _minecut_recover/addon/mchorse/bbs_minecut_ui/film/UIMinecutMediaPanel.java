package mchorse.bbs_minecut_ui.film;

import mchorse.bbs_mod.camera.clips.ClipFactoryData;
import mchorse.bbs_mod.film.replays.ModelTrackIds;
import mchorse.bbs_mod.film.Film;
import mchorse.bbs_mod.film.replays.Replay;
import mchorse.bbs_mod.forms.FormUtilsClient;
import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.graphics.window.Window;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.ui.Keys;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.film.UIClipsPanel;
import mchorse.bbs_mod.ui.film.UIFilmPanel;
import mchorse.bbs_mod.ui.film.replays.UIReplayList;
import mchorse.bbs_mod.ui.film.replays.UIReplaysEditor;
import mchorse.bbs_mod.ui.film.toolbar.TimelineClipTypeGroups;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.IUIElement;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.UIScrollView;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIButton;
import mchorse.bbs_mod.ui.framework.elements.utils.UIDraggable;
import mchorse.bbs_minecut_ui.styles.MinecutTokens;
import mchorse.bbs_mod.ui.framework.styles.UIStyle;
import mchorse.bbs_mod.ui.utils.ScrollDirection;
import mchorse.bbs_mod.ui.utils.context.ContextMenuManager;
import mchorse.bbs_mod.ui.utils.icons.Icon;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import mchorse.bbs_mod.utils.MathUtils;
import mchorse.bbs_mod.utils.clips.Clip;
import mchorse.bbs_mod.utils.colors.Colors;
import mchorse.bbs_mod.utils.factory.IFactory;

import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Minecut Media dock leaf: Replays cards, Camera palette, or Actions palette.
 * Classic tab chrome supplies the label when several share a {@code TabbedNode}.
 */
public class UIMinecutMediaPanel extends UIMinecutRegion
{
    public enum Mode
    {
        REPLAYS,
        CAMERA,
        ACTIONS,
        TRACKS
    }

    private static final int GAP = 6;
    private static final int CARD_W = 78;
    private static final int CARD_H = 72;
    private static final int LIST_H = 36;
    private static final int HINT_H = 22;
    private static final int ROW_H = 28;
    private static final int GROUP_H = 28;
    private static final float PROPS_RATIO = 0.58F;

    private final UIFilmPanel film;
    private final Mode mode;
    private final UIScrollView grid;
    private final UIButton addReplayButton;
    private final UIButton listModeButton;
    private final UIElement replayPropsHost;
    private final Map<String, Boolean> groupExpanded = new HashMap<>();
    private final Map<String, Float> groupOpen = new HashMap<>();
    private int buildY;
    private int replayCardCount = -1;
    private boolean animating;
    private boolean listMode;
    private float cardScale = 1F;
    private float propsOpen;
    private boolean propsWantOpen;
    private int propsReplayIndex = -1;

    private boolean paletteDragging;
    private boolean trackPaletteDrag;
    private Icon paletteDragIcon;
    private String paletteDragLabel;
    private int paletteDragAccent;
    private String trackPaletteType;

    public UIMinecutMediaPanel(UIFilmPanel film, Mode mode)
    {
        super(mode == Mode.REPLAYS ? "Replays"
            : mode == Mode.CAMERA ? "Camera"
            : mode == Mode.ACTIONS ? "Actions"
            : "Tracks");
        this.noHeader();
        this.film = film;
        this.mode = mode;

        this.addReplayButton = new UIButton(IKey.constant("+ Add replay"), (b) -> this.handleAddReplay());
        this.addReplayButton.tooltip(UIKeys.SCENE_REPLAYS_CONTEXT_ADD);
        this.addReplayButton.relative(this).x(8).y(2).w(1F, -48).h(HINT_H - 4);
        this.addReplayButton.context(this::fillReplayContextMenu);
        this.add(this.addReplayButton);

        this.listModeButton = new UIButton(IKey.constant("☰"), (b) -> this.toggleListMode());
        this.listModeButton.tooltip(IKey.constant("Grid / List"));
        this.listModeButton.relative(this).x(1F, -36).y(2).w(28).h(HINT_H - 4);
        this.add(this.listModeButton);

        this.grid = new UIScrollView();
        this.grid.scroll.direction = ScrollDirection.VERTICAL;
        this.grid.scroll.scrollSpeed = 20;
        this.grid.relative(this).x(0).y(HINT_H).w(1F).h(1F, -HINT_H);
        this.grid.context(this::fillReplayContextMenu);
        this.add(this.grid);

        this.replayPropsHost = new UIElement();
        this.replayPropsHost.relative(this).x(1F).y(0).w(0).h(1F);
        this.replayPropsHost.setVisible(false);
        this.add(this.replayPropsHost);

        this.addReplayButton.setVisible(mode == Mode.REPLAYS);
        this.listModeButton.setVisible(mode == Mode.REPLAYS);

        if (mode == Mode.REPLAYS)
        {
            /* Overlay is hidden in Minecut — its .inside() keybinds never fire. Mirror them here. */
            this.keys().register(Keys.REPLAYS_REMOVE, () ->
            {
                UIReplayList list = this.getReplayList();

                if (list != null)
                {
                    list.removeReplay();
                }
            }).inside().active(() ->
            {
                UIReplayList list = this.getReplayList();

                return list != null && !list.getCurrent().isEmpty();
            });
            this.keys().register(Keys.FORMS_PICK, () ->
            {
                if (this.film.anchoredReplaysPanel != null && this.film.anchoredReplaysPanel.pickEdit != null)
                {
                    this.film.anchoredReplaysPanel.pickEdit.pick.clickItself();
                }
            }).inside().active(() -> this.film.anchoredReplaysPanel != null && !this.film.anchoredReplaysPanel.replays.getCurrent().isEmpty());
            this.keys().register(Keys.FORMS_EDIT, () ->
            {
                if (this.film.anchoredReplaysPanel != null && this.film.anchoredReplaysPanel.pickEdit != null)
                {
                    this.film.anchoredReplaysPanel.pickEdit.edit.clickItself();
                }
            }).inside().active(() -> this.film.anchoredReplaysPanel != null && !this.film.anchoredReplaysPanel.replays.getCurrent().isEmpty());
        }
    }

    public Mode getMode()
    {
        return this.mode;
    }

    public UIElement getReplayPropertiesHost()
    {
        return this.replayPropsHost;
    }

    private int topBarH()
    {
        return HINT_H;
    }

    private int propsPixelW()
    {
        if (this.mode != Mode.REPLAYS || this.area.w <= 0)
        {
            return 0;
        }

        return Math.round(this.area.w * PROPS_RATIO * this.propsOpen);
    }

    private void syncReplaySplitLayout()
    {
        boolean replays = this.mode == Mode.REPLAYS;
        int top = this.topBarH();
        int propsW = this.propsPixelW();
        boolean propsVisible = replays && propsW > 2;

        this.replayPropsHost.setVisible(propsVisible);
        this.addReplayButton.setVisible(replays);
        this.listModeButton.setVisible(replays);

        if (replays && propsVisible)
        {
            /* Props fill the full height (including the top toolbar band) so General
               is not stuck under empty space next to + Add replay. */
            this.grid.resetFlex().relative(this).x(0).y(top).w(1F, -propsW).h(1F, -top);
            this.replayPropsHost.resetFlex().relative(this).x(1F, -propsW).y(0).w(0F, propsW).h(1F);
            this.addReplayButton.resetFlex().relative(this).x(8).y(2).w(1F, -(propsW + 48)).h(HINT_H - 4);
            this.listModeButton.resetFlex().relative(this).x(1F, -(propsW + 36)).y(2).w(28).h(HINT_H - 4);
        }
        else
        {
            this.grid.resetFlex().relative(this).x(0).y(top).w(1F).h(1F, -top);
            this.replayPropsHost.resetFlex().relative(this).x(1F).y(0).w(0).h(1F);
            this.addReplayButton.resetFlex().relative(this).x(8).y(2).w(1F, -48).h(HINT_H - 4);
            this.listModeButton.resetFlex().relative(this).x(1F, -36).y(2).w(28).h(HINT_H - 4);
        }

        this.grid.resize();

        if (propsVisible)
        {
            this.replayPropsHost.resize();
        }
    }

    private void toggleListMode()
    {
        this.listMode = !this.listMode;
        this.refreshCards();
    }

    public void refreshCards()
    {
        this.rebuildContent(true);
    }

    private void rebuildContent(boolean clearDrag)
    {
        this.grid.removeAll();
        this.buildY = GAP;

        if (clearDrag && !this.trackPaletteDrag)
        {
            this.clearPaletteDrag();
        }

        Film data = this.film.getData();

        if (data == null)
        {
            this.grid.setVisible(false);
            this.addReplayButton.setVisible(false);
            this.listModeButton.setVisible(false);
            this.replayCardCount = 0;

            return;
        }

        this.grid.setVisible(true);
        this.addReplayButton.setVisible(this.mode == Mode.REPLAYS);
        this.listModeButton.setVisible(this.mode == Mode.REPLAYS);

        if (this.mode == Mode.REPLAYS)
        {
            this.fillReplayCards(data);
            int cardW = this.scaledCardW();
            int cardH = this.scaledCardH();
            int listW = Math.max(80, this.area.w - this.propsPixelW() - 16);

            if (this.listMode)
            {
                this.grid.scroll.scrollSize = GAP + data.replays.getList().size() * (LIST_H + GAP) + GAP;
            }
            else
            {
                int cols = Math.max(1, listW / (cardW + GAP));
                int rows = (int) Math.ceil(data.replays.getList().size() / (double) cols);

                this.grid.scroll.scrollSize = GAP + rows * (cardH + 16 + GAP) + GAP;
            }
        }
        else if (this.mode == Mode.TRACKS)
        {
            this.fillModelTrackPalette();
            this.grid.scroll.scrollSize = this.buildY + GAP;
        }
        else
        {
            this.fillClipTypePalette(this.mode == Mode.CAMERA);
            this.grid.scroll.scrollSize = this.buildY + GAP;
        }

        this.syncReplaySplitLayout();
        this.grid.resize();
    }

    private int scaledCardW()
    {
        return Math.max(48, Math.round(CARD_W * this.cardScale));
    }

    private int scaledCardH()
    {
        return Math.max(44, Math.round(CARD_H * this.cardScale));
    }

    private void openPropsPanel(int replayIndex)
    {
        this.propsReplayIndex = replayIndex;
        this.propsWantOpen = true;
        this.selectReplayOnly(replayIndex);
    }

    private void closePropsPanel()
    {
        this.propsWantOpen = false;
        this.propsReplayIndex = -1;
    }

    private UIReplayList getReplayList()
    {
        if (this.film.replayEditor == null || this.film.replayEditor.replays == null)
        {
            return null;
        }

        return this.film.replayEditor.replays.replays;
    }

    private void fillReplayContextMenu(ContextMenuManager menu)
    {
        UIReplayList list = this.getReplayList();

        if (list != null)
        {
            list.fillContextMenu(menu);
        }
    }

    private void handleAddReplay()
    {
        UIReplayList list = this.getReplayList();

        if (list == null)
        {
            return;
        }

        list.addReplay();
        this.refreshCards();
    }

    private void fillClipTypePalette(boolean camera)
    {
        UIClipsPanel panel = camera ? this.film.cameraEditor : this.film.actionEditor;

        if (panel == null || panel.clips == null)
        {
            return;
        }

        IFactory<Clip, ClipFactoryData> factory = panel.clips.getFactory();
        List<TimelineClipTypeGroups.ClipGroup> groups = camera
            ? TimelineClipTypeGroups.forCamera(factory)
            : TimelineClipTypeGroups.forAction(factory);

        this.animating = false;

        for (int g = 0; g < groups.size(); g++)
        {
            TimelineClipTypeGroups.ClipGroup group = groups.get(g);
            String key = (camera ? "cam:" : "act:") + group.label.get();
            boolean expanded = this.groupExpanded.getOrDefault(key, g == 0);
            float open = this.groupOpen.getOrDefault(key, expanded ? 1F : 0F);
            float target = expanded ? 1F : 0F;

            if (Math.abs(open - target) > 0.01F)
            {
                open += (target - open) * 0.3F;
                this.animating = true;
            }
            else
            {
                open = target;
            }

            this.groupExpanded.put(key, expanded);
            this.groupOpen.put(key, open);

            int bodyFull = group.types.size() * (ROW_H + 2);
            int bodyH = Math.max(0, Math.round(bodyFull * open));
            ClipTypeGroup groupUi = new ClipTypeGroup(group.label.get(), key, group.types, factory, camera, expanded, bodyH);

            groupUi.relative(this.grid).x(2).y(this.buildY).w(1F, -4).h(GROUP_H + bodyH);
            this.grid.add(groupUi);
            this.buildY += GROUP_H + bodyH + 6;
        }
    }

    private class ClipTypeGroup extends UIElement
    {
        private final String title;
        private final String expandKey;
        private final boolean expanded;
        private final UIElement body;

        private ClipTypeGroup(String title, String expandKey, List<Link> types,
            IFactory<Clip, ClipFactoryData> factory, boolean camera, boolean expanded, int bodyH)
        {
            this.title = title;
            this.expandKey = expandKey;
            this.expanded = expanded;
            this.markContainer();

            this.body = new UIElement();
            this.body.relative(this).x(4).y(GROUP_H).w(1F, -4).h(Math.max(1, bodyH));
            this.body.setVisible(bodyH > 0);

            int y = 0;

            for (Link type : types)
            {
                UIElement row = UIMinecutMediaPanel.this.createTypeRow(type, factory, camera);

                row.relative(this.body).x(0).y(y).w(1F).h(ROW_H);
                this.body.add(row);
                y += ROW_H + 2;
            }

            this.add(this.body);
        }

        @Override
        public void render(UIContext context)
        {
            boolean hover = context.mouseY >= this.area.y && context.mouseY < this.area.y + GROUP_H
                && context.mouseX >= this.area.x && context.mouseX <= this.area.ex();
            UIStyle style = UIStyle.active();

            context.batcher.box(this.area.x, this.area.y, this.area.ex(), this.area.y + GROUP_H,
                hover ? style.inner() : style.elevated());
            context.batcher.textShadow(this.expanded ? "▾" : "▸", this.area.x + 6, this.area.y + 6, MinecutTokens.ACCENT);
            context.batcher.textShadow(this.title, this.area.x + 18, this.area.y + 6, MinecutTokens.TEXT);

            if (this.body.isVisible() && this.body.area.h > 0)
            {
                context.batcher.clip(this.area.x, this.area.y + GROUP_H, this.area.w, this.body.area.h, context);
                this.body.render(context);
                context.batcher.unclip(context);
            }
        }

        @Override
        protected boolean subMouseClicked(UIContext context)
        {
            if (context.mouseButton == 0 && context.mouseY >= this.area.y && context.mouseY < this.area.y + GROUP_H
                && context.mouseX >= this.area.x && context.mouseX <= this.area.ex())
            {
                UIMinecutMediaPanel.this.groupExpanded.put(this.expandKey, !this.expanded);
                UIMinecutMediaPanel.this.refreshCards();

                return true;
            }

            if (this.body.isVisible())
            {
                return super.subMouseClicked(context);
            }

            return false;
        }
    }

    private UIElement createTypeRow(Link type, IFactory<Clip, ClipFactoryData> factory, boolean camera)
    {
        ClipFactoryData data = factory.getData(type);
        Icon icon = data != null ? data.icon : Icons.FILM;
        int accent = data != null ? data.color : MinecutTokens.ACCENT_RGB;
        String label = this.resolveTypeLabel(type);

        /* Drag-only: ignore click; placement starts after the mouse actually moves. */
        UIDraggable row = new UIDraggable((ctx) ->
            UIMinecutMediaPanel.this.ensurePaletteDrag(type, camera, icon, label, accent));

        row.threshold(4);
        row.cursors(GLFW.GLFW_ARROW_CURSOR, GLFW.GLFW_HAND_CURSOR);
        row.rendering((context) ->
        {
            boolean dragging = UIMinecutMediaPanel.this.paletteDragging
                && label.equals(UIMinecutMediaPanel.this.paletteDragLabel);
            UIStyle style = UIStyle.active();

            context.batcher.box(row.area.x, row.area.y, row.area.ex(), row.area.ey(),
                dragging ? (Colors.A25 | (MinecutTokens.ACCENT & Colors.RGB)) : style.panel());
            context.batcher.box(row.area.x, row.area.y, row.area.x + 3, row.area.ey(), Colors.A100 | accent);
            context.batcher.icon(icon, Colors.WHITE, row.area.x + 16, row.area.my(), 0.5F, 0.5F);
            context.batcher.textShadow(label, row.area.x + 28, row.area.y + 8,
                dragging ? MinecutTokens.ACCENT : MinecutTokens.TEXT);
        });
        row.dragEnd(() -> UIMinecutMediaPanel.this.finishPaletteDrag(camera));

        return row;
    }

    private void fillModelTrackPalette()
    {
        this.animating = false;

        this.addTrackGroup("Core", new String[] {
            ModelTrackIds.POSE, ModelTrackIds.TRANSFORM, ModelTrackIds.VISIBLE,
            ModelTrackIds.COLOR, ModelTrackIds.OPACITY, ModelTrackIds.LIGHTING
        }, new Icon[] {
            Icons.POSE, Icons.ALL_DIRECTIONS, Icons.VISIBLE, Icons.MATERIAL, Icons.FILTER, Icons.LIGHT
        }, true);

        this.addTrackGroup("More", new String[] {
            ModelTrackIds.ANCHOR, ModelTrackIds.LOOK_AT, ModelTrackIds.INVERSE_KINEMATICS,
            ModelTrackIds.ILLUSION, ModelTrackIds.GLOW, ModelTrackIds.TEXTURE, ModelTrackIds.RENDER_DEPTH
        }, new Icon[] {
            Icons.LIMB, Icons.VISIBLE, Icons.LIMB, Icons.FILTER, Icons.LIGHT, Icons.IMAGE, Icons.ARROW_UP
        }, false);
    }

    private void addTrackGroup(String title, String[] ids, Icon[] icons, boolean defaultExpanded)
    {
        List<String> availableIds = new ArrayList<>();
        List<Icon> availableIcons = new ArrayList<>();

        for (int i = 0; i < ids.length; i++)
        {
            if (this.canShowTrackInPalette(ids[i]))
            {
                availableIds.add(ids[i]);
                availableIcons.add(icons[i]);
            }
        }

        if (availableIds.isEmpty())
        {
            return;
        }

        String expandKey = "tracks:" + title;
        boolean expanded = this.groupExpanded.getOrDefault(expandKey, defaultExpanded);
        float open = this.groupOpen.getOrDefault(expandKey, expanded ? 1F : 0F);
        float target = expanded ? 1F : 0F;

        if (Math.abs(open - target) > 0.01F)
        {
            open += (target - open) * 0.3F;
            this.animating = true;
        }
        else
        {
            open = target;
        }

        this.groupExpanded.put(expandKey, expanded);
        this.groupOpen.put(expandKey, open);

        int bodyFull = availableIds.size() * (ROW_H + 2);
        int bodyH = Math.max(0, Math.round(bodyFull * open));
        TrackTypeGroup groupUi = new TrackTypeGroup(title, expandKey,
            availableIds.toArray(new String[0]), availableIcons.toArray(new Icon[0]), expanded, bodyH);

        groupUi.relative(this.grid).x(2).y(this.buildY).w(1F, -4).h(GROUP_H + bodyH);
        this.grid.add(groupUi);
        this.buildY += GROUP_H + bodyH + 6;
    }

    private boolean canShowTrackInPalette(String trackId)
    {
        if (this.film.replayEditor == null)
        {
            return false;
        }

        Replay replay = this.film.replayEditor.getReplay();

        return ModelTrackIds.canAddFromPalette(replay, trackId);
    }

    private class TrackTypeGroup extends UIElement
    {
        private final String title;
        private final String expandKey;
        private final boolean expanded;
        private final UIElement body;

        private TrackTypeGroup(String title, String expandKey, String[] ids, Icon[] icons, boolean expanded, int bodyH)
        {
            this.title = title;
            this.expandKey = expandKey;
            this.expanded = expanded;
            this.markContainer();

            this.body = new UIElement();
            this.body.relative(this).x(4).y(GROUP_H).w(1F, -4).h(Math.max(1, bodyH));
            this.body.setVisible(bodyH > 0);

            int y = 0;

            for (int i = 0; i < ids.length; i++)
            {
                int accent = UIReplaysEditor.getColor(ids[i]) & Colors.RGB;
                Icon icon = icons[i];
                Icon sheetIcon = UIReplaysEditor.getIcon(ids[i]);

                if (sheetIcon != null)
                {
                    icon = sheetIcon;
                }

                UIElement row = UIMinecutMediaPanel.this.createTrackRow(ids[i], accent, icon);

                row.relative(this.body).x(0).y(y).w(1F).h(ROW_H);
                this.body.add(row);
                y += ROW_H + 2;
            }

            this.add(this.body);
        }

        @Override
        public void render(UIContext context)
        {
            boolean hover = context.mouseY >= this.area.y && context.mouseY < this.area.y + GROUP_H
                && context.mouseX >= this.area.x && context.mouseX <= this.area.ex();
            UIStyle style = UIStyle.active();
            float open = UIMinecutMediaPanel.this.groupOpen.getOrDefault(this.expandKey, this.expanded ? 1F : 0F);
            /* Soft caret: ▸ → ▾ while the body height animates. */
            String caret = open > 0.5F ? "▾" : "▸";

            context.batcher.box(this.area.x, this.area.y, this.area.ex(), this.area.y + GROUP_H,
                hover ? style.inner() : style.elevated());
            context.batcher.textShadow(caret, this.area.x + 6, this.area.y + 6, MinecutTokens.ACCENT);
            context.batcher.textShadow(this.title, this.area.x + 20, this.area.y + 6, MinecutTokens.TEXT);

            if (this.body.isVisible() && this.body.area.h > 0)
            {
                context.batcher.clip(this.area.x, this.area.y + GROUP_H, this.area.w, this.body.area.h, context);
                this.body.render(context);
                context.batcher.unclip(context);
            }
        }

        @Override
        protected boolean subMouseClicked(UIContext context)
        {
            if (context.mouseButton == 0 && context.mouseY >= this.area.y && context.mouseY < this.area.y + GROUP_H
                && context.mouseX >= this.area.x && context.mouseX <= this.area.ex())
            {
                UIMinecutMediaPanel.this.groupExpanded.put(this.expandKey, !this.expanded);
                UIMinecutMediaPanel.this.refreshCards();

                return true;
            }

            if (this.body.isVisible())
            {
                return super.subMouseClicked(context);
            }

            return false;
        }
    }

    private UIElement createTrackRow(String trackId, int accent, Icon icon)
    {
        String label = this.prettyTrackLabel(trackId);

        UIDraggable row = new UIDraggable((ctx) ->
            UIMinecutMediaPanel.this.ensureTrackDrag(trackId, icon, label, accent))
        {
            @Override
            protected boolean subMouseClicked(UIContext context)
            {
                if (this.area.isInside(context) && context.mouseButton == 0)
                {
                    /* Ghost follows the cursor immediately (like Camera clips). */
                    UIMinecutMediaPanel.this.beginTrackDragChrome(trackId, icon, label, accent);
                }

                return super.subMouseClicked(context);
            }

            @Override
            protected boolean subMouseReleased(UIContext context)
            {
                boolean clickOnly = this.isDragging() && !this.isActivelyDragging();

                boolean handled = super.subMouseReleased(context);

                if (clickOnly && context.mouseButton == 0)
                {
                    UIMinecutMediaPanel.this.clickAddTrack(trackId);
                }

                return handled;
            }
        };

        row.threshold(4);
        row.cursors(GLFW.GLFW_ARROW_CURSOR, GLFW.GLFW_HAND_CURSOR);
        row.rendering((context) ->
        {
            boolean dragging = UIMinecutMediaPanel.this.paletteDragging
                && UIMinecutMediaPanel.this.trackPaletteDrag
                && trackId.equals(UIMinecutMediaPanel.this.trackPaletteType);
            UIStyle style = UIStyle.active();

            context.batcher.box(row.area.x, row.area.y, row.area.ex(), row.area.ey(),
                dragging ? (Colors.A25 | (MinecutTokens.ACCENT & Colors.RGB)) : style.panel());
            context.batcher.box(row.area.x, row.area.y, row.area.x + 3, row.area.ey(), Colors.A100 | accent);
            context.batcher.icon(icon, Colors.WHITE, row.area.x + 16, row.area.my(), 0.5F, 0.5F);
            context.batcher.textShadow(label, row.area.x + 28, row.area.y + 8,
                dragging ? MinecutTokens.ACCENT : MinecutTokens.TEXT);
        });
        row.dragEnd(() -> UIMinecutMediaPanel.this.finishTrackDrag());

        return row;
    }

    private String prettyTrackLabel(String id)
    {
        if (id == null)
        {
            return "";
        }

        String[] parts = id.split("_");
        StringBuilder sb = new StringBuilder();

        for (String part : parts)
        {
            if (part.isEmpty())
            {
                continue;
            }

            if (sb.length() > 0)
            {
                sb.append(' ');
            }

            sb.append(Character.toUpperCase(part.charAt(0)));
            sb.append(part.substring(1));
        }

        return sb.toString();
    }

    private void beginTrackDragChrome(String trackId, Icon icon, String label, int accent)
    {
        this.paletteDragging = true;
        this.trackPaletteDrag = true;
        this.trackPaletteType = trackId;
        this.paletteDragIcon = icon;
        this.paletteDragLabel = label;
        this.paletteDragAccent = accent;
        this.film.beginModelTrackPlacement(trackId);
    }

    private void ensureTrackDrag(String trackId, Icon icon, String label, int accent)
    {
        if (!this.paletteDragging || !this.trackPaletteDrag)
        {
            this.beginTrackDragChrome(trackId, icon, label, accent);
        }
    }

    private void clickAddTrack(String trackId)
    {
        this.film.cancelModelTrackPlacement();
        this.clearPaletteDragUiOnly();
        this.film.clickAddModelTrack(trackId);
    }

    private void finishTrackDrag()
    {
        this.film.finishModelTrackPlacement();
        this.clearPaletteDragUiOnly();
    }

    /** Clears local drag chrome without cancelling film-level placement (owned by UIFilmPanel). */
    public void clearPaletteDragUiOnly()
    {
        this.paletteDragging = false;
        this.trackPaletteDrag = false;
        this.trackPaletteType = null;
        this.paletteDragIcon = null;
        this.paletteDragLabel = null;
    }

    private String resolveTypeLabel(Link type)
    {
        String name = UIKeys.C_CLIP.get(type).get();

        if (name == null || name.isEmpty() || name.equals(type.toString()) || name.contains("bbs.ui.camera.clips."))
        {
            name = type.path;
        }

        return name;
    }

    private void ensurePaletteDrag(Link type, boolean camera, Icon icon, String label, int accent)
    {
        if (!this.paletteDragging)
        {
            this.paletteDragging = true;
            this.paletteDragIcon = icon;
            this.paletteDragLabel = label;
            this.paletteDragAccent = accent;
            this.beginPlaceClipType(type, camera);
        }
    }

    private void finishPaletteDrag(boolean camera)
    {
        UIClipsPanel panel = camera ? this.film.cameraEditor : this.film.actionEditor;
        UIContext context = this.film.getContext();

        if (panel != null && panel.clips != null && context != null && panel.clips.isClipPlacementActive())
        {
            if (!panel.clips.confirmClipPlacementAt(context))
            {
                panel.clips.cancelClipPlacement();
            }
        }

        this.clearPaletteDrag();
    }

    private void clearPaletteDrag()
    {
        /* UI chrome only. Model-track placement confirm/cancel is owned by UIFilmPanel
           so dock remounts cannot abort an in-progress drop. */
        this.clearPaletteDragUiOnly();
    }

    private void beginPlaceClipType(Link type, boolean camera)
    {
        this.film.focusMinecutDockTab(camera ? "minecutTimelineCamera" : "minecutTimelineAction");

        UIClipsPanel panel = camera ? this.film.cameraEditor : this.film.actionEditor;

        if (panel != null && panel.clips != null)
        {
            panel.clips.toolbarAddClipType(type);
        }
    }

    private void fillReplayCards(Film data)
    {
        List<Replay> replays = data.replays.getList();
        int cardW = this.scaledCardW();
        int cardH = this.scaledCardH();
        int available = Math.max(cardW + GAP, this.area.w - this.propsPixelW() - 16);
        int cols = this.listMode ? 1 : Math.max(1, available / (cardW + GAP));

        this.replayCardCount = replays.size();

        for (int i = 0; i < replays.size(); i++)
        {
            final int index = i;
            Replay replay = replays.get(i);
            int itemW = this.listMode ? Math.max(80, available - GAP) : cardW;
            int itemH = this.listMode ? LIST_H : cardH + 14;

            UIElement card = new UIElement()
            {
                @Override
                public void render(UIContext context)
                {
                    boolean selected = UIMinecutMediaPanel.this.isReplaySelected(index);
                    boolean hover = this.area.isInside(context);
                    UIStyle style = UIStyle.active();

                    if (UIMinecutMediaPanel.this.listMode)
                    {
                        context.batcher.box(this.area.x, this.area.y, this.area.ex(), this.area.ey(),
                            selected ? style.inner() : (hover ? style.elevated() : style.panel()));
                        context.batcher.box(this.area.x, this.area.y, this.area.x + 3, this.area.ey(),
                            selected ? MinecutTokens.ACCENT : style.borderSoft());

                        Form form = replay.form.get();

                        if (form != null)
                        {
                            context.batcher.clip(this.area.x + 6, this.area.y + 2, 28, 28, context);
                            FormUtilsClient.renderUICachedStatic(form, context, this.area.x + 6, this.area.y + 2, this.area.x + 34, this.area.y + 30);
                            context.batcher.unclip(context);
                        }

                        String label = UIMinecutMediaPanel.this.replayLabel(replay, index);

                        context.batcher.textShadow(label, this.area.x + 40, this.area.y + 12, selected ? MinecutTokens.ACCENT : MinecutTokens.TEXT);
                    }
                    else
                    {
                        style.drawFormCell(context.batcher, this.area.x, this.area.y, this.area.w, this.area.h - 14, selected, hover);

                        Form form = replay.form.get();

                        if (form != null)
                        {
                            context.batcher.clip(this.area.x + 2, this.area.y + 2, this.area.w - 4, this.area.h - 18, context);
                            FormUtilsClient.renderUICachedStatic(form, context, this.area.x + 2, this.area.y + 2, this.area.ex() - 2, this.area.ey() - 16);
                            context.batcher.unclip(context);
                        }

                        String label = UIMinecutMediaPanel.this.replayLabel(replay, index);

                        if (label.length() > 12)
                        {
                            label = label.substring(0, 11) + "…";
                        }

                        context.batcher.textShadow(label, this.area.x + 2, this.area.ey() - 12, selected ? MinecutTokens.ACCENT : MinecutTokens.TEXT_DIM);
                    }
                }

                @Override
                protected boolean subMouseClicked(UIContext context)
                {
                    if (this.area.isInside(context) && context.mouseButton == 0)
                    {
                        UIMinecutMediaPanel.this.onReplayCardClick(index);

                        return true;
                    }

                    return super.subMouseClicked(context);
                }
            };

            card.context((menu) ->
            {
                UIMinecutMediaPanel.this.selectReplayOnly(index);
                UIMinecutMediaPanel.this.fillReplayContextMenu(menu);
            });

            if (this.listMode)
            {
                card.relative(this.grid).x(GAP).y(GAP + i * (LIST_H + GAP)).w(1F, -GAP * 2).h(LIST_H);
            }
            else
            {
                int col = i % cols;
                int row = i / cols;

                card.relative(this.grid).x(GAP + col * (itemW + GAP)).y(GAP + row * (itemH + GAP)).w(itemW).h(itemH);
            }

            this.grid.add(card);
        }
    }

    private String replayLabel(Replay replay, int index)
    {
        String label = replay.label.get();

        if (label == null || label.isEmpty())
        {
            Form f = replay.form.get();
            label = f != null ? f.getDisplayName() : replay.getId();
        }

        if (label == null || label.isEmpty())
        {
            label = "Replay " + (index + 1);
        }

        return label;
    }

    private boolean isReplaySelected(int index)
    {
        Film data = this.film.getData();

        if (data == null || this.film.replayEditor == null)
        {
            return false;
        }

        Replay current = this.film.replayEditor.getReplay();

        return current != null && data.replays.getList().indexOf(current) == index;
    }

    private void onReplayCardClick(int index)
    {
        if (this.isReplaySelected(index))
        {
            /* Second click on the selected morph toggles the side props menu. */
            if (this.propsWantOpen && this.propsReplayIndex == index)
            {
                this.closePropsPanel();
            }
            else
            {
                this.openPropsPanel(index);
            }

            return;
        }

        /* First click only selects — stay on Replays, do not jump to Tracks. */
        this.closePropsPanel();
        this.selectReplayOnly(index);
    }

    private void selectReplayOnly(int index)
    {
        Film data = this.film.getData();

        if (data == null || this.film.replayEditor == null)
        {
            return;
        }

        List<Replay> list = data.replays.getList();

        if (index < 0 || index >= list.size())
        {
            return;
        }

        this.film.replayEditor.setReplay(list.get(index), false, true);
        this.film.syncAnchoredReplaysPanelSelection(list.get(index), true);
    }

    private void selectReplay(int index)
    {
        this.onReplayCardClick(index);
    }

    @Override
    public void render(UIContext context)
    {
        float target = this.propsWantOpen && this.mode == Mode.REPLAYS ? 1F : 0F;

        if (Math.abs(this.propsOpen - target) > 0.01F)
        {
            this.propsOpen += (target - this.propsOpen) * 0.28F;
            this.syncReplaySplitLayout();
            this.resize();
        }
        else if (this.propsOpen != target)
        {
            this.propsOpen = target;
            this.syncReplaySplitLayout();
            this.resize();
        }

        if (this.mode == Mode.REPLAYS)
        {
            Film data = this.film.getData();
            int size = data == null ? 0 : data.replays.getList().size();

            if (size != this.replayCardCount)
            {
                this.refreshCards();
            }
        }

        if (this.animating && this.mode != Mode.REPLAYS)
        {
            this.rebuildContent(false);
        }

        /* Props chrome under children — drawing after super.render() covered the form fields
           (buttons still hit-tested / showed tooltips, but looked blank). */
        int propsW = this.propsPixelW();

        if (propsW > 2 && this.replayPropsHost.area.w > 0)
        {
            context.batcher.box(this.replayPropsHost.area.x, this.replayPropsHost.area.y, this.replayPropsHost.area.ex(), this.replayPropsHost.area.ey(), UIStyle.active().elevated());
            context.batcher.box(this.replayPropsHost.area.x, this.replayPropsHost.area.y, this.replayPropsHost.area.x + 1, this.replayPropsHost.area.ey(), MinecutTokens.ACCENT);
        }

        super.render(context);

        if (this.mode != Mode.REPLAYS)
        {
            int hintY = this.area.y + 4;

            context.batcher.box(this.area.x + 8, hintY, this.area.ex() - 8, hintY + HINT_H - 4, UIStyle.active().inner());
            context.batcher.textShadow("Press and drag onto the timeline",
                this.area.x + 14, hintY + 3, MinecutTokens.TEXT_MUTED);
        }

        if (this.mode == Mode.ACTIONS && (this.film.replayEditor == null || this.film.replayEditor.getReplay() == null))
        {
            context.batcher.textShadow("Select a replay first", this.area.mx() - 60, this.area.my(), MinecutTokens.TEXT_DIM);
        }

        if (this.mode == Mode.TRACKS && (this.film.replayEditor == null || this.film.replayEditor.getReplay() == null))
        {
            context.batcher.textShadow("Select a replay first", this.area.mx() - 60, this.area.my(), MinecutTokens.TEXT_DIM);
        }

        /* Drag ghost is painted by UIFilmPanel after all dock/floating windows. */
    }

    public boolean isPaletteDragging()
    {
        return this.paletteDragging;
    }

    /**
     * Draw the clip-type drag chip above every docked/floating window.
     */
    public void renderPaletteDragGhost(UIContext context)
    {
        if (!this.paletteDragging || this.paletteDragIcon == null)
        {
            return;
        }

        int gx = context.mouseX + 12;
        int gy = context.mouseY + 8;

        context.batcher.box(gx - 2, gy - 2, gx + 96, gy + 18, Colors.A75 | (MinecutTokens.PANEL & Colors.RGB));
        context.batcher.box(gx - 2, gy - 2, gx + 1, gy + 18, Colors.A100 | this.paletteDragAccent);
        context.batcher.icon(this.paletteDragIcon, Colors.WHITE, gx + 10, gy + 8, 0.5F, 0.5F);
        context.batcher.textShadow(this.paletteDragLabel, gx + 20, gy + 4, MinecutTokens.TEXT);
    }

    @Override
    protected IUIElement childrenMouseScrolled(UIContext context)
    {
        /* Consume Ctrl+wheel for the whole Media window before grid / props scrolls
           (or the film panel's Ctrl+cursor jump) can steal it. */
        if (this.tryCardZoom(context))
        {
            return this;
        }

        return super.childrenMouseScrolled(context);
    }

    /**
     * Ctrl+Scroll (vertical or horizontal wheel) zooms morph cards while the cursor
     * is anywhere over this Replays panel.
     */
    public boolean tryCardZoom(UIContext context)
    {
        if (this.mode != Mode.REPLAYS || !this.area.isInside(context) || !Window.isCtrlPressed())
        {
            return false;
        }

        double wheel = context.mouseWheel != 0D ? context.mouseWheel : context.mouseWheelHorizontal;

        if (wheel == 0D)
        {
            return false;
        }

        float next = MathUtils.clamp(this.cardScale + (float) wheel * 0.12F, 0.55F, 1.7F);

        if (Math.abs(next - this.cardScale) > 0.001F)
        {
            this.cardScale = next;
            this.refreshCards();
        }

        return true;
    }

    @Override
    protected boolean subMouseClicked(UIContext context)
    {
        return super.subMouseClicked(context);
    }

    @Override
    public void resize()
    {
        super.resize();
        this.refreshCards();
    }
}
