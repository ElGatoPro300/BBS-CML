package mchorse.bbs_mod.ui.film.toolbar;

import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.graphics.window.Window;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.film.replays.UIReplaysEditor;
import mchorse.bbs_mod.ui.framework.UIBaseMenu;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.utils.EventPropagation;
import mchorse.bbs_mod.ui.framework.elements.utils.FontRenderer;
import mchorse.bbs_mod.ui.framework.elements.utils.UIRenderable;
import mchorse.bbs_mod.ui.utils.Area;
import mchorse.bbs_mod.ui.utils.Scroll;
import mchorse.bbs_mod.ui.utils.ScrollDirection;
import mchorse.bbs_mod.ui.utils.icons.Icon;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import mchorse.bbs_mod.utils.colors.Colors;

import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * The toolbar bar itself. Displays a row or column of section buttons
 * anchored to an edge of a timeline panel. Each section shows an icon and,
 * when space allows, its label; otherwise the label collapses into a hover
 * tooltip. Clicking a section opens its popup ({@link ToolbarMenu}) on the
 * global UI overlay.
 *
 * <p>Also coordinates open/close state so hovering across sections while one
 * is open switches to the hovered section without needing an extra click
 * (Blender-like behaviour).</p>
 */
public class TimelineToolbar extends UIElement
{
    private final List<ToolbarSection> sections = new ArrayList<>();

    /**
     * Cached areas of each section button after every render pass. Kept
     * separate from the child element list so opening a popup does not need
     * to walk the child tree.
     */
    private final List<Area> sectionAreas = new ArrayList<>();

    /**
     * Whether each section currently shows its label inline ({@code true}) or
     * is collapsed to icon-only ({@code false}).
     */
    private boolean[] sectionShowLabel = new boolean[0];

    /**
     * Index of the currently open section, or {@code -1} if no popup is open.
     */
    private int openIndex = -1;

    /**
     * Currently open root popup, or {@code null}.
     */
    private ToolbarMenu openMenu;

    /**
     * Invoked when the toolbar hierarchy changes or a section is opened,
     * so an active timeline interaction mode can be cancelled.
     */
    private Runnable interactionCancelListener;

    private TimelineToolbarDock dock = TimelineToolbarDock.BOTTOM;

    private UIElement hostPanel;

    private String panelId;

    private Runnable dockChangeListener;

    private final Area dragHandleArea = new Area();

    private final Area dragSeparatorArea = new Area();

    /**
     * Clip and scroll bounds for section buttons. Excludes the drag separator
     * and handle, which always stay visible at the end of the toolbar.
     */
    private final Area sectionsViewportArea = new Area();

    private final Scroll sectionsScroll;

    private UIRenderable dockOverlay;

    private boolean dockDragging;

    private TimelineToolbarDock hoverDock;

    private int dragMouseX;

    private int dragMouseY;

    private boolean sectionsPointerDown;

    private boolean sectionsScrolling;

    private int sectionsPointerDownX;

    private int sectionsPointerDownY;

    private double sectionsScrollDragStart;

    private int sectionsPendingSectionIndex = -1;

    /* Constructor */

    public TimelineToolbar()
    {
        super();

        this.h(TimelineToolbarSettings.TOOLBAR_HEIGHT);
        this.sectionsScroll = new Scroll(this.sectionsViewportArea, 0, ScrollDirection.HORIZONTAL);
        this.sectionsScroll.noScrollbar();
        /* Block mouse events inside the bar (clicks should not pass through to
         * the timeline) but let keyboard events propagate so Escape can still
         * close the editor when the cursor is over the toolbar (unless a dock
         * drag is in progress — see {@link #subKeyPressed}). */
        this.mouseEventPropagataion(EventPropagation.BLOCK_INSIDE);
    }

    /* API */

    public TimelineToolbarDock getDock()
    {
        return this.dock;
    }

    public void setDock(TimelineToolbarDock dock)
    {
        if (dock == null)
        {
            dock = TimelineToolbarDock.BOTTOM;
        }

        if (this.dock != dock)
        {
            this.sectionsScroll.setScroll(0D);
            this.cancelSectionsPointer();
        }

        this.dock = dock;
    }

    public void configureDockHost(UIElement host, String panelId, Runnable dockChangeListener)
    {
        this.hostPanel = host;
        this.panelId = panelId;
        this.dockChangeListener = dockChangeListener;

        if (this.dockOverlay == null && host != null)
        {
            this.dockOverlay = new UIRenderable(this::renderDockDragOverlay);
            host.add(this.dockOverlay);
        }

        this.ensureDockOverlayOnTop();
    }

