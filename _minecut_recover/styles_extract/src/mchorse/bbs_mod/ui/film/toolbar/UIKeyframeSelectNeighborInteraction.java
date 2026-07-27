package mchorse.bbs_mod.ui.film.toolbar;

import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframeSheet;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframes;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.graphs.UIKeyframeDopeSheet;
import mchorse.bbs_mod.ui.utils.Area;

import org.lwjgl.glfw.GLFW;

/**
 * Track + tick pick to select the previous or next keyframe when the timeline
 * has no active keyframe selection.
 */
public class UIKeyframeSelectNeighborInteraction
{
    private static final float TRACK_PULSE_MIN_ALPHA = 0F;
    private static final float TRACK_PULSE_MAX_ALPHA = 0.5F;

    private KeyframeSelectNeighborInteractionState state;
    private UIKeyframeSheet hoverSheet;

    public boolean isActive()
    {
        return this.state != null;
    }

    public UIKeyframeSheet getHoverSheet()
    {
        return this.hoverSheet;
    }

    public static float getTrackPulseAlpha()
    {
        return TimelineInteractionHints.getPulseAlpha(TRACK_PULSE_MIN_ALPHA, TRACK_PULSE_MAX_ALPHA);
    }

    public void enter(KeyframeSelectNeighborInteractionState state)
    {
        this.state = state;
        this.hoverSheet = null;
    }

    public void cancel()
    {
        this.state = null;
        this.hoverSheet = null;
    }

    public KeyframeSelectNeighborInteractionState getState()
    {
        return this.state;
    }

    public void updatePreview(UIKeyframes keyframes, UIContext context)
    {
        this.hoverSheet = null;

        if (this.state == null || !keyframes.area.isInside(context)
            || !(keyframes.getGraph() instanceof UIKeyframeDopeSheet dopeSheet))
        {
            return;
        }

        UIKeyframeSheet sheet = dopeSheet.getSheet(context.mouseY);

        if (sheet != null && !sheet.groupHeader && dopeSheet.isTrackRowVisible(sheet))
        {
            this.hoverSheet = sheet;
        }
    }

    public float getPreviewTick(UIKeyframes keyframes, UIContext context)
    {
        if (this.state == null || !keyframes.area.isInside(context))
        {
            return -1F;
        }

        return (float) keyframes.fromGraphX(context.mouseX);
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

        if (this.hoverSheet != null)
        {
            keyframes.selectNextKeyframeAt(context.mouseX, context.mouseY, this.state.direction);
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
}
