package mchorse.bbs_mod.ui.utils.context;

import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.utils.FontRenderer;
import mchorse.bbs_mod.ui.utils.icons.Icons;

public class ContextSeparatorAction extends ContextAction
{
    public ContextSeparatorAction()
    {
        super(Icons.NONE, IKey.constant(""), null);
    }

    @Override
    public int getWidth(FontRenderer font)
    {
        return 28;
    }

    @Override
    public void render(UIContext context, FontRenderer font, int x, int y, int w, int h, boolean hover, boolean selected)
    {
        int cy = y + h / 2;
        context.batcher.box(x + 2, cy, x + w - 2, cy + 1, 0x22ffffff);
    }
}