    /**
     * Keeps the drag preview overlay above timeline content. Required because
     * hosts such as {@link UIReplaysEditor}
     * add their keyframe editor after the overlay is first registered.
     */
    public void ensureDockOverlayOnTop()
    {
        if (this.dockOverlay == null || this.hostPanel == null)
        {
            return;
        }

        this.hostPanel.remove(this.dockOverlay);
        this.hostPanel.add(this.dockOverlay);
    }

    public boolean isDockDragging()
    {
        return this.dockDragging;
    }

    public void cancelDockDrag()
    {
        this.dockDragging = false;
        this.hoverDock = null;
    }

    public boolean isDockDragEnabled()
    {
        return BBSSettings.editorLayoutSettings == null
            || !BBSSettings.editorLayoutSettings.isLayoutLocked();
    }

    /**
     * Cancels an in-progress dock drag when Escape is pressed. Used from
     * {@link UIBaseMenu} so the editor is not
     * closed while repositioning a toolbar.
     *
     * @return {@code true} when a dock drag was active and was cancelled
     */
    public static boolean cancelDockDragIfEscape(UIContext context)
    {
        if (!context.isPressed(GLFW.GLFW_KEY_ESCAPE))
        {
            return false;
        }

        for (TimelineToolbar toolbar : context.menu.getRoot().getChildren(TimelineToolbar.class, new ArrayList<>(), true))
        {
            if (toolbar.isDockDragging())
            {
                toolbar.cancelDockDrag();

                return true;
            }
        }

        return false;
    }

    /**
     * Cancels every in-progress toolbar dock drag under {@code root}
     * (e.g. when the film layout becomes locked).
     */
    public static void cancelAllDockDrags(UIElement root)
    {
        if (root == null)
        {
            return;
        }

        for (TimelineToolbar toolbar : root.getChildren(TimelineToolbar.class, new ArrayList<>(), true))
        {
            if (toolbar.isDockDragging())
            {
                toolbar.cancelDockDrag();
            }
        }
    }

    private void cancelSectionsPointer()
    {
        this.sectionsPointerDown = false;
        this.sectionsScrolling = false;
        this.sectionsPendingSectionIndex = -1;
    }

    private boolean canDragScrollSections()
    {
        return this.sectionsScroll.hasScrollbar();
    }

    private boolean isSectionsDragScrollTarget(int mouseX, int mouseY)
    {
        return this.sectionsViewportArea.isInside(mouseX, mouseY)
            && !this.dragHandleArea.isInside(mouseX, mouseY)
            && !this.dragSeparatorArea.isInside(mouseX, mouseY);
    }

    private void startSectionsPointer(UIContext context)
    {
        this.sectionsPointerDown = true;
        this.sectionsScrolling = false;
        this.sectionsPointerDownX = context.mouseX;
        this.sectionsPointerDownY = context.mouseY;
        this.sectionsScrollDragStart = this.sectionsScroll.getScroll();
        this.sectionsPendingSectionIndex = this.getSectionIndexAt(context.mouseX, context.mouseY);
    }

    private void updateSectionsScrollDrag(UIContext context)
    {
        if (!this.sectionsPointerDown)
        {
            return;
        }

        if (!Window.isMouseButtonPressed(GLFW.GLFW_MOUSE_BUTTON_LEFT))
        {
            this.finishSectionsPointer(context);

            return;
        }

        int axisDelta = this.dock.isHorizontal()
            ? context.mouseX - this.sectionsPointerDownX
            : context.mouseY - this.sectionsPointerDownY;

        if (!this.sectionsScrolling)
        {
            if (Math.abs(axisDelta) <= TimelineToolbarSettings.SECTIONS_SCROLL_DRAG_THRESHOLD)
            {
                return;
            }

            this.sectionsScrolling = true;
        }

        this.sectionsScroll.setScroll(this.sectionsScrollDragStart - axisDelta);
        context.requestCursor(GLFW.GLFW_HAND_CURSOR);
    }

    private void finishSectionsPointer(UIContext context)
    {
        if (!this.sectionsPointerDown)
        {
            return;
        }

        if (!this.sectionsScrolling && this.sectionsPendingSectionIndex >= 0)
        {
            int index = this.sectionsPendingSectionIndex;

            if (this.openMenu != null && this.openIndex == index)
            {
                this.closeOpenMenu();
            }
            else
            {
                this.openSection(context, index);
            }

            context.setTimelineToolbarConsumePointer(true);
        }

        this.cancelSectionsPointer();
    }

