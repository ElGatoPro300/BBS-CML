package mchorse.bbs_mod.camera.clips.screen;

import mchorse.bbs_mod.settings.values.core.ValueColor;
import mchorse.bbs_mod.settings.values.core.ValueGroup;
import mchorse.bbs_mod.settings.values.numeric.ValueDouble;
import mchorse.bbs_mod.utils.colors.Color;
import mchorse.bbs_mod.utils.colors.Colors;

/**
 * Static property values used when {@link LetterboxClip#useKeyframes} is disabled.
 * Kept separate from keyframe channels so animation data is never destroyed.
 */
public class LetterboxUniform extends ValueGroup
{
    public final ValueColor color = new ValueColor("color", Color.rgba(Colors.A100));
    public final ValueDouble width = new ValueDouble("width", 1D);
    public final ValueDouble height = new ValueDouble("height", 0.4D);
    public final ValueDouble smoothness = new ValueDouble("smoothness", 0D);
    public final ValueDouble rotation = new ValueDouble("rotation", 0D);
    public final ValueDouble zoom = new ValueDouble("zoom", 1D);
    public final ValueDouble offsetX = new ValueDouble("offsetX", 0D);
    public final ValueDouble offsetY = new ValueDouble("offsetY", 0D);

    public LetterboxUniform(String id)
    {
        super(id);

        this.add(this.color);
        this.add(this.width);
        this.add(this.height);
        this.add(this.smoothness);
        this.add(this.rotation);
        this.add(this.zoom);
        this.add(this.offsetX);
        this.add(this.offsetY);
    }
}
