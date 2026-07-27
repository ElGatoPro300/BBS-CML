package mchorse.bbs_minecut_ui.film;

import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.IUIElement;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_minecut_ui.styles.MinecutTokens;

/**
 * Single-purpose Properties dock leaf. Classic tab chrome supplies the label
 * when several of these share a {@code TabbedNode}.
 */
public class UIMinecutPropertiesPanel extends UIMinecutRegion
{
    public final UIElement host;

    public UIMinecutPropertiesPanel(String title)
    {
        super(title);
        this.noHeader();

        this.host = new UIElement();
        this.host.relative(this).x(0).y(0).w(1F).h(1F);
        this.add(this.host);
    }

    public UIElement getHost()
    {
        return this.host;
    }

    public boolean hasVisibleContent()
    {
        if (this.host.getChildren().isEmpty())
        {
            return false;
        }

        for (IUIElement child : this.host.getChildren())
        {
            if (child instanceof UIElement)
            {
                UIElement el = (UIElement) child;

                if (el.isVisible() && !el.getChildren().isEmpty())
                {
                    return true;
                }
            }
            else if (child.isVisible())
            {
                return true;
            }
        }

        return false;
    }

    @Override
    public void render(UIContext context)
    {
        super.render(context);

        if (!this.hasVisibleContent())
        {
            String empty = "Select a clip";
            int w = context.batcher.getFont().getWidth(empty);

            context.batcher.textShadow(empty, this.area.mx() - w / 2, this.area.my(), MinecutTokens.TEXT_DIM);
        }
    }
}
