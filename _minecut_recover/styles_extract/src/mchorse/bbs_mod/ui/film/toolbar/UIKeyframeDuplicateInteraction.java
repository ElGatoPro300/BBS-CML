package mchorse.bbs_mod.ui.film.toolbar;

import mchorse.bbs_mod.graphics.window.Window;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframes;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.graphs.UIKeyframeDopeSheet;
import mchorse.bbs_mod.ui.utils.Area;

import org.lwjgl.glfw.GLFW;

/**
 * Keyframe duplicate preview modes matching Alt+click: single-track targets
 * share the hovered factory; multi-track duplicates stay on selected tracks.
 */
public class UIKeyframeDuplicateInteraction
{
    private KeyframeDuplicateInteractionState state;
    private float anchorTick = -1F;
    private boolean canConfirm;

    public boolean isActive()
    {
        return this.state != null;
    }

    public void enter(KeyframeDuplicateInteractionState state)
    {
        this.state = state;
        this.anchorTick = -1F;
        this.canConfirm = false;
    }

    public void cancel()
    {
        this.state = null;
        this.anchorTick = -1F;
        this.canConfirm = false;
    }

    public KeyframeDuplicateInteractionState getState()
    {
        return this.state;
    }

    public float getAnchorTick()
    {
        return this.anchorTick;
    }

    public void updatePreview(UIKeyframes keyframes, UIContext context)
    {
        this.anchorTick = -1F;
        this.canConfirm = false;

        if (this.state == null || !keyframes.area.isInside(context)
            || !keyframes.hasSelectedKeyframes())
        {
            return;
        }

        if (!(keyframes.getGraph() instanceof UIKeyframeDopeSheet dopeSheet))
        {
            return;
        }

        float tick = this.resolveAnchorTick(keyframes, context);

        if (tick < 0F)
        {
            return;
        }

        this.anchorTick = tick;
        this.canConfirm = dopeSheet.canDuplicatePreview(tick, context.mouseY);
    }

    private float resolveAnchorTick(UIKeyframes keyframes, UIContext context)
    {
        if (this.state.lockedTick >= 0)
        {
            return this.state.lockedTick;
        }

        if (!keyframes.area.isInside(context))
        {
            return -1F;
        }

        float tick = (float) keyframes.fromGraphX(context.mouseX);

        if (!Window.isShiftPressed())
        {
            tick = Math.round(tick);
        }

        return tick;
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

        if (this.canConfirm && this.anchorTick >= 0F)
        {
            keyframes.duplicateSelectedKeyframes(this.anchorTick, context.mouseY);
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

    public void renderPreviews(UIKeyframes keyframes, UIContext context)
    {
        if (this.state == null || this.anchorTick < 0F
            || !(keyframes.getGraph() instanceof UIKeyframeDopeSheet dopeSheet))
        {
            return;
        }

        int mouseY = context.mouseY;

        dopeSheet.renderDuplicatePreviews(context, this.anchorTick, mouseY);
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
