package mchorse.bbs_mod.camera.clips.misc;

import mchorse.bbs_mod.settings.values.core.ValueColor;
import mchorse.bbs_mod.settings.values.core.ValueGroup;
import mchorse.bbs_mod.settings.values.numeric.ValueDouble;
import mchorse.bbs_mod.utils.colors.Color;

/**
 * Static property values used when {@link ImageClip#useKeyframes} is disabled.
 * Kept separate from keyframe channels so animation data is never destroyed.
 */
public class ImageUniform extends ValueGroup
{
    public final ValueDouble offsetX = new ValueDouble("offsetX", 0D);
    public final ValueDouble offsetY = new ValueDouble("offsetY", 0D);
    public final ValueDouble rotation = new ValueDouble("rotation", 0D);
    public final ValueDouble blend = new ValueDouble("blend", 0D, 0D, 1D);
    public final ValueDouble x = new ValueDouble("x", 0D);
    public final ValueDouble y = new ValueDouble("y", 0D);
    public final ValueDouble width = new ValueDouble("width", 100D);
    public final ValueDouble height = new ValueDouble("height", 100D);
    public final ValueDouble anchorX = new ValueDouble("anchorX", 0.5D);
    public final ValueDouble anchorY = new ValueDouble("anchorY", 0.5D);
    public final ValueDouble windowX = new ValueDouble("windowX", 0.5D);
    public final ValueDouble windowY = new ValueDouble("windowY", 0.5D);
    public final ValueDouble opacity = new ValueDouble("opacity", 1D, 0D, 1D);
    public final ValueColor color = new ValueColor("color", Color.white());

    public ImageUniform(String id)
    {
        super(id);

        this.add(this.offsetX);
        this.add(this.offsetY);
        this.add(this.rotation);
        this.add(this.blend);
        this.add(this.x);
        this.add(this.y);
        this.add(this.width);
        this.add(this.height);
        this.add(this.anchorX);
        this.add(this.anchorY);
        this.add(this.windowX);
        this.add(this.windowY);
        this.add(this.opacity);
        this.add(this.color);
    }
}
