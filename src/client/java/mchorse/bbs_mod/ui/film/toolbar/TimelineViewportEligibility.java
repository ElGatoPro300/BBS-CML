package mchorse.bbs_mod.ui.film.toolbar;

import mchorse.bbs_mod.ui.film.UIFilmPanel;
import mchorse.bbs_mod.ui.film.replays.UIReplaysEditor;

/**
 * Enabled-state rules for Actor / viewport toolbar actions.
 */
public final class TimelineViewportEligibility
{
    public static boolean isViewportAvailable(UIFilmPanel panel)
    {
        if (panel == null || panel.isFlying())
        {
            return false;
        }

        return panel.preview.isVisible()
            && panel.preview.area.w > 0
            && panel.preview.area.h > 0;
    }

    public static boolean canMoveReplay(UIReplaysEditor editor)
    {
        return editor != null && editor.getReplay() != null;
    }

    private TimelineViewportEligibility()
    {}
}
