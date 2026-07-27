package mchorse.bbs_mod.ui.framework.tooltips;

import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.utils.FontRenderer;
import mchorse.bbs_mod.ui.framework.tooltips.styles.TooltipStyle;
import mchorse.bbs_mod.ui.utils.Area;
import mchorse.bbs_mod.utils.Direction;
import mchorse.bbs_mod.utils.MathUtils;

import java.util.List;

public class LabelTooltip implements ITooltip
{
    public IKey label;
    public int width = 200;
    public Direction direction;

    public LabelTooltip(IKey label, Direction direction)
    {
        this.label = label;
        this.direction = direction;
    }

    public LabelTooltip(IKey label, int width, Direction direction)
    {
        this(label, direction);
        this.width = width;
    }

    @Override
    public IKey getLabel()
    {
        return this.label;
    }

    @Override
    public void renderTooltip(UIContext context)
    {
        String label = this.label.get();

        if (label.isEmpty())
        {
            return;
        }

        FontRenderer font = context.batcher.getFont();
        List<String> strings = font.wrap(label, this.width);

        if (strings.isEmpty())
        {
            return;
        }

        TooltipStyle style = TooltipStyle.get();
        Direction dir = this.direction;
        Area area = context.tooltip.area;

        this.calculate(context, strings, dir, area, Area.SHARED);

        if (Area.SHARED.intersects(area))
        {
            this.calculate(context, strings, dir.opposite(), area, Area.SHARED);
        }

        Area.SHARED.offset(3);
        style.renderBackground(context, Area.SHARED);
        Area.SHARED.offset(-3);

        for (String line : strings)
        {
            context.batcher.text(line, Area.SHARED.x, Area.SHARED.y, style.getTextColor());

            Area.SHARED.y += font.getHeight() + 4;
        }
    }

    private void calculate(UIContext context, List<String> strings, Direction dir, Area elementArea, Area targetArea)
    {
        FontRenderer font = context.batcher.getFont();
        int w = strings.size() == 1 ? font.getWidth(strings.get(0)) : this.width;
        int h = (font.getHeight() + 4) * strings.size() - 4;
        int x = elementArea.x(dir.anchorX) - (int) (w * (1 - dir.anchorX)) + 6 * dir.factorX;
        int y = elementArea.y(dir.anchorY) - (int) (h * (1 - dir.anchorY)) + 6 * dir.factorY;

        x = MathUtils.clamp(x, 3, context.menu.width - w - 3);
        y = MathUtils.clamp(y, 3, context.menu.height - h - 3);

        targetArea.set(x, y, w, h);
    }
}
