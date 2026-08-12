package mchorse.bbs_mod.ui.film.toolbar;

import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.ui.utils.icons.Icon;

import java.util.ArrayList;
import java.util.List;

/**
 * A root entry in a timeline toolbar. Rendered as an icon with an optional
 * inline label (when horizontal space allows). Collapsed sections fall back to
 * icon-only with a hover tooltip. Clicking opens a popup with the section's
 * items.
 */
public class ToolbarSection
{
    public final IKey label;
    public final Icon icon;
    public final List<ToolbarItem> items = new ArrayList<>();

    public ToolbarSection(IKey label, Icon icon)
    {
        this.label = label;
        this.icon = icon;
    }

    public ToolbarSection add(ToolbarItem item)
    {
        this.items.add(item);

        return this;
    }

    public ToolbarSection add(ToolbarItem... items)
    {
        for (ToolbarItem item : items)
        {
            this.items.add(item);
        }

        return this;
    }
}
