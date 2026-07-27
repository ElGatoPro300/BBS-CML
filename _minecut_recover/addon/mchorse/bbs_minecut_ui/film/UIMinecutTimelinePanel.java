package mchorse.bbs_minecut_ui.film;

import mchorse.bbs_mod.ui.framework.elements.UIElement;

/**
 * Single-purpose Timeline dock leaf hosting one BBS editor (Replay / Camera / Action).
 * Classic tab chrome supplies the label when several share a {@code TabbedNode}.
 */
public class UIMinecutTimelinePanel extends UIMinecutRegion
{
    public final UIElement content;

    public UIMinecutTimelinePanel(String title)
    {
        super(title);
        this.noHeader();

        this.content = new UIElement();
        this.content.relative(this).x(0).y(0).w(1F).h(1F);
        this.add(this.content);
    }

    public UIElement getContent()
    {
        return this.content;
    }
}
