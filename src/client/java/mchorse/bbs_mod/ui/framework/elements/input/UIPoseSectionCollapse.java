package mchorse.bbs_mod.ui.framework.elements.input;

import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.ui.framework.elements.UIElement;

/**
 * Colored disclosure header (timeline Color / Glow track colors) with animated body,
 * using the same track-group accent bar + fade / body-rail style as Extra / Color grade.
 */
public class UIPoseSectionCollapse extends UIElement
{
    /**
     * Padding under the header so the first labels are not flush against the
     * disclosure toggle (kept inside the body so the section gradient still covers it).
     */
    public static final int BODY_TOP_GAP = 6;
    /** Padding under body content so the last row is not scissored when reopening. */
    public static final int BODY_BOTTOM_GAP = 2;

    private final UITrackStyleSectionHeader header;
    private final UIAnimatedCollapseShell shell;
    private final Runnable onExpand;
    private boolean expanded;

    public UIPoseSectionCollapse(IKey label, int trackColor, UIElement content)
    {
        this(label, trackColor, content, null);
    }

    public UIPoseSectionCollapse(IKey label, int trackColor, UIElement content, Runnable onExpand)
    {
        super();

        this.h(20);

        this.onExpand = onExpand;
        this.header = new UITrackStyleSectionHeader(label, trackColor, () -> this.expanded, (b) -> this.setExpanded(!this.expanded));
        this.header.full(this);

        UITrackStyleSectionBody body = new UITrackStyleSectionBody(this.header::getColor);

        body.addContent(content.marginTop(BODY_TOP_GAP));
        body.addContent(new UIElement().h(BODY_BOTTOM_GAP));

        this.shell = new UIAnimatedCollapseShell(body).flushToHost(true);

        this.add(this.header);
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
        if (this.expanded == expanded && this.shell.isOpen() == expanded)
        {
            return;
        }

        this.expanded = expanded;

        if (expanded && this.onExpand != null)
        {
            this.onExpand.run();
        }

        this.shell.setExpanded(expanded, this);
    }
}
