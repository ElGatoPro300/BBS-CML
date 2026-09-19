package mchorse.bbs_mod.ui.film.toolbar;

import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.utils.EventPropagation;
import mchorse.bbs_mod.ui.framework.elements.utils.FontRenderer;
import mchorse.bbs_mod.ui.utils.Area;
import mchorse.bbs_mod.ui.utils.Scroll;
import mchorse.bbs_mod.ui.utils.ScrollDirection;
import mchorse.bbs_mod.utils.MathUtils;
import mchorse.bbs_mod.utils.colors.Colors;

import org.lwjgl.glfw.GLFW;

import java.util.List;

/**
 * Popup element rendered on top of the global UI overlay showing the list of
 * items belonging to a toolbar section (or a nested submenu). Positions itself
 * in absolute overlay coordinates and coordinates open/close of its own
 * child submenus.
 *
 * <p>Phase 1 scope: this element renders and reacts to hover / click, but the
 * bound {@link ToolbarItem#runnable} references are typically {@code null},
 * so clicking a leaf is a no-op that just closes the chain.</p>
 */
public class ToolbarMenu extends UIElement
{
    /* Chain references */

    /**
     * Owner toolbar; the root of the popup chain. Used to coordinate open state
     * with the section buttons on the bar.
     */
    public final TimelineToolbar toolbar;

    /**
     * Parent popup in the chain, or {@code null} if this popup is directly
     * attached to a section button on the toolbar.
     */
    public final ToolbarMenu parentMenu;

    /* Content */

    public final List<ToolbarItem> items;

    /* Layout & interaction state */

    /**
     * Column at which shortcut text starts inside a row, in local pixels
     * relative to the row's left edge. Computed once in {@link #computeLayout}
     * so all shortcuts line up right-aligned to a common column.
     */
    private int shortcutColumn;

    /**
     * X coordinate at which the trailing arrow column starts (for submenu rows).
     */
    private int arrowColumn;

    /**
     * When {@code true}, the popup opened towards the left of its anchor (the
     * parent row) instead of the default right side. Used so the parent row's
     * trailing arrow flips to {@code <}.
     */
    private boolean openedToLeft;

    /**
     * Index of the item whose submenu is currently opened (in {@link #items}),
     * or {@code -1} if no submenu is open.
     */
    private int openChildIndex = -1;

    /**
     * Currently open child submenu, or {@code null}.
     */
    private ToolbarMenu openChild;

    /**
     * When {@code true} on the chain root, the entire popup chain is removed
     * at the start of the next {@link #render(UIContext)} pass. Deferring
     * avoids mutating the overlay child list during {@code mouseClicked}.
     */
    private boolean pendingCloseChain;

    /**
     * Natural (unclamped) popup size before fitting to the screen.
     */
    private int contentWidth;

    private int contentHeight;

    /**
     * Inner rectangle used for clipping and scrolling item rows (excludes
     * scrollbar gutters).
     */
    private final Area viewportArea = new Area();

    private final Scroll verticalScroll;

    private final Scroll horizontalScroll;

    /* Constructor */

    public ToolbarMenu(TimelineToolbar toolbar, ToolbarMenu parentMenu, List<ToolbarItem> items)
    {
        super();

        this.toolbar = toolbar;
        this.parentMenu = parentMenu;
        this.items = items;

        this.verticalScroll = new Scroll(this.viewportArea, 0, ScrollDirection.VERTICAL);
        this.verticalScroll.cancelScrollEdge = true;
        this.horizontalScroll = new Scroll(this.viewportArea, 0, ScrollDirection.HORIZONTAL);
        this.horizontalScroll.cancelScrollEdge = true;

        this.eventPropagataion(EventPropagation.BLOCK_INSIDE);
    }

    /* Public API */

