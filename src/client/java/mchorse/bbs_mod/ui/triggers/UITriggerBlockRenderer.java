package mchorse.bbs_mod.ui.triggers;

import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.IUIElement;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.utils.UIModelRenderer;

public class UITriggerBlockRenderer extends UIModelRenderer
{
    @Override
    public void render(UIContext context)
    {
        this.setupPosition();
        this.processInputs(context);
        
        for (IUIElement child : this.getChildren())
        {
            child.render(context);
        }
    }

    @Override
    protected void renderUserModel(UIContext context)
    {}
}
