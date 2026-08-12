package mchorse.bbs_mod.ui.film.replays.overlays;

import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.utils.EventPropagation;

public class UIReplayPropertiesPanel extends UIElement
{
    public UIReplayPropertiesPanel(UIReplaysOverlayPanel overlay)
    {
        overlay.attachPropertiesHost(this);
        this.mouseEventPropagataion(EventPropagation.BLOCK_INSIDE);
    }

    @Override
    public void render(UIContext context)
    {
        context.batcher.box(this.area.x, this.area.y, this.area.ex(), this.area.ey(), 0xFF141418);
        context.batcher.outline(this.area.x, this.area.y, this.area.ex(), this.area.ey(), 0xFF2A2A35, 1);

        super.render(context);
    }
}
