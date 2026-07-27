package mchorse.bbs_mod.ui.framework.elements.buttons;

import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.IUIElement;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.utils.FontRenderer;
import mchorse.bbs_mod.ui.framework.elements.utils.ITextColoring;
import mchorse.bbs_mod.utils.colors.Colors;

import java.util.function.Consumer;

public class UIButton extends UIClickable<UIButton> implements ITextColoring
{
    public IKey label;

    public int textColor = Colors.WHITE;
    public boolean textShadow = true;

    public boolean custom;
    public int customColor;
    public boolean background = true;

    public UIButton(IKey label, Consumer<UIButton> callback)
    {
        super(callback);

        this.label = label;
        this.h(20);
    }

    public UIButton color(int color)
    {
        this.custom = true;
        this.customColor = color & Colors.RGB;

        return this;
    }

    public UIButton textColor(int color, boolean shadow)
    {
        this.textColor = color;
        this.textShadow = shadow;

        return this;
    }

    public UIButton background(boolean background)
    {
        this.background = background;

        return this;
    }

    @Override
    public void setColor(int color, boolean shadow)
    {
        this.textColor = color;
        this.textShadow = shadow;
    }

    @Override
    protected UIButton get()
    {
        return this;
    }

    @Override
    protected void renderSkin(UIContext context)
    {
        FontRenderer font = context.batcher.getFont();
        String label = font.limitToWidth(this.label.get(), this.area.w - 6);
        int tx = this.area.mx(font.getWidth(label));
        int ty = this.area.my(font.getHeight());

        if (this.background)
        {
            int base = (this.custom ? this.customColor : BBSSettings.primaryColor.get()) & Colors.RGB;
            boolean nLeft = this.hasNeighbor(-1, 0);
            boolean nRight = this.hasNeighbor(1, 0);
            boolean nTop = this.hasNeighbor(0, -1);
            boolean nBottom = this.hasNeighbor(0, 1);

            mchorse.bbs_mod.ui.framework.styles.UIStyle.active().drawButton(
                context,
                this.area,
                base,
                this.hover,
                this.custom,
                nLeft,
                nRight,
                nTop,
                nBottom
            );
        }

        context.batcher.text(label, tx, ty, this.textColor, this.textShadow);

        this.renderLockedArea(context);
    }

    /* True when another backgrounded button sits flush against this one in the
       given direction: dx -1/+1 = left/right, dy -1/+1 = above/below. */
    private boolean hasNeighbor(int dx, int dy)
    {
        UIElement parent = this.getParent();

        if (parent == null)
        {
            return false;
        }

        for (IUIElement child : parent.getChildren())
        {
            if (child == this || !(child instanceof UIButton other))
            {
                continue;
            }

            if (!other.background || !other.isVisible())
            {
                continue;
            }

            if (dx > 0 && touchesV(other, this.area.ex(), other.area.x))
            {
                return true;
            }
            if (dx < 0 && touchesV(other, this.area.x, other.area.ex()))
            {
                return true;
            }
            if (dy > 0 && touchesH(other, this.area.ey(), other.area.y))
            {
                return true;
            }
            if (dy < 0 && touchesH(other, this.area.y, other.area.ey()))
            {
                return true;
            }
        }

        return false;
    }

    /* Vertically-stacked-edges helper: the two given X coords touch and the
       other button shares this one's vertical span. */
    private boolean touchesV(UIButton other, int edgeA, int edgeB)
    {
        return Math.abs(edgeA - edgeB) <= 1
            && other.area.y == this.area.y && other.area.h == this.area.h;
    }

    /* Horizontal-edges helper: the two given Y coords touch and the other
       button shares this one's horizontal span. */
    private boolean touchesH(UIButton other, int edgeA, int edgeB)
    {
        return Math.abs(edgeA - edgeB) <= 1
            && other.area.x == this.area.x && other.area.w == this.area.w;
    }
}