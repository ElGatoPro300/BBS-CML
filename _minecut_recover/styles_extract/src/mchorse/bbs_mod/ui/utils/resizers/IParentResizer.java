package mchorse.bbs_mod.ui.utils.resizers;

import mchorse.bbs_mod.ui.utils.Area;

public interface IParentResizer
{
    public void apply(Area area, IResizer resizer, ChildResizer child);
}