    public TimelineToolbar setSections(List<ToolbarSection> newSections)
    {
        this.notifyInteractionCancel();
        this.closeOpenMenu();
        this.sections.clear();
        this.sections.addAll(newSections);
        this.sectionAreas.clear();
        this.sectionShowLabel = new boolean[newSections.size()];

        for (int i = 0; i < newSections.size(); i++)
        {
            this.sectionAreas.add(new Area());
        }

        this.sectionsScroll.setScroll(0D);

        this.cancelSectionsPointer();

        return this;
    }

    public List<ToolbarSection> getSections()
    {
        return this.sections;
    }

    public boolean isMenuOpen()
    {
        return this.openMenu != null;
    }

    public int getOpenIndex()
    {
        return this.openIndex;
    }

    /**
     * Returns the section index under the given screen coordinates, or
     * {@code -1} if the point is not over a section button.
     */
    public int getSectionIndexAt(int mouseX, int mouseY)
    {
        if (!this.sectionsViewportArea.isInside(mouseX, mouseY))
        {
            return -1;
        }

        for (int i = 0; i < this.sectionAreas.size(); i++)
        {
            Area section = this.sectionAreas.get(i);

            if (section.w <= 0 || section.h <= 0)
            {
                continue;
            }

            if (section.isInside(mouseX, mouseY))
            {
                return i;
            }
        }

        return -1;
    }

    public boolean isToolbarSectionAt(int mouseX, int mouseY)
    {
        return this.getSectionIndexAt(mouseX, mouseY) >= 0;
    }

    public void notifyChainClosed()
    {
        this.openMenu = null;
        this.openIndex = -1;
    }

    /**
     * Whether {@code menu} is still the root popup tracked by this toolbar.
     * Used when deferred closes run so replacing a menu via hover-switch does
     * not clear the newly opened section.
     */
    public boolean isActiveRootMenu(ToolbarMenu menu)
    {
        return this.openMenu == menu;
    }

    public void closeOpenMenu()
    {
        if (this.openMenu != null)
        {
            this.openMenu.closeChain();
        }
    }

    public void setInteractionCancelListener(Runnable listener)
    {
        this.interactionCancelListener = listener;
    }

    private void notifyInteractionCancel()
    {
        if (this.interactionCancelListener != null)
        {
            this.interactionCancelListener.run();
        }
    }

    /* Layout */

    private void layoutSections(FontRenderer font)
    {
        if (this.dock.isHorizontal())
        {
            this.layoutSectionsHorizontal(font);
        }
        else
        {
            this.layoutSectionsVertical(font);
        }

        this.layoutDragHandle();
    }

    private void layoutSectionsHorizontal(FontRenderer font)
    {
        int size = this.sections.size();

        if (this.sectionAreas.size() != size)
        {
            this.sectionAreas.clear();

            for (int i = 0; i < size; i++)
            {
                this.sectionAreas.add(new Area());
            }
        }

        if (this.sectionShowLabel.length != size)
        {
            this.sectionShowLabel = new boolean[size];
        }

        for (int i = 0; i < size; i++)
        {
            this.sectionShowLabel[i] = true;
        }

        int reserved = TimelineToolbarSettings.getDragHandleReserved();
        int padding = TimelineToolbarSettings.TOOLBAR_PADDING;
        int available = this.area.w - padding * 2 - reserved;
        int total = this.computeSectionsWidth(font, this.sectionShowLabel);

        for (int i = size - 1; i >= 0 && total > available; i--)
        {
            if (this.sectionShowLabel[i])
            {
                this.sectionShowLabel[i] = false;
                total = this.computeSectionsWidth(font, this.sectionShowLabel);
            }
        }

        this.sectionsViewportArea.set(this.area.x + padding, this.area.y, available, this.area.h);

        this.sectionsScroll.direction = ScrollDirection.HORIZONTAL;
        this.sectionsScroll.scrollSize = total;
        this.sectionsScroll.clamp();

        int scroll = (int) Math.round(this.sectionsScroll.getScroll());
        int x = this.sectionsViewportArea.x - scroll;
        int y = this.sectionsViewportArea.y;
        int h = this.sectionsViewportArea.h;

        for (int i = 0; i < size; i++)
        {
            int w = this.getSectionWidth(font, i, this.sectionShowLabel[i]);
            Area a = this.sectionAreas.get(i);

            a.setPos(x, y);
            a.setSize(w, h);

            x += w;

            if (i < size - 1)
            {
                x += TimelineToolbarSettings.TOOLBAR_SECTION_SPACING;
            }
        }
    }

