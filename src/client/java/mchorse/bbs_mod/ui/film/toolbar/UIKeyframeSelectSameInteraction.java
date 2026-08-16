package mchorse.bbs_mod.ui.film.toolbar;

import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframeSheet;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframes;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.graphs.KeyframeType;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.graphs.UIKeyframeDopeSheet;
import mchorse.bbs_mod.ui.utils.Area;
import mchorse.bbs_mod.utils.Pair;
import mchorse.bbs_mod.utils.colors.Colors;
import mchorse.bbs_mod.utils.keyframes.Keyframe;

import org.lwjgl.glfw.GLFW;

/**
 * Pick a reference keyframe under the cursor to select every keyframe sharing
 * its value. Clicking an empty area, right clicking or pressing Escape cancels
 * the operation, matching the other timeline interaction modes.
 */
public class UIKeyframeSelectSameInteraction
{
    private KeyframeSelectSameInteractionState state;
    private Keyframe hoverKeyframe;
    private UIKeyframeSheet hoverSheet;

    public boolean isActive()
    {
        return this.state != null;
    }

    public Keyframe getHoverKeyframe()
    {
        return this.hoverKeyframe;
    }

    public UIKeyframeSheet getHoverSheet()
    {
        return this.hoverSheet;
    }

    public void enter(KeyframeSelectSameInteractionState state)
    {
        this.state = state;
        this.hoverKeyframe = null;
        this.hoverSheet = null;
    }

    public void cancel()
    {
        this.state = null;
        this.hoverKeyframe = null;
        this.hoverSheet = null;
    }

    public KeyframeSelectSameInteractionState getState()
    {
        return this.state;
    }

    public void updatePreview(UIKeyframes keyframes, UIContext context)
    {
        this.hoverKeyframe = null;
        this.hoverSheet = null;

        if (this.state == null || !keyframes.area.isInside(context)
            || !(keyframes.getGraph() instanceof UIKeyframeDopeSheet dopeSheet))
        {
            return;
        }

        Pair<Keyframe, KeyframeType> found = dopeSheet.findKeyframe(context.mouseX, context.mouseY);

        if (found != null)
        {
            UIKeyframeSheet sheet = dopeSheet.getSheet(context.mouseY);

            if (sheet != null && !sheet.groupHeader && dopeSheet.isTrackRowVisible(sheet))
            {
                this.hoverKeyframe = found.a;
                this.hoverSheet = sheet;
            }
        }
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

        if (this.hoverKeyframe != null && this.hoverSheet != null)
        {
            keyframes.selectSameValue(this.hoverKeyframe, this.hoverSheet, this.state.currentTrackOnly);
        }

        this.cancel();

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

    public void renderPreview(UIKeyframes keyframes, UIContext context)
    {
        if (this.state == null || this.hoverKeyframe == null || this.hoverSheet == null
            || !(keyframes.getGraph() instanceof UIKeyframeDopeSheet dopeSheet))
        {
            return;
        }

        dopeSheet.renderPreviewKeyframeAt(context, this.hoverSheet, (float) this.hoverKeyframe.getTick(), Colors.ACTIVE | Colors.A100);
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
