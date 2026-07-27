package mchorse.bbs_mod.ui.framework.elements.utils;

import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.IUIElement;
import mchorse.bbs_mod.ui.utils.Area;

import java.util.function.Consumer;

public class UIRenderable implements IUIElement
{
    public Consumer<UIContext> callback;

    public UIRenderable(Consumer<UIContext> callback)
    {
        this.callback = callback;
    }

    @Override
    public void resize()
    {}

    @Override
    public boolean isEnabled()
    {
        return false;
    }

    @Override
    public boolean isVisible()
    {
        return true;
    }

    @Override
    public IUIElement mouseClicked(UIContext context)
    {
        return null;
    }

    @Override
    public IUIElement mouseScrolled(UIContext context)
    {
        return null;
    }

    @Override
    public IUIElement mouseReleased(UIContext context)
    {
        return null;
    }

    @Override
    public IUIElement keyPressed(UIContext context)
    {
        return null;
    }

    @Override
    public IUIElement textInput(UIContext context)
    {
        return null;
    }

    @Override
    public boolean canBeRendered(Area viewport)
    {
        return true;
    }

    @Override
    public void render(UIContext context)
    {
        if (this.callback != null)
        {
            this.callback.accept(context);
        }
    }
}