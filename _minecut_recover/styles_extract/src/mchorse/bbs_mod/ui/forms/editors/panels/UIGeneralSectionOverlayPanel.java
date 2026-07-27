package mchorse.bbs_mod.ui.forms.editors.panels;

import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.UIScrollView;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIOverlayPanel;
import mchorse.bbs_mod.ui.utils.UI;

public class UIGeneralSectionOverlayPanel extends UIOverlayPanel
{
    public UIGeneralSectionOverlayPanel(IKey title, UIElement content)
    {
        super(title);

        content.removeFromParent();

        UIScrollView scrollView = UI.scrollView(5, 6, content);

        scrollView.full(this.content);
        this.content.add(scrollView);
    }

    @Override
    public UIGeneralSectionOverlayPanel resizable()
    {
        super.resizable();

        return this;
    }

    @Override
    public UIGeneralSectionOverlayPanel resizable(boolean value)
    {
        super.resizable(value);

        return this;
    }
}
