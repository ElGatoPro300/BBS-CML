package mchorse.bbs_mod.ui.utils.resizers;

import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.utils.Area;

public interface IResizer
{
    public void preApply(Area area);

    public void apply(Area area);

    public void postApply(Area area);

    public void add(UIElement parent, UIElement child);

    public void remove(UIElement parent, UIElement child);

    public int getX();

    public int getY();

    public int getW();

    public int getH();
}