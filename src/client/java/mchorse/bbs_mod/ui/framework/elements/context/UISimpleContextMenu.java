package mchorse.bbs_mod.ui.framework.elements.context;

import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.input.list.UIList;
import mchorse.bbs_mod.ui.utils.context.ContextAction;

public class UISimpleContextMenu extends UIContextMenu
{
    public UIList<ContextAction> actions;

    private ContextAction action;

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
}