    /**
     * Positions this popup so that its top-right anchors just above the given
     * anchor rectangle (used for root popups opening upward from the toolbar).
     * Falls back to opening downward if there is not enough space above.
     */
    public void openAbove(UIContext context, Area anchor)
    {
        this.computeLayout(context.batcher.getFont());

        int margin = TimelineToolbarSettings.MENU_SCREEN_MARGIN;
        int screenH = context.menu.height;
        int y;

        if (anchor.y - TimelineToolbarSettings.MENU_GAP - this.contentHeight >= margin)
        {
            y = anchor.y - TimelineToolbarSettings.MENU_GAP - this.contentHeight;
        }
        else
        {
            y = anchor.ey() + TimelineToolbarSettings.MENU_GAP;
        }

        this.finalizeOpen(context, anchor.x, y);
    }

    /**
     * Positions this popup below the anchor (used when the toolbar is docked to
     * the top edge). Falls back to opening above when there is not enough space.
     */
    public void openBelow(UIContext context, Area anchor)
    {
        this.computeLayout(context.batcher.getFont());

        int margin = TimelineToolbarSettings.MENU_SCREEN_MARGIN;
        int screenH = context.menu.height;
        int y;

        if (anchor.ey() + TimelineToolbarSettings.MENU_GAP + this.contentHeight <= screenH - margin)
        {
            y = anchor.ey() + TimelineToolbarSettings.MENU_GAP;
        }
        else
        {
            y = anchor.y - TimelineToolbarSettings.MENU_GAP - this.contentHeight;
        }

        this.finalizeOpen(context, anchor.x, y);
    }

    /**
     * Opens a root popup relative to a toolbar section, accounting for which
     * edge the toolbar is docked to.
     */
    public void openFromToolbar(UIContext context, Area anchor, TimelineToolbarDock dock)
    {
        switch (dock)
        {
            case TOP:
                this.openBelow(context, anchor);
                break;
            case LEFT:
                this.openBesides(context, anchor);
                break;
            case RIGHT:
                this.openBesidesLeft(context, anchor);
                break;
            case BOTTOM:
            default:
                this.openAbove(context, anchor);
                break;
        }
    }

    /**
     * Positions this popup as a child submenu of another popup, defaulting to
     * the right side of the parent row and flipping left when needed.
     */
    public void openBesides(UIContext context, Area rowRect)
    {
        this.computeLayout(context.batcher.getFont());

        int margin = TimelineToolbarSettings.MENU_SCREEN_MARGIN;
        int screenW = context.menu.width;
        int maxInnerW = screenW - margin * 2;
        int wEst = Math.min(this.contentWidth, maxInnerW);

        int preferredX = rowRect.ex() + TimelineToolbarSettings.MENU_GAP;
        int x;

        if (preferredX + wEst <= screenW - margin)
        {
            x = preferredX;
            this.openedToLeft = false;
        }
        else
        {
            int leftX = rowRect.x - TimelineToolbarSettings.MENU_GAP - wEst;

            if (leftX >= margin)
            {
                x = leftX;
                this.openedToLeft = true;
            }
            else
            {
                x = Math.max(margin, screenW - wEst - margin);
                this.openedToLeft = false;
            }
        }

        this.finalizeOpen(context, x, rowRect.y);
    }

    /**
     * Positions this popup to the left of the anchor (used when the toolbar is
     * docked to the right edge). Falls back to the right side when needed.
     */
    public void openBesidesLeft(UIContext context, Area rowRect)
    {
        this.computeLayout(context.batcher.getFont());

        int margin = TimelineToolbarSettings.MENU_SCREEN_MARGIN;
        int screenW = context.menu.width;
        int maxInnerW = screenW - margin * 2;
        int wEst = Math.min(this.contentWidth, maxInnerW);

        int preferredX = rowRect.x - TimelineToolbarSettings.MENU_GAP - wEst;
        int x;

        if (preferredX >= margin)
        {
            x = preferredX;
            this.openedToLeft = true;
        }
        else
        {
            int rightX = rowRect.ex() + TimelineToolbarSettings.MENU_GAP;

            if (rightX + wEst <= screenW - margin)
            {
                x = rightX;
                this.openedToLeft = false;
            }
            else
            {
                x = Math.max(margin, screenW - wEst - margin);
                this.openedToLeft = true;
            }
        }

        this.finalizeOpen(context, x, rowRect.y);
    }

