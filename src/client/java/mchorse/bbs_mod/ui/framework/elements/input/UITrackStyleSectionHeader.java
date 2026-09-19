package mchorse.bbs_mod.ui.framework.elements.input;

import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIClickable;
import mchorse.bbs_mod.ui.framework.elements.utils.FontRenderer;
import mchorse.bbs_mod.ui.utils.Area;
import mchorse.bbs_mod.ui.utils.icons.Icon;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import mchorse.bbs_mod.utils.colors.Colors;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Clickable section header matching replay/keyframe track group style:
 * 2px vertical accent + horizontal fade, fold icon, and label (no solid button chrome).
 */
public class UITrackStyleSectionHeader extends UIClickable<UITrackStyleSectionHeader>
{
    public static final int ACCENT_WIDTH = 2;
    /** Body gradient strength under nested children (header uses a stronger fade). */
    public static final float BODY_GRADIENT_ALPHA = 0.2F;

    private final IKey label;
    private final Supplier<Boolean> expanded;
    private int color;

    public UITrackStyleSectionHeader(IKey label, int color, Supplier<Boolean> expanded, Consumer<UITrackStyleSectionHeader> callback)
    {
        super(callback);

        this.label = label;
        this.color = color & Colors.RGB;
        this.expanded = expanded;
        this.h(20);
    }

    public UITrackStyleSectionHeader color(int color)
    {
        this.color = color & Colors.RGB;

        return this;
    }

    public int getColor()
    {
        return this.color;
    }

    /**
     * Solid accent bar + header-strength horizontal fade (used by the clickable title row).
     */
    public static void renderHeaderChrome(UIContext context, Area area, int rgb, boolean hover)
    {
        float leftAlpha = hover ? 0.55F : 0.5F;

        context.batcher.box(area.x, area.y, area.x + ACCENT_WIDTH, area.ey(), Colors.A100 | rgb);
        context.batcher.gradientHBox(area.x, area.y, area.ex(), area.ey(), Colors.setA(rgb, leftAlpha), Colors.setA(rgb, 0F));
    }

    /**
     * Light horizontal fade under disclosure body children (no accent bar / indent).
     */
    public static void renderBodyChrome(UIContext context, Area area, int rgb)
    {
        if (area.h <= 0 || area.w <= 0)
        {
            return;
        }

        context.batcher.gradientHBox(area.x, area.y, area.ex(), area.ey(), Colors.setA(rgb, BODY_GRADIENT_ALPHA), Colors.setA(rgb, 0F));
    }

    @Override
    protected UITrackStyleSectionHeader get()
    {
        return this;
    }

    @Override
    protected void renderSkin(UIContext context)
    {
        renderHeaderChrome(context, this.area, this.color, this.hover);

        Icon arrow = Boolean.TRUE.equals(this.expanded.get()) ? Icons.UNCOLLAPSED : Icons.COLLAPSED;
        int iconX = this.area.x + 4;
        int midY = this.area.my();

        context.batcher.icon(arrow, iconX, midY - arrow.h / 2);

        FontRenderer font = context.batcher.getFont();
        int textX = iconX + arrow.w + 4;
        String text = font.limitToWidth(this.label.get(), Math.max(0, this.area.ex() - textX - 4));
        int textColor = this.hover ? Colors.WHITE : (Colors.WHITE & 0xeeffffff);

        context.batcher.textShadow(text, textX, midY - font.getHeight() / 2, textColor);
    }
}
