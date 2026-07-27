package mchorse.bbs_mod.ui.framework.tooltips;

import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.ui.framework.UIContext;

public interface ITooltip
{
    public IKey getLabel();

    public void renderTooltip(UIContext context);
}