    public boolean isOpenedToLeft()
    {
        return this.openedToLeft;
    }

    public void closeChain()
    {
        this.getChainRoot().pendingCloseChain = true;
    }

    private ToolbarMenu getChainRoot()
    {
        ToolbarMenu root = this;

        while (root.parentMenu != null)
        {
            root = root.parentMenu;
        }

        return root;
    }

    private void processPendingCloseChain()
    {
        if (!this.pendingCloseChain)
        {
            return;
        }

        this.pendingCloseChain = false;

        ToolbarMenu current = this;

        while (current != null)
        {
            ToolbarMenu next = current.openChild;

            current.openChild = null;
            current.openChildIndex = -1;
            current.removeFromParent();
            current = next;
        }

        if (this.toolbar.isActiveRootMenu(this))
        {
            this.toolbar.notifyChainClosed();
        }
    }

    public void closeChild()
    {
        if (this.openChild != null)
        {
            this.openChild.closeChild();
            this.openChild.removeFromParent();
            this.openChild = null;
        }

        this.openChildIndex = -1;
    }

    /**
     * Registers this popup and any open child submenus on the context so
     * timelines underneath can ignore pointer hover while menus are open.
     */
    public void collectChainAreas(UIContext context)
    {
        context.registerTimelineToolbarMenuArea(this.area);

        if (this.openChild != null)
        {
            this.openChild.collectChainAreas(context);
        }
    }

    public boolean isChainAt(int x, int y)
    {
        return this.chainContainsPoint(x, y);
    }

    /* Layout */

    private void computeLayout(FontRenderer font)
    {
        int labelMaxWidth = 0;
        int shortcutMaxWidth = 0;
        boolean anyHasSubmenu = false;

        for (ToolbarItem item : this.items)
        {
            if (item.separator || item.label == null)
            {
                continue;
            }

            int labelW = font.getWidth(item.label.get());

            if (labelW > labelMaxWidth) labelMaxWidth = labelW;

            if (item.keyCombo != null)
            {
                int scW = font.getWidth(item.keyCombo.getKeyCombo());

                if (scW > shortcutMaxWidth) shortcutMaxWidth = scW;
            }

            if (item.hasChildren())
            {
                anyHasSubmenu = true;
            }
        }

        int contentLeft = TimelineToolbarSettings.MENU_ITEM_PADDING_LEFT
            + TimelineToolbarSettings.MENU_ITEM_ICON_SLOT
            + TimelineToolbarSettings.MENU_ITEM_ICON_LABEL_GAP;

        this.shortcutColumn = contentLeft + labelMaxWidth + TimelineToolbarSettings.MENU_LABEL_SHORTCUT_GAP;

        int rowEnd = this.shortcutColumn + shortcutMaxWidth;

        if (anyHasSubmenu)
        {
            rowEnd += TimelineToolbarSettings.MENU_SHORTCUT_ARROW_GAP + TimelineToolbarSettings.MENU_ARROW_WIDTH;
        }

        this.arrowColumn = rowEnd - TimelineToolbarSettings.MENU_ARROW_WIDTH;

        int totalWidth = Math.max(TimelineToolbarSettings.MENU_MIN_WIDTH,
            rowEnd + TimelineToolbarSettings.MENU_ITEM_PADDING_RIGHT + TimelineToolbarSettings.MENU_WIDTH_PADDING);

        int totalHeight = 4;

        for (ToolbarItem item : this.items)
        {
            totalHeight += this.getItemHeight(item);
        }

        totalHeight += 4;

        this.contentWidth = totalWidth;
        this.contentHeight = totalHeight;
    }

