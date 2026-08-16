package mchorse.bbs_mod.ui.film.toolbar;

import mchorse.bbs_mod.graphics.window.Window;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframeSheet;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframes;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.graphs.UIKeyframeDopeSheet;
import mchorse.bbs_mod.ui.utils.Area;
import mchorse.bbs_mod.utils.Pair;
import mchorse.bbs_mod.utils.colors.Colors;

import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Keyframe insert preview modes for the replay / keyframe toolbar: column
 * previews (I-key groups) and single-track previews (Ctrl+click equivalent).
 */
public class UIKeyframeInsertInteraction
{
    public static final List<String> COLUMN_TRACK_IDS = Arrays.asList(
        "x", "y", "z", "yaw", "pitch", "headYaw", "bodyYaw", "sneaking", "sprinting", "damage"
    );

    private KeyframeInsertInteractionState state;
    private final List<Pair<UIKeyframeSheet, Float>> columnPreviews = new ArrayList<>();
    private UIKeyframeSheet individualSheet;
    private float individualTick = -1F;

    public boolean isActive()
    {
        return this.state != null;
    }

    public void enter(KeyframeInsertInteractionState state)
    {
        this.state = state;
        this.columnPreviews.clear();
        this.individualSheet = null;
        this.individualTick = -1F;
    }

    public void cancel()
    {
        this.state = null;
        this.columnPreviews.clear();
        this.individualSheet = null;
        this.individualTick = -1F;
    }

    public KeyframeInsertInteractionState getState()
    {
        return this.state;
    }

    public void updatePreview(UIKeyframes keyframes, UIContext context)
    {
        this.columnPreviews.clear();
        this.individualSheet = null;
        this.individualTick = -1F;

        if (this.state == null || !keyframes.area.isInside(context))
        {
            return;
        }

        if (!(keyframes.getGraph() instanceof UIKeyframeDopeSheet dopeSheet))
        {
            return;
        }

        float tick = this.resolveTick(keyframes, context);

        if (tick < 0F)
        {
            return;
        }

        if (this.state.column)
        {
            for (UIKeyframeSheet sheet : dopeSheet.getSheets())
            {
                if (sheet.groupHeader || !COLUMN_TRACK_IDS.contains(sheet.id))
                {
                    continue;
                }

                this.columnPreviews.add(new Pair<>(sheet, tick));
            }
        }
        else
        {
            UIKeyframeSheet sheet = dopeSheet.getSheet(context.mouseY);

            if (sheet != null && !sheet.groupHeader)
            {
                this.individualSheet = sheet;
                this.individualTick = tick;
            }
        }
    }

    private float resolveTick(UIKeyframes keyframes, UIContext context)
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

        if (this.state.column)
        {
            if (!this.columnPreviews.isEmpty())
            {
                this.state.columnConfirm.insertAt(this.columnPreviews.get(0).b);
            }

            this.cancel();
        }
        else if (this.individualSheet != null && this.individualTick >= 0F)
        {
            this.state.individualConfirm.insertAt(this.individualSheet, this.individualTick);
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
        if (this.state == null || !(keyframes.getGraph() instanceof UIKeyframeDopeSheet dopeSheet))
        {
            return;
        }

        if (this.state.column)
        {
            for (Pair<UIKeyframeSheet, Float> preview : this.columnPreviews)
            {
                if (!dopeSheet.isTrackRowVisible(preview.a))
                {
                    continue;
                }

                dopeSheet.renderPreviewKeyframeAt(context, preview.a, preview.b, Colors.WHITE);
            }
        }
        else if (this.individualSheet != null && this.individualTick >= 0F
            && dopeSheet.isTrackRowVisible(this.individualSheet))
        {
            dopeSheet.renderPreviewKeyframeAt(context, this.individualSheet, this.individualTick, Colors.WHITE);
        }
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
