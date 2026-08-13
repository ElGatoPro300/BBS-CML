package mchorse.bbs_mod.ui.framework.elements;

import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.utils.FontRenderer;
import mchorse.bbs_mod.ui.framework.elements.utils.UILabel;
import mchorse.bbs_mod.ui.utils.Area;
import mchorse.bbs_mod.ui.utils.UIConstants;
import mchorse.bbs_mod.ui.utils.icons.Icon;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import mchorse.bbs_mod.utils.colors.Colors;

/**
 * Foldable group of widgets used by clip property panels.
 * Click the header to hide or show {@link #fields}.
 */
public class UISection extends UIElement
{
    /* Same charcoal as the film workspace chrome (#191a1c), fully opaque. */
    private static final int PANEL = 0xFF191A1C;
    private static final int PANEL_HOVER = 0xFF222326;
    private static final int BAR_H = 14;
    private static final int CHEVRON_PAD = 2;

    public UILabel title;
    public UIElement fields;

    private boolean open = true;

    public UISection()
    {
        this(IKey.EMPTY);
    }

    public UISection(IKey title)
    {
        super();

        this.title = new UILabel(title)
        {
            @Override
            public void render(UIContext context)
            {
                UISection.this.paintBar(context, this);
            }
        };
        this.title.h(BAR_H);

        this.fields = new UIElement();
        this.fields.column().stretch().vertical().height(20);

        this.column(UIConstants.MARGIN).stretch().vertical().padding(2);
        this.add(this.title, this.fields);
    }

    @Override
    public boolean subMouseClicked(UIContext context)
    {
        if (this.title.area.isInside(context))
        {
            this.toggle();

            return true;
        }

        return super.subMouseClicked(context);
    }

    @Override
    public void render(UIContext context)
    {
        context.batcher.box(this.area.x, this.area.y, this.area.ex(), this.area.ey(), PANEL);

        super.render(context);
    }

    public UISection title(IKey title)
    {
        this.title.label = title;

        return this;
    }

    public boolean isExpanded()
    {
        return this.open;
    }

    public void toggle()
    {
        this.setExpanded(!this.open);
    }

    public void setExpanded(boolean expanded)
    {
        if (this.open == expanded)
        {
            return;
        }

        this.open = expanded;

        if (expanded)
        {
            this.add(this.fields);
        }
        else
        {
            this.fields.removeFromParent();
        }

        if (this.getParent() != null)
        {
            this.getParent().resize();
        }
    }

    private void paintBar(UIContext context, UILabel title)
    {
        Area bar = title.area;
        FontRenderer font = context.batcher.getFont();
        boolean hover = bar.isInside(context);

        context.batcher.box(bar.x, bar.y, bar.ex(), bar.ey(), hover ? PANEL_HOVER : PANEL);

        Icon chevron = this.open ? Icons.UNCOLLAPSED : Icons.COLLAPSED;
        int ix = bar.ex() - chevron.w - CHEVRON_PAD;
        int iy = bar.my() - chevron.h / 2;

        context.batcher.icon(chevron, hover ? Colors.WHITE : 0xFFCCCCCC, ix, iy);

        int maxW = Math.max(8, ix - bar.x - 6);
        String text = font.limitToWidth(title.label.get(), maxW);

        context.batcher.textShadow(text, bar.x + 4, bar.my() - font.getHeight() / 2, Colors.WHITE);
    }
}