    private void layoutSectionsVertical(FontRenderer font)
    {
        int size = this.sections.size();

        if (this.sectionAreas.size() != size)
        {
            this.sectionAreas.clear();

            for (int i = 0; i < size; i++)
            {
                this.sectionAreas.add(new Area());
            }
        }

        if (this.sectionShowLabel.length != size)
        {
            this.sectionShowLabel = new boolean[size];
        }

        for (int i = 0; i < size; i++)
        {
            this.sectionShowLabel[i] = false;
        }

        int padding = TimelineToolbarSettings.TOOLBAR_PADDING;
        int reserved = TimelineToolbarSettings.getDragHandleReserved();
        int w = TimelineToolbarSettings.SECTION_ICON_SIZE;
        int x = this.area.x + (this.area.w - w) / 2;
        int viewportY = this.area.y + padding;
        int viewportH = this.area.h - padding * 2 - reserved;
        int total = this.computeSectionsHeight(size);

        this.sectionsViewportArea.set(x, viewportY, w, viewportH);

        this.sectionsScroll.direction = ScrollDirection.VERTICAL;
        this.sectionsScroll.scrollSize = total;
        this.sectionsScroll.clamp();

        int scroll = (int) Math.round(this.sectionsScroll.getScroll());
        int y = this.sectionsViewportArea.y - scroll;

        for (int i = 0; i < size; i++)
        {
            int h = TimelineToolbarSettings.SECTION_ICON_SIZE;
            Area a = this.sectionAreas.get(i);

            a.setPos(x, y);
            a.setSize(w, h);

            y += h;

            if (i < size - 1)
            {
                y += TimelineToolbarSettings.TOOLBAR_SECTION_SPACING;
            }
        }
    }

    private void layoutDragHandle()
    {
        int handle = TimelineToolbarSettings.DRAG_HANDLE_SIZE;
        int gap = TimelineToolbarSettings.DRAG_HANDLE_SEPARATOR_GAP;
        int sep = TimelineToolbarSettings.DRAG_HANDLE_SEPARATOR_SIZE;
        int padding = TimelineToolbarSettings.TOOLBAR_PADDING;

        if (this.dock.isHorizontal())
        {
            int handleX = this.area.ex() - padding - handle;
            int handleY = this.area.y + (this.area.h - handle) / 2;
            int sepX = handleX - gap - sep;

            this.dragHandleArea.set(handleX, handleY, handle, handle);
            this.dragSeparatorArea.set(sepX, this.area.y + 3, sep, this.area.h - 6);
        }
        else
        {
            int handleX = this.area.x + (this.area.w - handle) / 2;
            int handleY = this.area.ey() - padding - handle;
            int sepY = handleY - gap - sep;

            this.dragHandleArea.set(handleX, handleY, handle, handle);
            this.dragSeparatorArea.set(this.area.x + 3, sepY, this.area.w - 6, sep);
        }
    }

    private int computeSectionsWidth(FontRenderer font, boolean[] showLabel)
    {
        int size = this.sections.size();
        int total = 0;

        for (int i = 0; i < size; i++)
        {
            total += this.getSectionWidth(font, i, showLabel[i]);

            if (i < size - 1)
            {
                total += TimelineToolbarSettings.TOOLBAR_SECTION_SPACING;
            }
        }

        return total;
    }

    private int computeSectionsHeight(int size)
    {
        if (size <= 0)
        {
            return 0;
        }

        return size * TimelineToolbarSettings.SECTION_ICON_SIZE
            + (size - 1) * TimelineToolbarSettings.TOOLBAR_SECTION_SPACING;
    }

    private void scrollSectionIntoView(FontRenderer font, int index)
    {
        if (index < 0 || index >= this.sections.size() || !this.sectionsScroll.hasScrollbar())
        {
            return;
        }

        if (this.dock.isHorizontal())
        {
            int contentX = 0;

            for (int i = 0; i < index; i++)
            {
                contentX += this.getSectionWidth(font, i, this.sectionShowLabel[i]);

                if (i < this.sections.size() - 1)
                {
                    contentX += TimelineToolbarSettings.TOOLBAR_SECTION_SPACING;
                }
            }

            int w = this.getSectionWidth(font, index, this.sectionShowLabel[index]);

            this.sectionsScroll.scrollIntoView(contentX, w, 0);
        }
        else
        {
            int contentY = index * (TimelineToolbarSettings.SECTION_ICON_SIZE
                + TimelineToolbarSettings.TOOLBAR_SECTION_SPACING);
            int h = TimelineToolbarSettings.SECTION_ICON_SIZE;

            this.sectionsScroll.scrollIntoView(contentY, h, 0);
        }

        this.sectionsScroll.updateTarget();
    }

