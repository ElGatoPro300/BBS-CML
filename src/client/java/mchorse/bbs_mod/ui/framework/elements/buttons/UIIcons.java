package mchorse.bbs_mod.ui.framework.elements.buttons;

import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.ui.dashboard.panels.UIDashboardPanels;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.tooltips.ITooltip;
import mchorse.bbs_mod.ui.framework.tooltips.LabelTooltip;
import mchorse.bbs_mod.ui.utils.Area;
import mchorse.bbs_mod.ui.utils.UIConstants;
import mchorse.bbs_mod.ui.utils.UIUtils;
import mchorse.bbs_mod.ui.utils.icons.Icon;
import mchorse.bbs_mod.utils.Direction;
import mchorse.bbs_mod.utils.colors.Colors;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * A horizontal row of selectable icons — an icon-based alternative to {@link UICirculate}. Each item
 * is an icon with its own tooltip; clicking an icon selects it (the active one is highlighted), and
 * the icon currently under the cursor shows its tooltip. Items split the element width evenly.
 */
public class UIIcons extends UIClickable<UIIcons>
{
    protected List<Item> items = new ArrayList<>();
    protected int value;
    private final Area tooltipCell = new Area();

    public UIIcons(Consumer<UIIcons> callback)
    {
        super(callback);

        this.h(UIConstants.CONTROL_HEIGHT);
    }

    public UIIcons add(Icon icon, IKey tooltip)
    {
        this.items.add(new Item(icon, tooltip));

        return this;
    }

    public int getCount()
    {
        return this.items.size();
    }

    public int getValue()
    {
        return this.value;
    }

    public void setValue(int value)
    {
        if (!this.items.isEmpty())
        {
            this.value = Math.max(0, Math.min(value, this.items.size() - 1));
        }
    }

    @Override
    protected UIIcons get()
    {
        return this;
    }

    @Override
    public boolean subMouseClicked(UIContext context)
    {
        if (this.isEnabled() && context.mouseButton == 0 && this.area.isInside(context) && !this.items.isEmpty())
        {
            int index = this.indexAt(context.mouseX);

            if (index != this.value)
            {
                this.value = index;
                UIUtils.playClick();

                if (this.callback != null)
                {
                    this.callback.accept(this);
                }
            }

            return true;
        }

        return super.subMouseClicked(context);
    }

    private int indexAt(int mouseX)
    {
        int count = this.items.size();
        int index = (int) ((mouseX - this.area.x) / (this.area.w / (float) count));

        return Math.max(0, Math.min(index, count - 1));
    }

    private void cellBounds(int index, Area out)
    {
        int count = this.items.size();
        float cellW = this.area.w / (float) count;
        int x1 = this.area.x + (int) (index * cellW);
        int x2 = index == count - 1 ? this.area.ex() : this.area.x + (int) ((index + 1) * cellW);

        out.set(x1, this.area.y, x2 - x1, this.area.h);
    }

    @Override
    protected void renderSkin(UIContext context)
    {
        this.tooltip = null;
        this.tooltipCell.set(0, 0, 0, 0);

        int count = this.items.size();

        if (count == 0)
        {
            this.renderLockedArea(context);

            return;
        }

        float cellW = this.area.w / (float) count;
        int hovered = this.hover ? this.indexAt(context.mouseX) : -1;

        for (int i = 0; i < count; i++)
        {
            int x1 = this.area.x + (int) (i * cellW);
            int x2 = i == count - 1 ? this.area.ex() : this.area.x + (int) ((i + 1) * cellW);
            boolean active = i == this.value;
            boolean cellHover = i == hovered;

            if (active)
            {
                Area.SHARED.set(x1, this.area.y, x2 - x1, this.area.h);
                UIDashboardPanels.renderHighlight(context.batcher, Area.SHARED, Direction.BOTTOM);
            }
            else if (cellHover)
            {
                context.batcher.box(x1, this.area.y, x2, this.area.ey(), BBSSettings.chromeSurface());
            }

            int iconColor = active ? Colors.WHITE : (cellHover ? Colors.LIGHTEST_GRAY : Colors.setA(Colors.WHITE, 0.6F));

            context.batcher.icon(this.items.get(i).icon, iconColor, (x1 + x2) / 2, this.area.my(), 0.5F, 0.5F);
        }

        if (hovered >= 0)
        {
            this.cellBounds(hovered, this.tooltipCell);
            this.tooltip = this.items.get(hovered).tooltip;
        }

        this.renderLockedArea(context);
    }

    @Override
    public void renderTooltip(UIContext context, Area area)
    {
        if (this.tooltip == null || this.tooltipCell.w <= 0)
        {
            return;
        }

        /* Anchor to the hovered icon cell, not the full row (otherwise tooltips
           sit under unrelated controls such as the Transform toggle above). */
        context.tooltip.area.set(
            context.globalX(this.tooltipCell.x),
            context.globalY(this.tooltipCell.y),
            this.tooltipCell.w,
            this.tooltipCell.h
        );
        this.tooltip.renderTooltip(context);
    }

    private static class Item
    {
        public final Icon icon;
        public final ITooltip tooltip;

        public Item(Icon icon, IKey tooltip)
        {
            this.icon = icon;
            this.tooltip = new LabelTooltip(tooltip, Direction.BOTTOM);
        }
    }
}
