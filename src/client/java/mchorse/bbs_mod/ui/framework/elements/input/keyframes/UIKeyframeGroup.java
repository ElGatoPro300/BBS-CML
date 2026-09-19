package mchorse.bbs_mod.ui.framework.elements.input.keyframes;

import mchorse.bbs_mod.l10n.keys.IKey;

import java.util.ArrayList;
import java.util.List;

public class UIKeyframeGroup extends UIKeyframeElement
{
    public List<UIKeyframeElement> children = new ArrayList<>();
    public boolean collapsed = true;

    public UIKeyframeGroup(IKey title)
    {
        super(title, 0);
    }

    public void add(UIKeyframeElement element)
    {
        this.children.add(element);
    }
}