    /**
     * Clamps the popup to the screen, reserves scrollbar gutters when content
     * overflows, and resets scroll offsets.
     */
    private void finalizeOpen(UIContext context, int x, int y)
    {
        int margin = TimelineToolbarSettings.MENU_SCREEN_MARGIN;
        int screenW = context.menu.width;
        int screenH = context.menu.height;
        int sb = BBSSettings.scrollbarWidth.get();

        int maxInnerW = screenW - margin * 2;
        int maxInnerH = screenH - margin * 2;

        int innerW = Math.min(this.contentWidth, maxInnerW);
        int innerH = Math.min(this.contentHeight, maxInnerH);

        boolean needV = this.contentHeight > innerH;

        if (needV)
        {
            maxInnerW -= sb;
        }

        innerW = Math.min(this.contentWidth, maxInnerW);
        boolean needH = this.contentWidth > innerW;

        if (needH)
        {
            maxInnerH -= sb;
            innerH = Math.min(this.contentHeight, maxInnerH);
            needV = this.contentHeight > innerH;
        }

        if (needV && this.contentWidth > innerW)
        {
            innerW = Math.min(this.contentWidth, screenW - margin * 2 - sb);
            needH = this.contentWidth > innerW;
        }

        int popupW = innerW + (needV ? sb : 0);
        int popupH = innerH + (needH ? sb : 0);

        x = MathUtils.clamp(x, margin, Math.max(margin, screenW - popupW - margin));
        y = MathUtils.clamp(y, margin, Math.max(margin, screenH - popupH - margin));

        this.area.set(x, y, popupW, popupH);
        this.viewportArea.set(x, y, innerW, innerH);

        this.verticalScroll.setScroll(0);
        this.horizontalScroll.setScroll(0);
        this.verticalScroll.scrollSize = this.contentHeight;
        this.horizontalScroll.scrollSize = this.contentWidth;
        this.verticalScroll.clamp();
        this.horizontalScroll.clamp();
    }

    private int getScrollX()
    {
        return this.viewportArea.x - (int) this.horizontalScroll.getScroll();
    }

    private int getContentYStart()
    {
        return this.viewportArea.y + 4 - (int) this.verticalScroll.getScroll();
    }

    private int getItemHeight(ToolbarItem item)
    {
        if (item.separator)
        {
            return TimelineToolbarSettings.MENU_SEPARATOR_HEIGHT;
        }

        return TimelineToolbarSettings.MENU_ITEM_HEIGHT;
    }

    private int getRowIndexAt(int mouseX, int mouseY)
    {
        if (!this.viewportArea.isInside(mouseX, mouseY))
        {
            return -1;
        }

        int y = this.getContentYStart();

        for (int i = 0; i < this.items.size(); i++)
        {
            ToolbarItem item = this.items.get(i);
            int h = this.getItemHeight(item);

            if (mouseY >= y && mouseY < y + h)
            {
                return i;
            }

            y += h;
        }

        return -1;
    }

    private Area getRowArea(int index)
    {
        int y = this.getContentYStart();

        for (int i = 0; i < this.items.size(); i++)
        {
            ToolbarItem item = this.items.get(i);
            int h = this.getItemHeight(item);

            if (i == index)
            {
                Area a = new Area();

                a.set(this.getScrollX(), y, this.contentWidth, h);

                return a;
            }

            y += h;
        }

        return null;
    }

    /**
     * Whether {@code (x, y)} lies inside any open descendant submenu of this
     * popup. Used when submenus overlap their parent after viewport clamping.
     */
    private boolean isPointerOverDescendantMenu(int x, int y)
    {
        ToolbarMenu descendant = this.openChild;

        while (descendant != null)
        {
            if (descendant.area.isInside(x, y))
            {
                return true;
            }

            descendant = descendant.openChild;
        }

        return false;
    }

