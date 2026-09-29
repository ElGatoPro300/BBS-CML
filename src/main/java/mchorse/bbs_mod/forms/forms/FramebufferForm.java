package mchorse.bbs_mod.forms.forms;

import mchorse.bbs_mod.settings.values.numeric.ValueBoolean;
import mchorse.bbs_mod.settings.values.numeric.ValueFloat;
import mchorse.bbs_mod.settings.values.numeric.ValueInt;

public class FramebufferForm extends Form
{
    public final ValueInt width = new ValueInt("width", 512);
    public final ValueInt height = new ValueInt("height", 512);
    public final ValueFloat scale = new ValueFloat("scale", 0.5F);
    /**
     * Canvas multiplier on X/Y. {@code 1} is the legacy crop. Larger values expand
     * the visible crop area (and the world quad). Pixel budget is capped by the
     * renderer at 4096 per edge — past that the picture pixelates instead of lagging.
     */
    public final ValueFloat viewExtentX = new ValueFloat("view_extent_x", 1F, 0.01F, Float.POSITIVE_INFINITY);
    public final ValueFloat viewExtentY = new ValueFloat("view_extent_y", 1F, 0.01F, Float.POSITIVE_INFINITY);
    /**
     * World only: upright billboard facing the camera + FBO content re-rendered as an
     * orbit impostor (different sides when walking around). Ideal with low resolution.
     */
    public final ValueBoolean lookAt = new ValueBoolean("look_at", false);

    public FramebufferForm()
    {
        this.width.invisible();
        this.height.invisible();
        this.viewExtentX.invisible();
        this.viewExtentY.invisible();

        this.add(this.width);
        this.add(this.height);
        this.add(this.scale);
        this.add(this.viewExtentX);
        this.add(this.viewExtentY);
        this.add(this.lookAt);
    }
}
