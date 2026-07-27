package mchorse.bbs_mod.ui.film.toolbar;

import mchorse.bbs_mod.ui.framework.UIContext;

import java.util.ArrayList;
import java.util.List;

/**
 * Tracks screen rectangles occupied by open timeline toolbar popups so
 * timelines underneath can ignore hover / modifier previews while the
 * cursor is over a menu chain.
 */
public final class TimelineToolbarPointerBlock
{
    public static void prepare(UIContext context)
    {
        List<ToolbarMenu> roots = context.menu.overlay.getChildren(ToolbarMenu.class, new ArrayList<>(), true);

        for (ToolbarMenu menu : roots)
        {
            if (menu.parentMenu == null)
            {
                menu.collectChainAreas(context);
            }
        }
    }

    public static boolean blocksPointer(UIContext context)
    {
        if (context.isTimelineToolbarConsumePointer())
        {
            return true;
        }

        return context.isPointerOverTimelineToolbarMenu(context.mouseX, context.mouseY);
    }

    private TimelineToolbarPointerBlock()
    {}
}