    /**
     * When a child submenu popup overlaps its parent's anchor row (e.g. opened
     * to the left), clicks on that row must stay with the parent semantics:
     * pure containers are a no-op and must not close the chain.
     */
    private boolean isClickOnParentPureSubmenuAnchor(UIContext context)
    {
        if (this.parentMenu == null || this.parentMenu.openChildIndex < 0)
        {
            return false;
        }

        Area row = this.parentMenu.getRowArea(this.parentMenu.openChildIndex);

        if (row == null || !row.isInside(context))
        {
            return false;
        }

        ToolbarItem item = this.parentMenu.items.get(this.parentMenu.openChildIndex);

        return item.isPureSubmenuContainer();
    }

    /**
     * Whether {@code (x, y)} lies inside any popup of this toolbar menu chain
     * (root section menu plus open nested submenus).
     */
    private boolean isClickWithinMenuChain(int x, int y)
    {
        ToolbarMenu current = this.getChainRoot();

        while (current != null)
        {
            if (current.area.isInside(x, y))
            {
                return true;
            }

            current = current.openChild;
        }

        return false;
    }

    /* Rendering */

    @Override
    public void render(UIContext context)
    {
        ToolbarMenu root = this.getChainRoot();

        if (root.pendingCloseChain)
        {
            if (this == root)
            {
                root.processPendingCloseChain();
            }

            return;
        }

        this.renderBackground(context);
        this.verticalScroll.drag(context.mouseX, context.mouseY);
        this.horizontalScroll.drag(context.mouseX, context.mouseY);
        this.updateHover(context);

        context.batcher.clip(this.viewportArea, context);
        this.renderItems(context);
        context.batcher.unclip(context);

        this.verticalScroll.renderScrollbar(context.batcher);
        this.horizontalScroll.renderScrollbar(context.batcher);

        super.render(context);

        this.renderDisabledReasonTooltip(context);
        this.checkDismissByDistance(context);
    }

    private void renderBackground(UIContext context)
    {
        context.batcher.dropShadow(this.area.x, this.area.y, this.area.ex(), this.area.ey(),
            TimelineToolbarSettings.MENU_SHADOW_OFFSET,
            TimelineToolbarSettings.getMenuShadowInner(),
            TimelineToolbarSettings.getMenuShadowOuter());
        context.batcher.box(this.area.x, this.area.y, this.area.ex(), this.area.ey(),
            TimelineToolbarSettings.getMenuBackground());
        context.batcher.outline(this.area.x, this.area.y, this.area.ex(), this.area.ey(),
            TimelineToolbarSettings.getMenuBorder());
    }

    private void updateHover(UIContext context)
    {
        int mx = context.mouseX;
        int my = context.mouseY;

        if (!this.area.isInside(context))
        {
            if (this.openChild != null
                && (this.openChild.isChainAt(mx, my)
                    || this.isPointerInOpenChildBridge(mx, my)))
            {
                return;
            }

            if (this.openChildIndex != -1)
            {
                this.closeChild();
            }

            return;
        }

        if (this.isPointerOverDescendantMenu(mx, my))
        {
            return;
        }

        int hoveredIndex = this.getRowIndexAt(mx, my);

        if (hoveredIndex < 0)
        {
            if (this.openChildIndex != -1)
            {
                this.closeChild();
            }

            return;
        }

        ToolbarItem item = this.items.get(hoveredIndex);

        if (item.separator)
        {
            if (this.openChildIndex != -1)
            {
                this.closeChild();
            }

            return;
        }

        if (item.hasChildren())
        {
            if (this.openChildIndex != hoveredIndex)
            {
                this.openSubmenu(context, hoveredIndex, item);
            }
        }
        else if (this.openChildIndex != -1)
        {
            this.closeChild();
        }
    }

    private void openSubmenu(UIContext context, int index, ToolbarItem item)
    {
        this.closeChild();

        ToolbarMenu child = new ToolbarMenu(this.toolbar, this, item.children);
        context.menu.overlay.add(child);
        child.openBesides(context, this.getRowArea(index));

        this.openChildIndex = index;
        this.openChild = child;
    }

