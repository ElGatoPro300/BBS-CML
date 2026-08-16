package mchorse.bbs_mod.ui.film.toolbar;

/**
 * Edge of a timeline panel where the toolbar can be anchored.
 */
public enum TimelineToolbarDock
{
    TOP,
    BOTTOM,
    LEFT,
    RIGHT;

    public boolean isHorizontal()
    {
        return this == TOP || this == BOTTOM;
    }

    public String toId()
    {
        return this.name().toLowerCase();
    }

    public static TimelineToolbarDock fromId(String id)
    {
        if (id == null || id.isEmpty())
        {
            return BOTTOM;
        }

        for (TimelineToolbarDock dock : values())
        {
            if (dock.toId().equals(id))
            {
                return dock;
            }
        }

        return BOTTOM;
    }
}
