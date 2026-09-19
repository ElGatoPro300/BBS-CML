package mchorse.bbs_mod.ui.framework.elements.input.keyframes;

import mchorse.bbs_mod.l10n.keys.IKey;

public abstract class UIKeyframeElement
{
    public IKey title;
    public int color;
    public boolean separator;
    
    public UIKeyframeElement(IKey title, int color)
    {
        this.title = title;
        this.color = color;
    }
}