    /**
     * Hit-test corridor between the anchored submenu row and its child popup,
     * including {@link TimelineToolbarSettings#MENU_GAP} and padding.
     */
    private boolean isPointerInOpenChildBridge(int x, int y)
    {
        if (this.openChild == null || this.openChildIndex < 0)
        {
            return false;
        }

        Area row = this.getRowArea(this.openChildIndex);

        if (row == null)
        {
            return false;
        }

        Area child = this.openChild.area;
        int pad = TimelineToolbarSettings.SUBMENU_BRIDGE_PADDING;
        int bridgeX1;
        int bridgeX2;

        if (this.openChild.isOpenedToLeft())
        {
            bridgeX1 = child.ex() - pad;
            bridgeX2 = row.x + pad;
        }
        else
        {
            bridgeX1 = row.ex() - pad;
            bridgeX2 = child.x + pad;
        }

        int bridgeY1 = Math.min(row.y, child.y) - pad;
        int bridgeY2 = Math.max(row.ey(), child.ey()) + pad;

        if (bridgeX2 <= bridgeX1)
        {
            bridgeX1 = Math.min(row.x, child.x) - pad;
            bridgeX2 = Math.max(row.ex(), child.ex()) + pad;
        }

        return x >= bridgeX1 && x < bridgeX2 && y >= bridgeY1 && y < bridgeY2;
    }

    private void renderItems(UIContext context)
    {
        FontRenderer font = context.batcher.getFont();
        int y = this.getContentYStart();
        int clipTop = this.viewportArea.y;
        int clipBottom = this.viewportArea.ey();
        boolean pointerOnDescendant = this.isPointerOverDescendantMenu(context.mouseX, context.mouseY);

        for (int i = 0; i < this.items.size(); i++)
        {
            ToolbarItem item = this.items.get(i);
            int h = this.getItemHeight(item);

            if (y + h < clipTop || y >= clipBottom)
            {
                y += h;

                continue;
            }

            if (item.separator)
            {
                this.renderSeparator(context, y, h);
            }
            else
            {
                boolean hover = !pointerOnDescendant
                    && this.viewportArea.isInside(context.mouseX, context.mouseY)
                    && context.mouseY >= y && context.mouseY < y + h;

                this.renderRow(context, font, item, i, y, h, hover);
            }

            y += h;
        }
    }

    private void renderSeparator(UIContext context, int y, int h)
    {
        int midY = y + h / 2;
        int x1 = this.getScrollX() + 6;
        int x2 = this.getScrollX() + this.contentWidth - 6;

        context.batcher.box(x1, midY, x2, midY + 1, TimelineToolbarSettings.getMenuBorder());
    }

