package mchorse.bbs_mod.ui.framework.elements.input;

import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIButton;
import mchorse.bbs_mod.utils.colors.Colors;

import java.util.Arrays;

/**
 * Colored disclosure header (timeline Color / Glow track colors) with animated body.
 */
public class UIPoseSectionCollapse extends UIElement
{
    private static final IKey EXPANDED_ARROW = IKey.constant(" ▼");

    private final IKey baseLabel;
    private final UIButton toggle;
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

        this.baseLabel = label;
        this.onExpand = onExpand;
        this.toggle = new UIButton(label, (b) -> this.setExpanded(!this.expanded));
        this.toggle.color(trackColor & Colors.RGB).h(20);
        this.toggle.full(this);

        this.shell = new UIAnimatedCollapseShell(content);

        this.add(this.toggle);
    }

    public UIButton getToggle()
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

    public void setExpanded(boolean expanded)
    {
        if (this.expanded == expanded && this.shell.isOpen() == expanded)
        {
            return;
        }

        this.expanded = expanded;
        this.toggle.label = expanded
            ? IKey.comp(Arrays.asList(this.baseLabel, EXPANDED_ARROW))
            : this.baseLabel;

        if (expanded && this.onExpand != null)
        {
            this.onExpand.run();
        }

        this.shell.setExpanded(expanded, this);
    }
}
