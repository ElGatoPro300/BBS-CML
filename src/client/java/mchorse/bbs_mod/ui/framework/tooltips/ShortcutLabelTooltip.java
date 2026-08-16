package mchorse.bbs_mod.ui.framework.tooltips;

import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.utils.FontRenderer;
import mchorse.bbs_mod.ui.framework.tooltips.styles.TooltipStyle;
import mchorse.bbs_mod.ui.utils.Area;
import mchorse.bbs_mod.ui.utils.keys.KeyCombo;
import mchorse.bbs_mod.utils.Direction;
import mchorse.bbs_mod.utils.MathUtils;

import java.util.List;

/**
 * Tooltip that appends a user-configurable keyboard shortcut in parentheses
 * after the label, rendered in a muted gray color.
 */
public class ShortcutLabelTooltip implements ITooltip
{
    public IKey label;
    public KeyCombo shortcut;
    public int width = 200;
    public Direction direction;

    public ShortcutLabelTooltip(IKey label, KeyCombo shortcut, Direction direction)
    {
        this.label = label;
        this.shortcut = shortcut;
        this.direction = direction;
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
        String suffix = this.getShortcutSuffix();
        List<String> strings = font.wrap(label, this.width);

        if (strings.isEmpty())
        {
            return;
        }

        TooltipStyle style = TooltipStyle.get();
        Direction dir = this.direction;
        Area area = context.tooltip.area;
        int contentWidth = this.getContentWidth(font, strings, suffix);

        this.calculate(context, strings, contentWidth, dir, area, Area.SHARED);

        if (Area.SHARED.intersects(area))
        {
            this.calculate(context, strings, contentWidth, dir.opposite(), area, Area.SHARED);
        }

        Area.SHARED.offset(3);
        style.renderBackground(context, Area.SHARED);
        Area.SHARED.offset(-3);

        for (int i = 0; i < strings.size(); i++)
        {
            String line = strings.get(i);

            context.batcher.text(line, Area.SHARED.x, Area.SHARED.y, style.getTextColor());

            if (i == 0 && !suffix.isEmpty())
            {
                int shortcutX = Area.SHARED.x + font.getWidth(line);

                context.batcher.text(suffix, shortcutX, Area.SHARED.y, style.getShortcutTextColor());
            }

            Area.SHARED.y += font.getHeight() + 4;
        }
    }

    private String getShortcutSuffix()
    {
        if (this.shortcut == null || this.shortcut.keys.isEmpty())
        {
            return "";
        }

        return " (" + this.shortcut.getKeyCombo() + ")";
    }

    private int getContentWidth(FontRenderer font, List<String> strings, String suffix)
    {
        int w = 0;

        for (int i = 0; i < strings.size(); i++)
        {
            int lineWidth = font.getWidth(strings.get(i));

            if (i == 0)
            {
                lineWidth += font.getWidth(suffix);
            }

            w = Math.max(w, lineWidth);
        }

        if (strings.size() > 1)
        {
            w = Math.max(w, this.width);
        }

        return w;
    }

    private void calculate(UIContext context, List<String> strings, int contentWidth, Direction dir, Area elementArea, Area targetArea)
    {
        FontRenderer font = context.batcher.getFont();
        int w = contentWidth;
        int h = (font.getHeight() + 4) * strings.size() - 4;
        int x = elementArea.x(dir.anchorX) - (int) (w * (1 - dir.anchorX)) + 6 * dir.factorX;
        int y = elementArea.y(dir.anchorY) - (int) (h * (1 - dir.anchorY)) + 6 * dir.factorY;

        x = MathUtils.clamp(x, 3, context.menu.width - w - 3);
        y = MathUtils.clamp(y, 3, context.menu.height - h - 3);

        targetArea.set(x, y, w, h);
    }
}