    private int getSectionWidth(FontRenderer font, int index, boolean showLabel)
    {
        if (!showLabel)
        {
            return TimelineToolbarSettings.SECTION_ICON_SIZE;
        }

        String text = this.sections.get(index).label.get();

        return TimelineToolbarSettings.SECTION_ICON_SIZE
            + TimelineToolbarSettings.SECTION_LABEL_PADDING
            + font.getWidth(text)
            + TimelineToolbarSettings.SECTION_LABEL_TRAILING_PADDING;
    }

    /* Dock drag */

    private void startDockDrag(UIContext context)
    {
        if (!this.isDockDragEnabled())
        {
            return;
        }

        this.cancelSectionsPointer();
        this.closeOpenMenu();
        this.ensureDockOverlayOnTop();
        this.dockDragging = true;
        this.dragMouseX = context.mouseX;
        this.dragMouseY = context.mouseY;
        this.hoverDock = this.computeNearestDropDock(context.mouseX, context.mouseY);
    }

    private void updateDockDrag(UIContext context)
    {
        if (!this.dockDragging)
        {
            return;
        }

        if (!this.isDockDragEnabled())
        {
            this.cancelDockDrag();

            return;
        }

        this.hoverDock = this.computeNearestDropDock(context.mouseX, context.mouseY);
        context.requestCursor(GLFW.GLFW_HAND_CURSOR);
    }

    private void finishDockDrag(UIContext context)
    {
        if (!this.dockDragging)
        {
            return;
        }

        if (!this.isDockDragEnabled())
        {
            this.cancelDockDrag();

            return;
        }

        TimelineToolbarDock target = this.computeNearestDropDock(context.mouseX, context.mouseY);

        this.dockDragging = false;
        this.hoverDock = null;

        if (target != null && target != this.dock)
        {
            this.applyDock(target);
        }
    }

    private void applyDock(TimelineToolbarDock dock)
    {
        this.setDock(dock);

        if (this.panelId != null)
        {
            TimelineToolbarDockSync.setDock(this.panelId, dock);
        }

        if (this.dockChangeListener != null)
        {
            this.dockChangeListener.run();
        }

        TimelineToolbarDockSync.refreshLinkedToolbars(this.hostPanel, this.panelId);
    }

    private TimelineToolbarDock computeNearestDropDock(int mouseX, int mouseY)
    {
        if (this.hostPanel == null)
        {
            return this.dock;
        }

        Area host = this.hostPanel.area;
        List<TimelineToolbarDock> inside = new ArrayList<>();

        for (TimelineToolbarDock candidate : TimelineToolbarDock.values())
        {
            if (this.getDropZoneRect(host, candidate).isInside(mouseX, mouseY))
            {
                inside.add(candidate);
            }
        }

        if (inside.contains(this.dock))
        {
            return this.dock;
        }

        if (inside.size() == 1)
        {
            return inside.get(0);
        }

        if (!inside.isEmpty())
        {
            return this.pickNearestDock(mouseX, mouseY, inside);
        }

        return this.pickNearestDock(mouseX, mouseY, Arrays.asList(TimelineToolbarDock.values()));
    }

    private TimelineToolbarDock pickNearestDock(int mouseX, int mouseY, List<TimelineToolbarDock> candidates)
    {
        if (this.hostPanel == null || candidates.isEmpty())
        {
            return this.dock;
        }

        Area host = this.hostPanel.area;
        TimelineToolbarDock nearest = candidates.get(0);
        double nearestDist = Double.MAX_VALUE;

        for (TimelineToolbarDock candidate : candidates)
        {
            Area zone = this.getDropZoneRect(host, candidate);
            double dist = this.distanceToRect(mouseX, mouseY, zone);

            if (dist < nearestDist)
            {
                nearestDist = dist;
                nearest = candidate;
            }
        }

        return nearest;
    }

    private Area getDropZoneRect(Area host, TimelineToolbarDock dock)
    {
        int thickness = TimelineToolbarSettings.getThickness(dock);
        Area zone = new Area();

        switch (dock)
        {
            case TOP:
                zone.set(host.x, host.y, host.w, thickness);
                break;
            case BOTTOM:
                zone.set(host.x, host.ey() - thickness, host.w, thickness);
                break;
            case LEFT:
                zone.set(host.x, host.y, thickness, host.h);
                break;
            case RIGHT:
                zone.set(host.ex() - thickness, host.y, thickness, host.h);
                break;
        }

        return zone;
    }

