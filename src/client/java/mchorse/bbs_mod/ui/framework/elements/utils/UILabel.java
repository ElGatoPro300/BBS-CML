package mchorse.bbs_mod.ui.framework.elements.utils;

import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.utils.colors.Colors;

import java.util.function.Supplier;

public class UILabel extends UIElement implements ITextColoring
{
    public IKey label;
    public int color;
    public boolean textShadow = true;
    public float anchorX;
    public float anchorY;
    public int background;
    public Supplier<Integer> backgroundColor;

    public UILabel(IKey label)
    {
        this(label, Colors.WHITE);
    }

    public UILabel(IKey label, int color)
    {
        super();

        this.label = label;
        this.color = color;
    }

    @Override
    public void setColor(int color, boolean shadow)
    {
        this.color(color, shadow);
    }

    public UILabel color(int color)
    {
        return this.color(color, true);
    }

    public UILabel color(int color, boolean textShadow)
    {
        this.textShadow = textShadow;
        this.color = color;

        return this;
    }

    public UILabel background()
    {
        return this.background(Colors.A50);
    }

    public UILabel background(int color)
    {
        this.background = color;

        return this;
    }

    public UILabel background(Supplier<Integer> color)
    {
        this.backgroundColor = color;

        return this;
    }

    public UILabel labelAnchor(float x, float y)
    {
        this.anchorX = x;
        this.anchorY = y;

        return this;
    }

    @Override
    public void render(UIContext context)
    {
        FontRenderer font = context.batcher.getFont();
        String label = font.limitToWidth(this.label.get(), this.area.w - 4);
        int x = this.area.x(this.anchorX, font.getWidth(label));
        int y = this.area.y(this.anchorY, font.getHeight());
        int background = this.backgroundColor == null ? this.background : this.backgroundColor.get();

        context.batcher.textCard(label, x, y, this.color, background, 3, this.textShadow);

        super.render(context);
    }
}