    private void renderRow(UIContext context, FontRenderer font, ToolbarItem item, int index, int y, int h,
        boolean hover)
    {
        boolean enabled = item.isEnabled();
        int labelColor = enabled ? TimelineToolbarSettings.MENU_ITEM_FG
            : TimelineToolbarSettings.MENU_ITEM_DISABLED_FG;
        int shortcutColor = enabled ? TimelineToolbarSettings.MENU_ITEM_SHORTCUT_FG
            : TimelineToolbarSettings.MENU_ITEM_DISABLED_FG;
        int iconColor = enabled ? TimelineToolbarSettings.MENU_ITEM_FG
            : TimelineToolbarSettings.MENU_ITEM_DISABLED_FG;

        int rowX1 = this.getScrollX();
        int rowX2 = rowX1 + this.contentWidth;

        if (hover && (enabled || item.hasChildren()))
        {
            context.batcher.box(rowX1, y, rowX2, y + h, TimelineToolbarSettings.MENU_ITEM_HOVER);
        }

        if (item.destructive)
        {
            /* Vertical red bar on the left, same visual as UIIcon for REMOVE/TRASH. */
            context.batcher.box(rowX1, y, rowX1 + 2, y + h, Colors.A100 | Colors.RED);
            context.batcher.gradientHBox(rowX1 + 2, y, rowX1 + 20, y + h, Colors.A25 | Colors.RED, 0);
        }
        else if (item.accentColor != 0)
        {
            /* Vertical color bar on the left, same visual as ColorfulContextAction. */
            context.batcher.box(rowX1, y, rowX1 + 2, y + h, Colors.A100 | item.accentColor);
            context.batcher.gradientHBox(rowX1 + 2, y, rowX1 + 20, y + h, Colors.A25 | item.accentColor, 0);
        }

        int iconSlotX = rowX1 + TimelineToolbarSettings.MENU_ITEM_PADDING_LEFT;
        int iconCenterY = y + h / 2;

        if (item.icon != null)
        {
            context.batcher.icon(item.icon, iconColor,
                iconSlotX + TimelineToolbarSettings.MENU_ITEM_ICON_SLOT / 2, iconCenterY, 0.5F, 0.5F);
        }

        int labelX = iconSlotX + TimelineToolbarSettings.MENU_ITEM_ICON_SLOT
            + TimelineToolbarSettings.MENU_ITEM_ICON_LABEL_GAP;
        int labelY = y + (h - font.getHeight()) / 2 + 1;

        if (item.label != null)
        {
            context.batcher.text(item.label.get(), labelX, labelY, labelColor, false);
        }

        if (item.keyCombo != null)
        {
            String shortcut = item.keyCombo.getKeyCombo();

            context.batcher.text(shortcut, rowX1 + this.shortcutColumn, labelY, shortcutColor, false);
        }

        if (item.hasChildren())
        {
            boolean submenuOpen = this.openChildIndex == index && this.openChild != null;
            String arrow = submenuOpen && this.openChild.isOpenedToLeft()
                ? "<"
                : ">";
            int arrowX = rowX1 + this.arrowColumn;
            int arrowY = labelY;

            context.batcher.text(arrow, arrowX, arrowY,
                enabled ? TimelineToolbarSettings.MENU_ARROW_FG
                    : TimelineToolbarSettings.MENU_ITEM_DISABLED_FG, false);
        }
    }

    /**
     * Draw the disabled-reason card after all rows so it is not covered by
     * labels/icons of items rendered later in the same popup.
     */
    private void renderDisabledReasonTooltip(UIContext context)
    {
        if (!this.area.isInside(context)
            || this.isPointerOverDescendantMenu(context.mouseX, context.mouseY))
        {
            return;
        }

        int hoveredIndex = this.getRowIndexAt(context.mouseX, context.mouseY);

        if (hoveredIndex < 0)
        {
            return;
        }

        ToolbarItem item = this.items.get(hoveredIndex);

        if (item.separator || item.isEnabled())
        {
            return;
        }

        IKey reason = item.getDisabledReason();

        if (reason == null)
        {
            return;
        }

        TimelineToolbarTooltips.drawForeground(context, reason.get(), context.mouseX, context.mouseY,
            TimelineToolbarSettings.MENU_ITEM_DISABLED_REASON_FG, Colors.A75, this.toolbar.getDock(), true);
    }

    private void checkDismissByDistance(UIContext context)
    {
        /* Only the root popup runs the check to avoid duplicated work. */
        if (this.parentMenu != null)
        {
            return;
        }

        int mx = context.mouseX;
        int my = context.mouseY;
        int threshold = TimelineToolbarSettings.TOOLBAR_MENU_DISMISS_DISTANCE_PX;

        if (this.chainContainsPoint(mx, my))
        {
            return;
        }

        if (this.toolbar.area.isInside(mx, my))
        {
            return;
        }

        int minDist = this.distanceToRect(this.toolbar.area, mx, my);
        ToolbarMenu current = this;

        while (current != null)
        {
            int d = this.distanceToRect(current.area, mx, my);

            if (d < minDist) minDist = d;

            current = current.openChild;
        }

        if (minDist > threshold)
        {
            this.closeChain();
        }
    }