    private double distanceToRect(int x, int y, Area rect)
    {
        int dx = 0;

        if (x < rect.x)
        {
            dx = rect.x - x;
        }
        else if (x > rect.ex())
        {
            dx = x - rect.ex();
        }

        int dy = 0;

        if (y < rect.y)
        {
            dy = rect.y - y;
        }
        else if (y > rect.ey())
        {
            dy = y - rect.ey();
        }

        return Math.sqrt((double) dx * dx + (double) dy * dy);
    }

    private void renderDockDragOverlay(UIContext context)
    {
        if (!this.dockDragging || this.hostPanel == null)
        {
            return;
        }

        Area host = this.hostPanel.area;
        int border = BBSSettings.primaryColor(Colors.A50);
        int fill = BBSSettings.primaryColor(Colors.A25);
        int highlightFill = BBSSettings.primaryColor(Colors.A75);
        int t = TimelineToolbarSettings.DOCK_PREVIEW_BORDER;

        for (TimelineToolbarDock candidate : TimelineToolbarDock.values())
        {
            Area zone = this.getDropZoneRect(host, candidate);
            boolean highlight = candidate == this.hoverDock;
            boolean current = candidate == this.dock;
            int zoneFill = highlight ? highlightFill : fill;
            int zoneBorder = highlight ? BBSSettings.primaryColor(Colors.A100) : border;

            if (current && !highlight)
            {
                zoneFill = BBSSettings.primaryColor(Colors.A25);
            }

            context.batcher.box(zone.x, zone.y, zone.ex(), zone.ey(), zoneFill);
            context.batcher.box(zone.x, zone.y, zone.ex(), zone.y + t, zoneBorder);
            context.batcher.box(zone.x, zone.ey() - t, zone.ex(), zone.ey(), zoneBorder);
            context.batcher.box(zone.x, zone.y, zone.x + t, zone.ey(), zoneBorder);
            context.batcher.box(zone.ex() - t, zone.y, zone.ex(), zone.ey(), zoneBorder);
        }
    }

    /* Rendering */

    @Override
    public void render(UIContext context)
    {
        if (this.dockDragging)
        {
            if (this.hostPanel == null || !this.hostPanel.isVisible() || !this.isVisible())
            {
                this.cancelDockDrag();
            }
            else if (!Window.isMouseButtonPressed(GLFW.GLFW_MOUSE_BUTTON_LEFT))
            {
                this.finishDockDrag(context);
            }
        }

        FontRenderer font = context.batcher.getFont();

        this.updateSectionsScrollDrag(context);
        this.sectionsScroll.drag(context.mouseX, context.mouseY);
        this.layoutSections(font);

        this.renderBar(context);
        this.renderDragSeparator(context);
        this.renderDragHandle(context);

        context.batcher.clip(this.sectionsViewportArea, context);

        int hovered = this.getSectionIndexAt(context.mouseX, context.mouseY);
        boolean suppressSectionHover = context.isPointerOverOverlayPanel(context.mouseX, context.mouseY)
            || (this.openMenu != null && this.isPointerOverOpenMenu(context));

        this.renderSections(context, font, hovered, suppressSectionHover);

        context.batcher.unclip(context);

        this.sectionsScroll.renderScrollbar(context.batcher);

        this.updateHoverSwitch(context);
        this.updateDockDrag(context);

        super.render(context);

        /* Queue after super.render(): UIElement.render() calls resetTooltip()
         * when the mouse is inside this bar (BLOCK_INSIDE), which would wipe a
         * card queued earlier in the same pass. Only show the hover card when
         * the section label is collapsed (icon-only mode). */
        if (hovered >= 0 && !this.sectionShowLabel[hovered] && !suppressSectionHover && !this.sectionsScrolling)
        {
            ToolbarSection section = this.sections.get(hovered);
            String text = section.label.get();

            TimelineToolbarTooltips.drawForeground(context, text, context.mouseX, context.mouseY,
                Colors.WHITE, Colors.A75, this.dock, this.isMenuOpen());
        }
        else if (this.dragHandleArea.isInside(context.mouseX, context.mouseY) && !this.dockDragging)
        {
            String tip = this.isDockDragEnabled()
                ? UIKeys.TIMELINE_TOOLBAR_DRAG.get()
                : UIKeys.TIMELINE_TOOLBAR_DRAG_LOCKED.get();

            TimelineToolbarTooltips.drawForeground(context, tip,
                context.mouseX, context.mouseY, Colors.WHITE, Colors.A75, this.dock, this.isMenuOpen());
        }
    }

