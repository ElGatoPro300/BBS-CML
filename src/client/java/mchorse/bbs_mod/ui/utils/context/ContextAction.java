package mchorse.bbs_mod.ui.utils.context;

import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.utils.FontRenderer;
import mchorse.bbs_mod.ui.utils.icons.Icon;
import mchorse.bbs_mod.utils.colors.Colors;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ContextAction
{
    public static final int SUBMENU_ARROW_WIDTH = 12;

    public Icon icon;
    public IKey label;
    public Runnable runnable;

    public IKey keyCategory;
    public int[] keys;
    public int order = -1;

    /**
     * When non-empty, hovering this row opens a child context menu (flyout).
     */
    public List<ContextAction> children = Collections.emptyList();

    /**
     * Flips the trailing submenu arrow to {@code <} while the flyout is open on the left.
     */
    public boolean submenuOpenedToLeft;

    public ContextAction(Icon icon, IKey label, Runnable runnable)
    {
        this.icon = icon;
        this.label = label;
        this.runnable = runnable;
    }

    public ContextAction key(IKey keyCategory, int... keys)
    {
        this.keyCategory = keyCategory;

        return this.key(keys);
    }

    public ContextAction key(int... keys)
    {
        this.keys = keys;

        return this;
    }

    public ContextAction order(int order)
    {
        this.order = order;

        return this;
    }

    public ContextAction children(ContextAction... children)
    {
        this.children = new ArrayList<>(Arrays.asList(children));

        return this;
    }

    public boolean hasChildren()
    {
        return this.children != null && !this.children.isEmpty();
    }

    public int getWidth(FontRenderer font)
    {
        int width = 28 + font.getWidth(this.label.get());

        if (this.hasChildren())
        {
            width += SUBMENU_ARROW_WIDTH;
        }

        return width;
    }

    public void render(UIContext context, FontRenderer font, int x, int y, int w, int h, boolean hover, boolean selected)
    {
        this.renderBackground(context, x, y, w, h, hover, selected);

        context.batcher.icon(this.icon, x + 2, y + h / 2, 0, 0.5F);
        context.batcher.text(this.label.get(), x + 22, y + (h - font.getHeight()) / 2 + 1, Colors.WHITE, false);

        if (this.hasChildren())
        {
            String arrow = this.submenuOpenedToLeft ? "<" : ">";
            int arrowX = x + w - SUBMENU_ARROW_WIDTH + 2;
            int arrowY = y + (h - font.getHeight()) / 2 + 1;

            context.batcher.text(arrow, arrowX, arrowY, Colors.WHITE, false);
        }
    }

    protected void renderBackground(UIContext context, int x, int y, int w, int h, boolean hover, boolean selected)
    {
        if (hover)
        {
            context.batcher.box(x, y, x + w, y + h, Colors.A50 | BBSSettings.primaryColor.get());
        }
    }
}