    private boolean chainContainsPoint(int x, int y)
    {
        ToolbarMenu current = this;

        while (current != null)
        {
            if (current.area.isInside(x, y))
            {
                return true;
            }

            current = current.openChild;
        }

        return false;
    }

    private int distanceToRect(Area a, int x, int y)
    {
        int dx = 0;
        int dy = 0;

        if (x < a.x) dx = a.x - x;
        else if (x > a.ex()) dx = x - a.ex();

        if (y < a.y) dy = a.y - y;
        else if (y > a.ey()) dy = y - a.ey();

        if (dx == 0 && dy == 0) return 0;

        return (int) Math.sqrt(dx * dx + dy * dy);
    }

    /* Input */

    @Override
    protected boolean subMouseClicked(UIContext context)
    {
        if (this.area.isInside(context))
        {
            if (this.verticalScroll.mouseClicked(context.mouseX, context.mouseY)
                || this.horizontalScroll.mouseClicked(context.mouseX, context.mouseY))
            {
                context.setTimelineToolbarConsumePointer(true);

                return true;
            }
        }

        if (!this.area.isInside(context))
        {
            /* Let the toolbar handle section-button clicks (toggle / switch)
             * instead of treating them as outside dismiss. */
            if (this.toolbar.isToolbarSectionAt(context.mouseX, context.mouseY))
            {
                return false;
            }

            /* A nested submenu must not treat clicks on the parent popup (e.g.
             * the anchored Interpolation row) as an outside dismiss. */
            if (this.isClickWithinMenuChain(context.mouseX, context.mouseY))
            {
                return false;
            }

            /* Click outside chain: close everything and let it propagate to
             * whatever is below (so the existing right-click context menu can
             * still open normally). */
            this.closeChain();

            return false;
        }

        /* Clicks on any toolbar popup row must not reach the timeline underneath. */
        context.setTimelineToolbarConsumePointer(true);

        if (this.isPointerOverDescendantMenu(context.mouseX, context.mouseY))
        {
            return true;
        }

        int index = this.getRowIndexAt(context.mouseX, context.mouseY);

        if (index < 0)
        {
            return true;
        }

        if (this.isClickOnParentPureSubmenuAnchor(context))
        {
            return true;
        }

        ToolbarItem item = this.items.get(index);

        if (item.separator || !item.isEnabled())
        {
            return true;
        }

        if (context.mouseButton == 0)
        {
            if (item.hasChildren())
            {
                /* Pure containers (e.g. Interpolation): hover-only, click is a no-op. */
                if (item.hasDefaultAction())
                {
                    if (item.runnable != null)
                    {
                        item.runnable.run();
                    }

                    this.closeChain();
                }

                return true;
            }
            else if (item.runnable != null)
            {
                item.runnable.run();
                this.closeChain();
            }
            else
            {
                /* Leaf action (e.g. Linear, Bezier): close even when not wired yet. */
                this.closeChain();
            }
        }
        else if (context.mouseButton == 1)
        {
            /* Right-click inside the popup just dismisses without opening the
             * underlying context menu. */
            this.closeChain();
        }

        return true;
    }

    @Override
    protected boolean subMouseReleased(UIContext context)
    {
        this.verticalScroll.mouseReleased(context.mouseX, context.mouseY);
        this.horizontalScroll.mouseReleased(context.mouseX, context.mouseY);

        return super.subMouseReleased(context);
    }

    @Override
    protected boolean subMouseScrolled(UIContext context)
    {
        if (!this.area.isInside(context))
        {
            return false;
        }

        if (this.verticalScroll.mouseScroll(context)
            || this.horizontalScroll.mouseScroll(context))
        {
            context.setTimelineToolbarConsumePointer(true);

            return true;
        }

        return super.subMouseScrolled(context);
    }

    @Override
    protected boolean subKeyPressed(UIContext context)
    {
        if (context.isPressed(GLFW.GLFW_KEY_ESCAPE))
        {
            this.closeChain();

            return true;
        }

        return super.subKeyPressed(context);
    }
}
