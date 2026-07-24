package mchorse.bbs_mod.ui.framework.elements.input;

import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIClickable;
import mchorse.bbs_mod.ui.framework.elements.utils.FontRenderer;
import mchorse.bbs_mod.utils.colors.Colors;

import java.util.function.Consumer;

/**
 * Slim colored disclosure bar (left label, right arrow) with animated body.
 * Same open/close animation as before; header is thinner than a chunky {@code UIButton}.
 */
public class UIPoseSectionCollapse extends UIElement
{
    private static final int HEADER_H = 16;

    private IKey baseLabel;
    private final SectionHeader toggle;
    private final UIAnimatedCollapseShell shell;
    private final Runnable onExpand;
    private Runnable onToggle;
    private boolean expanded;
    private int trackColor;

    public UIPoseSectionCollapse(IKey label, int trackColor, UIElement content)
    {
        this(label, trackColor, content, null);
    }

    public UIPoseSectionCollapse(IKey label, int trackColor, UIElement content, Runnable onExpand)
    {
        super();

        this.h(HEADER_H);

        this.baseLabel = label;
        this.trackColor = trackColor & Colors.RGB;
        this.onExpand = onExpand;
        this.toggle = new SectionHeader((b) ->
        {
            if (this.onToggle != null)
            {
                this.onToggle.run();
            }

            this.setExpanded(!this.expanded);
        });
        this.toggle.full(this).h(HEADER_H);

        this.shell = new UIAnimatedCollapseShell(content);

        this.add(this.toggle);
        this.syncHeaderLabel();
    }

    public UIPoseSectionCollapse onToggle(Runnable onToggle)
    {
        this.onToggle = onToggle;

        return this;
    }

    public SectionHeader getToggle()
    {
        return this.toggle;
    }

    public UIAnimatedCollapseShell getShell()
    {
        return this.shell;
    }

    public boolean isExpanded()
    {
        return this.expanded;
    }

    public void setBaseLabel(IKey label)
    {
        this.baseLabel = label;
        this.syncHeaderLabel();
    }

    public void setExpanded(boolean expanded)
    {
        /* Skip only when label + shell body already match (shell must be attached when open). */
        if (this.expanded == expanded && this.shell.isOpen() == expanded && (!expanded || this.shell.hasParent()))
        {
            return;
        }

        this.shell.setExpanded(expanded, this);

        /* Attach can no-op if the host is not in the tree yet — keep arrow in sync. */
        this.expanded = this.shell.isOpen() || (expanded && this.shell.isAnimating());
        this.syncHeaderLabel();

        if (this.expanded && this.onExpand != null)
        {
            this.onExpand.run();
        }
    }

    private void syncHeaderLabel()
    {
        this.toggle.setLabel(this.baseLabel);
        this.toggle.setExpanded(this.expanded);
        this.toggle.setTrackColor(this.trackColor);
    }

    /**
     * Thin colored bar: name on the left, disclosure arrow on the right.
     */
    public static class SectionHeader extends UIClickable<SectionHeader>
    {
        private IKey label = IKey.EMPTY;
        private boolean open;
        private int color = Colors.ACTIVE & Colors.RGB;

        public SectionHeader(Consumer<SectionHeader> callback)
        {
            super(callback);

            this.h(HEADER_H);
        }

        public void setLabel(IKey label)
        {
            this.label = label == null ? IKey.EMPTY : label;
        }

        public void setExpanded(boolean open)
        {
            this.open = open;
        }

        public void setTrackColor(int color)
        {
            this.color = color & Colors.RGB;
        }

        @Override
        protected SectionHeader get()
        {
            return this;
        }

        @Override
        protected void renderSkin(UIContext context)
        {
            int fill = 0xFF000000 | Colors.mulRGB(this.color, this.hover ? 1.12F : 1F);
            int border = 0xFF000000 | Colors.mulRGB(this.color, this.hover ? 1.35F : 0.55F);

            context.batcher.box(this.area.x, this.area.y, this.area.ex(), this.area.ey(), fill);
            context.batcher.outline(this.area.x, this.area.y, this.area.ex(), this.area.ey(), border);

            FontRenderer font = context.batcher.getFont();
            String text = this.label.get();
            String arrow = this.open ? "▼" : "▶";
            int textY = this.area.my(font.getHeight());
            int maxTextW = Math.max(8, this.area.w - 22);

            text = font.limitToWidth(text, maxTextW);
            context.batcher.textShadow(text, this.area.x + 6, textY, Colors.WHITE);
            context.batcher.textShadow(arrow, this.area.ex() - 6 - font.getWidth(arrow), textY, Colors.WHITE);
        }
    }
}
