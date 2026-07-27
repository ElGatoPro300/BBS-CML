package mchorse.bbs_mod.ui.film.toolbar;

import mchorse.bbs_mod.ui.film.UIClips;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.utils.Area;

import org.lwjgl.glfw.GLFW;

/**
 * Loop In/Out tick pick: pulsing column follows the cursor until confirmed.
 */
public class UILoopMarkerInteraction
{
    private LoopMarkerInteractionState state;
    private int previewTick = -1;

    public boolean isActive()
    {
        return this.state != null;
    }

    public void enter(LoopMarkerInteractionState state)
    {
        this.state = state;
        this.previewTick = -1;
    }

    public void cancel()
    {
        this.state = null;
        this.previewTick = -1;
    }

    public int getPreviewTick()
    {
        return this.previewTick;
    }

    public void updatePreview(UIClips clips, UIContext context)
    {
        this.previewTick = -1;

        if (this.state == null || clips.hasEmbeddedView() || !clips.getVerticalArea().isInside(context))
        {
            return;
        }

        this.previewTick = clips.fromGraphX(context.mouseX);
    }

    /**
     * @return {@code true} if the event was consumed
     */
    public boolean handleMouseClicked(UIClips clips, UIContext context)
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

        if (!clips.getVerticalArea().isInside(context))
        {
            return true;
        }

        if (this.previewTick >= 0)
        {
            if (this.state.setMin)
            {
                clips.setLoopMinAt(this.previewTick);
            }
            else
            {
                clips.setLoopMaxAt(this.previewTick);
            }

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

    public void renderTickPulse(UIClips clips, UIContext context)
    {
        if (this.state == null || this.previewTick < 0)
        {
            return;
        }

        int x = clips.toGraphX(this.previewTick);
        Area area = clips.area;

        TimelineInteractionHints.renderPulsingTickColumn(context, x, area.y, area.ey());
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
