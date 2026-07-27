package mchorse.bbs_mod.ui.framework.elements;

import mchorse.bbs_mod.ui.framework.elements.utils.IViewportStack;

public interface IViewport
{
    public void apply(IViewportStack stack);

    public void unapply(IViewportStack stack);
}