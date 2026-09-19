package mchorse.bbs_mod.ui.framework.elements.navigation;

import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.ui.dashboard.panels.UIDashboardPanels;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIButton;
import mchorse.bbs_mod.ui.framework.elements.utils.FontRenderer;
import mchorse.bbs_mod.ui.utils.UIUtils;
import mchorse.bbs_mod.ui.utils.icons.Icon;
import mchorse.bbs_mod.utils.colors.Colors;

import java.util.function.Consumer;

public class UIIconTabButton extends UIButton
{
    private static final int REMOVE_GUTTER = 22;
    private static final int CONTENT_PADDING_LEFT = 8;

    private final Icon icon;
    private Consumer<UIIconTabButton> removeCallback;
    private boolean active;

    public UIIconTabButton(IKey label, Icon icon, Consumer<UIButton> callback)
    {
        super(label, callback);

        this.icon = icon;
    }

    public UIIconTabButton removable(Consumer<UIIconTabButton> callback)
    {
        this.removeCallback = callback;

        return this;
    }

    public boolean isRemovable()
    {
        return this.removeCallback != null;
    }

    /* Marks this tab as the currently selected one (drives its styling). */
    public UIIconTabButton active(boolean active)
    {
        this.active = active;

        return this;
    }

    @Override
    public boolean subMouseClicked(UIContext context)
    {
        if (context.mouseButton == 0 && this.isRemovable() && this.isRemoveInside(context))
        {
            UIUtils.playClick();
            this.removeCallback.accept(this);

            return true;
        }

        return super.subMouseClicked(context);
    }

    @Override
    protected void renderSkin(UIContext context)
    {
        int x1 = this.area.x;
        int y1 = this.area.y;
        int x2 = this.area.ex();
        int y2 = this.area.ey();

        if (this.background)
        {
            if (this.active)
            {
                /* Active tab: same procedural fade-to-transparent highlight as
                   selected dashboard panels — gradient up into transparency
                   with a solid accent bar along the bottom. */
                UIDashboardPanels.renderHighlight(context.batcher, this.area);
            }
            else
            {
                /* Inactive tab: subtle dark gradient with a thin full border. */
                int top = this.hover ? 0xFF34343C : 0xFF2A2A30;
                int bottom = this.hover ? 0xFF26262C : 0xFF1E1E22;

                context.batcher.gradientVBox(x1, y1, x2, y2, top, bottom);
                context.batcher.outline(x1, y1, x2, y2, 0xFF15151A);
            }
        }
        else if (this.hover)
        {
            /* Borderless tabs (the "+" add button) get a subtle hover fill. */
            context.batcher.box(x1, y1, x2, y2, 0x22FFFFFF);
        }

        FontRenderer font = context.batcher.getFont();
        int removeGutter = this.isRemovable() ? REMOVE_GUTTER : 0;
        String rawLabel = this.label.get();
        boolean iconOnly = this.icon != null && (rawLabel == null || rawLabel.isEmpty());
        String label = iconOnly ? "" : font.limitToWidth(rawLabel, this.area.w - 26 - removeGutter - CONTENT_PADDING_LEFT);

        int iconWidth = this.icon == null ? 0 : this.icon.w;
        int iconHeight = this.icon == null ? 0 : this.icon.h;
        int fontHeight = font.getHeight();
        int contentHeight = Math.max(iconHeight, fontHeight);
        int contentY = this.area.my(contentHeight);
        int gap = this.icon == null || iconOnly ? 0 : 5;
        int startX = iconOnly ? this.area.mx(iconWidth) : this.area.x + CONTENT_PADDING_LEFT;
        int iconY = contentY + (contentHeight - iconHeight) / 2;
        int textY = contentY + (contentHeight - fontHeight) / 2;

        /* Active tab text is full white; inactive is muted unless hovered. */
        int contentColor = (this.active || this.hover) ? Colors.WHITE : 0xFFA6A6AE;

        if (this.icon != null)
        {
            context.batcher.icon(this.icon, contentColor, startX, iconY);
        }

        context.batcher.text(label, startX + iconWidth + gap, textY, contentColor, this.textShadow);

        if (this.isRemovable())
        {
            boolean removeHover = this.isRemoveInside(context);
            int cx = this.area.ex() - REMOVE_GUTTER / 2;
            int cy = this.area.my();

            if (removeHover)
            {
                /* Highlight box behind the close glyph on hover. */
                context.batcher.box(cx - 8, cy - 8, cx + 8, cy + 8, 0x33FFFFFF);
            }

            int crossColor = removeHover ? Colors.WHITE : (this.active ? 0xFFE0E0E6 : 0xFF8C8C94);

            this.renderCross(context, cx, cy, crossColor);
        }

        this.renderLockedArea(context);
    }

    /* Draws a small 7px "×" from two diagonal pixel strokes. */
    private void renderCross(UIContext context, int cx, int cy, int color)
    {
        for (int i = -3; i <= 3; i++)
        {
            context.batcher.box(cx + i, cy + i, cx + i + 1, cy + i + 1, color);
            context.batcher.box(cx + i, cy - i, cx + i + 1, cy - i + 1, color);
        }
    }

    private boolean isRemoveInside(UIContext context)
    {
        return this.area.isInside(context) && context.mouseX >= this.area.ex() - REMOVE_GUTTER;
    }
}
