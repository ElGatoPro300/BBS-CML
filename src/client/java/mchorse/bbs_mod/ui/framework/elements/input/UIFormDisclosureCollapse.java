package mchorse.bbs_mod.ui.framework.elements.input;

import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.utils.colors.Colors;

/**
 * Shared disclosure used by Color grade / Extra and similar form sections:
 * timeline track-group header (accent bar + horizontal fade) with fold icon,
 * and a body rail that continues the accent under nested children.
 */
public class UIFormDisclosureCollapse extends UIElement
{
    public static final int TITLE_COLOR = 0xfffa0e49;
    /** Amber — matches the Color timeline track ({@link Colors#INACTIVE}). */
    public static final int EXTRA_COLOR = Colors.INACTIVE;
    /**
     * Padding under the header so label accents (e.g. Spanish tildes) are not
     * clipped by {@link UIAnimatedCollapseShell}'s content scissor.
     */
    public static final int BODY_TOP_GAP = 3;
    /** Padding under body content so the last row is not scissored when reopening. */
    public static final int BODY_BOTTOM_GAP = 2;

    private final UITrackStyleSectionHeader header;
    private final UIAnimatedCollapseShell shell;
    private UIElement shellHost;
    private Runnable onExpand;
    private boolean expanded;

    public UIFormDisclosureCollapse(IKey label, UIElement body)
    {
        this(label, body, TITLE_COLOR);
    }

    public UIFormDisclosureCollapse(IKey label, UIElement body, int color)
    {
        super();

        this.h(20);
        this.shellHost = this;

        this.header = new UITrackStyleSectionHeader(label, color, () -> this.expanded, (b) -> this.setExpanded(!this.expanded));
        this.header.full(this);
        this.add(this.header);

        UITrackStyleSectionBody padded = new UITrackStyleSectionBody(this.header::getColor);

        padded.addContent(body.marginTop(BODY_TOP_GAP));
        padded.addContent(new UIElement().h(BODY_BOTTOM_GAP));

        this.shell = new UIAnimatedCollapseShell(padded).flushToHost(true);
    }

    public UIFormDisclosureCollapse color(int color)
    {
        this.header.color(color);

        return this;
    }

    public UIFormDisclosureCollapse onExpand(Runnable onExpand)
    {
        this.onExpand = onExpand;

        return this;
    }

    /**
     * When the disclosure header is nested inside a wrapper (e.g. Color grade),
     * attach the animated body after that wrapper instead of this element.
     */
    public UIFormDisclosureCollapse shellHost(UIElement host)
    {
        this.shellHost = host == null ? this : host;

        return this;
    }

    public UITrackStyleSectionHeader getHeader()
    {
        return this.header;
    }

    public UIAnimatedCollapseShell getShell()
    {
        return this.shell;
    }

    public boolean isExpanded()
    {
        return this.expanded;
    }

    public void setExpanded(boolean expanded)
    {
        this.setExpanded(expanded, true);
    }

    /**
     * @param animate when false, snaps open so a parent (Extra) can measure the
     *                final Color grade height before animating itself
     */
    public void setExpanded(boolean expanded, boolean animate)
    {
        if (this.expanded == expanded && this.shell.isOpen() == expanded)
        {
            return;
        }

        this.expanded = expanded;

        if (expanded && this.onExpand != null)
        {
            this.onExpand.run();
        }

        this.shell.setExpanded(expanded, this.shellHost, animate);
    }
}
