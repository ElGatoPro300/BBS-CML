package mchorse.bbs_mod.ui.framework.elements.navigation;

import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.utils.colors.Colors;

public class UIControlBar extends UIElement
{
    @Override
    public void render(UIContext context)
    {
        this.area.render(context.batcher, Colors.CONTROL_BAR);

        super.render(context);
    }
}
