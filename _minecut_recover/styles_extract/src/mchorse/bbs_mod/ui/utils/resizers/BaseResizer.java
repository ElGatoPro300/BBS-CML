package mchorse.bbs_mod.ui.utils.resizers;

import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.utils.Area;

public abstract class BaseResizer implements IResizer, IParentResizer
{
    @Override
    public void preApply(Area area)
    {}

    @Override
    public void apply(Area area)
    {}

    @Override
    public void apply(Area area, IResizer resizer, ChildResizer child)
    {}

    @Override
    public void postApply(Area area)
    {}

    @Override
    public void add(UIElement parent, UIElement child)
    {}

    @Override
    public void remove(UIElement parent, UIElement child)
    {}
}