    private void renderBar(UIContext context)
    {
        context.batcher.box(this.area.x, this.area.y, this.area.ex(), this.area.ey(),
            TimelineToolbarSettings.TOOLBAR_BACKGROUND);

        switch (this.dock)
        {
            case TOP:
                context.batcher.box(this.area.x, this.area.ey() - 1, this.area.ex(), this.area.ey(),
                    TimelineToolbarSettings.TOOLBAR_BORDER);
                break;
            case BOTTOM:
                context.batcher.box(this.area.x, this.area.y, this.area.ex(), this.area.y + 1,
                    TimelineToolbarSettings.TOOLBAR_BORDER);
                break;
            case LEFT:
                context.batcher.box(this.area.ex() - 1, this.area.y, this.area.ex(), this.area.ey(),
                    TimelineToolbarSettings.TOOLBAR_BORDER);
                break;
            case RIGHT:
                context.batcher.box(this.area.x, this.area.y, this.area.x + 1, this.area.ey(),
                    TimelineToolbarSettings.TOOLBAR_BORDER);
                break;
        }
    }

    private void renderDragSeparator(UIContext context)
    {
        if (this.dragSeparatorArea.w <= 0 || this.dragSeparatorArea.h <= 0)
        {
            return;
        }

        Area a = this.dragSeparatorArea;

        context.batcher.box(a.x, a.y, a.ex(), a.ey(), TimelineToolbarSettings.TOOLBAR_BORDER);
    }

    private void renderDragHandle(UIContext context)
    {
        if (this.dragHandleArea.w <= 0)
        {
            return;
        }

        Area a = this.dragHandleArea;
        boolean enabled = this.isDockDragEnabled();
        boolean hover = enabled && (a.isInside(context.mouseX, context.mouseY) || this.dockDragging);

        if (hover)
        {
            context.batcher.box(a.x, a.y, a.ex(), a.ey(), TimelineToolbarSettings.SECTION_HOVER_COLOR);
        }

        context.batcher.icon(Icons.ALL_DIRECTIONS, enabled ? Colors.WHITE : Colors.GRAY, a.mx(), a.my(), 0.5F, 0.5F);
    }

    private void renderSections(UIContext context, FontRenderer font, int hovered, boolean suppressHover)
    {
        for (int i = 0; i < this.sections.size(); i++)
        {
            Area a = this.sectionAreas.get(i);

            if (a.w <= 0)
            {
                continue;
            }

            ToolbarSection section = this.sections.get(i);
            boolean isOpen = this.openIndex == i;
            boolean isHover = hovered == i && !suppressHover;
            boolean showLabel = this.sectionShowLabel[i];

            this.renderSection(context, font, section, a, isHover, isOpen, showLabel);
        }
    }

    private void renderSection(UIContext context, FontRenderer font, ToolbarSection section, Area a,
        boolean hover, boolean isOpen, boolean showLabel)
    {
        if (isOpen)
        {
            context.batcher.box(a.x, a.y, a.ex(), a.ey(), TimelineToolbarSettings.SECTION_OPEN_COLOR);
        }
        else if (hover)
        {
            context.batcher.box(a.x, a.y, a.ex(), a.ey(), TimelineToolbarSettings.SECTION_HOVER_COLOR);
        }

        int iconSize = TimelineToolbarSettings.SECTION_ICON_SIZE;
        int iconY = a.y + (a.h - iconSize) / 2;
        Icon icon = section.icon;

        if (icon != null)
        {
            float iconX = showLabel ? a.x + iconSize / 2F : a.mx();
            float iconYCenter = iconY + iconSize / 2F;

            context.batcher.icon(icon, Colors.WHITE, iconX, iconYCenter, 0.5F, 0.5F);
        }

        if (showLabel)
        {
            String text = section.label.get();
            int labelX = a.x + iconSize + TimelineToolbarSettings.SECTION_LABEL_PADDING;
            int labelY = a.y + (a.h - font.getHeight()) / 2 + 1;
            int labelMaxX = a.ex() - TimelineToolbarSettings.SECTION_LABEL_TRAILING_PADDING;

            if (labelX < labelMaxX)
            {
                context.batcher.text(text, labelX, labelY, Colors.WHITE, false);
            }
        }
    }

    private boolean isPointerOverOpenMenu(UIContext context)
    {
        if (this.openMenu == null)
        {
            return false;
        }

        return this.openMenu.isChainAt(context.mouseX, context.mouseY);
    }

