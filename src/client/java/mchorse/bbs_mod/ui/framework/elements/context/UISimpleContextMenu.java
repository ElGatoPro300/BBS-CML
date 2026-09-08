package mchorse.bbs_mod.ui.framework.elements.context;

import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.events.UIRemovedEvent;
import mchorse.bbs_mod.ui.framework.elements.input.list.UIList;
import mchorse.bbs_mod.ui.utils.Area;
import mchorse.bbs_mod.ui.utils.context.ContextAction;
import mchorse.bbs_mod.ui.utils.context.ContextCategoryAction;
import mchorse.bbs_mod.ui.utils.context.ContextSeparatorAction;

public class UISimpleContextMenu extends UIContextMenu
{
    private static final int SUBMENU_GAP = 2;
    private static final int SUBMENU_BRIDGE_PAD = 6;
    private static final int SUBMENU_SCREEN_MARGIN = 5;

    public UIList<ContextAction> actions;

    private ContextAction action;
    private UISimpleContextMenu parentMenu;
    private UISimpleContextMenu childMenu;
    private int openChildIndex = -1;
    private boolean childOpenedToLeft;
    /** True while closing a hover flyout without dismissing this menu. */
    private boolean closingChildSilently;

    public UISimpleContextMenu()
    {
        super();

        this.actions = new UIActionList((action) ->
        {
            if (action.get(0).runnable != null)
            {
                this.action = action.get(0);
            }
        });

        this.actions.cancelScrollEdge().full(this);
        this.add(this.actions);
    }

    @Override
    public boolean isEmpty()
    {
        return this.actions.getList().isEmpty();
    }

    @Override
    public void setMouse(UIContext context)
    {
        int w = 100;

        for (ContextAction action : this.actions.getList())
        {
            w = Math.max(action.getWidth(context.batcher.getFont()), w);
        }

        this.set(context.mouseX(), context.mouseY(), w, 0).h(this.actions.scroll.scrollSize).maxH(context.menu.height - 10).bounds(context.menu.overlay, 5);
    }

    /**
     * Place this menu beside {@code row} (right by default, left when clipped).
     */
    public void setBeside(UIContext context, Area row)
    {
        int w = 100;

        for (ContextAction action : this.actions.getList())
        {
            w = Math.max(action.getWidth(context.batcher.getFont()), w);
        }

        int h = this.actions.scroll.scrollSize;
        int margin = SUBMENU_SCREEN_MARGIN;
        int screenW = context.menu.width;
        int screenH = context.menu.height;
        int maxH = screenH - margin * 2;

        h = Math.min(h, maxH);

        int preferredX = row.ex() + SUBMENU_GAP;
        int x;

        if (preferredX + w <= screenW - margin)
        {
            x = preferredX;
            this.childOpenedToLeft = false;
        }
        else
        {
            int leftX = row.x - SUBMENU_GAP - w;

            if (leftX >= margin)
            {
                x = leftX;
                this.childOpenedToLeft = true;
            }
            else
            {
                x = Math.max(margin, screenW - w - margin);
                this.childOpenedToLeft = false;
            }
        }

        int y = Math.max(margin, Math.min(row.y, screenH - h - margin));

        this.set(x, y, w, h);
        this.resize();
    }

    public boolean isChildOpenedToLeft()
    {
        return this.childOpenedToLeft;
    }

    @Override
    public void removeFromParent()
    {
        this.closeChild();

        super.removeFromParent();
    }

    @Override
    public boolean subMouseClicked(UIContext context)
    {
        this.action = null;

        if (this.area.isInside(context))
        {
            return false;
        }

        if (this.childMenu != null && this.childMenu.area.isInside(context))
        {
            return false;
        }

        if (this.isPointerInOpenChildBridge(context.mouseX, context.mouseY))
        {
            return false;
        }

        if (this.parentMenu != null)
        {
            if (this.parentMenu.area.isInside(context)
                || this.parentMenu.isPointerInOpenChildBridge(context.mouseX, context.mouseY))
            {
                /* Stay open while the pointer is still over the parent row / bridge;
                   hover logic on the parent will close this flyout if needed. */
                return false;
            }

            this.parentMenu.removeFromParent();

            return true;
        }

        this.removeFromParent();

        return true;
    }

