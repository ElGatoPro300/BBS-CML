package mchorse.bbs_mod.forms.forms;

import mchorse.bbs_mod.data.types.BaseType;
import mchorse.bbs_mod.data.types.MapType;
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
     * Display quad faces the camera (yaw + pitch), like Video/Label billboards.
     * Applies in world / film and in the form-editor preview (not UI thumbnails).
     */
    public final ValueBoolean billboard = new ValueBoolean("billboard", false);
    /**
     * FBO content is re-oriented as a camera look-at impostor (different sides when orbiting).
     * Ideal with low resolution. Independent of {@link #billboard}. Applies in world / film
     * and in the form-editor preview (not UI thumbnails).
     */
    public final ValueBoolean cameraContent = new ValueBoolean("camera_content", false);

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
        this.add(this.billboard);
        this.add(this.cameraContent);
    }

    @Override
    public void fromData(BaseType data)
    {
        boolean legacyLookAt = false;

        if (data instanceof MapType map && map.has("look_at"))
        {
            /* Pre-split: one toggle drove both billboard + camera content. */
            legacyLookAt = map.getBool("look_at");
            map.remove("look_at");
        }

        super.fromData(data);

        if (legacyLookAt)
        {
            this.billboard.set(true);
            this.cameraContent.set(true);
        }
    }
}
