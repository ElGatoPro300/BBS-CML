package mchorse.bbs_mod.ui.film.toolbar;

import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframeSheet;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframes;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.graphs.UIKeyframeDopeSheet;
import mchorse.bbs_mod.ui.utils.Area;

import org.lwjgl.glfw.GLFW;

/**
 * Manages an in-progress timeline interaction (pick a track, later pick a tick,
 * etc.). Renders the hint above the toolbar and coordinates cancel / confirm
 * input. Intended to grow into the full Phase 2d/3 interaction system.
 */
public class UIInteractionModeOverlay
{
    private TimelineInteractionState state;

    public boolean isActive()
    {
        return this.state != null;
    }

    public void enter(TimelineInteractionState state)
    {
        this.state = state;
    }

    public void cancel()
    {
        this.state = null;
    }

    public TimelineInteractionState getState()
    {
        return this.state;
    }

    public boolean isSheetEligible(UIKeyframeSheet sheet)
    {
        return this.state != null && sheet != null && this.state.eligible.test(sheet);
    }

    /**
     * Pulsing alpha for eligible track backgrounds (0 to max, smooth sine).
     */
    public static float getHoveredTrackPulseAlpha()
    {
        return TimelineInteractionHints.getPulseAlpha(
            TimelineToolbarSettings.INTERACTION_TRACK_HOVER_PULSE_MIN_ALPHA,
            TimelineToolbarSettings.INTERACTION_TRACK_HOVER_PULSE_MAX_ALPHA);
    }

    public static float getEligibleTrackAlpha()
    {
        return TimelineInteractionHints.getPulseAlpha(
            TimelineToolbarSettings.INTERACTION_TRACK_ELIGIBLE_PULSE_MIN_ALPHA,
            TimelineToolbarSettings.INTERACTION_TRACK_ELIGIBLE_PULSE_MAX_ALPHA);
    }

    /**
     * @return {@code true} if the event was consumed
     */
    public boolean handleMouseClicked(UIKeyframes keyframes, UIContext context)
    {
        if (this.state == null)
        {
            return false;
        }

        if (context.mouseButton == 1)
        {
            this.cancel();

            return true;
        }

        if (context.mouseButton != 0)
        {
            return false;
        }

        if (!keyframes.area.isInside(context))
        {
            return true;
        }

        if (!(keyframes.getGraph() instanceof UIKeyframeDopeSheet dopeSheet))
        {
            return true;
        }

        UIKeyframeSheet sheet = dopeSheet.getSheet(context.mouseY);

        if (sheet != null && this.state.eligible.test(sheet))
        {
            this.state.onConfirm.accept(sheet);
            this.cancel();
        }

        return true;
    }

    /**
     * @return {@code true} if the event was consumed
     */
    public boolean handleKeyPressed(UIContext context)
    {
        if (this.state == null)
        {
            return false;
        }

        if (context.isPressed(GLFW.GLFW_KEY_ESCAPE))
        {
            this.cancel();

            return true;
        }

        return false;
    }

    public void renderHint(UIContext context, Area area, UIElement source)
    {
        if (this.state == null)
        {
            return;
        }

        TimelineInteractionHints.renderHint(context, area, this.state.hint, source);
    }

    public boolean hasAnyEligibleTrack(UIKeyframes keyframes)
    {
        if (this.state == null || !(keyframes.getGraph() instanceof UIKeyframeDopeSheet dopeSheet))
        {
            return false;
        }

        for (UIKeyframeSheet sheet : dopeSheet.getSheets())
        {
            if (this.state.eligible.test(sheet))
            {
                return true;
            }
        }

        return false;
    }
}