    /**
     * While a popup chain is open, section switching on hover is only allowed
     * when the cursor is on the toolbar bar itself — not when it moves over an
     * open menu/submenu (including overlap in small windows).
     */
    private boolean shouldAllowSectionHoverSwitch(UIContext context)
    {
        if (this.openMenu == null)
        {
            return false;
        }

        if (this.isPointerOverOpenMenu(context))
        {
            return false;
        }

        return this.area.isInside(context.mouseX, context.mouseY);
    }

    private void updateHoverSwitch(UIContext context)
    {
        if (this.sectionsPointerDown || this.sectionsScrolling)
        {
            return;
        }

        if (!this.shouldAllowSectionHoverSwitch(context))
        {
            return;
        }

        int hovered = this.getSectionIndexAt(context.mouseX, context.mouseY);

        if (hovered >= 0 && hovered != this.openIndex)
        {
            this.openSection(context, hovered);
        }
    }

    /* Open logic */

    private void openSection(UIContext context, int index)
    {
        this.notifyInteractionCancel();

        FontRenderer font = context.batcher.getFont();

        this.layoutSections(font);
        this.scrollSectionIntoView(font, index);
        this.layoutSections(font);

        if (this.openMenu != null)
        {
            this.openMenu.closeChain();
        }
        else
        {
            this.purgeOrphanMenus(context);
        }

        ToolbarSection section = this.sections.get(index);

        if (section.items.isEmpty())
        {
            return;
        }

        ToolbarMenu menu = new ToolbarMenu(this, null, section.items);
        context.menu.overlay.add(menu);
        menu.openFromToolbar(context, this.sectionAreas.get(index), this.dock);

        this.openIndex = index;
        this.openMenu = menu;
    }

    /**
     * Removes stray popup elements that lost sync with {@link #openMenu}
     * (e.g. after a pinned submenu click before hover-only behaviour).
     */
    private void purgeOrphanMenus(UIContext context)
    {
        for (ToolbarMenu menu : context.menu.overlay.getChildren(ToolbarMenu.class, new ArrayList<>(), true))
        {
            if (menu.toolbar == this)
            {
                menu.removeFromParent();
            }
        }

        this.notifyChainClosed();
    }

    /* Input */

    @Override
    protected boolean subMouseClicked(UIContext context)
    {
        if (!this.area.isInside(context))
        {
            return false;
        }

        this.layoutSections(context.batcher.getFont());

        if (this.dragHandleArea.isInside(context.mouseX, context.mouseY) && context.mouseButton == 0)
        {
            if (this.isDockDragEnabled())
            {
                this.startDockDrag(context);
                context.setTimelineToolbarConsumePointer(true);

                return true;
            }

            /* Consume the click so locked handles don't fall through to the timeline. */
            context.setTimelineToolbarConsumePointer(true);

            return true;
        }

        if (context.mouseButton == 0 && this.canDragScrollSections()
            && this.isSectionsDragScrollTarget(context.mouseX, context.mouseY))
        {
            this.startSectionsPointer(context);
            context.setTimelineToolbarConsumePointer(true);

            return true;
        }

        int index = this.getSectionIndexAt(context.mouseX, context.mouseY);

        if (index < 0)
        {
            return true;
        }

        if (context.mouseButton == 0)
        {
            if (this.openMenu != null && this.openIndex == index)
            {
                this.closeOpenMenu();
            }
            else
            {
                this.openSection(context, index);
            }

            context.setTimelineToolbarConsumePointer(true);
        }

        return true;
    }

    @Override
    protected boolean subKeyPressed(UIContext context)
    {
        if (this.dockDragging && context.isPressed(GLFW.GLFW_KEY_ESCAPE))
        {
            this.cancelDockDrag();

            return true;
        }

        return super.subKeyPressed(context);
    }

    @Override
    protected boolean subMouseScrolled(UIContext context)
    {
        if (!this.area.isInside(context))
        {
            return false;
        }

        this.layoutSections(context.batcher.getFont());

        if (!this.sectionsScroll.hasScrollbar())
        {
            return false;
        }

        if (!this.sectionsViewportArea.isInside(context.mouseX, context.mouseY))
        {
            return false;
        }

        if (this.sectionsScroll.mouseScroll(context))
        {
            this.layoutSections(context.batcher.getFont());

            return true;
        }

        return false;
    }

    @Override
    protected boolean subMouseReleased(UIContext context)
    {
        if (this.sectionsPointerDown && context.mouseButton == 0)
        {
            this.finishSectionsPointer(context);

            return true;
        }

        if (this.dockDragging)
        {
            this.finishDockDrag(context);

            return true;
        }

        return super.subMouseReleased(context);
    }
}
