package mchorse.bbs_minecut_ui.film;

import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_minecut_ui.styles.MinecutTokens;
import mchorse.bbs_mod.ui.framework.styles.UIStyle;
import mchorse.bbs_mod.utils.colors.Colors;

/**
 * Shared Minecut panel chrome: flat dark fill, soft border, optional title + accent underline.
 */
public class UIMinecutRegion extends UIElement
{
    public static final int HEADER_H = 22;

    private final String title;
    private boolean drawHeader = true;

    public UIMinecutRegion(String title)
    {
        this.title = title;
        this.markContainer();
        this.mouseEventPropagataion(mchorse.bbs_mod.ui.framework.elements.utils.EventPropagation.BLOCK_INSIDE);
    }

    public UIMinecutRegion noHeader()
    {
        this.drawHeader = false;

        return this;
    }

    public String getTitle()
    {
        return this.title;
    }

    public int contentTop()
    {
        return this.drawHeader ? HEADER_H : 0;
    }

    @Override
    public void render(UIContext context)
    {
        UIStyle style = UIStyle.active();

        context.batcher.box(this.area.x, this.area.y, this.area.ex(), this.area.ey(), style.panel());
        context.batcher.outline(this.area.x, this.area.y, this.area.ex(), this.area.ey(), style.borderSoft());

        if (this.drawHeader)
        {
            context.batcher.box(this.area.x, this.area.y, this.area.ex(), this.area.y + HEADER_H, style.elevated());
            context.batcher.box(this.area.x, this.area.y + HEADER_H - 1, this.area.ex(), this.area.y + HEADER_H, style.borderSoft());
            context.batcher.textShadow(this.title, this.area.x + 8, this.area.y + 7, MinecutTokens.TEXT);
        }

        super.render(context);
    }

    public void drawTabUnderline(UIContext context, int tabX, int tabW)
    {
        context.batcher.box(tabX, this.area.y + HEADER_H - 2, tabX + tabW, this.area.y + HEADER_H, Colors.A100 | (MinecutTokens.ACCENT & Colors.RGB));
    }
}
