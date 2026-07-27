package mchorse.bbs_minecut_ui.styles;

import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.utils.Batcher2D;
import mchorse.bbs_mod.ui.framework.styles.UIStyle;
import mchorse.bbs_mod.ui.utils.Area;
import mchorse.bbs_mod.utils.colors.Colors;

/**
 * Film-Editor MineCut dark-cyan chrome for BBS widgets.
 */
public class MinecutUIStyle extends UIStyle
{
    @Override
    public int chrome()
    {
        return MinecutTokens.TOPBAR;
    }

    @Override
    public int panel()
    {
        return MinecutTokens.PANEL;
    }

    @Override
    public int elevated()
    {
        return MinecutTokens.PANEL_ELEVATED;
    }

    @Override
    public int inner()
    {
        return MinecutTokens.PANEL_INNER;
    }

    @Override
    public int border()
    {
        return MinecutTokens.BORDER;
    }

    @Override
    public int borderSoft()
    {
        return MinecutTokens.BORDER_SOFT;
    }

    @Override
    public int accent()
    {
        return MinecutTokens.ACCENT;
    }

    @Override
    public int accentDim()
    {
        return MinecutTokens.ACCENT_DIM;
    }

    @Override
    public int text()
    {
        return MinecutTokens.TEXT;
    }

    @Override
    public int textDim()
    {
        return MinecutTokens.TEXT_DIM;
    }

    @Override
    public int textMuted()
    {
        return MinecutTokens.TEXT_MUTED;
    }

    @Override
    public void drawPanel(Batcher2D batcher, int x, int y, int w, int h)
    {
        batcher.box(x, y, x + w, y + h, this.panel());
        batcher.outline(x, y, x + w, y + h, this.borderSoft());
    }

    @Override
    public void drawPanel(Batcher2D batcher, Area area)
    {
        this.drawPanel(batcher, area.x, area.y, area.w, area.h);
    }

    @Override
    public void drawSoftRect(Batcher2D batcher, int x, int y, int w, int h, int color)
    {
        batcher.box(x, y, x + w, y + h, color);
        batcher.outline(x, y, x + w, y + h, this.borderSoft());
    }

    @Override
    public void drawButton(UIContext context, Area area, int baseRgb, boolean hover, boolean customAccent,
        boolean nLeft, boolean nRight, boolean nTop, boolean nBottom)
    {
        int fill = hover ? Colors.mulRGB(baseRgb, 1.12F) : baseRgb;

        context.batcher.box(area.x, area.y, area.ex(), area.ey(), Colors.A100 | fill);
        context.batcher.outline(area.x, area.y, area.ex(), area.ey(), this.borderSoft());
    }

    @Override
    public void drawListSelection(Batcher2D batcher, Area area, boolean selected, boolean hover)
    {
        if (selected)
        {
            batcher.box(area.x, area.y, area.ex(), area.ey(), Colors.setA(this.accent(), 0.35F));
            batcher.box(area.x, area.y, area.x + 2, area.ey(), this.accent());
        }
        else if (hover)
        {
            batcher.box(area.x, area.y, area.ex(), area.ey(), Colors.setA(this.accent(), 0.12F));
        }
    }

    @Override
    public void drawOverlayChrome(Batcher2D batcher, Area area)
    {
        batcher.box(area.x, area.y, area.ex(), area.ey(), this.elevated());
        batcher.outline(area.x, area.y, area.ex(), area.ey(), this.border());
    }

    @Override
    public void drawFormCell(Batcher2D batcher, int x, int y, int w, int h, boolean selected, boolean hover)
    {
        int bg = this.inner();

        if (selected)
        {
            bg = Colors.setA(this.accent(), 0.25F);
        }
        else if (hover)
        {
            bg = Colors.setA(this.accent(), 0.1F);
        }

        batcher.box(x, y, x + w, y + h, bg);
        batcher.outline(x, y, x + w, y + h, selected ? this.accent() : this.borderSoft());
    }

    @Override
    public void drawTabUnderline(Batcher2D batcher, int x, int y, int textWidth, boolean active)
    {
        if (active)
        {
            batcher.box(x, y, x + textWidth, y + 2, this.accent());
        }
    }
}