    @Override
    public boolean subMouseReleased(UIContext context)
    {
        if (this.action != null)
        {
            this.action.runnable.run();
            this.removeFromParent();

            return true;
        }

        return super.subMouseReleased(context);
    }

    public void pick(int index)
    {
        this.actions.setIndex(index);

        ContextAction action = this.actions.getCurrentFirst();

        if (action != null && action.runnable != null)
        {
            action.runnable.run();
            this.removeFromParent();
        }
    }

    @Override
    public void render(UIContext context)
    {
        this.updateHoverSubmenu(context);
        this.syncSubmenuArrows();

        super.render(context);
    }

    private void syncSubmenuArrows()
    {
        for (int i = 0; i < this.actions.getList().size(); i++)
        {
            ContextAction action = this.actions.getList().get(i);

            action.submenuOpenedToLeft = this.openChildIndex == i && this.childMenu != null && this.childOpenedToLeft;
        }
    }

    private void updateHoverSubmenu(UIContext context)
    {
        int mx = context.mouseX;
        int my = context.mouseY;

        if (this.childMenu != null
            && (this.childMenu.area.isInside(mx, my) || this.isPointerInOpenChildBridge(mx, my)))
        {
            return;
        }

        if (!(this.actions instanceof UIActionList actionList))
        {
            return;
        }

        int hovered = actionList.getIndexAt(my);

        if (hovered < 0 || !this.area.isInside(mx, my))
        {
            this.closeChild();

            return;
        }

        ContextAction action = this.actions.getList().get(hovered);

        if (action instanceof ContextSeparatorAction || action instanceof ContextCategoryAction)
        {
            this.closeChild();

            return;
        }

        if (action.hasChildren())
        {
            if (this.openChildIndex != hovered)
            {
                this.openChild(context, hovered, action);
            }
        }
        else
        {
            this.closeChild();
        }
    }

    private void openChild(UIContext context, int index, ContextAction action)
    {
        this.closeChild();

        UISimpleContextMenu child = new UISimpleContextMenu();

        child.parentMenu = this;
        child.actions.add(action.children);
        context.menu.overlay.add(child);

        Area row = this.getRowArea(index);

        if (row == null)
        {
            child.removeFromParent();

            return;
        }

        child.setBeside(context, row);
        this.childOpenedToLeft = child.childOpenedToLeft;
        this.openChildIndex = index;
        this.childMenu = child;

        /* Picking an item in the flyout dismisses the whole menu tree. */
        child.getEvents().register(UIRemovedEvent.class, (e) ->
        {
            if (this.childMenu == child)
            {
                this.childMenu = null;
                this.openChildIndex = -1;
            }

            if (!this.closingChildSilently && this.hasParent())
            {
                this.removeFromParent();
            }
        });
    }

    private void closeChild()
    {
        if (this.childMenu != null)
        {
            UISimpleContextMenu child = this.childMenu;

            this.childMenu = null;
            this.openChildIndex = -1;
            this.closingChildSilently = true;
            child.removeFromParent();
            this.closingChildSilently = false;
        }
    }

    private Area getRowArea(int index)
    {
        if (!(this.actions instanceof UIActionList actionList) || index < 0 || index >= this.actions.getList().size())
        {
            return null;
        }

        int y = this.area.y - (int) this.actions.scroll.getScroll();

        for (int i = 0; i < index; i++)
        {
            y += actionList.getItemHeight(this.actions.getList().get(i));
        }

        int h = actionList.getItemHeight(this.actions.getList().get(index));

        return new Area(this.area.x, y, this.area.w, h);
    }

    private boolean isPointerInOpenChildBridge(int x, int y)
    {
        if (this.childMenu == null || this.openChildIndex < 0)
        {
            return false;
        }

        Area row = this.getRowArea(this.openChildIndex);

        if (row == null)
        {
            return false;
        }

        Area child = this.childMenu.area;
        int pad = SUBMENU_BRIDGE_PAD;
        int bridgeX1;
        int bridgeX2;

        if (this.childOpenedToLeft)
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
}
