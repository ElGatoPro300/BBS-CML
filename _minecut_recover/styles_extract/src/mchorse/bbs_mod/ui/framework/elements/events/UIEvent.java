package mchorse.bbs_mod.ui.framework.elements.events;

import mchorse.bbs_mod.ui.framework.elements.UIElement;

public abstract class UIEvent <T extends UIElement>
{
    public T element;

    public UIEvent(T element)
    {
        this.element = element;
    }
}